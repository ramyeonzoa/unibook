package com.unibook.service;

import com.unibook.domain.entity.*;
import com.unibook.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 게시글 찜 알림 테스트
 * 트랜잭션을 사용하지 않아 실제 DB 커밋 발생
 */
@SpringBootTest
@ActiveProfiles("test")
class PostWishlistedNotificationTest {

    @Autowired
    private WishlistService wishlistService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private WishlistRepository wishlistRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @MockBean
    private NotificationEmitterService notificationEmitterService;

    private User postOwner;
    private User wishlistUser;
    private Post post;

    @BeforeEach
    void setUp() {
        // 기존 테스트 데이터 정리
        cleanupTestData();
        
        // 테스트 데이터 생성
        postOwner = createUser("owner@test.com", "게시글 작성자");
        wishlistUser = createUser("wisher@test.com", "찜하는 사용자");
        
        post = createPost(postOwner, "테스트 교재", 15000);
    }
    
    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    @Test
    void 게시글_찜하기시_작성자에게_익명_알림_발송() throws InterruptedException {
        // Given
        assertEquals(0, post.getWishlistCount());
        assertEquals(0, notificationRepository.count());
        
        // When - 다른 사용자가 게시글을 찜함
        boolean result = wishlistService.toggleWishlist(wishlistUser.getUserId(), post.getPostId());
        
        // Then
        assertTrue(result, "찜하기가 성공해야 함");
        
        // 비동기 처리 대기
        Thread.sleep(2000);
        
        // 알림이 생성되었는지 확인
        assertEquals(1, notificationRepository.count());
        
        // 알림 내용 확인
        var notifications = notificationRepository.findByRecipientUserIdWithDetails(
                postOwner.getUserId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
        
        assertEquals(1, notifications.getTotalElements());
        Notification notification = notifications.getContent().get(0);
        
        // 익명 알림 검증
        assertEquals(Notification.NotificationType.POST_WISHLISTED, notification.getType());
        assertNull(notification.getActor(), "익명 알림이므로 actor가 없어야 함");
        assertEquals(post.getPostId(), notification.getRelatedPost().getPostId());
        assertTrue(notification.getTitle().contains("찜되었습니다"));
        assertTrue(notification.getContent().contains(post.getTitle()));
        assertFalse(notification.isRead());
        
        // SSE 발송 확인
        verify(notificationEmitterService, times(1)).sendNotificationToUser(
                eq(postOwner.getUserId()), any()
        );
    }

    @Test
    void 자신의_게시글은_찜할_수_없음() {
        // Given
        assertEquals(0, post.getWishlistCount());
        
        // When & Then - 자신의 게시글 찜하기 시도
        assertThrows(IllegalArgumentException.class, () -> {
            wishlistService.toggleWishlist(postOwner.getUserId(), post.getPostId());
        });
        
        // 찜 개수 변화 없음
        Post updatedPost = postRepository.findById(post.getPostId()).orElseThrow();
        assertEquals(0, updatedPost.getWishlistCount());
        
        // 알림도 생성되지 않음
        assertEquals(0, notificationRepository.count());
    }

    @Test
    void 찜_취소시_알림_발송_안함() throws InterruptedException {
        // Given - 먼저 찜하기
        wishlistService.toggleWishlist(wishlistUser.getUserId(), post.getPostId());
        Thread.sleep(1000);
        
        long initialNotificationCount = notificationRepository.count();
        assertEquals(1, initialNotificationCount);
        
        // When - 찜 취소
        boolean result = wishlistService.toggleWishlist(wishlistUser.getUserId(), post.getPostId());
        
        // Then
        assertFalse(result, "찜 취소여야 함");
        Thread.sleep(1000);
        
        // 추가 알림이 생성되지 않음
        assertEquals(initialNotificationCount, notificationRepository.count());
    }

    // 헬퍼 메서드
    private User createUser(String email, String name) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password("password123")
                .phoneNumber("010-1234-5678")
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .verified(true)
                .build();
        return userRepository.save(user);
    }

    private Post createPost(User author, String title, int price) {
        Post post = Post.builder()
                .title(title)
                .description("테스트용 게시글")
                .productType(Post.ProductType.TEXTBOOK)
                .price(price)
                .status(Post.PostStatus.AVAILABLE)
                .transactionMethod(Post.TransactionMethod.DIRECT)
                .campusLocation("정문")
                .user(author)
                .build();
        return postRepository.save(post);
    }
    
    private void cleanupTestData() {
        notificationRepository.deleteAll();
        wishlistRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.findByEmail("owner@test.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("wisher@test.com").ifPresent(userRepository::delete);
    }
}