package com.unibook.repository;

import com.unibook.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자별 알림 목록 조회 (읽지 않은 알림 우선, 최신순)
     * CountQuery 분리로 Fetch Join과 페이징 최적화
     */
    @Query(
        value = "SELECT n FROM Notification n " +
                "LEFT JOIN FETCH n.actor " +
                "LEFT JOIN FETCH n.relatedPost " +
                "WHERE n.recipient.userId = :userId " +
                "ORDER BY n.isRead ASC, n.createdAt DESC",
        countQuery = "SELECT COUNT(n) FROM Notification n " +
                     "WHERE n.recipient.userId = :userId"
    )
    Page<Notification> findByRecipientUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자별 읽지 않은 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n " +
           "WHERE n.recipient.userId = :userId AND n.isRead = false")
    long countUnreadByRecipientUserId(@Param("userId") Long userId);

    /**
     * 사용자별 전체 알림 개수 조회
     */
    long countByRecipientUserId(Long userId);

    /**
     * 사용자별 읽지 않은 알림 목록 조회 (페이징 지원)
     * Page 타입으로 반환하여 Pageable 파라미터가 제대로 동작하도록 함
     */
    @Query(
        value = "SELECT n FROM Notification n " +
                "LEFT JOIN FETCH n.actor " +
                "LEFT JOIN FETCH n.relatedPost " +
                "WHERE n.recipient.userId = :userId AND n.isRead = false " +
                "ORDER BY n.createdAt DESC",
        countQuery = "SELECT COUNT(n) FROM Notification n " +
                     "WHERE n.recipient.userId = :userId AND n.isRead = false"
    )
    Page<Notification> findUnreadByRecipientUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 알림을 읽음으로 표시
     * clearAutomatically = true로 영속성 컨텍스트 동기화
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true " +
           "WHERE n.notificationId = :notificationId AND n.recipient.userId = :userId")
    int markAsReadByIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    /**
     * 사용자의 모든 알림을 읽음으로 표시
     * clearAutomatically = true로 영속성 컨텍스트 동기화
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true " +
           "WHERE n.recipient.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 1년 이상된 알림 삭제 (스케줄러용)
     * clearAutomatically = true로 영속성 컨텍스트 동기화
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 특정 게시글 관련 알림 삭제 (게시글 삭제 시)
     * clearAutomatically = true로 영속성 컨텍스트 동기화
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.relatedPost.postId = :postId")
    int deleteByRelatedPostId(@Param("postId") Long postId);
    
    /**
     * 특정 채팅방의 읽지 않은 메시지 알림 조회
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.recipient.userId = :userId " +
           "AND n.type = com.unibook.domain.entity.Notification$NotificationType.NEW_MESSAGE " +
           "AND n.isRead = false " +
           "AND n.url = :chatRoomUrl")
    List<Notification> findUnreadChatNotificationsByUserAndChatRoom(@Param("userId") Long userId, 
                                                                   @Param("chatRoomUrl") String chatRoomUrl);
}