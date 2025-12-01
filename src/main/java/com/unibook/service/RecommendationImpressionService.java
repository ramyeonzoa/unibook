package com.unibook.service;

import com.unibook.domain.entity.RecommendationClick.RecommendationType;
import com.unibook.domain.entity.RecommendationImpression;
import com.unibook.domain.entity.User;
import com.unibook.repository.RecommendationImpressionRepository;
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
public class RecommendationImpressionService {

  private final RecommendationImpressionRepository impressionRepository;
  private final UserRepository userRepository;

  // 중복 방지를 위한 시간 윈도우 (5분)
  private static final int DUPLICATE_WINDOW_MINUTES = 5;

  /**
   * 추천 노출 기록 (비동기 처리)
   * - 비로그인 사용자는 userId가 null
   * - 성능 영향을 최소화하기 위해 비동기로 처리
   * - 5분 이내 같은 세션+타입은 중복으로 간주하여 기록하지 않음
   *
   * @param sessionId 세션 ID (필수)
   * @param userId 사용자 ID (비로그인 시 null)
   * @param type 추천 타입 (FOR_YOU / SIMILAR)
   * @param count 노출된 추천 개수
   * @param pageType 페이지 타입 (main, detail 등)
   * @param sourcePostId 추천 기준 게시글 ID (SIMILAR 타입일 경우만)
   */
  @Async
  @Transactional
  public void recordImpression(String sessionId, Long userId, RecommendationType type,
                                Integer count, String pageType, Long sourcePostId) {
    try {
      // 입력 검증
      if (sessionId == null || sessionId.trim().isEmpty()) {
        log.warn("세션 ID가 없는 노출 기록 시도");
        return;
      }

      if (type == null || count == null || count <= 0) {
        log.warn("유효하지 않은 노출 기록 시도: type={}, count={}", type, count);
        return;
      }

      // 중복 체크: 5분 이내 같은 세션+타입은 중복으로 간주
      LocalDateTime since = LocalDateTime.now().minusMinutes(DUPLICATE_WINDOW_MINUTES);
      boolean isDuplicate = impressionRepository.existsBySessionIdAndTypeAndSince(
              sessionId, type, since);

      if (isDuplicate) {
        log.debug("중복 노출 기록 방지: sessionId={}, type={}", sessionId, type);
        return;
      }

      // 사용자 조회 (로그인 사용자일 경우)
      User user = null;
      if (userId != null) {
        user = userRepository.findById(userId).orElse(null);
        if (user == null) {
          log.warn("존재하지 않는 사용자 노출 기록 시도: userId={}", userId);
          // 비로그인 사용자로 처리
        }
      }

      // 노출 기록 생성
      RecommendationImpression impression = RecommendationImpression.builder()
              .sessionId(sessionId)
              .user(user)
              .type(type)
              .count(count)
              .pageType(pageType)
              .sourcePostId(sourcePostId)
              .impressedAt(LocalDateTime.now())
              .build();

      impressionRepository.save(impression);
      log.debug("추천 노출 기록 저장 완료: sessionId={}, userId={}, type={}, count={}",
              sessionId, userId, type, count);

    } catch (Exception e) {
      // 노출 기록 실패가 사용자 경험에 영향을 주면 안 되므로 로그만 남김
      log.error("추천 노출 기록 저장 실패: sessionId={}, userId={}, type={}",
              sessionId, userId, type, e);
    }
  }

  /**
   * 기간별 총 노출 수 조회
   */
  @Transactional(readOnly = true)
  public long getImpressionCountByPeriod(LocalDateTime start, LocalDateTime end) {
    return impressionRepository.sumCountByPeriod(start, end);
  }

  /**
   * 타입별 기간별 총 노출 수 조회
   */
  @Transactional(readOnly = true)
  public long getImpressionCountByTypeAndPeriod(RecommendationType type,
                                                 LocalDateTime start,
                                                 LocalDateTime end) {
    return impressionRepository.sumCountByTypeAndPeriod(type, start, end);
  }

  /**
   * 기간별 유니크 세션 수 조회
   */
  @Transactional(readOnly = true)
  public long getUniqueSessionCountByPeriod(LocalDateTime start, LocalDateTime end) {
    return impressionRepository.countDistinctSessionsByPeriod(start, end);
  }

  /**
   * 전체 노출 수 조회
   */
  @Transactional(readOnly = true)
  public long getTotalImpressionCount() {
    return impressionRepository.count();
  }
}
