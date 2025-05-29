package com.unibook.controller.dto;

import com.unibook.domain.entity.Subject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 과목과 교수를 함께 생성하는 요청 DTO (Phase 1)
 * 과목명 우선 UX를 위한 통합 요청
 */
@Schema(description = "과목 및 교수 통합 생성 요청")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectWithProfessorRequest {
    
    @Schema(description = "과목명", example = "경영학원론", required = true)
    @NotBlank(message = "과목명은 필수입니다")
    @Size(min = 1, max = 100, message = "과목명은 1자 이상 100자 이하여야 합니다")
    private String subjectName;
    
    @Schema(description = "교수명", example = "김철수", required = true)
    @NotBlank(message = "교수명은 필수입니다")
    @Size(min = 1, max = 50, message = "교수명은 1자 이상 50자 이하여야 합니다")
    private String professorName;
    
    @Schema(description = "학과 ID (선택사항, 교양과목은 null 가능)", example = "1")
    private Long departmentId;
    
    @Schema(description = "과목 타입", example = "MAJOR", defaultValue = "MAJOR")
    @Builder.Default
    private Subject.SubjectType subjectType = Subject.SubjectType.MAJOR;
    
    /**
     * 교차 필드 검증: 전공과목은 학과ID 필수
     */
    @AssertTrue(message = "전공과목은 학과를 선택해야 합니다")
    public boolean isDepartmentIdValid() {
        // 교양과목은 departmentId가 null일 수도 있고 값이 있을 수도 있음
        if (subjectType == Subject.SubjectType.GENERAL) {
            return true; // 교양과목은 항상 유효
        }
        // 전공과목은 departmentId가 필수
        return departmentId != null;
    }
}