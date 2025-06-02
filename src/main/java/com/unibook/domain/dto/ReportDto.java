package com.unibook.domain.dto;

import com.unibook.domain.entity.Report;
import com.unibook.domain.entity.Report.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class ReportDto {
    
    /**
     * 신고 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull(message = "신고 타입은 필수입니다")
        private ReportType reportType;
        
        @NotNull(message = "신고 대상 ID는 필수입니다")
        private Long targetId;
        
        @NotNull(message = "신고 카테고리는 필수입니다")
        private ReportCategory category;
        
        @Size(max = 1000, message = "신고 내용은 1000자 이하여야 합니다")
        private String content;
    }
    
    /**
     * 신고 처리 요청 DTO (관리자용)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessRequest {
        @NotNull(message = "처리 상태는 필수입니다")
        private ReportStatus status;
        
        @Size(max = 1000, message = "관리자 메모는 1000자 이하여야 합니다")
        private String adminNote;
        
        private Boolean blockPost;  // 게시글 차단 여부
    }
    
    /**
     * 신고 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long reportId;
        private ReportType reportType;
        private Long targetId;
        private String targetTitle; // 게시글 제목 또는 채팅방 정보
        private ReportCategory category;
        private String categoryDescription;
        private String content;
        private ReportStatus status;
        private String statusDescription;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
        
        // 신고자 정보
        private Long reporterId;
        private String reporterName;
        
        // 피신고자 정보
        private Long targetUserId;
        private String targetUserName;
        
        // 관리자 정보
        private String adminNote;
        private String processedByName;
        
        public static Response from(Report report) {
            return Response.builder()
                    .reportId(report.getReportId())
                    .reportType(report.getReportType())
                    .targetId(report.getTargetId())
                    .category(report.getCategory())
                    .categoryDescription(report.getCategory().getDescription())
                    .content(report.getContent())
                    .status(report.getStatus())
                    .statusDescription(report.getStatus().getDescription())
                    .createdAt(report.getCreatedAt())
                    .processedAt(report.getProcessedAt())
                    .reporterId(report.getReporter().getUserId())
                    .reporterName(report.getReporter().getName())
                    .targetUserId(report.getTargetUser().getUserId())
                    .targetUserName(report.getTargetUser().getName())
                    .adminNote(report.getAdminNote())
                    .processedByName(report.getProcessedBy() != null ? 
                            report.getProcessedBy().getName() : null)
                    .build();
        }
        
        public static Response from(Report report, String targetTitle) {
            Response response = from(report);
            response.setTargetTitle(targetTitle);
            return response;
        }
    }
    
    /**
     * 신고 목록 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private Long reportId;
        private ReportType reportType;
        private String reportTypeDescription;
        private ReportCategory category;
        private String categoryDescription;
        private ReportStatus status;
        private String statusDescription;
        private LocalDateTime createdAt;
        
        // 신고자 정보
        private String reporterName;
        
        // 피신고자 정보
        private String targetUserName;
        
        public static ListResponse from(Report report) {
            return ListResponse.builder()
                    .reportId(report.getReportId())
                    .reportType(report.getReportType())
                    .reportTypeDescription(report.getReportType().getDescription())
                    .category(report.getCategory())
                    .categoryDescription(report.getCategory().getDescription())
                    .status(report.getStatus())
                    .statusDescription(report.getStatus().getDescription())
                    .createdAt(report.getCreatedAt())
                    .reporterName(report.getReporter().getName())
                    .targetUserName(report.getTargetUser().getName())
                    .build();
        }
    }
    
    /**
     * 신고 가능 여부 확인 응답 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckResponse {
        private boolean canReport;        // 신고 가능 여부
        private boolean isDuplicate;      // 중복 신고 여부
        private boolean isOverDailyLimit; // 일일 제한 초과 여부
        private int todayReportCount;     // 오늘 신고 횟수
        private int dailyLimit;           // 일일 제한 횟수
        private String message;           // 사용자에게 표시할 메시지
        
        public static CheckResponse createCanReport() {
            return CheckResponse.builder()
                    .canReport(true)
                    .isDuplicate(false)
                    .isOverDailyLimit(false)
                    .message("신고 가능")
                    .build();
        }
        
        public static CheckResponse createDuplicate() {
            return CheckResponse.builder()
                    .canReport(false)
                    .isDuplicate(true)
                    .isOverDailyLimit(false)
                    .message("이미 신고한 대상입니다.")
                    .build();
        }
        
        public static CheckResponse createOverDailyLimit(int todayCount, int dailyLimit) {
            return CheckResponse.builder()
                    .canReport(false)
                    .isDuplicate(false)
                    .isOverDailyLimit(true)
                    .todayReportCount(todayCount)
                    .dailyLimit(dailyLimit)
                    .message("일일 신고 횟수를 초과했습니다. (오늘 " + todayCount + "/" + dailyLimit + "건)")
                    .build();
        }
    }
}