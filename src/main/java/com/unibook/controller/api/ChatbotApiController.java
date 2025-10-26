package com.unibook.controller.api;

import com.unibook.domain.dto.ChatbotRequestDto;
import com.unibook.domain.dto.ChatbotResponseDto;
import com.unibook.service.ChatbotService;
import com.unibook.service.EmbeddingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG 챗봇 API 컨트롤러
 *
 * 엔드포인트:
 * - POST /api/chatbot/chat - 질문에 대한 답변 생성
 * - GET  /api/chatbot/health - 헬스체크
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotApiController {

  private final ChatbotService chatbotService;
  private final EmbeddingService embeddingService;

  /**
   * 챗봇 질문 API
   *
   * POST /api/chatbot/ask
   * Request Body: { "question": "교재를 어떻게 등록하나요?" }
   * Response: { "answer": "...", "confidence": 0.92, "sources": [...] }
   *
   * @param request 사용자 질문
   * @return 챗봇 응답
   */
  @PostMapping("/ask")
  public ResponseEntity<Map<String, Object>> ask(@Valid @RequestBody ChatbotRequestDto request) {
    log.info("POST /api/chatbot/ask - question: {}", request.getQuestion());

    try {
      ChatbotResponseDto response = chatbotService.chat(request.getQuestion());

      // 표준 응답 형식으로 래핑
      Map<String, Object> result = Map.of(
          "success", true,
          "data", response,
          "message", "챗봇 응답 생성 완료"
      );

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("챗봇 API 오류", e);

      // 에러 응답 생성
      Map<String, Object> errorResult = Map.of(
          "success", false,
          "message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
          "error", e.getMessage()
      );

      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(errorResult);
    }
  }

  /**
   * 챗봇 헬스체크 API
   *
   * GET /api/chatbot/health
   * Response: { "status": "OK", "embeddingModel": "...", "chatModel": "..." }
   *
   * @return 시스템 상태 정보
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.debug("GET /api/chatbot/health");

    try {
      Map<String, String> healthInfo = Map.of(
          "status", "OK",
          "embeddingService", embeddingService.getEmbeddingModelInfo(),
          "chatService", chatbotService.getServiceInfo()
      );

      return ResponseEntity.ok(healthInfo);

    } catch (Exception e) {
      log.error("헬스체크 실패", e);

      Map<String, String> errorInfo = Map.of(
          "status", "ERROR",
          "message", e.getMessage()
      );

      return ResponseEntity
          .status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(errorInfo);
    }
  }
}
