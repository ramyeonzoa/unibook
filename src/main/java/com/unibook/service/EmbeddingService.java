package com.unibook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibook.domain.dto.ChatbotKnowledgeDto;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * FAQ 데이터를 임베딩(벡터)으로 변환하고 유사도 검색을 제공하는 서비스
 *
 * 동작 방식:
 * 1. 서버 시작 시 rag_seed.json 파일 로드
 * 2. 각 FAQ를 OpenAI Embedding API로 벡터 변환
 * 3. In-Memory Vector Store에 저장
 * 4. 사용자 질문 시 유사도 검색 수행
 */
@Service
@Slf4j
public class EmbeddingService {

  @Value("${openai.api.key}")
  private String openAiApiKey;

  @Value("${openai.api.embedding-model}")
  private String embeddingModelName;

  private EmbeddingModel embeddingModel;
  private EmbeddingStore<TextSegment> embeddingStore;
  private int embeddingCount = 0;

  /**
   * 서버 시작 시 자동 실행
   * FAQ 데이터를 로드하고 임베딩 생성
   */
  @PostConstruct
  public void init() {
    log.info("EmbeddingService 초기화 시작");

    try {
      // 1. OpenAI Embedding Model 초기화
      embeddingModel = OpenAiEmbeddingModel.builder()
          .apiKey(openAiApiKey)
          .modelName(embeddingModelName)
          .build();
      log.info("OpenAI Embedding Model 초기화 완료: {}", embeddingModelName);

      // 2. In-Memory Vector Store 생성
      embeddingStore = new InMemoryEmbeddingStore<>();
      log.info("In-Memory Vector Store 생성 완료");

      // 3. FAQ 데이터 로드 및 임베딩
      loadAndEmbedFaqData();

      log.info("EmbeddingService 초기화 완료");

    } catch (Exception e) {
      log.error("EmbeddingService 초기화 실패", e);
      throw new RuntimeException("EmbeddingService 초기화 실패", e);
    }
  }

  /**
   * rag_seed.json 파일을 읽어서 각 FAQ를 임베딩하여 저장
   */
  private void loadAndEmbedFaqData() throws IOException {
    log.info("rag_seed.json 파일 로드 중");

    // 1. JSON 파일 읽기
    ClassPathResource resource = new ClassPathResource("chatbot/rag_seed.json");
    ObjectMapper objectMapper = new ObjectMapper();

    List<ChatbotKnowledgeDto> faqList;
    try (InputStream inputStream = resource.getInputStream()) {
      faqList = objectMapper.readValue(
          inputStream,
          new TypeReference<List<ChatbotKnowledgeDto>>() {}
      );
    }

    log.info("FAQ 데이터 {}개 로드 완료", faqList.size());

    // 2. 각 FAQ를 임베딩하여 Vector Store에 저장
    int successCount = 0;
    for (ChatbotKnowledgeDto faq : faqList) {
      try {
        // FAQ의 질문 + 답변을 합친 텍스트
        String fullText = faq.getFullText();

        // 메타데이터 생성 (검색 결과에서 FAQ 식별용)
        Metadata metadata = Metadata.from("id", faq.getId())
            .put("category", faq.getCategory())
            .put("question", faq.getQuestion())
            .put("answer", faq.getAnswer());

        // 앵커가 있으면 메타데이터에 추가
        if (faq.getAnchors() != null && !faq.getAnchors().isEmpty()) {
          metadata.put("anchors", String.join(",", faq.getAnchors()));
        }

        // TextSegment 생성
        TextSegment segment = TextSegment.from(fullText, metadata);

        // OpenAI API로 임베딩 생성 (텍스트 → 벡터)
        Embedding embedding = embeddingModel.embed(fullText).content();

        // Vector Store에 저장
        embeddingStore.add(embedding, segment);
        embeddingCount++;

        successCount++;

      } catch (Exception e) {
        log.error("FAQ 임베딩 실패: {} - {}", faq.getId(), e.getMessage());
      }
    }

    log.info("FAQ 임베딩 완료: {}/{} 성공", successCount, faqList.size());
  }

  /**
   * 사용자 질문과 유사한 FAQ를 검색
   *
   * @param query 사용자 질문
   * @param maxResults 반환할 최대 결과 수
   * @param minScore 최소 유사도 점수 (0.0 ~ 1.0)
   * @return 유사한 FAQ 목록 (유사도 높은 순)
   */
  public List<EmbeddingMatch<TextSegment>> findRelevant(String query, int maxResults, double minScore) {
    log.debug("유사 FAQ 검색: query='{}', maxResults={}, minScore={}", query, maxResults, minScore);

    try {
      // 1. 사용자 질문을 벡터로 변환
      Embedding queryEmbedding = embeddingModel.embed(query).content();

      // 2. Vector Store에서 유사도 검색
      List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
          queryEmbedding,
          maxResults,
          minScore
      );

      log.debug("검색 결과: {}개 FAQ 매칭", matches.size());

      // 디버깅용 로그 (상위 3개)
      for (int i = 0; i < Math.min(3, matches.size()); i++) {
        EmbeddingMatch<TextSegment> match = matches.get(i);
        String faqId = match.embedded().metadata().getString("id");
        double score = match.score();
        log.debug("  {}. {} (유사도: {:.3f})", i + 1, faqId, score);
      }

      return matches;

    } catch (Exception e) {
      log.error("FAQ 검색 실패: {}", e.getMessage(), e);
      throw new RuntimeException("FAQ 검색 중 오류 발생", e);
    }
  }

  /**
   * 임베딩 모델 정보 조회 (헬스체크용)
   */
  public String getEmbeddingModelInfo() {
    return String.format("Model: %s, Store size: %d FAQs",
        embeddingModelName,
        embeddingCount
    );
  }
}
