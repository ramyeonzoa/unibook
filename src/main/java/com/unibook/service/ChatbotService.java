package com.unibook.service;

import com.unibook.domain.dto.ChatbotResponseDto;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 기반 챗봇 서비스
 *
 * RAG (Retrieval-Augmented Generation) 파이프라인:
 * 1. 사용자 질문 받기
 * 2. 유사한 FAQ 검색 (EmbeddingService)
 * 3. 검색된 FAQ를 프롬프트에 삽입
 * 4. GPT-5-nano에게 자연스러운 답변 요청
 * 5. 답변 + 메타데이터 반환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

  private final EmbeddingService embeddingService;

  @Value("${openai.api.key}")
  private String openAiApiKey;

  @Value("${openai.api.model}")
  private String modelName;

  @Value("${openai.api.max-tokens}")
  private int maxTokens;

  @Value("${openai.api.timeout}")
  private int timeoutSeconds;

  private ChatLanguageModel chatModel;

  /**
   * GPT-5-nano 모델 초기화
   */
  @PostConstruct
  public void init() {
    log.info("ChatbotService 초기화 시작");

    chatModel = OpenAiChatModel.builder()
        .apiKey(openAiApiKey)
        .modelName(modelName)
        .temperature(1.0)
        .maxCompletionTokens(maxTokens)
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .build();

    log.info("GPT 모델 초기화 완료: model={}, temperature=1.0, maxCompletionTokens={}",
        modelName, maxTokens);
  }

  /**
   * 사용자 질문에 대한 답변 생성 (RAG 파이프라인)
   *
   * @param userQuestion 사용자 질문
   * @return 챗봇 응답 (답변, 신뢰도, 출처)
   */
  public ChatbotResponseDto chat(String userQuestion) {
    log.info("사용자 질문: {}", userQuestion);

    try {
      // Step 1: 유사한 FAQ 검색 (상위 10개 후보 가져오기)
      double similarityThreshold = 0.6; // 유사도 임계값 조정 (김치찌개 차단 & 정상 질문 통과)
      List<EmbeddingMatch<TextSegment>> relevantDocs =
          embeddingService.findRelevant(userQuestion, 10, 0.0)
              .stream()
              .filter(match -> match.score() >= similarityThreshold)
              .limit(3)
              .collect(Collectors.toList());

      log.info("검색된 FAQ 개수: {}", relevantDocs.size());

      // 유사도 점수 로깅 (디버깅/튜닝용)
      for (int i = 0; i < relevantDocs.size(); i++) {
        EmbeddingMatch<TextSegment> match = relevantDocs.get(i);
        String faqId = match.embedded().metadata().getString("id");
        String question = match.embedded().metadata().getString("question");
        double score = match.score();
        log.info("  [{}] FAQ: {} | 질문: {} | 유사도: {}",
          i + 1, faqId, question.substring(0, Math.min(30, question.length())),
          String.format("%.4f", score));
      }

      // Step 2: 검색 결과 없으면 기본 응답
      if (relevantDocs.isEmpty()) {
        log.warn("관련 FAQ를 찾지 못했습니다");
        return createNoMatchResponse();
      }

      // Step 3: Context 구성 (검색된 FAQ들)
      String context = buildContext(relevantDocs);
      log.info("Context 구성 완료 (길이: {}자)", context.length());

      // Step 4: GPT-5-nano에게 전달할 프롬프트 생성
      String prompt = buildPrompt(context, userQuestion);
      log.info("프롬프트 생성 완료 (길이: {}자)", prompt.length());
      log.debug("전송할 프롬프트:\n{}", prompt);

      // Step 5: GPT-5-nano 호출
      UserMessage userMessage = UserMessage.from(prompt);
      log.info("UserMessage 생성 완료");

      Response<AiMessage> response = chatModel.generate(userMessage);
      log.info("Response 객체: {}", response);

      AiMessage aiMessage = response.content();
      log.info("AiMessage 객체: {}", aiMessage);
      log.info("AiMessage null? {}", aiMessage == null);

      String answer = aiMessage != null ? aiMessage.text() : null;
      log.info("답변 생성 완료 (길이: {}자)", answer == null ? "null" : answer.length());
      log.info("GPT 응답: [{}]", answer);

      // Step 6: 응답 DTO 생성
      return buildResponse(answer, relevantDocs);

    } catch (Exception e) {
      log.error("챗봇 오류 발생", e);
      return createErrorResponse(e);
    }
  }

  /**
   * 검색된 FAQ들로 Context 문자열 구성
   */
  private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
    StringBuilder context = new StringBuilder();

    for (int i = 0; i < matches.size(); i++) {
      EmbeddingMatch<TextSegment> match = matches.get(i);
      String question = match.embedded().metadata().getString("question");
      String answer = match.embedded().metadata().getString("answer");

      context.append(String.format("FAQ %d:\n", i + 1));
      context.append(String.format("질문: %s\n", question));
      context.append(String.format("답변: %s\n\n", answer));
    }

    return context.toString();
  }

  /**
   * GPT-5-nano에게 전달할 프롬프트 생성
   */
  private String buildPrompt(String context, String userQuestion) {
    return String.format("""
        당신은 Unibook 대학 교재 거래 플랫폼의 친절한 고객 지원 챗봇입니다.

        다음 FAQ 정보를 참고하여 사용자의 질문에 답변해주세요:

        %s

        사용자 질문: %s

        답변 시 유의사항:
        - 친절하고 자연스러운 말투로 답변하세요
        - FAQ 정보에 있는 내용만 답변하세요
        - FAQ 정보에 없는 내용은 추측하지 마세요
        - 교재 거래와 무관한 질문(요리, 날씨, 일반 상식 등)은 정중히 거절하세요
        - 필요한 경우 관련 페이지 링크를 안내하세요 (예: /faq, /guide)
        - 답변은 2-3문장으로 간결하게 작성하세요
        """, context, userQuestion);
  }

  /**
   * 응답 DTO 생성
   */
  private ChatbotResponseDto buildResponse(String answer, List<EmbeddingMatch<TextSegment>> matches) {
    // 평균 신뢰도 계산
    double avgConfidence = matches.stream()
        .mapToDouble(EmbeddingMatch::score)
        .average()
        .orElse(0.0);

    // 출처 FAQ 정보 목록
    List<ChatbotResponseDto.SourceInfo> sources = matches.stream()
        .map(match -> {
          String anchors = match.embedded().metadata().getString("anchors");
          String firstAnchor = null;
          if (anchors != null && !anchors.isEmpty()) {
            firstAnchor = anchors.split(",")[0];
          }

          return ChatbotResponseDto.SourceInfo.builder()
              .category(match.embedded().metadata().getString("category"))
              .question(match.embedded().metadata().getString("question"))
              .anchor(firstAnchor)
              .build();
        })
        .collect(Collectors.toList());

    // 관련 앵커 링크 추출
    List<String> anchors = matches.stream()
        .map(match -> match.embedded().metadata().getString("anchors"))
        .filter(anchor -> anchor != null && !anchor.isEmpty())
        .flatMap(anchor -> List.of(anchor.split(",")).stream())
        .distinct()
        .limit(3)
        .collect(Collectors.toList());

    return ChatbotResponseDto.builder()
        .answer(answer)
        .confidence(avgConfidence)
        .sources(sources)
        .relatedAnchors(anchors)
        .status("SUCCESS")
        .build();
  }

  /**
   * 관련 FAQ를 찾지 못했을 때 기본 응답
   */
  private ChatbotResponseDto createNoMatchResponse() {
    return ChatbotResponseDto.builder()
        .answer("죄송합니다. 질문에 대한 답변을 찾지 못했습니다.\n\n" +
                "더 자세한 도움이 필요하시면 다음을 이용해주세요:\n" +
                "• FAQ 페이지: /faq\n" +
                "• 이용 가이드: /guide\n" +
                "• 이메일 문의: unibooknotify@gmail.com")
        .confidence(0.0)
        .sources(List.of())
        .relatedAnchors(List.of("/faq", "/guide"))
        .status("NO_MATCH")
        .build();
  }

  /**
   * 에러 발생 시 응답
   */
  private ChatbotResponseDto createErrorResponse(Exception e) {
    return ChatbotResponseDto.builder()
        .answer("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\n\n" +
                "문제가 지속되면 unibooknotify@gmail.com으로 문의해주세요.")
        .confidence(0.0)
        .sources(List.of())
        .relatedAnchors(List.of())
        .status("ERROR")
        .build();
  }

  /**
   * 헬스체크용 정보 반환
   */
  public String getServiceInfo() {
    return String.format("Model: %s, MaxTokens: %d",
        modelName, maxTokens);
  }
}
