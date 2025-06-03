package com.unibook.security;

import com.unibook.common.Messages;
import com.unibook.domain.entity.AdminAction;
import com.unibook.domain.entity.User;
import com.unibook.exception.UserSuspendedException;
import com.unibook.repository.AdminActionRepository;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final AdminActionRepository adminActionRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug(Messages.LOG_LOGIN_ATTEMPT, email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    Messages.USER_NOT_FOUND + email
                ));
        
        log.debug(Messages.LOG_USER_FOUND, 
                  user.getEmail(), user.getStatus(), user.isVerified());
        
        // 정지된 사용자 체크
        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            // 현재 활성 정지 정보 조회
            Optional<AdminAction> activeSuspension = adminActionRepository.findActiveSuspension(
                user.getUserId(), LocalDateTime.now());
            
            if (activeSuspension.isPresent()) {
                AdminAction suspension = activeSuspension.get();
                
                // 만료 시간이 설정되어 있고 현재 시간이 만료 시간을 지났다면 자동 해제
                if (suspension.getExpiresAt() != null && LocalDateTime.now().isAfter(suspension.getExpiresAt())) {
                    log.info("정지 기간이 만료되어 자동 해제합니다. userId: {}", user.getUserId());
                    
                    // 사용자 상태를 ACTIVE로 변경
                    user.setStatus(User.UserStatus.ACTIVE);
                    userRepository.save(user);
                    
                    // 자동 해제 기록 생성
                    AdminAction unsuspendAction = AdminAction.builder()
                        .adminId(1L) // 시스템 자동 해제
                        .targetType(AdminAction.TargetType.USER)
                        .targetId(user.getUserId())
                        .actionType(AdminAction.ActionType.UNSUSPEND)
                        .reason("자동 만료")
                        .build();
                    adminActionRepository.save(unsuspendAction);
                    
                    // 정상 로그인 진행
                } else {
                    // 여전히 정지 상태인 경우 예외 발생
                    throw new UserSuspendedException(
                        suspension.getReason(), 
                        suspension.getExpiresAt()
                    );
                }
            } else {
                // 정지 기록이 없는데 상태가 SUSPENDED인 경우 (데이터 불일치)
                log.warn("정지 상태이지만 정지 기록이 없습니다. userId: {}", user.getUserId());
                // 상태를 ACTIVE로 변경
                user.setStatus(User.UserStatus.ACTIVE);
                userRepository.save(user);
            }
        }
        
        // TODO: Day 11 - ActivityLogService로 로그인 시도 기록
        // activityLogService.logLoginAttempt(email, true, request.getRemoteAddr());
        
        return new UserPrincipal(user);
    }
}