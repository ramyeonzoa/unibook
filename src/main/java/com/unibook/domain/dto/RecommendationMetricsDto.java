package com.unibook.domain.dto;

import com.unibook.domain.entity.RecommendationClick.RecommendationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 추천 시스템 메트릭 관련 DTO
 */
public class RecommendationMetricsDto {

  /**
   * 전체 메트릭 응답 DTO
   */
  @Builder
  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor
  public static class Response {
    private Map<String, Long> clicksByType;  // 타입별 클릭 수
    private Long totalClicks;  // 총 클릭 수
    private LocalDateTime periodStart;  // 조회 기간 시작
    private LocalDateTime periodEnd;  // 조회 기간 종료
    private List<DailyMetric> dailyMetrics;  // 일별 통계
  }

  /**
   * 위치별 메트릭
   */
  @Builder
  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor
  public static class PositionMetric {
    private Integer position;  // 위치 (0부터 시작)
    private Long clicks;  // 클릭 수
    private Long impressions;  // 노출 수 (추정치)
    private Double ctr;  // 클릭률 (%)
  }

  /**
   * 일별 메트릭
   */
  @Builder
  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor
  public static class DailyMetric {
    private String date;  // 날짜 (yyyy-MM-dd)
    private Long forYouClicks;  // 맞춤 추천 클릭 수
    private Long similarClicks;  // 비슷한 게시글 클릭 수
    private Long totalClicks;  // 전체 클릭 수
  }

  /**
   * 타입별 통계
   */
  @Builder
  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor
  public static class TypeStats {
    private RecommendationType type;
    private Long clicks;
    private Long impressions;
    private Double ctr;
  }

  /**
   * 클릭 추적 요청 DTO (API용)
   */
  @Builder
  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor
  public static class ClickTrackingRequest {
    private Long postId;
    private String type;  // "FOR_YOU" or "SIMILAR"
    private Integer position;
    private Long sourcePostId;  // SIMILAR 타입일 경우만

    public RecommendationType getTypeEnum() {
      return RecommendationType.valueOf(type);
    }
  }

  /**
   * 노출 추적 요청 DTO (API용)
   */
  @Builder
  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor
  public static class ImpressionTrackingRequest {
    private String sessionId;  // 세션 ID (필수)
    private String type;  // "FOR_YOU" or "SIMILAR"
    private Integer count;  // 노출된 추천 개수
    private String pageType;  // "main", "detail", "list" 등
    private Long sourcePostId;  // SIMILAR 타입일 경우만

    public RecommendationType getTypeEnum() {
      return RecommendationType.valueOf(type);
    }
  }
}
