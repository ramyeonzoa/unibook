package com.unibook.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 챗봇 응답 DTO
 * 사용자 질문에 대한 답변과 메타데이터를 포함
 *
 * API 응답 예시:
 * {
 *   "answer": "판매글 작성을 클릭한 뒤 상품 유형을 선택하고...",
 *   "confidence": 0.92,
 *   "sources": ["guide.post_creation", "faq.post_listing"],
 *   "relatedAnchors": ["/guide#post-guide"]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponseDto {

  /**
   * GPT가 생성한 자연스러운 답변
   */
  private String answer;

  /**
   * 답변의 신뢰도 (0.0 ~ 1.0)
   * - 임베딩 유사도 점수의 평균
   * - 0.7 이상: 높은 신뢰도
   * - 0.5 ~ 0.7: 중간 신뢰도
   * - 0.5 미만: 낮은 신뢰도 (FAQ 페이지 안내)
   */
  private Double confidence;

  /**
   * 답변 생성에 사용된 FAQ 정보 목록
   * 프론트엔드에서 "참고한 정보" 섹션에 표시
   */
  private List<SourceInfo> sources;

  /**
   * 참고한 FAQ 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SourceInfo {
    private String faqId;    // FAQ ID (MRR 계산용)
    private String category;
    private String question;
    private String anchor;  // 관련 페이지 링크 (예: /faq#serviceAccordion)
  }

  /**
   * 관련 페이지 링크 (선택적)
   * 예: ["/guide#post-guide", "/faq#tradeAccordion"]
   * 프론트엔드에서 "자세히 보기" 링크로 활용
   */
  private List<String> relatedAnchors;

  /**
   * 에러 또는 특수 상황 표시 (선택적)
   * 예: "RATE_LIMITED", "NO_MATCH_FOUND"
   */
  private String status;

  /**
   * 신뢰도가 높은 답변인지 확인
   */
  public boolean isHighConfidence() {
    return confidence != null && confidence >= 0.7;
  }

  /**
   * FAQ 페이지 안내가 필요한 낮은 신뢰도인지 확인
   */
  public boolean isLowConfidence() {
    return confidence != null && confidence < 0.5;
  }
}
