package com.unibook.controller.dto;

import com.unibook.domain.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 게시글 검색 요청 DTO
 * PostController의 list() 메서드 파라미터들을 통합
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchRequest {
    
    // 페이징 관련
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 12;
    
    // 검색 조건
    private String search;
    private Post.ProductType productType;
    private Post.PostStatus status;
    private Long schoolId;
    private String sortBy;
    
    // 가격 범위
    private Integer minPrice;
    private Integer maxPrice;
    
    // 특정 조건 검색
    private Long subjectId;
    private Long professorId;
    private String bookTitle;
    private Long bookId;
    private Long departmentId;
    
    /**
     * PostController의 기존 11개 파라미터로부터 PostSearchRequest 생성
     * 기존 동작을 100% 보존하기 위한 정적 팩토리 메서드
     */
    public static PostSearchRequest from(int page, int size, String search, 
                                       Post.ProductType productType, Post.PostStatus status, 
                                       Long schoolId, String sortBy, Integer minPrice, Integer maxPrice,
                                       Long subjectId, Long professorId, String bookTitle, Long bookId, Long departmentId) {
        return PostSearchRequest.builder()
                .page(page)
                .size(size)
                .search(search)
                .productType(productType)
                .status(status)
                .schoolId(schoolId)
                .sortBy(sortBy)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .subjectId(subjectId)
                .professorId(professorId)
                .bookTitle(bookTitle)
                .bookId(bookId)
                .departmentId(departmentId)
                .build();
    }
    
    /**
     * 요청 파라미터 정규화 (PostController 기존 로직과 완전 동일)
     * 기존 동작을 100% 보존하기 위해 원본 필드는 수정하지 않음
     */
    public void normalizeForController() {
        // 페이지 크기 검증 - 기존 로직과 동일
        if (size > 100) {
            size = 12; // DEFAULT_PAGE_SIZE
        }
        
        // 페이지 번호는 Spring이 자동 처리하므로 별도 검증 불필요
        
        // sortBy 기본값 설정 - 기존 로직과 완전 동일
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = (search != null && !search.trim().isEmpty()) ? "RELEVANCE" : "NEWEST";
        }
        
        // 주의: search, bookTitle 등은 원본 그대로 유지 (기존 PostController 동작과 동일)
    }
    
    /**
     * 검색어가 있는지 확인
     * 
     * @return 검색어 존재 여부
     */
    public boolean hasSearchKeyword() {
        return search != null && !search.trim().isEmpty();
    }
    
    /**
     * 특정 조건 검색인지 확인 (과목, 교수, 책제목, 책ID)
     * 
     * @return 특정 조건 검색 여부
     */
    public boolean hasSpecificFilters() {
        return subjectId != null || professorId != null || 
               (bookTitle != null && !bookTitle.trim().isEmpty()) ||
               bookId != null;
    }
    
    /**
     * Pageable 객체 생성
     * 검색어 유무에 따라 정렬 옵션 적용 여부 결정
     * 
     * @return Pageable 객체
     */
    public Pageable toPageable() {
        if (hasSearchKeyword()) {
            // 검색어가 있는 경우 Sort 제거 (서비스 레이어에서 처리)
            return PageRequest.of(page, size);
        } else {
            // 검색어가 없는 경우에만 정렬 옵션 적용
            Sort sort = switch (sortBy) {
                case "PRICE_ASC" -> Sort.by("price").ascending();
                case "PRICE_DESC" -> Sort.by("price").descending();
                case "VIEW_COUNT" -> Sort.by("viewCount").descending();
                case "NEWEST" -> Sort.by("createdAt").descending();
                default -> Sort.by("createdAt").descending();
            };
            return PageRequest.of(page, size, sort);
        }
    }
    
    /**
     * 검색 키워드 배열 생성 (하이라이팅용)
     * 기존 PostController 로직과 완전 동일: search.trim().toLowerCase().split("\\s+")
     * 
     * @return 정규화된 키워드 배열 또는 null
     */
    public String[] getSearchKeywords() {
        if (!hasSearchKeyword()) {
            return null;
        }
        
        // 기존 로직과 완전 동일: trim() 처리 포함
        String normalized = search.trim().toLowerCase();
        return normalized.split("\\s+");
    }
    
    /**
     * 페이지 제목 생성용 메서드
     * 
     * @return 검색 조건을 나타내는 문자열
     */
    public String getSearchDescription() {
        if (subjectId != null) {
            return "과목별 검색";
        } else if (professorId != null) {
            return "교수별 검색";
        } else if (bookId != null) {
            return "교재별 게시글";
        } else if (bookTitle != null && !bookTitle.trim().isEmpty()) {
            return "책 제목 검색: " + bookTitle;
        } else if (hasSearchKeyword()) {
            return "통합 검색: " + search;
        } else {
            return "전체 게시글";
        }
    }
}