package com.unibook.controller.api;

import com.unibook.domain.entity.User;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 정보 API 컨트롤러
 */
@Tag(name = "User API", description = "사용자 정보 조회 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {
    
    private final UserService userService;
    
    /**
     * 현재 로그인된 사용자 정보 조회
     * 학과 및 학교 정보 포함
     */
    @Operation(summary = "현재 사용자 정보 조회", description = "현재 로그인된 사용자의 기본 정보와 학과, 학교 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @Parameter(hidden = true) Authentication authentication) {
        
        // 현재 로그인 사용자의 ID 가져오기
        com.unibook.security.UserPrincipal userPrincipal = 
                (com.unibook.security.UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        
        // 사용자 정보 조회 (학과, 학교 정보 포함)
        User user = userService.findByIdWithDepartmentAndSchool(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        
        // 학과 정보
        if (user.getDepartment() != null) {
            Map<String, Object> department = new HashMap<>();
            department.put("departmentId", user.getDepartment().getDepartmentId());
            department.put("departmentName", user.getDepartment().getDepartmentName());
            result.put("department", department);
            
            // 학교 정보
            if (user.getDepartment().getSchool() != null) {
                Map<String, Object> school = new HashMap<>();
                school.put("schoolId", user.getDepartment().getSchool().getSchoolId());
                school.put("schoolName", user.getDepartment().getSchool().getSchoolName());
                result.put("school", school);
            }
        }
        
        log.debug("현재 사용자 정보 조회 완료: userId={}, name={}", userId, user.getName());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 현재 사용자의 이메일 인증 상태 조회
     */
    @Operation(summary = "이메일 인증 상태 조회", description = "현재 로그인된 사용자의 이메일 인증 상태를 반환합니다.")
    @GetMapping("/verification-status")
    public ResponseEntity<Map<String, Object>> getVerificationStatus(
            @Parameter(hidden = true) Authentication authentication) {
        
        com.unibook.security.UserPrincipal userPrincipal = 
                (com.unibook.security.UserPrincipal) authentication.getPrincipal();
        
        Map<String, Object> result = new HashMap<>();
        result.put("verified", userPrincipal.isVerified());
        result.put("email", userPrincipal.getEmail());
        
        return ResponseEntity.ok(result);
    }
}