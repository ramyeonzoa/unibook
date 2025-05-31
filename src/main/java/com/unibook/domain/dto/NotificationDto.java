package com.unibook.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unibook.domain.entity.Notification;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 관련 DTO 클래스
 */
public class NotificationDto {

    /**
     * 알림 응답 DTO
     */
    @Builder
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long notificationId;
        private String type;
        private String title;
        private String content;
        private String url;
        @JsonProperty("isRead")
        private boolean isRead;
        private String actorName;
        private Long relatedPostId;
        private LocalDateTime createdAt;

        /**
         * Entity를 Response DTO로 변환
         */
        public static Response from(Notification notification) {
            return Response.builder()
                    .notificationId(notification.getNotificationId())
                    .type(notification.getType().name())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .url(notification.getUrl())
                    .isRead(notification.isRead())
                    .actorName(notification.getActor() != null ? notification.getActor().getName() : null)
                    .relatedPostId(notification.getRelatedPost() != null ? notification.getRelatedPost().getPostId() : null)
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }

    /**
     * 알림 생성 요청 DTO
     */
    @Builder
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        private Long recipientUserId;
        private Long actorUserId;
        private Notification.NotificationType type;
        private Long relatedPostId;
        private String title;
        private String content;
        private String url;
    }

    /**
     * 알림 카운트 응답 DTO
     */
    @Builder
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class CountResponse {
        private long totalCount;
        private long unreadCount;
    }
}