package com.unibook.domain.dto;

import com.unibook.domain.entity.AdminAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminActionDto {
    private Long actionId;
    private Long adminId;
    private String adminName;
    private String targetType;
    private Long targetId;
    private String targetName; // User name or Post title
    private String actionType;
    private String actionDescription;
    private String reason;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long relatedReportId;
    
    public static AdminActionDto from(AdminAction action) {
        return AdminActionDto.builder()
            .actionId(action.getActionId())
            .adminId(action.getAdminId())
            .targetType(action.getTargetType().name())
            .targetId(action.getTargetId())
            .actionType(action.getActionType().name())
            .actionDescription(action.getActionType().getDescription())
            .reason(action.getReason())
            .expiresAt(action.getExpiresAt())
            .createdAt(action.getCreatedAt())
            .relatedReportId(action.getRelatedReportId())
            .build();
    }
    
    public static AdminActionDto fromWithDetails(AdminAction action, String adminName, String targetName) {
        AdminActionDto dto = from(action);
        dto.setAdminName(adminName);
        dto.setTargetName(targetName);
        return dto;
    }
}