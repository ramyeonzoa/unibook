package com.unibook.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 챗봇 평가용 질문 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationQuestion {

  /**
   * 평가 질문 ID
   */
  private String id;

  /**
   * 질문 내용
   */
  private String question;

  /**
   * 카테고리
   */
  private String category;

  /**
   * 난이도 (easy, medium, hard)
   */
  private String difficulty;

  /**
   * 기대 답변 (간략)
   */
  private String expectedAnswer;

  /**
   * 답변에 반드시 포함되어야 할 키워드
   */
  private List<String> mustIncludeKeywords;

  /**
   * 관련 FAQ ID 목록
   */
  private List<String> relevantFaqIds;

  /**
   * 매칭되어야 하는가? (false면 거부되어야 함)
   */
  private boolean shouldMatch;
}
