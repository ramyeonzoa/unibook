package com.unibook.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unibook.benchmark.BenchmarkConfig;
import com.unibook.performance.StatisticsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 벤치마크 결과 저장 및 관리
 */
@Component
@Slf4j
public class PerformanceResultSaver {
    
    private final BenchmarkConfig benchmarkConfig;
    private final ObjectMapper objectMapper;
    
    public PerformanceResultSaver(BenchmarkConfig benchmarkConfig) {
        this.benchmarkConfig = benchmarkConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 결과 디렉토리 생성
        ensureResultDirectoryExists();
    }
    
    /**
     * 통계 결과 저장
     */
    public void saveStatistics(String testName, StatisticsCalculator.PerformanceStatistics stats) {
        if (!benchmarkConfig.isSaveResults()) {
            log.debug("결과 저장이 비활성화되어 있습니다.");
            return;
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = String.format("%s/%s_%s.json", 
                benchmarkConfig.getResultDirectory(), testName, timestamp);
        
        Map<String, Object> result = new HashMap<>();
        result.put("testName", testName);
        result.put("timestamp", timestamp);
        result.put("statistics", stats);
        result.put("environment", getEnvironmentInfo());
        result.put("configuration", getBenchmarkConfiguration());
        
        try {
            objectMapper.writeValue(new File(filename), result);
            log.info("📁 벤치마크 결과 저장: {}", filename);
            
            // 추가로 CSV 형태로도 저장
            saveCsvSummary(testName, stats, timestamp);
            
        } catch (IOException e) {
            log.error("❌ 벤치마크 결과 저장 실패: {}", filename, e);
        }
    }
    
    /**
     * 추가 메트릭 저장
     */
    public void saveAdditionalMetrics(String testName, Map<String, Object> metrics) {
        if (!benchmarkConfig.isSaveResults()) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = String.format("%s/%s_metrics_%s.json", 
                benchmarkConfig.getResultDirectory(), testName, timestamp);
        
        Map<String, Object> result = new HashMap<>();
        result.put("testName", testName);
        result.put("timestamp", timestamp);
        result.put("metrics", metrics);
        result.put("environment", getEnvironmentInfo());
        
        try {
            objectMapper.writeValue(new File(filename), result);
            log.info("📊 추가 메트릭 저장: {}", filename);
        } catch (IOException e) {
            log.error("❌ 추가 메트릭 저장 실패: {}", filename, e);
        }
    }
    
    /**
     * 비교 결과 저장
     */
    public void saveComparisonResult(StatisticsCalculator.ComparisonResult comparison) {
        if (!benchmarkConfig.isSaveResults()) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = String.format("%s/comparison_%s_vs_%s_%s.json", 
                benchmarkConfig.getResultDirectory(), 
                sanitizeFilename(comparison.getBaselineName()),
                sanitizeFilename(comparison.getImprovedName()),
                timestamp);
        
        Map<String, Object> result = new HashMap<>();
        result.put("comparison", comparison);
        result.put("timestamp", timestamp);
        result.put("environment", getEnvironmentInfo());
        
        try {
            objectMapper.writeValue(new File(filename), result);
            log.info("📈 비교 결과 저장: {}", filename);
        } catch (IOException e) {
            log.error("❌ 비교 결과 저장 실패: {}", filename, e);
        }
    }
    
    /**
     * CSV 요약 저장
     */
    private void saveCsvSummary(String testName, StatisticsCalculator.PerformanceStatistics stats, String timestamp) {
        String csvFilename = String.format("%s/summary_%s.csv", benchmarkConfig.getResultDirectory(), testName);
        
        try {
            File csvFile = new File(csvFilename);
            boolean isNewFile = !csvFile.exists();
            
            StringBuilder csvContent = new StringBuilder();
            
            // 헤더 추가 (새 파일인 경우)
            if (isNewFile) {
                csvContent.append("timestamp,testName,samples,mean_ms,median_ms,min_ms,max_ms,p90_ms,p95_ms,p99_ms,stdDev_ms,mean_ns,median_ns,min_ns,max_ns,outliers\n");
            }
            
            // 데이터 추가 (밀리초와 나노초 모두 저장)
            csvContent.append(String.format("%s,%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.0f,%.0f,%.0f,%.0f,%d\n",
                    timestamp, testName, stats.getFilteredSamples(),
                    stats.getMean() / 1_000_000, stats.getMedian() / 1_000_000, 
                    stats.getMin() / 1_000_000, stats.getMax() / 1_000_000,
                    stats.getP90() / 1_000_000, stats.getP95() / 1_000_000, 
                    stats.getP99() / 1_000_000, stats.getStandardDeviation() / 1_000_000,
                    stats.getMean(), stats.getMedian(), stats.getMin(), stats.getMax(),
                    stats.getOutlierCount()));
            
            // 파일에 쓰기 (append 모드)
            java.nio.file.Files.write(csvFile.toPath(), csvContent.toString().getBytes(),
                    isNewFile ? java.nio.file.StandardOpenOption.CREATE : java.nio.file.StandardOpenOption.APPEND);
            
            log.debug("📊 CSV 요약 저장: {}", csvFilename);
            
        } catch (IOException e) {
            log.error("❌ CSV 요약 저장 실패: {}", csvFilename, e);
        }
    }
    
    /**
     * 전체 요약 리포트 생성
     */
    public void generateSummaryReport() {
        File resultDir = new File(benchmarkConfig.getResultDirectory());
        File[] resultFiles = resultDir.listFiles((dir, name) -> name.endsWith(".json") && !name.contains("comparison"));
        
        if (resultFiles == null || resultFiles.length == 0) {
            log.warn("📭 저장된 벤치마크 결과가 없습니다.");
            return;
        }
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("📊 전체 벤치마크 결과 요약 리포트");
        System.out.println("=".repeat(100));
        System.out.printf("%-40s | %8s | %8s | %8s | %8s | %10s\n", 
                "테스트명", "평균(ms)", "중앙값", "P95", "샘플수", "시간");
        System.out.println("-".repeat(100));
        
        for (File file : resultFiles) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(file, Map.class);
                
                String testName = (String) result.get("testName");
                String timestamp = (String) result.get("timestamp");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) result.get("statistics");
                
                if (stats != null) {
                    Double mean = (Double) stats.get("mean");
                    Double median = (Double) stats.get("median");
                    Double p95 = (Double) stats.get("p95");
                    Integer samples = (Integer) stats.get("filteredSamples");
                    
                    System.out.printf("%-40s | %8.2f | %8.2f | %8.2f | %8d | %10s\n", 
                            truncateString(testName, 40), mean, median, p95, samples, 
                            timestamp.substring(11)); // 시간 부분만
                }
                
            } catch (IOException e) {
                log.error("❌ 결과 파일 읽기 실패: {}", file.getName(), e);
            }
        }
        
        System.out.println("=".repeat(100));
        System.out.printf("총 %d개의 테스트 결과\n", resultFiles.length);
        System.out.println("=".repeat(100));
    }
    
    /**
     * 결과 디렉토리 생성
     */
    private void ensureResultDirectoryExists() {
        File resultDir = new File(benchmarkConfig.getResultDirectory());
        if (!resultDir.exists()) {
            boolean created = resultDir.mkdirs();
            if (created) {
                log.info("📁 결과 디렉토리 생성: {}", resultDir.getAbsolutePath());
            } else {
                log.error("❌ 결과 디렉토리 생성 실패: {}", resultDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 환경 정보 수집
     */
    private Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> env = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        env.put("javaVersion", System.getProperty("java.version"));
        env.put("javaVendor", System.getProperty("java.vendor"));
        env.put("osName", System.getProperty("os.name"));
        env.put("osVersion", System.getProperty("os.version"));
        env.put("osArch", System.getProperty("os.arch"));
        env.put("availableProcessors", runtime.availableProcessors());
        env.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
        env.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
        env.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
        
        return env;
    }
    
    /**
     * 벤치마크 설정 정보
     */
    private Map<String, Object> getBenchmarkConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("warmupIterations", benchmarkConfig.getWarmupIterations());
        config.put("measurementIterations", benchmarkConfig.getMeasurementIterations());
        config.put("concurrentThreads", benchmarkConfig.getConcurrentThreads());
        config.put("testSchoolIds", benchmarkConfig.getTestSchoolIds());
        config.put("performanceThreshold", benchmarkConfig.getPerformanceThresholdMs());
        return config;
    }
    
    /**
     * 파일명 안전화 (특수문자 제거)
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
    
    /**
     * 문자열 길이 제한
     */
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}