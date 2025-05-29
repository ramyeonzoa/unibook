package com.unibook.domain.dto;

import com.unibook.domain.entity.Subject;
import com.unibook.domain.entity.SubjectBook;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 과목-교재 매핑 정보 DTO
 */
@Data
@NoArgsConstructor
@Builder
public class SubjectBookDto {
    
    private Long subjectBookId;
    
    // 과목 정보
    private Long subjectId;
    private String subjectName;
    private Subject.SubjectType subjectType;
    
    // 교수 정보
    private Long professorId;
    private String professorName;
    
    // 학과 정보
    private Long departmentId;
    private String departmentName;
    
    // 책 정보
    private Long bookId;
    private String bookTitle;
    
    /**
     * Repository 프로젝션용 생성자
     * 필드 선언 순서와 동일하게 파라미터 배치
     */
    public SubjectBookDto(Long subjectBookId,
                         Long subjectId, String subjectName, Subject.SubjectType subjectType,
                         Long professorId, String professorName,
                         Long departmentId, String departmentName,
                         Long bookId, String bookTitle) {
        this.subjectBookId = subjectBookId;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.subjectType = subjectType;
        this.professorId = professorId;
        this.professorName = professorName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
    }
    
    /**
     * Entity -> DTO 변환
     */
    public static SubjectBookDto from(SubjectBook subjectBook) {
        Subject subject = subjectBook.getSubject();
        return SubjectBookDto.builder()
                .subjectBookId(subjectBook.getSubjectBookId())
                .subjectId(subject.getSubjectId())
                .subjectName(subject.getSubjectName())
                .subjectType(subject.getType())
                .professorId(subject.getProfessor().getProfessorId())
                .professorName(subject.getProfessor().getProfessorName())
                .departmentId(subject.getProfessor().getDepartment().getDepartmentId())
                .departmentName(subject.getProfessor().getDepartment().getDepartmentName())
                .bookId(subjectBook.getBook().getBookId())
                .bookTitle(subjectBook.getBook().getTitle())
                .build();
    }
    
    // year/semester 정보는 Post 레벨에서 관리되므로 이 메서드는 제거됨
    
    /**
     * 과목 정보 표시 문자열
     */
    public String getSubjectDisplayName() {
        return String.format("%s (%s - %s)", subjectName, departmentName, professorName);
    }
}