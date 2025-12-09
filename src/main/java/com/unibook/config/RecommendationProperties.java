package com.unibook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "recommendation")
@Configuration
public class RecommendationProperties {

  // 적응형 가중치 임계값
  private long minUserViewsForCollaborative = 10;
  private long minTotalViewsForCollaborative = 1000;
  private long intermediateUserViews = 30;
  private long intermediateTotalViews = 5000;

  // 가중치 설정
  private double defaultContentWeight = 0.90;
  private double defaultCollaborativeWeight = 0.10;
  private double intermediateContentWeight = 0.70;
  private double intermediateCollaborativeWeight = 0.30;
  private double balancedContentWeight = 0.50;
  private double balancedCollaborativeWeight = 0.50;

  // 최신성 계산 기준 (일)
  private long recencyDays = 30;
  private double contentRecencyBoostWeight = 0.10;

  // 다중 행동 추천 시스템 설정
  private int maxClicksToFetch = 20;
  private int maxWishlistsToFetch = 15;
  private int maxViewsToFetch = 30;
  private int collaborativeCandidateLimit = 50;
  private int personalizedCandidateLimit = 500;
  private int similarCandidateLimit = 200;

  // 시간 감쇠 설정
  private double timeDecayLambda = 0.1;
  private int timeDecayThresholdDays = 7;

  // 유사도 가중치
  private double isbnWeight = 0.50;
  private double subjectWeight = 0.25;
  private double departmentWeight = 0.15;
  private double similarityRecencyWeight = 0.10;

  // 슬롯 기반 믹싱 설정
  private boolean slotMixEnabled = true;
  private int slotMixSize = 10;
  private double slotMixPersonalizedRatio = 1.0;
  private double slotMixPopularRatio = 0.0;
  private double slotMixFreshRatio = 0.0;
  private double slotMixExploreEpsilon = 0.0;
  private int slotMixExploreSize = 2;
  private int slotMixPopularLookbackDays = 7;
  private int slotMixFreshWindowDays = 2;
  private int slotMixPopularCacheSize = 50;
  private int slotMixFreshCacheSize = 50;
  private int slotMixPopularCacheTtlSeconds = 60;
  private int slotMixFreshCacheTtlSeconds = 60;
}
