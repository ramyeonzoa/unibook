package com.unibook.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 예외의 기본 클래스
 * 모든 커스텀 예외는 이 클래스를 상속받아야 함
 */
@Getter
public abstract class BusinessException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    
    protected BusinessException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
    
    protected BusinessException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}