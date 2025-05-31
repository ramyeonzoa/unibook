package com.unibook.service;

import com.unibook.domain.entity.*;
import com.unibook.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 찜한 게시글 상태 변경 알림 간단한 통합 테스트
 * 
 * 주의: @Transactional을 제거하여 실제 DB 커밋이 일어나도록 함
 * 이는 @Async 메서드가 별도 트랜잭션에서 실행되기 때문
 */
@SpringBootTest
@ActiveProfiles("test")
class PostServiceWishlistNotificationSimpleTest {

    @Autowired
    private PostService postService;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WishlistRepository wishlistRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @MockBean
    private NotificationEmitterService notificationEmitterService;

    private User author;
    private User wishlistUser;
    private Post post;

    @BeforeEach
    void setUp() {
        // 기존 테스트 데이터 정리
        cleanupTestData();
        
        // 테스트 데이터 준비
        author = createUser("author@test.com", "작성자");
        wishlistUser = createUser("wisher@test.com", "찜한사용자");
        
        post = createPost(author, "테스트 게시글");
        
        // 찜하기 생성
        Wishlist wishlist = Wishlist.builder()
                .user(wishlistUser)
                .post(post)
                .build();
        wishlistRepository.save(wishlist);
    }
    
    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        cleanupTestData();
    }
    
    private void cleanupTestData() {
        // 순서 중요: 외래키 제약 때문에 자식 테이블부터 삭제
        notificationRepository.deleteAll();
        wishlistRepository.deleteAll();
        postRepository.deleteAll();
        // 테스트용 사용자만 삭제 (이메일로 식별)
        userRepository.findByEmail("author@test.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("wisher@test.com").ifPresent(userRepository::delete);
    }

    @Test
    void 게시글_상태_변경시_찜한_사용자에게_알림_발송() throws InterruptedException {
        // Given
        assertEquals(Post.PostStatus.AVAILABLE, post.getStatus());
        assertEquals(0, notificationRepository.count());

        // When - 게시글 상태를 RESERVED로 변경
        postService.updatePostStatus(post.getPostId(), Post.PostStatus.RESERVED);

        // Then - 비동기 처리를 위해 충분히 대기 (비동기 작업 완료 대기)
        Thread.sleep(2000);
        
        // 알림이 생성되었는지 확인
        long createdNotificationsCount = notificationRepository.count();
        assertEquals(1, createdNotificationsCount, 
                "찜한 사용자에게 알림이 1개 생성되어야 함. 실제: " + createdNotificationsCount);
        
        // 생성된 알림 내용 확인
        var notifications = notificationRepository.findByRecipientUserIdWithDetails(
                wishlistUser.getUserId(), org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        
        Notification notification = notifications.getContent().get(0);
        assertEquals(Notification.NotificationType.WISHLIST_STATUS_CHANGED, notification.getType());
        assertEquals(post.getPostId(), notification.getRelatedPost().getPostId());
        assertFalse(notification.isRead());
        assertTrue(notification.getTitle().contains("상태 변경"));
        
        // SSE 발송 확인
        verify(notificationEmitterService, times(1)).sendNotificationToUser(
                eq(wishlistUser.getUserId()), any());
    }

    @Test
    void 상태가_동일한_경우_알림_발송_안함() throws InterruptedException {
        // Given - 이미 AVAILABLE 상태
        assertEquals(Post.PostStatus.AVAILABLE, post.getStatus());

        // When - 동일한 상태로 변경 시도
        postService.updatePostStatus(post.getPostId(), Post.PostStatus.AVAILABLE);

        // Then
        Thread.sleep(500);
        assertEquals(0, notificationRepository.count());
        verify(notificationEmitterService, never()).sendNotificationToUser(anyLong(), any());
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

    private Post createPost(User author, String title) {
        Post post = Post.builder()
                .title(title)
                .description("테스트 내용")
                .productType(Post.ProductType.TEXTBOOK)
                .price(10000)
                .status(Post.PostStatus.AVAILABLE)
                .transactionMethod(Post.TransactionMethod.DIRECT)
                .campusLocation("본교")
                .user(author)
                .build();
        return postRepository.save(post);
    }
}