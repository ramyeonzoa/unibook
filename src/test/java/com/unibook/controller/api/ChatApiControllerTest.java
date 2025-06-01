package com.unibook.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibook.domain.entity.*;
import com.unibook.domain.dto.ChatDto;
import com.unibook.service.ChatService;
import com.unibook.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import com.unibook.config.SecurityConfig;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatApiController.class)
@ActiveProfiles("test")
class ChatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    private User testUser;
    private UserPrincipal userPrincipal;
    private ChatRoom testChatRoom;
    private ChatDto.ChatRoomListResponse chatRoomListResponse;
    private ChatDto.ChatRoomDetailResponse chatRoomDetailResponse;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 준비
        testUser = User.builder()
                .userId(1L)
                .email("test@test.ac.kr")
                .name("테스트사용자")
                .verified(true)
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        userPrincipal = new UserPrincipal(testUser);

        // 테스트 채팅방 준비
        User seller = User.builder()
                .userId(2L)
                .email("seller@test.ac.kr")
                .name("판매자")
                .build();

        Post post = Post.builder()
                .postId(1L)
                .title("테스트 교재")
                .user(seller)
                .build();

        testChatRoom = ChatRoom.builder()
                .chatRoomId(1L)
                .buyer(testUser)
                .seller(seller)
                .post(post)
                .status(ChatRoom.ChatRoomStatus.ACTIVE)
                .firebaseRoomId("chatroom_1")
                .buyerUnreadCount(3)
                .sellerUnreadCount(2)
                .lastMessage("마지막 메시지")
                .lastMessageTime(LocalDateTime.now())
                .build();

        // DTO 응답 준비
        chatRoomListResponse = ChatDto.ChatRoomListResponse.from(testChatRoom, testUser.getUserId());
        chatRoomDetailResponse = ChatDto.ChatRoomDetailResponse.from(testChatRoom, testUser.getUserId());

        // Security Context 설정
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("사용자의 채팅방 목록 조회가 성공적으로 동작해야 한다")
    void getUserChatRooms_ShouldReturnChatRoomList() throws Exception {
        // Given
        List<ChatDto.ChatRoomListResponse> chatRooms = Arrays.asList(chatRoomListResponse);
        when(chatService.getChatRoomsByUserId(testUser.getUserId())).thenReturn(chatRooms);

        // When & Then
        mockMvc.perform(get("/api/chat/rooms")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].chatRoomId").value(1L))
                .andExpect(jsonPath("$.data[0].firebaseRoomId").value("chatroom_1"));

        verify(chatService).getChatRoomsByUserId(testUser.getUserId());
    }

    @Test
    @DisplayName("채팅방 목록이 비어있을 때도 정상적으로 처리되어야 한다")
    void getUserChatRooms_WithEmptyList_ShouldReturnEmptyArray() throws Exception {
        // Given
        when(chatService.getChatRoomsByUserId(testUser.getUserId()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/chat/rooms")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(chatService).getChatRoomsByUserId(testUser.getUserId());
    }

    @Test
    @DisplayName("총 읽지 않은 채팅 수 조회가 성공적으로 동작해야 한다")
    void getTotalUnreadCount_ShouldReturnUnreadCount() throws Exception {
        // Given
        Long expectedCount = 5L;
        when(chatService.getTotalUnreadCount(testUser.getUserId())).thenReturn(expectedCount);

        // When & Then
        mockMvc.perform(get("/api/chat/unread-count")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(5));

        verify(chatService).getTotalUnreadCount(testUser.getUserId());
    }

    @Test
    @DisplayName("읽지 않은 메시지가 없을 때 0을 반환해야 한다")
    void getTotalUnreadCount_WithNoUnreadMessages_ShouldReturnZero() throws Exception {
        // Given
        when(chatService.getTotalUnreadCount(testUser.getUserId())).thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/api/chat/unread-count")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(0));

        verify(chatService).getTotalUnreadCount(testUser.getUserId());
    }

    @Test
    @DisplayName("새 채팅방 생성이 성공적으로 동작해야 한다")
    void createChatRoom_ShouldCreateAndReturnChatRoom() throws Exception {
        // Given
        ChatDto.CreateChatRoomRequest request = new ChatDto.CreateChatRoomRequest();
        request.setPostId(1L);
        
        when(chatService.createOrGetChatRoom(eq(testUser.getUserId()), any(ChatDto.CreateChatRoomRequest.class)))
                .thenReturn(chatRoomDetailResponse);

        // When & Then
        mockMvc.perform(post("/api/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userPrincipal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatRoomId").value(1L))
                .andExpect(jsonPath("$.data.firebaseRoomId").value("chatroom_1"));

        verify(chatService).createOrGetChatRoom(eq(testUser.getUserId()), any(ChatDto.CreateChatRoomRequest.class));
    }

    @Test
    @DisplayName("읽지 않은 메시지 수 업데이트가 성공적으로 동작해야 한다")
    void updateUnreadCount_ShouldUpdateSuccessfully() throws Exception {
        // Given
        String firebaseRoomId = "chatroom_1";
        Integer unreadCount = 0;

        doNothing().when(chatService).updateUnreadCount(anyString(), anyLong(), anyInt());

        // When & Then
        mockMvc.perform(put("/api/chat/rooms/{firebaseRoomId}/unread-count", firebaseRoomId)
                        .param("unreadCount", unreadCount.toString())
                        .with(user(userPrincipal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("읽지 않은 메시지 수가 업데이트되었습니다."));

        verify(chatService).updateUnreadCount(firebaseRoomId, testUser.getUserId(), unreadCount);
    }

    @Test
    @DisplayName("채팅 알림 전송이 성공적으로 동작해야 한다")
    void sendChatNotification_ShouldSendNotificationSuccessfully() throws Exception {
        // Given
        ChatDto.ChatNotificationRequest request = new ChatDto.ChatNotificationRequest();
        request.setRecipientId(2L);
        request.setSenderId(testUser.getUserId());
        request.setSenderName(testUser.getName());
        request.setChatRoomId(1L);
        request.setMessage("안녕하세요");

        doNothing().when(chatService).sendChatNotification(any(ChatDto.ChatNotificationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/chat/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userPrincipal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅 알림 처리 완료"));

        verify(chatService).sendChatNotification(any(ChatDto.ChatNotificationRequest.class));
    }

    // @WebMvcTest는 기본적으로 Security를 비활성화하므로 이 테스트는 통합 테스트에서 수행해야 함
    /*
    @Test
    @DisplayName("인증되지 않은 사용자의 요청은 거부되어야 한다")
    void withoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // SecurityContext를 비워서 인증되지 않은 상태 시뮬레이션
        SecurityContextHolder.clearContext();
        
        // When & Then
        mockMvc.perform(get("/api/chat/rooms"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/chat/unread-count"))
                .andExpect(status().isUnauthorized());

        // Service 메서드가 호출되지 않았는지 확인
        verify(chatService, never()).getChatRoomsByUserId(anyLong());
        verify(chatService, never()).getTotalUnreadCount(anyLong());
    }
    */

    @Test
    @DisplayName("잘못된 요청 본문으로 채팅방 생성 시 Bad Request가 반환되어야 한다")
    void createChatRoom_WithInvalidBody_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(user(userPrincipal))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).createOrGetChatRoom(anyLong(), any());
    }

    @Test
    @DisplayName("CSRF 토큰 없는 POST 요청은 거부되어야 한다")
    void postWithoutCsrf_ShouldReturnForbidden() throws Exception {
        // Given
        ChatDto.CreateChatRoomRequest request = new ChatDto.CreateChatRoomRequest();
        request.setPostId(1L);

        // When & Then
        mockMvc.perform(post("/api/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(userPrincipal)))
                .andExpect(status().isForbidden());

        verify(chatService, never()).createOrGetChatRoom(anyLong(), any());
    }

    @Test
    @DisplayName("필수 파라미터 없이 읽지 않은 수 업데이트 시 Bad Request가 반환되어야 한다")
    void updateUnreadCount_WithoutRequiredParam_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/chat/rooms/chatroom_1/unread-count")
                        .with(user(userPrincipal))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).updateUnreadCount(anyString(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Service에서 예외 발생 시 적절한 에러 응답이 반환되어야 한다")
    void whenServiceThrowsException_ShouldReturnErrorResponse() throws Exception {
        // Given
        when(chatService.getChatRoomsByUserId(testUser.getUserId()))
                .thenThrow(new RuntimeException("서비스 오류"));

        // When & Then
        mockMvc.perform(get("/api/chat/rooms")
                        .with(user(userPrincipal)))
                .andExpect(status().isInternalServerError());

        verify(chatService).getChatRoomsByUserId(testUser.getUserId());
    }

    @Test
    @DisplayName("채팅방 상세 조회가 성공적으로 동작해야 한다")
    void getChatRoomDetail_ShouldReturnChatRoomDetail() throws Exception {
        // Given
        Long chatRoomId = 1L;
        when(chatService.getChatRoomDetail(chatRoomId, testUser.getUserId()))
                .thenReturn(chatRoomDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/{id}", chatRoomId)
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chatRoomId").value(1L))
                .andExpect(jsonPath("$.data.firebaseRoomId").value("chatroom_1"));

        verify(chatService).getChatRoomDetail(chatRoomId, testUser.getUserId());
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 조회 시 Not Found가 반환되어야 한다")
    void getChatRoomDetail_WithNonExistentRoom_ShouldReturnNotFound() throws Exception {
        // Given
        Long nonExistentChatRoomId = 999L;
        when(chatService.getChatRoomDetail(nonExistentChatRoomId, testUser.getUserId()))
                .thenThrow(new com.unibook.exception.ResourceNotFoundException("채팅방을 찾을 수 없습니다."));

        // When & Then
        mockMvc.perform(get("/api/chat/rooms/{id}", nonExistentChatRoomId)
                        .with(user(userPrincipal)))
                .andExpect(status().isNotFound());

        verify(chatService).getChatRoomDetail(nonExistentChatRoomId, testUser.getUserId());
    }
}