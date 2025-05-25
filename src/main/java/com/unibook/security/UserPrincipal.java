package com.unibook.security;

import com.unibook.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class UserPrincipal implements UserDetails {
    
    private final Long userId;
    private final String email;
    private final String password;
    private final String name;
    private final User.UserRole role;
    private final User.UserStatus status;
    private final boolean verified;
    
    public UserPrincipal(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.name = user.getName();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.verified = user.isVerified();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return email; // 이메일을 사용자명으로 사용
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return status != User.UserStatus.SUSPENDED;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        // 개발 단계에서는 이메일 인증 전에도 로그인 가능하도록 설정
        // Day 4에서 이메일 인증 구현 후 && verified 조건 추가
        return status == User.UserStatus.ACTIVE;
    }
}