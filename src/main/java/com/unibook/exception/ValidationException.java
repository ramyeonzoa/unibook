package com.unibook.exception;

import org.springframework.http.HttpStatus;

/**
 * 유효성 검증 실패 예외
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }
    
    public ValidationException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
    
    // 구체적인 검증 예외들
    public static class EmailAlreadyExistsException extends ValidationException {
        public EmailAlreadyExistsException(String email) {
            super("이미 사용중인 이메일입니다: " + email, "EMAIL_ALREADY_EXISTS");
        }
    }
    
    public static class InvalidDepartmentException extends ValidationException {
        public InvalidDepartmentException() {
            super("유효하지 않은 학과입니다", "INVALID_DEPARTMENT");
        }
    }
    
    public static class InvalidUniversityEmailException extends ValidationException {
        public InvalidUniversityEmailException() {
            super("학교 이메일 도메인이 일치하지 않습니다", "INVALID_UNIVERSITY_EMAIL");
        }
    }
    
    public static class EmptyFileException extends ValidationException {
        public EmptyFileException() {
            super("파일이 비어있습니다", "EMPTY_FILE");
        }
    }
    
    public static class FileSizeExceededException extends ValidationException {
        public FileSizeExceededException(long maxSize) {
            super("파일 크기는 " + (maxSize / 1024 / 1024) + "MB를 초과할 수 없습니다", "FILE_SIZE_EXCEEDED");
        }
    }
    
    public static class InvalidFileExtensionException extends ValidationException {
        public InvalidFileExtensionException(String allowedExtensions) {
            super("허용되지 않는 파일 형식입니다. 허용된 확장자: " + allowedExtensions, "INVALID_FILE_EXTENSION");
        }
    }
}