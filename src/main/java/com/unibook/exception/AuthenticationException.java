package com.unibook.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증/인가 관련 예외
 */
public class AuthenticationException extends BusinessException {
    
    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR");
    }
    
    public AuthenticationException(String message, String errorCode) {
        super(message, HttpStatus.UNAUTHORIZED, errorCode);
    }
    
    // 구체적인 인증 예외들
    public static class InvalidCredentialsException extends AuthenticationException {
        public InvalidCredentialsException() {
            super("이메일 또는 비밀번호가 올바르지 않습니다", "INVALID_CREDENTIALS");
        }
    }
    
    public static class AccountNotVerifiedException extends AuthenticationException {
        public AccountNotVerifiedException() {
            super("이메일 인증이 필요합니다", "ACCOUNT_NOT_VERIFIED");
        }
    }
    
    public static class AccountSuspendedException extends AuthenticationException {
        public AccountSuspendedException() {
            super("정지된 계정입니다", "ACCOUNT_SUSPENDED");
        }
    }
    
    public static class UnauthorizedAccessException extends BusinessException {
        public UnauthorizedAccessException(String resource) {
            super(resource + "에 대한 접근 권한이 없습니다", HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACCESS");
        }
    }
}