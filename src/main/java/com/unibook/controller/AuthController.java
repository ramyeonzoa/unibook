package com.unibook.controller;

import com.unibook.common.Messages;
import com.unibook.domain.dto.SignupRequestDto;
import com.unibook.domain.dto.UserResponseDto;
import com.unibook.domain.entity.Department;
import com.unibook.exception.ValidationException;
import com.unibook.repository.DepartmentRepository;
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
    private final DepartmentRepository departmentRepository;
    
    // 회원가입 페이지
    @GetMapping("/signup")
    public String showSignupForm(Model model) {
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
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
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