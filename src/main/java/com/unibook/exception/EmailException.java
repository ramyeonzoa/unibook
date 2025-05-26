package com.unibook.exception;

import org.springframework.http.HttpStatus;

/**
 * 이메일 관련 예외
 */
public class EmailException extends BusinessException {
    
    public EmailException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_ERROR");
    }
    
    public EmailException(String message, String errorCode) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, errorCode);
    }
    
    public EmailException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_ERROR", cause);
    }
}