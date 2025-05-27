package com.unibook.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * 인터셉터 등록 및 기타 웹 관련 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final VerificationInterceptor verificationInterceptor;
    
    @Value("${app.file.upload-dir}")
    private String uploadDir;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(verificationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/error", "/favicon.ico", "/uploads/**"
                );
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 파일을 URL로 접근할 수 있도록 설정
        // 절대 경로와 상대 경로 모두 처리
        String location = uploadDir.startsWith("/") ? "file:" + uploadDir : "file:./" + uploadDir;
        if (!location.endsWith("/")) {
            location += "/";
        }
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
        
        log.info("정적 리소스 핸들러 등록: /uploads/** -> {}", location);
    }
}