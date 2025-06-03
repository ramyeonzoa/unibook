package com.unibook.repository;

import com.unibook.domain.entity.KeywordAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeywordAlertRepository extends JpaRepository<KeywordAlert, Long> {
    
    /**
     * 특정 사용자의 모든 키워드 알림 조회
     */
    @Query("SELECT ka FROM KeywordAlert ka " +
           "JOIN FETCH ka.user u " +
           "WHERE ka.user.userId = :userId " +
           "ORDER BY ka.createdAt DESC")
    List<KeywordAlert> findByUserUserId(@Param("userId") Long userId);
    
    /**
     * 모든 키워드 알림 조회 (새 게시글 매칭용)
     */
    @Query("SELECT ka FROM KeywordAlert ka " +
           "JOIN FETCH ka.user u")
    List<KeywordAlert> findAllWithUser();
    
    /**
     * 사용자와 키워드 조합으로 중복 체크
     */
    boolean existsByUserUserIdAndKeyword(Long userId, String keyword);
    
    /**
     * 사용자와 키워드 조합으로 키워드 알림 삭제
     */
    void deleteByUserUserIdAndKeyword(Long userId, String keyword);
    
    /**
     * 특정 키워드를 포함하는 모든 알림 조회 (대소문자 구분 없음)
     */
    @Query("SELECT ka FROM KeywordAlert ka " +
           "JOIN FETCH ka.user u " +
           "WHERE LOWER(ka.keyword) = LOWER(:keyword)")
    List<KeywordAlert> findByKeywordIgnoreCase(@Param("keyword") String keyword);
}