package com.unibook.service;

import com.unibook.domain.entity.School;
import com.unibook.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolService {
    
    private final SchoolRepository schoolRepository;
    
    public List<School> getAllSchools() {
        return schoolRepository.findAll();
    }
    
    public Optional<School> getSchoolById(Long id) {
        return schoolRepository.findById(id);
    }
    
    public Optional<School> getSchoolByName(String schoolName) {
        return schoolRepository.findBySchoolName(schoolName);
    }
    
    @Transactional
    public School saveSchool(School school) {
        return schoolRepository.save(school);
    }
    
    public boolean isValidUniversityEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf("@") + 1);
        return schoolRepository.existsByAllDomainsContaining(domain);
    }
    
    public List<School> searchSchools(String keyword) {
        return schoolRepository.findBySchoolNameContaining(keyword);
    }
    
    public boolean existsBySchoolName(String schoolName) {
        return schoolRepository.existsBySchoolName(schoolName);
    }
}