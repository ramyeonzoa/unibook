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
        
        // Check if data already exists
        if (schoolRepository.count() > 0) {
            log.info("Schools already exist in database. Skipping initialization.");
            return;
        }
        
        try {
            // Load schools from email domains CSV
            int schoolCount = loadSchoolsFromCsv();
            log.info("Successfully loaded {} schools", schoolCount);
            
            // Load departments from department CSV
            int departmentCount = loadDepartmentsFromCsv();
            log.info("Successfully loaded {} departments", departmentCount);
            
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
}