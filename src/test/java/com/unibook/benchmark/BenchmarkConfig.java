package com.unibook.benchmark;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "benchmark")
@Data
public class BenchmarkConfig {
    
    /**
     * 워밍업 반복 횟수 (JVM 최적화를 위한 사전 실행)
     */
    private int warmupIterations = 10;
    
    /**
     * 실제 측정 반복 횟수
     */
    private int measurementIterations = 50;
    
    /**
     * 동시 접근 테스트용 스레드 수
     */
    private int concurrentThreads = 10;
    
    /**
     * 스레드당 작업 수
     */
    private int operationsPerThread = 20;
    
    /**
     * GC 로깅 활성화 여부
     */
    private boolean enableGcLogging = true;
    
    /**
     * 상세 로깅 활성화 여부
     */
    private boolean enableDetailedLogging = false;
    
    /**
     * 테스트할 학교 ID 목록 (실제 DB에 존재하는 값들)
     */
    private Long[] testSchoolIds = {1L, 2L, 3L, 4L, 5L};
    
    /**
     * 결과 저장 여부
     */
    private boolean saveResults = true;
    
    /**
     * 결과 저장 디렉토리
     */
    private String resultDirectory = "benchmark-results";
    
    /**
     * 차트 생성 여부
     */
    private boolean generateCharts = false;
    
    /**
     * 성능 기준값 (ms) - 이보다 느리면 경고
     */
    private double performanceThresholdMs = 100.0;
    
    /**
     * 메모리 사용량 모니터링 간격 (ms)
     */
    private long memoryMonitoringIntervalMs = 5000;
    
    /**
     * 설정 검증
     */
    public void validate() {
        if (warmupIterations <= 0) {
            throw new IllegalArgumentException("warmupIterations must be positive");
        }
        if (measurementIterations <= 0) {
            throw new IllegalArgumentException("measurementIterations must be positive");
        }
        if (testSchoolIds == null || testSchoolIds.length == 0) {
            throw new IllegalArgumentException("testSchoolIds must not be empty");
        }
    }
    
    /**
     * 설정 정보 출력
     */
    public void printConfiguration() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔧 벤치마크 설정");
        System.out.println("=".repeat(60));
        System.out.printf("워밍업 반복 횟수: %d\n", warmupIterations);
        System.out.printf("측정 반복 횟수: %d\n", measurementIterations);
        System.out.printf("동시 접근 스레드 수: %d\n", concurrentThreads);
        System.out.printf("스레드당 작업 수: %d\n", operationsPerThread);
        System.out.printf("테스트 대상 학교 수: %d\n", testSchoolIds.length);
        System.out.printf("결과 저장: %s\n", saveResults ? "활성화" : "비활성화");
        System.out.printf("상세 로깅: %s\n", enableDetailedLogging ? "활성화" : "비활성화");
        System.out.printf("성능 기준값: %.1f ms\n", performanceThresholdMs);
        System.out.println("=".repeat(60));
    }
}