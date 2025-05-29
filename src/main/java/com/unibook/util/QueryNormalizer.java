package com.unibook.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 검색어 정규화 유틸리티
 * 일관된 검색을 위해 공백 제거, 영문 소문자 처리, Unicode 정규화 등을 수행
 */
public final class QueryNormalizer {
    
    // 정규식 패턴 미리 컴파일 (성능 최적화)
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}&&[^_-]]"); // 밑줄, 하이픈 제외
    
    // 유틸리티 클래스 인스턴스화 방지
    private QueryNormalizer() {
        throw new AssertionError("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }
    
    /**
     * 문자열을 정규화합니다 (통합된 단일 메서드)
     * - 앞뒤 공백 제거
     * - 연속된 공백을 하나로 통합
     * - 영문은 소문자로 변환 (한글은 영향 없음)
     * - Unicode 정규화 (NFC)
     * - 특수문자 제거 (밑줄, 하이픈 제외)
     * 
     * @param input 원본 문자열
     * @return 정규화된 문자열 (null 입력 시 빈 문자열 반환)
     */
    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        // Unicode 정규화 (한글 조합 문자 통일)
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFC);
        
        // 공백 정규화 및 소문자 변환
        normalized = WHITESPACE_PATTERN.matcher(normalized)
            .replaceAll(" ")
            .toLowerCase(Locale.ROOT);
        
        // 특수문자 제거 (밑줄, 하이픈 제외)
        normalized = PUNCTUATION_PATTERN.matcher(normalized)
            .replaceAll(" ")
            .replaceAll("\\s+", " ")  // 특수문자 제거 후 다시 공백 정리
            .trim();
        
        return normalized;
    }
    
    /**
     * 두 문자열이 정규화 후 동일한지 비교
     * 
     * @param str1 비교할 문자열 1
     * @param str2 비교할 문자열 2
     * @return 정규화 후 동일 여부
     */
    public static boolean areEqual(String str1, String str2) {
        return normalize(str1).equals(normalize(str2));
    }
}