package com.unibook.repository;

import com.unibook.domain.entity.PostView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {

    /**
     * 특정 사용자의 조회 기록 개수
     */
    long countByUser_UserId(Long userId);

    /**
     * 특정 사용자의 최근 조회 기록 조회
     */
    @Query("SELECT pv FROM PostView pv " +
           "WHERE pv.user.userId = :userId " +
           "ORDER BY pv.viewedAt DESC")
    List<PostView> findByUser_UserIdOrderByViewedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 게시글을 조회한 사용자들 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT pv.user.userId FROM PostView pv " +
           "WHERE pv.post.postId = :postId AND pv.user.userId IS NOT NULL")
    List<Long> findDistinctUserIdsByPostId(@Param("postId") Long postId);

    /**
     * 특정 사용자가 최근 조회한 게시글 ID 목록
     * GROUP BY로 중복 제거, 각 게시글의 가장 최근 조회 시각 기준 정렬
     */
    @Query("SELECT pv.post.postId FROM PostView pv " +
           "WHERE pv.user.userId = :userId " +
           "GROUP BY pv.post.postId " +
           "ORDER BY MAX(pv.viewedAt) DESC")
    List<Long> findRecentViewedPostIdsByUser_UserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 게시글의 총 조회수
     */
    long countByPost_PostId(Long postId);

    /**
     * 특정 기간 동안의 조회 기록
     */
    @Query("SELECT pv FROM PostView pv " +
           "WHERE pv.viewedAt BETWEEN :startDate AND :endDate")
    List<PostView> findByViewedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Collaborative Filtering을 위한 쿼리:
     * "이 게시글을 본 사용자들이 본 다른 게시글들"
     */
    @Query("SELECT pv2.post.postId, COUNT(pv2.post.postId) as viewCount " +
           "FROM PostView pv1 " +
           "JOIN PostView pv2 ON pv1.user.userId = pv2.user.userId " +
           "WHERE pv1.post.postId = :postId " +
           "AND pv2.post.postId != :postId " +
           "AND pv1.user.userId IS NOT NULL " +
           "GROUP BY pv2.post.postId " +
           "ORDER BY viewCount DESC")
    List<Object[]> findCollaborativePostsByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * "이 사용자와 비슷한 취향을 가진 사용자들이 본 게시글들"
     */
    @Query("SELECT pv2.post.postId, COUNT(pv2.post.postId) as viewCount " +
           "FROM PostView pv1 " +
           "JOIN PostView pv2 ON pv1.post.postId = pv2.post.postId " +
           "WHERE pv1.user.userId = :userId " +
           "AND pv2.user.userId != :userId " +
           "AND pv2.user.userId IS NOT NULL " +
           "GROUP BY pv2.post.postId " +
           "ORDER BY viewCount DESC")
    List<Object[]> findCollaborativePostsByUserId(@Param("userId") Long userId, Pageable pageable);
}
