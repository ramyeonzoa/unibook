package com.unibook.util;

import com.unibook.common.AppConstants;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 페이징 관련 유틸리티 클래스
 * - 페이지 번호와 크기의 유효성 검증
 * - Pageable 객체 생성 표준화
 */
public class PageableUtils {
    
    /**
     * 기본 정렬 없이 Pageable 생성
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 검증된 Pageable 객체
     */
    public static Pageable createPageable(int page, int size) {
        return createPageable(page, size, Sort.unsorted());
    }
    
    /**
     * 정렬 조건과 함께 Pageable 생성
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 조건
     * @return 검증된 Pageable 객체
     */
    public static Pageable createPageable(int page, int size, Sort sort) {
        // 페이지 번호는 0 이상
        page = Math.max(page, 0);
        
        // 페이지 크기는 1 이상, MAX_PAGE_SIZE 이하
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * 문자열 정렬 조건으로 Pageable 생성
     * 
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sortBy 정렬 필드명
     * @param direction 정렬 방향 ("ASC" 또는 "DESC")
     * @return 검증된 Pageable 객체
     */
    public static Pageable createPageable(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
            
        Sort sort = Sort.by(sortDirection, sortBy);
        return createPageable(page, size, sort);
    }
    
    /**
     * 기본 페이지 크기로 Pageable 생성
     * 
     * @param page 페이지 번호
     * @return 기본 크기의 Pageable 객체
     */
    public static Pageable createPageableWithDefaultSize(int page) {
        return createPageable(page, AppConstants.DEFAULT_PAGE_SIZE);
    }
    
    /**
     * 기본 페이지 크기와 정렬로 Pageable 생성
     * 
     * @param page 페이지 번호
     * @param sort 정렬 조건
     * @return 기본 크기의 Pageable 객체
     */
    public static Pageable createPageableWithDefaultSize(int page, Sort sort) {
        return createPageable(page, AppConstants.DEFAULT_PAGE_SIZE, sort);
    }
}