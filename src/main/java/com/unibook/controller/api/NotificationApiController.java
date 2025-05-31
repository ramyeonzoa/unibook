package com.unibook.controller.api;

import com.unibook.controller.dto.ApiResponse;
import com.unibook.domain.dto.NotificationDto;
import com.unibook.security.UserPrincipal;
import com.unibook.service.NotificationEmitterService;
import com.unibook.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 알림 API 컨트롤러
 * SSE 실시간 알림 및 알림 관리 기능 제공
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final NotificationEmitterService emitterService;

    /**
     * SSE 연결 생성 (실시간 알림 수신)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createSseConnection(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return emitterService.createEmitter(userPrincipal.getUserId());
    }

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto.Response>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDto.Response> notifications = 
                notificationService.getNotifications(userPrincipal.getUserId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.success("알림 목록을 조회했습니다.", notifications));
    }

    /**
     * 읽지 않은 알림 목록 조회 (헤더 드롭다운용)
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationDto.Response>>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "10") int limit) {
        
        Page<NotificationDto.Response> notifications = 
                notificationService.getUnreadNotifications(userPrincipal.getUserId(), limit);
        
        return ResponseEntity.ok(ApiResponse.success("읽지 않은 알림을 조회했습니다.", notifications));
    }

    /**
     * 알림 카운트 조회
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<NotificationDto.CountResponse>> getNotificationCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        NotificationDto.CountResponse count = 
                notificationService.getNotificationCount(userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("알림 카운트를 조회했습니다.", count));
    }

    /**
     * 특정 알림을 읽음으로 표시
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        boolean success = notificationService.markAsRead(notificationId, userPrincipal.getUserId());
        
        if (success) {
            // 읽음 처리 후 최신 카운트 전송
            NotificationDto.CountResponse count = 
                    notificationService.getNotificationCount(userPrincipal.getUserId());
            emitterService.sendCountUpdateToUser(userPrincipal.getUserId(), count);
            
            return ResponseEntity.ok(ApiResponse.success("알림을 읽음으로 표시했습니다."));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("알림을 찾을 수 없습니다."));
        }
    }

    /**
     * 모든 알림을 읽음으로 표시
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        int updatedCount = notificationService.markAllAsRead(userPrincipal.getUserId());
        
        // 읽음 처리 후 최신 카운트 전송
        NotificationDto.CountResponse count = 
                notificationService.getNotificationCount(userPrincipal.getUserId());
        emitterService.sendCountUpdateToUser(userPrincipal.getUserId(), count);
        
        return ResponseEntity.ok(ApiResponse.success(
                updatedCount + "개의 알림을 읽음으로 표시했습니다.", updatedCount));
    }

    /**
     * 현재 연결된 SSE 사용자 수 (개발/디버깅용)
     */
    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionCount() {
        return ResponseEntity.ok(ApiResponse.success("연결 정보를 조회했습니다.", 
                emitterService.getConnectionInfo()));
    }

}