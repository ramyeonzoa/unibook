package com.unibook.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 임베딩 캐시 데이터 DTO
 * 파일로 저장/로드되는 캐시 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingCacheDto {

  /**
   * 캐시 버전
   */
  private String version;

  /**
   * FAQ 파일 해시 (SHA-256)
   */
  private String faqHash;

  /**
   * 임베딩 모델명
   */
  private String embeddingModel;

  /**
   * 임베딩 벡터 차원
   */
  private int dimensions;

  /**
   * 생성 시각
   */
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;

  /**
   * 캐시된 임베딩 목록
   */
  private List<CachedEmbedding> embeddings;

  /**
   * 개별 임베딩 데이터
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CachedEmbedding {

    /**
     * FAQ ID
     */
    private String id;

    /**
     * 원본 텍스트 (질문 + 답변)
     */
    private String text;

    /**
     * 임베딩 벡터
     */
    private float[] vector;

    /**
     * 메타데이터
     */
    private Metadata metadata;
  }

  /**
   * 메타데이터
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Metadata {

    /**
     * 카테고리
     */
    private String category;

    /**
     * 질문
     */
    private String question;

    /**
     * 답변
     */
    private String answer;

    /**
     * 앵커 링크
     */
    private String anchors;
  }
}
