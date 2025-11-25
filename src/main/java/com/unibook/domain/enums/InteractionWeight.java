package com.unibook.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 상호작용 행동별 가중치
 * 다중 행동 추천 시스템에서 각 행동의 중요도를 나타냄
 */
@Getter
@RequiredArgsConstructor
public enum InteractionWeight {

  /**
   * 클릭: 가장 강한 관심 신호 (가중치 1.0)
   * 추천 항목을 실제로 클릭한 경우
   */
  CLICK(1.0, "클릭"),

  /**
   * 찜: 중간 강도 관심 신호 (가중치 0.7)
   * 위시리스트에 추가한 경우
   */
  WISHLIST(0.7, "찜"),

  /**
   * 조회: 약한 관심 신호 (가중치 0.3)
   * 게시글을 열람한 경우
   */
  VIEW(0.3, "조회");

  private final double weight;
  private final String description;

  /**
   * 시간 감쇠가 적용된 가중치 계산
   *
   * @param daysSince 상호작용 이후 경과 일수
   * @param lambda 시간 감쇠 상수 (기본값: 0.1)
   * @param decayThreshold 감쇠 시작 임계값 (일 단위, 기본값: 7일)
   * @return 시간 감쇠가 적용된 가중치
   */
  public double getDecayedWeight(long daysSince, double lambda, int decayThreshold) {
    if (daysSince <= decayThreshold) {
      return weight;
    }
    return weight * Math.exp(-lambda * (daysSince - decayThreshold));
  }
}
