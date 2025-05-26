package com.unibook.common;

/**
 * 애플리케이션 전역 메시지 상수
 * 사용자에게 보여지는 메시지와 로그 메시지를 중앙 관리
 */
public final class Messages {
    
    private Messages() {
        // 인스턴스 생성 방지
    }
    
    // ===== 성공 메시지 =====
    /** 회원가입 성공 메시지 */
    public static final String SIGNUP_SUCCESS = "회원가입이 완료되었습니다. 이메일로 발송된 인증 링크를 확인해주세요.";
    
    /** 로그아웃 성공 메시지 */
    public static final String LOGOUT_SUCCESS = "로그아웃되었습니다.";
    
    /** 이메일 인증 필요 메시지 */
    public static final String EMAIL_NOT_VERIFIED = "이메일 인증이 필요합니다. 인증 메일을 확인해주세요.";
    
    // ===== 에러 메시지 =====
    /** 회원가입 실패 메시지 */
    public static final String SIGNUP_ERROR = "회원가입 중 오류가 발생했습니다. 다시 시도해주세요.";
    
    /** 로그인 실패 메시지 */
    public static final String LOGIN_ERROR = "이메일 또는 비밀번호가 올바르지 않습니다.";
    
    /** 사용자를 찾을 수 없음 */
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다: ";
    
    // ===== 데이터 초기화 메시지 =====
    /** 학교 데이터 로드 실패 */
    public static final String SCHOOL_DATA_LOAD_FAILED = "No schools were loaded. Data initialization failed.";
    
    /** 학과 데이터 로드 실패 */
    public static final String DEPARTMENT_DATA_LOAD_FAILED = "No departments were loaded. Data initialization failed.";
    
    /** 학과 무결성 오류 */
    public static final String DEPARTMENT_INTEGRITY_ERROR = "Found %d departments without schools. Data integrity violated.";
    
    // ===== 로그 메시지 =====
    /** 로그인 시도 로그 */
    public static final String LOG_LOGIN_ATTEMPT = "Attempting to authenticate user: {}";
    
    /** 사용자 찾음 로그 */
    public static final String LOG_USER_FOUND = "User found: {}, Status: {}, Verified: {}";
    
    /** 회원가입 처리 로그 */
    public static final String LOG_SIGNUP_PROCESSING = "Processing signup for email: {}";
    
    /** 회원가입 성공 로그 */
    public static final String LOG_USER_CREATED = "User created successfully with ID: {}";
    
    /** 회원가입 실패 로그 */
    public static final String LOG_SIGNUP_FAILED = "Signup failed: {}";
    
    /** 예상치 못한 에러 로그 */
    public static final String LOG_UNEXPECTED_ERROR = "Unexpected error during signup";
    
    /** 대학 이메일 도메인 불일치 경고 */
    public static final String LOG_INVALID_UNIVERSITY_EMAIL = "Invalid university email domain: {} for school: {}";
    
    // ===== 데이터 초기화 로그 =====
    /** 데이터 로드 시작 */
    public static final String LOG_LOADING_DATA = "Loading {} from CSV...";
    
    /** 데이터 로드 완료 */
    public static final String LOG_DATA_LOADED = "Loaded {} {} from CSV";
    
    /** 데이터 무결성 확인 */
    public static final String LOG_DATA_INTEGRITY_CHECK = "Data integrity check - Schools: {}, Departments: {}";
    
    /** 데이터 무결성 통과 */
    public static final String LOG_DATA_INTEGRITY_PASSED = "Data integrity verification passed!";
    
    /** CSV 처리 진행 상황 */
    public static final String LOG_CSV_PROGRESS = "Processed {} departments";
    
    /** CSV flush 로그 */
    public static final String LOG_CSV_FLUSH = "Flushed after {} departments";
    
    // ===== 파일 경로 =====
    /** 학교 CSV 파일 경로 */
    public static final String CSV_SCHOOLS_FILE = "data/univ-email-250411-final.csv";
    
    /** 학과 CSV 파일 경로 */
    public static final String CSV_DEPARTMENTS_FILE = "data/univ-dept-mapped.csv";
}