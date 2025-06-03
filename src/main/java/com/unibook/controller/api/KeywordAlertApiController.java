package com.unibook.controller.api;

import com.unibook.domain.entity.KeywordAlert;
import com.unibook.exception.DuplicateResourceException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.security.UserPrincipal;
import com.unibook.service.KeywordAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keyword-alerts")
@RequiredArgsConstructor
@Slf4j
public class KeywordAlertApiController {
    
    private final KeywordAlertService keywordAlertService;
    
    /**
     * 키워드 알림 등록
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addKeywordAlert(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            String keyword = request.get("keyword");
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "키워드를 입력해주세요"
                ));
            }
            
            Long userId = userPrincipal.getUserId();
            log.info("키워드 알림 등록 요청: userId={}, keyword={}", userId, keyword);
            
            // 이메일 인증 확인
            if (!userPrincipal.isVerified()) {
                log.warn("이메일 미인증 사용자의 키워드 알림 등록 시도: {}", userPrincipal.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일 인증이 필요합니다. 인증 후 다시 시도해주세요 📧",
                        "needVerification", true
                ));
            }
            
            KeywordAlert savedAlert = keywordAlertService.addKeywordAlert(userId, keyword);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "키워드 알림이 등록되었습니다",
                    "keyword", savedAlert.getKeyword()
            ));
            
        } catch (DuplicateResourceException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("키워드 알림 등록 실패: keyword={}", request.get("keyword"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "키워드 알림 등록 중 오류가 발생했습니다"
            ));
        }
    }
    
    /**
     * 내 키워드 알림 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyKeywordAlerts(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            Long userId = userPrincipal.getUserId();
            List<KeywordAlert> keywordAlerts = keywordAlertService.getUserKeywordAlerts(userId);
            
            // 키워드 문자열만 추출
            List<String> keywords = keywordAlerts.stream()
                    .map(KeywordAlert::getKeyword)
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "keywords", keywords,
                    "count", keywords.size()
            ));
            
        } catch (Exception e) {
            log.error("키워드 알림 목록 조회 실패: userId={}", userPrincipal.getUserId(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "키워드 목록 조회 중 오류가 발생했습니다"
            ));
        }
    }
    
    /**
     * 키워드 알림 삭제
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> removeKeywordAlert(
            @RequestParam String keyword,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "삭제할 키워드를 지정해주세요"
                ));
            }
            
            Long userId = userPrincipal.getUserId();
            log.info("키워드 알림 삭제 요청: userId={}, keyword={}", userId, keyword);
            
            keywordAlertService.removeKeywordAlert(userId, keyword);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "키워드 알림이 삭제되었습니다"
            ));
            
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("키워드 알림 삭제 실패: keyword={}", keyword, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "키워드 삭제 중 오류가 발생했습니다"
            ));
        }
    }
    
    /**
     * 키워드 알림 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getKeywordCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            Long userId = userPrincipal.getUserId();
            long count = keywordAlertService.getUserKeywordCount(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));
            
        } catch (Exception e) {
            log.error("키워드 개수 조회 실패: userId={}", userPrincipal.getUserId(), e);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", 0
            ));
        }
    }
}