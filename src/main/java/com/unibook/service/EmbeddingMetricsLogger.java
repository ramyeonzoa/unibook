package com.unibook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unibook.domain.dto.EmbeddingMetrics;
import com.unibook.domain.dto.EmbeddingMetricsSummary;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 임베딩 메트릭 로거
 * CSV 및 JSON Lines 형식으로 메트릭 기록
 */
@Component
@Slf4j
public class EmbeddingMetricsLogger {

  private static final String METRICS_DIR = "data";
  private static final String CSV_FILE = METRICS_DIR + "/embedding-metrics.csv";
  private static final String JSONL_FILE = METRICS_DIR + "/embedding-metrics.jsonl";
  private static final String CSV_HEADER = "timestamp,cache_hit,loading_time_ms,api_calls,total_tokens,estimated_cost_usd,embedding_count,embedding_model\n";

  // text-embedding-3-small 가격: $0.00002 per 1K tokens
  private static final double COST_PER_1K_TOKENS = 0.00002;

  private final ObjectMapper objectMapper;

  public EmbeddingMetricsLogger() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  @PostConstruct
  public void init() {
    try {
      // 디렉터리 생성
      Path metricsDir = Paths.get(METRICS_DIR);
      if (!Files.exists(metricsDir)) {
        Files.createDirectories(metricsDir);
        log.info("메트릭 디렉터리 생성: {}", metricsDir.toAbsolutePath());
      }

      // CSV 헤더 생성 (파일이 없을 경우)
      Path csvPath = Paths.get(CSV_FILE);
      if (!Files.exists(csvPath)) {
        Files.writeString(csvPath, CSV_HEADER);
        log.info("CSV 메트릭 파일 생성: {}", csvPath.toAbsolutePath());
      }

      // JSON Lines 파일 생성 (빈 파일)
      Path jsonlPath = Paths.get(JSONL_FILE);
      if (!Files.exists(jsonlPath)) {
        Files.createFile(jsonlPath);
        log.info("JSON Lines 메트릭 파일 생성: {}", jsonlPath.toAbsolutePath());
      }

    } catch (IOException e) {
      log.error("메트릭 파일 초기화 실패", e);
    }
  }

  /**
   * 메트릭 기록
   */
  public void logMetrics(EmbeddingMetrics metrics) {
    try {
      // CSV 추가
      appendToCsv(metrics);

      // JSON Lines 추가
      appendToJsonLines(metrics);

      // 콘솔 출력 (강조)
      printMetricsToConsole(metrics);

    } catch (Exception e) {
      log.error("메트릭 로깅 실패", e);
    }
  }

  /**
   * CSV 파일에 추가
   */
  private void appendToCsv(EmbeddingMetrics m) throws IOException {
    String line = String.format("%s,%s,%d,%d,%d,%.8f,%d,%s\n",
      m.getTimestamp().toString(),
      m.isCacheHit(),
      m.getLoadingTimeMs(),
      m.getApiCalls(),
      m.getTotalTokens(),
      m.getEstimatedCostUsd(),
      m.getEmbeddingCount(),
      m.getEmbeddingModel()
    );

    Files.writeString(Paths.get(CSV_FILE), line,
      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  /**
   * JSON Lines 파일에 추가
   */
  private void appendToJsonLines(EmbeddingMetrics m) throws IOException {
    String json = objectMapper.writeValueAsString(m) + "\n";
    Files.writeString(Paths.get(JSONL_FILE), json,
      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  /**
   * 콘솔에 메트릭 출력
   */
  private void printMetricsToConsole(EmbeddingMetrics m) {
    log.info("═══════════════════════════════════════════");
    log.info("📊 임베딩 로드 메트릭");
    log.info("─────────────────────────────────────────");
    log.info("  캐시 히트: {}", m.isCacheHit() ? "✓ YES" : "✗ NO");
    log.info("  로딩 시간: {} ms", m.getLoadingTimeMs());
    log.info("  API 호출: {} 회", m.getApiCalls());
    log.info("  토큰 수: {} tokens", m.getTotalTokens());
    log.info("  예상 비용: ${}", String.format("%.6f", m.getEstimatedCostUsd()));
    log.info("  임베딩 수: {} 개", m.getEmbeddingCount());
    log.info("  소스: {}", m.getCacheSource());
    log.info("═══════════════════════════════════════════");
  }

  /**
   * 모든 메트릭 로드
   */
  public List<EmbeddingMetrics> loadAllMetrics() throws IOException {
    List<EmbeddingMetrics> metrics = new ArrayList<>();
    Path jsonlPath = Paths.get(JSONL_FILE);

    if (!Files.exists(jsonlPath)) {
      return metrics;
    }

    try (Stream<String> lines = Files.lines(jsonlPath)) {
      lines.forEach(line -> {
        try {
          EmbeddingMetrics metric = objectMapper.readValue(line, EmbeddingMetrics.class);
          metrics.add(metric);
        } catch (Exception e) {
          log.warn("메트릭 파싱 실패: {}", line, e);
        }
      });
    }

    return metrics;
  }

  /**
   * 메트릭 요약 통계 조회
   */
  public EmbeddingMetricsSummary getSummary() throws IOException {
    List<EmbeddingMetrics> allMetrics = loadAllMetrics();

    if (allMetrics.isEmpty()) {
      return EmbeddingMetricsSummary.builder()
        .totalLoads(0)
        .cacheHits(0)
        .cacheMisses(0)
        .cacheHitRate(0.0)
        .totalCostUsd(0.0)
        .avgLoadingTimeMs(0.0)
        .avgLoadingTimeMsWithCache(0.0)
        .avgLoadingTimeMsWithoutCache(0.0)
        .savedCostUsd(0.0)
        .build();
    }

    long totalLoads = allMetrics.size();
    long cacheHits = allMetrics.stream().filter(EmbeddingMetrics::isCacheHit).count();
    long cacheMisses = totalLoads - cacheHits;
    double cacheHitRate = (double) cacheHits / totalLoads * 100.0;

    double totalCost = allMetrics.stream()
      .mapToDouble(EmbeddingMetrics::getEstimatedCostUsd)
      .sum();

    double avgLoadingTime = allMetrics.stream()
      .mapToLong(EmbeddingMetrics::getLoadingTimeMs)
      .average()
      .orElse(0.0);

    double avgLoadingTimeWithCache = allMetrics.stream()
      .filter(EmbeddingMetrics::isCacheHit)
      .mapToLong(EmbeddingMetrics::getLoadingTimeMs)
      .average()
      .orElse(0.0);

    double avgLoadingTimeWithoutCache = allMetrics.stream()
      .filter(m -> !m.isCacheHit())
      .mapToLong(EmbeddingMetrics::getLoadingTimeMs)
      .average()
      .orElse(0.0);

    // 절약한 비용 계산 (캐시 히트 시 API 호출 안 했다면 들었을 비용)
    double savedCost = cacheHits * (avgLoadingTimeWithoutCache > 0 ?
      allMetrics.stream()
        .filter(m -> !m.isCacheHit())
        .mapToDouble(EmbeddingMetrics::getEstimatedCostUsd)
        .average()
        .orElse(0.0) : 0.0);

    return EmbeddingMetricsSummary.builder()
      .totalLoads(totalLoads)
      .cacheHits(cacheHits)
      .cacheMisses(cacheMisses)
      .cacheHitRate(cacheHitRate)
      .totalCostUsd(totalCost)
      .avgLoadingTimeMs(avgLoadingTime)
      .avgLoadingTimeMsWithCache(avgLoadingTimeWithCache)
      .avgLoadingTimeMsWithoutCache(avgLoadingTimeWithoutCache)
      .savedCostUsd(savedCost)
      .build();
  }

  /**
   * 비용 계산 (토큰 수 기반)
   */
  public static double calculateCost(int totalTokens) {
    return totalTokens * COST_PER_1K_TOKENS / 1000.0;
  }
}
