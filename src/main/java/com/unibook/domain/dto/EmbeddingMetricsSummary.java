package com.unibook.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 임베딩 메트릭 요약 통계 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingMetricsSummary {

  /**
   * 총 로드 횟수
   */
  private long totalLoads;

  /**
   * 캐시 히트 횟수
   */
  private long cacheHits;

  /**
   * 캐시 미스 횟수
   */
  private long cacheMisses;

  /**
   * 캐시 히트율 (%)
   */
  private double cacheHitRate;

  /**
   * 총 비용 (USD)
   */
  private double totalCostUsd;

  /**
   * 평균 로딩 시간 (ms)
   */
  private double avgLoadingTimeMs;

  /**
   * 캐시 히트 시 평균 로딩 시간 (ms)
   */
  private double avgLoadingTimeMsWithCache;

  /**
   * 캐시 미스 시 평균 로딩 시간 (ms)
   */
  private double avgLoadingTimeMsWithoutCache;

  /**
   * 절약한 비용 (USD)
   */
  private double savedCostUsd;
}
