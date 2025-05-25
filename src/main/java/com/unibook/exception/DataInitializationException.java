package com.unibook.exception;

import org.springframework.http.HttpStatus;

/**
 * 데이터 초기화 중 발생하는 예외
 * 주로 시스템 시작 시 CSV 로드 등에서 사용
 */
public class DataInitializationException extends BusinessException {
    
    public DataInitializationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "DATA_INITIALIZATION_ERROR");
    }
    
    public DataInitializationException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "DATA_INITIALIZATION_ERROR", cause);
    }
    
    // 구체적인 초기화 예외들
    public static class CsvLoadException extends DataInitializationException {
        public CsvLoadException(String fileName, Throwable cause) {
            super("CSV 파일 로드 실패: " + fileName, cause);
        }
    }
    
    public static class DataIntegrityException extends DataInitializationException {
        public DataIntegrityException(String message) {
            super("데이터 무결성 오류: " + message);
        }
    }
}