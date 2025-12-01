package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 추천 노출 기록 엔티티
 * - 추천 시스템 성능 측정을 위한 노출(Impression) 추적
 * - CTR(Click-Through Rate) 계산에 활용
 * - 추천이 사용자에게 표시된 것을 기록
 */
@Entity
@Table(name = "recommendation_impressions", indexes = {
  @Index(name = "idx_rec_imp_user_id", columnList = "user_id"),
  @Index(name = "idx_rec_imp_session_id", columnList = "session_id"),
  @Index(name = "idx_rec_imp_type", columnList = "type"),
  @Index(name = "idx_rec_imp_impressed_at", columnList = "impressed_at"),
  @Index(name = "idx_rec_imp_session_type_date", columnList = "session_id, type, impressed_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RecommendationImpression {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long impressionId;

  /**
   * 노출된 사용자 (비로그인 사용자는 NULL)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private User user;

  /**
   * 세션 ID (중복 제거 및 비로그인 사용자 추적용)
   */
  @Column(name = "session_id", nullable = false, length = 100)
  private String sessionId;

  /**
   * 추천 타입 (맞춤 추천 / 비슷한 게시글)
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RecommendationClick.RecommendationType type;

  /**
   * 노출된 추천 개수
   */
  @Column(nullable = false)
  private Integer count;

  /**
   * 노출 시각
   */
  @Column(name = "impressed_at", nullable = false)
  private LocalDateTime impressedAt;

  /**
   * 페이지 타입 (main, detail, list 등)
   */
  @Column(name = "page_type", length = 50)
  private String pageType;

  /**
   * 추천 기준 게시글 ID (SIMILAR 타입일 경우만)
   */
  @Column(name = "source_post_id")
  private Long sourcePostId;

  @PrePersist
  protected void onCreate() {
    if (impressedAt == null) {
      impressedAt = LocalDateTime.now();
    }
  }
}
