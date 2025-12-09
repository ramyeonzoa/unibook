package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 추천 클릭 기록 엔티티
 * - 추천 시스템 성능 측정을 위한 클릭 추적
 * - CTR(Click-Through Rate) 계산에 활용
 */
@Entity
@Table(name = "recommendation_clicks", indexes = {
    @Index(name = "idx_rec_click_user_id", columnList = "user_id"),
    @Index(name = "idx_rec_click_post_id", columnList = "post_id"),
    @Index(name = "idx_rec_click_type", columnList = "type"),
    @Index(name = "idx_rec_click_clicked_at", columnList = "clicked_at"),
    @Index(name = "idx_rec_click_type_position", columnList = "type, position")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RecommendationClick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clickId;

    /**
     * 클릭한 사용자 (비로그인 사용자는 NULL)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    /**
     * 클릭된 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    /**
     * 추천 타입 (맞춤 추천 / 비슷한 게시글)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationType type;

    /**
     * 추천 목록 내 위치 (0부터 시작)
     */
    @Column(name = "position")
    private Integer position;

    /**
     * 클릭 시각
     */
    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    /**
     * 추천 기준 게시글 ID (SIMILAR 타입일 경우만)
     */
    @Column(name = "source_post_id")
    private Long sourcePostId;

    /**
     * 추천 슬롯/소스 라벨 (personalized, popular, fresh, explore 등)
     */
    @Column(name = "source_label", length = 30)
    private String sourceLabel;

    @PrePersist
    protected void onCreate() {
        if (clickedAt == null) {
            clickedAt = LocalDateTime.now();
        }
    }

    /**
     * 추천 타입
     */
    public enum RecommendationType {
        FOR_YOU("맞춤 추천"),
        SIMILAR("비슷한 게시글");

        private final String description;

        RecommendationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
