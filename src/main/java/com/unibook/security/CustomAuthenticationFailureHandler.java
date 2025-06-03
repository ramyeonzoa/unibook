package com.unibook.security;

import com.unibook.exception.UserSuspendedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
        
        String errorMessage;
        String redirectUrl = "/login?error=true";
        
        // UserSuspendedException이 직접 오거나, 다른 예외의 cause로 올 수 있음
        UserSuspendedException suspendedException = null;
        if (exception instanceof UserSuspendedException) {
            suspendedException = (UserSuspendedException) exception;
        } else if (exception.getCause() instanceof UserSuspendedException) {
            suspendedException = (UserSuspendedException) exception.getCause();
        }
        
        if (suspendedException != null) {
            // 정지 메시지를 간단하게 포맷팅해서 전달
            String suspensionMessage = "정지 사유: " + suspendedException.getReason();
            if (suspendedException.isPermanent()) {
                suspensionMessage += "\n영구정지";
            } else if (suspendedException.getExpiresAt() != null) {
                suspensionMessage += "\n해제일: " + suspendedException.getExpiresAt().format(DATE_FORMATTER);
            }
            
            redirectUrl = "/login?error=true&message=" + URLEncoder.encode(suspensionMessage, StandardCharsets.UTF_8);
            log.info("정지된 사용자 로그인 시도");
            
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "이메일 또는 비밀번호가 올바르지 않습니다.";
            redirectUrl = "/login?error=true&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            
        } else if (exception instanceof DisabledException) {
            errorMessage = "계정이 비활성화되었습니다.";
            redirectUrl = "/login?error=true&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            
        } else {
            errorMessage = "로그인에 실패했습니다.";
            redirectUrl = "/login?error=true&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        }
        
        response.sendRedirect(redirectUrl);
    }
}