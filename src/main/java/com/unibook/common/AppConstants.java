package com.unibook.common;

/**
 * 애플리케이션 전역 상수
 * Magic Number를 제거하고 중앙 관리를 위한 상수 클래스
 */
public final class AppConstants {
    
    private AppConstants() {
        // 인스턴스 생성 방지
    }
    
    // ===== 검색 관련 =====
    /** 학교 검색 기본 제한 개수 */
    public static final int SCHOOL_SEARCH_LIMIT = 10;
    
    /** 학과 검색 기본 제한 개수 */
    public static final int DEPARTMENT_SEARCH_LIMIT = 200;
    
    /** 검색어 최소 길이 */
    public static final int MIN_SEARCH_LENGTH = 2;
    
    // ===== 유효성 검증 =====
    /** 비밀번호 최소 길이 */
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    /** 비밀번호 최대 길이 */
    public static final int MAX_PASSWORD_LENGTH = 20;
    
    /** 이름 최소 길이 */
    public static final int MIN_NAME_LENGTH = 2;
    
    /** 이름 최대 길이 */
    public static final int MAX_NAME_LENGTH = 50;
    
    // ===== 데이터 처리 =====
    /** CSV 처리 진행 상황 로그 주기 */
    public static final int CSV_LOG_INTERVAL = 100;
    
    /** CSV 필드 최소 개수 */
    public static final int MIN_CSV_FIELDS = 2;
    
    // ===== 기타 =====
    /** 이메일 구분자 */
    public static final String EMAIL_SEPARATOR = "@";
}