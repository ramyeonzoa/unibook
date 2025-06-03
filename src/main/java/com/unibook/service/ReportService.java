package com.unibook.service;

import com.unibook.domain.dto.ReportDto;
import com.unibook.domain.entity.*;
import com.unibook.domain.entity.Report.*;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.ChatRoomRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.ReportRepository;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AdminActionService adminActionService;
    
    private static final int DAILY_REPORT_LIMIT = 10; // 일일 신고 제한
    private static final int AUTO_BLIND_THRESHOLD = 3; // 자동 블라인드 기준
    private static final int USER_WARNING_THRESHOLD = 5; // 사용자 경고 기준
    
    /**
     * 신고 접수
     */
    @Transactional
    public Report createReport(Long reporterId, ReportType reportType, Long targetId, 
                             ReportCategory category, String content) {
        log.info("신고 접수 시작: reporter={}, type={}, target={}", reporterId, reportType, targetId);
        
        // 1. 신고자 확인
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        // 2. 일일 신고 제한 확인
        LocalDateTime today = LocalDate.now().atStartOfDay();
        long todayReportCount = reportRepository.countTodayReportsByUser(reporterId, today);
        if (todayReportCount >= DAILY_REPORT_LIMIT) {
            throw new ValidationException("일일 신고 횟수를 초과했습니다. (최대 " + DAILY_REPORT_LIMIT + "건)");
        }
        
        // 3. 중복 신고 확인
        if (reportRepository.existsByReporterUserIdAndReportTypeAndTargetId(reporterId, reportType, targetId)) {
            throw new ValidationException("이미 신고한 대상입니다.");
        }
        
        // 4. 신고 대상 및 피신고자 확인
        User targetUser = null;
        String targetTitle = "";
        
        switch (reportType) {
            case POST:
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
                targetUser = post.getUser();
                targetTitle = post.getTitle();
                
                // 본인 게시글 신고 방지
                if (targetUser.getUserId().equals(reporterId)) {
                    throw new ValidationException("본인의 게시글은 신고할 수 없습니다.");
                }
                break;
                
            case CHAT:
                ChatRoom chatRoom = chatRoomRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
                
                // 채팅방 참여자가 아닌 경우
                if (!chatRoom.getBuyer().getUserId().equals(reporterId) && 
                    !chatRoom.getSeller().getUserId().equals(reporterId)) {
                    throw new ValidationException("해당 채팅방의 참여자가 아닙니다.");
                }
                
                // 상대방을 피신고자로 설정
                targetUser = chatRoom.getBuyer().getUserId().equals(reporterId) ? 
                            chatRoom.getSeller() : chatRoom.getBuyer();
                targetTitle = "채팅 대화";
                break;
                
            case USER:
                targetUser = userRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
                targetTitle = targetUser.getName() + " 사용자";
                
                // 본인 신고 방지
                if (targetUser.getUserId().equals(reporterId)) {
                    throw new ValidationException("본인을 신고할 수 없습니다.");
                }
                break;
        }
        
        // 5. 신고 생성
        Report report = Report.builder()
                .reporter(reporter)
                .reportType(reportType)
                .targetId(targetId)
                .targetUser(targetUser)
                .category(category)
                .content(content)
                .status(ReportStatus.PENDING)
                .build();
        
        Report savedReport = reportRepository.save(report);
        log.info("신고 접수 완료: reportId={}", savedReport.getReportId());
        
        // 6. 자동 블라인드 처리 확인
        checkAutoBlind(reportType, targetId);
        
        // 7. 관리자 알림 (비동기)
        notificationService.notifyAdminsOfNewReport(savedReport, targetTitle);
        
        return savedReport;
    }
    
    /**
     * 자동 블라인드 처리 확인
     */
    private void checkAutoBlind(ReportType reportType, Long targetId) {
        long uniqueReporters = reportRepository.countUniqueReportersForTarget(reportType, targetId);
        
        if (uniqueReporters >= AUTO_BLIND_THRESHOLD) {
            log.warn("자동 블라인드 임계값 도달: type={}, targetId={}, reporters={}", 
                    reportType, targetId, uniqueReporters);
            
            if (reportType == ReportType.POST) {
                // 게시글 자동 숨김 처리
                postRepository.findById(targetId).ifPresent(post -> {
                    post.setStatus(Post.PostStatus.BLOCKED);
                    postRepository.save(post);
                    log.info("게시글 자동 블라인드 처리: postId={}", targetId);
                });
            }
            // TODO: 채팅방이나 사용자에 대한 자동 처리 로직 추가
        }
    }
    
    /**
     * 신고 처리 (관리자)
     */
    @Transactional
    public Report processReport(Long reportId, Long adminId, ReportStatus newStatus, String adminNote) {
        log.info("신고 처리 시작: reportId={}, adminId={}, status={}", reportId, adminId, newStatus);
        
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("신고를 찾을 수 없습니다."));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("관리자를 찾을 수 없습니다."));
        
        // 권한 확인 (관리자인지)
        // TODO: Spring Security 권한 확인 추가
        
        // 이미 처리된 신고인지 확인
        if (report.getStatus() != ReportStatus.PENDING && report.getStatus() != ReportStatus.PROCESSING) {
            throw new ValidationException("이미 처리된 신고입니다.");
        }
        
        // 신고 처리
        report.processReport(admin, newStatus, adminNote);
        Report processedReport = reportRepository.save(report);
        
        // 처리 결과에 따른 추가 조치
        if (newStatus == ReportStatus.COMPLETED) {
            handleReportAction(report);
        }
        
        // 신고자에게 처리 결과 알림
        notificationService.notifyReportProcessed(report.getReporter().getUserId(), reportId, newStatus);
        
        log.info("신고 처리 완료: reportId={}", reportId);
        return processedReport;
    }
    
    /**
     * 신고 처리와 함께 게시글 차단
     */
    @Transactional
    public Report processReportWithPostBlock(Long reportId, Long adminId, String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("신고를 찾을 수 없습니다."));
        
        // 게시글 신고가 아닌 경우 에러
        if (report.getReportType() != ReportType.POST) {
            throw new ValidationException("게시글 신고가 아닙니다.");
        }
        
        // 게시글 차단 처리 (AdminAction과 통합)
        Post post = postRepository.findById(report.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        // AdminActionService를 통해 게시글 차단 (Post 상태 변경 + AdminAction 기록)
        String blockReason = "신고 처리: " + report.getCategory().getDescription() + 
                           (adminNote != null && !adminNote.trim().isEmpty() ? " - " + adminNote : "");
        adminActionService.blockPost(post.getPostId(), blockReason, adminId, reportId);
        
        // 신고 처리 완료
        Report processedReport = processReport(reportId, adminId, ReportStatus.COMPLETED, 
                adminNote + " (게시글 차단됨)");
        
        log.info("신고 처리 및 게시글 차단 완료: reportId={}, postId={}", reportId, post.getPostId());
        
        return processedReport;
    }
    
    /**
     * 신고 처리에 따른 조치
     */
    private void handleReportAction(Report report) {
        // 피신고자의 누적 신고 수 확인
        long userReportCount = reportRepository.countByTargetUserUserIdAndStatus(
                report.getTargetUser().getUserId(), ReportStatus.COMPLETED);
        
        if (userReportCount >= USER_WARNING_THRESHOLD) {
            // TODO: 사용자 제재 처리 (계정 정지 등)
            log.warn("사용자 경고 임계값 도달: userId={}, reportCount={}", 
                    report.getTargetUser().getUserId(), userReportCount);
        }
        
        // 신고 대상별 추가 처리
        switch (report.getReportType()) {
            case POST:
                // 게시글 삭제/숨김 처리는 관리자가 수동으로
                break;
            case CHAT:
                // 채팅방 차단 등의 처리
                break;
            case USER:
                // 사용자 제재 처리
                break;
        }
    }
    
    /**
     * 신고 목록 조회 (관리자)
     */
    public Page<Report> getReports(ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return reportRepository.findAllWithUsers(pageable);
    }
    
    /**
     * 신고 상세 조회
     */
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("신고를 찾을 수 없습니다."));
    }
    
    /**
     * 사용자의 신고 내역 조회
     */
    public Page<Report> getUserReports(Long userId, Pageable pageable) {
        return reportRepository.findByReporterUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * 신고 통계 조회
     */
    public List<Object[]> getReportStatsByCategory(LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.getReportStatsByCategory(startDate, endDate);
    }
    
    /**
     * 가장 많이 신고된 사용자 목록
     */
    public List<Object[]> getMostReportedUsers(Pageable pageable) {
        return reportRepository.getMostReportedUsers(ReportStatus.COMPLETED, pageable);
    }
    
    /**
     * 대기 중인 신고 수
     */
    public long getPendingReportsCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }
    
    /**
     * 중복 신고 여부 확인 (기존 메서드 - 호환성 유지)
     */
    public boolean isDuplicateReport(Long reporterId, ReportType reportType, Long targetId) {
        return reportRepository.existsByReporterUserIdAndReportTypeAndTargetId(
                reporterId, reportType, targetId);
    }
    
    /**
     * 신고 가능 여부 종합 확인 (중복 + 일일 제한)
     */
    public ReportDto.CheckResponse checkReportEligibility(Long reporterId, ReportType reportType, Long targetId) {
        // 1. 중복 신고 확인
        boolean isDuplicate = reportRepository.existsByReporterUserIdAndReportTypeAndTargetId(
                reporterId, reportType, targetId);
        
        if (isDuplicate) {
            return ReportDto.CheckResponse.createDuplicate();
        }
        
        // 2. 일일 신고 제한 확인
        LocalDateTime today = LocalDate.now().atStartOfDay();
        long todayReportCount = reportRepository.countTodayReportsByUser(reporterId, today);
        
        if (todayReportCount >= DAILY_REPORT_LIMIT) {
            return ReportDto.CheckResponse.createOverDailyLimit((int) todayReportCount, DAILY_REPORT_LIMIT);
        }
        
        // 3. 신고 가능
        ReportDto.CheckResponse response = ReportDto.CheckResponse.createCanReport();
        response.setTodayReportCount((int) todayReportCount);
        response.setDailyLimit(DAILY_REPORT_LIMIT);
        return response;
    }
}