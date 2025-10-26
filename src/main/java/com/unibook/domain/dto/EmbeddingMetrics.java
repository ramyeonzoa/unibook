package com.unibook.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 임베딩 로딩 메트릭 DTO
 * 성능 및 비용 추적용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingMetrics {

  /**
   * 측정 시각
   */
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  /**
   * 캐시 히트 여부
   */
  private boolean cacheHit;

  /**
   * 로딩 시간 (밀리초)
   */
  private long loadingTimeMs;

  /**
   * API 호출 횟수
   */
  private int apiCalls;

  /**
   * 총 토큰 수
   */
  private int totalTokens;

  /**
   * 예상 비용 (USD)
   */
  private double estimatedCostUsd;

  /**
   * 임베딩 개수
   */
  private int embeddingCount;

  /**
   * 임베딩 모델명
   */
  private String embeddingModel;

  /**
   * FAQ 해시값
   */
  private String faqHash;

  /**
   * 캐시 소스 (api, file, error)
   */
  private String cacheSource;
}
