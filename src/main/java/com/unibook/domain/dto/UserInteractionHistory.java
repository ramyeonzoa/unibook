package com.unibook.domain.dto;

import com.unibook.domain.enums.InteractionWeight;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 상호작용 이력 DTO
 * 다중 행동 추천 시스템에서 사용자의 클릭, 찜, 조회 이력을 통합 관리
 */
@Getter
@Builder
public class UserInteractionHistory {

  @Builder.Default
  private List<InteractionRecord> clicks = new ArrayList<>();

  @Builder.Default
  private List<InteractionRecord> wishlists = new ArrayList<>();

  @Builder.Default
  private List<InteractionRecord> views = new ArrayList<>();

  /**
   * 모든 상호작용 레코드를 하나의 리스트로 반환
   */
  public List<InteractionRecord> getAllInteractions() {
    List<InteractionRecord> all = new ArrayList<>();
    all.addAll(clicks);
    all.addAll(wishlists);
    all.addAll(views);
    return all;
  }

  /**
   * 총 상호작용 수 반환
   */
  public int getTotalCount() {
    return clicks.size() + wishlists.size() + views.size();
  }

  /**
   * 상호작용 레코드
   */
  @Getter
  @Builder
  public static class InteractionRecord {

    private Long postId;
    private LocalDateTime timestamp;
    private InteractionWeight weight;

    /**
     * 시간 감쇠가 적용된 가중치 계산
     *
     * @param lambda 시간 감쇠 상수
     * @param thresholdDays 감쇠 시작 임계값 (일 단위)
     * @param currentTime 현재 시각
     * @return 시간 감쇠가 적용된 가중치
     */
    public double getDecayedWeight(double lambda, int thresholdDays, LocalDateTime currentTime) {
      long daysSince = ChronoUnit.DAYS.between(timestamp, currentTime);
      return weight.getDecayedWeight(daysSince, lambda, thresholdDays);
    }

    /**
     * 시간 감쇠가 적용된 가중치 계산 (현재 시각 기준)
     */
    public double getDecayedWeight(double lambda, int thresholdDays) {
      return getDecayedWeight(lambda, thresholdDays, LocalDateTime.now());
    }

    /**
     * 원본 가중치 반환
     */
    public double getBaseWeight() {
      return weight.getWeight();
    }
  }
}
