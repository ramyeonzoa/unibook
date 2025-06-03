package com.unibook.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SuspensionRequest {
    
    @NotBlank(message = "정지 사유는 필수입니다.")
    @Size(min = 10, max = 500, message = "정지 사유는 10자 이상 500자 이하로 입력해주세요.")
    private String reason;
    
    private LocalDateTime expiresAt;  // null이면 영구정지
    
    // 편의를 위한 기간 설정 (일 단위)
    private Integer durationDays;  // null이면 영구정지
    
    public LocalDateTime getCalculatedExpiresAt() {
        if (durationDays != null && durationDays > 0) {
            return LocalDateTime.now().plusDays(durationDays);
        }
        return expiresAt;
    }
}