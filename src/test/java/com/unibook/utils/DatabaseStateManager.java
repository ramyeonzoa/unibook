package com.unibook.utils;

import com.unibook.benchmark.BenchmarkConfig;
import com.unibook.repository.DepartmentRepository;
import com.unibook.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * 벤치마크를 위한 데이터베이스 상태 관리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseStateManager {
    
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final BenchmarkConfig benchmarkConfig;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * 벤치마크 시작 전 데이터베이스 상태 검증 및 초기화
     */
    @Transactional(readOnly = true)
    public void ensureConsistentState() {
        log.info("=== 데이터베이스 상태 검증 시작 ===");
        
        // 1. 테스트용 학교 데이터 존재 확인
        validateTestSchools();
        
        // 2. 학교별 학과 데이터 존재 확인  
        validateDepartmentData();
        
        // 3. 데이터 일관성 검증
        validateDataConsistency();
        
        log.info("=== 데이터베이스 상태 검증 완료 ===");
    }
    
    /**
     * 테스트용 학교들이 존재하는지 확인
     */
    private void validateTestSchools() {
        Long[] testSchoolIds = benchmarkConfig.getTestSchoolIds();
        
        for (Long schoolId : testSchoolIds) {
            boolean exists = schoolRepository.existsById(schoolId);
            if (!exists) {
                throw new IllegalStateException(
                    String.format("테스트용 학교 ID %d가 데이터베이스에 존재하지 않습니다.", schoolId));
            }
        }
        
        log.info("테스트용 학교 {}개 검증 완료", testSchoolIds.length);
    }
    
    /**
     * 학교별 학과 데이터 존재 확인
     */
    private void validateDepartmentData() {
        Long[] testSchoolIds = benchmarkConfig.getTestSchoolIds();
        
        for (Long schoolId : testSchoolIds) {
            long departmentCount = departmentRepository.countBySchool_SchoolId(schoolId);
            if (departmentCount == 0) {
                log.warn("학교 ID {}에 학과 데이터가 없습니다.", schoolId);
            } else {
                log.debug("학교 ID {}: {}개 학과", schoolId, departmentCount);
            }
        }
        
        log.info("학과 데이터 검증 완료");
    }
    
    /**
     * 데이터 일관성 검증
     */
    private void validateDataConsistency() {
        // 1. Orphaned Department 체크 (School 없는 Department)
        long orphanedDepartments = entityManager.createQuery(
            "SELECT COUNT(d) FROM Department d WHERE d.school IS NULL", Long.class)
            .getSingleResult();
        
        if (orphanedDepartments > 0) {
            log.warn("School이 없는 Department가 {}개 있습니다.", orphanedDepartments);
        }
        
        // 2. 중복 데이터 체크
        long duplicateSchools = entityManager.createQuery(
            "SELECT COUNT(s) FROM School s GROUP BY s.schoolName HAVING COUNT(s) > 1", Long.class)
            .getResultList().size();
        
        if (duplicateSchools > 0) {
            log.warn("중복된 학교명이 {}개 있습니다.", duplicateSchools);
        }
        
        log.info("데이터 일관성 검증 완료 - Orphaned: {}, Duplicates: {}", 
                orphanedDepartments, duplicateSchools);
    }
    
    /**
     * 벤치마크 테스트 후 정리
     */
    public void cleanup() {
        // 현재는 읽기 전용 테스트이므로 특별한 정리 불필요
        // 필요시 임시 데이터 정리 로직 추가
        log.info("데이터베이스 정리 완료");
    }
    
    /**
     * 캐시 클리어 (성능 테스트 정확도 향상)
     */
    public void clearCaches() {
        // JPA 1차 캐시 클리어
        entityManager.clear();
        
        // Hibernate 2차 캐시 클리어 (설정되어 있다면)
        entityManager.getEntityManagerFactory().getCache().evictAll();
        
        log.debug("캐시 클리어 완료");
    }
    
    /**
     * 데이터베이스 통계 정보 출력
     */
    @Transactional(readOnly = true)
    public void printDatabaseStats() {
        long totalSchools = schoolRepository.count();
        long totalDepartments = departmentRepository.count();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 데이터베이스 통계");
        System.out.println("=".repeat(50));
        System.out.printf("전체 학교 수: %,d개\n", totalSchools);
        System.out.printf("전체 학과 수: %,d개\n", totalDepartments);
        System.out.printf("학교당 평균 학과 수: %.1f개\n", (double) totalDepartments / totalSchools);
        
        // 테스트 대상 학교별 학과 수
        System.out.println("\n📋 테스트 대상 학교별 학과 수:");
        for (Long schoolId : benchmarkConfig.getTestSchoolIds()) {
            long deptCount = departmentRepository.countBySchool_SchoolId(schoolId);
            String schoolName = schoolRepository.findById(schoolId)
                    .map(school -> school.getSchoolName())
                    .orElse("Unknown");
            System.out.printf("  • %s (ID: %d): %,d개\n", schoolName, schoolId, deptCount);
        }
        System.out.println("=".repeat(50));
    }
    
    /**
     * 특정 학교의 학과 수 조회
     */
    @Transactional(readOnly = true)
    public long getDepartmentCount(Long schoolId) {
        return departmentRepository.countBySchool_SchoolId(schoolId);
    }
}