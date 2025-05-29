package com.unibook.repository;

import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByDepartmentNameAndSchool_SchoolId(String departmentName, Long schoolId);
    List<Department> findBySchool_SchoolId(Long schoolId);
    List<Department> findBySchool(School school);
    Optional<Department> findBySchoolAndDepartmentName(School school, String departmentName);
    
    // 교양학부 존재 확인용 (ID 기반으로 프록시 로딩 없이 체크)
    boolean existsBySchool_SchoolIdAndDepartmentName(Long schoolId, String departmentName);
    
    // 학교 ID와 학과명으로 학과 조회 (교양학부 조회 등에 사용)
    Optional<Department> findBySchool_SchoolIdAndDepartmentName(Long schoolId, String departmentName);
}