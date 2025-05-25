package com.unibook.config;

import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.School;
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
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== Starting Data Initialization ===");
        
        // Check if data already exists
        if (schoolRepository.count() > 0) {
            log.info("Schools already exist in database. Skipping initialization.");
            return;
        }
        
        // Load schools from email domains CSV
        loadSchoolsFromCsv();
        
        // Load departments from department CSV
        loadDepartmentsFromCsv();
        
        log.info("=== Data Initialization Completed ===");
    }
    
    private void loadSchoolsFromCsv() {
        log.info("Loading schools from CSV...");
        
        try {
            ClassPathResource resource = new ClassPathResource("data/univ-email-250411-final.csv");
            
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
                    if (parts.length >= 2) {
                        String emailDomain = parts[0].trim();
                        String schoolName = parts[1].trim();
                        
                        // Get or create school
                        School school = schoolMap.get(schoolName);
                        if (school == null) {
                            school = new School();
                            school.setSchoolName(schoolName);
                            school.setAllDomains(new HashSet<>());
                            schoolMap.put(schoolName, school);
                        }
                        
                        // Add email domain
                        school.getAllDomains().add(emailDomain);
                        // Set primary domain (first domain for the school)
                        if (school.getPrimaryDomain() == null) {
                            school.setPrimaryDomain(emailDomain);
                        }
                    }
                }
                
                // Save all schools
                for (School school : schoolMap.values()) {
                    if (!schoolService.existsBySchoolName(school.getSchoolName())) {
                        schoolRepository.save(school);
                        count++;
                    }
                }
                
                log.info("Loaded {} schools from CSV", count);
            }
        } catch (Exception e) {
            log.error("Error loading schools from CSV", e);
        }
    }
    
    private void loadDepartmentsFromCsv() {
        log.info("Loading departments from CSV...");
        
        try {
            ClassPathResource resource = new ClassPathResource("data/univ-dept-mapped.csv");
            
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
                    if (parts.length >= 2) {
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
                                Department department = new Department();
                                department.setDepartmentName(departmentName);
                                department.setSchool(school);
                                departmentRepository.save(department);
                                count++;
                            }
                        } else {
                            log.warn("School not found for department: {} - {}", schoolName, departmentName);
                        }
                    }
                }
                
                log.info("Loaded {} departments from CSV", count);
            }
        } catch (Exception e) {
            log.error("Error loading departments from CSV", e);
        }
    }
}