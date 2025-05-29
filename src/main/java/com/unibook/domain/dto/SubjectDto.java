package com.unibook.domain.dto;

import com.unibook.domain.entity.Subject;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 과목 정보 DTO
 */
@Data
@NoArgsConstructor
@Builder
public class SubjectDto {
    
    private Long subjectId;
    private String subjectName;
    private Subject.SubjectType type;
    
    // 교수 정보
    private Long professorId;
    private String professorName;
    
    // 학과 정보
    private Long departmentId;
    private String departmentName;
    
    /**
     * Repository 프로젝션용 생성자
     * 필드 선언 순서와 동일하게 파라미터 배치
     */
    public SubjectDto(Long subjectId, String subjectName, Subject.SubjectType type,
                     Long professorId, String professorName,
                     Long departmentId, String departmentName) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.type = type;
        this.professorId = professorId;
        this.professorName = professorName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }
    
    /**
     * Entity -> DTO 변환
     */
    public static SubjectDto from(Subject subject) {
        return SubjectDto.builder()
                .subjectId(subject.getSubjectId())
                .subjectName(subject.getSubjectName())
                .type(subject.getType())
                .professorId(subject.getProfessor().getProfessorId())
                .professorName(subject.getProfessor().getProfessorName())
                .departmentId(subject.getProfessor().getDepartment().getDepartmentId())
                .departmentName(subject.getProfessor().getDepartment().getDepartmentName())
                .build();
    }
    
    /**
     * 표시용 이름 (과목명 + 교수명 + 학과명)
     */
    public String getDisplayName() {
        return String.format("%s (%s - %s)", 
            subjectName, professorName, departmentName);
    }
    
    /**
     * 과목 타입 표시명
     */
    public String getTypeDisplayName() {
        return type != null ? type.getDisplayName() : "";
    }
}