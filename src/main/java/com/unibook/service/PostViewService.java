package com.unibook.service;

import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.PostView;
import com.unibook.domain.entity.User;
import com.unibook.repository.PostRepository;
import com.unibook.repository.PostViewRepository;
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
public class PostViewService {

    private final PostViewRepository postViewRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 조회 기록 (비동기 처리)
     * - 비로그인 사용자는 userId가 null
     * - 성능 영향을 최소화하기 위해 비동기로 처리
     *
     * @param postId 조회된 게시글 ID
     * @param userId 조회한 사용자 ID (비로그인 시 null)
     */
    @Async
    @Transactional
    public void recordView(Long postId, Long userId) {
        try {
            Post post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                log.warn("존재하지 않는 게시글 조회 기록 시도: postId={}", postId);
                return;
            }

            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.warn("존재하지 않는 사용자 조회 기록 시도: userId={}", userId);
                    // 비로그인 사용자로 처리
                }
            }

            PostView postView = PostView.builder()
                    .post(post)
                    .user(user)
                    .viewedAt(LocalDateTime.now())
                    .build();

            postViewRepository.save(postView);
            log.debug("게시글 조회 기록 저장 완료: postId={}, userId={}", postId, userId);

        } catch (Exception e) {
            // 조회 기록 실패가 사용자 경험에 영향을 주면 안 되므로 로그만 남김
            log.error("게시글 조회 기록 저장 실패: postId={}, userId={}", postId, userId, e);
        }
    }

    /**
     * 특정 사용자의 조회 기록 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUserViewCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return postViewRepository.countByUser_UserId(userId);
    }

    /**
     * 전체 조회 기록 개수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalViewCount() {
        return postViewRepository.count();
    }

    /**
     * 특정 게시글의 조회수 조회
     */
    @Transactional(readOnly = true)
    public long getPostViewCount(Long postId) {
        return postViewRepository.countByPost_PostId(postId);
    }
}
