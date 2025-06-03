package com.unibook.controller.api;

import com.unibook.controller.dto.ApiResponse;
import com.unibook.controller.dto.SuspensionRequest;
import com.unibook.controller.dto.UnsuspendRequest;
import com.unibook.domain.dto.AdminActionDto;
import com.unibook.domain.entity.AdminAction;
import com.unibook.security.UserPrincipal;
import com.unibook.service.AdminActionService;
import com.unibook.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminActionApiController {
    
    private final AdminActionService adminActionService;
    private final UserService userService;
    
    /**
     * 사용자 정지
     */
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @Valid @RequestBody SuspensionRequest request,
            Authentication auth) {
        
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Long adminId = principal.getUserId();
            
            adminActionService.suspendUser(
                userId, 
                request.getReason(), 
                request.getCalculatedExpiresAt(), 
                adminId
            );
            
            log.info("사용자 정지 처리 완료 - userId: {}, adminId: {}", userId, adminId);
            return ResponseEntity.ok(ApiResponse.success("사용자가 정지되었습니다.", null));
            
        } catch (IllegalStateException e) {
            log.warn("사용자 정지 처리 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("사용자 정지 처리 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 사용자 정지 해제
     */
    @PostMapping("/users/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<Void>> unsuspendUser(
            @PathVariable Long userId,
            @Valid @RequestBody UnsuspendRequest request,
            Authentication auth) {
        
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Long adminId = principal.getUserId();
            
            adminActionService.unsuspendUser(userId, request.getReason(), adminId);
            
            log.info("사용자 정지 해제 완료 - userId: {}, adminId: {}", userId, adminId);
            return ResponseEntity.ok(ApiResponse.success("정지가 해제되었습니다.", null));
            
        } catch (IllegalStateException e) {
            log.warn("사용자 정지 해제 실패 - userId: {}, error: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("사용자 정지 해제 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게시글 차단 해제
     */
    @PostMapping("/posts/{postId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockPost(
            @PathVariable Long postId,
            @Valid @RequestBody UnsuspendRequest request,
            Authentication auth) {
        
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Long adminId = principal.getUserId();
            
            adminActionService.unblockPost(postId, request.getReason(), adminId);
            
            log.info("게시글 차단 해제 완료 - postId: {}, adminId: {}", postId, adminId);
            return ResponseEntity.ok(ApiResponse.success("게시글 차단이 해제되었습니다.", null));
            
        } catch (IllegalStateException e) {
            log.warn("게시글 차단 해제 실패 - postId: {}, error: {}", postId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("게시글 차단 해제 중 오류 발생 - postId: {}", postId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 사용자 조치 이력 조회
     */
    @GetMapping("/users/{userId}/actions")
    public ResponseEntity<ApiResponse<List<AdminActionDto>>> getUserActions(@PathVariable Long userId) {
        try {
            List<AdminAction> actions = adminActionService.getUserActionHistory(userId);
            List<AdminActionDto> dtos = actions.stream()
                .map(AdminActionDto::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("조회 완료", dtos));
            
        } catch (Exception e) {
            log.error("사용자 조치 이력 조회 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 게시글 조치 이력 조회
     */
    @GetMapping("/posts/{postId}/actions")
    public ResponseEntity<ApiResponse<List<AdminActionDto>>> getPostActions(@PathVariable Long postId) {
        try {
            List<AdminAction> actions = adminActionService.getPostActionHistory(postId);
            List<AdminActionDto> dtos = actions.stream()
                .map(AdminActionDto::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("조회 완료", dtos));
            
        } catch (Exception e) {
            log.error("게시글 조치 이력 조회 중 오류 발생 - postId: {}", postId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 관리자 조치 이력 조회
     */
    @GetMapping("/admins/{adminId}/actions")
    public ResponseEntity<ApiResponse<List<AdminActionDto>>> getAdminActions(@PathVariable Long adminId) {
        try {
            List<AdminAction> actions = adminActionService.getAdminActionHistory(adminId);
            List<AdminActionDto> dtos = actions.stream()
                .map(AdminActionDto::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("조회 완료", dtos));
            
        } catch (Exception e) {
            log.error("관리자 조치 이력 조회 중 오류 발생 - adminId: {}", adminId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 최근 조치 이력 조회 (대시보드용)
     */
    @GetMapping("/actions/recent")
    public ResponseEntity<ApiResponse<List<AdminActionDto>>> getRecentActions(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AdminAction> actions = adminActionService.getRecentActions(limit);
            List<AdminActionDto> dtos = actions.stream()
                .map(AdminActionDto::from)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("조회 완료", dtos));
            
        } catch (Exception e) {
            log.error("최근 조치 이력 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
    
    /**
     * 사용자 정지 상태 확인
     */
    @GetMapping("/users/{userId}/suspension-status")
    public ResponseEntity<ApiResponse<Boolean>> getUserSuspensionStatus(@PathVariable Long userId) {
        try {
            boolean isSuspended = adminActionService.isUserSuspended(userId);
            return ResponseEntity.ok(ApiResponse.success("조회 완료", isSuspended));
            
        } catch (Exception e) {
            log.error("사용자 정지 상태 확인 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }
}