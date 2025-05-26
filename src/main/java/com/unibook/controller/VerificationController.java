package com.unibook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이메일 인증 관련 페이지 컨트롤러
 */
@Controller
public class VerificationController {
    
    @GetMapping("/verification-required")
    public String verificationRequired(@RequestParam(required = false) String returnUrl, Model model) {
        model.addAttribute("returnUrl", returnUrl != null ? returnUrl : "/");
        return "auth/verification-required";
    }
}