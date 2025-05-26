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
    
    /** 이메일 인증 완료 (재로그인 필요) */
    public static final String EMAIL_VERIFIED_NEED_LOGIN = "🎉 이메일 인증이 완료되었습니다! 새로운 권한 적용을 위해 다시 로그인해주세요.";
    
    /** 이메일 인증 완료 */
    public static final String EMAIL_VERIFIED = "🎉 이메일 인증이 완료되었습니다! 이제 모든 기능을 사용할 수 있습니다.";
    
    /** 인증 메일 재발송 */
    public static final String EMAIL_RESENT = "인증 이메일이 재발송되었습니다. 이메일을 확인해주세요.";
    
    /** 비밀번호 재설정 메일 발송 */
    public static final String PASSWORD_RESET_EMAIL_SENT = "비밀번호 재설정 링크가 이메일로 발송되었습니다.";
    
    /** 비밀번호 변경 완료 */
    public static final String PASSWORD_CHANGED = "비밀번호가 성공적으로 변경되었습니다.";
    
    // ===== 에러 메시지 =====
    /** 회원가입 실패 메시지 */
    public static final String SIGNUP_ERROR = "회원가입 중 오류가 발생했습니다. 다시 시도해주세요.";
    
    /** 로그인 실패 메시지 */
    public static final String LOGIN_ERROR = "이메일 또는 비밀번호가 올바르지 않습니다.";
    
    /** 사용자를 찾을 수 없음 */
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다: ";
    
    /** 사용자를 찾을 수 없음 (단순) */
    public static final String USER_NOT_FOUND_SIMPLE = "사용자를 찾을 수 없습니다.";
    
    /** 비밀번호가 이전과 동일함 */
    public static final String PASSWORD_SAME_AS_PREVIOUS = "새 비밀번호는 이전 비밀번호와 달라야 합니다.";
    
    /** 토큰 관련 에러 */
    public static final String TOKEN_INVALID = "유효하지 않은 토큰입니다.";
    public static final String TOKEN_EXPIRED_OR_USED = "만료되었거나 이미 사용된 토큰입니다.";
    
    /** 처리 중 오류 */
    public static final String EMAIL_VERIFICATION_ERROR = "이메일 인증 처리 중 오류가 발생했습니다.";
    public static final String EMAIL_RESEND_ERROR = "이메일 재발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    public static final String PASSWORD_RESET_REQUEST_ERROR = "비밀번호 재설정 요청 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    public static final String PASSWORD_RESET_ERROR = "비밀번호 재설정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    public static final String TOKEN_VERIFICATION_ERROR = "토큰 확인 중 오류가 발생했습니다.";
    
    /** 비밀번호 검증 에러 */
    public static final String PASSWORD_NOT_MATCH = "비밀번호가 일치하지 않습니다.";
    public static final String PASSWORD_TOO_SHORT = "비밀번호는 최소 8자 이상이어야 합니다.";
    public static final String PASSWORD_NEED_LETTER = "비밀번호는 영문자를 포함해야 합니다.";
    public static final String PASSWORD_NEED_DIGIT = "비밀번호는 숫자를 포함해야 합니다.";
    public static final String PASSWORD_NEED_SPECIAL = "비밀번호는 특수문자(@$!%*#?&_)를 포함해야 합니다.";
    
    /** API 응답 메시지 */
    public static final String LOGIN_REQUIRED = "로그인이 필요합니다.";
    public static final String EMAIL_ALREADY_VERIFIED = "이미 이메일 인증이 완료된 계정입니다.";
    public static final String EMAIL_RESENT_API = "인증 메일이 재발송되었습니다.";
    public static final String EMAIL_RESEND_FAILED_API = "인증 메일 재발송에 실패했습니다.";
    
    // ===== Rate Limiting 메시지 =====
    /** Rate Limit 초과 - 쿨다운 */
    public static final String RATE_LIMIT_COOLDOWN = "잠시 후 다시 시도해주세요. (%d초 남음)";
    
    /** Rate Limit 초과 - 시간당 최대 횟수 */
    public static final String RATE_LIMIT_MAX_ATTEMPTS = "시간당 최대 %d회까지만 요청할 수 있습니다. 잠시 후 다시 시도해주세요.";
    
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
    
    /** 새 사용자 등록 로그 */
    public static final String LOG_NEW_USER_REGISTERED = "New user registered: {}";
    
    /** 이메일 발송 실패 로그 */
    public static final String LOG_EMAIL_SEND_FAILED = "Failed to send verification email";
    
    /** 이메일 인증 완료 로그 */
    public static final String LOG_EMAIL_VERIFIED = "Email verification completed for user: {}";
    
    /** 동일 사용자 인증 - 재로그인 필요 */
    public static final String LOG_SAME_USER_VERIFIED = "Same user verified - logging out for session refresh: {}";
    
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