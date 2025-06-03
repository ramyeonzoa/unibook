package com.unibook.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UnsuspendRequest {
    
    @NotBlank(message = "해제 사유는 필수입니다.")
    @Size(min = 5, max = 500, message = "해제 사유는 5자 이상 500자 이하로 입력해주세요.")
    private String reason;
}