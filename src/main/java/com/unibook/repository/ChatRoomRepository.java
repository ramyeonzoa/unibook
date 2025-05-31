package com.unibook.repository;

import com.unibook.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    /**
     * 특정 사용자가 참여한 채팅방 목록 조회 (최근 메시지 순)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.buyer b " +
           "LEFT JOIN FETCH cr.seller s " +
           "LEFT JOIN FETCH cr.post p " +
           "WHERE (cr.buyer.userId = :userId OR cr.seller.userId = :userId) " +
           "AND cr.status = 'ACTIVE' " +
           "ORDER BY cr.lastMessageTime DESC NULLS LAST, cr.createdAt DESC")
    List<ChatRoom> findByUserIdOrderByLastMessageTimeDesc(@Param("userId") Long userId);
    
    /**
     * 구매자와 판매자, 게시글로 기존 채팅방 찾기
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.buyer.userId = :buyerId " +
           "AND cr.seller.userId = :sellerId " +
           "AND cr.post.postId = :postId " +
           "AND cr.status = 'ACTIVE'")
    Optional<ChatRoom> findByBuyerAndSellerAndPost(
        @Param("buyerId") Long buyerId, 
        @Param("sellerId") Long sellerId, 
        @Param("postId") Long postId
    );
    
    /**
     * Firebase Room ID로 채팅방 찾기
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.buyer " +
           "LEFT JOIN FETCH cr.seller " +
           "LEFT JOIN FETCH cr.post " +
           "WHERE cr.firebaseRoomId = :firebaseRoomId")
    Optional<ChatRoom> findByFirebaseRoomId(@Param("firebaseRoomId") String firebaseRoomId);
    
    /**
     * 구매자로서의 읽지 않은 메시지 수
     */
    @Query("SELECT COALESCE(SUM(cr.buyerUnreadCount), 0) " +
           "FROM ChatRoom cr " +
           "WHERE cr.buyer.userId = :userId " +
           "AND cr.status = 'ACTIVE'")
    Long getBuyerUnreadCountByUserId(@Param("userId") Long userId);
    
    /**
     * 판매자로서의 읽지 않은 메시지 수
     */
    @Query("SELECT COALESCE(SUM(cr.sellerUnreadCount), 0) " +
           "FROM ChatRoom cr " +
           "WHERE cr.seller.userId = :userId " +
           "AND cr.status = 'ACTIVE'")
    Long getSellerUnreadCountByUserId(@Param("userId") Long userId);
    
    /**
     * 게시글에 대한 채팅방 목록 (판매자용)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.buyer " +
           "LEFT JOIN FETCH cr.seller " +
           "LEFT JOIN FETCH cr.post " +
           "WHERE cr.post.postId = :postId " +
           "AND cr.status = 'ACTIVE' " +
           "ORDER BY cr.createdAt DESC")
    List<ChatRoom> findByPostIdOrderByCreatedAtDesc(@Param("postId") Long postId);
    
    /**
     * 특정 채팅방 상세 조회 (권한 확인용)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.buyer b " +
           "LEFT JOIN FETCH cr.seller s " +
           "LEFT JOIN FETCH cr.post p " +
           "LEFT JOIN FETCH p.user " +
           "WHERE cr.chatRoomId = :chatRoomId " +
           "AND (cr.buyer.userId = :userId OR cr.seller.userId = :userId) " +
           "AND cr.status = 'ACTIVE'")
    Optional<ChatRoom> findByIdAndUserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}