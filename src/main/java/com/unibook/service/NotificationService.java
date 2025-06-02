package com.unibook.service;

import com.unibook.domain.dto.NotificationDto;
import com.unibook.domain.entity.Notification;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.domain.entity.Report;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.repository.NotificationRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationEmitterService emitterService;

    /**
     * 알림 생성 (비동기 처리)
     */
    @Async
    @Transactional
    public void createNotificationAsync(NotificationDto.CreateRequest request) {
        try {
            // 비동기 컨텍스트에서는 findById 사용 (getReferenceById는 LazyInitializationException 위험)
            User recipient = userRepository.findById(request.getRecipientUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("받는 사용자를 찾을 수 없습니다."));

            User actor = null;
            if (request.getActorUserId() != null) {
                actor = userRepository.findById(request.getActorUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("행동 사용자를 찾을 수 없습니다."));
            }

            Post relatedPost = null;
            if (request.getRelatedPostId() != null) {
                relatedPost = postRepository.findById(request.getRelatedPostId())
                        .orElseThrow(() -> new ResourceNotFoundException("관련 게시글을 찾을 수 없습니다."));
            }

            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(request.getType())
                    .relatedPost(relatedPost)
                    .title(request.getTitle())
                    .content(request.getContent())
                    .url(request.getUrl())
                    .build();

            Notification saved = notificationRepository.save(notification);
            log.info("비동기 알림 생성됨: userId={}, type={}, title={}", 
                    request.getRecipientUserId(), request.getType(), request.getTitle());
            
            // 실시간 알림 전송
            NotificationDto.Response response = NotificationDto.Response.from(saved);
            emitterService.sendNotificationToUser(request.getRecipientUserId(), response);
            
        } catch (Exception e) {
            log.error("비동기 알림 생성 실패: userId={}, type={}", 
                    request.getRecipientUserId(), request.getType(), e);
        }
    }

    /**
     * 알림 생성 (동기 처리 - 내부용)
     */
    @Transactional
    public NotificationDto.Response createNotification(NotificationDto.CreateRequest request) {
        // getReferenceById 사용으로 불필요한 DB 조회 최소화
        User recipient = userRepository.getReferenceById(request.getRecipientUserId());

        User actor = null;
        if (request.getActorUserId() != null) {
            actor = userRepository.getReferenceById(request.getActorUserId());
        }

        Post relatedPost = null;
        if (request.getRelatedPostId() != null) {
            relatedPost = postRepository.getReferenceById(request.getRelatedPostId());
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(request.getType())
                .relatedPost(relatedPost)
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("알림 생성됨: userId={}, type={}, title={}", 
                request.getRecipientUserId(), request.getType(), request.getTitle());
        
        NotificationDto.Response response = NotificationDto.Response.from(saved);
        
        // 실시간 알림 전송
        emitterService.sendNotificationToUser(request.getRecipientUserId(), response);
        
        return response;
    }

    /**
     * 찜한 게시글 상태 변경 알림 생성 (비동기)
     */
    @Async
    @Transactional
    public void createWishlistStatusNotificationAsync(Long recipientUserId, Long postId, Post.PostStatus newStatus) {
        try {
            String title = "찜한 게시글 상태 변경";
            String content = generateStatusChangeMessage(newStatus);
            String url = "/posts/" + postId;

            NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                    .recipientUserId(recipientUserId)
                    .type(Notification.NotificationType.WISHLIST_STATUS_CHANGED)
                    .relatedPostId(postId)
                    .title(title)
                    .content(content)
                    .url(url)
                    .build();

            createNotification(request);
        } catch (Exception e) {
            log.error("찜 상태 변경 알림 생성 실패: userId={}, postId={}", recipientUserId, postId, e);
        }
    }

    /**
     * 게시글 찜됨 익명 알림 생성 (비동기)
     * 누가 찜했는지는 알려주지 않음
     */
    @Async
    @Transactional
    public void createPostWishlistedNotificationAsync(Long postOwnerId, Long postId, String postTitle) {
        try {
            String title = "회원님의 게시글이 찜되었습니다! 💝";
            String content = String.format("'%s' 게시글을 누군가 찜했습니다.", postTitle);
            String url = "/posts/" + postId;

            NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                    .recipientUserId(postOwnerId)
                    .actorUserId(null)  // 익명이므로 actor 없음
                    .type(Notification.NotificationType.POST_WISHLISTED)
                    .relatedPostId(postId)
                    .title(title)
                    .content(content)
                    .url(url)
                    .build();

            createNotification(request);
            log.info("게시글 찜 익명 알림 생성: postOwnerId={}, postId={}", postOwnerId, postId);
        } catch (Exception e) {
            log.error("게시글 찜 익명 알림 생성 실패: postOwnerId={}, postId={}", postOwnerId, postId, e);
        }
    }

    /**
     * 사용자별 알림 목록 조회
     */
    public Page<NotificationDto.Response> getNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByRecipientUserIdWithDetailsExcludingNewMessage(userId, pageable);
        return notifications.map(NotificationDto.Response::from);
    }

    /**
     * 사용자별 읽지 않은 알림 목록 조회 (헤더 드롭다운용)
     */
    public Page<NotificationDto.Response> getUnreadNotifications(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Notification> notifications = notificationRepository.findUnreadByRecipientUserIdWithDetails(userId, pageable);
        return notifications.map(NotificationDto.Response::from);
    }

    /**
     * 알림 카운트 조회
     */
    public NotificationDto.CountResponse getNotificationCount(Long userId) {
        long totalCount = notificationRepository.countByRecipientUserIdExcludingNewMessage(userId);
        long unreadCount = notificationRepository.countUnreadByRecipientUserIdExcludingNewMessage(userId);
        
        return NotificationDto.CountResponse.builder()
                .totalCount(totalCount)
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * 특정 알림을 읽음으로 표시
     */
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        int updatedCount = notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
        return updatedCount > 0;
    }

    /**
     * 모든 알림을 읽음으로 표시
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    /**
     * 특정 게시글 관련 알림 삭제 (게시글 삭제 시 호출)
     */
    @Transactional
    public void deleteNotificationsByPostId(Long postId) {
        int deletedCount = notificationRepository.deleteByRelatedPostId(postId);
        log.info("게시글 삭제로 인한 알림 삭제: postId={}, count={}", postId, deletedCount);
    }

    /**
     * 상태 변경 메시지 생성
     */
    private String generateStatusChangeMessage(Post.PostStatus newStatus) {
        if (newStatus == null) {
            return "찜한 게시글의 상태가 변경되었습니다.";
        }
        
        return switch (newStatus) {
            case AVAILABLE -> "찜한 게시글이 다시 판매중으로 변경되었습니다.";
            case RESERVED -> "찜한 게시글이 예약중으로 변경되었습니다.";
            case COMPLETED -> "찜한 게시글이 거래완료되었습니다.";
            case BLOCKED -> "찜한 게시글이 관리자에 의해 차단되었습니다.";
        };
    }
    
    /**
     * 관리자들에게 새 신고 알림 (비동기)
     */
    @Async
    @Transactional
    public void notifyAdminsOfNewReport(Report report, String targetTitle) {
        try {
            // TODO: 관리자 권한을 가진 사용자들 조회 로직 추가 필요
            // 임시로 로그만 출력
            log.info("새로운 신고 접수 - 관리자 알림 필요: reportId={}, target={}", 
                    report.getReportId(), targetTitle);
            
            // 실제 구현 시:
            // List<User> admins = userRepository.findByRole("ROLE_ADMIN");
            // for (User admin : admins) {
            //     NotificationDto.CreateRequest request = ...
            //     createNotification(request);
            // }
        } catch (Exception e) {
            log.error("관리자 신고 알림 발송 실패", e);
        }
    }
    
    /**
     * 신고 처리 결과 알림 (비동기)
     */
    @Async
    @Transactional
    public void notifyReportProcessed(Long reporterId, Long reportId, Report.ReportStatus status) {
        try {
            String title = "신고 처리 결과";
            String content = switch (status) {
                case COMPLETED -> "신고하신 내용이 처리되었습니다. 감사합니다.";
                case REJECTED -> "신고하신 내용을 검토한 결과, 규정 위반 사항이 확인되지 않았습니다.";
                default -> "신고하신 내용이 처리 중입니다.";
            };
            
            NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                    .recipientUserId(reporterId)
                    .type(Notification.NotificationType.REPORT_PROCESSED)
                    .title(title)
                    .content(content)
                    .url("/reports/my") // 내 신고 내역 페이지로
                    .build();
            
            createNotification(request);
        } catch (Exception e) {
            log.error("신고 처리 결과 알림 생성 실패: reporterId={}, reportId={}", reporterId, reportId, e);
        }
    }
}