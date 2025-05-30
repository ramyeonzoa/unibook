package com.unibook.controller.api;

import com.unibook.security.UserPrincipal;
import com.unibook.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
public class WishlistApiController {
    
    private final WishlistService wishlistService;
    
    /**
     * 찜하기 토글 (추가/제거)
     */
    @PostMapping("/toggle/{postId}")
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            Long userId = userPrincipal.getUserId();
            log.info("찜하기 토글 요청: userId={}, postId={}", userId, postId);
            
            // 이메일 인증 확인
            if (!userPrincipal.isVerified()) {
                log.warn("이메일 미인증 사용자의 찜하기 시도: {}", userPrincipal.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일 인증이 필요합니다. 인증 후 다시 시도해주세요 📧",
                        "needVerification", true
                ));
            }
            
            boolean isWishlisted = wishlistService.toggleWishlist(userId, postId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isWishlisted", isWishlisted,
                    "message", isWishlisted ? "찜 목록에 추가되었습니다" : "찜 목록에서 제거되었습니다"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("찜하기 토글 실패: postId={}", postId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "찜하기 처리 중 오류가 발생했습니다"
            ));
        }
    }
    
    /**
     * 찜 상태 확인
     */
    @GetMapping("/check/{postId}")
    public ResponseEntity<Map<String, Object>> checkWishlistStatus(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;
            boolean isWishlisted = wishlistService.isWishlisted(userId, postId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isWishlisted", isWishlisted
            ));
            
        } catch (Exception e) {
            log.error("찜 상태 확인 실패: postId={}", postId, e);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isWishlisted", false
            ));
        }
    }
    
    /**
     * 찜하기 제거 (마이페이지에서 사용)
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> removeWishlist(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            Long userId = userPrincipal.getUserId();
            wishlistService.removeWishlist(userId, postId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "찜 목록에서 제거되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("찜하기 제거 실패: postId={}", postId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "찜하기 제거 중 오류가 발생했습니다"
            ));
        }
    }
}