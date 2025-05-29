package com.unibook.repository;

import com.unibook.domain.entity.Subject;
import com.unibook.domain.dto.SubjectDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    // 교수의 과목 조회 - 페이징 지원
    @Query("SELECT new com.unibook.domain.dto.SubjectDto(" +
           "s.subjectId, s.subjectName, s.type, " +
           "s.professor.professorId, s.professor.professorName, " +
           "s.professor.department.departmentId, s.professor.department.departmentName) " +
           "FROM Subject s " +
           "WHERE s.professor.professorId = :professorId " +
           "ORDER BY s.subjectName")
    Page<SubjectDto> findSubjectsByProfessor(@Param("professorId") Long professorId, Pageable pageable);
    
    // 중복 체크 - 과목명+교수ID로 체크 (서비스에서 정규화 처리)
    boolean existsBySubjectNameAndProfessor_ProfessorId(
        String subjectName, Long professorId);
    
    // 과목명으로 검색 - 영문 혼용 케이스 고려하여 LOWER() 함수 사용
    @Query(value = "SELECT new com.unibook.domain.dto.SubjectDto(" +
                   "s.subjectId, s.subjectName, s.type, " +
                   "s.professor.professorId, s.professor.professorName, " +
                   "s.professor.department.departmentId, s.professor.department.departmentName) " +
                   "FROM Subject s " +
                   "WHERE LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%')) " +
                   "ORDER BY CASE " +
                   "  WHEN :departmentId IS NOT NULL AND s.professor.department.departmentId = :departmentId THEN 0 " +
                   "  ELSE 1 " +
                   "END, s.subjectName, s.professor.professorName",
           countQuery = "SELECT COUNT(s) FROM Subject s " +
                       "WHERE LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%'))")
    Page<SubjectDto> findSubjectsByName(@Param("normalizedQuery") String normalizedQuery, 
                                       @Param("departmentId") Long departmentId,
                                       Pageable pageable);
    
    // 학과별 과목 조회 - 교양/전공 구분 포함, 페이징 지원
    @Query("SELECT new com.unibook.domain.dto.SubjectDto(" +
           "s.subjectId, s.subjectName, s.type, " +
           "s.professor.professorId, s.professor.professorName, " +
           "s.professor.department.departmentId, s.professor.department.departmentName) " +
           "FROM Subject s " +
           "WHERE s.professor.department.departmentId = :departmentId " +
           "AND (:type IS NULL OR s.type = :type) " +
           "ORDER BY s.type, s.subjectName")
    Page<SubjectDto> findSubjectsByDepartmentAndType(@Param("departmentId") Long departmentId,
                                                     @Param("type") Subject.SubjectType type,
                                                     Pageable pageable);
    
    // SubjectDto 단건 조회 (API에서 사용)
    @Query("SELECT new com.unibook.domain.dto.SubjectDto(" +
           "s.subjectId, s.subjectName, s.type, " +
           "s.professor.professorId, s.professor.professorName, " +
           "s.professor.department.departmentId, s.professor.department.departmentName) " +
           "FROM Subject s " +
           "WHERE s.subjectId = :subjectId")
    java.util.Optional<SubjectDto> findSubjectDtoById(@Param("subjectId") Long subjectId);
    
    // 과목명과 교수ID로 Subject Entity 조회 - Spring Data 네이밍 규칙 준수
    java.util.Optional<Subject> findBySubjectNameAndProfessor_ProfessorId(
        String subjectName, Long professorId);
    
    // 학교별 과목 검색 (Phase 1 추가: 학교 경계 적용)
    @Query(value = "SELECT new com.unibook.domain.dto.SubjectDto(" +
                   "s.subjectId, s.subjectName, s.type, " +
                   "s.professor.professorId, s.professor.professorName, " +
                   "s.professor.department.departmentId, s.professor.department.departmentName) " +
                   "FROM Subject s " +
                   "WHERE s.professor.department.school.schoolId = :schoolId " +
                   "AND LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%')) " +
                   "ORDER BY s.subjectName, s.professor.professorName",
           countQuery = "SELECT COUNT(s) FROM Subject s " +
                       "WHERE s.professor.department.school.schoolId = :schoolId " +
                       "AND LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%'))")
    Page<SubjectDto> findSubjectsByNameAndSchool(@Param("normalizedQuery") String normalizedQuery,
                                                @Param("schoolId") Long schoolId,
                                                Pageable pageable);
}