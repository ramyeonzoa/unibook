package com.unibook.exception;

import org.springframework.http.HttpStatus;

/**
 * Rate Limit 초과 시 발생하는 예외
 */
public class RateLimitException extends BusinessException {
    
    public RateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }
    
    public RateLimitException(String message, String errorCode) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, errorCode);
    }
}