package com.unibook.config;

import com.unibook.common.AppConstants;
import com.unibook.common.Messages;
import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.School;
import com.unibook.exception.DataInitializationException;
import com.unibook.repository.DepartmentRepository;
import com.unibook.repository.SchoolRepository;
import com.unibook.service.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolService schoolService;
    
    @Override
    public void run(String... args) {
        try {
            initializeData();
        } catch (Exception e) {
            log.error("Failed to initialize data", e);
            // 애플리케이션 시작은 계속되도록 함
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void initializeData() throws Exception {
        log.info("=== Starting Data Initialization ===");
        
        // Check if schools already exist
        boolean schoolsExist = schoolRepository.count() > 0;
        
        if (schoolsExist) {
            log.info("Schools already exist in database. Skipping school and department initialization.");
            // 교양학부는 별도로 확인하고 생성
            createGeneralEducationDepartments();
            log.info("General education departments check completed.");
            return;
        }
        
        try {
            // Load schools from email domains CSV
            int schoolCount = loadSchoolsFromCsv();
            log.info("Successfully loaded {} schools", schoolCount);
            
            // Load departments from department CSV
            int departmentCount = loadDepartmentsFromCsv();
            log.info("Successfully loaded {} departments", departmentCount);
            
            // Create general education departments for each school
            createGeneralEducationDepartments();
            log.info("Successfully created general education departments");
            
            // Verify data integrity
            verifyDataIntegrity();
            
            log.info("=== Data Initialization Completed Successfully ===");
        } catch (Exception e) {
            log.error("Error during data initialization. Rolling back all changes.", e);
            throw e;  // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
    }
    
    private int loadSchoolsFromCsv() throws Exception {
        log.info(Messages.LOG_LOADING_DATA, "schools");
        
        try {
            ClassPathResource resource = new ClassPathResource(Messages.CSV_SCHOOLS_FILE);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                // Skip header with BOM
                String header = reader.readLine();
                if (header != null && header.startsWith("\ufeff")) {
                    header = header.substring(1);
                }
                
                String line;
                int count = 0;
                Map<String, School> schoolMap = new HashMap<>();
                
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= AppConstants.MIN_CSV_FIELDS) {
                        String emailDomain = parts[0].trim();
                        String schoolName = parts[1].trim();
                        
                        // Get or create school
                        School school = schoolMap.get(schoolName);
                        if (school == null) {
                            school = School.builder()
                                    .schoolName(schoolName)
                                    .primaryDomain(emailDomain.isEmpty() ? null : emailDomain)
                                    .allDomains(new HashSet<>())
                                    .build();
                            schoolMap.put(schoolName, school);
                        }
                        
                        // Add email domain if not empty
                        if (!emailDomain.isEmpty()) {
                            school.getAllDomains().add(emailDomain);
                            // Update primary domain if it was null
                            if (school.getPrimaryDomain() == null) {
                                school.setPrimaryDomain(emailDomain);
                            }
                        }
                    }
                }
                
                // Save all schools in batches for better performance
                List<School> schoolsToSave = new ArrayList<>();
                for (School school : schoolMap.values()) {
                    if (!schoolService.existsBySchoolName(school.getSchoolName())) {
                        schoolsToSave.add(school);
                    }
                }
                
                // Batch save
                if (!schoolsToSave.isEmpty()) {
                    List<School> savedSchools = schoolRepository.saveAll(schoolsToSave);
                    count = savedSchools.size();
                }
                
                return count;
            }
        } catch (Exception e) {
            log.error("Error loading schools from CSV", e);
            throw new DataInitializationException.CsvLoadException("univ-email-250411-final.csv", e);
        }
    }
    
    private int loadDepartmentsFromCsv() throws Exception {
        log.info(Messages.LOG_LOADING_DATA, "departments");
        
        try {
            ClassPathResource resource = new ClassPathResource(Messages.CSV_DEPARTMENTS_FILE);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                // Skip header with BOM
                String header = reader.readLine();
                if (header != null && header.startsWith("\ufeff")) {
                    header = header.substring(1);
                }
                
                String line;
                int count = 0;
                
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= AppConstants.MIN_CSV_FIELDS) {
                        String schoolName = parts[0].trim();
                        String departmentName = parts[1].trim();
                        
                        // Find school
                        Optional<School> schoolOpt = schoolService.getSchoolByName(schoolName);
                        if (schoolOpt.isPresent()) {
                            School school = schoolOpt.get();
                            
                            // Check if department already exists
                            boolean exists = departmentRepository.existsByDepartmentNameAndSchool_SchoolId(
                                    departmentName, school.getSchoolId());
                            
                            if (!exists) {
                                Department department = Department.builder()
                                        .departmentName(departmentName)
                                        .school(school)
                                        .build();
                                departmentRepository.save(department);
                                count++;
                                
                                // Batch save를 위한 flush (매 100개마다)
                                if (count % AppConstants.CSV_LOG_INTERVAL == 0) {
                                    departmentRepository.flush();
                                    log.debug(Messages.LOG_CSV_FLUSH, count);
                                }
                            }
                        } else {
                            log.warn("School not found for department: {} - {}", schoolName, departmentName);
                        }
                    }
                }
                
                return count;
            }
        } catch (Exception e) {
            log.error("Error loading departments from CSV", e);
            throw new DataInitializationException.CsvLoadException("univ-dept-mapped.csv", e);
        }
    }
    
    private void verifyDataIntegrity() {
        long schoolCount = schoolRepository.count();
        long departmentCount = departmentRepository.count();
        
        log.info(Messages.LOG_DATA_INTEGRITY_CHECK, schoolCount, departmentCount);
        
        // 최소한의 데이터가 있는지 확인
        if (schoolCount == 0) {
            throw new DataInitializationException.DataIntegrityException(Messages.SCHOOL_DATA_LOAD_FAILED);
        }
        
        if (departmentCount == 0) {
            throw new DataInitializationException.DataIntegrityException(Messages.DEPARTMENT_DATA_LOAD_FAILED);
        }
        
        // 모든 학과가 학교를 가지고 있는지 확인
        long orphanDepartments = departmentRepository.findAll().stream()
                .filter(dept -> dept.getSchool() == null)
                .count();
                
        if (orphanDepartments > 0) {
            throw new DataInitializationException.DataIntegrityException(
                String.format(Messages.DEPARTMENT_INTEGRITY_ERROR, orphanDepartments)
            );
        }
        
        log.info(Messages.LOG_DATA_INTEGRITY_PASSED);
    }
    
    /**
     * 모든 학교에 교양학부를 추가하는 메서드
     * 각 학교별로 교양 과목들을 관리하기 위한 기본 학과 생성
     * 
     * 성능 최적화:
     * 1. findAll()로 한 번에 모든 학교 조회 (N+1 쿼리 방지)
     * 2. ID 기반 exists 체크로 프록시 로딩 방지
     * 3. 배치 저장 후 한 번에 flush
     */
    @Transactional
    public void createGeneralEducationDepartments() {
        log.info("교양학부 생성 시작...");
        int createdCount = 0;
        int existingCount = 0;
        int errorCount = 0;
        
        // 모든 학교를 한 번에 조회 (네트워크 호출 1회)
        List<School> schools = schoolRepository.findAll();
        log.info("총 {}개 학교에 대해 교양학부 생성 검토", schools.size());
        
        List<Department> newDepartments = new ArrayList<>();
        
        for (School school : schools) {
            try {
                // ID 기반 존재 확인 (프록시 로딩 없음)
                boolean exists = departmentRepository.existsBySchool_SchoolIdAndDepartmentName(
                        school.getSchoolId(), AppConstants.GENERAL_EDUCATION_DEPT_NAME);
                
                if (exists) {
                    existingCount++;
                    log.trace("{} - 교양학부가 이미 존재함", school.getSchoolName());
                    continue;
                }
                
                // 교양학부 생성 (메모리에만 저장)
                Department generalDept = Department.builder()
                        .departmentName(AppConstants.GENERAL_EDUCATION_DEPT_NAME)
                        .school(school)
                        .build();
                
                newDepartments.add(generalDept);
                createdCount++;
                log.debug("{} - 교양학부 생성 예정", school.getSchoolName());
                
            } catch (Exception e) {
                errorCount++;
                log.error("학교 {}에 대한 교양학부 생성 준비 실패: {}", 
                         school.getSchoolName(), e.getMessage());
            }
        }
        
        // 배치로 한 번에 저장
        if (!newDepartments.isEmpty()) {
            try {
                departmentRepository.saveAll(newDepartments);
                departmentRepository.flush(); // 제약조건 위반 즉시 감지
                log.info("교양학부 {}개 배치 저장 완료", newDepartments.size());
                
            } catch (DataIntegrityViolationException e) {
                // 동시성 문제 발생 시 개별 저장으로 fallback
                log.warn("배치 저장 실패, 개별 저장으로 전환: {}", e.getMessage());
                createdCount = 0; // 재계산
                
                for (Department dept : newDepartments) {
                    try {
                        // 다시 한 번 존재 확인 후 저장
                        boolean stillNotExists = !departmentRepository.existsBySchool_SchoolIdAndDepartmentName(
                                dept.getSchool().getSchoolId(), dept.getDepartmentName());
                        
                        if (stillNotExists) {
                            departmentRepository.saveAndFlush(dept);
                            createdCount++;
                        } else {
                            existingCount++;
                        }
                    } catch (DataIntegrityViolationException ignored) {
                        // 다른 트랜잭션에서 이미 생성됨
                        existingCount++;
                    }
                }
            }
        }
        
        log.info("교양학부 생성 완료 - 생성: {}개, 기존: {}개, 오류: {}개", 
                createdCount, existingCount, errorCount);
        
        if (errorCount > 0) {
            log.warn("일부 학교의 교양학부 생성에 실패했습니다. 로그를 확인하세요.");
        }
    }
}