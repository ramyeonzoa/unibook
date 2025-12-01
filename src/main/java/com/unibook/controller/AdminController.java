package com.unibook.controller;

import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.Report;
import com.unibook.domain.entity.User;
import com.unibook.domain.entity.RecommendationClick;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.dto.EmbeddingMetricsSummary;
import com.unibook.domain.dto.EvaluationResult;
import com.unibook.domain.dto.RecommendationMetricsDto;
import com.unibook.service.PostService;
import com.unibook.service.ReportService;
import com.unibook.service.UserService;
import com.unibook.service.EmbeddingMetricsLogger;
import com.unibook.service.ChatbotEvaluationService;
import com.unibook.service.RecommendationMetricsService;
import com.unibook.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ReportService reportService;
    private final UserService userService;
    private final PostService postService;
    private final EmbeddingMetricsLogger metricsLogger;
    private final ChatbotEvaluationService chatbotEvaluationService;
    private final RecommendationMetricsService recommendationMetricsService;
    
    /**
     * 관리자 대시보드 메인
     */
    @GetMapping
    public String dashboard(Model model, @AuthenticationPrincipal UserPrincipal adminPrincipal) {
        log.info("관리자 대시보드 접근: adminId={}", adminPrincipal.getUserId());
        
        // 대시보드 통계 정보
        long pendingReports = reportService.getPendingReportsCount();
        
        // 최근 7일간 통계 (임시로 간단한 카운트만)
        // TODO: 실제 통계 서비스 구현 후 개선
        long totalUsers = userService.getTotalUserCount();
        long totalPosts = postService.getTotalPostCount();
        
        // 최근 신고 목록 (5개)
        Pageable recentReportsPageable = PageRequest.of(0, 5);
        Page<Report> recentReports = reportService.getReports(null, recentReportsPageable);
        
        model.addAttribute("pendingReports", pendingReports);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("recentReports", recentReports.getContent());
        
        return "admin/dashboard";
    }
    
    /**
     * 신고 관리 페이지
     */
    @GetMapping("/reports")
    public String reports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Report.ReportStatus status,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportService.getReports(status, pageable);
        
        model.addAttribute("reports", reports);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", Report.ReportStatus.values());
        
        return "admin/reports";
    }
    
    /**
     * 신고 상세 조회 (기존 페이지)
     */
    @GetMapping("/reports/{reportId}")
    public String reportDetail(@PathVariable Long reportId, Model model) {
        try {
            Report report = reportService.getReportById(reportId);
            model.addAttribute("report", report);
            return "admin/report-detail";
        } catch (Exception e) {
            log.error("신고 상세 조회 실패: reportId={}", reportId, e);
            return "redirect:/admin/reports?error=notfound";
        }
    }
    
    /**
     * 신고 상세 조회 (Modal용 API)
     */
    @GetMapping("/api/reports/{reportId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportDetail(@PathVariable Long reportId) {
        try {
            Report report = reportService.getReportDetailWithContent(reportId);
            
            // 순환 참조 방지를 위해 필요한 데이터만 Map으로 변환
            Map<String, Object> reportData = Map.of(
                "reportId", report.getReportId(),
                "reportType", report.getReportType().name(),
                "targetId", report.getTargetId(),
                "category", Map.of(
                    "name", report.getCategory().name(),
                    "description", report.getCategory().getDescription()
                ),
                "content", report.getContent() != null ? report.getContent() : "",
                "status", report.getStatus().name(),
                "createdAt", report.getCreatedAt().toString(),
                "reporter", Map.of(
                    "userId", report.getReporter().getUserId(),
                    "name", report.getReporter().getName(),
                    "email", report.getReporter().getEmail()
                ),
                "targetUser", Map.of(
                    "userId", report.getTargetUser().getUserId(),
                    "name", report.getTargetUser().getName(),
                    "email", report.getTargetUser().getEmail()
                )
            );
            
            Map<String, Object> response = Map.of(
                "success", true,
                "report", reportData
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("신고 상세 조회 실패: reportId={}", reportId, e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "신고 정보를 찾을 수 없습니다."
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 사용자 관리 페이지
     */
    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;
        
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search.trim(), pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        
        return "admin/users";
    }
    
    /**
     * 게시글 관리 페이지
     */
    @GetMapping("/posts")
    public String posts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Post.PostStatus postStatus = null;
        
        if (status != null && !status.isEmpty()) {
            try {
                postStatus = Post.PostStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid post status: {}", status);
            }
        }
        
        Page<PostResponseDto> posts = postService.searchPostsForAdmin(search, postStatus, pageable);
        Map<String, Long> statusStats = postService.getPostStatusStats();
        
        model.addAttribute("posts", posts);
        model.addAttribute("search", search);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statusStats", statusStats);
        
        return "admin/posts";
    }
    
    /**
     * 통계 페이지
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        // 기본 통계
        long totalUsers = userService.getTotalUserCount();
        long totalPosts = postService.getTotalPostCount();
        long totalReports = reportService.getReports(null, PageRequest.of(0, 1)).getTotalElements();
        long pendingReports = reportService.getPendingReportsCount();
        
        // 게시글 상태별 통계
        Map<String, Long> postStatusStats = postService.getPostStatusStats();
        
        // 최근 7일간의 통계 (추후 구현)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        
        // 월별 가입자 수 (추후 구현)
        // 월별 게시글 수 (추후 구현)
        // 인기 카테고리 (추후 구현)
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("totalReports", totalReports);
        model.addAttribute("pendingReports", pendingReports);
        model.addAttribute("postStatusStats", postStatusStats);
        
        return "admin/statistics";
    }
    
    /**
     * 캐시 성능 모니터링 페이지
     */
    @GetMapping("/cache-stats")
    public String cacheStats() {
        return "admin/cache-stats";
    }

    /**
     * 임베딩 메트릭 요약 통계 조회 (API)
     */
    @GetMapping("/api/embedding-metrics/summary")
    @ResponseBody
    public ResponseEntity<EmbeddingMetricsSummary> getEmbeddingMetricsSummary() {
        try {
            EmbeddingMetricsSummary summary = metricsLogger.getSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("임베딩 메트릭 요약 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 임베딩 메트릭 CSV 다운로드 (API)
     */
    @GetMapping("/api/embedding-metrics/export")
    public ResponseEntity<?> exportEmbeddingMetrics() {
        try {
            java.nio.file.Path csvPath = java.nio.file.Paths.get("data/embedding-metrics.csv");

            if (!java.nio.file.Files.exists(csvPath)) {
                return ResponseEntity.notFound().build();
            }

            org.springframework.core.io.Resource resource =
                new org.springframework.core.io.FileSystemResource(csvPath);

            return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=embedding-metrics.csv")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                    "text/csv; charset=UTF-8")
                .body(resource);

        } catch (Exception e) {
            log.error("임베딩 메트릭 CSV 다운로드 실패", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "CSV 다운로드 실패: " + e.getMessage()));
        }
    }

    /**
     * 챗봇 평가 실행 (API)
     */
    @GetMapping("/api/chatbot/evaluate")
    @ResponseBody
    public ResponseEntity<EvaluationResult> evaluateChatbot() {
        try {
            log.info("챗봇 평가 시작 (관리자 요청)");
            EvaluationResult result = chatbotEvaluationService.evaluate();
            log.info("챗봇 평가 완료: 정확도={}%, 키워드 커버리지={}%",
                String.format("%.1f", result.getAccuracy() * 100),
                String.format("%.1f", result.getKeywordCoverage() * 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("챗봇 평가 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 추천 시스템 메트릭 페이지
     */
    @GetMapping("/recommendations")
    public String recommendations(
            @RequestParam(defaultValue = "7") int days,
            Model model) {
        log.info("추천 메트릭 페이지 접근: days={}", days);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        // 기본 메트릭
        RecommendationMetricsDto.Response metrics =
                recommendationMetricsService.getMetrics(startDate, endDate);

        // CTR 관련 메트릭 추가
        long totalImpressions = recommendationMetricsService.getTotalImpressions(startDate, endDate);
        double overallCTR = recommendationMetricsService.calculateCTR(startDate, endDate);

        // 타입별 통계
        List<RecommendationMetricsDto.TypeStats> typeStats =
                recommendationMetricsService.getTypeStats(startDate, endDate);

        // 타입별 통계를 각각 분리해서 템플릿으로 보냄
        RecommendationMetricsDto.TypeStats forYouStats = typeStats.stream()
                .filter(ts -> ts.getType() == RecommendationClick.RecommendationType.FOR_YOU)
                .findFirst()
                .orElse(null);

        RecommendationMetricsDto.TypeStats similarStats = typeStats.stream()
                .filter(ts -> ts.getType() == RecommendationClick.RecommendationType.SIMILAR)
                .findFirst()
                .orElse(null);

        model.addAttribute("metrics", metrics);
        model.addAttribute("totalImpressions", totalImpressions);
        model.addAttribute("overallCTR", overallCTR);
        model.addAttribute("typeStats", typeStats);
        model.addAttribute("forYouStats", forYouStats);
        model.addAttribute("similarStats", similarStats);
        model.addAttribute("days", days);

        return "admin/recommendations";
    }

    /**
     * 추천 시스템 메트릭 API (AJAX용)
     */
    @GetMapping("/api/recommendations/metrics")
    @ResponseBody
    public ResponseEntity<RecommendationMetricsDto.Response> getRecommendationMetricsApi(
            @RequestParam(defaultValue = "7") int days) {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);

            RecommendationMetricsDto.Response metrics =
                    recommendationMetricsService.getMetrics(startDate, endDate);

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("추천 메트릭 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
