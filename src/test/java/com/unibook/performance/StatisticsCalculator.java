package com.unibook.performance;

import lombok.Data;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 성능 통계 계산 유틸리티
 */
public class StatisticsCalculator {
    
    /**
     * 실행 시간 목록으로부터 상세 통계 계산
     */
    public static PerformanceStatistics calculateStatistics(List<Long> executionTimes, String operationName) {
        if (executionTimes == null || executionTimes.isEmpty()) {
            throw new IllegalArgumentException("실행 시간 목록이 비어있습니다.");
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        executionTimes.forEach(time -> stats.addValue(time.doubleValue()));
        
        // 아웃라이어 제거 (IQR 방식)
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        
        List<Long> filteredTimes = executionTimes.stream()
                .filter(time -> time >= lowerBound && time <= upperBound)
                .collect(Collectors.toList());
        
        // 필터링된 데이터로 재계산
        DescriptiveStatistics filteredStats = new DescriptiveStatistics();
        filteredTimes.forEach(time -> filteredStats.addValue(time.doubleValue()));
        
        return PerformanceStatistics.builder()
                .operationName(operationName)
                .totalSamples(executionTimes.size())
                .filteredSamples(filteredTimes.size())
                .mean(filteredStats.getMean())
                .median(filteredStats.getPercentile(50))
                .min(filteredStats.getMin())
                .max(filteredStats.getMax())
                .standardDeviation(filteredStats.getStandardDeviation())
                .variance(filteredStats.getVariance())
                .p90(filteredStats.getPercentile(90))
                .p95(filteredStats.getPercentile(95))
                .p99(filteredStats.getPercentile(99))
                .outlierCount(executionTimes.size() - filteredTimes.size())
                .build();
    }
    
    /**
     * 두 통계 결과 비교
     */
    public static ComparisonResult compareStatistics(PerformanceStatistics baseline, PerformanceStatistics improved) {
        if (baseline == null || improved == null) {
            throw new IllegalArgumentException("비교할 통계 데이터가 null입니다.");
        }
        
        double meanImprovement = calculateImprovementRatio(baseline.getMean(), improved.getMean());
        double medianImprovement = calculateImprovementRatio(baseline.getMedian(), improved.getMedian());
        double p95Improvement = calculateImprovementRatio(baseline.getP95(), improved.getP95());
        
        return ComparisonResult.builder()
                .baselineName(baseline.getOperationName())
                .improvedName(improved.getOperationName())
                .meanImprovementRatio(meanImprovement)
                .medianImprovementRatio(medianImprovement)
                .p95ImprovementRatio(p95Improvement)
                .baselineStats(baseline)
                .improvedStats(improved)
                .build();
    }
    
    private static double calculateImprovementRatio(double baseline, double improved) {
        if (baseline <= 0 || improved <= 0) {
            return 0.0;
        }
        return baseline / improved;
    }
    
    /**
     * 성능 통계 데이터
     */
    @Data
    @lombok.Builder
    public static class PerformanceStatistics {
        private String operationName;
        private int totalSamples;
        private int filteredSamples;
        private double mean;
        private double median;
        private double min;
        private double max;
        private double standardDeviation;
        private double variance;
        private double p90;
        private double p95;
        private double p99;
        private int outlierCount;
        
        /**
         * 상세 리포트 출력
         */
        public void printDetailedReport() {
            System.out.println("\n" + "=".repeat(80));
            System.out.printf("📊 %s - 상세 성능 분석 리포트\n", operationName);
            System.out.println("=".repeat(80));
            
            System.out.printf("📈 기본 통계:\n");
            System.out.printf("   • 총 샘플 수: %,d개 (아웃라이어 제거 후: %,d개)\n", totalSamples, filteredSamples);
            System.out.printf("   • 평균 실행 시간: %.2f ms\n", mean);
            System.out.printf("   • 중앙값: %.2f ms\n", median);
            System.out.printf("   • 표준편차: %.2f ms\n", standardDeviation);
            System.out.printf("   • 분산: %.2f\n", variance);
            
            System.out.printf("\n🎯 성능 지표:\n");
            System.out.printf("   • 최고 성능: %.2f ms\n", min);
            System.out.printf("   • 최저 성능: %.2f ms\n", max);
            System.out.printf("   • 90퍼센타일: %.2f ms\n", p90);
            System.out.printf("   • 95퍼센타일: %.2f ms\n", p95);
            System.out.printf("   • 99퍼센타일: %.2f ms\n", p99);
            
            System.out.printf("\n🚨 품질 지표:\n");
            System.out.printf("   • 아웃라이어 비율: %.1f%% (%,d개)\n", 
                    (outlierCount * 100.0 / totalSamples), outlierCount);
            System.out.printf("   • 성능 일관성: %s\n", getConsistencyRating());
            System.out.printf("   • 전체 성능 등급: %s\n", getPerformanceGrade());
            
            System.out.println("=".repeat(80));
        }
        
        /**
         * 간단한 요약 출력
         */
        public void printSummary() {
            System.out.printf("%-40s | 평균: %6.2f ms | 중앙값: %6.2f ms | P95: %6.2f ms | 샘플: %,d개\n", 
                    operationName, mean, median, p95, filteredSamples);
        }
        
        /**
         * 성능 일관성 평가
         */
        private String getConsistencyRating() {
            double cv = standardDeviation / mean * 100; // 변동계수
            if (cv < 5) return "매우 일관적 (CV: " + String.format("%.1f%%)", cv);
            if (cv < 15) return "일관적 (CV: " + String.format("%.1f%%)", cv);
            if (cv < 30) return "보통 (CV: " + String.format("%.1f%%)", cv);
            return "불일관적 (CV: " + String.format("%.1f%%)", cv);
        }
        
        /**
         * 전체 성능 등급 평가
         */
        private String getPerformanceGrade() {
            if (mean < 5) return "A+ (매우 빠름)";
            if (mean < 15) return "A (빠름)";
            if (mean < 30) return "B+ (양호)";
            if (mean < 50) return "B (보통)";
            if (mean < 100) return "C (느림)";
            return "D (매우 느림)";
        }
    }
    
    /**
     * 성능 비교 결과
     */
    @Data
    @lombok.Builder
    public static class ComparisonResult {
        private String baselineName;
        private String improvedName;
        private double meanImprovementRatio;
        private double medianImprovementRatio;
        private double p95ImprovementRatio;
        private PerformanceStatistics baselineStats;
        private PerformanceStatistics improvedStats;
        
        /**
         * 비교 결과 출력
         */
        public void printComparisonReport() {
            System.out.println("\n" + "=".repeat(80));
            System.out.printf("⚡ 성능 개선 비교 리포트\n");
            System.out.println("=".repeat(80));
            System.out.printf("기준: %s\n", baselineName);
            System.out.printf("개선: %s\n", improvedName);
            System.out.println("-".repeat(80));
            
            System.out.printf("평균 실행 시간: %.2f ms → %.2f ms (%.2fx 개선)\n", 
                    baselineStats.getMean(), improvedStats.getMean(), meanImprovementRatio);
            System.out.printf("중앙값: %.2f ms → %.2f ms (%.2fx 개선)\n", 
                    baselineStats.getMedian(), improvedStats.getMedian(), medianImprovementRatio);
            System.out.printf("95퍼센타일: %.2f ms → %.2f ms (%.2fx 개선)\n", 
                    baselineStats.getP95(), improvedStats.getP95(), p95ImprovementRatio);
            
            System.out.printf("\n종합 개선 효과: %s\n", getOverallImprovementRating());
            System.out.println("=".repeat(80));
        }
        
        private String getOverallImprovementRating() {
            double avgImprovement = (meanImprovementRatio + medianImprovementRatio + p95ImprovementRatio) / 3.0;
            
            if (avgImprovement >= 10) return "🚀 혁신적 개선 (10x+)";
            if (avgImprovement >= 5) return "⚡ 대폭 개선 (5x+)";
            if (avgImprovement >= 2) return "📈 상당한 개선 (2x+)";
            if (avgImprovement >= 1.5) return "✅ 개선됨 (1.5x+)";
            if (avgImprovement >= 1.1) return "🔧 약간 개선 (1.1x+)";
            return "❌ 개선 효과 없음";
        }
    }
}