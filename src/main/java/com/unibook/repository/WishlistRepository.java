package com.unibook.repository;

import com.unibook.domain.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    @Query("SELECT w FROM Wishlist w " +
           "JOIN FETCH w.post p " +
           "JOIN FETCH p.user u " +
           "LEFT JOIN FETCH p.postImages " +
           "WHERE w.user.userId = :userId " +
           "ORDER BY w.createdAt DESC")
    Page<Wishlist> findByUserIdWithPost(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 특정 사용자와 게시글로 Wishlist 삭제
     */
    void deleteByUserUserIdAndPostPostId(Long userId, Long postId);
}