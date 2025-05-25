package com.unibook.security;

import com.unibook.common.Messages;
import com.unibook.domain.entity.User;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug(Messages.LOG_LOGIN_ATTEMPT, email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    Messages.USER_NOT_FOUND + email
                ));
        
        log.debug(Messages.LOG_USER_FOUND, 
                  user.getEmail(), user.getStatus(), user.isVerified());
        
        // TODO: Day 11 - ActivityLogService로 로그인 시도 기록
        // activityLogService.logLoginAttempt(email, true, request.getRemoteAddr());
        
        return new UserPrincipal(user);
    }
}