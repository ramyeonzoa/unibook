package com.unibook.repository;

import com.unibook.domain.entity.Report;
import com.unibook.domain.entity.Report.ReportStatus;
import com.unibook.domain.entity.Report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    // 중복 신고 확인
    boolean existsByReporterUserIdAndReportTypeAndTargetId(Long reporterId, ReportType reportType, Long targetId);
    
    // 특정 대상에 대한 신고 수 조회
    long countByReportTypeAndTargetIdAndStatus(ReportType reportType, Long targetId, ReportStatus status);
    
    // 특정 사용자가 받은 신고 수 조회
    long countByTargetUserUserIdAndStatus(Long targetUserId, ReportStatus status);
    
    // 상태별 신고 목록 조회 (관리자용)
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
    
    // 모든 신고 목록 조회 (관리자용)
    @Query("SELECT r FROM Report r " +
           "LEFT JOIN FETCH r.reporter " +
           "LEFT JOIN FETCH r.targetUser " +
           "ORDER BY r.createdAt DESC")
    Page<Report> findAllWithUsers(Pageable pageable);
    
    // 특정 기간 내 신고 통계
    @Query("SELECT r.category, COUNT(r) FROM Report r " +
           "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY r.category")
    List<Object[]> getReportStatsByCategory(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // 가장 많이 신고된 사용자 목록
    @Query("SELECT r.targetUser, COUNT(r) as reportCount FROM Report r " +
           "WHERE r.status = :status " +
           "GROUP BY r.targetUser " +
           "ORDER BY reportCount DESC")
    List<Object[]> getMostReportedUsers(@Param("status") ReportStatus status, Pageable pageable);
    
    // 특정 게시글에 대한 신고 목록
    List<Report> findByReportTypeAndTargetIdOrderByCreatedAtDesc(ReportType reportType, Long targetId);
    
    // 오늘 특정 사용자가 한 신고 수 (신고 제한용)
    @Query("SELECT COUNT(r) FROM Report r " +
           "WHERE r.reporter.userId = :reporterId " +
           "AND r.createdAt >= :today")
    long countTodayReportsByUser(@Param("reporterId") Long reporterId, 
                                @Param("today") LocalDateTime today);
    
    // 처리되지 않은 신고 수
    long countByStatus(ReportStatus status);
    
    // 특정 사용자의 신고 내역
    Page<Report> findByReporterUserIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);
    
    // 자동 블라인드를 위한 신고 수 확인
    @Query("SELECT COUNT(DISTINCT r.reporter.userId) FROM Report r " +
           "WHERE r.reportType = :type " +
           "AND r.targetId = :targetId " +
           "AND r.status != 'REJECTED'")
    long countUniqueReportersForTarget(@Param("type") ReportType type, 
                                      @Param("targetId") Long targetId);
}