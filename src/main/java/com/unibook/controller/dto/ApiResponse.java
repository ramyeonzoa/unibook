package com.unibook.controller.dto;

/**
 * 표준 API 응답 DTO
 * 모든 API 컨트롤러에서 공통으로 사용
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
    
    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}