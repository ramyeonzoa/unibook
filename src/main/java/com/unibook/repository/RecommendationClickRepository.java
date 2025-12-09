package com.unibook.repository;

import com.unibook.domain.entity.RecommendationClick;
import com.unibook.domain.entity.RecommendationClick.RecommendationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationClickRepository extends JpaRepository<RecommendationClick, Long> {

    /**
     * 특정 타입의 클릭 수 조회
     */
    long countByType(RecommendationType type);

    /**
     * 기간별 클릭 수 조회
     */
    long countByClickedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 특정 타입 + 기간별 클릭 수 조회
     */
    long countByTypeAndClickedAtBetween(RecommendationType type, LocalDateTime start, LocalDateTime end);

    /**
     * 사용자별 클릭 수 조회
     */
    long countByUserUserId(Long userId);

    /**
     * 게시글별 클릭 수 조회
     */
    long countByPostPostId(Long postId);

    /**
     * 타입별 클릭 수 통계
     */
    @Query("SELECT rc.type, COUNT(rc) FROM RecommendationClick rc GROUP BY rc.type")
    List<Object[]> countClicksByType();

    /**
     * 위치별 클릭 분포 (특정 타입)
     */
    @Query("SELECT rc.position, COUNT(rc) FROM RecommendationClick rc " +
           "WHERE rc.type = :type " +
           "GROUP BY rc.position " +
           "ORDER BY rc.position")
    List<Object[]> countClicksByPosition(@Param("type") RecommendationType type);

    /**
     * 일별 클릭 통계 (최근 N일)
     */
    @Query("SELECT DATE(rc.clickedAt), COUNT(rc) FROM RecommendationClick rc " +
           "WHERE rc.clickedAt >= :startDate " +
           "GROUP BY DATE(rc.clickedAt) " +
           "ORDER BY DATE(rc.clickedAt)")
    List<Object[]> countDailyClicks(@Param("startDate") LocalDateTime startDate);

    /**
     * 타입별 일별 클릭 통계
     */
    @Query("SELECT DATE(rc.clickedAt), rc.type, COUNT(rc) FROM RecommendationClick rc " +
           "WHERE rc.clickedAt >= :startDate " +
           "GROUP BY DATE(rc.clickedAt), rc.type " +
           "ORDER BY DATE(rc.clickedAt), rc.type")
    List<Object[]> countDailyClicksByType(@Param("startDate") LocalDateTime startDate);

    /**
     * 가장 많이 클릭된 게시글 (특정 타입)
     */
    @Query("SELECT rc.post.postId, COUNT(rc) as clickCount FROM RecommendationClick rc " +
           "WHERE rc.type = :type " +
           "AND rc.clickedAt >= :startDate " +
           "GROUP BY rc.post.postId " +
           "ORDER BY clickCount DESC")
    List<Object[]> findMostClickedPosts(@Param("type") RecommendationType type,
                                        @Param("startDate") LocalDateTime startDate);

    /**
     * 사용자의 최근 클릭 이력 조회 (타임스탬프 포함)
     * 다중 행동 추천 시스템용
     */
    @Query("SELECT rc.post.postId, rc.clickedAt FROM RecommendationClick rc " +
           "WHERE rc.user.userId = :userId " +
           "ORDER BY rc.clickedAt DESC")
    List<Object[]> findRecentClicksWithTimestampByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 슬롯/소스 라벨별 클릭 수 통계 (기간 필터)
     */
    @Query("SELECT rc.sourceLabel, COUNT(rc) FROM RecommendationClick rc " +
           "WHERE rc.clickedAt BETWEEN :start AND :end " +
           "GROUP BY rc.sourceLabel")
    List<Object[]> countClicksBySourceLabel(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
