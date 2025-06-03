package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * 알림 Entity
 * 사용자별 알림 관리 (찜 상태 변경, 게시글 찜됨, 메시지 등)
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_recipient_read", columnList = "recipient_user_id, is_read"),
    @Index(name = "idx_recipient_created", columnList = "recipient_user_id, created_at")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@ToString(exclude = {"recipient", "actor", "relatedPost"})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    /**
     * 알림 받는 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    /**
     * 알림을 발생시킨 사용자 (nullable - 시스템 알림의 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * 관련 게시글 (nullable - 전체 공지사항 등의 경우)
     * 게시글 삭제 시 관련 알림도 자동 삭제되도록 외래키 제약 설정
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_post_id", foreignKey = @ForeignKey(name = "fk_notification_post"))
    private Post relatedPost;

    /**
     * 알림 제목
     */
    @Column(length = 200)
    private String title;

    /**
     * 알림 내용
     */
    @Column(length = 1000)
    private String content;

    /**
     * 연결 URL (클릭 시 이동할 경로)
     */
    @Column(length = 500)
    private String url;

    /**
     * 추가 데이터 (JSON 형태로 저장)
     * 향후 확장을 위한 유연한 페이로드
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    /**
     * 읽음 여부 (primitive boolean 사용)
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isRead = false;

    /**
     * 알림 타입 Enum
     */
    public enum NotificationType {
        WISHLIST_STATUS_CHANGED,  // 찜한 게시글 상태 변경
        POST_WISHLISTED,          // 내 게시글이 찜됨
        NEW_MESSAGE,              // 새 채팅 메시지
        REPORT_PROCESSED,         // 신고 처리 완료
        KEYWORD_MATCH,            // 키워드 매칭 게시글 등록
        SYSTEM_NOTICE             // 시스템 공지사항 (향후)
    }

    /**
     * 알림을 읽음으로 표시
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 페이로드에 데이터 추가
     */
    public void addPayload(String key, Object value) {
        if (this.payload == null) {
            this.payload = new HashMap<>();
        }
        this.payload.put(key, value);
    }

    /**
     * 페이로드에서 데이터 조회
     */
    public Object getPayload(String key) {
        return this.payload != null ? this.payload.get(key) : null;
    }
}