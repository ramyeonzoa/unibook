package com.unibook.controller.dto;

import com.unibook.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * 에러 응답을 위한 표준 DTO
 */
public class ErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(ErrorCode errorCode, String message, String path) {
        this();
        this.code = errorCode.getCode();
        this.message = message;
        this.path = path;
    }
    
    public ErrorResponse(String code, String message, String path) {
        this();
        this.code = code;
        this.message = message;
        this.path = path;
    }
    
    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(errorCode, message, path);
    }
    
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, path);
    }
    
    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}