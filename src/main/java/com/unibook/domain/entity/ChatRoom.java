package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 채팅방 Entity
 * - 1:1 채팅방만 지원
 * - 실제 메시지는 Firebase Firestore에 저장
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"buyer", "seller", "post"})
public class ChatRoom extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;
    
    /**
     * 구매자 (채팅을 시작한 사람)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    /**
     * 판매자 (게시글 작성자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    
    /**
     * 관련 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    /**
     * 채팅방 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;
    
    /**
     * Firebase Firestore의 채팅방 Document ID
     * - "chatroom_{chatRoomId}" 형태
     */
    @Column(name = "firebase_room_id", nullable = false, unique = true)
    private String firebaseRoomId;
    
    /**
     * 마지막 메시지 내용 (미리보기용)
     */
    @Column(name = "last_message", length = 500)
    private String lastMessage;
    
    /**
     * 마지막 메시지 시간 (정렬용)
     */
    @Column(name = "last_message_time")
    private java.time.LocalDateTime lastMessageTime;
    
    /**
     * 구매자의 읽지 않은 메시지 수
     */
    @Column(name = "buyer_unread_count")
    @Builder.Default
    private Integer buyerUnreadCount = 0;
    
    /**
     * 판매자의 읽지 않은 메시지 수
     */
    @Column(name = "seller_unread_count")
    @Builder.Default
    private Integer sellerUnreadCount = 0;
    
    /**
     * 채팅방 상태 enum
     */
    public enum ChatRoomStatus {
        ACTIVE,    // 활성 상태
        BLOCKED,   // 차단됨 (신고 등)
        COMPLETED  // 거래 완료로 종료
    }
    
    /**
     * Firebase Room ID 생성
     */
    @PrePersist
    public void generateFirebaseRoomId() {
        if (this.firebaseRoomId == null) {
            // 임시 ID 생성 (실제 ID는 저장 후 업데이트)
            this.firebaseRoomId = "temp_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 실제 Firebase Room ID 설정
     */
    public void setActualFirebaseRoomId() {
        this.firebaseRoomId = "chatroom_" + this.chatRoomId;
    }
    
    /**
     * 특정 사용자의 읽지 않은 메시지 수 반환
     */
    public Integer getUnreadCountForUser(Long userId) {
        if (userId.equals(buyer.getUserId())) {
            return buyerUnreadCount;
        } else if (userId.equals(seller.getUserId())) {
            return sellerUnreadCount;
        }
        return 0;
    }
    
    /**
     * 특정 사용자의 읽지 않은 메시지 수 설정
     */
    public void setUnreadCountForUser(Long userId, Integer count) {
        if (userId.equals(buyer.getUserId())) {
            this.buyerUnreadCount = count;
        } else if (userId.equals(seller.getUserId())) {
            this.sellerUnreadCount = count;
        }
    }
    
    /**
     * 상대방 사용자 반환
     */
    public User getOtherUser(Long currentUserId) {
        if (currentUserId.equals(buyer.getUserId())) {
            return seller;
        } else if (currentUserId.equals(seller.getUserId())) {
            return buyer;
        }
        throw new IllegalArgumentException("사용자가 이 채팅방의 참여자가 아닙니다.");
    }
    
    /**
     * 사용자가 이 채팅방의 참여자인지 확인
     */
    public boolean isParticipant(Long userId) {
        return userId.equals(buyer.getUserId()) || userId.equals(seller.getUserId());
    }
}