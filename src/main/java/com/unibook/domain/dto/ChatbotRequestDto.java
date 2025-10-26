package com.unibook.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇에게 질문을 보낼 때 사용하는 요청 DTO
 *
 * API 예시:
 * POST /api/chatbot/chat
 * {
 *   "question": "교재를 어떻게 등록하나요?"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequestDto {

  /**
   * 사용자 질문
   * - 최소 2자 이상 (너무 짧으면 의미 없음)
   * - 최대 500자 이하 (너무 길면 처리 부담)
   */
  @NotBlank(message = "질문을 입력해주세요")
  @Size(min = 2, max = 500, message = "질문은 2자 이상 500자 이하여야 합니다")
  private String question;
}
