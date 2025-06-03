package com.unibook.repository;

import com.unibook.domain.entity.AdminAction;
import com.unibook.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {
    
    /**
     * 특정 대상에 대한 최근 조치 조회
     */
    Optional<AdminAction> findFirstByTargetTypeAndTargetIdOrderByCreatedAtDesc(
        AdminAction.TargetType targetType, Long targetId);
    
    /**
     * 특정 사용자의 현재 활성 정지 조회
     */
    @Query("SELECT aa FROM AdminAction aa WHERE aa.targetType = 'USER' AND aa.targetId = :userId " +
           "AND aa.actionType = 'SUSPEND' AND (aa.expiresAt IS NULL OR aa.expiresAt > :now)")
    Optional<AdminAction> findActiveSuspension(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * 만료된 조치들 조회 (자동 해제용)
     */
    @Query("SELECT aa FROM AdminAction aa WHERE aa.expiresAt < :now " +
           "AND aa.actionType IN ('SUSPEND', 'BLOCK')")
    List<AdminAction> findExpiredActions(@Param("now") LocalDateTime now);
    
    /**
     * 관리자별 조치 이력
     */
    List<AdminAction> findByAdminIdOrderByCreatedAtDesc(Long adminId);
    
    /**
     * 대상별 조치 이력
     */
    List<AdminAction> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
        AdminAction.TargetType targetType, Long targetId);
    
    /**
     * 특정 신고와 관련된 조치들
     */
    List<AdminAction> findByRelatedReportIdOrderByCreatedAtDesc(Long reportId);
    
    /**
     * 최근 조치 이력 (관리자 대시보드용)
     */
    @Query("SELECT aa FROM AdminAction aa ORDER BY aa.createdAt DESC")
    List<AdminAction> findRecentActions(@Param("limit") int limit);
    
    /**
     * 조치 타입별 개수 (통계용)
     */
    @Query("SELECT aa.actionType, COUNT(aa) FROM AdminAction aa " +
           "WHERE aa.createdAt >= :startDate GROUP BY aa.actionType")
    List<Object[]> countActionsByType(@Param("startDate") LocalDateTime startDate);
}