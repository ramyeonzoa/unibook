package com.unibook.repository;

import com.unibook.domain.entity.RecommendationClick.RecommendationType;
import com.unibook.domain.entity.RecommendationImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationImpressionRepository extends JpaRepository<RecommendationImpression, Long> {

  /**
   * 특정 타입의 노출 수 조회
   */
  long countByType(RecommendationType type);

  /**
   * 기간별 노출 수 조회
   */
  long countByImpressedAtBetween(LocalDateTime start, LocalDateTime end);

  /**
   * 특정 타입 + 기간별 노출 수 조회
   */
  long countByTypeAndImpressedAtBetween(RecommendationType type, LocalDateTime start, LocalDateTime end);

  /**
   * 기간별 총 노출 수 (count 필드 합산)
   */
  @Query("SELECT COALESCE(SUM(ri.count), 0) FROM RecommendationImpression ri " +
         "WHERE ri.impressedAt BETWEEN :start AND :end")
  long sumCountByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /**
   * 타입별 기간별 총 노출 수 (count 필드 합산)
   */
  @Query("SELECT COALESCE(SUM(ri.count), 0) FROM RecommendationImpression ri " +
         "WHERE ri.type = :type AND ri.impressedAt BETWEEN :start AND :end")
  long sumCountByTypeAndPeriod(@Param("type") RecommendationType type,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

  /**
   * 타입별 노출 수 통계
   */
  @Query("SELECT ri.type, COALESCE(SUM(ri.count), 0) FROM RecommendationImpression ri GROUP BY ri.type")
  List<Object[]> sumCountByType();

  /**
   * 일별 노출 수 통계 (최근 N일)
   */
  @Query("SELECT DATE(ri.impressedAt), COALESCE(SUM(ri.count), 0) FROM RecommendationImpression ri " +
         "WHERE ri.impressedAt >= :startDate " +
         "GROUP BY DATE(ri.impressedAt) " +
         "ORDER BY DATE(ri.impressedAt)")
  List<Object[]> sumDailyImpressions(@Param("startDate") LocalDateTime startDate);

  /**
   * 타입별 일별 노출 수 통계
   */
  @Query("SELECT DATE(ri.impressedAt), ri.type, COALESCE(SUM(ri.count), 0) FROM RecommendationImpression ri " +
         "WHERE ri.impressedAt >= :startDate " +
         "GROUP BY DATE(ri.impressedAt), ri.type " +
         "ORDER BY DATE(ri.impressedAt), ri.type")
  List<Object[]> sumDailyImpressionsByType(@Param("startDate") LocalDateTime startDate);

  /**
   * 중복 체크: 특정 세션, 타입, 시간 범위 내 존재 여부
   * (5분 이내 같은 세션 + 같은 타입 = 중복으로 간주)
   */
  @Query("SELECT COUNT(ri) > 0 FROM RecommendationImpression ri " +
         "WHERE ri.sessionId = :sessionId " +
         "AND ri.type = :type " +
         "AND ri.impressedAt >= :since")
  boolean existsBySessionIdAndTypeAndSince(@Param("sessionId") String sessionId,
                                           @Param("type") RecommendationType type,
                                           @Param("since") LocalDateTime since);

  /**
   * 기간별 유니크 세션 수
   */
  @Query("SELECT COUNT(DISTINCT ri.sessionId) FROM RecommendationImpression ri " +
         "WHERE ri.impressedAt BETWEEN :start AND :end")
  long countDistinctSessionsByPeriod(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}
