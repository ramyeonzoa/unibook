package com.unibook.controller;

import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.Report;
import com.unibook.domain.entity.User;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.service.PostService;
import com.unibook.service.ReportService;
import com.unibook.service.UserService;
import com.unibook.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * 신고 상세 조회
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
}