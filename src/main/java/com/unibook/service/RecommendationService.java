package com.unibook.service;

import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.dto.RecommendationWeights;
import com.unibook.domain.dto.UserInteractionHistory;
import com.unibook.domain.entity.Book;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.Subject;
import com.unibook.domain.enums.InteractionWeight;
import com.unibook.repository.PostRepository;
import com.unibook.repository.PostViewRepository;
import com.unibook.repository.RecommendationClickRepository;
import com.unibook.repository.WishlistRepository;
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
    private final RecommendationClickRepository recommendationClickRepository;
    private final WishlistRepository wishlistRepository;

    // 적응형 가중치 임계값
    private static final long MIN_USER_VIEWS_FOR_COLLABORATIVE = 10;
    private static final long MIN_TOTAL_VIEWS_FOR_COLLABORATIVE = 1000;
    private static final long INTERMEDIATE_USER_VIEWS = 30;
    private static final long INTERMEDIATE_TOTAL_VIEWS = 5000;

    // 최신성 계산 기준 (일)
    private static final long MAX_DAYS_FOR_RECENCY = 30;

    // 다중 행동 추천 시스템 설정
    private static final int MAX_CLICKS_TO_FETCH = 20;      // 클릭 최대 조회 수
    private static final int MAX_WISHLISTS_TO_FETCH = 15;   // 찜 최대 조회 수
    private static final int MAX_VIEWS_TO_FETCH = 30;       // 조회 최대 조회 수

    // 시간 감쇠 설정
    private static final double TIME_DECAY_LAMBDA = 0.1;    // 시간 감쇠 상수 (λ)
    private static final int TIME_DECAY_THRESHOLD_DAYS = 7; // 감쇠 시작 임계값 (7일)

    /**
     * 사용자 맞춤 추천 (메인 페이지용)
     * 성능 최적화: 사용자 상호작용 이력 1회 조회 + Post 일괄 조회
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

            // 3. [최적화] 사용자 상호작용 이력 1회 조회
            UserInteractionHistory history = getUserInteractionHistory(userId);

            // 4. [최적화] 상호작용한 모든 게시글 ID 수집
            Set<Long> interactionPostIds = new HashSet<>();
            history.getClicks().forEach(r -> interactionPostIds.add(r.getPostId()));
            history.getWishlists().forEach(r -> interactionPostIds.add(r.getPostId()));
            history.getViews().forEach(r -> interactionPostIds.add(r.getPostId()));

            // 5. [최적화] 게시글 일괄 조회 (IN 쿼리 1회)
            Map<Long, Post> interactionPostMap = new HashMap<>();
            if (!interactionPostIds.isEmpty()) {
                List<Post> interactionPosts = postRepository.findAllById(interactionPostIds);
                interactionPostMap = interactionPosts.stream()
                        .collect(Collectors.toMap(Post::getPostId, p -> p));
            }

            // 6. 각 게시글에 대한 점수 계산
            Map<Long, Double> scores = new HashMap<>();
            for (Post post : candidates) {
                double contentScore = calculateContentBasedScore(post, history, interactionPostMap);
                double collaborativeScore = calculateCollaborativeScore(post, userId);

                double finalScore = contentScore * weights.getContent()
                        + collaborativeScore * weights.getCollaborative();

                scores.put(post.getPostId(), finalScore);
            }

            // 7. 점수 순으로 정렬하여 상위 N개 반환
            List<Long> topPostIds = scores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 8. DTO 변환 (순서 유지)
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
     * 다중 행동 추천 시스템: 클릭, 찜, 조회 이력을 차등 가중치로 활용
     * [최적화] 이력 1회 조회 + Post Map에서 O(1) 조회
     *
     * 가중치:
     * - 클릭 (1.0) > 찜 (0.7) > 조회 (0.3)
     * - 시간 감쇠: 7일 이후 지수 감소 (λ=0.1)
     * - 같은 책 (ISBN): 50%
     * - 같은 과목: 25%
     * - 같은 학과: 15%
     * - 최신성: 10%
     */
    private double calculateContentBasedScore(Post post, UserInteractionHistory history, Map<Long, Post> interactionPostMap) {
        if (history.getTotalCount() == 0) {
            return 0.5; // 이력 없으면 중립 점수
        }

        // 1. 모든 상호작용에 대한 가중 유사도 계산
        double totalScore = 0.0;
        double totalWeight = 0.0;
        LocalDateTime now = LocalDateTime.now();

        // 1-1. 클릭 이력 처리 (가중치 1.0, 시간 감쇠 적용)
        for (UserInteractionHistory.InteractionRecord click : history.getClicks()) {
            Post clickedPost = interactionPostMap.get(click.getPostId());
            if (clickedPost != null) {
                double similarity = calculateSimilarityScore(clickedPost, post);
                double decayedWeight = click.getDecayedWeight(TIME_DECAY_LAMBDA, TIME_DECAY_THRESHOLD_DAYS, now);

                totalScore += similarity * decayedWeight;
                totalWeight += decayedWeight;
            }
        }

        // 1-2. 찜 이력 처리 (가중치 0.7, 감쇠 없음)
        for (UserInteractionHistory.InteractionRecord wishlist : history.getWishlists()) {
            Post wishlistPost = interactionPostMap.get(wishlist.getPostId());
            if (wishlistPost != null) {
                double similarity = calculateSimilarityScore(wishlistPost, post);
                double weight = wishlist.getBaseWeight();

                totalScore += similarity * weight;
                totalWeight += weight;
            }
        }

        // 1-3. 조회 이력 처리 (가중치 0.3, 감쇠 없음)
        for (UserInteractionHistory.InteractionRecord view : history.getViews()) {
            Post viewedPost = interactionPostMap.get(view.getPostId());
            if (viewedPost != null) {
                double similarity = calculateSimilarityScore(viewedPost, post);
                double weight = view.getBaseWeight();

                totalScore += similarity * weight;
                totalWeight += weight;
            }
        }

        // 2. 가중 평균 계산
        double score = totalWeight > 0 ? totalScore / totalWeight : 0.5;

        // 3. 최신성 보정 (0.0 ~ 0.1 추가)
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

    /**
     * 사용자의 모든 상호작용 이력 조회
     * 다중 행동 추천 시스템용
     *
     * @param userId 사용자 ID
     * @return 사용자 상호작용 이력 (클릭, 찜, 조회)
     */
    private UserInteractionHistory getUserInteractionHistory(Long userId) {
        if (userId == null) {
            return UserInteractionHistory.builder().build();
        }

        List<UserInteractionHistory.InteractionRecord> clicks = new ArrayList<>();
        List<UserInteractionHistory.InteractionRecord> wishlists = new ArrayList<>();
        List<UserInteractionHistory.InteractionRecord> views = new ArrayList<>();

        try {
            // 1. 클릭 이력 조회 (가중치 1.0)
            List<Object[]> clickResults = recommendationClickRepository
                    .findRecentClicksWithTimestampByUserId(userId, PageRequest.of(0, MAX_CLICKS_TO_FETCH));

            for (Object[] row : clickResults) {
                Long postId = (Long) row[0];
                LocalDateTime timestamp = (LocalDateTime) row[1];

                clicks.add(UserInteractionHistory.InteractionRecord.builder()
                        .postId(postId)
                        .timestamp(timestamp)
                        .weight(InteractionWeight.CLICK)
                        .build());
            }

            // 2. 찜 이력 조회 (가중치 0.7)
            List<Long> wishlistPostIds = wishlistRepository.findPostIdsByUserId(userId);
            int wishlistCount = 0;
            for (Long postId : wishlistPostIds) {
                if (wishlistCount >= MAX_WISHLISTS_TO_FETCH) {
                    break;
                }

                // 찜은 timestamp가 없으므로 현재 시각 사용 (감쇠 없음)
                wishlists.add(UserInteractionHistory.InteractionRecord.builder()
                        .postId(postId)
                        .timestamp(LocalDateTime.now())
                        .weight(InteractionWeight.WISHLIST)
                        .build());

                wishlistCount++;
            }

            // 3. 조회 이력 조회 (가중치 0.3)
            List<Long> viewedPostIds = postViewRepository
                    .findRecentViewedPostIdsByUser_UserId(userId, PageRequest.of(0, MAX_VIEWS_TO_FETCH));

            for (Long postId : viewedPostIds) {
                // 조회는 timestamp가 없으므로 현재 시각 사용 (감쇠 없음)
                views.add(UserInteractionHistory.InteractionRecord.builder()
                        .postId(postId)
                        .timestamp(LocalDateTime.now())
                        .weight(InteractionWeight.VIEW)
                        .build());
            }

        } catch (Exception e) {
            log.warn("사용자 상호작용 이력 조회 실패: userId={}", userId, e);
        }

        return UserInteractionHistory.builder()
                .clicks(clicks)
                .wishlists(wishlists)
                .views(views)
                .build();
    }
}
