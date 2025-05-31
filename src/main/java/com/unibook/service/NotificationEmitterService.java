package com.unibook.service;

import com.unibook.domain.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 연결 관리 서비스
 * 사용자별 다중 연결 지원
 */
@Service
@Slf4j
public class NotificationEmitterService {

    // 사용자별 다중 SSE 연결 저장소 (userId -> List<SseEmitter>)
    // CopyOnWriteArrayList로 동시성 문제 해결
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    // SSE 타임아웃 (5분)
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /**
     * 새 SSE 연결 추가
     */
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 사용자별 emitter 목록에 추가 (CopyOnWriteArrayList로 thread-safe)
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        log.info("SSE 연결 생성: userId={}, 총 연결 수={}", userId, userEmitters.get(userId).size());
        
        // 연결 성공 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결이 성공했습니다."));
        } catch (IOException e) {
            log.error("SSE 연결 초기 메시지 전송 실패: userId={}", userId, e);
            removeEmitter(userId, emitter);
            emitter.completeWithError(e);
            return emitter;
        }
        
        // 연결 종료 시 정리 콜백 설정
        setupEmitterCallbacks(userId, emitter);
        
        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송 (모든 연결에 전송)
     */
    public void sendNotificationToUser(Long userId, NotificationDto.Response notification) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("SSE 연결이 없는 사용자: userId={}", userId);
            return;
        }
        
        // 실패한 연결을 저장할 리스트
        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.error("실시간 알림 전송 실패: userId={}", userId, e);
                failedEmitters.add(emitter);
            }
        }
        
        // 실패한 연결 제거
        if (!failedEmitters.isEmpty()) {
            emitters.removeAll(failedEmitters);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
        
        log.info("실시간 알림 전송: userId={}, 성공={}, 실패={}", 
                userId, emitters.size(), failedEmitters.size());
    }

    /**
     * 특정 사용자에게 카운트 업데이트 전송
     */
    public void sendCountUpdateToUser(Long userId, NotificationDto.CountResponse count) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        
        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("count-update")
                        .data(count));
            } catch (IOException e) {
                log.error("카운트 업데이트 전송 실패: userId={}", userId, e);
                failedEmitters.add(emitter);
            }
        }
        
        // 실패한 연결 제거
        if (!failedEmitters.isEmpty()) {
            emitters.removeAll(failedEmitters);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * 연결 상태 정보 조회
     */
    public Map<String, Object> getConnectionInfo() {
        int totalConnections = userEmitters.values().stream()
                .mapToInt(List::size)
                .sum();
        
        return Map.of(
                "totalUsers", userEmitters.size(),
                "totalConnections", totalConnections,
                "userConnections", userEmitters.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().size()
                        ))
        );
    }

    /**
     * 특정 연결 제거
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
            log.info("SSE 연결 제거: userId={}, 남은 연결 수={}", 
                    userId, emitters.isEmpty() ? 0 : emitters.size());
        }
    }

    /**
     * SSE 연결 콜백 설정
     */
    private void setupEmitterCallbacks(Long userId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            log.info("SSE 연결 정상 종료: userId={}", userId);
            removeEmitter(userId, emitter);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}", userId);
            removeEmitter(userId, emitter);
        });
        
        emitter.onError(throwable -> {
            log.error("SSE 연결 에러: userId={}", userId, throwable);
            removeEmitter(userId, emitter);
        });
    }
}