package com.unibook.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 열거형
 * HTTP 상태 코드와 에러 메시지를 통일 관리
 */
public enum ErrorCode {
    
    // ===== 검증 오류 (400 Bad Request) =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "V001", "입력값이 유효하지 않습니다"),
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "V002", "필수 입력값이 누락되었습니다"),
    INVALID_PAGE_PARAMETER(HttpStatus.BAD_REQUEST, "V003", "페이징 파라미터가 유효하지 않습니다"),
    
    // ===== 리소스 관련 오류 =====
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "요청한 리소스를 찾을 수 없습니다"),
    PROFESSOR_NOT_FOUND(HttpStatus.NOT_FOUND, "R002", "교수를 찾을 수 없습니다"),
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "R003", "과목을 찾을 수 없습니다"),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "R004", "학과를 찾을 수 없습니다"),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "R005", "책을 찾을 수 없습니다"),
    
    // ===== 중복 리소스 오류 (409 Conflict) =====
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "D001", "이미 존재하는 리소스입니다"),
    DUPLICATE_PROFESSOR(HttpStatus.CONFLICT, "D002", "이미 존재하는 교수입니다"),
    DUPLICATE_SUBJECT(HttpStatus.CONFLICT, "D003", "이미 존재하는 과목입니다"),
    DUPLICATE_KEYWORD_ALERT(HttpStatus.CONFLICT, "D004", "이미 등록된 키워드입니다"),
    
    // ===== 인증/권한 오류 (401/403) =====
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A001", "인증에 실패했습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다"),
    
    // ===== 비즈니스 로직 오류 (422 Unprocessable Entity) =====
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "B001", "비즈니스 규칙 위반입니다"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "B002", "동시 수정으로 인한 충돌이 발생했습니다"),
    
    // ===== 시스템 오류 (500 Internal Server Error) =====
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "내부 서버 오류가 발생했습니다"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "데이터베이스 오류가 발생했습니다");
    
    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;
    
    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
}