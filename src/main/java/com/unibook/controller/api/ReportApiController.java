package com.unibook.controller.api;

import com.unibook.controller.dto.ApiResponse;
import com.unibook.domain.dto.ReportDto;
import com.unibook.domain.entity.Report;
import com.unibook.domain.entity.Report.ReportStatus;
import com.unibook.security.UserPrincipal;
import com.unibook.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportApiController {
    
    private final ReportService reportService;
    
    /**
     * 신고 접수
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportDto.Response>> createReport(
            @Valid @RequestBody ReportDto.Request request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            log.info("신고 접수 요청: userId={}, reportType={}, targetId={}, category={}", 
                    userPrincipal.getUserId(), request.getReportType(), request.getTargetId(), request.getCategory());
            
            Report report = reportService.createReport(
                    userPrincipal.getUserId(),
                    request.getReportType(),
                    request.getTargetId(),
                    request.getCategory(),
                    request.getContent()
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                    "신고가 접수되었습니다.",
                    ReportDto.Response.from(report)
            ));
            
        } catch (Exception e) {
            log.error("신고 접수 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 신고 목록 조회 (관리자용)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReportDto.ListResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportService.getReports(status, pageable);
        
        Page<ReportDto.ListResponse> response = reports.map(ReportDto.ListResponse::from);
        
        return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공", response));
    }
    
    /**
     * 신고 상세 조회 (관리자용)
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportDto.Response>> getReport(@PathVariable Long reportId) {
        try {
            Report report = reportService.getReportById(reportId);
            return ResponseEntity.ok(ApiResponse.success("신고 상세 조회 성공", ReportDto.Response.from(report)));
        } catch (Exception e) {
            log.error("신고 조회 실패: reportId={}", reportId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 신고 처리 (관리자용)
     */
    @PutMapping("/{reportId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportDto.Response>> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportDto.ProcessRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {
        
        try {
            Report processedReport;
            
            if (Boolean.TRUE.equals(request.getBlockPost())) {
                // 게시글 차단과 신고 처리를 함께 수행
                processedReport = reportService.processReportWithPostBlock(
                        reportId,
                        adminPrincipal.getUserId(),
                        request.getAdminNote()
                );
            } else {
                processedReport = reportService.processReport(
                        reportId,
                        adminPrincipal.getUserId(),
                        request.getStatus(),
                        request.getAdminNote()
                );
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                    "신고가 처리되었습니다.",
                    ReportDto.Response.from(processedReport)
            ));
            
        } catch (Exception e) {
            log.error("신고 처리 실패: reportId={}", reportId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 내 신고 내역 조회
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ReportDto.ListResponse>>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportService.getUserReports(userPrincipal.getUserId(), pageable);
        
        Page<ReportDto.ListResponse> response = reports.map(ReportDto.ListResponse::from);
        
        return ResponseEntity.ok(ApiResponse.success("내 신고 내역 조회 성공", response));
    }
    
    /**
     * 신고 카테고리 목록 조회
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryInfo>>> getCategories() {
        List<CategoryInfo> categories = Arrays.stream(Report.ReportCategory.values())
                .map(category -> new CategoryInfo(category.name(), category.getDescription()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("신고 카테고리 조회 성공", categories));
    }
    
    /**
     * 대기 중인 신고 수 조회 (관리자용)
     */
    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getPendingCount() {
        long count = reportService.getPendingReportsCount();
        return ResponseEntity.ok(ApiResponse.success("대기 중인 신고 수 조회 성공", count));
    }
    
    /**
     * 신고 가능 여부 종합 확인 (중복 + 일일 제한)
     */
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportDto.CheckResponse>> checkReportEligibility(
            @RequestParam String reportType,
            @RequestParam Long targetId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            Report.ReportType type = Report.ReportType.valueOf(reportType);
            ReportDto.CheckResponse checkResult = reportService.checkReportEligibility(
                    userPrincipal.getUserId(), type, targetId);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "신고 가능 여부 확인 성공", 
                    checkResult
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("올바르지 않은 신고 타입입니다."));
        } catch (Exception e) {
            log.error("신고 가능 여부 확인 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("신고 가능 여부 확인에 실패했습니다."));
        }
    }
    
    // 카테고리 정보 DTO
    @lombok.Data
    @lombok.AllArgsConstructor
    static class CategoryInfo {
        private String value;
        private String description;
    }
}