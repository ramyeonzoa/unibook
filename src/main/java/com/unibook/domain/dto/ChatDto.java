package com.unibook.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.domain.entity.ChatRoom;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 관련 DTO 클래스들
 */
public class ChatDto {
    
    /**
     * 채팅방 목록용 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRoomListResponse {
        private Long chatRoomId;
        private String firebaseRoomId;
        
        // 상대방 정보
        private Long otherUserId;
        private String otherUserName;
        
        // 게시글 정보
        private Long postId;
        private String postTitle;
        private String postThumbnail; // 첫 번째 이미지
        private Long postOwnerId; // 게시글 작성자 ID (구매자/판매자 구분용)
        
        // 마지막 메시지 정보
        private String lastMessage;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastMessageTime;
        
        // 읽지 않은 메시지 수
        private Integer unreadCount;
        
        // 채팅방 상태
        private ChatRoom.ChatRoomStatus status;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        /**
         * Entity에서 DTO로 변환 (현재 사용자 기준)
         */
        public static ChatRoomListResponse from(ChatRoom chatRoom, Long currentUserId) {
            User otherUser = chatRoom.getOtherUser(currentUserId);
            Post post = chatRoom.getPost();
            
            // 게시글 썸네일 (첫 번째 이미지)
            String thumbnail = null;
            if (post.getPostImages() != null && !post.getPostImages().isEmpty()) {
                thumbnail = post.getPostImages().get(0).getImageUrl();
            }
            
            return ChatRoomListResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .firebaseRoomId(chatRoom.getFirebaseRoomId())
                .otherUserId(otherUser.getUserId())
                .otherUserName(otherUser.getName())
                .postId(post.getPostId())
                .postTitle(post.getTitle())
                .postThumbnail(thumbnail)
                .postOwnerId(post.getUser().getUserId())
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageTime(chatRoom.getLastMessageTime())
                .unreadCount(chatRoom.getUnreadCountForUser(currentUserId))
                .status(chatRoom.getStatus())
                .createdAt(chatRoom.getCreatedAt())
                .build();
        }
    }
    
    /**
     * 채팅방 생성 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChatRoomRequest {
        private Long postId;
        private String initialMessage; // 선택적 초기 메시지
    }
    
    /**
     * 채팅방 상세 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRoomDetailResponse {
        private Long chatRoomId;
        private String firebaseRoomId;
        
        // 상대방 정보
        private Long otherUserId;
        private String otherUserName;
        private String otherUserEmail;
        
        // 게시글 정보
        private Long postId;
        private String postTitle;
        private String postThumbnail;
        private Post.PostStatus postStatus;
        private Integer postPrice;
        private Long postOwnerId;
        
        // 채팅방 정보
        private ChatRoom.ChatRoomStatus status;
        private Integer unreadCount;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        /**
         * Entity에서 DTO로 변환
         */
        public static ChatRoomDetailResponse from(ChatRoom chatRoom, Long currentUserId) {
            User otherUser = chatRoom.getOtherUser(currentUserId);
            Post post = chatRoom.getPost();
            
            String thumbnail = null;
            if (post.getPostImages() != null && !post.getPostImages().isEmpty()) {
                thumbnail = post.getPostImages().get(0).getImageUrl();
            }
            
            return ChatRoomDetailResponse.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .firebaseRoomId(chatRoom.getFirebaseRoomId())
                .otherUserId(otherUser.getUserId())
                .otherUserName(otherUser.getName())
                .otherUserEmail(otherUser.getEmail())
                .postId(post.getPostId())
                .postTitle(post.getTitle())
                .postThumbnail(thumbnail)
                .postStatus(post.getStatus())
                .postPrice(post.getPrice())
                .postOwnerId(post.getUser().getUserId())
                .status(chatRoom.getStatus())
                .unreadCount(chatRoom.getUnreadCountForUser(currentUserId))
                .createdAt(chatRoom.getCreatedAt())
                .build();
        }
    }
    
    /**
     * Firebase 메시지 DTO (Firestore 문서 구조)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FirebaseMessage {
        private String messageId;
        private Long senderId;
        private String senderName;
        private String content;
        private MessageType type;
        private String imageUrl; // 이미지 메시지인 경우
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;
        
        // 읽음 상태 (사용자별)
        @JsonProperty("isReadByBuyer")
        private Boolean isReadByBuyer;
        
        @JsonProperty("isReadBySeller")
        private Boolean isReadBySeller;
        
        /**
         * 메시지 타입
         */
        public enum MessageType {
            TEXT,    // 텍스트 메시지
            IMAGE,   // 이미지 메시지
            SYSTEM   // 시스템 메시지 (거래 상태 변경 등)
        }
    }
    
    /**
     * 메시지 전송 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        private String content;
        private FirebaseMessage.MessageType type;
        private String imageUrl; // 이미지 메시지인 경우
    }
    
    /**
     * 읽음 처리 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarkAsReadRequest {
        private String lastReadMessageId;
    }
    
    /**
     * 채팅 알림 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatNotificationRequest {
        private Long recipientId;    // 수신자 ID
        private Long senderId;       // 발신자 ID
        private String senderName;   // 발신자 이름
        private Long chatRoomId;     // 채팅방 ID
        private String message;      // 메시지 내용
    }
}