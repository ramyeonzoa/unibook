package com.unibook.controller.api;

import com.unibook.controller.dto.ApiResponse;
import com.unibook.domain.dto.ChatDto;
import com.unibook.domain.entity.ChatRoom;
import com.unibook.security.UserPrincipal;
import com.unibook.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatApiController {
    
    private final ChatService chatService;
    
    /**
     * 채팅방 생성 또는 기존 채팅방 반환
     */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatDto.ChatRoomDetailResponse>> createOrGetChatRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ChatDto.CreateChatRoomRequest request) {
        
        ChatDto.ChatRoomDetailResponse response = chatService.createOrGetChatRoom(
            userPrincipal.getUserId(), request);
        
        return ResponseEntity.ok(ApiResponse.success("채팅방이 생성되었습니다.", response));
    }
    
    /**
     * 내 채팅방 목록 조회
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatDto.ChatRoomListResponse>>> getMyChatRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<ChatDto.ChatRoomListResponse> chatRooms = chatService.getChatRoomsByUserId(
            userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("채팅방 목록을 조회했습니다.", chatRooms));
    }
    
    /**
     * 채팅방 상세 조회
     */
    @GetMapping("/rooms/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatDto.ChatRoomDetailResponse>> getChatRoomDetail(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ChatDto.ChatRoomDetailResponse response = chatService.getChatRoomDetail(
            chatRoomId, userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("채팅방 정보를 조회했습니다.", response));
    }
    
    /**
     * Firebase Room ID로 채팅방 조회
     */
    @GetMapping("/rooms/firebase/{firebaseRoomId}")
    public ResponseEntity<ApiResponse<ChatDto.ChatRoomDetailResponse>> getChatRoomByFirebaseId(
            @PathVariable String firebaseRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ChatDto.ChatRoomDetailResponse response = chatService.getChatRoomByFirebaseId(
            firebaseRoomId, userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("채팅방 정보를 조회했습니다.", response));
    }
    
    /**
     * Firebase Room ID로 채팅방 기본 정보 조회 (글로벌 리스너용)
     */
    @GetMapping("/rooms/by-firebase-id/{firebaseRoomId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatRoomIdByFirebaseId(
            @PathVariable String firebaseRoomId) {
        
        // 글로벌 리스너에서는 인증 없이도 기본 정보 조회 가능
        ChatRoom chatRoom = chatService.getChatRoomByFirebaseIdWithoutAuth(firebaseRoomId);
        
        Map<String, Object> result = Map.of(
            "chatRoomId", chatRoom.getChatRoomId(),
            "firebaseRoomId", chatRoom.getFirebaseRoomId()
        );
        
        return ResponseEntity.ok(ApiResponse.success("채팅방 정보를 조회했습니다.", result));
    }
    
    /**
     * 게시글별 채팅방 목록 (판매자용)
     */
    @GetMapping("/rooms/post/{postId}")
    public ResponseEntity<ApiResponse<List<ChatDto.ChatRoomListResponse>>> getChatRoomsByPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<ChatDto.ChatRoomListResponse> chatRooms = chatService.getChatRoomsByPostId(
            postId, userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("게시글 채팅방 목록을 조회했습니다.", chatRooms));
    }
    
    /**
     * 마지막 메시지 정보 업데이트 (Firebase에서 호출)
     */
    @PutMapping("/rooms/{firebaseRoomId}/last-message")
    public ResponseEntity<ApiResponse<Void>> updateLastMessage(
            @PathVariable String firebaseRoomId,
            @RequestParam String lastMessage,
            @RequestParam String timestamp) {
        
        // ISO 8601 형식의 timestamp를 LocalDateTime으로 변환
        LocalDateTime dateTime;
        try {
            // Z를 +00:00으로 변경하고 LocalDateTime으로 파싱
            String localTimestamp = timestamp.replace("Z", "");
            if (localTimestamp.contains(".")) {
                // 밀리초 제거
                localTimestamp = localTimestamp.substring(0, localTimestamp.indexOf('.'));
            }
            dateTime = LocalDateTime.parse(localTimestamp);
        } catch (Exception e) {
            // 파싱 실패 시 현재 시간 사용
            log.error("Timestamp 파싱 실패: {}", timestamp, e);
            dateTime = LocalDateTime.now();
        }
        
        chatService.updateLastMessage(firebaseRoomId, lastMessage, dateTime);
        
        return ResponseEntity.ok(ApiResponse.<Void>success("마지막 메시지가 업데이트되었습니다.", null));
    }
    
    /**
     * 읽지 않은 메시지 수 업데이트
     */
    @PutMapping("/rooms/{firebaseRoomId}/unread-count")
    public ResponseEntity<ApiResponse<Void>> updateUnreadCount(
            @PathVariable String firebaseRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Integer unreadCount) {
        
        chatService.updateUnreadCount(firebaseRoomId, userPrincipal.getUserId(), unreadCount);
        
        return ResponseEntity.ok(ApiResponse.<Void>success("읽지 않은 메시지 수가 업데이트되었습니다.", null));
    }
    
    /**
     * 총 읽지 않은 메시지 수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getTotalUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long unreadCount = chatService.getTotalUnreadCount(userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("읽지 않은 메시지 수를 조회했습니다.", unreadCount));
    }
    
    /**
     * 채팅방 상태 변경
     */
    @PutMapping("/rooms/{chatRoomId}/status")
    public ResponseEntity<ApiResponse<Void>> updateChatRoomStatus(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam ChatRoom.ChatRoomStatus status) {
        
        chatService.updateChatRoomStatus(chatRoomId, userPrincipal.getUserId(), status);
        
        return ResponseEntity.ok(ApiResponse.<Void>success("채팅방 상태가 변경되었습니다.", null));
    }
    
    /**
     * 상대방의 읽지 않은 메시지 수 증가
     */
    @PostMapping("/rooms/{firebaseRoomId}/increment-unread")
    public ResponseEntity<ApiResponse<Void>> incrementOtherUserUnreadCount(
            @PathVariable String firebaseRoomId,
            @RequestBody(required = false) Map<String, String> requestBody,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        String currentMessage = null;
        if (requestBody != null && requestBody.containsKey("currentMessage")) {
            currentMessage = requestBody.get("currentMessage");
        }
        
        chatService.incrementOtherUserUnreadCount(firebaseRoomId, userPrincipal.getUserId(), currentMessage);
        
        return ResponseEntity.ok(ApiResponse.<Void>success("상대방의 읽지 않은 메시지 수가 증가되었습니다.", null));
    }
    
    /**
     * 채팅방 삭제 (비활성화)
     */
    @DeleteMapping("/rooms/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> deleteChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        chatService.deleteChatRoom(chatRoomId, userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.<Void>success("채팅방이 삭제되었습니다.", null));
    }
    
    /**
     * 채팅 알림 전송 (브라우저에서 호출)
     */
    @PostMapping("/notify")
    public ResponseEntity<ApiResponse<Void>> sendChatNotification(
            @RequestBody ChatDto.ChatNotificationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // 발신자 확인 (현재 로그인 사용자가 발신자여야 함)
        if (!userPrincipal.getUserId().equals(request.getSenderId())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<Void>error("잘못된 요청입니다."));
        }
        
        // 채팅 알림 전송
        chatService.sendChatNotification(request);
        
        return ResponseEntity.ok(ApiResponse.<Void>success("채팅 알림 처리 완료", null));
    }
    
    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회
     */
    @GetMapping("/rooms/{chatRoomId}/unread-count")
    public ResponseEntity<ApiResponse<Integer>> getChatRoomUnreadCount(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Integer unreadCount = chatService.getChatRoomUnreadCount(chatRoomId, userPrincipal.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success("채팅방 읽지 않은 메시지 수를 조회했습니다.", unreadCount));
    }
}