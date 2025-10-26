package com.unibook.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 챗봇 평가 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

  /**
   * 평가 시각
   */
  private LocalDateTime timestamp;

  /**
   * FAQ 개수
   */
  private int faqCount;

  /**
   * Similarity threshold
   */
  private double threshold;

  /**
   * 총 평가 질문 수
   */
  private int totalQuestions;

  /**
   * 정답 개수
   */
  private int correctAnswers;

  /**
   * 정확도 (0.0 ~ 1.0)
   */
  private double accuracy;

  /**
   * 키워드 커버리지 (0.0 ~ 1.0)
   */
  private double keywordCoverage;

  /**
   * MRR (Mean Reciprocal Rank) - 정답 FAQ의 평균 순위 역수 (0.0 ~ 1.0)
   * 1.0에 가까울수록 정답이 상위에 위치
   */
  private double mrr;

  /**
   * 난이도별 정확도
   */
  private Map<String, Double> accuracyByDifficulty;

  /**
   * 평균 응답 시간 (ms)
   */
  private double avgResponseTimeMs;

  /**
   * 개별 질문 결과
   */
  private List<QuestionResult> questionResults;

  /**
   * 개별 질문 평가 결과
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QuestionResult {
    private String questionId;
    private String question;
    private String difficulty;
    private boolean shouldMatch;
    private boolean actuallyMatched;
    private boolean correct;
    private int keywordsFound;
    private int totalKeywords;
    private String answer;
    private long responseTimeMs;
  }
}
