package com.unibook.controller;

import com.unibook.common.Messages;
import com.unibook.domain.dto.NotificationDto;
import com.unibook.domain.entity.User;
import com.unibook.exception.ValidationException;
import com.unibook.security.UserPrincipal;
import com.unibook.repository.UserRepository;
import com.unibook.service.NotificationService;
import com.unibook.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    
    /**
     * 마이페이지 조회
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String profile(Model model, 
                         @AuthenticationPrincipal UserPrincipal userPrincipal,
                         @PageableDefault(size = 10) Pageable pageable) {
        // Department, School 정보까지 한 번에 가져오기 (N+1 방지)
        User user = userRepository.findByIdWithDepartmentAndSchool(userPrincipal.getUserId())
                .orElseThrow(() -> new ValidationException("사용자를 찾을 수 없습니다."));
        
        // 알림 데이터 조회
        Page<NotificationDto.Response> notifications = notificationService.getNotifications(
                userPrincipal.getUserId(), pageable);
        NotificationDto.CountResponse notificationCount = notificationService.getNotificationCount(
                userPrincipal.getUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        model.addAttribute("infoUpdateForm", new InfoUpdateForm(user.getPhoneNumber()));
        model.addAttribute("passwordError", false);  // 기본값 설정
        model.addAttribute("infoError", false);      // 기본값 설정
        
        // 알림 관련 데이터 추가
        model.addAttribute("notifications", notifications);
        model.addAttribute("totalCount", notificationCount.getTotalCount());
        model.addAttribute("unreadCount", notificationCount.getUnreadCount());
        
        return "profile";
    }
    
    /**
     * 비밀번호 변경
     */
    @PostMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(@Validated @ModelAttribute PasswordChangeForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserPrincipal userPrincipal,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        
        User user = userService.findById(userPrincipal.getUserId())
                .orElseThrow(() -> new ValidationException("사용자를 찾을 수 없습니다."));
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            bindingResult.rejectValue("currentPassword", "error.currentPassword", 
                    "현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 일치 확인
        if (!form.getNewPassword().equals(form.getNewPasswordConfirm())) {
            bindingResult.rejectValue("newPasswordConfirm", "error.newPasswordConfirm", 
                    "새 비밀번호가 일치하지 않습니다.");
        }
        
        // 이전 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(form.getNewPassword(), user.getPassword())) {
            bindingResult.rejectValue("newPassword", "error.newPassword", 
                    "이전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("infoUpdateForm", new InfoUpdateForm(user.getPhoneNumber()));
            model.addAttribute("passwordError", true);
            return "profile";
        }
        
        try {
            userService.updatePassword(user.getUserId(), form.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다.");
            return "redirect:/profile";
            
        } catch (Exception e) {
            log.error("비밀번호 변경 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호 변경 중 오류가 발생했습니다.");
            return "redirect:/profile";
        }
    }
    
    /**
     * 정보 수정 (전화번호)
     */
    @PostMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public String updateInfo(@Validated @ModelAttribute InfoUpdateForm form,
                            BindingResult bindingResult,
                            @AuthenticationPrincipal UserPrincipal userPrincipal,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        
        User user = userRepository.findByIdWithDepartmentAndSchool(userPrincipal.getUserId())
                .orElseThrow(() -> new ValidationException("사용자를 찾을 수 없습니다."));
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            bindingResult.rejectValue("currentPassword", "error.currentPassword", 
                    "현재 비밀번호가 일치하지 않습니다.");
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
            model.addAttribute("passwordError", false);
            model.addAttribute("infoError", true);
            return "profile";
        }
        
        try {
            userService.updatePhoneNumber(user.getUserId(), form.getPhoneNumber());
            redirectAttributes.addFlashAttribute("successMessage", "정보가 수정되었습니다.");
            return "redirect:/profile";
            
        } catch (Exception e) {
            log.error("정보 수정 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "정보 수정 중 오류가 발생했습니다.");
            return "redirect:/profile";
        }
    }
    
    /**
     * 비밀번호 변경 폼
     */
    @Data
    public static class PasswordChangeForm {
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        private String currentPassword;
        
        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&_])[A-Za-z\\d@$!%*#?&_]{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자(@$!%*#?&_)를 포함해야 합니다.")
        private String newPassword;
        
        @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
        private String newPasswordConfirm;
    }
    
    /**
     * 정보 수정 폼
     */
    @Data
    public static class InfoUpdateForm {
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        private String currentPassword;
        
        @NotBlank(message = "전화번호를 입력해주세요.")
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
        private String phoneNumber;
        
        public InfoUpdateForm() {}
        
        public InfoUpdateForm(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
}