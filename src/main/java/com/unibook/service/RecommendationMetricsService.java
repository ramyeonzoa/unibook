package com.unibook.service;

import com.unibook.domain.dto.RecommendationMetricsDto;
import com.unibook.domain.entity.RecommendationClick.RecommendationType;
import com.unibook.repository.PostViewRepository;
import com.unibook.repository.RecommendationClickRepository;
import com.unibook.repository.RecommendationImpressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationMetricsService {

  private final RecommendationClickRepository clickRepository;
  private final RecommendationImpressionRepository impressionRepository;
  private final PostViewRepository postViewRepository;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * 전체 메트릭 조회
   *
   * @param startDate 시작 날짜
   * @param endDate 종료 날짜
   * @return 메트릭 DTO
   */
  @Transactional(readOnly = true)
  public RecommendationMetricsDto.Response getMetrics(LocalDateTime startDate, LocalDateTime endDate) {
    // 1. 기본 통계
    long totalClicks = clickRepository.countByClickedAtBetween(startDate, endDate);

    // 2. 타입별 클릭 수 계산
    Map<String, Long> clicksByType = calculateClicksByType(startDate, endDate);

    // 3. 일별 메트릭
    List<RecommendationMetricsDto.DailyMetric> dailyMetrics = getDailyMetrics(startDate);

    return RecommendationMetricsDto.Response.builder()
            .clicksByType(clicksByType)
            .totalClicks(totalClicks)
            .periodStart(startDate)
            .periodEnd(endDate)
            .dailyMetrics(dailyMetrics)
            .build();
  }

  /**
   * 타입별 클릭 수 계산
   */
  @Transactional(readOnly = true)
  public Map<String, Long> calculateClicksByType(LocalDateTime startDate, LocalDateTime endDate) {
    Map<String, Long> clicksByType = new HashMap<>();

    for (RecommendationType type : RecommendationType.values()) {
      long clicks = clickRepository.countByTypeAndClickedAtBetween(type, startDate, endDate);
      clicksByType.put(type.name(), clicks);
    }

    return clicksByType;
  }

  /**
   * 위치별 클릭률 분석
   * 실제 노출 데이터를 사용하여 CTR 계산
   */
  @Transactional(readOnly = true)
  public List<RecommendationMetricsDto.PositionMetric> getPositionMetrics(RecommendationType type) {
    List<Object[]> results = clickRepository.countClicksByPosition(type);

    return results.stream()
            .map(row -> {
              Integer position = (Integer) row[0];
              Long clicks = (Long) row[1];

              // 전체 기간의 노출 수를 사용 (위치별 노출은 현재 추적하지 않으므로 전체 노출 수 사용)
              long totalImpressions = impressionRepository.countByType(type);
              // 위치별 추정: 전체 노출을 위치 수로 나눔 (대략적 추정)
              long impressions = totalImpressions > 0 ? totalImpressions / Math.max(results.size(), 1) : 0;

              double ctr = impressions > 0 ? (clicks * 100.0 / impressions) : 0.0;

              return RecommendationMetricsDto.PositionMetric.builder()
                      .position(position)
                      .clicks(clicks)
                      .impressions(impressions)
                      .ctr(Math.round(ctr * 100.0) / 100.0)
                      .build();
            })
            .collect(Collectors.toList());
  }

  /**
   * 일별 메트릭 조회
   */
  @Transactional(readOnly = true)
  public List<RecommendationMetricsDto.DailyMetric> getDailyMetrics(LocalDateTime startDate) {
    List<Object[]> results = clickRepository.countDailyClicksByType(startDate);

    // 날짜별로 그룹화
    Map<String, RecommendationMetricsDto.DailyMetric> metricsMap = new HashMap<>();

    for (Object[] row : results) {
      // JPQL DATE() 함수는 java.sql.Date를 반환하므로 변환 필요
      java.sql.Date sqlDate = (java.sql.Date) row[0];
      LocalDate date = sqlDate.toLocalDate();
      RecommendationType type = (RecommendationType) row[1];
      Long count = (Long) row[2];

      String dateStr = date.format(DATE_FORMATTER);

      RecommendationMetricsDto.DailyMetric metric = metricsMap.getOrDefault(dateStr,
              RecommendationMetricsDto.DailyMetric.builder()
                      .date(dateStr)
                      .forYouClicks(0L)
                      .similarClicks(0L)
                      .totalClicks(0L)
                      .build());

      if (type == RecommendationType.FOR_YOU) {
        metric.setForYouClicks(count);
      } else if (type == RecommendationType.SIMILAR) {
        metric.setSimilarClicks(count);
      }

      metric.setTotalClicks(metric.getForYouClicks() + metric.getSimilarClicks());
      metricsMap.put(dateStr, metric);
    }

    // 날짜순 정렬
    return metricsMap.values().stream()
            .sorted(Comparator.comparing(RecommendationMetricsDto.DailyMetric::getDate))
            .collect(Collectors.toList());
  }

  /**
   * 가장 많이 클릭된 게시글 조회
   */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> getMostClickedPosts(RecommendationType type,
                                                        LocalDateTime startDate,
                                                        int limit) {
    List<Object[]> results = clickRepository.findMostClickedPosts(type, startDate);

    return results.stream()
            .limit(limit)
            .map(row -> {
              Map<String, Object> map = new HashMap<>();
              map.put("postId", row[0]);
              map.put("clicks", row[1]);
              return map;
            })
            .collect(Collectors.toList());
  }

  /**
   * 타입별 클릭 수 통계
   */
  @Transactional(readOnly = true)
  public Map<String, Long> getClickStatsByType() {
    List<Object[]> results = clickRepository.countClicksByType();

    Map<String, Long> stats = new HashMap<>();
    for (Object[] row : results) {
      RecommendationType type = (RecommendationType) row[0];
      Long count = (Long) row[1];
      stats.put(type.name(), count);
    }

    return stats;
  }

  /**
   * 전체 CTR 계산 (기간별)
   *
   * @param startDate 시작 날짜
   * @param endDate 종료 날짜
   * @return CTR (%)
   */
  @Transactional(readOnly = true)
  public double calculateCTR(LocalDateTime startDate, LocalDateTime endDate) {
    long totalClicks = clickRepository.countByClickedAtBetween(startDate, endDate);
    long totalImpressions = impressionRepository.sumCountByPeriod(startDate, endDate);

    if (totalImpressions == 0) {
      return 0.0;
    }

    double ctr = (totalClicks * 100.0) / totalImpressions;
    return Math.round(ctr * 100.0) / 100.0; // 소수점 2자리
  }

  /**
   * 타입별 CTR 계산 (기간별)
   *
   * @param type 추천 타입
   * @param startDate 시작 날짜
   * @param endDate 종료 날짜
   * @return CTR (%)
   */
  @Transactional(readOnly = true)
  public double calculateCTRByType(RecommendationType type, LocalDateTime startDate, LocalDateTime endDate) {
    long clicks = clickRepository.countByTypeAndClickedAtBetween(type, startDate, endDate);
    long impressions = impressionRepository.sumCountByTypeAndPeriod(type, startDate, endDate);

    if (impressions == 0) {
      return 0.0;
    }

    double ctr = (clicks * 100.0) / impressions;
    return Math.round(ctr * 100.0) / 100.0; // 소수점 2자리
  }

  /**
   * 타입별 통계 조회 (클릭, 노출, CTR)
   *
   * @param startDate 시작 날짜
   * @param endDate 종료 날짜
   * @return 타입별 통계 리스트
   */
  @Transactional(readOnly = true)
  public List<RecommendationMetricsDto.TypeStats> getTypeStats(LocalDateTime startDate, LocalDateTime endDate) {
    List<RecommendationMetricsDto.TypeStats> statsList = new ArrayList<>();

    for (RecommendationType type : RecommendationType.values()) {
      long clicks = clickRepository.countByTypeAndClickedAtBetween(type, startDate, endDate);
      long impressions = impressionRepository.sumCountByTypeAndPeriod(type, startDate, endDate);
      double ctr = impressions == 0 ? 0.0 :
              Math.round((clicks * 100.0 / impressions) * 100.0) / 100.0;

      statsList.add(RecommendationMetricsDto.TypeStats.builder()
              .type(type)
              .clicks(clicks)
              .impressions(impressions)
              .ctr(ctr)
              .build());
    }

    return statsList;
  }

  /**
   * 전체 노출 수 조회
   *
   * @param startDate 시작 날짜
   * @param endDate 종료 날짜
   * @return 전체 노출 수
   */
  @Transactional(readOnly = true)
  public long getTotalImpressions(LocalDateTime startDate, LocalDateTime endDate) {
    return impressionRepository.sumCountByPeriod(startDate, endDate);
  }

  /**
   * 타입별 노출 수 조회
   *
   * @param type 추천 타입
   * @param startDate 시작 날짜
   * @param endDate 종료 날짜
   * @return 노출 수
   */
  @Transactional(readOnly = true)
  public long getImpressionsByType(RecommendationType type, LocalDateTime startDate, LocalDateTime endDate) {
    return impressionRepository.sumCountByTypeAndPeriod(type, startDate, endDate);
  }
}
