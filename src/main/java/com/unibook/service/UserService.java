package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.common.Messages;
import com.unibook.domain.dto.SignupRequestDto;
import com.unibook.domain.dto.UserResponseDto;
import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.User;
import com.unibook.exception.ValidationException;
import com.unibook.repository.DepartmentRepository;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)  // 동시 가입 방지
    public UserResponseDto signup(SignupRequestDto signupDto) {
        log.info(Messages.LOG_SIGNUP_PROCESSING, signupDto.getEmail());
        
        // 이메일 중복 체크
        if (existsByEmail(signupDto.getEmail())) {
            throw new ValidationException.EmailAlreadyExistsException(signupDto.getEmail());
        }
        
        // 학과 확인
        Department department = departmentRepository.findById(signupDto.getDepartmentId())
                .orElseThrow(() -> new ValidationException.InvalidDepartmentException());
        
        // 대학 이메일 도메인 검증
        String emailDomain = signupDto.getEmail().substring(signupDto.getEmail().indexOf(AppConstants.EMAIL_SEPARATOR) + 1);
        boolean isValidUniversityEmail = false;
        
        if (department.getSchool() != null && department.getSchool().getAllDomains() != null) {
            isValidUniversityEmail = department.getSchool().getAllDomains().contains(emailDomain);
        }
        
        if (!isValidUniversityEmail) {
            log.warn(Messages.LOG_INVALID_UNIVERSITY_EMAIL, 
                    emailDomain, department.getSchool().getSchoolName());
            // 개발 단계에서는 경고만 하고 진행 (나중에 예외 처리)
            // throw new ValidationException.InvalidUniversityEmailException();
        }
        
        // 사용자 생성
        User user = User.builder()
                .email(signupDto.getEmail())
                .password(passwordEncoder.encode(signupDto.getPassword()))
                .name(signupDto.getName())
                .phoneNumber(signupDto.getPhoneNumber())
                .department(department)
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .verified(false) // 이메일 인증 전까지는 false
                .build();
        
        User savedUser = userRepository.save(user);
        log.info(Messages.LOG_USER_CREATED, savedUser.getUserId());
        
        // TODO: Day 11 - ActivityLogService로 회원가입 성공 로그 기록
        // activityLogService.logUserActivity(savedUser.getEmail(), "SIGNUP", "SUCCESS");
        
        return UserResponseDto.from(savedUser);
    }
    
    public boolean isEmailAvailable(String email) {
        return !existsByEmail(email);
    }
}