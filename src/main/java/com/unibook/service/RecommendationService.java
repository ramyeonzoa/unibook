package com.unibook.service;

import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.dto.RecommendationWeights;
import com.unibook.domain.entity.Book;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.Subject;
import com.unibook.repository.PostRepository;
import com.unibook.repository.PostViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 추천 시스템 서비스
 * - 적응형 하이브리드 추천 (Content-based + Collaborative)
 * - 정규화된 점수 시스템 (0.0 ~ 1.0)
 * - 데이터 상황에 따라 가중치 자동 조정
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final PostRepository postRepository;
    private final PostViewRepository postViewRepository;
    private final PostViewService postViewService;

    // 적응형 가중치 임계값
    private static final long MIN_USER_VIEWS_FOR_COLLABORATIVE = 10;
    private static final long MIN_TOTAL_VIEWS_FOR_COLLABORATIVE = 1000;
    private static final long INTERMEDIATE_USER_VIEWS = 30;
    private static final long INTERMEDIATE_TOTAL_VIEWS = 5000;

    // 최신성 계산 기준 (일)
    private static final long MAX_DAYS_FOR_RECENCY = 30;

    /**
     * 사용자 맞춤 추천 (메인 페이지용)
     *
     * @param userId 사용자 ID (비로그인 시 null)
     * @param limit  추천 개수
     * @return 추천 게시글 목록
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getPersonalizedRecommendations(Long userId, int limit) {
        try {
            // 1. 적응형 가중치 계산
            RecommendationWeights weights = calculateAdaptiveWeights(userId);
            log.debug("추천 가중치: strategy={}, content={}, collaborative={}",
                    weights.getStrategy(), weights.getContent(), weights.getCollaborative());

            // 2. 후보 게시글 조회 (AVAILABLE만)
            List<Post> candidates = postRepository.findByStatus(Post.PostStatus.AVAILABLE);

            // 본인 글 제외
            if (userId != null) {
                candidates = candidates.stream()
                        .filter(p -> !p.getUser().getUserId().equals(userId))
                        .collect(Collectors.toList());
            }

            if (candidates.isEmpty()) {
                log.warn("추천 후보 게시글이 없습니다.");
                return Collections.emptyList();
            }

            // 3. 사용자가 최근 본 게시글들 (참고용)
            List<Long> recentViewedPostIds = new ArrayList<>();
            if (userId != null) {
                recentViewedPostIds = postViewRepository.findRecentViewedPostIdsByUser_UserId(
                        userId, PageRequest.of(0, 10)
                );
            }

            // 4. 각 게시글에 대한 점수 계산
            Map<Long, Double> scores = new HashMap<>();
            for (Post post : candidates) {
                double contentScore = calculateContentBasedScore(post, userId, recentViewedPostIds);
                double collaborativeScore = calculateCollaborativeScore(post, userId);

                double finalScore = contentScore * weights.getContent()
                        + collaborativeScore * weights.getCollaborative();

                scores.put(post.getPostId(), finalScore);
            }

            // 5. 점수 순으로 정렬하여 상위 N개 반환
            List<Long> topPostIds = scores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 6. DTO 변환 (순서 유지)
            return candidates.stream()
                    .filter(p -> topPostIds.contains(p.getPostId()))
                    .sorted(Comparator.comparingInt(p -> topPostIds.indexOf(p.getPostId())))
                    .map(PostResponseDto::from)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("추천 시스템 오류", e);
            return Collections.emptyList();
        }
    }

    /**
     * 비슷한 게시글 추천 (상세 페이지용)
     *
     * @param postId 기준 게시글 ID
     * @param limit  추천 개수
     * @return 비슷한 게시글 목록
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getSimilarPosts(Long postId, int limit) {
        try {
            Optional<Post> basePostOpt = postRepository.findByIdWithDetails(postId);
            if (basePostOpt.isEmpty()) {
                return Collections.emptyList();
            }
            Post basePost = basePostOpt.get();

            // 후보 게시글 조회 (AVAILABLE만, 본인 글 제외)
            List<Post> candidates = postRepository.findByStatus(Post.PostStatus.AVAILABLE).stream()
                    .filter(p -> !p.getPostId().equals(postId))
                    .filter(p -> !p.getUser().getUserId().equals(basePost.getUser().getUserId()))
                    .collect(Collectors.toList());

            // 유사도 점수 계산
            Map<Long, Double> scores = new HashMap<>();
            for (Post candidate : candidates) {
                double score = calculateSimilarityScore(basePost, candidate);
                scores.put(candidate.getPostId(), score);
            }

            // 상위 N개 반환
            List<Long> topPostIds = scores.entrySet().stream()
                    .filter(e -> e.getValue() > 0.0) // 점수 있는 것만
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            return candidates.stream()
                    .filter(p -> topPostIds.contains(p.getPostId()))
                    .sorted(Comparator.comparingInt(p -> topPostIds.indexOf(p.getPostId())))
                    .map(PostResponseDto::from)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("비슷한 게시글 추천 오류", e);
            return Collections.emptyList();
        }
    }

    /**
     * 적응형 가중치 계산
     * - 데이터 부족: Content-based 90%
     * - 데이터 중간: Content-based 70%
     * - 데이터 충분: Balanced 50-50%
     */
    private RecommendationWeights calculateAdaptiveWeights(Long userId) {
        long userViewCount = 0;
        if (userId != null) {
            userViewCount = postViewService.getUserViewCount(userId);
        }
        long totalViewCount = postViewService.getTotalViewCount();

        // 데이터 충분
        if (userViewCount >= INTERMEDIATE_USER_VIEWS && totalViewCount >= INTERMEDIATE_TOTAL_VIEWS) {
            return RecommendationWeights.getBalanced();
        }

        // 중간 단계
        if (userViewCount >= MIN_USER_VIEWS_FOR_COLLABORATIVE && totalViewCount >= MIN_TOTAL_VIEWS_FOR_COLLABORATIVE) {
            return RecommendationWeights.getIntermediate();
        }

        // 데이터 부족 (기본)
        return RecommendationWeights.getDefault();
    }

    /**
     * Content-based 점수 계산 (0.0 ~ 1.0로 정규화)
     *
     * 가중치:
     * - 같은 책 (ISBN): 50%
     * - 같은 과목: 25%
     * - 같은 학과: 15%
     * - 최신성: 10%
     */
    private double calculateContentBasedScore(Post post, Long userId, List<Long> recentViewedPostIds) {
        double score = 0.0;

        // 사용자가 최근 본 게시글이 있다면, 그것들과의 유사도 계산
        if (!recentViewedPostIds.isEmpty()) {
            double maxSimilarity = 0.0;
            for (Long viewedPostId : recentViewedPostIds) {
                Optional<Post> viewedPostOpt = postRepository.findByIdWithDetails(viewedPostId);
                if (viewedPostOpt.isPresent()) {
                    double similarity = calculateSimilarityScore(viewedPostOpt.get(), post);
                    maxSimilarity = Math.max(maxSimilarity, similarity);
                }
            }
            score = maxSimilarity;
        } else {
            // 최근 본 게시글이 없으면 기본 점수
            score = 0.5; // 중립 점수
        }

        // 최신성 보정 (0.0 ~ 0.1 추가)
        double recencyScore = calculateRecencyScore(post);
        score += recencyScore * 0.1;

        return Math.min(score, 1.0); // 1.0 초과 방지
    }

    /**
     * Collaborative 점수 계산 (0.0 ~ 1.0로 정규화)
     * "이 사용자와 비슷한 취향의 사용자들이 본 게시글"
     */
    private double calculateCollaborativeScore(Post post, Long userId) {
        if (userId == null) {
            return 0.0;
        }

        try {
            // "이 사용자와 비슷한 취향의 사용자들이 본 게시글" 조회
            List<Object[]> collaborativePosts = postViewRepository.findCollaborativePostsByUserId(
                    userId, PageRequest.of(0, 50)
            );

            if (collaborativePosts.isEmpty()) {
                return 0.0;
            }

            // 해당 게시글이 목록에 있는지 확인
            long maxViewCount = 1L;
            long currentViewCount = 0L;

            for (Object[]result : collaborativePosts) {
                Long collaborativePostId = (Long) result[0];
                Long viewCount = (Long) result[1];

                maxViewCount = Math.max(maxViewCount, viewCount);

                if (collaborativePostId.equals(post.getPostId())) {
                    currentViewCount = viewCount;
                }
            }

            // 정규화 (0.0 ~ 1.0)
            return (double) currentViewCount / maxViewCount;

        } catch (Exception e) {
            log.warn("Collaborative 점수 계산 실패", e);
            return 0.0;
        }
    }

    /**
     * 두 게시글 간 유사도 점수 계산 (0.0 ~ 1.0)
     *
     * 가중치:
     * - 같은 책 (ISBN): 0.5
     * - 같은 과목: 0.25
     * - 같은 학과: 0.15
     * - 최신성: 0.10
     */
    private double calculateSimilarityScore(Post post1, Post post2) {
        double score = 0.0;

        // 1. 같은 책 (ISBN) - 50%
        if (hasSameBook(post1, post2)) {
            score += 0.5;
        }

        // 2. 같은 과목 - 25%
        if (hasSameSubject(post1, post2)) {
            score += 0.25;
        }

        // 3. 같은 학과 - 15%
        if (hasSameDepartment(post1, post2)) {
            score += 0.15;
        }

        // 4. 최신성 - 10%
        score += calculateRecencyScore(post2) * 0.10;

        return Math.min(score, 1.0); // 1.0 초과 방지
    }

    /**
     * 같은 책 여부 (ISBN 비교)
     */
    private boolean hasSameBook(Post post1, Post post2) {
        Book book1 = post1.getBook();
        Book book2 = post2.getBook();

        if (book1 == null || book2 == null) {
            return false;
        }

        return book1.getIsbn().equals(book2.getIsbn());
    }

    /**
     * 같은 과목 여부
     */
    private boolean hasSameSubject(Post post1, Post post2) {
        Subject subject1 = post1.getSubject();
        Subject subject2 = post2.getSubject();

        if (subject1 == null || subject2 == null) {
            return false;
        }

        return subject1.getSubjectId().equals(subject2.getSubjectId());
    }

    /**
     * 같은 학과 여부
     */
    private boolean hasSameDepartment(Post post1, Post post2) {
        Subject subject1 = post1.getSubject();
        Subject subject2 = post2.getSubject();

        if (subject1 == null || subject2 == null) {
            return false;
        }

        if (subject1.getProfessor() == null || subject2.getProfessor() == null) {
            return false;
        }

        if (subject1.getProfessor().getDepartment() == null ||
            subject2.getProfessor().getDepartment() == null) {
            return false;
        }

        return subject1.getProfessor().getDepartment().getDepartmentId()
                .equals(subject2.getProfessor().getDepartment().getDepartmentId());
    }

    /**
     * 최신성 점수 계산 (0.0 ~ 1.0)
     * - 오늘 등록: 1.0
     * - 30일 전: 0.0
     * - 선형 감소
     */
    private double calculateRecencyScore(Post post) {
        long daysOld = ChronoUnit.DAYS.between(post.getCreatedAt(), LocalDateTime.now());

        if (daysOld < 0) {
            return 1.0; // 미래 날짜는 1.0
        }

        if (daysOld >= MAX_DAYS_FOR_RECENCY) {
            return 0.0; // 30일 이상은 0.0
        }

        // 선형 감소
        return 1.0 - ((double) daysOld / MAX_DAYS_FOR_RECENCY);
    }
}
