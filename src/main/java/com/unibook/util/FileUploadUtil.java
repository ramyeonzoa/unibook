package com.unibook.util;

import com.unibook.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FileUploadUtil {
    
    @Value("${app.file.upload-dir}")
    private String uploadDir;
    
    @Value("${app.file.max-size}")
    private long maxFileSize;
    
    @Value("${app.file.allowed-extensions}")
    private String allowedExtensions;
    
    /**
     * 파일 유효성 검증
     */
    public void validateFile(MultipartFile file) {
        // 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new ValidationException.EmptyFileException();
        }
        
        // 파일 크기 확인
        if (file.getSize() > maxFileSize) {
            throw new ValidationException.FileSizeExceededException(maxFileSize);
        }
        
        // 파일 확장자 확인
        String filename = file.getOriginalFilename();
        if (filename == null || !hasAllowedExtension(filename)) {
            throw new ValidationException.InvalidFileExtensionException(allowedExtensions);
        }
    }
    
    /**
     * 파일 저장
     */
    public String saveFile(MultipartFile file, String subDirectory) throws IOException {
        validateFile(file);
        
        // 고유한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        // 저장 경로 생성 (자동으로 디렉토리 생성)
        Path uploadPath = Paths.get(uploadDir + subDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
        }
        
        // 파일 저장
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File saved: {} -> {}", originalFilename, filePath.toAbsolutePath());
        
        // 상대 경로 반환 (DB 저장용) - /uploads 포함
        return "/uploads" + subDirectory + uniqueFilename;
    }
    
    /**
     * 파일 삭제
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.warn("파일 경로가 비어있어 삭제를 건너뜁니다.");
            return;
        }
        
        try {
            // 보안: 경로 조작 공격 방지
            if (filePath.contains("..") || filePath.contains("\\")) {
                log.error("잘못된 파일 경로: {}", filePath);
                return;
            }
            
            // URL 경로(/uploads/...)를 실제 파일 경로로 변환
            String actualPath = filePath;
            if (filePath.startsWith("/uploads/")) {
                // "/uploads/"를 제거하고 uploadDir와 결합
                actualPath = filePath.substring(9); // "/uploads/" 길이 = 9
            }
            
            // 절대 경로 생성 및 정규화
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetPath = basePath.resolve(actualPath).normalize();
            
            // 보안: 업로드 디렉토리 외부 접근 방지
            if (!targetPath.startsWith(basePath)) {
                log.error("업로드 디렉토리 외부 접근 시도: {}", filePath);
                return;
            }
            
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                log.info("파일 삭제 성공: {}", targetPath);
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다: {}", targetPath);
            }
        } catch (IOException e) {
            // 파일 삭제 실패는 로그만 남기고 예외를 던지지 않음
            log.error("파일 삭제 실패: {}", filePath, e);
        }
    }
    
    /**
     * 파일 확장자 확인
     */
    private boolean hasAllowedExtension(String filename) {
        String extension = getFileExtension(filename);
        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        return allowed.contains(extension.toLowerCase());
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 이미지 파일인지 확인
     */
    public boolean isImageFile(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(extension);
    }
}