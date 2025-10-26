package com.unibook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unibook.domain.dto.ChatbotResponseDto;
import com.unibook.domain.dto.EvaluationQuestion;
import com.unibook.domain.dto.EvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 챗봇 자동 평가 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotEvaluationService {

  private final ChatbotService chatbotService;
  private final EmbeddingService embeddingService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 평가 데이터셋 로드
   */
  public List<EvaluationQuestion> loadEvaluationDataset() throws IOException {
    ClassPathResource resource = new ClassPathResource("chatbot/evaluation-dataset.json");

    try (InputStream inputStream = resource.getInputStream()) {
      return objectMapper.readValue(
        inputStream,
        new TypeReference<List<EvaluationQuestion>>() {}
      );
    }
  }

  /**
   * 챗봇 평가 실행
   */
  public EvaluationResult evaluate() throws IOException {
    return evaluate(null);
  }

  /**
   * 챗봇 평가 실행 (description 포함)
   */
  public EvaluationResult evaluate(String description) throws IOException {
    log.info("챗봇 평가 시작");

    List<EvaluationQuestion> dataset = loadEvaluationDataset();
    log.info("평가 질문 {}개 로드 완료", dataset.size());

    List<EvaluationResult.QuestionResult> questionResults = new ArrayList<>();
    int correctCount = 0;
    int totalKeywords = 0;
    int foundKeywords = 0;
    long totalResponseTime = 0;
    double totalReciprocalRank = 0.0; // MRR 계산용
    int mrrQuestionCount = 0; // MRR 계산 대상 질문 수

    // 난이도별 통계
    Map<String, Integer> correctByDifficulty = new HashMap<>();
    Map<String, Integer> totalByDifficulty = new HashMap<>();

    for (EvaluationQuestion question : dataset) {
      long startTime = System.currentTimeMillis();

      try {
        // 챗봇에게 질문
        ChatbotResponseDto response = chatbotService.chat(question.getQuestion());
        long responseTime = System.currentTimeMillis() - startTime;
        totalResponseTime += responseTime;

        // 매칭 여부 판단
        boolean actuallyMatched = response.getSources() != null && !response.getSources().isEmpty();

        // 정답 판단
        boolean correct = (actuallyMatched == question.isShouldMatch());

        if (correct) {
          correctCount++;
          correctByDifficulty.merge(question.getDifficulty(), 1, Integer::sum);
        }

        // 난이도별 총 개수
        totalByDifficulty.merge(question.getDifficulty(), 1, Integer::sum);

        // 키워드 체크
        int keywordsFoundInQuestion = 0;
        if (question.getMustIncludeKeywords() != null) {
          totalKeywords += question.getMustIncludeKeywords().size();

          String answer = response.getAnswer() != null ? response.getAnswer() : "";
          for (String keyword : question.getMustIncludeKeywords()) {
            if (answer.contains(keyword)) {
              keywordsFoundInQuestion++;
              foundKeywords++;
            }
          }
        }

        // MRR 계산 (shouldMatch가 true인 질문에 대해서만)
        if (question.isShouldMatch() && question.getRelevantFaqIds() != null && !question.getRelevantFaqIds().isEmpty()) {
          mrrQuestionCount++;

          // 응답에서 반환된 FAQ ID 목록 추출
          if (response.getSources() != null && !response.getSources().isEmpty()) {
            List<String> returnedFaqIds = response.getSources().stream()
              .map(ChatbotResponseDto.SourceInfo::getFaqId)
              .toList();

            // 정답 FAQ가 몇 번째에 있는지 찾기
            int rank = -1;
            for (int i = 0; i < returnedFaqIds.size(); i++) {
              if (question.getRelevantFaqIds().contains(returnedFaqIds.get(i))) {
                rank = i + 1; // 1-based index
                break;
              }
            }

            // Reciprocal Rank 계산 (정답을 찾은 경우)
            if (rank > 0) {
              totalReciprocalRank += 1.0 / rank;
            }
            // 정답을 못 찾은 경우 0 (이미 totalReciprocalRank에 더하지 않음)
          }
          // 매칭 결과가 없으면 0 (이미 totalReciprocalRank에 더하지 않음)
        }

        // 개별 결과 저장
        EvaluationResult.QuestionResult qResult = EvaluationResult.QuestionResult.builder()
          .questionId(question.getId())
          .question(question.getQuestion())
          .difficulty(question.getDifficulty())
          .shouldMatch(question.isShouldMatch())
          .actuallyMatched(actuallyMatched)
          .correct(correct)
          .keywordsFound(keywordsFoundInQuestion)
          .totalKeywords(question.getMustIncludeKeywords() != null ? question.getMustIncludeKeywords().size() : 0)
          .answer(response.getAnswer())
          .responseTimeMs(responseTime)
          .build();

        questionResults.add(qResult);

        log.info("[{}/{}] {} | 예상:{} 실제:{} 정답:{} ({}ms)",
          questionResults.size(), dataset.size(),
          question.getQuestion(),
          question.isShouldMatch() ? "매칭" : "거부",
          actuallyMatched ? "매칭" : "거부",
          correct ? "O" : "X",
          responseTime
        );

      } catch (Exception e) {
        log.error("평가 중 오류: {}", question.getQuestion(), e);
      }
    }

    // 난이도별 정확도 계산
    Map<String, Double> accuracyByDifficulty = new HashMap<>();
    for (String difficulty : totalByDifficulty.keySet()) {
      int total = totalByDifficulty.get(difficulty);
      int correct = correctByDifficulty.getOrDefault(difficulty, 0);
      accuracyByDifficulty.put(difficulty, (double) correct / total);
    }

    // MRR 계산
    double mrr = mrrQuestionCount > 0 ? totalReciprocalRank / mrrQuestionCount : 0.0;

    // 전체 결과 생성
    EvaluationResult result = EvaluationResult.builder()
      .timestamp(LocalDateTime.now())
      .faqCount(embeddingService.getFaqCount())
      .threshold(0.625)
      .totalQuestions(dataset.size())
      .correctAnswers(correctCount)
      .accuracy((double) correctCount / dataset.size())
      .keywordCoverage(totalKeywords > 0 ? (double) foundKeywords / totalKeywords : 0.0)
      .mrr(mrr)
      .accuracyByDifficulty(accuracyByDifficulty)
      .avgResponseTimeMs((double) totalResponseTime / dataset.size())
      .questionResults(questionResults)
      .build();

    // 결과 저장
    saveResult(result, description);

    log.info("═══════════════════════════════════════════");
    log.info("📊 챗봇 평가 결과");
    log.info("─────────────────────────────────────────");
    log.info("  총 질문: {}개", result.getTotalQuestions());
    log.info("  정답: {}개", result.getCorrectAnswers());
    log.info("  정확도: {}", String.format("%.1f%%", result.getAccuracy() * 100));
    log.info("  키워드 커버리지: {}", String.format("%.1f%%", result.getKeywordCoverage() * 100));
    log.info("  MRR (Mean Reciprocal Rank): {}", String.format("%.4f", result.getMrr()));
    log.info("  평균 응답 시간: {} ms", String.format("%.0f", result.getAvgResponseTimeMs()));
    log.info("  난이도별 정확도:");
    accuracyByDifficulty.forEach((difficulty, accuracy) ->
      log.info("    - {}: {}", difficulty, String.format("%.1f%%", accuracy * 100))
    );
    log.info("═══════════════════════════════════════════");

    return result;
  }

  /**
   * 평가 결과 저장
   */
  private void saveResult(EvaluationResult result, String description) {
    try {
      // data 디렉터리 확인
      Path dataDir = Paths.get("data");
      if (!Files.exists(dataDir)) {
        Files.createDirectories(dataDir);
      }

      // JSON 파일로 저장
      String filename = String.format("evaluation-result-%s.json",
        result.getTimestamp().toString().replace(":", "-"));
      Path jsonPath = dataDir.resolve(filename);

      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      mapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), result);

      log.info("평가 결과 저장 완료: {}", jsonPath.toAbsolutePath());

      // CSV 요약본도 저장
      saveSummaryToCsv(result, description);

    } catch (Exception e) {
      log.error("평가 결과 저장 실패", e);
    }
  }

  /**
   * CSV 요약 저장
   */
  private void saveSummaryToCsv(EvaluationResult result, String description) throws IOException {
    Path csvPath = Paths.get("data/evaluation-summary.csv");

    // 헤더 생성 (파일이 없을 경우)
    if (!Files.exists(csvPath)) {
      String header = "timestamp,faq_count,threshold,total_questions,correct_answers,accuracy,keyword_coverage,mrr,avg_response_ms,description\n";
      Files.writeString(csvPath, header);
    }

    // 데이터 추가
    String line = String.format("%s,%d,%.2f,%d,%d,%.4f,%.4f,%.4f,%.2f,%s\n",
      result.getTimestamp().toString(),
      result.getFaqCount(),
      result.getThreshold(),
      result.getTotalQuestions(),
      result.getCorrectAnswers(),
      result.getAccuracy(),
      result.getKeywordCoverage(),
      result.getMrr(),
      result.getAvgResponseTimeMs(),
      description != null ? description : ""
    );

    Files.writeString(csvPath, line, StandardOpenOption.APPEND);
    log.info("CSV 요약 저장 완료: {}", csvPath.toAbsolutePath());
  }
}
