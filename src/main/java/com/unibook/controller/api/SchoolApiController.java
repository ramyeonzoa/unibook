package com.unibook.controller.api;

import com.unibook.common.AppConstants;
import com.unibook.domain.entity.School;
import com.unibook.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolApiController {
    
    private final SchoolService schoolService;
    
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSchools(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "" + AppConstants.SCHOOL_SEARCH_LIMIT) int limit) {
        
        // 최소 2글자 이상 입력해야 검색
        if (keyword == null || keyword.trim().length() < AppConstants.MIN_SEARCH_LENGTH) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        
        List<Map<String, Object>> results = schoolService.searchSchools(keyword.trim())
                .stream()
                .limit(limit)
                .map(school -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", school.getSchoolId());
                    item.put("text", school.getSchoolName());
                    item.put("value", school.getSchoolId());
                    return item;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularSchools() {
        // 인기 학교 목록 (나중에 로직 추가)
        // 지금은 처음 10개 학교 반환
        List<Map<String, Object>> results = schoolService.getAllSchools()
                .stream()
                .limit(AppConstants.SCHOOL_SEARCH_LIMIT)
                .map(school -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", school.getSchoolId());
                    item.put("text", school.getSchoolName());
                    item.put("value", school.getSchoolId());
                    return item;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(results);
    }
}