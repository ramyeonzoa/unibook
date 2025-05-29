package com.unibook.repository;

import com.unibook.domain.entity.Professor;
import com.unibook.domain.dto.ProfessorDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    
    // DTO 프로젝션으로 학과별 교수 조회
    @Query("SELECT new com.unibook.domain.dto.ProfessorDto(" +
           "p.professorId, p.professorName, " +
           "p.department.departmentId, p.department.departmentName) " +
           "FROM Professor p " +
           "WHERE p.department.departmentId = :deptId " +
           "ORDER BY p.professorName")
    List<ProfessorDto> findProfessorsByDepartment(@Param("deptId") Long departmentId);
    
    // 중복 체크용 - 대소문자 구분 없이
    boolean existsByProfessorNameIgnoreCaseAndDepartment_DepartmentId(String professorName, Long departmentId);
    
    // DTO 프로젝션으로 학과별 교수 조회 (페이지 지원)
    @Query(value = "SELECT new com.unibook.domain.dto.ProfessorDto(" +
                   "p.professorId, p.professorName, " +
                   "p.department.departmentId, p.department.departmentName) " +
                   "FROM Professor p " +
                   "WHERE p.department.departmentId = :deptId " +
                   "ORDER BY p.professorName",
           countQuery = "SELECT COUNT(p) FROM Professor p " +
                       "WHERE p.department.departmentId = :deptId")
    Page<ProfessorDto> findProfessorsByDepartment(@Param("deptId") Long departmentId, 
                                                 Pageable pageable);
    
    // 교수명으로 검색 - 인덱스 성능 최적화 (서비스에서 정규화된 쿼리 받음)
    @Query(value = "SELECT new com.unibook.domain.dto.ProfessorDto(" +
                   "p.professorId, p.professorName, " +
                   "p.department.departmentId, p.department.departmentName) " +
                   "FROM Professor p " +
                   "WHERE LOWER(p.professorName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%')) " +
                   "ORDER BY CASE " +
                   "  WHEN :departmentId IS NOT NULL AND p.department.departmentId = :departmentId THEN 0 " +
                   "  ELSE 1 " +
                   "END, p.professorName",
           countQuery = "SELECT COUNT(p) FROM Professor p " +
                       "WHERE LOWER(p.professorName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%'))")
    Page<ProfessorDto> findProfessorsByName(@Param("normalizedQuery") String normalizedQuery, 
                                           @Param("departmentId") Long departmentId, 
                                           Pageable pageable);
    
    // DTO 프로젝션으로 ID 기반 교수 조회 - 네이밍 일관성 개선
    @Query("SELECT new com.unibook.domain.dto.ProfessorDto(" +
           "p.professorId, p.professorName, " +
           "p.department.departmentId, p.department.departmentName) " +
           "FROM Professor p " +
           "WHERE p.professorId = :professorId")
    Optional<ProfessorDto> findProfessorDtoById(@Param("professorId") Long professorId);
    
    // findOrCreate용 단일 조회 메서드 (검색 API 재활용 방지)
    @Query("SELECT new com.unibook.domain.dto.ProfessorDto(" +
           "p.professorId, p.professorName, " +
           "p.department.departmentId, p.department.departmentName) " +
           "FROM Professor p " +
           "WHERE LOWER(p.professorName) = LOWER(:professorName) " +
           "AND p.department.departmentId = :departmentId")
    Optional<ProfessorDto> findProfessorByNameAndDepartment(@Param("professorName") String professorName,
                                                           @Param("departmentId") Long departmentId);
    
    // 학교별 교수 검색 (Phase 1 추가: 학교 경계 적용)
    @Query(value = "SELECT new com.unibook.domain.dto.ProfessorDto(" +
                   "p.professorId, p.professorName, " +
                   "p.department.departmentId, p.department.departmentName) " +
                   "FROM Professor p " +
                   "WHERE p.department.school.schoolId = :schoolId " +
                   "AND LOWER(p.professorName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%')) " +
                   "ORDER BY p.professorName",
           countQuery = "SELECT COUNT(p) FROM Professor p " +
                       "WHERE p.department.school.schoolId = :schoolId " +
                       "AND LOWER(p.professorName) LIKE LOWER(CONCAT('%', :normalizedQuery, '%'))")
    Page<ProfessorDto> findProfessorsByNameAndSchool(@Param("normalizedQuery") String normalizedQuery,
                                                    @Param("schoolId") Long schoolId,
                                                    Pageable pageable);
    
    // 특정 학교 내에서 교수명으로 검색 - 학교 경계 제한
    @Query(value = "SELECT new com.unibook.domain.dto.ProfessorDto(" +
                   "p.professorId, p.professorName, " +
                   "p.department.departmentId, p.department.departmentName) " +
                   "FROM Professor p " +
                   "WHERE p.professorName LIKE CONCAT('%', :normalizedQuery, '%') " +
                   "AND p.department.school.schoolId = :schoolId " +
                   "ORDER BY CASE WHEN p.department.departmentId = :departmentId THEN 0 ELSE 1 END, " +
                   "p.professorName",
           countQuery = "SELECT COUNT(p) FROM Professor p " +
                       "WHERE p.professorName LIKE CONCAT('%', :normalizedQuery, '%') " +
                       "AND p.department.school.schoolId = :schoolId")
    Page<ProfessorDto> searchByNameAndSchool(@Param("normalizedQuery") String normalizedQuery, 
                                           @Param("schoolId") Long schoolId,
                                           @Param("departmentId") Long departmentId, 
                                           Pageable pageable);
    
    // 특정 학교의 모든 학과에서 교수 검색 (교양학부 포함)
    @Query("SELECT new com.unibook.domain.dto.ProfessorDto(" +
           "p.professorId, p.professorName, " +
           "p.department.departmentId, p.department.departmentName) " +
           "FROM Professor p " +
           "WHERE p.professorName = :professorName " +
           "AND p.department.school.schoolId = :schoolId " +
           "ORDER BY p.department.departmentName")
    List<ProfessorDto> findProfessorsByNameAndSchool(@Param("professorName") String professorName,
                                                    @Param("schoolId") Long schoolId);
}