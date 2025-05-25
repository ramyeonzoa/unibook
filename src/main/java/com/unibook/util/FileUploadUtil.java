package com.unibook.util;

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
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        
        // 파일 크기 확인
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("파일 크기는 %dMB를 초과할 수 없습니다.", maxFileSize / (1024 * 1024))
            );
        }
        
        // 파일 확장자 확인
        String filename = file.getOriginalFilename();
        if (filename == null || !hasAllowedExtension(filename)) {
            throw new IllegalArgumentException(
                "허용되지 않는 파일 형식입니다. 허용된 확장자: " + allowedExtensions
            );
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
        
        // 저장 경로 생성
        Path uploadPath = Paths.get(uploadDir + subDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 파일 저장
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 상대 경로 반환 (DB 저장용)
        return subDirectory + uniqueFilename;
    }
    
    /**
     * 파일 삭제
     */
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir + filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 파일 삭제 실패는 로그만 남기고 예외를 던지지 않음
            System.err.println("Failed to delete file: " + filePath);
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