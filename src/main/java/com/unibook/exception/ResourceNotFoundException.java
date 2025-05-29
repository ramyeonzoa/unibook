package com.unibook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends BusinessException {
    
    private final ErrorCode errorCode;
    
    public ResourceNotFoundException(String message) {
        this(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
    
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(message, HttpStatus.NOT_FOUND, errorCode.getCode());
        this.errorCode = errorCode;
    }
    
    public ResourceNotFoundException(String resourceType, Long id) {
        this(ErrorCode.RESOURCE_NOT_FOUND, resourceType + "을(를) 찾을 수 없습니다. ID: " + id);
    }
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        this(ErrorCode.RESOURCE_NOT_FOUND, resourceType + "을(를) 찾을 수 없습니다: " + identifier);
    }
    
    public ErrorCode getBusinessErrorCode() {
        return errorCode;
    }
    
    // 구체적인 리소스 예외들
    public static class UserNotFoundException extends ResourceNotFoundException {
        public UserNotFoundException(String email) {
            super("사용자", email);
        }
        
        public UserNotFoundException(Long userId) {
            super("사용자", userId);
        }
    }
    
    public static class DepartmentNotFoundException extends ResourceNotFoundException {
        public DepartmentNotFoundException(Long departmentId) {
            super("학과", departmentId);
        }
    }
    
    public static class SchoolNotFoundException extends ResourceNotFoundException {
        public SchoolNotFoundException(Long schoolId) {
            super("학교", schoolId);
        }
    }
    
    public static class PostNotFoundException extends ResourceNotFoundException {
        public PostNotFoundException(Long postId) {
            super("게시글", postId);
        }
    }
    
    public static class BookNotFoundException extends ResourceNotFoundException {
        public BookNotFoundException(Long bookId) {
            super("교재", bookId);
        }
    }
}