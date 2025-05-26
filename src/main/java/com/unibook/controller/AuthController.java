package com.unibook.controller;

import com.unibook.common.Messages;
import com.unibook.domain.dto.SignupRequestDto;
import com.unibook.domain.dto.UserResponseDto;
import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.User;
import com.unibook.exception.ValidationException;
import com.unibook.repository.DepartmentRepository;
import com.unibook.service.EmailService;
import com.unibook.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final EmailService emailService;
    private final DepartmentRepository departmentRepository;
    
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
            log.info("New user registered: {}", newUser.getEmail());
            
            // 이메일 발송은 별도로 처리 (순환 참조 방지)
            try {
                User user = userService.getUserByEmail(newUser.getEmail()).orElseThrow();
                emailService.sendVerificationEmail(user);
            } catch (Exception e) {
                log.error("Failed to send verification email", e);
                // 이메일 발송 실패해도 회원가입은 성공
            }
            
            redirectAttributes.addFlashAttribute("successMessage", Messages.SIGNUP_SUCCESS);
            return "redirect:/login";
            
        } catch (ValidationException e) {
            log.error(Messages.LOG_SIGNUP_FAILED, e.getMessage());
            
            // TODO: Day 11 - ActivityLogService로 회원가입 실패 로그 기록
            // activityLogService.logUserActivity(signupDto.getEmail(), "SIGNUP", "FAILED: " + e.getMessage());
            
            bindingResult.rejectValue("email", "error.signupForm", e.getMessage());
            restoreDepartmentSelection(signupDto, model);
            return "auth/signup";
        } catch (Exception e) {
            log.error(Messages.LOG_UNEXPECTED_ERROR, e);
            bindingResult.rejectValue("email", "error.signupForm", Messages.SIGNUP_ERROR);
            restoreDepartmentSelection(signupDto, model);
            return "auth/signup";
        }
    }
    
    // 로그인 페이지
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               Model model) {
        
        if (error != null) {
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
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        try {
            userService.verifyEmail(token);
            redirectAttributes.addFlashAttribute("successMessage", "이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Email verification failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
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
            User user = userService.validateUserForEmailResend(email);
            emailService.sendVerificationEmail(user);
            redirectAttributes.addFlashAttribute("successMessage", "인증 이메일이 재발송되었습니다. 이메일을 확인해주세요.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Email resend failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
            User user = userService.getUserForPasswordReset(email);
            emailService.sendPasswordResetEmail(user);
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호 재설정 링크가 이메일로 발송되었습니다.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Password reset request failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }
    
    // 비밀번호 재설정 페이지
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        try {
            userService.validatePasswordResetToken(token);
            model.addAttribute("token", token);
            return "auth/reset-password";
        } catch (Exception e) {
            log.error("Invalid password reset token: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/login";
        }
    }
    
    // 비밀번호 재설정 처리
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        try {
            // 비밀번호 일치 확인
            if (!newPassword.equals(confirmPassword)) {
                throw new ValidationException("비밀번호가 일치하지 않습니다.");
            }
            
            // 비밀번호 복잡성 검증
            validatePasswordComplexity(newPassword);
            
            userService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 성공적으로 변경되었습니다.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
    
    // 비밀번호 복잡성 검증 메서드
    private void validatePasswordComplexity(String password) {
        if (password.length() < 8) {
            throw new ValidationException("비밀번호는 최소 8자 이상이어야 합니다.");
        }
        if (!password.matches(".*[A-Za-z].*")) {
            throw new ValidationException("비밀번호는 영문자를 포함해야 합니다.");
        }
        if (!password.matches(".*\\d.*")) {
            throw new ValidationException("비밀번호는 숫자를 포함해야 합니다.");
        }
        if (!password.matches(".*[@$!%*#?&_].*")) {
            throw new ValidationException("비밀번호는 특수문자(@$!%*#?&_)를 포함해야 합니다.");
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
}