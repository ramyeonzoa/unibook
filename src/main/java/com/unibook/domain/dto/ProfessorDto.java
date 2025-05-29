package com.unibook.domain.dto;

import com.unibook.domain.entity.Professor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 교수 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessorDto {
    
    private Long professorId;
    private String professorName;
    private Long departmentId;
    private String departmentName;
    
    /**
     * Entity -> DTO 변환
     */
    public static ProfessorDto from(Professor professor) {
        return ProfessorDto.builder()
                .professorId(professor.getProfessorId())
                .professorName(professor.getProfessorName())
                .departmentId(professor.getDepartment().getDepartmentId())
                .departmentName(professor.getDepartment().getDepartmentName())
                .build();
    }
    
    /**
     * 표시용 이름 (교수명 + 학과명)
     */
    public String getDisplayName() {
        return String.format("%s (%s)", professorName, departmentName);
    }
}