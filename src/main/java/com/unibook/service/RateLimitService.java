package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.common.Messages;
import com.unibook.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 발송 Rate Limiting 서비스
 * - 재발송 쿨다운: 1분
 * - 시간당 최대 시도: 5회
 */
@Slf4j
@Service
@EnableScheduling
public class RateLimitService {
    
    // 이메일별 시도 기록
    private final Map<String, List<LocalDateTime>> emailAttempts = new ConcurrentHashMap<>();
    
    /**
     * 이메일 발송 가능 여부 체크
     * @param email 대상 이메일
     * @param action 액션 유형 (로깅용)
     * @throws RateLimitException 제한 초과 시
     */
    public void checkEmailRateLimit(String email, String action) {
        LocalDateTime now = LocalDateTime.now();
        
        // 해당 이메일의 시도 기록 가져오기
        List<LocalDateTime> attempts = emailAttempts.computeIfAbsent(email, k -> new ArrayList<>());
        
        synchronized (attempts) {
            // 1시간 이내의 시도만 유지
            attempts.removeIf(attempt -> attempt.isBefore(now.minusHours(AppConstants.RATE_LIMIT_RETENTION_HOURS)));
            
            // 시간당 최대 시도 횟수 체크
            if (attempts.size() >= AppConstants.EMAIL_RATE_LIMIT_MAX_ATTEMPTS_PER_HOUR) {
                log.warn("Rate limit exceeded for email: {} on action: {}. Attempts in last hour: {}", 
                    email, action, attempts.size());
                throw new RateLimitException(
                    String.format(Messages.RATE_LIMIT_MAX_ATTEMPTS, AppConstants.EMAIL_RATE_LIMIT_MAX_ATTEMPTS_PER_HOUR)
                );
            }
            
            // 마지막 시도로부터 쿨다운 시간 체크
            if (!attempts.isEmpty()) {
                LocalDateTime lastAttempt = attempts.get(attempts.size() - 1);
                long secondsSinceLastAttempt = ChronoUnit.SECONDS.between(lastAttempt, now);
                
                if (secondsSinceLastAttempt < AppConstants.EMAIL_RATE_LIMIT_COOLDOWN_SECONDS) {
                    long remainingSeconds = AppConstants.EMAIL_RATE_LIMIT_COOLDOWN_SECONDS - secondsSinceLastAttempt;
                    log.info("Cooldown active for email: {} on action: {}. Remaining seconds: {}", 
                        email, action, remainingSeconds);
                    throw new RateLimitException(
                        String.format(Messages.RATE_LIMIT_COOLDOWN, remainingSeconds)
                    );
                }
            }
            
            // 현재 시도 기록
            attempts.add(now);
            log.info("Email rate limit check passed for: {} on action: {}. Total attempts in last hour: {}", 
                email, action, attempts.size());
        }
    }
    
    /**
     * 주기적으로 오래된 기록 정리
     */
    @Scheduled(fixedDelay = AppConstants.RATE_LIMIT_CLEANUP_INTERVAL)
    public void cleanupOldEntries() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(AppConstants.RATE_LIMIT_RETENTION_HOURS);
        int totalCleaned = 0;
        
        for (Map.Entry<String, List<LocalDateTime>> entry : emailAttempts.entrySet()) {
            List<LocalDateTime> attempts = entry.getValue();
            synchronized (attempts) {
                int beforeSize = attempts.size();
                attempts.removeIf(attempt -> attempt.isBefore(cutoffTime));
                int removed = beforeSize - attempts.size();
                if (removed > 0) {
                    totalCleaned += removed;
                }
            }
        }
        
        // 빈 엔트리 제거
        emailAttempts.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        if (totalCleaned > 0) {
            log.info("Rate limit cleanup completed. Removed {} old entries. Active emails: {}", 
                totalCleaned, emailAttempts.size());
        }
    }
}