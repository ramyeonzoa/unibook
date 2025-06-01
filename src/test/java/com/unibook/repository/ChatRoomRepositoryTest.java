package com.unibook.repository;

import com.unibook.domain.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatRoomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    // 헬퍼 메서드: BaseEntity audit 필드 설정
    private void setAuditFields(BaseEntity entity) {
        entity.setCreatedAt(java.time.LocalDateTime.now());
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        entity.setCreatedBy(BaseEntity.SYSTEM_USER_ID);
        entity.setUpdatedBy(BaseEntity.SYSTEM_USER_ID);
    }

    private User buyer;
    private User seller;
    private User anotherUser;
    private Post post;
    private ChatRoom activeChatRoom;
    private ChatRoom completedChatRoom;
    private ChatRoom deletedChatRoom;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        School school = School.builder()
                .schoolName("테스트대학교")
                .primaryDomain("test.ac.kr")
                .build();
        setAuditFields(school);
        entityManager.persistAndFlush(school);

        Department department = Department.builder()
                .departmentName("컴퓨터공학과")
                .school(school)
                .build();
        setAuditFields(department);
        entityManager.persistAndFlush(department);

        // 사용자들 생성
        buyer = User.builder()
                .email("buyer@test.ac.kr")
                .password("password123!")
                .name("구매자")
                .phoneNumber("010-1234-5678")
                .department(department)
                .verified(true)
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        setAuditFields(buyer);
        entityManager.persistAndFlush(buyer);

        seller = User.builder()
                .email("seller@test.ac.kr")
                .password("password123!")
                .name("판매자")
                .phoneNumber("010-9876-5432")
                .department(department)
                .verified(true)
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        setAuditFields(seller);
        entityManager.persistAndFlush(seller);

        anotherUser = User.builder()
                .email("another@test.ac.kr")
                .password("password123!")
                .name("다른사용자")
                .phoneNumber("010-5555-5555")
                .department(department)
                .verified(true)
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        setAuditFields(anotherUser);
        entityManager.persistAndFlush(anotherUser);

        // 게시글 생성
        post = Post.builder()
                .title("테스트 교재")
                .description("테스트 설명")
                .price(15000)
                .productType(Post.ProductType.TEXTBOOK)
                .status(Post.PostStatus.AVAILABLE)
                .transactionMethod(Post.TransactionMethod.DIRECT)
                .campusLocation("중앙캠퍼스")
                .user(seller)
                .viewCount(0)
                .build();
        setAuditFields(post);
        entityManager.persistAndFlush(post);

        // 다양한 상태의 채팅방들 생성
        activeChatRoom = ChatRoom.builder()
                .buyer(buyer)
                .seller(seller)
                .post(post)
                .status(ChatRoom.ChatRoomStatus.ACTIVE)
                .buyerUnreadCount(2)
                .sellerUnreadCount(1)
                .lastMessage("안녕하세요")
                .build();
        setAuditFields(activeChatRoom);
        entityManager.persistAndFlush(activeChatRoom);
        activeChatRoom.setActualFirebaseRoomId(); // ID가 생성된 후 Firebase Room ID 설정
        entityManager.persistAndFlush(activeChatRoom);

        completedChatRoom = ChatRoom.builder()
                .buyer(anotherUser)
                .seller(seller)
                .post(post)
                .status(ChatRoom.ChatRoomStatus.COMPLETED)
                .buyerUnreadCount(0)
                .sellerUnreadCount(0)
                .lastMessage("거래 완료되었습니다")
                .build();
        setAuditFields(completedChatRoom);
        entityManager.persistAndFlush(completedChatRoom);
        completedChatRoom.setActualFirebaseRoomId(); // ID가 생성된 후 Firebase Room ID 설정
        entityManager.persistAndFlush(completedChatRoom);

        deletedChatRoom = ChatRoom.builder()
                .buyer(buyer)
                .seller(seller)
                .post(post)
                .status(ChatRoom.ChatRoomStatus.DELETED)
                .buyerUnreadCount(0)
                .sellerUnreadCount(0)
                .lastMessage("게시글이 삭제되어 채팅이 종료되었습니다.")
                .build();
        setAuditFields(deletedChatRoom);
        entityManager.persistAndFlush(deletedChatRoom);
        deletedChatRoom.setActualFirebaseRoomId(); // ID가 생성된 후 Firebase Room ID 설정
        entityManager.persistAndFlush(deletedChatRoom);
    }

    @Test
    @DisplayName("특정 사용자의 채팅방 목록 조회 시 DELETED 상태는 제외되어야 한다")
    void findByUserIdOrderByLastMessageTimeDesc_ShouldExcludeDeletedChatRooms() {
        // When
        List<ChatRoom> buyerChatRooms = chatRoomRepository.findByUserIdOrderByLastMessageTimeDesc(buyer.getUserId());
        List<ChatRoom> sellerChatRooms = chatRoomRepository.findByUserIdOrderByLastMessageTimeDesc(seller.getUserId());

        // Then
        // 구매자: ACTIVE(1개) + DELETED(1개) 중 ACTIVE만 반환
        assertEquals(1, buyerChatRooms.size());
        assertEquals(ChatRoom.ChatRoomStatus.ACTIVE, buyerChatRooms.get(0).getStatus());
        assertEquals(activeChatRoom.getChatRoomId(), buyerChatRooms.get(0).getChatRoomId());

        // 판매자: ACTIVE(1개) + COMPLETED(1개) + DELETED(1개) 중 ACTIVE, COMPLETED만 반환
        assertEquals(2, sellerChatRooms.size());
        sellerChatRooms.forEach(chatRoom -> 
                assertNotEquals(ChatRoom.ChatRoomStatus.DELETED, chatRoom.getStatus()));
        
        // DELETED 상태의 채팅방이 포함되지 않았는지 확인
        boolean hasDeletedChatRoom = sellerChatRooms.stream()
                .anyMatch(cr -> cr.getChatRoomId().equals(deletedChatRoom.getChatRoomId()));
        assertFalse(hasDeletedChatRoom);
    }

    @Test
    @DisplayName("구매자 읽지 않은 메시지 수 계산 시 DELETED 상태는 제외되어야 한다")
    void getBuyerUnreadCountByUserId_ShouldExcludeDeletedChatRooms() {
        // When
        Long buyerUnreadCount = chatRoomRepository.getBuyerUnreadCountByUserId(buyer.getUserId());

        // Then
        // ACTIVE 채팅방의 읽지 않은 수(2)만 포함, DELETED 채팅방은 제외
        assertEquals(2L, buyerUnreadCount);
    }

    @Test
    @DisplayName("판매자 읽지 않은 메시지 수 계산 시 DELETED 상태는 제외되어야 한다")
    void getSellerUnreadCountByUserId_ShouldExcludeDeletedChatRooms() {
        // When
        Long sellerUnreadCount = chatRoomRepository.getSellerUnreadCountByUserId(seller.getUserId());

        // Then
        // ACTIVE(1) + COMPLETED(0) = 1, DELETED는 제외
        assertEquals(1L, sellerUnreadCount);
    }

    @Test
    @DisplayName("특정 채팅방 상세 조회 시 DELETED 상태는 조회되지 않아야 한다")
    void findByIdAndUserId_ShouldNotFindDeletedChatRoom() {
        // When & Then
        // ACTIVE 채팅방은 조회 가능
        Optional<ChatRoom> activeResult = chatRoomRepository.findByIdAndUserId(
                activeChatRoom.getChatRoomId(), buyer.getUserId());
        assertTrue(activeResult.isPresent());
        assertEquals(ChatRoom.ChatRoomStatus.ACTIVE, activeResult.get().getStatus());

        // COMPLETED 채팅방도 조회 가능
        Optional<ChatRoom> completedResult = chatRoomRepository.findByIdAndUserId(
                completedChatRoom.getChatRoomId(), seller.getUserId());
        assertTrue(completedResult.isPresent());
        assertEquals(ChatRoom.ChatRoomStatus.COMPLETED, completedResult.get().getStatus());

        // DELETED 채팅방은 조회 불가
        Optional<ChatRoom> deletedResult = chatRoomRepository.findByIdAndUserId(
                deletedChatRoom.getChatRoomId(), buyer.getUserId());
        assertFalse(deletedResult.isPresent());
    }

    @Test
    @DisplayName("Firebase Room ID로 채팅방 조회는 상태와 무관하게 동작해야 한다")
    void findByFirebaseRoomId_ShouldFindChatRoomRegardlessOfStatus() {
        // When & Then
        // ACTIVE 채팅방 조회
        Optional<ChatRoom> activeResult = chatRoomRepository.findByFirebaseRoomId(
                activeChatRoom.getFirebaseRoomId());
        assertTrue(activeResult.isPresent());
        assertEquals(ChatRoom.ChatRoomStatus.ACTIVE, activeResult.get().getStatus());

        // DELETED 채팅방도 조회 가능 (Firebase 연동을 위해)
        Optional<ChatRoom> deletedResult = chatRoomRepository.findByFirebaseRoomId(
                deletedChatRoom.getFirebaseRoomId());
        assertTrue(deletedResult.isPresent());
        assertEquals(ChatRoom.ChatRoomStatus.DELETED, deletedResult.get().getStatus());
    }

    @Test
    @DisplayName("게시글의 모든 채팅방 조회는 상태와 무관하게 동작해야 한다 (게시글 삭제용)")
    void findAllByPostId_ShouldFindAllChatRoomsRegardlessOfStatus() {
        // When
        List<ChatRoom> allChatRooms = chatRoomRepository.findAllByPostId(post.getPostId());

        // Then
        // 모든 상태의 채팅방이 조회되어야 함 (ACTIVE, COMPLETED, DELETED)
        assertEquals(3, allChatRooms.size());
        
        boolean hasActive = allChatRooms.stream()
                .anyMatch(cr -> cr.getStatus() == ChatRoom.ChatRoomStatus.ACTIVE);
        boolean hasCompleted = allChatRooms.stream()
                .anyMatch(cr -> cr.getStatus() == ChatRoom.ChatRoomStatus.COMPLETED);
        boolean hasDeleted = allChatRooms.stream()
                .anyMatch(cr -> cr.getStatus() == ChatRoom.ChatRoomStatus.DELETED);
        
        assertTrue(hasActive);
        assertTrue(hasCompleted);
        assertTrue(hasDeleted);
    }

    @Test
    @DisplayName("게시글의 활성 채팅방 목록 조회 시 DELETED 상태는 제외되어야 한다")
    void findByPostIdOrderByCreatedAtDesc_ShouldExcludeDeletedChatRooms() {
        // When
        List<ChatRoom> activeChatRooms = chatRoomRepository.findByPostIdOrderByCreatedAtDesc(post.getPostId());

        // Then
        // ACTIVE, COMPLETED는 포함, DELETED는 제외
        assertEquals(2, activeChatRooms.size());
        activeChatRooms.forEach(chatRoom -> 
                assertNotEquals(ChatRoom.ChatRoomStatus.DELETED, chatRoom.getStatus()));
        
        boolean hasDeletedChatRoom = activeChatRooms.stream()
                .anyMatch(cr -> cr.getChatRoomId().equals(deletedChatRoom.getChatRoomId()));
        assertFalse(hasDeletedChatRoom);
    }

    @Test
    @DisplayName("읽지 않은 메시지가 없는 사용자의 경우 0을 반환해야 한다")
    void getUnreadCountByUserId_WithNoUnreadMessages_ShouldReturnZero() {
        // Given
        // 읽지 않은 메시지가 없는 새로운 사용자 생성
        User newUser = User.builder()
                .email("newuser@test.ac.kr")
                .password("password123!")
                .name("신규사용자")
                .phoneNumber("010-1111-1111")
                .department(buyer.getDepartment())
                .verified(true)
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        setAuditFields(newUser);
        entityManager.persistAndFlush(newUser);

        // When
        Long buyerUnreadCount = chatRoomRepository.getBuyerUnreadCountByUserId(newUser.getUserId());
        Long sellerUnreadCount = chatRoomRepository.getSellerUnreadCountByUserId(newUser.getUserId());

        // Then
        assertEquals(0L, buyerUnreadCount);
        assertEquals(0L, sellerUnreadCount);
    }
}