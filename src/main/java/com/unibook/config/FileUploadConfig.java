package com.unibook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    @Value("${app.file.upload-dir}")
    private String uploadDir;
    
    @Value("${app.file.post-images.path}")
    private String postImagesPath;
    
    @PostConstruct
    public void init() {
        // 업로드 디렉토리 생성
        createDirectory(uploadDir);
        createDirectory(postImagesPath);
    }
    
    private void createDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Created upload directory: " + directory.getAbsolutePath());
            }
        }
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** URL로 접근 시 실제 파일 경로 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }
}