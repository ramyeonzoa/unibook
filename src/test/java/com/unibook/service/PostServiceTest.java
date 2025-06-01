package com.unibook.service;

import com.unibook.domain.entity.*;
import com.unibook.repository.ChatRoomRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.SubjectRepository;
import com.unibook.repository.BookRepository;
import com.unibook.repository.WishlistRepository;
import com.unibook.util.FileUploadUtil;
import com.unibook.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private SubjectRepository subjectRepository;
    
    @Mock
    private WishlistRepository wishlistRepository;
    
    @Mock
    private FileUploadUtil fileUploadUtil;
    
    @Mock
    private SubjectBookService subjectBookService;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private PostService postService;
    
    private Post testPost;
    private User testUser;
    private School testSchool;
    private Department testDepartment;
    private Subject testSubject;
    private Book testBook;
    private List<ChatRoom> testChatRooms;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testSchool = School.builder()
                .schoolId(1L)
                .schoolName("테스트대학교")
                .build();
        
        testDepartment = Department.builder()
                .departmentId(1L)
                .departmentName("컴퓨터공학과")
                .school(testSchool)
                .build();
        
        testUser = User.builder()
                .userId(1L)
                .email("test@test.ac.kr")
                .name("테스트사용자")
                .department(testDepartment)
                .build();
        
        testSubject = Subject.builder()
                .subjectId(1L)
                .subjectName("데이터구조")
                .type(Subject.SubjectType.MAJOR)
                .build();
        
        testBook = Book.builder()
                .bookId(1L)
                .title("테스트교재")
                .author("테스트저자")
                .publisher("테스트출판사")
                .build();
        
        testPost = Post.builder()
                .postId(1L)
                .title("테스트 게시글")
                .description("테스트 내용")
                .price(10000)
                .productType(Post.ProductType.TEXTBOOK)
                .status(Post.PostStatus.AVAILABLE)
                .transactionMethod(Post.TransactionMethod.DIRECT)
                .campusLocation("중앙캠퍼스")
                .user(testUser)
                .subject(testSubject)
                .book(testBook)
                .postImages(Collections.emptyList())
                .build();
        
        // 테스트 채팅방 준비
        User buyer1 = User.builder().userId(2L).email("buyer1@test.ac.kr").name("구매자1").build();
        User buyer2 = User.builder().userId(3L).email("buyer2@test.ac.kr").name("구매자2").build();
        
        ChatRoom chatRoom1 = ChatRoom.builder()
                .chatRoomId(1L)
                .buyer(buyer1)
                .seller(testUser)
                .post(testPost)
                .status(ChatRoom.ChatRoomStatus.ACTIVE)
                .firebaseRoomId("chatroom_1")
                .buyerUnreadCount(2)
                .sellerUnreadCount(0)
                .build();
        
        ChatRoom chatRoom2 = ChatRoom.builder()
                .chatRoomId(2L)
                .buyer(buyer2)
                .seller(testUser)
                .post(testPost)
                .status(ChatRoom.ChatRoomStatus.ACTIVE)
                .firebaseRoomId("chatroom_2")
                .buyerUnreadCount(1)
                .sellerUnreadCount(3)
                .build();
        
        testChatRooms = Arrays.asList(chatRoom1, chatRoom2);
    }
    
    @Test
    @DisplayName("게시글 삭제 시 관련 채팅방들이 DELETED 상태로 변경되어야 한다")
    void deletePost_ShouldSetChatRoomsToDeleted() {
        // Given
        Long postId = 1L;
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(chatRoomRepository.findAllByPostId(postId)).thenReturn(testChatRooms);
        
        // When
        postService.deletePost(postId);
        
        // Then
        // 1. 채팅방 조회가 호출되었는지 확인
        verify(chatRoomRepository).findAllByPostId(postId);
        
        // 2. 각 채팅방의 상태가 DELETED로 변경되었는지 확인
        testChatRooms.forEach(chatRoom -> {
            assertEquals(ChatRoom.ChatRoomStatus.DELETED, chatRoom.getStatus());
            assertEquals("게시글이 삭제되어 채팅이 종료되었습니다.", chatRoom.getLastMessage());
            assertNotNull(chatRoom.getLastMessageTime());
        });
        
        // 3. 채팅방들이 저장되었는지 확인
        verify(chatRoomRepository).saveAll(testChatRooms);
        
        // 4. 게시글이 실제로 삭제되었는지 확인
        verify(postRepository).delete(testPost);
    }
    
    @Test
    @DisplayName("게시글 삭제 시 연관된 채팅방이 없어도 정상 처리되어야 한다")
    void deletePost_WithNoChatRooms_ShouldDeletePostSuccessfully() {
        // Given
        Long postId = 1L;
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(chatRoomRepository.findAllByPostId(postId)).thenReturn(Collections.emptyList());
        
        // When
        postService.deletePost(postId);
        
        // Then
        // 1. 채팅방 조회가 호출되었는지 확인
        verify(chatRoomRepository).findAllByPostId(postId);
        
        // 2. saveAll이 호출되지 않았는지 확인 (빈 리스트이므로)
        verify(chatRoomRepository, never()).saveAll(any());
        
        // 3. 게시글이 삭제되었는지 확인
        verify(postRepository).delete(testPost);
    }
    
    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시 ResourceNotFoundException이 발생해야 한다")
    void deletePost_WithNonExistentPost_ShouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentPostId = 999L;
        
        when(postRepository.findById(nonExistentPostId)).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postService.deletePost(nonExistentPostId)
        );
        
        assertEquals("게시글을 찾을 수 없습니다.", exception.getMessage());
        
        // 채팅방 관련 로직이 실행되지 않았는지 확인
        verify(chatRoomRepository, never()).findAllByPostId(any());
        verify(postRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("SubjectBook 연결이 있는 게시글 삭제 시 참조 카운트가 감소되어야 한다")
    void deletePost_WithSubjectBook_ShouldDecrementReferenceCount() {
        // Given
        Long postId = 1L;
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(chatRoomRepository.findAllByPostId(postId)).thenReturn(Collections.emptyList());
        
        // When
        postService.deletePost(postId);
        
        // Then
        // SubjectBook 참조 카운트 감소가 호출되었는지 확인
        verify(subjectBookService).decrementPostCount(
                testSubject.getSubjectId(),
                testBook.getBookId()
        );
        
        verify(postRepository).delete(testPost);
    }
    
    @Test
    @DisplayName("Subject나 Book이 없는 게시글 삭제 시 SubjectBook 처리를 건너뛰어야 한다")
    void deletePost_WithoutSubjectOrBook_ShouldSkipSubjectBookProcessing() {
        // Given
        Long postId = 1L;
        Post postWithoutSubjectBook = Post.builder()
                .postId(1L)
                .title("테스트 게시글")
                .description("테스트 내용")
                .price(10000)
                .productType(Post.ProductType.NOTE)
                .status(Post.PostStatus.AVAILABLE)
                .transactionMethod(Post.TransactionMethod.DIRECT)
                .campusLocation("중앙캠퍼스")
                .user(testUser)
                .subject(null) // Subject 없음
                .book(null)    // Book 없음
                .postImages(Collections.emptyList())
                .build();
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(postWithoutSubjectBook));
        when(chatRoomRepository.findAllByPostId(postId)).thenReturn(Collections.emptyList());
        
        // When
        postService.deletePost(postId);
        
        // Then
        // SubjectBook 서비스가 호출되지 않았는지 확인
        verify(subjectBookService, never()).decrementPostCount(any(), any());
        
        verify(postRepository).delete(postWithoutSubjectBook);
    }
    
    @Test
    @DisplayName("채팅방 상태 변경 중 예외가 발생해도 게시글 삭제는 계속 진행되어야 한다")
    void deletePost_ChatRoomUpdateFails_ShouldContinueWithPostDeletion() {
        // Given
        Long postId = 1L;
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(chatRoomRepository.findAllByPostId(postId)).thenReturn(testChatRooms);
        when(chatRoomRepository.saveAll(any())).thenThrow(new RuntimeException("채팅방 업데이트 실패"));
        
        // When & Then
        // 예외가 발생하지만 게시글 삭제까지 진행되어야 함
        assertDoesNotThrow(() -> postService.deletePost(postId));
        
        // 채팅방 업데이트가 시도되었는지 확인
        verify(chatRoomRepository).saveAll(testChatRooms);
        
        // 예외가 발생해도 게시글 삭제는 진행되어야 함
        verify(postRepository).delete(testPost);
    }
}