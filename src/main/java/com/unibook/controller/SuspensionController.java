package com.unibook.controller;

import com.unibook.domain.entity.AdminAction;
import com.unibook.security.UserPrincipal;
import com.unibook.service.AdminActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class SuspensionController {
    
    private final AdminActionService adminActionService;
    
    /**
     * 정지 페이지
     */
    @GetMapping("/suspended")
    public String suspendedPage(Model model, @AuthenticationPrincipal UserPrincipal principal) {
        
        // 인증된 사용자가 있는 경우에만 정지 정보 조회
        if (principal != null) {
            Optional<AdminAction> activeSuspension = adminActionService.getActiveSuspension(principal.getUserId());
            
            if (activeSuspension.isPresent()) {
                AdminAction suspension = activeSuspension.get();
                model.addAttribute("suspension", suspension);
                model.addAttribute("userName", principal.getName());
                model.addAttribute("isPermanent", suspension.getExpiresAt() == null);
                model.addAttribute("expiresAt", suspension.getExpiresAt());
                model.addAttribute("reason", suspension.getReason());
            }
        }
        
        return "auth/suspended";
    }
}