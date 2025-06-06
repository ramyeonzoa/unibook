package com.unibook.controller;

import com.unibook.common.Messages;
import com.unibook.domain.dto.SignupRequestDto;
import com.unibook.domain.dto.UserResponseDto;
import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.User;
import com.unibook.exception.RateLimitException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.DepartmentRepository;
import com.unibook.security.UserPrincipal;
import com.unibook.service.EmailService;
import com.unibook.service.RateLimitService;
import com.unibook.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final EmailService emailService;
    private final DepartmentRepository departmentRepository;
    private final RateLimitService rateLimitService;
    
    // 회원가입 페이지
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupRequestDto());
        return "auth/signup";
    }
    
    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupRequestDto signupDto,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        // 유효성 검사 에러가 있으면 다시 폼으로
        if (bindingResult.hasErrors()) {
            restoreDepartmentSelection(signupDto, model);
            return "auth/signup";
        }
        
        try {
            UserResponseDto newUser = userService.signup(signupDto);
            log.info(Messages.LOG_NEW_USER_REGISTERED, newUser.getEmail());
            
            // 이메일 발송 처리
            sendVerificationEmailSafely(newUser);
            
            redirectAttributes.addFlashAttribute("successMessage", Messages.SIGNUP_SUCCESS);
            return "redirect:/login";
            
        } catch (ValidationException e) {
            return handleSignupValidationError(e, signupDto, bindingResult, model);
        } catch (Exception e) {
            return handleSignupGeneralError(e, signupDto, bindingResult, model);
        }
    }
    
    // 로그인 페이지
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               @RequestParam(value = "message", required = false) String message,
                               Model model) {
        
        // CustomAuthenticationFailureHandler에서 전달한 메시지를 사용
        // URL 파라미터로 메시지가 전달되지 않은 경우에만 기본 메시지 사용
        if (error != null && message == null) {
            model.addAttribute("errorMessage", Messages.LOGIN_ERROR);
        }
        
        if (logout != null) {
            model.addAttribute("successMessage", Messages.LOGOUT_SUCCESS);
        }
        
        return "auth/login";
    }
    
    // 이메일 중복 체크 (AJAX)
    @GetMapping("/api/auth/check-email")
    @ResponseBody
    public boolean checkEmailAvailable(@RequestParam String email) {
        return userService.isEmailAvailable(email);
    }
    
    // 이메일 인증 처리
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, 
                             @AuthenticationPrincipal UserPrincipal currentUser,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            User verifiedUser = userService.verifyEmail(token);
            log.info(Messages.LOG_EMAIL_VERIFIED, verifiedUser.getEmail());
            
            // 현재 로그인한 사용자와 인증을 완료한 사용자가 동일한 경우
            if (currentUser != null && currentUser.getUserId().equals(verifiedUser.getUserId())) {
                return handleSameUserVerification(verifiedUser, currentUser, request, redirectAttributes);
            }
            
            // 다른 사용자거나 비로그인 상태인 경우
            return handleDifferentUserVerification(verifiedUser, currentUser, redirectAttributes);
            
        } catch (ResourceNotFoundException e) {
            // 토큰을 찾을 수 없는 경우
            log.error("Email verification failed - token not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", Messages.TOKEN_INVALID);
            return "redirect:/token-error";
        } catch (ValidationException e) {
            // 검증 오류 (토큰 만료, 이미 사용됨, 잘못된 타입 등)
            log.error("Email verification validation failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", Messages.TOKEN_EXPIRED_OR_USED);
            return "redirect:/token-error";
        } catch (Exception e) {
            // 예상치 못한 오류
            log.error("Email verification failed with unexpected error", e);
            redirectAttributes.addFlashAttribute("errorMessage", Messages.EMAIL_VERIFICATION_ERROR);
            return "redirect:/token-error";
        }
    }
    
    // 토큰 에러 페이지
    @GetMapping("/token-error")
    public String tokenError(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("errorMessage", message);
        return "error/token-error";
    }
    
    // 이메일 재발송 페이지
    @GetMapping("/resend-verification")
    public String resendVerificationForm() {
        return "auth/resend-verification";
    }
    
    // 이메일 재발송 처리
    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            // Rate Limiting 체크 및 이메일 발송 처리
            processEmailResend(email);
            
            redirectAttributes.addFlashAttribute("successMessage", Messages.EMAIL_RESENT);
            redirectAttributes.addFlashAttribute("showEmailHelp", true);
            return "redirect:/login";
        } catch (RateLimitException e) {
            log.warn("Email resend rate limit exceeded for: {}", email);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/resend-verification";
        } catch (ResourceNotFoundException | ValidationException e) {
            log.error("Email resend failed - validation error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/resend-verification";
        } catch (Exception e) {
            log.error("Email resend failed with unexpected error", e);
            redirectAttributes.addFlashAttribute("errorMessage", Messages.EMAIL_RESEND_ERROR);
            return "redirect:/resend-verification";
        }
    }
    
    // 비밀번호 재설정 요청 페이지
    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }
    
    // 비밀번호 재설정 요청 처리
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            // Rate Limiting 체크 및 비밀번호 재설정 이메일 발송 처리
            processPasswordResetRequest(email);
            
            redirectAttributes.addFlashAttribute("successMessage", Messages.PASSWORD_RESET_EMAIL_SENT);
            redirectAttributes.addFlashAttribute("showEmailHelp", true);
            return "redirect:/login";
        } catch (RateLimitException e) {
            log.warn("Password reset rate limit exceeded for: {}", email);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/forgot-password";
        } catch (ResourceNotFoundException e) {
            log.error("Password reset request failed - user not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/forgot-password";
        } catch (Exception e) {
            log.error("Password reset request failed with unexpected error", e);
            redirectAttributes.addFlashAttribute("errorMessage", Messages.PASSWORD_RESET_REQUEST_ERROR);
            return "redirect:/forgot-password";
        }
    }
    
    // 비밀번호 재설정 페이지
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, 
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        try {
            userService.validatePasswordResetToken(token);
            model.addAttribute("token", token);
            return "auth/reset-password";
        } catch (ResourceNotFoundException e) {
            log.error("Password reset token not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", Messages.TOKEN_INVALID);
            return "redirect:/token-error";
        } catch (ValidationException e) {
            log.error("Password reset token validation failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", Messages.TOKEN_EXPIRED_OR_USED);
            return "redirect:/token-error";
        } catch (Exception e) {
            log.error("Invalid password reset token - unexpected error", e);
            redirectAttributes.addFlashAttribute("errorMessage", Messages.TOKEN_VERIFICATION_ERROR);
            return "redirect:/token-error";
        }
    }
    
    // 비밀번호 재설정 처리
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        try {
            // 비밀번호 검증 (일치 확인 + 복잡성 검증)
            validatePasswordReset(newPassword, confirmPassword);
            
            userService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", Messages.PASSWORD_CHANGED);
            return "redirect:/login";
        } catch (ResourceNotFoundException e) {
            // 토큰을 찾을 수 없는 경우
            log.error("Password reset failed - token not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", Messages.TOKEN_INVALID);
            return "redirect:/token-error";
        } catch (ValidationException e) {
            // 검증 오류 (토큰 만료, 같은 비밀번호, 복잡성 규칙 등)
            log.error("Password reset validation failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        } catch (Exception e) {
            log.error("Password reset failed with unexpected error", e);
            redirectAttributes.addFlashAttribute("errorMessage", Messages.PASSWORD_RESET_ERROR);
            return "redirect:/reset-password?token=" + token;
        }
    }
    
    // 비밀번호 복잡성 검증 메서드
    private void validatePasswordComplexity(String password) {
        if (password.length() < 8) {
            throw new ValidationException(Messages.PASSWORD_TOO_SHORT);
        }
        if (!password.matches(".*[A-Za-z].*")) {
            throw new ValidationException(Messages.PASSWORD_NEED_LETTER);
        }
        if (!password.matches(".*\\d.*")) {
            throw new ValidationException(Messages.PASSWORD_NEED_DIGIT);
        }
        if (!password.matches(".*[@$!%*#?&_].*")) {
            throw new ValidationException(Messages.PASSWORD_NEED_SPECIAL);
        }
    }
    
    // 선택한 학과 정보 복원 (중복 코드 제거)
    private void restoreDepartmentSelection(SignupRequestDto signupDto, Model model) {
        if (signupDto.getDepartmentId() != null) {
            departmentRepository.findById(signupDto.getDepartmentId())
                .ifPresent(dept -> {
                    String departmentText = dept.getSchool().getSchoolName() + " - " + dept.getDepartmentName();
                    model.addAttribute("selectedDepartmentText", departmentText);
                });
        }
    }
    
    // API endpoint for authenticated users to resend verification email
    @PostMapping("/api/auth/resend-verification")
    @ResponseBody
    public ResponseEntity<?> resendVerificationEmailForAuthenticatedUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // 사용자 검증
            ResponseEntity<?> validationError = validateAuthenticatedUserForEmailResend(userPrincipal);
            if (validationError != null) {
                return validationError;
            }
            
            String email = userPrincipal.getEmail();
            
            // Rate Limiting 체크
            rateLimitService.checkEmailRateLimit(email, "resend-verification");
            
            // 사용자 조회 및 인증 메일 발송
            User user = userService.getUserByEmail(email)
                    .orElseThrow(() -> new ValidationException(Messages.USER_NOT_FOUND_SIMPLE));
            emailService.sendVerificationEmail(user);
            
            return ResponseEntity.ok(Map.of(
                    "message", Messages.EMAIL_RESENT_API,
                    "email", email
            ));
        } catch (RateLimitException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to resend verification email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", Messages.EMAIL_RESEND_FAILED_API));
        }
    }
    
    /**
     * 이메일 발송 안전 처리 (회원가입 성공과 무관하게 처리)
     */
    private void sendVerificationEmailSafely(UserResponseDto newUser) {
        try {
            User user = userService.getUserByEmail(newUser.getEmail()).orElseThrow();
            emailService.sendVerificationEmail(user);
        } catch (Exception e) {
            log.error(Messages.LOG_EMAIL_SEND_FAILED, e);
            // 이메일 발송 실패해도 회원가입은 성공
        }
    }
    
    /**
     * 회원가입 검증 오류 처리
     */
    private String handleSignupValidationError(ValidationException e, SignupRequestDto signupDto, 
                                             BindingResult bindingResult, Model model) {
        log.error(Messages.LOG_SIGNUP_FAILED, e.getMessage());
        
        // TODO: Day 11 - ActivityLogService로 회원가입 실패 로그 기록
        // activityLogService.logUserActivity(signupDto.getEmail(), "SIGNUP", "FAILED: " + e.getMessage());
        
        bindingResult.rejectValue("email", "error.signupForm", e.getMessage());
        restoreDepartmentSelection(signupDto, model);
        return "auth/signup";
    }
    
    /**
     * 회원가입 일반 오류 처리
     */
    private String handleSignupGeneralError(Exception e, SignupRequestDto signupDto, 
                                          BindingResult bindingResult, Model model) {
        log.error(Messages.LOG_UNEXPECTED_ERROR, e);
        bindingResult.rejectValue("email", "error.signupForm", Messages.SIGNUP_ERROR);
        restoreDepartmentSelection(signupDto, model);
        return "auth/signup";
    }
    
    /**
     * 동일 사용자 이메일 인증 처리 (세션 갱신 필요)
     */
    private String handleSameUserVerification(User verifiedUser, UserPrincipal currentUser, 
                                            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.info(Messages.LOG_SAME_USER_VERIFIED, verifiedUser.getEmail());
        
        // 세션 갱신을 위해 수동으로 로그아웃 처리
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, null, auth);
        }
        
        redirectAttributes.addFlashAttribute("successMessage", Messages.EMAIL_VERIFIED_NEED_LOGIN);
        redirectAttributes.addFlashAttribute("autoEmail", verifiedUser.getEmail());
        return "redirect:/login";
    }
    
    /**
     * 다른 사용자 또는 비로그인 사용자 이메일 인증 처리
     */
    private String handleDifferentUserVerification(User verifiedUser, UserPrincipal currentUser, 
                                                 RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage", Messages.EMAIL_VERIFIED);
        redirectAttributes.addFlashAttribute("autoEmail", verifiedUser.getEmail());
        
        // 로그인 상태가 아니면 로그인 페이지로, 로그인 상태면 홈으로
        return currentUser == null ? "redirect:/login" : "redirect:/";
    }
    
    /**
     * 비밀번호 재설정 검증 (일치 확인 + 복잡성 검증)
     */
    private void validatePasswordReset(String newPassword, String confirmPassword) {
        // 비밀번호 일치 확인
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException(Messages.PASSWORD_NOT_MATCH);
        }
        
        // 비밀번호 복잡성 검증
        validatePasswordComplexity(newPassword);
    }
    
    /**
     * 인증된 사용자의 이메일 재발송 요청 검증
     */
    private ResponseEntity<?> validateAuthenticatedUserForEmailResend(UserPrincipal userPrincipal) {
        // 로그인한 사용자의 정보 확인
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", Messages.LOGIN_REQUIRED));
        }
        
        // 이미 인증된 사용자인지 확인
        if (userPrincipal.isVerified()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", Messages.EMAIL_ALREADY_VERIFIED));
        }
        
        // 검증 통과
        return null;
    }
    
    /**
     * 이메일 재발송 처리 (Rate Limiting + 사용자 검증 + 이메일 발송)
     */
    private void processEmailResend(String email) {
        // Rate Limiting 체크
        rateLimitService.checkEmailRateLimit(email, "resend-verification");
        
        // 사용자 검증 및 이메일 발송
        User user = userService.validateUserForEmailResend(email);
        emailService.sendVerificationEmail(user);
    }
    
    /**
     * 비밀번호 재설정 요청 처리 (Rate Limiting + 사용자 검증 + 이메일 발송)
     */
    private void processPasswordResetRequest(String email) {
        // Rate Limiting 체크
        rateLimitService.checkEmailRateLimit(email, "password-reset");
        
        // 사용자 검증 및 이메일 발송
        User user = userService.getUserForPasswordReset(email);
        emailService.sendPasswordResetEmail(user);
    }
}