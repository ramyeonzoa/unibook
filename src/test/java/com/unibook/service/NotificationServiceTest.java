package com.unibook.service;

import com.unibook.domain.dto.NotificationDto;
import com.unibook.domain.entity.Notification;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.repository.NotificationRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * NotificationService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private NotificationEmitterService emitterService;
    
    @InjectMocks
    private NotificationService notificationService;
    
    private User testUser;
    private User actorUser;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .email("test@university.ac.kr")
                .name("테스트 사용자")
                .build();
                
        actorUser = User.builder()
                .userId(2L)
                .email("actor@university.ac.kr")
                .name("액터 사용자")
                .build();
                
        testPost = Post.builder()
                .postId(1L)
                .title("테스트 게시글")
                .user(testUser)
                .build();
    }
    
    @Test
    @DisplayName("동기 알림 생성 - 정상 케이스")
    void createNotification_Success() {
        // given
        NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                .recipientUserId(1L)
                .actorUserId(2L)
                .type(Notification.NotificationType.POST_WISHLISTED)
                .relatedPostId(1L)
                .title("게시글이 찜되었습니다")
                .content("누군가 회원님의 게시글을 찜했습니다.")
                .url("/posts/1")
                .build();
                
        Notification savedNotification = Notification.builder()
                .notificationId(1L)
                .recipient(testUser)
                .actor(actorUser)
                .type(Notification.NotificationType.POST_WISHLISTED)
                .relatedPost(testPost)
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .build();
                
        given(userRepository.getReferenceById(1L)).willReturn(testUser);
        given(userRepository.getReferenceById(2L)).willReturn(actorUser);
        given(postRepository.getReferenceById(1L)).willReturn(testPost);
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);
        
        // when
        NotificationDto.Response response = notificationService.createNotification(request);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getNotificationId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo("POST_WISHLISTED");
        assertThat(response.getTitle()).isEqualTo("게시글이 찜되었습니다");
        
        verify(emitterService).sendNotificationToUser(eq(1L), any(NotificationDto.Response.class));
    }
    
    @Test
    @DisplayName("비동기 알림 생성 - 정상 케이스")
    void createNotificationAsync_Success() throws Exception {
        // given
        NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                .recipientUserId(1L)
                .actorUserId(2L)
                .type(Notification.NotificationType.POST_WISHLISTED)
                .relatedPostId(1L)
                .title("게시글이 찜되었습니다")
                .content("누군가 회원님의 게시글을 찜했습니다.")
                .url("/posts/1")
                .build();
                
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.findById(2L)).willReturn(Optional.of(actorUser));
        given(postRepository.findById(1L)).willReturn(Optional.of(testPost));
        given(notificationRepository.save(any(Notification.class))).willReturn(
            Notification.builder()
                .notificationId(1L)
                .recipient(testUser)
                .actor(actorUser)
                .relatedPost(testPost)
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .build()
        );
        
        // when
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
            notificationService.createNotificationAsync(request)
        );
        
        // 비동기 작업 완료 대기
        future.get(5, TimeUnit.SECONDS);
        
        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(emitterService).sendNotificationToUser(eq(1L), any(NotificationDto.Response.class));
    }
    
    @Test
    @DisplayName("비동기 알림 생성 - 사용자 없음 예외 처리")
    void createNotificationAsync_UserNotFound() throws Exception {
        // given
        NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                .recipientUserId(999L)
                .type(Notification.NotificationType.POST_WISHLISTED)
                .title("게시글이 찜되었습니다")
                .build();
                
        given(userRepository.findById(999L)).willReturn(Optional.empty());
        
        // when
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
            notificationService.createNotificationAsync(request)
        );
        
        // 비동기 작업 완료 대기
        future.get(5, TimeUnit.SECONDS);
        
        // then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emitterService, never()).sendNotificationToUser(anyLong(), any());
    }
    
    @Test
    @DisplayName("알림 목록 조회")
    void getNotifications_Success() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        
        Notification notification = Notification.builder()
                .notificationId(1L)
                .recipient(testUser)
                .actor(actorUser)
                .type(Notification.NotificationType.POST_WISHLISTED)
                .title("테스트 알림")
                .isRead(false)
                .build();
                
        Page<Notification> notificationPage = new PageImpl<>(List.of(notification));
        
        given(notificationRepository.findByRecipientUserIdWithDetails(userId, pageable))
                .willReturn(notificationPage);
        
        // when
        Page<NotificationDto.Response> result = notificationService.getNotifications(userId, pageable);
        
        // then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 알림");
        assertThat(result.getContent().get(0).isRead()).isFalse();
    }
    
    @Test
    @DisplayName("읽지 않은 알림 조회")
    void getUnreadNotifications_Success() {
        // given
        Long userId = 1L;
        int limit = 10;
        Pageable pageable = PageRequest.of(0, limit);
        
        Notification unreadNotification = Notification.builder()
                .notificationId(1L)
                .recipient(testUser)
                .type(Notification.NotificationType.WISHLIST_STATUS_CHANGED)
                .title("찜한 게시글 상태 변경")
                .isRead(false)
                .build();
                
        Page<Notification> unreadPage = new PageImpl<>(List.of(unreadNotification));
        
        given(notificationRepository.findUnreadByRecipientUserIdWithDetails(userId, pageable))
                .willReturn(unreadPage);
        
        // when
        Page<NotificationDto.Response> result = notificationService.getUnreadNotifications(userId, limit);
        
        // then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isRead()).isFalse();
    }
    
    @Test
    @DisplayName("알림 카운트 조회")
    void getNotificationCount_Success() {
        // given
        Long userId = 1L;
        given(notificationRepository.countByRecipientUserId(userId)).willReturn(10L);
        given(notificationRepository.countUnreadByRecipientUserId(userId)).willReturn(3L);
        
        // when
        NotificationDto.CountResponse count = notificationService.getNotificationCount(userId);
        
        // then
        assertThat(count.getTotalCount()).isEqualTo(10L);
        assertThat(count.getUnreadCount()).isEqualTo(3L);
    }
    
    @Test
    @DisplayName("알림 읽음 처리 - 성공")
    void markAsRead_Success() {
        // given
        Long notificationId = 1L;
        Long userId = 1L;
        
        given(notificationRepository.markAsReadByIdAndUserId(notificationId, userId))
                .willReturn(1);
        
        // when
        boolean result = notificationService.markAsRead(notificationId, userId);
        
        // then
        assertThat(result).isTrue();
        verify(notificationRepository).markAsReadByIdAndUserId(notificationId, userId);
    }
    
    @Test
    @DisplayName("알림 읽음 처리 - 실패")
    void markAsRead_NotFound() {
        // given
        Long notificationId = 999L;
        Long userId = 1L;
        
        given(notificationRepository.markAsReadByIdAndUserId(notificationId, userId))
                .willReturn(0);
        
        // when
        boolean result = notificationService.markAsRead(notificationId, userId);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("모든 알림 읽음 처리")
    void markAllAsRead_Success() {
        // given
        Long userId = 1L;
        given(notificationRepository.markAllAsReadByUserId(userId)).willReturn(5);
        
        // when
        int result = notificationService.markAllAsRead(userId);
        
        // then
        assertThat(result).isEqualTo(5);
        verify(notificationRepository).markAllAsReadByUserId(userId);
    }
    
    @Test
    @DisplayName("찜한 게시글 상태 변경 알림 생성")
    void createWishlistStatusNotificationAsync_Success() throws Exception {
        // given
        Long recipientUserId = 1L;
        Long postId = 1L;
        Post.PostStatus newStatus = Post.PostStatus.RESERVED;
        
        Notification savedNotification = Notification.builder()
                .notificationId(1L)
                .recipient(testUser)
                .relatedPost(testPost)
                .type(Notification.NotificationType.WISHLIST_STATUS_CHANGED)
                .title("찜한 게시글 상태 변경")
                .content("찜한 게시글이 예약중으로 변경되었습니다.")
                .url("/posts/1")
                .build();
                
        given(userRepository.findById(recipientUserId)).willReturn(Optional.of(testUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(testPost));
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);
        
        // when
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
            notificationService.createWishlistStatusNotificationAsync(recipientUserId, postId, newStatus)
        );
        
        // 비동기 작업 완료 대기
        future.get(5, TimeUnit.SECONDS);
        
        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(Notification.NotificationType.WISHLIST_STATUS_CHANGED);
        assertThat(saved.getTitle()).isEqualTo("찜한 게시글 상태 변경");
        assertThat(saved.getContent()).isEqualTo("찜한 게시글이 예약중으로 변경되었습니다.");
        assertThat(saved.getUrl()).isEqualTo("/posts/1");
    }
    
    @Test
    @DisplayName("상태 변경 메시지 생성 - null 처리")
    void generateStatusChangeMessage_NullStatus() throws Exception {
        // given
        Long recipientUserId = 1L;
        Long postId = 1L;
        Post.PostStatus nullStatus = null;
        
        given(userRepository.findById(recipientUserId)).willReturn(Optional.of(testUser));
        given(postRepository.findById(postId)).willReturn(Optional.of(testPost));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(1L);
            return notification;
        });
        
        // when
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
            notificationService.createWishlistStatusNotificationAsync(recipientUserId, postId, nullStatus)
        );
        
        // 비동기 작업 완료 대기
        future.get(5, TimeUnit.SECONDS);
        
        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getContent()).isEqualTo("찜한 게시글의 상태가 변경되었습니다.");
    }
}