package com.unibook.service;

import com.unibook.domain.entity.Post;
import com.unibook.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * 권한 체크 공통 서비스
 * 기존 Controller의 권한 체크 로직을 중앙화하여 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    /**
     * 게시글 소유자인지 확인
     * 기존 코드: post.getUser().getUserId().equals(userPrincipal.getUserId())
     */
    public boolean isOwner(Post post, UserPrincipal userPrincipal) {
        if (post == null || userPrincipal == null) {
            return false;
        }
        
        if (post.getUser() == null || post.getUser().getUserId() == null) {
            return false;
        }
        
        return post.getUser().getUserId().equals(userPrincipal.getUserId());
    }
    
    /**
     * 관리자 권한인지 확인
     * 기존 코드: userPrincipal.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))
     */
    public boolean isAdmin(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return false;
        }
        
        return userPrincipal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
    
    /**
     * 소유자이거나 관리자인지 확인
     * 기존 코드: isOwner || isAdmin
     */
    public boolean isOwnerOrAdmin(Post post, UserPrincipal userPrincipal) {
        return isOwner(post, userPrincipal) || isAdmin(userPrincipal);
    }
    
    /**
     * 게시글 수정 가능 여부 확인 (복잡한 로직)
     * 기존 코드: (isOwner && post.getStatus() != Post.PostStatus.BLOCKED) || isAdmin
     */
    public boolean canEdit(Post post, UserPrincipal userPrincipal) {
        if (post == null || userPrincipal == null) {
            return false;
        }
        
        boolean owner = isOwner(post, userPrincipal);
        boolean admin = isAdmin(userPrincipal);
        
        // 관리자는 모든 게시글을 수정 가능
        if (admin) {
            return true;
        }
        
        // 소유자는 BLOCKED 상태가 아닌 경우에만 수정 가능
        if (owner && post.getStatus() != Post.PostStatus.BLOCKED) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 소유자 또는 관리자 권한 검증 (권한 없으면 예외 발생)
     * 기존 코드의 if (!isOwner && !isAdmin) { throw new AccessDeniedException(...); } 패턴
     */
    public void requireOwnerOrAdmin(Post post, UserPrincipal userPrincipal, String errorMessage) {
        if (!isOwnerOrAdmin(post, userPrincipal)) {
            throw new AccessDeniedException(errorMessage);
        }
    }
    
    /**
     * 소유자 권한 검증 (권한 없으면 예외 발생)
     * 기존 코드의 if (!isOwner) { throw new AccessDeniedException(...); } 패턴
     */
    public void requireOwner(Post post, UserPrincipal userPrincipal, String errorMessage) {
        if (!isOwner(post, userPrincipal)) {
            throw new AccessDeniedException(errorMessage);
        }
    }
    
    /**
     * 게시글 수정 권한 검증 (BLOCKED 상태 고려, 권한 없으면 예외 발생)
     * BLOCKED 게시글은 관리자만 수정 가능
     */
    public void requireCanEdit(Post post, UserPrincipal userPrincipal, String errorMessage) {
        if (!canEdit(post, userPrincipal)) {
            throw new AccessDeniedException(errorMessage);
        }
    }
    
    /**
     * detail 페이지용 권한 정보 계산 (null 안전)
     * 기존 코드의 복잡한 null 체크와 권한 계산 로직
     */
    public AuthorizationInfo calculateDetailPageAuth(Post post, UserPrincipal userPrincipal) {
        boolean isOwner = false;
        boolean canEdit = false;
        boolean isAdmin = false;
        
        if (userPrincipal != null) {
            isOwner = isOwner(post, userPrincipal);
            isAdmin = isAdmin(userPrincipal);
            canEdit = canEdit(post, userPrincipal);
        }
        
        return AuthorizationInfo.builder()
                .isOwner(isOwner)
                .isAdmin(isAdmin)
                .canEdit(canEdit)
                .build();
    }
    
    /**
     * 권한 정보를 담는 DTO 클래스
     */
    public static class AuthorizationInfo {
        private final boolean isOwner;
        private final boolean isAdmin;
        private final boolean canEdit;
        
        private AuthorizationInfo(boolean isOwner, boolean isAdmin, boolean canEdit) {
            this.isOwner = isOwner;
            this.isAdmin = isAdmin;
            this.canEdit = canEdit;
        }
        
        public static AuthorizationInfoBuilder builder() {
            return new AuthorizationInfoBuilder();
        }
        
        public boolean isOwner() { return isOwner; }
        public boolean isAdmin() { return isAdmin; }
        public boolean canEdit() { return canEdit; }
        
        public static class AuthorizationInfoBuilder {
            private boolean isOwner;
            private boolean isAdmin;
            private boolean canEdit;
            
            public AuthorizationInfoBuilder isOwner(boolean isOwner) {
                this.isOwner = isOwner;
                return this;
            }
            
            public AuthorizationInfoBuilder isAdmin(boolean isAdmin) {
                this.isAdmin = isAdmin;
                return this;
            }
            
            public AuthorizationInfoBuilder canEdit(boolean canEdit) {
                this.canEdit = canEdit;
                return this;
            }
            
            public AuthorizationInfo build() {
                return new AuthorizationInfo(isOwner, isAdmin, canEdit);
            }
        }
    }
}