package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 게시글 조회 기록 엔티티
 * - 추천 시스템을 위한 사용자 행동 데이터 수집
 * - Content-based 및 Collaborative Filtering에 활용
 */
@Entity
@Table(name = "post_views", indexes = {
    @Index(name = "idx_post_views_user_id", columnList = "user_id"),
    @Index(name = "idx_post_views_post_id", columnList = "post_id"),
    @Index(name = "idx_post_views_viewed_at", columnList = "viewed_at"),
    @Index(name = "idx_post_views_user_viewed", columnList = "user_id, viewed_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PostView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long viewId;

    /**
     * 조회한 사용자 (비로그인 사용자는 NULL)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    /**
     * 조회된 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    /**
     * 조회 시각
     */
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }
}
