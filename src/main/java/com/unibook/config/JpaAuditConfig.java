package com.unibook.config;

import com.unibook.domain.entity.BaseEntity;
import com.unibook.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return new AuditorAwareImpl();
    }

    public static class AuditorAwareImpl implements AuditorAware<Long> {
        @Override
        public Optional<Long> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() 
                || "anonymousUser".equals(authentication.getPrincipal())) {
                // 0L = 시스템 작업 (회원가입, 데이터 초기화, 스케줄러 등)
                // 실제 사용자 ID는 1부터 시작하므로 충돌 없음
                return Optional.of(BaseEntity.SYSTEM_USER_ID);
            }
            
            // 실제 사용자가 로그인한 경우
            if (authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                return Optional.of(userPrincipal.getUserId());
            }
            
            // 기본값 반환 (예상치 못한 경우도 시스템으로 처리)
            return Optional.of(BaseEntity.SYSTEM_USER_ID);
        }
    }
}