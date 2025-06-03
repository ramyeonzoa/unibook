package com.unibook.service;

import com.unibook.domain.entity.KeywordAlert;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.exception.DuplicateResourceException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.KeywordAlertRepository;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KeywordAlertService {
    
    private final KeywordAlertRepository keywordAlertRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    private static final int MAX_KEYWORDS_PER_USER = 10; // 사용자당 최대 키워드 개수
    
    /**
     * 키워드 알림 추가
     */
    @Transactional
    public KeywordAlert addKeywordAlert(Long userId, String keyword) {
        // 키워드 정규화 (앞뒤 공백 제거, 소문자 변환)
        String normalizedKeyword = keyword.trim().toLowerCase();
        
        // 키워드 길이 검증
        if (normalizedKeyword.length() < 2) {
            throw new ValidationException("키워드는 2자 이상이어야 합니다.");
        }
        if (normalizedKeyword.length() > 50) {
            throw new ValidationException("키워드는 50자 이하여야 합니다.");
        }
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
        
        // 중복 체크
        if (keywordAlertRepository.existsByUserUserIdAndKeyword(userId, normalizedKeyword)) {
            throw DuplicateResourceException.keywordAlert(normalizedKeyword);
        }
        
        // 사용자별 키워드 개수 제한 체크
        List<KeywordAlert> userKeywords = keywordAlertRepository.findByUserUserId(userId);
        if (userKeywords.size() >= MAX_KEYWORDS_PER_USER) {
            throw new ValidationException("키워드는 최대 " + MAX_KEYWORDS_PER_USER + "개까지 등록 가능합니다.");
        }
        
        // 키워드 알림 생성
        KeywordAlert keywordAlert = KeywordAlert.builder()
                .user(user)
                .keyword(normalizedKeyword)
                .build();
        
        KeywordAlert savedAlert = keywordAlertRepository.save(keywordAlert);
        
        log.info("키워드 알림 등록: userId={}, keyword={}", userId, normalizedKeyword);
        return savedAlert;
    }
    
    /**
     * 키워드 알림 삭제
     */
    @Transactional
    public void removeKeywordAlert(Long userId, String keyword) {
        String normalizedKeyword = keyword.trim().toLowerCase();
        
        // 존재 여부 확인
        if (!keywordAlertRepository.existsByUserUserIdAndKeyword(userId, normalizedKeyword)) {
            throw new ResourceNotFoundException("등록되지 않은 키워드입니다.");
        }
        
        keywordAlertRepository.deleteByUserUserIdAndKeyword(userId, normalizedKeyword);
        log.info("키워드 알림 삭제: userId={}, keyword={}", userId, normalizedKeyword);
    }
    
    /**
     * 사용자의 키워드 알림 목록 조회
     */
    public List<KeywordAlert> getUserKeywordAlerts(Long userId) {
        return keywordAlertRepository.findByUserUserId(userId);
    }
    
    /**
     * 새 게시글과 키워드 매칭 체크 및 알림 발송
     */
    @Transactional
    public void checkKeywordMatching(Post post) {
        try {
            // 게시글 제목을 소문자로 변환하여 매칭
            String postTitle = post.getTitle().toLowerCase();
            
            // 모든 키워드 알림 조회
            List<KeywordAlert> allKeywordAlerts = keywordAlertRepository.findAllWithUser();
            
            // 매칭되는 키워드 찾기
            for (KeywordAlert alert : allKeywordAlerts) {
                String keyword = alert.getKeyword();
                
                // 키워드가 게시글 제목에 포함되어 있는지 확인
                if (postTitle.contains(keyword)) {
                    Long userId = alert.getUser().getUserId();
                    
                    // 자신의 게시글은 알림 발송하지 않음
                    if (!post.getUser().getUserId().equals(userId)) {
                        // 키워드 매칭 알림 발송 (비동기)
                        notificationService.createKeywordMatchNotificationAsync(
                                userId, 
                                post.getPostId(), 
                                post.getTitle(),
                                keyword
                        );
                        
                        log.info("키워드 매칭 알림 발송: userId={}, postId={}, keyword={}", 
                                userId, post.getPostId(), keyword);
                    }
                }
            }
            
        } catch (Exception e) {
            // 키워드 매칭 실패가 게시글 생성을 방해하면 안 됨
            log.warn("키워드 매칭 처리 중 오류 발생: postId={}, error={}", 
                    post.getPostId(), e.getMessage());
        }
    }
    
    /**
     * 키워드 알림 개수 조회 (사용자별)
     */
    public long getUserKeywordCount(Long userId) {
        return keywordAlertRepository.findByUserUserId(userId).size();
    }
    
    /**
     * 특정 키워드를 등록한 사용자 수 조회
     */
    public long getKeywordUserCount(String keyword) {
        String normalizedKeyword = keyword.trim().toLowerCase();
        return keywordAlertRepository.findByKeywordIgnoreCase(normalizedKeyword).size();
    }
}