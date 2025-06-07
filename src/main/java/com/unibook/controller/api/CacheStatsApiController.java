package com.unibook.controller.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 캐시 통계 API 컨트롤러
 * 
 * Department Caffeine Cache의 실시간 성능 지표를 제공합니다.
 * 
 * 제공 정보:
 * - 히트율, 미스율
 * - 총 요청 수, 캐시 크기
 * - 평균 로드 시간
 * - 메모리 사용량 추정
 * - 제거(eviction) 통계
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheStatsApiController {
    
    private final CacheManager cacheManager;
    
    /**
     * Department 캐시 상세 통계 조회
     * 
     * @return 캐시 성능 지표 맵
     */
    @GetMapping("/departments/stats")
    public ResponseEntity<Map<String, Object>> getDepartmentCacheStats() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Caffeine Cache 인스턴스 가져오기
            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("departments");
            if (caffeineCache == null) {
                return ResponseEntity.ok(createErrorResponse("departments 캐시를 찾을 수 없습니다."));
            }
            
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            CacheStats stats = nativeCache.stats();
            
            // 기본 통계
            result.put("cacheName", "departments");
            result.put("timestamp", System.currentTimeMillis());
            
            // 요청 통계
            long totalRequests = stats.requestCount();
            long hits = stats.hitCount();
            long misses = stats.missCount();
            
            result.put("totalRequests", totalRequests);
            result.put("hits", hits);
            result.put("misses", misses);
            
            // 히트율 계산 (소수점 2자리)
            double hitRate = totalRequests > 0 ? (double) hits / totalRequests * 100 : 0.0;
            double missRate = totalRequests > 0 ? (double) misses / totalRequests * 100 : 0.0;
            
            DecimalFormat df = new DecimalFormat("#.##");
            result.put("hitRate", df.format(hitRate) + "%");
            result.put("missRate", df.format(missRate) + "%");
            
            // 캐시 크기 및 용량
            result.put("currentSize", nativeCache.estimatedSize());
            result.put("maximumSize", 1000); // CacheConfig에서 설정한 값
            
            // 로드 성능 통계
            result.put("averageLoadTime", formatNanosToMs(stats.averageLoadPenalty()));
            result.put("totalLoadTime", formatNanosToMs(stats.totalLoadTime()));
            result.put("loadCount", stats.loadCount());
            
            // 제거(Eviction) 통계
            result.put("evictionCount", stats.evictionCount());
            result.put("evictionWeight", stats.evictionWeight());
            
            // 메모리 사용량 추정 (대략적)
            long estimatedMemoryUsage = nativeCache.estimatedSize() * 100; // Department당 약 100바이트
            result.put("estimatedMemoryUsageBytes", estimatedMemoryUsage);
            result.put("estimatedMemoryUsageMB", String.format("%.2f MB", estimatedMemoryUsage / (1024.0 * 1024.0)));
            
            // 성능 등급 계산
            String performanceGrade = calculatePerformanceGrade(hitRate, totalRequests);
            result.put("performanceGrade", performanceGrade);
            
            // 효과 메시지
            result.put("effectMessage", generateEffectMessage(hitRate, hits, totalRequests));
            
            log.debug("캐시 통계 조회 완료: 히트율 {}%, 총 요청 {}", df.format(hitRate), totalRequests);
            
        } catch (Exception e) {
            log.error("캐시 통계 조회 중 오류 발생", e);
            return ResponseEntity.ok(createErrorResponse("캐시 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 모든 캐시의 요약 통계
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAllCachesSummary() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            result.put("cacheNames", cacheManager.getCacheNames());
            
            // departments 캐시만 상세 정보 제공
            CaffeineCache departmentsCache = (CaffeineCache) cacheManager.getCache("departments");
            if (departmentsCache != null) {
                CacheStats stats = departmentsCache.getNativeCache().stats();
                
                Map<String, Object> deptSummary = new HashMap<>();
                deptSummary.put("totalRequests", stats.requestCount());
                deptSummary.put("hitRate", String.format("%.2f%%", 
                    stats.requestCount() > 0 ? (double) stats.hitCount() / stats.requestCount() * 100 : 0.0));
                deptSummary.put("currentSize", departmentsCache.getNativeCache().estimatedSize());
                
                result.put("departments", deptSummary);
            }
            
        } catch (Exception e) {
            log.error("전체 캐시 요약 조회 중 오류 발생", e);
            return ResponseEntity.ok(createErrorResponse("캐시 요약 조회 중 오류가 발생했습니다."));
        }
        
        return ResponseEntity.ok(result);
    }
    
    // === 헬퍼 메서드들 ===
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
    
    private String formatNanosToMs(double nanos) {
        return String.format("%.2f ms", nanos / 1_000_000.0);
    }
    
    private String calculatePerformanceGrade(double hitRate, long totalRequests) {
        if (totalRequests < 10) return "측정 중...";
        if (hitRate >= 95) return "A+ (탁월함)";
        if (hitRate >= 90) return "A (우수함)";
        if (hitRate >= 80) return "B+ (양호함)";
        if (hitRate >= 70) return "B (보통)";
        if (hitRate >= 60) return "C (개선 필요)";
        return "D (문제 있음)";
    }
    
    private String generateEffectMessage(double hitRate, long hits, long totalRequests) {
        if (totalRequests < 10) {
            return "캐시 사용량이 적어 효과 측정 중입니다.";
        }
        
        if (hitRate >= 95) {
            long savedQueries = hits;
            return String.format("🎉 놀라운 성능! %,d개의 DB 쿼리를 절약했습니다. (%.1f%% 히트율)", 
                savedQueries, hitRate);
        } else if (hitRate >= 80) {
            return String.format("✅ 좋은 성능! %.1f%% 히트율로 성능이 크게 개선되었습니다.", hitRate);
        } else {
            return String.format("⚠️ 히트율 %.1f%%로 개선이 필요할 수 있습니다.", hitRate);
        }
    }
}