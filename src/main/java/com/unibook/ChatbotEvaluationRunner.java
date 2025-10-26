package com.unibook;

import com.unibook.domain.dto.EvaluationResult;
import com.unibook.service.ChatbotEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 챗봇 평가 실행 전용 Runner
 *
 * 사용법:
 * ./gradlew bootRun --args='--spring.profiles.active=evaluation'
 */
@Slf4j
@Component
@Profile("evaluation")
@RequiredArgsConstructor
public class ChatbotEvaluationRunner implements CommandLineRunner {

  private final ChatbotEvaluationService chatbotEvaluationService;

  @Override
  public void run(String... args) throws Exception {
    log.info("═══════════════════════════════════════════");
    log.info("챗봇 자동 평가 시작");
    log.info("═══════════════════════════════════════════");

    try {
      EvaluationResult result = chatbotEvaluationService.evaluate();

      log.info("═══════════════════════════════════════════");
      log.info("평가 완료!");
      log.info("═══════════════════════════════════════════");

      // 애플리케이션 종료
      System.exit(0);

    } catch (Exception e) {
      log.error("평가 실행 중 오류 발생", e);
      System.exit(1);
    }
  }
}
