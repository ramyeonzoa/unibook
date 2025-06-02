package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_reporter", columnList = "reporter_id"),
    @Index(name = "idx_report_target_user", columnList = "target_user_id"),
    @Index(name = "idx_report_created", columnList = "created_at"),
    @Index(name = "idx_report_type_target", columnList = "report_type, target_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType reportType;
    
    @Column(nullable = false)
    private Long targetId; // postId 또는 chatRoomId
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser; // 피신고자
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportCategory category;
    
    @Column(columnDefinition = "TEXT")
    private String content; // 상세 신고 내용
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String adminNote; // 관리자 처리 메모
    
    private LocalDateTime processedAt; // 처리 시간
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy; // 처리한 관리자
    
    // 신고 타입
    public enum ReportType {
        POST("게시글"),
        CHAT("채팅"),
        USER("사용자");
        
        private final String description;
        
        ReportType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 신고 카테고리
    public enum ReportCategory {
        SPAM("스팸/도배"),
        FRAUD("사기/허위매물"),
        INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
        ABUSIVE_BEHAVIOR("욕설/비방"),
        ILLEGAL_ITEM("금지 물품"),
        OTHER("기타");
        
        private final String description;
        
        ReportCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 신고 상태
    public enum ReportStatus {
        PENDING("대기중"),
        PROCESSING("처리중"),
        COMPLETED("처리완료"),
        REJECTED("기각");
        
        private final String description;
        
        ReportStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 신고 처리
    public void processReport(User admin, ReportStatus newStatus, String note) {
        this.status = newStatus;
        this.adminNote = note;
        this.processedAt = LocalDateTime.now();
        this.processedBy = admin;
    }
    
    // 동일 신고 여부 확인
    public boolean isSameReport(Long reporterId, ReportType type, Long targetId) {
        return this.reporter.getUserId().equals(reporterId) &&
               this.reportType == type &&
               this.targetId.equals(targetId);
    }
}