package com.unibook.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

import java.time.LocalDateTime;

/**
 * 정지된 사용자가 로그인 시도할 때 발생하는 예외
 */
@Getter
public class UserSuspendedException extends AuthenticationException {
    
    private final String reason;
    private final LocalDateTime expiresAt;
    private final boolean isPermanent;
    
    public UserSuspendedException(String message, String reason, LocalDateTime expiresAt) {
        super(message);
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.isPermanent = (expiresAt == null);
    }
    
    public UserSuspendedException(String reason, LocalDateTime expiresAt) {
        this("계정이 정지되었습니다.", reason, expiresAt);
    }
}