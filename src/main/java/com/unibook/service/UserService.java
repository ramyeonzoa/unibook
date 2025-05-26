package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.common.Messages;
import com.unibook.domain.dto.SignupRequestDto;
import com.unibook.domain.dto.UserResponseDto;
import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.EmailVerificationToken;
import com.unibook.domain.entity.User;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.DepartmentRepository;
import com.unibook.repository.EmailVerificationTokenRepository;
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
    private final EmailVerificationTokenRepository tokenRepository;
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
            // 도메인 정보가 있는 학교는 엄격하게 검증
            if (department.getSchool().getAllDomains() != null && !department.getSchool().getAllDomains().isEmpty()) {
                log.error(Messages.LOG_INVALID_UNIVERSITY_EMAIL, 
                        emailDomain, department.getSchool().getSchoolName());
                String validDomains = String.join(", @", department.getSchool().getAllDomains());
                throw new ValidationException(
                    String.format("%s의 이메일 도메인(@%s)으로 가입해주세요.", 
                        department.getSchool().getSchoolName(), validDomains)
                );
            } else {
                // 도메인 정보가 없는 학교는 일단 허용 (추후 관리자가 도메인 추가)
                log.warn("School {} has no domain information. Allowing any email domain.", 
                        department.getSchool().getSchoolName());
            }
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
        
        // 이메일 발송은 Controller에서 처리 (순환 참조 방지)
        
        // TODO: Day 11 - ActivityLogService로 회원가입 성공 로그 기록
        // activityLogService.logUserActivity(savedUser.getEmail(), "SIGNUP", "SUCCESS");
        
        return UserResponseDto.from(savedUser);
    }
    
    public boolean isEmailAvailable(String email) {
        return !existsByEmail(email);
    }
    
    /**
     * 이메일 인증 처리
     */
    @Transactional
    public User verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 인증 토큰입니다."));
        
        if (!verificationToken.isValid()) {
            throw new ValidationException("만료되었거나 이미 사용된 토큰입니다.");
        }
        
        if (verificationToken.getTokenType() != EmailVerificationToken.TokenType.EMAIL_VERIFICATION) {
            throw new ValidationException("이메일 인증용 토큰이 아닙니다.");
        }
        
        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);
        
        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);
        
        log.info("Email verified successfully for user: {}", user.getEmail());
        return user;
    }
    
    /**
     * 이메일 재발송을 위한 사용자 검증
     */
    public User validateUserForEmailResend(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("등록되지 않은 이메일입니다."));
        
        if (user.isVerified()) {
            throw new ValidationException("이미 인증된 이메일입니다.");
        }
        
        return user;
    }
    
    /**
     * 비밀번호 재설정을 위한 사용자 조회
     */
    public User getUserForPasswordReset(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("등록되지 않은 이메일입니다."));
    }
    
    /**
     * 비밀번호 재설정 토큰 검증
     */
    public void validatePasswordResetToken(String token) {
        EmailVerificationToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 토큰입니다."));
        
        if (!resetToken.isValid()) {
            throw new ValidationException("만료되었거나 이미 사용된 토큰입니다.");
        }
        
        if (resetToken.getTokenType() != EmailVerificationToken.TokenType.PASSWORD_RESET) {
            throw new ValidationException("비밀번호 재설정용 토큰이 아닙니다.");
        }
    }
    
    /**
     * 비밀번호 재설정
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        EmailVerificationToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 토큰입니다."));
        
        validatePasswordResetToken(token);
        
        User user = resetToken.getUser();
        
        // 새 비밀번호가 이전 비밀번호와 같은지 확인
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("Password reset attempt with same password for user: {}", user.getEmail());
            throw new ValidationException(Messages.PASSWORD_SAME_AS_PREVIOUS);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);
        
        log.info("Password reset successfully for user: {}", user.getEmail());
    }
}