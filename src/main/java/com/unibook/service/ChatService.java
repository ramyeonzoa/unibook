package com.unibook.service;

import com.unibook.domain.dto.ChatDto;
import com.unibook.domain.dto.NotificationDto;
import com.unibook.domain.entity.ChatRoom;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.domain.entity.Notification;
import com.unibook.exception.ValidationException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.repository.ChatRoomRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.UserRepository;
import com.unibook.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    
    /**
     * 채팅방 생성 또는 기존 채팅방 반환
     */
    @Transactional
    public ChatDto.ChatRoomDetailResponse createOrGetChatRoom(Long buyerId, ChatDto.CreateChatRoomRequest request) {
        // 게시글 조회
        Post post = postRepository.findById(request.getPostId())
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        // 구매자 조회
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        User seller = post.getUser();
        
        // 자기 자신과 채팅 방지
        if (buyerId.equals(seller.getUserId())) {
            throw new ValidationException("본인이 작성한 게시글과는 채팅할 수 없습니다.");
        }
        
        // 기존 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByBuyerAndSellerAndPost(buyerId, seller.getUserId(), post.getPostId())
            .orElseGet(() -> createNewChatRoom(buyer, seller, post));
        
        return ChatDto.ChatRoomDetailResponse.from(chatRoom, buyerId);
    }
    
    /**
     * 새 채팅방 생성
     */
    @Transactional
    public ChatRoom createNewChatRoom(User buyer, User seller, Post post) {
        ChatRoom chatRoom = ChatRoom.builder()
            .buyer(buyer)
            .seller(seller)
            .post(post)
            .status(ChatRoom.ChatRoomStatus.ACTIVE)
            .buyerUnreadCount(0)
            .sellerUnreadCount(0)
            .build();
        
        // 저장 후 실제 Firebase Room ID 설정
        chatRoom = chatRoomRepository.save(chatRoom);
        chatRoom.setActualFirebaseRoomId();
        chatRoom = chatRoomRepository.save(chatRoom);
        
        log.info("새 채팅방 생성: {} (구매자: {}, 판매자: {}, 게시글: {})", 
            chatRoom.getChatRoomId(), buyer.getUserId(), seller.getUserId(), post.getPostId());
        
        return chatRoom;
    }
    
    /**
     * 사용자의 채팅방 목록 조회
     */
    public List<ChatDto.ChatRoomListResponse> getChatRoomsByUserId(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserIdOrderByLastMessageTimeDesc(userId);
        
        return chatRooms.stream()
            .map(chatRoom -> ChatDto.ChatRoomListResponse.from(chatRoom, userId))
            .collect(Collectors.toList());
    }
    
    /**
     * 채팅방 상세 조회 (권한 체크 포함)
     */
    public ChatDto.ChatRoomDetailResponse getChatRoomDetail(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        return ChatDto.ChatRoomDetailResponse.from(chatRoom, userId);
    }
    
    /**
     * Firebase Room ID로 채팅방 조회
     */
    public ChatDto.ChatRoomDetailResponse getChatRoomByFirebaseId(String firebaseRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByFirebaseRoomId(firebaseRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
        
        // 권한 체크
        if (!chatRoom.isParticipant(userId)) {
            throw new ValidationException("채팅방에 접근할 권한이 없습니다.");
        }
        
        return ChatDto.ChatRoomDetailResponse.from(chatRoom, userId);
    }
    
    /**
     * Firebase Room ID로 채팅방 조회 (인증 없이, 글로벌 리스너용)
     */
    public ChatRoom getChatRoomByFirebaseIdWithoutAuth(String firebaseRoomId) {
        return chatRoomRepository.findByFirebaseRoomId(firebaseRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
    }
    
    /**
     * 마지막 메시지 정보 업데이트
     */
    @Transactional
    public void updateLastMessage(String firebaseRoomId, String lastMessage, LocalDateTime timestamp) {
        ChatRoom chatRoom = chatRoomRepository.findByFirebaseRoomId(firebaseRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
        
        chatRoom.setLastMessage(lastMessage);
        chatRoom.setLastMessageTime(timestamp);
        
        chatRoomRepository.save(chatRoom);
    }
    
    /**
     * 읽지 않은 메시지 수 업데이트
     */
    @Transactional
    public void updateUnreadCount(String firebaseRoomId, Long userId, Integer unreadCount) {
        ChatRoom chatRoom = chatRoomRepository.findByFirebaseRoomId(firebaseRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
        
        chatRoom.setUnreadCountForUser(userId, unreadCount);
        chatRoomRepository.save(chatRoom);
    }
    
    /**
     * 사용자의 총 읽지 않은 메시지 수 조회
     */
    public Long getTotalUnreadCount(Long userId) {
        Long buyerUnreadCount = chatRoomRepository.getBuyerUnreadCountByUserId(userId);
        Long sellerUnreadCount = chatRoomRepository.getSellerUnreadCountByUserId(userId);
        
        return (buyerUnreadCount != null ? buyerUnreadCount : 0L) + 
               (sellerUnreadCount != null ? sellerUnreadCount : 0L);
    }
    
    /**
     * 채팅방 상태 변경
     */
    @Transactional
    public void updateChatRoomStatus(Long chatRoomId, Long userId, ChatRoom.ChatRoomStatus status) {
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        chatRoom.setStatus(status);
        chatRoomRepository.save(chatRoom);
    }
    
    /**
     * 게시글에 대한 채팅방 목록 (판매자용)
     */
    public List<ChatDto.ChatRoomListResponse> getChatRoomsByPostId(Long postId, Long sellerId) {
        // 게시글 작성자 권한 체크
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        if (!post.getUser().getUserId().equals(sellerId)) {
            throw new ValidationException("게시글 작성자만 채팅방 목록을 조회할 수 있습니다.");
        }
        
        List<ChatRoom> chatRooms = chatRoomRepository.findByPostIdOrderByCreatedAtDesc(postId);
        
        return chatRooms.stream()
            .map(chatRoom -> ChatDto.ChatRoomListResponse.from(chatRoom, sellerId))
            .collect(Collectors.toList());
    }
    
    /**
     * 상대방의 읽지 않은 메시지 수 증가
     */
    @Transactional
    public void incrementOtherUserUnreadCount(String firebaseRoomId, Long currentUserId) {
        ChatRoom chatRoom = chatRoomRepository.findByFirebaseRoomId(firebaseRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
        
        // 현재 사용자가 구매자인지 판매자인지 확인
        if (currentUserId.equals(chatRoom.getBuyer().getUserId())) {
            // 현재 사용자가 구매자면 판매자의 unreadCount 증가
            chatRoom.setSellerUnreadCount(chatRoom.getSellerUnreadCount() + 1);
        } else if (currentUserId.equals(chatRoom.getSeller().getUserId())) {
            // 현재 사용자가 판매자면 구매자의 unreadCount 증가
            chatRoom.setBuyerUnreadCount(chatRoom.getBuyerUnreadCount() + 1);
        }
        
        chatRoomRepository.save(chatRoom);
        log.info("상대방 읽지 않은 메시지 수 증가: firebaseRoomId={}, currentUserId={}", 
                firebaseRoomId, currentUserId);
    }
    
    /**
     * 채팅방 삭제 (비활성화)
     */
    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(chatRoomId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없거나 접근 권한이 없습니다."));
        
        // 실제 삭제 대신 상태 변경
        chatRoom.setStatus(ChatRoom.ChatRoomStatus.BLOCKED);
        chatRoomRepository.save(chatRoom);
        
        log.info("채팅방 비활성화: {} (사용자: {})", chatRoomId, userId);
    }
    
    /**
     * 채팅 알림 전송 (벨 알림 시스템 사용 안 함, 채팅 전용)
     */
    @Transactional
    public void sendChatNotification(ChatDto.ChatNotificationRequest request) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
        
        log.info("채팅 알림 처리 완료 (벨 알림 제외): recipient={}, sender={}, chatRoom={}", 
                request.getRecipientId(), request.getSenderId(), request.getChatRoomId());
    }
    
    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회
     */
    @Transactional(readOnly = true)
    public Integer getChatRoomUnreadCount(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("채팅방을 찾을 수 없습니다."));
        
        // 사용자가 채팅방 참여자인지 확인
        if (!chatRoom.getBuyer().getUserId().equals(userId) && 
            !chatRoom.getSeller().getUserId().equals(userId)) {
            throw new ValidationException("채팅방에 참여하지 않은 사용자입니다.");
        }
        
        // 사용자가 구매자인지 판매자인지 확인하여 읽지 않은 메시지 수 반환
        return chatRoom.getUnreadCountForUser(userId);
    }
}