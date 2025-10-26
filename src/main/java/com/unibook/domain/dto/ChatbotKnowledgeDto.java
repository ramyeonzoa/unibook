package com.unibook.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 챗봇의 지식 베이스(FAQ) 데이터 구조
 * rag_seed.json 파일의 각 항목을 Java 객체로 매핑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotKnowledgeDto {

  /**
   * FAQ 고유 ID
   * 예: "guide.quick_start", "faq.signup_flow"
   */
  private String id;

  /**
   * 카테고리
   * 예: "Guide", "FAQ", "Policy", "Feature"
   */
  private String category;

  /**
   * 사용자가 물을 법한 자연스러운 질문
   * 예: "Unibook을 처음 사용할 때 어떤 순서로 시작하나요?"
   */
  private String question;

  /**
   * 질문에 대한 명확하고 구체적인 답변
   */
  private String answer;

  /**
   * 원본 소스 파일 경로 (추적용)
   * JSON 필드명: source_path → Java: sourcePath
   */
  @JsonProperty("source_path")
  private String sourcePath;

  /**
   * 관련 페이지 앵커 링크
   * 예: ["/guide#signup-guide"]
   */
  private List<String> anchors;

  /**
   * 임베딩에 사용할 전체 텍스트 생성
   * 질문 + 답변을 합쳐서 Vector DB에 저장
   *
   * @return 질문과 답변을 합친 텍스트
   */
  public String getFullText() {
    return question + " " + answer;
  }

  /**
   * 디버깅용 간단한 설명
   */
  @Override
  public String toString() {
    return String.format("FAQ[%s: %s]", id, question);
  }
}
