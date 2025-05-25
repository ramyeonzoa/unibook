package com.unibook.controller.api;

import com.unibook.common.AppConstants;
import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.School;
import com.unibook.repository.DepartmentRepository;
import com.unibook.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentApiController {
    
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    
    // 학교별 학과 목록 조회
    @GetMapping("/by-school/{schoolId}")
    public List<Map<String, Object>> getDepartmentsBySchool(@PathVariable Long schoolId) {
        List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
        
        return departments.stream()
                .map(dept -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", dept.getDepartmentId());
                    map.put("name", dept.getDepartmentName());
                    return map;
                })
                .collect(Collectors.toList());
    }
    
    // 학교명으로 학과 검색 (자동완성용)
    @GetMapping("/search")
    public List<Map<String, Object>> searchDepartments(@RequestParam String query,
                                                       @RequestParam(defaultValue = "" + AppConstants.DEPARTMENT_SEARCH_LIMIT) int limit) {
        // 먼저 학교명으로 검색
        List<School> schools = schoolRepository.findBySchoolNameContainingIgnoreCase(query);
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (School school : schools) {
            List<Department> departments = departmentRepository.findBySchool(school);
            
            // 학과를 이름순으로 정렬
            departments.sort((a, b) -> a.getDepartmentName().compareTo(b.getDepartmentName()));
            
            for (Department dept : departments) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", dept.getDepartmentId());
                map.put("text", school.getSchoolName() + " - " + dept.getDepartmentName());
                map.put("schoolId", school.getSchoolId());
                map.put("schoolName", school.getSchoolName());
                map.put("departmentName", dept.getDepartmentName());
                results.add(map);
            }
        }
        
        // 요청된 limit으로 제한
        return results.stream().limit(limit).collect(Collectors.toList());
    }
}