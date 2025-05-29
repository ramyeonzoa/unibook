package com.unibook.controller.dto;

import com.unibook.domain.entity.Subject;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * 과목 선택 요청 DTO
 * JSR-303 교차 필드 검증 포함
 */
public class SubjectSelectionRequest {
    
    private Long subjectId;          // 기존 과목 선택 시
    
    @Size(min = 1, max = 100, message = "과목명은 1자 이상 100자 이하여야 합니다")
    private String subjectName;      // 새 과목 생성 시
    
    private Long professorId;        // 새 과목 생성 시
    
    private Subject.SubjectType subjectType; // 과목 타입 (선택사항)
    
    /**
     * 교차 필드 검증: subjectId가 있거나, subjectName+professorId가 있어야 함
     * @AssertTrue로 자동 검증
     */
    @AssertTrue(message = "과목 ID 또는 과목명+교수ID가 필요합니다")
    public boolean isValidSelection() {
        return subjectId != null || (subjectName != null && professorId != null);
    }
    
    // Getters and Setters
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    
    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }
    
    public Subject.SubjectType getSubjectType() { return subjectType; }
    public void setSubjectType(Subject.SubjectType subjectType) { this.subjectType = subjectType; }
    
    @Override
    public String toString() {
        return String.format("SubjectSelectionRequest{subjectId=%d, subjectName='%s', professorId=%d, subjectType=%s}", 
                           subjectId, subjectName, professorId, subjectType);
    }
}