package com.unibook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 리소스 중복 시 발생하는 예외
 * 예: 이미 존재하는 교수, 과목 등
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends BusinessException {
    
    private final ErrorCode errorCode;
    
    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(message, HttpStatus.CONFLICT, errorCode.getCode());
        this.errorCode = errorCode;
    }
    
    public DuplicateResourceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, errorCode.getCode(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getBusinessErrorCode() {
        return errorCode;
    }
    
    /**
     * 교수 중복 예외 생성
     */
    public static DuplicateResourceException professor(String professorName) {
        return new DuplicateResourceException(
            ErrorCode.DUPLICATE_PROFESSOR,
            String.format("이미 존재하는 교수입니다: %s", professorName)
        );
    }
    
    /**
     * 과목 중복 예외 생성
     */
    public static DuplicateResourceException subject(String subjectName) {
        return new DuplicateResourceException(
            ErrorCode.DUPLICATE_SUBJECT,
            String.format("이미 존재하는 과목입니다: %s", subjectName)
        );
    }
    
    /**
     * 일반 리소스 중복 예외 생성
     */
    public static DuplicateResourceException of(String resourceType, String identifier) {
        return new DuplicateResourceException(
            ErrorCode.DUPLICATE_RESOURCE,
            String.format("이미 존재하는 %s입니다: %s", resourceType, identifier)
        );
    }
    
    /**
     * 키워드 알림 중복 예외 생성
     */
    public static DuplicateResourceException keywordAlert(String keyword) {
        return new DuplicateResourceException(
            ErrorCode.DUPLICATE_KEYWORD_ALERT,
            String.format("이미 등록된 키워드입니다: %s", keyword)
        );
    }
}