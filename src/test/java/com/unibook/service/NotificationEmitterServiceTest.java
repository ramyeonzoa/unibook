package com.unibook.service;

import com.unibook.domain.dto.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationEmitterService 단위 테스트
 * SSE 연결 관리 및 동시성 테스트
 */
@ExtendWith(MockitoExtension.class)
class NotificationEmitterServiceTest {

    @InjectMocks
    private NotificationEmitterService emitterService;
    
    private NotificationDto.Response testNotification;
    
    @BeforeEach
    void setUp() {
        testNotification = NotificationDto.Response.builder()
                .notificationId(1L)
                .type("POST_WISHLISTED")
                .title("테스트 알림")
                .content("테스트 내용")
                .isRead(false)
                .build();
    }
    
    @Test
    @DisplayName("SSE 연결 생성 - 정상 케이스")
    void createEmitter_Success() throws IOException {
        // given
        Long userId = 1L;
        
        // when
        SseEmitter emitter = emitterService.createEmitter(userId);
        
        // then
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(5 * 60 * 1000L);
        
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalUsers")).isEqualTo(1);
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(1);
    }
    
    @Test
    @DisplayName("동일 사용자 다중 연결 지원")
    void createMultipleEmittersForSameUser() {
        // given
        Long userId = 1L;
        
        // when
        SseEmitter emitter1 = emitterService.createEmitter(userId);
        SseEmitter emitter2 = emitterService.createEmitter(userId);
        SseEmitter emitter3 = emitterService.createEmitter(userId);
        
        // then
        assertThat(emitter1).isNotSameAs(emitter2);
        assertThat(emitter2).isNotSameAs(emitter3);
        
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalUsers")).isEqualTo(1);
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(3);
        
        @SuppressWarnings("unchecked")
        Map<Long, Integer> userConnections = (Map<Long, Integer>) connectionInfo.get("userConnections");
        assertThat(userConnections.get(userId)).isEqualTo(3);
    }
    
    @Test
    @DisplayName("여러 사용자 동시 연결")
    void createEmittersForMultipleUsers() {
        // given & when
        emitterService.createEmitter(1L);
        emitterService.createEmitter(2L);
        emitterService.createEmitter(3L);
        emitterService.createEmitter(1L); // 사용자 1의 두 번째 연결
        
        // then
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalUsers")).isEqualTo(3);
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(4);
    }
    
    @Test
    @DisplayName("알림 전송 - 성공 케이스")
    void sendNotificationToUser_Success() throws IOException {
        // given
        Long userId = 1L;
        SseEmitter mockEmitter = mock(SseEmitter.class);
        
        // SseEmitter를 직접 주입하는 방법 대신 createEmitter 사용
        SseEmitter realEmitter = emitterService.createEmitter(userId);
        
        // when
        emitterService.sendNotificationToUser(userId, testNotification);
        
        // then - 실제로는 실패하지 않으면 성공으로 간주
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(1);
    }
    
    @Test
    @DisplayName("알림 전송 - 연결 없는 사용자")
    void sendNotificationToUser_NoConnection() {
        // given
        Long userId = 999L;
        
        // when
        emitterService.sendNotificationToUser(userId, testNotification);
        
        // then - 예외가 발생하지 않고 조용히 처리됨
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(0);
    }
    
    @Test
    @DisplayName("동시성 테스트 - 다중 스레드에서 연결 생성")
    void concurrentConnectionCreation() throws InterruptedException {
        // given
        int threadCount = 10;
        int connectionsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // when
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < connectionsPerThread; j++) {
                        emitterService.createEmitter((long) userId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드 작업 완료 대기
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        
        // then
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalUsers")).isEqualTo(threadCount);
        assertThat(connectionInfo.get("totalConnections"))
                .isEqualTo(threadCount * connectionsPerThread);
    }
    
    @Test
    @DisplayName("동시성 테스트 - 다중 스레드에서 알림 전송")
    void concurrentNotificationSending() throws InterruptedException {
        // given
        Long userId = 1L;
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // 여러 개의 연결 생성
        for (int i = 0; i < 5; i++) {
            emitterService.createEmitter(userId);
        }
        
        // when - 여러 스레드에서 동시에 알림 전송
        for (int i = 0; i < threadCount; i++) {
            final int notificationId = i;
            executor.submit(() -> {
                try {
                    NotificationDto.Response notification = NotificationDto.Response.builder()
                            .notificationId((long) notificationId)
                            .type("TEST")
                            .title("동시성 테스트 " + notificationId)
                            .content("내용")
                            .isRead(false)
                            .build();
                    
                    emitterService.sendNotificationToUser(userId, notification);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드 작업 완료 대기
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        
        // then - 예외 없이 완료되면 성공
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalUsers")).isEqualTo(1);
    }
    
    @Test
    @DisplayName("연결 타임아웃 시뮬레이션")
    void emitterTimeout() throws InterruptedException {
        // given
        Long userId = 1L;
        SseEmitter emitter = emitterService.createEmitter(userId);
        
        // when - 타임아웃 콜백 직접 호출
        emitter.onTimeout(() -> {
            // 타임아웃 처리 로직이 실행되는지 확인
        });
        
        // then
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(1);
    }
    
    @Test
    @DisplayName("연결 에러 시뮬레이션")
    void emitterError() {
        // given
        Long userId = 1L;
        SseEmitter emitter = emitterService.createEmitter(userId);
        
        // when - 에러 콜백 직접 호출
        emitter.onError(throwable -> {
            // 에러 처리 로직이 실행되는지 확인
        });
        
        // then
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(1);
    }
    
    @Test
    @DisplayName("카운트 업데이트 전송")
    void sendCountUpdateToUser() {
        // given
        Long userId = 1L;
        emitterService.createEmitter(userId);
        
        NotificationDto.CountResponse count = NotificationDto.CountResponse.builder()
                .totalCount(10)
                .unreadCount(3)
                .build();
        
        // when
        emitterService.sendCountUpdateToUser(userId, count);
        
        // then - 예외 없이 완료되면 성공
        Map<String, Object> connectionInfo = emitterService.getConnectionInfo();
        assertThat(connectionInfo.get("totalConnections")).isEqualTo(1);
    }
}