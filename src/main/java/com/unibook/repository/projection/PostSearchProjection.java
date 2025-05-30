package com.unibook.repository.projection;

/**
 * Full-text 검색 결과 프로젝션
 * Post 엔티티와 검색 관련도 점수를 함께 반환
 */
public interface PostSearchProjection {
    /**
     * 게시글 ID
     */
    Long getPostId();
    
    /**
     * 검색 관련도 총점
     * (제목 점수 + 설명 점수 + 책 정보 점수)
     */
    Double getTotalScore();
}