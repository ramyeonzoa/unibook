package com.unibook.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정
 * 
 * 현재: Spring의 기본 ConcurrentMapCache 사용 (TTL 없음)
 * 
 * TODO: 프로덕션 환경에서는 Caffeine Cache 도입 고려
 * - TTL(Time To Live) 설정 가능
 * - 최대 크기 제한 가능
 * - 자동 만료 및 제거 정책 설정 가능
 * 
 * Caffeine 도입 시:
 * 1. build.gradle에 의존성 추가: implementation 'com.github.ben-manes.caffeine:caffeine'
 * 2. CaffeineCacheManager Bean 설정
 * 3. 캐시별 TTL 및 크기 제한 설정
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // 현재는 application.yml의 spring.cache 설정만 사용
    // 향후 Caffeine 도입 시 이곳에 CacheManager Bean 추가
}