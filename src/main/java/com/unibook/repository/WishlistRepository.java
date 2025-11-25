package com.unibook.repository;

import com.unibook.domain.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    /**
     * 특정 사용자와 게시글로 Wishlist 찾기
     */
    Optional<Wishlist> findByUserUserIdAndPostPostId(Long userId, Long postId);
    
    /**
     * 특정 사용자와 게시글로 Wishlist 존재 여부 확인
     */
    boolean existsByUserUserIdAndPostPostId(Long userId, Long postId);
    
    /**
     * 특정 사용자의 모든 Wishlist 찾기 (페이징)
     */
    @Query(value = "SELECT w FROM Wishlist w " +
                   "JOIN FETCH w.post p " +
                   "JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH p.postImages " +
                   "WHERE w.user.userId = :userId " +
                   "ORDER BY w.createdAt DESC",
           countQuery = "SELECT COUNT(w) FROM Wishlist w WHERE w.user.userId = :userId")
    Page<Wishlist> findByUserIdWithPost(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 특정 사용자와 게시글로 Wishlist 삭제
     */
    void deleteByUserUserIdAndPostPostId(Long userId, Long postId);
    
    /**
     * 특정 게시글을 찜한 모든 사용자의 Wishlist 찾기
     */
    @Query("SELECT w FROM Wishlist w " +
           "JOIN FETCH w.user u " +
           "WHERE w.post.postId = :postId")
    List<Wishlist> findByPostIdWithUser(@Param("postId") Long postId);

    /**
     * 특정 사용자가 찜한 게시글 ID 목록 조회
     * 다중 행동 추천 시스템용
     */
    @Query("SELECT w.post.postId FROM Wishlist w WHERE w.user.userId = :userId")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);
}