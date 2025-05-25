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
}