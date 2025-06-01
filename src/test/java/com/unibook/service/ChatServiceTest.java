package com.unibook.service;

import com.unibook.domain.dto.ChatDto;
import com.unibook.domain.dto.NotificationDto;
import com.unibook.domain.entity.*;
import com.unibook.repository.ChatRoomRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.UserRepository;
import com.unibook.repository.NotificationRepository;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private ChatService chatService;
    
    private User buyer;
    private User seller;
    private Post post;
    private ChatRoom activeChatRoom;
    private ChatRoom completedChatRoom;
    private ChatRoom deletedChatRoom;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        School school = School.builder()
                .schoolId(1L)
                .schoolName("테스트대학교")
                .build();
        
        Department department = Department.builder()
                .departmentId(1L)
                .departmentName("컴퓨터공학과")
                .school(school)
                .build();
        
        buyer = User.builder()
                .userId(1L)
                .email("buyer@test.ac.kr")
                .name("구매자")
                .department(department)
                .build();
        
        seller = User.builder()
                .userId(2L)
                .email("seller@test.ac.kr")
                .name("판매자")
                .department(department)
                .build();
        
        post = Post.builder()
                .postId(1L)
                .title("테스트 교재")
                .description("테스트 설명")
                .price(15000)
                .productType(Post.ProductType.TEXTBOOK)
                .status(Post.PostStatus.AVAILABLE)
                .transactionMethod(Post.TransactionMethod.DIRECT)
                .campusLocation("중앙캠퍼스")
                .user(seller)
                .build();
        
        activeChatRoom = ChatRoom.builder()
                .chatRoomId(1L)
                .buyer(buyer)
                .seller(seller)
                .post(post)
                .status(ChatRoom.ChatRoomStatus.ACTIVE)
                .firebaseRoomId("chatroom_1")
                .buyerUnreadCount(3)
                .sellerUnreadCount(2)
                .build();
        
        completedChatRoom = ChatRoom.builder()
                .chatRoomId(2L)
                .buyer(buyer)
                .seller(seller)
                .post(post)
                .status(ChatRoom.ChatRoomStatus.COMPLETED)
                .firebaseRoomId("chatroom_2")
                .buyerUnreadCount(1)
                .sellerUnreadCount(0)
                .build();
        
        deletedChatRoom = ChatRoom.builder()
                .chatRoomId(3L)
                .buyer(buyer)
                .seller(seller)
                .post(post)
                .status(ChatRoom.ChatRoomStatus.DELETED)
                .firebaseRoomId("chatroom_3")
                .buyerUnreadCount(5) // 삭제된 채팅방의 읽지 않은 수는 계산에서 제외되어야 함
                .sellerUnreadCount(3)
                .build();
    }
    
    @Test
    @DisplayName("사용자의 총 읽지 않은 채팅 수 조회 시 DELETED 상태는 제외되어야 한다")
    void getTotalUnreadCount_ShouldExcludeDeletedChatRooms() {
        // Given
        Long userId = buyer.getUserId();
        
        // Repository에서 DELETED 상태를 제외한 결과 반환 (3+1=4, DELETED의 5는 제외)
        when(chatRoomRepository.getBuyerUnreadCountByUserId(userId)).thenReturn(4L);
        when(chatRoomRepository.getSellerUnreadCountByUserId(userId)).thenReturn(0L);
        
        // When
        Long totalUnreadCount = chatService.getTotalUnreadCount(userId);
        
        // Then
        assertEquals(4L, totalUnreadCount);
        
        // Repository 메서드들이 호출되었는지 확인
        verify(chatRoomRepository).getBuyerUnreadCountByUserId(userId);
        verify(chatRoomRepository).getSellerUnreadCountByUserId(userId);
    }
    
    @Test
    @DisplayName("판매자의 총 읽지 않은 채팅 수 조회 시 DELETED 상태는 제외되어야 한다")
    void getTotalUnreadCount_ForSeller_ShouldExcludeDeletedChatRooms() {
        // Given
        Long sellerId = seller.getUserId();
        
        // Repository에서 DELETED 상태를 제외한 결과 반환 (2+0=2, DELETED의 3은 제외)
        when(chatRoomRepository.getBuyerUnreadCountByUserId(sellerId)).thenReturn(0L);
        when(chatRoomRepository.getSellerUnreadCountByUserId(sellerId)).thenReturn(2L);
        
        // When
        Long totalUnreadCount = chatService.getTotalUnreadCount(sellerId);
        
        // Then
        assertEquals(2L, totalUnreadCount);
        
        verify(chatRoomRepository).getBuyerUnreadCountByUserId(sellerId);
        verify(chatRoomRepository).getSellerUnreadCountByUserId(sellerId);
    }
    
    @Test
    @DisplayName("사용자의 채팅방 목록 조회 시 DELETED 상태는 제외되어야 한다")
    void getChatRoomsByUserId_ShouldExcludeDeletedChatRooms() {
        // Given
        Long userId = buyer.getUserId();
        List<ChatRoom> activeChatRooms = Arrays.asList(activeChatRoom, completedChatRoom);
        
        when(chatRoomRepository.findByUserIdOrderByLastMessageTimeDesc(userId))
                .thenReturn(activeChatRooms);
        
        // When
        List<ChatDto.ChatRoomListResponse> result = chatService.getChatRoomsByUserId(userId);
        
        // Then
        assertEquals(2, result.size());
        
        // Repository에서 이미 DELETED를 제외하고 반환하므로 service도 그대로 반환
        verify(chatRoomRepository).findByUserIdOrderByLastMessageTimeDesc(userId);
    }
    
    @Test
    @DisplayName("채팅방 생성 시 구매자와 판매자가 동일하면 예외가 발생해야 한다")
    void createOrGetChatRoom_WithSameBuyerAndSeller_ShouldThrowValidationException() {
        // Given
        ChatDto.CreateChatRoomRequest request = new ChatDto.CreateChatRoomRequest();
        request.setPostId(post.getPostId());
        Long sellerId = seller.getUserId();
        
        when(postRepository.findById(post.getPostId())).thenReturn(Optional.of(post));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        
        // When & Then
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> chatService.createOrGetChatRoom(sellerId, request)
        );
        
        assertEquals("본인이 작성한 게시글과는 채팅할 수 없습니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("새로운 채팅방 생성 시 정상적으로 생성되어야 한다")
    void createOrGetChatRoom_NewChatRoom_ShouldCreateSuccessfully() {
        // Given
        ChatDto.CreateChatRoomRequest request = new ChatDto.CreateChatRoomRequest();
        request.setPostId(post.getPostId());
        Long buyerId = buyer.getUserId();
        
        when(postRepository.findById(post.getPostId())).thenReturn(Optional.of(post));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(chatRoomRepository.findByBuyerAndSellerAndPost(buyerId, seller.getUserId(), post.getPostId()))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom chatRoom = invocation.getArgument(0);
            chatRoom.setChatRoomId(1L);
            return chatRoom;
        });
        
        // When
        ChatDto.ChatRoomDetailResponse result = chatService.createOrGetChatRoom(buyerId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getChatRoomId());
        assertEquals("chatroom_1", result.getFirebaseRoomId());
        
        verify(chatRoomRepository, times(2)).save(any(ChatRoom.class)); // 한번은 생성, 한번은 firebaseRoomId 업데이트
    }
    
    @Test
    @DisplayName("기존 채팅방이 있으면 기존 채팅방을 반환해야 한다")
    void createOrGetChatRoom_ExistingChatRoom_ShouldReturnExisting() {
        // Given
        ChatDto.CreateChatRoomRequest request = new ChatDto.CreateChatRoomRequest();
        request.setPostId(post.getPostId());
        Long buyerId = buyer.getUserId();
        
        when(postRepository.findById(post.getPostId())).thenReturn(Optional.of(post));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(chatRoomRepository.findByBuyerAndSellerAndPost(buyerId, seller.getUserId(), post.getPostId()))
                .thenReturn(Optional.of(activeChatRoom));
        
        // When
        ChatDto.ChatRoomDetailResponse result = chatService.createOrGetChatRoom(buyerId, request);
        
        // Then
        assertEquals(activeChatRoom.getChatRoomId(), result.getChatRoomId());
        
        // 새로운 채팅방이 생성되지 않았는지 확인
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 게시글로 채팅방 생성 시 예외가 발생해야 한다")
    void createOrGetChatRoom_WithNonExistentPost_ShouldThrowResourceNotFoundException() {
        // Given
        ChatDto.CreateChatRoomRequest request = new ChatDto.CreateChatRoomRequest();
        request.setPostId(999L);
        Long buyerId = buyer.getUserId();
        
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> chatService.createOrGetChatRoom(buyerId, request)
        );
        
        assertEquals("게시글을 찾을 수 없습니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("채팅방 읽지 않은 수 업데이트가 정상적으로 동작해야 한다")
    void updateUnreadCount_ShouldUpdateCorrectly() {
        // Given
        String firebaseRoomId = activeChatRoom.getFirebaseRoomId();
        Long userId = buyer.getUserId();
        Integer newCount = 5;
        
        when(chatRoomRepository.findByFirebaseRoomId(firebaseRoomId))
                .thenReturn(Optional.of(activeChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(activeChatRoom);
        
        // When
        chatService.updateUnreadCount(firebaseRoomId, userId, newCount);
        
        // Then
        assertEquals(newCount, activeChatRoom.getUnreadCountForUser(userId));
        verify(chatRoomRepository).save(activeChatRoom);
    }
    
    @Test
    @DisplayName("Firebase Room ID로 채팅방 조회가 정상적으로 동작해야 한다")
    void getChatRoomByFirebaseId_ShouldReturnChatRoom() {
        // Given
        String firebaseRoomId = activeChatRoom.getFirebaseRoomId();
        Long userId = buyer.getUserId();
        
        when(chatRoomRepository.findByFirebaseRoomId(firebaseRoomId))
                .thenReturn(Optional.of(activeChatRoom));
        
        // When
        ChatDto.ChatRoomDetailResponse result = chatService.getChatRoomByFirebaseId(firebaseRoomId, userId);
        
        // Then
        assertEquals(activeChatRoom.getChatRoomId(), result.getChatRoomId());
        verify(chatRoomRepository).findByFirebaseRoomId(firebaseRoomId);
    }
    
    @Test
    @DisplayName("존재하지 않는 Firebase Room ID로 조회 시 예외가 발생해야 한다")
    void getChatRoomByFirebaseId_WithNonExistentId_ShouldThrowResourceNotFoundException() {
        // Given
        String nonExistentFirebaseRoomId = "nonexistent_room";
        Long userId = buyer.getUserId();
        
        when(chatRoomRepository.findByFirebaseRoomId(nonExistentFirebaseRoomId))
                .thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> chatService.getChatRoomByFirebaseId(nonExistentFirebaseRoomId, userId)
        );
        
        assertEquals("채팅방을 찾을 수 없습니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("Firebase Room ID로 조회 시 권한이 없으면 예외가 발생해야 한다")
    void getChatRoomByFirebaseId_WithUnauthorizedUser_ShouldThrowValidationException() {
        // Given
        String firebaseRoomId = activeChatRoom.getFirebaseRoomId();
        Long unauthorizedUserId = 999L;
        
        when(chatRoomRepository.findByFirebaseRoomId(firebaseRoomId))
                .thenReturn(Optional.of(activeChatRoom));
        
        // When & Then
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> chatService.getChatRoomByFirebaseId(firebaseRoomId, unauthorizedUserId)
        );
        
        assertEquals("채팅방에 접근할 권한이 없습니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("마지막 메시지 업데이트가 정상적으로 동작해야 한다")
    void updateLastMessage_ShouldUpdateCorrectly() {
        // Given
        String firebaseRoomId = activeChatRoom.getFirebaseRoomId();
        String lastMessage = "새로운 메시지";
        LocalDateTime timestamp = LocalDateTime.now();
        
        when(chatRoomRepository.findByFirebaseRoomId(firebaseRoomId))
                .thenReturn(Optional.of(activeChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(activeChatRoom);
        
        // When
        chatService.updateLastMessage(firebaseRoomId, lastMessage, timestamp);
        
        // Then
        assertEquals(lastMessage, activeChatRoom.getLastMessage());
        assertEquals(timestamp, activeChatRoom.getLastMessageTime());
        verify(chatRoomRepository).save(activeChatRoom);
    }
    
    @Test
    @DisplayName("읽지 않은 메시지가 없는 사용자의 경우 0을 반환해야 한다")
    void getTotalUnreadCount_WithNoUnreadMessages_ShouldReturnZero() {
        // Given
        Long userId = 999L;
        
        when(chatRoomRepository.getBuyerUnreadCountByUserId(userId)).thenReturn(0L);
        when(chatRoomRepository.getSellerUnreadCountByUserId(userId)).thenReturn(0L);
        
        // When
        Long totalUnreadCount = chatService.getTotalUnreadCount(userId);
        
        // Then
        assertEquals(0L, totalUnreadCount);
    }
    
    @Test
    @DisplayName("null 값이 반환되는 경우 0으로 처리해야 한다")
    void getTotalUnreadCount_WithNullValues_ShouldReturnZero() {
        // Given
        Long userId = 999L;
        
        when(chatRoomRepository.getBuyerUnreadCountByUserId(userId)).thenReturn(null);
        when(chatRoomRepository.getSellerUnreadCountByUserId(userId)).thenReturn(null);
        
        // When
        Long totalUnreadCount = chatService.getTotalUnreadCount(userId);
        
        // Then
        assertEquals(0L, totalUnreadCount);
    }
    
    @Test
    @DisplayName("상대방의 읽지 않은 메시지 수 증가가 정상적으로 동작해야 한다")
    void incrementOtherUserUnreadCount_ShouldIncrementCorrectly() {
        // Given
        String firebaseRoomId = activeChatRoom.getFirebaseRoomId();
        Long currentUserId = buyer.getUserId();
        
        when(chatRoomRepository.findByFirebaseRoomId(firebaseRoomId))
                .thenReturn(Optional.of(activeChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(activeChatRoom);
        
        // When
        chatService.incrementOtherUserUnreadCount(firebaseRoomId, currentUserId);
        
        // Then
        assertEquals(3, activeChatRoom.getSellerUnreadCount()); // 2 + 1 = 3
        verify(chatRoomRepository).save(activeChatRoom);
    }
    
    @Test
    @DisplayName("채팅방 상태 변경이 정상적으로 동작해야 한다")
    void updateChatRoomStatus_ShouldUpdateCorrectly() {
        // Given
        Long chatRoomId = activeChatRoom.getChatRoomId();
        Long userId = buyer.getUserId();
        ChatRoom.ChatRoomStatus newStatus = ChatRoom.ChatRoomStatus.COMPLETED;
        
        when(chatRoomRepository.findByIdAndUserId(chatRoomId, userId))
                .thenReturn(Optional.of(activeChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(activeChatRoom);
        
        // When
        chatService.updateChatRoomStatus(chatRoomId, userId, newStatus);
        
        // Then
        assertEquals(newStatus, activeChatRoom.getStatus());
        verify(chatRoomRepository).save(activeChatRoom);
    }
    
    @Test
    @DisplayName("채팅 알림 전송이 정상적으로 동작해야 한다")
    void sendChatNotification_ShouldProcessCorrectly() {
        // Given
        ChatDto.ChatNotificationRequest request = new ChatDto.ChatNotificationRequest();
        request.setChatRoomId(1L);
        request.setRecipientId(2L);
        request.setSenderId(1L);
        request.setSenderName("테스터");
        request.setMessage("안녕하세요");
        
        when(chatRoomRepository.findById(request.getChatRoomId()))
                .thenReturn(Optional.of(activeChatRoom));
        
        // When
        chatService.sendChatNotification(request);
        
        // Then
        verify(chatRoomRepository).findById(request.getChatRoomId());
        // 채팅 알림은 벨 알림을 사용하지 않으므로 notificationService 호출하지 않음
        verify(notificationService, never()).createNotification(any(NotificationDto.CreateRequest.class));
    }
}