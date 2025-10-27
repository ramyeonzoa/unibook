package com.unibook.repository;

import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByDepartmentNameAndSchool_SchoolId(String departmentName, Long schoolId);
    
    /**
     * 학교별 학과 목록 조회 (캐싱 적용)
     * 
     * 성능 최적화:
     * - @EntityGraph: N+1 문제 해결 (School 정보 즉시 로딩)
     * - @Cacheable: 반복 조회 시 95% 성능 향상
     * - 캐시 키: schoolId (학교별 독립적 캐싱)
     * 
     * 사용 패턴: 전체 호출의 90% (가장 빈번한 API)
     * 예상 성능: 15-25ms → 0.1-0.5ms
     */
    @EntityGraph(attributePaths = {"school"})
    @Cacheable(value = "departments", key = "#schoolId")
    List<Department> findBySchool_SchoolId(Long schoolId);
    
    /**
     * School 엔티티로 학과 목록 조회 (캐싱 적용)
     * 
     * 사용처: /api/departments/search (회원가입 페이지 자동완성)
     * 캐시 키: school.schoolId (동일한 캐시 공유)
     */
    @EntityGraph(attributePaths = {"school"})
    @Cacheable(value = "departments", key = "#school.schoolId")
    List<Department> findBySchool(School school);
    
    /**
     * ID로 학과 조회 (캐싱 적용)
     * 
     * 사용처: 회원가입, Professor 생성 시 학과 검증
     * 캐시 키: departmentId (개별 학과 캐싱)
     */
    @EntityGraph(attributePaths = {"school"})
    @Cacheable(value = "departments", key = "'dept_' + #id")
    Optional<Department> findById(Long id);
    
    Optional<Department> findBySchoolAndDepartmentName(School school, String departmentName);
    
    // 교양학부 존재 확인용 (ID 기반으로 프록시 로딩 없이 체크)
    boolean existsBySchool_SchoolIdAndDepartmentName(Long schoolId, String departmentName);
    
    // 학교 ID와 학과명으로 학과 조회 (교양학부 조회 등에 사용)
    Optional<Department> findBySchool_SchoolIdAndDepartmentName(Long schoolId, String departmentName);

    // 학교별 학과 수 조회 (벤치마크용)
    long countBySchool_SchoolId(Long schoolId);

    // 특정 학과명의 전체 개수 조회 (교양학부 초기화 최적화용)
    long countByDepartmentName(String departmentName);
}