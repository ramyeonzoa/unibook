package com.unibook.performance;

import com.unibook.benchmark.BenchmarkConfig;
import com.unibook.utils.DatabaseStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

/**
 * 성능 테스트를 위한 기반 클래스
 * 
 * 주요 기능:
 * - JVM 워밍업
 * - 메모리 및 GC 모니터링
 * - 데이터베이스 상태 관리
 * - 테스트 환경 초기화/정리
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.format_sql=false",
    "logging.level.org.hibernate.SQL=WARN",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN",
    "spring.profiles.active=benchmark"
})
@Slf4j
public abstract class PerformanceTestBase {
    
    @Autowired
    protected BenchmarkConfig benchmarkConfig;
    
    @Autowired
    protected DatabaseStateManager databaseStateManager;
    
    // 성능 측정을 위한 초기 상태
    protected long initialMemory;
    protected long initialGcCount;
    protected long initialGcTime;
    protected long testStartTime;
    
    @BeforeEach
    void setupPerformanceTest() {
        log.info("🚀 성능 테스트 준비 시작");
        
        testStartTime = System.currentTimeMillis();
        
        // 벤치마크 설정 검증 및 출력
        benchmarkConfig.validate();
        if (benchmarkConfig.isEnableDetailedLogging()) {
            benchmarkConfig.printConfiguration();
        }
        
        // 데이터베이스 상태 초기화 및 검증
        databaseStateManager.ensureConsistentState();
        if (benchmarkConfig.isEnableDetailedLogging()) {
            databaseStateManager.printDatabaseStats();
        }
        
        // 메모리 정리 및 GC
        performGarbageCollection();
        
        // 초기 상태 기록
        recordInitialState();
        
        // JVM 워밍업
        performWarmup();
        
        log.info("✅ 성능 테스트 준비 완료 (소요시간: {}ms)", 
                System.currentTimeMillis() - testStartTime);
    }
    
    @AfterEach
    void cleanupPerformanceTest() {
        long cleanupStartTime = System.currentTimeMillis();
        
        // 최종 상태 기록 및 분석
        recordFinalState();
        
        // 데이터베이스 정리
        databaseStateManager.cleanup();
        
        // 메모리 정리
        performGarbageCollection();
        
        long totalTestTime = cleanupStartTime - testStartTime;
        log.info("🏁 성능 테스트 정리 완료 (총 소요시간: {}ms)", totalTestTime);
    }
    
    /**
     * 강제 가비지 컬렉션 수행
     */
    protected void performGarbageCollection() {
        for (int i = 0; i < 3; i++) {
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (benchmarkConfig.isEnableDetailedLogging()) {
            log.debug("🗑️ 가비지 컬렉션 완료");
        }
    }
    
    /**
     * 초기 상태 기록
     */
    protected void recordInitialState() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        initialMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        initialGcCount = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        initialGcTime = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        
        if (benchmarkConfig.isEnableDetailedLogging()) {
            log.info("📊 초기 상태 - 메모리: {} MB, GC 횟수: {}, GC 시간: {} ms", 
                    initialMemory / 1024 / 1024, initialGcCount, initialGcTime);
        }
    }
    
    /**
     * 최종 상태 기록 및 분석
     */
    protected void recordFinalState() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long finalMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long finalGcCount = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long finalGcTime = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        
        long memoryIncrease = finalMemory - initialMemory;
        long gcCountIncrease = finalGcCount - initialGcCount;
        long gcTimeIncrease = finalGcTime - initialGcTime;
        
        // 메모리 누수 체크
        double memoryIncreaseRatio = (double) memoryIncrease / initialMemory * 100;
        if (memoryIncreaseRatio > 50) {
            log.warn("⚠️ 메모리 사용량이 {}% 증가했습니다. 메모리 누수 가능성 확인 필요", 
                    String.format("%.1f", memoryIncreaseRatio));
        }
        
        if (benchmarkConfig.isEnableDetailedLogging()) {
            log.info("📊 최종 상태 - 메모리: {} MB (+{} MB), GC: {} 회 (+{}), GC 시간: {} ms (+{} ms)", 
                    finalMemory / 1024 / 1024, memoryIncrease / 1024 / 1024,
                    finalGcCount, gcCountIncrease,
                    finalGcTime, gcTimeIncrease);
        }
    }
    
    /**
     * JVM 워밍업 수행 (추상 메서드 - 각 테스트에서 구현)
     */
    protected abstract void performWarmup();
    
    /**
     * 성능 측정 시 정확도를 높이기 위한 헬퍼 메서드들
     */
    
    /**
     * 정확한 시간 측정을 위한 나노초 타이머
     */
    protected long measureExecutionTime(Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // ms로 변환
    }
    
    /**
     * 캐시 클리어 (정확한 콜드 스타트 측정)
     */
    protected void clearAllCaches() {
        databaseStateManager.clearCaches();
        performGarbageCollection();
    }
    
    /**
     * CPU 집약적인 더미 작업 (시스템 부하 시뮬레이션)
     */
    protected void simulateCpuLoad(int durationMs) {
        long endTime = System.currentTimeMillis() + durationMs;
        while (System.currentTimeMillis() < endTime) {
            // CPU 집약적인 계산
            Math.sqrt(Math.random() * 1000);
        }
    }
    
    /**
     * 메모리 압박 시뮬레이션
     */
    protected void simulateMemoryPressure() {
        // 일시적으로 메모리를 사용하여 GC 유발
        @SuppressWarnings("unused")
        byte[][] memoryPressure = new byte[100][1024 * 1024]; // 100MB
        
        performGarbageCollection();
    }
    
    /**
     * 성능 기준값 검증
     */
    protected void validatePerformanceThreshold(double actualMs, String operationName) {
        double threshold = benchmarkConfig.getPerformanceThresholdMs();
        
        if (actualMs > threshold) {
            log.warn("⚠️ {} 성능이 기준값을 초과했습니다. 실제: {:.2f}ms, 기준: {:.2f}ms", 
                    operationName, actualMs, threshold);
        } else {
            log.debug("✅ {} 성능이 기준값 내에 있습니다. 실제: {:.2f}ms, 기준: {:.2f}ms", 
                    operationName, actualMs, threshold);
        }
    }
    
    /**
     * 통계적 유의성을 위한 최소 샘플 수 계산
     */
    protected int calculateMinimumSampleSize(double expectedMean, double expectedStdDev, double errorMargin) {
        // 95% 신뢰구간을 위한 t-value (자유도가 충분히 클 때)
        double tValue = 1.96;
        
        // Cohen's d 효과 크기 기반 샘플 크기 계산
        double requiredSampleSize = Math.pow(tValue * expectedStdDev / (errorMargin * expectedMean), 2);
        
        return Math.max(30, (int) Math.ceil(requiredSampleSize)); // 최소 30개 샘플
    }
    
    /**
     * 시스템 리소스 상태 출력
     */
    protected void printSystemResources() {
        Runtime runtime = Runtime.getRuntime();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("💻 시스템 리소스 상태");
        System.out.println("=".repeat(60));
        System.out.printf("CPU 코어 수: %d\n", runtime.availableProcessors());
        System.out.printf("최대 메모리: %,d MB\n", runtime.maxMemory() / 1024 / 1024);
        System.out.printf("할당된 메모리: %,d MB\n", runtime.totalMemory() / 1024 / 1024);
        System.out.printf("사용 가능한 메모리: %,d MB\n", runtime.freeMemory() / 1024 / 1024);
        System.out.printf("사용 중인 메모리: %,d MB\n", 
                (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        System.out.println("=".repeat(60));
    }
}