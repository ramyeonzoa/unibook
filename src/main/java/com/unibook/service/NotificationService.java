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
            User recipient = findUserForAsync(request.getRecipientUserId());
            User actor = findUserForAsync(request.getActorUserId());
            Post relatedPost = findPostForAsync(request.getRelatedPostId());

            Notification notification = buildNotification(request, recipient, actor, relatedPost);
            
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
        User recipient = getReferenceUser(request.getRecipientUserId());
        User actor = getReferenceUser(request.getActorUserId());
        Post relatedPost = getReferencePost(request.getRelatedPostId());

        Notification notification = buildNotification(request, recipient, actor, relatedPost);

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

        createNotificationSafely("찜 상태 변경", request, 
                "userId", recipientUserId, "postId", postId, "status", newStatus);
    }

    /**
     * 게시글 찜됨 익명 알림 생성 (비동기)
     * 누가 찜했는지는 알려주지 않음
     */
    @Async
    @Transactional
    public void createPostWishlistedNotificationAsync(Long postOwnerId, Long postId, String postTitle) {
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

        createNotificationSafely("게시글 찜 익명", request, 
                "postOwnerId", postOwnerId, "postId", postId);
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
     * 가격 변동 메시지 생성
     */
    private String generatePriceChangeMessage(Integer oldPrice, Integer newPrice) {
        if (oldPrice == null || newPrice == null) {
            return "찜한 게시글의 가격이 변경되었습니다.";
        }
        
        int priceDiff = newPrice - oldPrice;
        String formattedOldPrice = String.format("%,d원", oldPrice);
        String formattedNewPrice = String.format("%,d원", newPrice);
        
        if (priceDiff > 0) {
            String increase = String.format("%,d원", priceDiff);
            return String.format("찜한 게시글의 가격이 %s에서 %s로 %s 올랐어요.", 
                    formattedOldPrice, formattedNewPrice, increase);
        } else if (priceDiff < 0) {
            String decrease = String.format("%,d원", Math.abs(priceDiff));
            return String.format("찜한 게시글의 가격이 %s에서 %s로 %s 내렸어요! 🎉", 
                    formattedOldPrice, formattedNewPrice, decrease);
        } else {
            return "찜한 게시글의 가격 정보가 업데이트되었습니다.";
        }
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
        
        createNotificationSafely("신고 처리 결과", request, 
                "reporterId", reporterId, "reportId", reportId, "status", status);
    }
    
    /**
     * 찜한 게시글 가격 변동 알림 생성 (비동기)
     */
    @Async
    @Transactional
    public void createWishlistPriceChangeNotificationAsync(Long recipientUserId, Long postId, Integer oldPrice, Integer newPrice) {
        try {
            String title = "찜한 게시글 가격 변경 💰";
            String content = generatePriceChangeMessage(oldPrice, newPrice);
            String url = "/posts/" + postId;

            NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                    .recipientUserId(recipientUserId)
                    .type(Notification.NotificationType.WISHLIST_PRICE_CHANGED)
                    .relatedPostId(postId)
                    .title(title)
                    .content(content)
                    .url(url)
                    .build();

            // createNotification을 직접 호출하는 대신 커스텀 로직으로 payload 포함 알림 생성
            createPriceChangeNotification(request, oldPrice, newPrice);
            log.info("찜한 게시글 가격 변동 알림 생성: userId={}, postId={}, {}원 -> {}원", 
                    recipientUserId, postId, oldPrice, newPrice);
        } catch (Exception e) {
            log.error("찜한 게시글 가격 변동 알림 생성 실패: userId={}, postId={}", recipientUserId, postId, e);
        }
    }
    
    /**
     * 가격 변동 알림 생성 (payload 포함)
     */
    private NotificationDto.Response createPriceChangeNotification(NotificationDto.CreateRequest request, Integer oldPrice, Integer newPrice) {
        // getReferenceById 사용으로 불필요한 DB 조회 최소화
        User recipient = getReferenceUser(request.getRecipientUserId());
        User actor = getReferenceUser(request.getActorUserId());
        Post relatedPost = getReferencePost(request.getRelatedPostId());

        Notification notification = buildNotification(request, recipient, actor, relatedPost);
        
        // 가격 정보를 payload에 추가
        notification.addPayload("oldPrice", oldPrice);
        notification.addPayload("newPrice", newPrice);
        notification.addPayload("priceChange", newPrice - oldPrice);

        Notification saved = notificationRepository.save(notification);
        log.info("가격 변동 알림 생성됨: userId={}, type={}, title={}", 
                request.getRecipientUserId(), request.getType(), request.getTitle());
        
        NotificationDto.Response response = NotificationDto.Response.from(saved);
        
        // 실시간 알림 전송
        emitterService.sendNotificationToUser(request.getRecipientUserId(), response);
        
        return response;
    }
    
    /**
     * 안전한 비동기 알림 생성 템플릿 (예외 처리 포함)
     */
    private void createNotificationSafely(String operationName, NotificationDto.CreateRequest request, Object... logParams) {
        try {
            createNotification(request);
            log.info("{} 알림 생성: {}", operationName, formatLogMessage(logParams));
        } catch (Exception e) {
            log.error("{} 알림 생성 실패: {}", operationName, formatLogMessage(logParams), e);
        }
    }
    
    /**
     * 로그 메시지 포맷팅
     */
    private String formatLogMessage(Object... params) {
        if (params == null || params.length == 0) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                sb.append(params[i]).append("=").append(params[i + 1]);
                if (i + 2 < params.length) sb.append(", ");
            }
        }
        return sb.toString();
    }
    
    /**
     * Notification 엔티티 빌드
     */
    private Notification buildNotification(NotificationDto.CreateRequest request, User recipient, User actor, Post relatedPost) {
        return Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(request.getType())
                .relatedPost(relatedPost)
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .build();
    }
    
    /**
     * 알림용 User 조회 (비동기 컨텍스트용 - findById 사용)
     */
    private User findUserForAsync(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }
    
    /**
     * 알림용 User 조회 (동기 컨텍스트용 - getReferenceById 사용)
     */
    private User getReferenceUser(Long userId) {
        if (userId == null) return null;
        return userRepository.getReferenceById(userId);
    }
    
    /**
     * 알림용 Post 조회 (비동기 컨텍스트용 - findById 사용)
     */
    private Post findPostForAsync(Long postId) {
        if (postId == null) return null;
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("관련 게시글을 찾을 수 없습니다."));
    }
    
    /**
     * 알림용 Post 조회 (동기 컨텍스트용 - getReferenceById 사용)
     */
    private Post getReferencePost(Long postId) {
        if (postId == null) return null;
        return postRepository.getReferenceById(postId);
    }
    
    /**
     * 키워드 매칭 알림 (비동기)
     */
    @Async
    @Transactional
    public void createKeywordMatchNotificationAsync(Long userId, Long postId, String postTitle, String keyword) {
        String title = "등록한 키워드와 일치하는 게시글이 올라왔어요! 🔔";
        String content = String.format("'%s' 키워드와 일치하는 '%s' 게시글이 등록되었습니다.", keyword, postTitle);
        String url = "/posts/" + postId;
        
        NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                .recipientUserId(userId)
                .actorUserId(null)  // 시스템 알림이므로 actor 없음
                .type(Notification.NotificationType.KEYWORD_MATCH)
                .relatedPostId(postId)
                .title(title)
                .content(content)
                .url(url)
                .build();
        
        createNotificationSafely("키워드 매칭", request, 
                "userId", userId, "postId", postId, "keyword", keyword);
    }
}