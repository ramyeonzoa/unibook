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
    
    // ===== 홈페이지 표시 =====
    /** 홈페이지 인기 도서 표시 개수 */
    public static final int HOME_POPULAR_BOOKS_LIMIT = 8;
    
    /** 홈페이지 최신 게시글 표시 개수 */
    public static final int HOME_RECENT_POSTS_LIMIT = 5;
    
    // ===== 기타 =====
    /** 이메일 구분자 */
    public static final String EMAIL_SEPARATOR = "@";
    
    // ===== Rate Limiting =====
    /** 이메일 재발송 쿨다운 시간 (초) */
    public static final int EMAIL_RATE_LIMIT_COOLDOWN_SECONDS = 60;
    
    /** 시간당 최대 이메일 발송 횟수 */
    public static final int EMAIL_RATE_LIMIT_MAX_ATTEMPTS_PER_HOUR = 5;
    
    /** Rate Limit 정리 주기 (밀리초) - 1시간 */
    public static final long RATE_LIMIT_CLEANUP_INTERVAL = 3600000;
    
    /** Rate Limit 기록 보관 시간 (시간) */
    public static final int RATE_LIMIT_RETENTION_HOURS = 2;
    
    // ===== 스케줄러 관련 =====
    /** 토큰 정리 기준 일수 */
    public static final int TOKEN_CLEANUP_DAYS = 7;
    
    // ===== 이메일 재시도 관련 =====
    /** 최대 이메일 재시도 횟수 */
    public static final int EMAIL_MAX_RETRY_ATTEMPTS = 3;
    
    /** 이메일 재시도 초기 지연 시간 (밀리초) */
    public static final long EMAIL_RETRY_DELAY = 1000L;
    
    /** 이메일 재시도 지연 배수 */
    public static final int EMAIL_RETRY_MULTIPLIER = 2;
    
    // ===== UI 관련 =====
    /** jQuery Autocomplete 최소 입력 길이 */
    public static final int AUTOCOMPLETE_MIN_LENGTH = 2;
    
    // ===== Spring Security 관련 =====
    /** 최대 동시 세션 수 */
    public static final int MAX_CONCURRENT_SESSIONS = 1;
}