package com.unibook.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibook.domain.dto.NotificationDto;
import com.unibook.domain.entity.User;
import com.unibook.security.UserPrincipal;
import com.unibook.service.NotificationEmitterService;
import com.unibook.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationApiController 통합 테스트
 */
@WebMvcTest(NotificationApiController.class)
class NotificationApiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private NotificationService notificationService;
    
    @MockBean
    private NotificationEmitterService emitterService;
    
    private UserPrincipal testPrincipal;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .email("test@university.ac.kr")
                .password("encodedPassword")
                .name("테스트 사용자")
                .phoneNumber("010-1234-5678")
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .verified(true)
                .build();
        
        testPrincipal = new UserPrincipal(testUser);
    }
    
    @Test
    @DisplayName("SSE 연결 생성")
    @WithMockUser
    void createSseConnection() throws Exception {
        // given
        SseEmitter mockEmitter = new SseEmitter();
        given(emitterService.createEmitter(anyLong())).willReturn(mockEmitter);
        
        // when & then
        mockMvc.perform(get("/api/notifications/stream")
                .with(user(testPrincipal))
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
        
        verify(emitterService).createEmitter(1L);
    }
    
    @Test
    @DisplayName("알림 목록 조회")
    @WithMockUser
    void getNotifications() throws Exception {
        // given
        NotificationDto.Response notification = NotificationDto.Response.builder()
                .notificationId(1L)
                .type("POST_WISHLISTED")
                .title("게시글이 찜되었습니다")
                .content("누군가 회원님의 게시글을 찜했습니다.")
                .url("/posts/1")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Page<NotificationDto.Response> notificationPage = new PageImpl<>(
            List.of(notification), PageRequest.of(0, 20), 1
        );
        
        given(notificationService.getNotifications(eq(1L), any(Pageable.class)))
                .willReturn(notificationPage);
        
        // when & then
        mockMvc.perform(get("/api/notifications")
                .with(user(testPrincipal))
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data.content[0].notificationId").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("POST_WISHLISTED"))
                .andExpect(jsonPath("$.data.content[0].title").value("게시글이 찜되었습니다"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
    }
    
    @Test
    @DisplayName("읽지 않은 알림 목록 조회")
    @WithMockUser
    void getUnreadNotifications() throws Exception {
        // given
        NotificationDto.Response unreadNotification = NotificationDto.Response.builder()
                .notificationId(2L)
                .type("WISHLIST_STATUS_CHANGED")
                .title("찜한 게시글 상태 변경")
                .content("찜한 게시글이 예약중으로 변경되었습니다.")
                .url("/posts/2")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Page<NotificationDto.Response> unreadPage = new PageImpl<>(
            List.of(unreadNotification), PageRequest.of(0, 10), 1
        );
        
        given(notificationService.getUnreadNotifications(eq(1L), eq(10)))
                .willReturn(unreadPage);
        
        // when & then
        mockMvc.perform(get("/api/notifications/unread")
                .with(user(testPrincipal))
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("읽지 않은 알림을 조회했습니다."))
                .andExpect(jsonPath("$.data.content[0].type").value("WISHLIST_STATUS_CHANGED"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
    }
    
    @Test
    @DisplayName("알림 카운트 조회")
    @WithMockUser
    void getNotificationCount() throws Exception {
        // given
        NotificationDto.CountResponse countResponse = NotificationDto.CountResponse.builder()
                .totalCount(15)
                .unreadCount(5)
                .build();
        
        given(notificationService.getNotificationCount(1L)).willReturn(countResponse);
        
        // when & then
        mockMvc.perform(get("/api/notifications/count")
                .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 카운트를 조회했습니다."))
                .andExpect(jsonPath("$.data.totalCount").value(15))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }
    
    @Test
    @DisplayName("특정 알림 읽음 처리 - 성공")
    @WithMockUser
    void markAsRead_Success() throws Exception {
        // given
        Long notificationId = 1L;
        given(notificationService.markAsRead(notificationId, 1L)).willReturn(true);
        given(notificationService.getNotificationCount(1L)).willReturn(
            NotificationDto.CountResponse.builder()
                    .totalCount(10)
                    .unreadCount(4)
                    .build()
        );
        
        // when & then
        mockMvc.perform(post("/api/notifications/{notificationId}/read", notificationId)
                .with(user(testPrincipal))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림을 읽음으로 표시했습니다."));
        
        verify(emitterService).sendCountUpdateToUser(eq(1L), any(NotificationDto.CountResponse.class));
    }
    
    @Test
    @DisplayName("특정 알림 읽음 처리 - 실패")
    @WithMockUser
    void markAsRead_NotFound() throws Exception {
        // given
        Long notificationId = 999L;
        given(notificationService.markAsRead(notificationId, 1L)).willReturn(false);
        
        // when & then
        mockMvc.perform(post("/api/notifications/{notificationId}/read", notificationId)
                .with(user(testPrincipal))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."));
    }
    
    @Test
    @DisplayName("모든 알림 읽음 처리")
    @WithMockUser
    void markAllAsRead() throws Exception {
        // given
        given(notificationService.markAllAsRead(1L)).willReturn(7);
        given(notificationService.getNotificationCount(1L)).willReturn(
            NotificationDto.CountResponse.builder()
                    .totalCount(10)
                    .unreadCount(0)
                    .build()
        );
        
        // when & then
        mockMvc.perform(post("/api/notifications/read-all")
                .with(user(testPrincipal))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("7개의 알림을 읽음으로 표시했습니다."))
                .andExpect(jsonPath("$.data").value(7));
        
        verify(emitterService).sendCountUpdateToUser(eq(1L), any(NotificationDto.CountResponse.class));
    }
    
    @Test
    @DisplayName("연결 정보 조회")
    @WithMockUser
    void getConnectionCount() throws Exception {
        // given
        Map<String, Object> connectionInfo = Map.of(
            "totalUsers", 5,
            "totalConnections", 8,
            "userConnections", Map.of(1L, 2, 2L, 3, 3L, 1, 4L, 1, 5L, 1)
        );
        
        given(emitterService.getConnectionInfo()).willReturn(connectionInfo);
        
        // when & then
        mockMvc.perform(get("/api/notifications/connections")
                .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("연결 정보를 조회했습니다."))
                .andExpect(jsonPath("$.data.totalUsers").value(5))
                .andExpect(jsonPath("$.data.totalConnections").value(8));
    }
    
    @Test
    @DisplayName("인증되지 않은 사용자 접근 차단")
    void unauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/notifications/stream"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(post("/api/notifications/1/read"))
                .andExpect(status().isForbidden()); // CSRF 토큰 없음
    }
}