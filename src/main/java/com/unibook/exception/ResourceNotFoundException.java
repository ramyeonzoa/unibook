package com.unibook.exception;

import org.springframework.http.HttpStatus;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + "을(를) 찾을 수 없습니다. ID: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + "을(를) 찾을 수 없습니다: " + identifier, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
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