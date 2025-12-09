package com.unibook.controller.api;

import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.dto.RecommendationMetricsDto;
import com.unibook.security.UserPrincipal;
import com.unibook.service.RecommendationClickService;
import com.unibook.service.RecommendationImpressionService;
import com.unibook.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 추천 시스템 API 컨트롤러
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final RecommendationClickService clickService;
    private final RecommendationImpressionService impressionService;

    /**
     * 사용자 맞춤 추천 (메인 페이지용)
     * GET /api/recommendations/for-you?limit=10
     */
    @GetMapping("/for-you")
    public ResponseEntity<Map<String, Object>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;
            log.debug("맞춤 추천 요청: userId={}, limit={}", userId, limit);

            List<PostResponseDto> recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "recommendations", recommendations,
                    "count", recommendations.size()
            ));

        } catch (Exception e) {
            log.error("맞춤 추천 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "추천 목록을 불러오는 중 오류가 발생했습니다",
                    "recommendations", List.of()
            ));
        }
    }

    /**
     * 비슷한 게시글 추천 (상세 페이지용)
     * GET /api/recommendations/similar/123?limit=6
     */
    @GetMapping("/similar/{postId}")
    public ResponseEntity<Map<String, Object>> getSimilarPosts(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "6") int limit) {

        try {
            log.debug("비슷한 게시글 추천 요청: postId={}, limit={}", postId, limit);

            List<PostResponseDto> similarPosts = recommendationService.getSimilarPosts(postId, limit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "similarPosts", similarPosts,
                    "count", similarPosts.size()
            ));

        } catch (Exception e) {
            log.error("비슷한 게시글 추천 조회 실패: postId={}", postId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "추천 목록을 불러오는 중 오류가 발생했습니다",
                    "similarPosts", List.of()
            ));
        }
    }

    /**
     * 추천 클릭 추적
     * POST /api/recommendations/track-click
     */
    @PostMapping("/track-click")
  public ResponseEntity<Map<String, Object>> trackClick(
          @RequestBody RecommendationMetricsDto.ClickTrackingRequest request,
          @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;

            log.debug("추천 클릭 추적: postId={}, type={}, position={}, userId={}",
                    request.getPostId(), request.getType(), request.getPosition(), userId);

            // 비동기로 클릭 기록
            clickService.recordClick(
                    request.getPostId(),
                    userId,
                    request.getTypeEnum(),
                    request.getPosition(),
                    request.getSourcePostId(),
                    request.getSourceLabel()
            );

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("추천 클릭 추적 실패", e);
            // 클릭 추적 실패는 사용자 경험에 영향 없으므로 success true 반환
            return ResponseEntity.ok(Map.of("success", true));
        }
    }

    /**
     * 추천 노출 추적
     * POST /api/recommendations/track-impression
     */
    @PostMapping("/track-impression")
  public ResponseEntity<Map<String, Object>> trackImpression(
          @RequestBody RecommendationMetricsDto.ImpressionTrackingRequest request,
          @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;

            log.debug("추천 노출 추적: sessionId={}, type={}, count={}, pageType={}, userId={}",
                    request.getSessionId(), request.getType(), request.getCount(),
                    request.getPageType(), userId);

            // 비동기로 노출 기록
            impressionService.recordImpression(
                    request.getSessionId(),
                    userId,
                    request.getTypeEnum(),
                    request.getCount(),
                    request.getPageType(),
                    request.getSourcePostId(),
                    request.getSourceLabel()
            );

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("추천 노출 추적 실패", e);
            // 노출 추적 실패는 사용자 경험에 영향 없으므로 success true 반환
            return ResponseEntity.ok(Map.of("success", true));
        }
    }
}
