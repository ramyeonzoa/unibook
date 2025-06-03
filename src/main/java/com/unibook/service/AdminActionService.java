package com.unibook.service;

import com.unibook.domain.entity.AdminAction;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.repository.AdminActionRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminActionService {
    
    private final AdminActionRepository adminActionRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    
    /**
     * 사용자 정지
     */
    @Transactional
    public void suspendUser(Long userId, String reason, LocalDateTime expiresAt, Long adminId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 이미 정지된 사용자인지 확인
        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new IllegalStateException("이미 정지된 사용자입니다.");
        }
        
        // 1. User 상태 변경
        user.setStatus(User.UserStatus.SUSPENDED);
        userRepository.save(user);
        
        // 2. 조치 기록 생성
        AdminAction action = AdminAction.builder()
            .adminId(adminId)
            .targetType(AdminAction.TargetType.USER)
            .targetId(userId)
            .actionType(AdminAction.ActionType.SUSPEND)
            .reason(reason)
            .expiresAt(expiresAt)
            .build();
        
        adminActionRepository.save(action);
        
        log.info("사용자 정지 처리 완료 - userId: {}, adminId: {}, expiresAt: {}", userId, adminId, expiresAt);
    }
    
    /**
     * 정지 해제
     */
    @Transactional
    public void unsuspendUser(Long userId, String reason, Long adminId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 정지된 사용자인지 확인
        if (user.getStatus() != User.UserStatus.SUSPENDED) {
            throw new IllegalStateException("정지되지 않은 사용자입니다.");
        }
        
        // 1. User 상태 변경
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        
        // 2. 해제 조치 기록
        AdminAction action = AdminAction.builder()
            .adminId(adminId)
            .targetType(AdminAction.TargetType.USER)
            .targetId(userId)
            .actionType(AdminAction.ActionType.UNSUSPEND)
            .reason(reason)
            .build();
        
        adminActionRepository.save(action);
        
        log.info("사용자 정지 해제 완료 - userId: {}, adminId: {}", userId, adminId);
    }
    
    /**
     * 게시글 차단 (기존 신고 시스템과 통합)
     */
    @Transactional
    public void blockPost(Long postId, String reason, Long adminId, Long relatedReportId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
        
        // 이미 차단된 게시글인지 확인
        if (post.getStatus() == Post.PostStatus.BLOCKED) {
            throw new IllegalStateException("이미 차단된 게시글입니다.");
        }
        
        // 1. Post 상태 변경
        post.setStatus(Post.PostStatus.BLOCKED);
        postRepository.save(post);
        
        // 2. 조치 기록 생성
        AdminAction action = AdminAction.builder()
            .adminId(adminId)
            .targetType(AdminAction.TargetType.POST)
            .targetId(postId)
            .actionType(AdminAction.ActionType.BLOCK)
            .reason(reason)
            .relatedReportId(relatedReportId)
            .build();
        
        adminActionRepository.save(action);
        
        log.info("게시글 차단 처리 완료 - postId: {}, adminId: {}, reportId: {}", postId, adminId, relatedReportId);
    }
    
    /**
     * 게시글 차단 해제
     */
    @Transactional
    public void unblockPost(Long postId, String reason, Long adminId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
        
        // 차단된 게시글인지 확인
        if (post.getStatus() != Post.PostStatus.BLOCKED) {
            throw new IllegalStateException("차단되지 않은 게시글입니다.");
        }
        
        // 1. Post 상태 변경
        post.setStatus(Post.PostStatus.AVAILABLE);
        postRepository.save(post);
        
        // 2. 해제 조치 기록
        AdminAction action = AdminAction.builder()
            .adminId(adminId)
            .targetType(AdminAction.TargetType.POST)
            .targetId(postId)
            .actionType(AdminAction.ActionType.UNBLOCK)
            .reason(reason)
            .build();
        
        adminActionRepository.save(action);
        
        log.info("게시글 차단 해제 완료 - postId: {}, adminId: {}", postId, adminId);
    }
    
    /**
     * 만료된 조치 자동 처리
     */
    @Scheduled(fixedRate = 300000) // 5분마다
    @Transactional
    public void processExpiredActions() {
        List<AdminAction> expiredActions = adminActionRepository.findExpiredActions(LocalDateTime.now());
        
        if (expiredActions.isEmpty()) {
            return;
        }
        
        log.info("만료된 조치 처리 시작 - 대상: {}개", expiredActions.size());
        
        for (AdminAction action : expiredActions) {
            try {
                if (action.getActionType() == AdminAction.ActionType.SUSPEND) {
                    // 정지 자동 해제
                    unsuspendUser(action.getTargetId(), "자동 만료", 1L); // 시스템 관리자 ID
                } else if (action.getActionType() == AdminAction.ActionType.BLOCK) {
                    // 게시글 차단 자동 해제 (필요한 경우)
                    // unblockPost(action.getTargetId(), "자동 만료", 1L);
                }
            } catch (Exception e) {
                log.error("만료된 조치 처리 중 오류 발생 - actionId: {}, error: {}", 
                         action.getActionId(), e.getMessage());
            }
        }
        
        log.info("만료된 조치 처리 완료");
    }
    
    /**
     * 사용자 정지 상태 확인
     */
    public boolean isUserSuspended(Long userId) {
        return adminActionRepository.findActiveSuspension(userId, LocalDateTime.now()).isPresent();
    }
    
    /**
     * 사용자의 현재 활성 정지 정보 조회
     */
    public Optional<AdminAction> getActiveSuspension(Long userId) {
        return adminActionRepository.findActiveSuspension(userId, LocalDateTime.now());
    }
    
    /**
     * 사용자 조치 이력 조회
     */
    public List<AdminAction> getUserActionHistory(Long userId) {
        return adminActionRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            AdminAction.TargetType.USER, userId);
    }
    
    /**
     * 게시글 조치 이력 조회
     */
    public List<AdminAction> getPostActionHistory(Long postId) {
        return adminActionRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            AdminAction.TargetType.POST, postId);
    }
    
    /**
     * 관리자 조치 이력 조회
     */
    public List<AdminAction> getAdminActionHistory(Long adminId) {
        return adminActionRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }
    
    /**
     * 신고 관련 조치 이력 조회
     */
    public List<AdminAction> getReportActionHistory(Long reportId) {
        return adminActionRepository.findByRelatedReportIdOrderByCreatedAtDesc(reportId);
    }
    
    /**
     * 최근 조치 이력 조회 (대시보드용)
     */
    public List<AdminAction> getRecentActions(int limit) {
        List<AdminAction> actions = adminActionRepository.findAll();
        return actions.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .toList();
    }
}