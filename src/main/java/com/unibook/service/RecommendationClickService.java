package com.unibook.service;

import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.RecommendationClick;
import com.unibook.domain.entity.RecommendationClick.RecommendationType;
import com.unibook.domain.entity.User;
import com.unibook.repository.PostRepository;
import com.unibook.repository.RecommendationClickRepository;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationClickService {

  private final RecommendationClickRepository clickRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  /**
   * 추천 클릭 기록 (비동기 처리)
   * - 비로그인 사용자는 userId가 null
   * - 성능 영향을 최소화하기 위해 비동기로 처리
   *
   * @param postId 클릭된 게시글 ID
   * @param userId 클릭한 사용자 ID (비로그인 시 null)
   * @param type 추천 타입 (FOR_YOU / SIMILAR)
   * @param position 추천 목록 내 위치 (0부터 시작)
   * @param sourcePostId 추천 기준 게시글 ID (SIMILAR 타입일 경우만)
   */
  @Async
  @Transactional
  public void recordClick(Long postId, Long userId, RecommendationType type,
                          Integer position, Long sourcePostId) {
    try {
      Post post = postRepository.findById(postId).orElse(null);
      if (post == null) {
        log.warn("존재하지 않는 게시글 클릭 기록 시도: postId={}", postId);
        return;
      }

      User user = null;
      if (userId != null) {
        user = userRepository.findById(userId).orElse(null);
        if (user == null) {
          log.warn("존재하지 않는 사용자 클릭 기록 시도: userId={}", userId);
          // 비로그인 사용자로 처리
        }
      }

      RecommendationClick click = RecommendationClick.builder()
              .post(post)
              .user(user)
              .type(type)
              .position(position)
              .sourcePostId(sourcePostId)
              .clickedAt(LocalDateTime.now())
              .build();

      clickRepository.save(click);
      log.debug("추천 클릭 기록 저장 완료: postId={}, userId={}, type={}, position={}",
              postId, userId, type, position);

    } catch (Exception e) {
      // 클릭 기록 실패가 사용자 경험에 영향을 주면 안 되므로 로그만 남김
      log.error("추천 클릭 기록 저장 실패: postId={}, userId={}, type={}",
              postId, userId, type, e);
    }
  }

  /**
   * 특정 사용자의 클릭 수 조회
   */
  @Transactional(readOnly = true)
  public long getUserClickCount(Long userId) {
    if (userId == null) {
      return 0;
    }
    return clickRepository.countByUserUserId(userId);
  }

  /**
   * 전체 클릭 수 조회
   */
  @Transactional(readOnly = true)
  public long getTotalClickCount() {
    return clickRepository.count();
  }

  /**
   * 특정 게시글의 클릭 수 조회
   */
  @Transactional(readOnly = true)
  public long getPostClickCount(Long postId) {
    return clickRepository.countByPostPostId(postId);
  }

  /**
   * 특정 타입의 클릭 수 조회
   */
  @Transactional(readOnly = true)
  public long getClickCountByType(RecommendationType type) {
    return clickRepository.countByType(type);
  }

  /**
   * 기간별 클릭 수 조회
   */
  @Transactional(readOnly = true)
  public long getClickCountByPeriod(LocalDateTime start, LocalDateTime end) {
    return clickRepository.countByClickedAtBetween(start, end);
  }
}
