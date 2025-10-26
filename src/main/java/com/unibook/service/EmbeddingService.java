package com.unibook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unibook.domain.dto.ChatbotKnowledgeDto;
import com.unibook.domain.dto.EmbeddingCacheDto;
import com.unibook.domain.dto.EmbeddingMetrics;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

  @Value("${chatbot.embedding.cache.enabled:true}")
  private boolean cacheEnabled;

  @Value("${chatbot.embedding.cache.file-path:data/embeddings-cache.json}")
  private String cacheFilePath;

  @Autowired
  private EmbeddingMetricsLogger metricsLogger;

  private EmbeddingModel embeddingModel;
  private EmbeddingStore<TextSegment> embeddingStore;
  private int embeddingCount = 0;
  private String currentFaqHash;

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
   * 캐시가 있으면 캐시에서 로드, 없으면 OpenAI API 호출 후 캐시 저장
   */
  private void loadAndEmbedFaqData() throws IOException {
    long startTime = System.currentTimeMillis();
    boolean cacheHit = false;
    int apiCalls = 0;
    int totalTokens = 0;
    String cacheSource = "error";

    try {
      log.info("FAQ 데이터 로딩 시작");

      // 1. FAQ JSON 파일 읽기
      ClassPathResource resource = new ClassPathResource("chatbot/rag_seed.json");
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());

      List<ChatbotKnowledgeDto> faqList;
      try (InputStream inputStream = resource.getInputStream()) {
        faqList = objectMapper.readValue(
          inputStream,
          new TypeReference<List<ChatbotKnowledgeDto>>() {}
        );
      }

      log.info("FAQ 데이터 {}개 로드 완료", faqList.size());

      // 2. FAQ 해시 계산
      currentFaqHash = calculateFaqHash(faqList);
      log.info("FAQ 해시: {}", currentFaqHash.substring(0, 16) + "...");

      // 3. 캐시 확인 및 로드
      if (cacheEnabled && isCacheValid(currentFaqHash)) {
        log.info("✓ 유효한 캐시 발견, 로드 중...");
        loadFromCache();
        cacheHit = true;
        cacheSource = "file";
      } else {
        if (cacheEnabled) {
          log.info("✗ 캐시 없음 또는 무효, OpenAI API 호출 중...");
        } else {
          log.info("캐시 비활성화, OpenAI API 호출 중...");
        }

        // 토큰 계산
        totalTokens = calculateTotalTokens(faqList);
        apiCalls = faqList.size();

        // API로 임베딩
        embedFaqs(faqList);

        // 캐시 저장
        if (cacheEnabled) {
          saveToCache(faqList);
        }

        cacheSource = "api";
      }

    } finally {
      long loadingTime = System.currentTimeMillis() - startTime;

      // 메트릭 기록
      EmbeddingMetrics metrics = EmbeddingMetrics.builder()
        .timestamp(LocalDateTime.now())
        .cacheHit(cacheHit)
        .loadingTimeMs(loadingTime)
        .apiCalls(apiCalls)
        .totalTokens(totalTokens)
        .estimatedCostUsd(EmbeddingMetricsLogger.calculateCost(totalTokens))
        .embeddingCount(embeddingCount)
        .embeddingModel(embeddingModelName)
        .faqHash(currentFaqHash)
        .cacheSource(cacheSource)
        .build();

      metricsLogger.logMetrics(metrics);
    }
  }

  /**
   * FAQ 리스트를 임베딩하여 Vector Store에 저장
   */
  private void embedFaqs(List<ChatbotKnowledgeDto> faqList) {
    int successCount = 0;
    for (ChatbotKnowledgeDto faq : faqList) {
      try {
        // FAQ의 질문 + 답변을 합친 텍스트
        String fullText = faq.getFullText();

        // 메타데이터 생성
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

        // OpenAI API로 임베딩 생성
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
   * FAQ 리스트의 SHA-256 해시 계산
   */
  private String calculateFaqHash(List<ChatbotKnowledgeDto> faqList) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(faqList);

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));

      // 바이트 배열을 16진수 문자열로 변환
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }

      return hexString.toString();

    } catch (Exception e) {
      log.error("FAQ 해시 계산 실패", e);
      return "";
    }
  }

  /**
   * 캐시 유효성 검증
   */
  private boolean isCacheValid(String currentHash) {
    try {
      Path cachePath = Paths.get(cacheFilePath);
      if (!Files.exists(cachePath)) {
        log.debug("캐시 파일 없음: {}", cacheFilePath);
        return false;
      }

      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());

      EmbeddingCacheDto cache = mapper.readValue(cachePath.toFile(), EmbeddingCacheDto.class);

      // 해시 비교
      if (!currentHash.equals(cache.getFaqHash())) {
        log.warn("FAQ 변경 감지 (해시 불일치), 캐시 무효화");
        return false;
      }

      // 모델명 비교
      if (!embeddingModelName.equals(cache.getEmbeddingModel())) {
        log.warn("임베딩 모델 변경 감지, 캐시 무효화");
        return false;
      }

      return true;

    } catch (Exception e) {
      log.error("캐시 유효성 검증 실패", e);
      return false;
    }
  }

  /**
   * 캐시에서 임베딩 로드
   */
  private void loadFromCache() {
    try {
      Path cachePath = Paths.get(cacheFilePath);
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());

      EmbeddingCacheDto cache = mapper.readValue(cachePath.toFile(), EmbeddingCacheDto.class);

      log.info("캐시에서 {}개 임베딩 로드 중...", cache.getEmbeddings().size());

      for (EmbeddingCacheDto.CachedEmbedding cached : cache.getEmbeddings()) {
        // 메타데이터 재구성
        Metadata metadata = Metadata.from("id", cached.getId())
          .put("category", cached.getMetadata().getCategory())
          .put("question", cached.getMetadata().getQuestion());

        if (cached.getMetadata().getAnchors() != null) {
          metadata.put("anchors", cached.getMetadata().getAnchors());
        }

        // TextSegment 생성
        TextSegment segment = TextSegment.from(cached.getText(), metadata);

        // Embedding 생성
        Embedding embedding = new Embedding(cached.getVector());

        // Vector Store에 저장
        embeddingStore.add(embedding, segment);
        embeddingCount++;
      }

      log.info("캐시 로드 완료: {}개 임베딩", embeddingCount);

    } catch (Exception e) {
      log.error("캐시 로드 실패", e);
      throw new RuntimeException("캐시 로드 실패", e);
    }
  }

  /**
   * 임베딩을 캐시에 저장
   */
  private void saveToCache(List<ChatbotKnowledgeDto> faqList) {
    try {
      log.info("임베딩 캐시 저장 중...");

      // 현재 Vector Store의 임베딩을 추출
      List<EmbeddingCacheDto.CachedEmbedding> cachedEmbeddings = new ArrayList<>();

      // FAQ와 임베딩을 매칭하여 저장 (순서 유지)
      for (int i = 0; i < faqList.size() && i < embeddingCount; i++) {
        ChatbotKnowledgeDto faq = faqList.get(i);

        // 해당 FAQ를 다시 임베딩 (이미 계산된 값 재사용을 위해서는 별도 저장 필요)
        Embedding embedding = embeddingModel.embed(faq.getFullText()).content();

        EmbeddingCacheDto.Metadata metadata = EmbeddingCacheDto.Metadata.builder()
          .category(faq.getCategory())
          .question(faq.getQuestion())
          .anchors(faq.getAnchors() != null ? String.join(",", faq.getAnchors()) : null)
          .build();

        EmbeddingCacheDto.CachedEmbedding cached = EmbeddingCacheDto.CachedEmbedding.builder()
          .id(faq.getId())
          .text(faq.getFullText())
          .vector(embedding.vector())
          .metadata(metadata)
          .build();

        cachedEmbeddings.add(cached);
      }

      // 캐시 DTO 생성
      EmbeddingCacheDto cache = EmbeddingCacheDto.builder()
        .version("1.0")
        .faqHash(currentFaqHash)
        .embeddingModel(embeddingModelName)
        .dimensions(1536) // text-embedding-3-small
        .createdAt(LocalDateTime.now())
        .embeddings(cachedEmbeddings)
        .build();

      // 파일로 저장
      Path cachePath = Paths.get(cacheFilePath);
      Files.createDirectories(cachePath.getParent());

      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      mapper.writerWithDefaultPrettyPrinter().writeValue(cachePath.toFile(), cache);

      log.info("캐시 저장 완료: {}", cachePath.toAbsolutePath());

    } catch (Exception e) {
      log.error("캐시 저장 실패", e);
    }
  }

  /**
   * 총 토큰 수 계산 (추정)
   */
  private int calculateTotalTokens(List<ChatbotKnowledgeDto> faqList) {
    return faqList.stream()
      .mapToInt(faq -> {
        String text = faq.getQuestion() + " " + faq.getAnswer();
        // 한글: 약 1 token per 2.5 characters
        // 영어: 약 1 token per 4 characters
        // 평균값 사용
        return (int) Math.ceil(text.length() / 2.5);
      })
      .sum();
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
