package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_actions", indexes = {
    @Index(name = "idx_admin_target", columnList = "target_type, target_id"),
    @Index(name = "idx_admin_expires", columnList = "expires_at"),
    @Index(name = "idx_admin_user", columnList = "admin_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminAction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionId;
    
    @Column(name = "admin_id", nullable = false)
    private Long adminId;  // 조치한 관리자 ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TargetType targetType;
    
    @Column(nullable = false)
    private Long targetId;  // 대상 ID (User ID 또는 Post ID)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionType actionType;
    
    @Column(length = 500, nullable = false)
    private String reason;
    
    private LocalDateTime expiresAt;  // 만료 시간 (정지/블록)
    
    @Column(name = "related_report_id")
    private Long relatedReportId;  // 신고 기반 조치인 경우
    
    // Enum 정의
    public enum TargetType {
        USER, POST
    }
    
    public enum ActionType {
        SUSPEND("사용자 정지"),
        UNSUSPEND("정지 해제"), 
        BLOCK("게시글 차단"),
        UNBLOCK("차단 해제");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}