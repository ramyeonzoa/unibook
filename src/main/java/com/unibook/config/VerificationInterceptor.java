package com.unibook.config;

import com.unibook.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;

/**
 * 이메일 인증 상태를 확인하는 인터셉터
 * 미인증 사용자의 접근을 제한하고 적절한 안내를 제공
 */
@Slf4j
@Component
public class VerificationInterceptor implements HandlerInterceptor {
    
    // 인증 없이 접근 가능한 URL 패턴
    private static final List<String> ALLOWED_PATHS = Arrays.asList(
        "/", "/home", "/login", "/logout", "/signup",
        "/verify-email", "/resend-verification", 
        "/forgot-password", "/reset-password",
        "/api/auth/check-email", "/api/auth/resend-verification",
        "/api/schools", "/api/departments",
        "/search", "/posts/view", "/books/view"
    );
    
    // 인증이 필요한 기능 URL 패턴
    private static final List<String> RESTRICTED_PATHS = Arrays.asList(
        "/posts/new", "/posts/edit", "/posts/delete",
        "/wishlist", "/chat", "/profile/edit"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 정적 리소스나 허용된 경로는 통과
        if (isAllowedPath(requestURI)) {
            return true;
        }
        
        // 인증 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            // 제한된 경로에 미인증 사용자가 접근하는 경우
            if (!userPrincipal.isVerified() && isRestrictedPath(requestURI)) {
                log.info("Unverified user {} attempted to access restricted path: {}", 
                         userPrincipal.getEmail(), requestURI);
                
                // AJAX 요청인 경우
                if (isAjaxRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"이메일 인증이 필요합니다.\"}");
                    return false;
                }
                
                // 일반 요청인 경우 인증 필요 페이지로 리다이렉트
                response.sendRedirect("/verification-required?returnUrl=" + requestURI);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, 
                          ModelAndView modelAndView) throws Exception {
        // 모든 뷰에 인증 상태 정보 추가
        if (modelAndView != null && !isAjaxRequest(request)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                modelAndView.addObject("isEmailVerified", userPrincipal.isVerified());
            }
        }
    }
    
    private boolean isAllowedPath(String path) {
        // 정적 리소스
        if (path.startsWith("/css/") || path.startsWith("/js/") || 
            path.startsWith("/images/") || path.startsWith("/webjars/")) {
            return true;
        }
        
        // 허용된 경로 확인
        return ALLOWED_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private boolean isRestrictedPath(String path) {
        return RESTRICTED_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }
}