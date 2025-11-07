package com.unibook.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 추천 시스템 가중치 DTO
 * - 데이터 상황에 따라 적응형으로 조정됨
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationWeights {

    /**
     * Content-based 가중치 (같은 책/과목/학과 기반)
     */
    private double content;

    /**
     * Collaborative filtering 가중치 (사용자 행동 기반)
     */
    private double collaborative;

    /**
     * 인기도 가중치 (조회수/찜 기반)
     */
    private double popularity;

    /**
     * 최신성 가중치 (등록일 기반)
     */
    private double recency;

    /**
     * 추천 전략 (데이터 상황 설명)
     */
    private String strategy;

    /**
     * 기본 가중치 (초기 상태: Content-based 위주)
     */
    public static RecommendationWeights getDefault() {
        return RecommendationWeights.builder()
                .content(0.90)
                .collaborative(0.10)
                .popularity(0.00)
                .recency(0.00)
                .strategy("content-heavy")
                .build();
    }

    /**
     * 데이터 충분 시 가중치 (균형잡힌 하이브리드)
     */
    public static RecommendationWeights getBalanced() {
        return RecommendationWeights.builder()
                .content(0.50)
                .collaborative(0.50)
                .popularity(0.00)
                .recency(0.00)
                .strategy("balanced-hybrid")
                .build();
    }

    /**
     * 중간 단계 가중치
     */
    public static RecommendationWeights getIntermediate() {
        return RecommendationWeights.builder()
                .content(0.70)
                .collaborative(0.30)
                .popularity(0.00)
                .recency(0.00)
                .strategy("content-collaborative-mix")
                .build();
    }
}
