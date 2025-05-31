/**
 * 알림 시스템 JavaScript
 */
$(document).ready(function() {
    // CSRF 토큰 설정
    const token = $('meta[name="_csrf"]').attr('content');
    const header = $('meta[name="_csrf_header"]').attr('content');
    
    $.ajaxSetup({
        beforeSend: function(xhr) {
            if (header && token) {
                xhr.setRequestHeader(header, token);
            }
        }
    });
    
    // 로그인한 사용자만 알림 시스템 초기화
    if ($('#notificationDropdownMobile').length || $('#notificationDropdownDesktop').length) {
        initNotificationSystem();
    }
});

/**
 * 알림 시스템 초기화
 */
function initNotificationSystem() {
    // SSE 연결 생성
    connectSSE();
    
    // 초기 알림 카운트 로드
    loadNotificationCount();
    
    // 드롭다운 열릴 때 알림 목록 로드 (모바일 + 데스크톱)
    $('#notificationDropdownMobile, #notificationDropdownDesktop').on('show.bs.dropdown', function() {
        loadUnreadNotifications();
    });
    
    // 모두 읽음 버튼 클릭 이벤트 (클래스 기반)
    $('.mark-all-read-btn').on('click', function(e) {
        e.preventDefault();
        markAllNotificationsAsRead();
    });
}

/**
 * SSE 연결 생성
 */
function connectSSE() {
    const eventSource = new EventSource('/api/notifications/stream');
    
    eventSource.addEventListener('connect', function(event) {
        // SSE 연결 성공
    });
    
    eventSource.addEventListener('notification', function(event) {
        const notification = JSON.parse(event.data);
        
        // 알림 카운트 증가
        incrementNotificationCount();
        
        // 토스트 알림 표시
        showNotificationToast(notification);
        
        // 드롭다운이 열려있으면 목록 새로고침
        if ($('#notificationDropdown').hasClass('show')) {
            loadUnreadNotifications();
        }
        
        // 커스텀 이벤트 발생 (마이페이지 실시간 업데이트용)
        $(document).trigger('notification:new', [notification]);
    });
    
    eventSource.addEventListener('count-update', function(event) {
        const countData = JSON.parse(event.data);
        updateNotificationBadge(countData.unreadCount);
    });
    
    eventSource.onerror = function(error) {
        eventSource.close();
        
        // 5초 후 재연결 시도
        setTimeout(connectSSE, 5000);
    };
}

/**
 * 알림 카운트 로드
 */
function loadNotificationCount() {
    $.get('/api/notifications/count')
        .done(function(response) {
            if (response.success) {
                updateNotificationBadge(response.data.unreadCount);
            }
        })
        .fail(function(xhr) {
            console.error('알림 카운트 로드 실패:', xhr);
        });
}

/**
 * 읽지 않은 알림 목록 로드
 */
function loadUnreadNotifications() {
    $.get('/api/notifications/unread?limit=10')
        .done(function(response) {
            if (response.success) {
                displayNotifications(response.data.content);
            }
        })
        .fail(function(xhr) {
            console.error('알림 목록 로드 실패:', xhr);
            $('.notification-list').html(
                '<li class="text-center py-3">' +
                '<span class="text-danger">알림을 불러올 수 없습니다.</span>' +
                '</li>'
            );
        });
}

/**
 * 알림 목록 표시
 */
function displayNotifications(notifications) {
    const $lists = $('.notification-list');
    
    if (notifications.length === 0) {
        $lists.html(
            '<li class="empty-state">' +
            '<i class="bi bi-bell-slash"></i>' +
            '<p>새로운 알림이 없습니다</p>' +
            '</li>'
        );
        return;
    }
    
    let html = '';
    notifications.forEach(function(notification) {
        html += createNotificationItem(notification);
    });
    
    $lists.html(html);
    
    // 알림 아이템 클릭 이벤트
    $('.notification-item').on('click', function() {
        const notificationId = $(this).data('notification-id');
        const url = $(this).data('url');
        
        markAsRead(notificationId, function() {
            if (url) {
                window.location.href = url;
            }
        });
    });
}

/**
 * 알림 아이템 HTML 생성
 */
function createNotificationItem(notification) {
    const iconClass = getNotificationIcon(notification.type);
    const timeAgo = formatTimeAgo(notification.createdAt);
    const unreadClass = notification.isRead ? '' : 'unread';
    
    return `
        <li class="notification-item ${unreadClass} px-3 py-2" 
            data-notification-id="${notification.notificationId}" 
            data-url="${notification.url || '#'}">
            <div class="d-flex gap-3">
                <div class="notification-icon ${iconClass.bg}">
                    <i class="${iconClass.icon}"></i>
                </div>
                <div class="notification-content">
                    <div class="notification-title">${notification.title}</div>
                    <div class="notification-text">${notification.content}</div>
                    <div class="notification-time">${timeAgo}</div>
                </div>
            </div>
        </li>
    `;
}

/**
 * 알림 타입별 아이콘 정보
 */
function getNotificationIcon(type) {
    switch(type) {
        case 'WISHLIST_STATUS_CHANGED':
            return { icon: 'bi bi-heart-fill', bg: 'wishlist' };
        case 'POST_WISHLISTED':
            return { icon: 'bi bi-star-fill', bg: 'post' };
        case 'NEW_MESSAGE':
            return { icon: 'bi bi-chat-dots-fill', bg: 'message' };
        default:
            return { icon: 'bi bi-bell-fill', bg: 'wishlist' };
    }
}

/**
 * 시간 포맷팅 (몇 분 전, 몇 시간 전 등)
 */
function formatTimeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);
    
    if (seconds < 60) return '방금 전';
    if (seconds < 3600) return Math.floor(seconds / 60) + '분 전';
    if (seconds < 86400) return Math.floor(seconds / 3600) + '시간 전';
    if (seconds < 604800) return Math.floor(seconds / 86400) + '일 전';
    
    return date.toLocaleDateString();
}

/**
 * 알림을 읽음으로 표시
 */
function markAsRead(notificationId, callback) {
    $.post(`/api/notifications/${notificationId}/read`)
        .done(function(response) {
            if (response.success) {
                $(`.notification-item[data-notification-id="${notificationId}"]`).removeClass('unread');
                if (callback) callback();
            }
        })
        .fail(function(xhr) {
            console.error('알림 읽음 처리 실패:', xhr);
        });
}

/**
 * 모든 알림을 읽음으로 표시
 */
function markAllNotificationsAsRead() {
    $.post('/api/notifications/read-all')
        .done(function(response) {
            if (response.success) {
                $('.notification-item').removeClass('unread');
                updateNotificationBadge(0);
                
                // 성공 메시지 표시
                showToast('success', response.message);
            }
        })
        .fail(function(xhr) {
            console.error('모든 알림 읽음 처리 실패:', xhr);
            showToast('error', '알림 처리 중 오류가 발생했습니다.');
        });
}

/**
 * 알림 배지 업데이트 (모바일 + 데스크톱 모두)
 */
function updateNotificationBadge(count) {
    const $badges = $('.notification-badge'); // 클래스 기반
    const $counts = $('.notification-count'); // 클래스 기반
    
    if (count > 0) {
        $counts.text(count > 99 ? '99+' : count);
        $badges.show();
        
        // 새 알림이 있으면 pulse 애니메이션
        if (!$badges.hasClass('pulse')) {
            $badges.addClass('pulse');
        }
    } else {
        $badges.hide();
        $badges.removeClass('pulse');
    }
}

/**
 * 알림 카운트 증가
 */
function incrementNotificationCount() {
    const currentCount = parseInt($('.notification-count').first().text()) || 0;
    updateNotificationBadge(currentCount + 1);
}

/**
 * 토스트 알림 표시
 */
function showNotificationToast(notification) {
    const iconClass = getNotificationIcon(notification.type);
    
    const toastHtml = `
        <div class="toast notification-toast" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header">
                <i class="${iconClass.icon} me-2"></i>
                <strong class="me-auto">새 알림</strong>
                <small>방금 전</small>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                <strong>${notification.title}</strong><br>
                ${notification.content}
            </div>
        </div>
    `;
    
    const $toast = $(toastHtml);
    $('body').append($toast);
    
    const toast = new bootstrap.Toast($toast[0], {
        delay: 5000,
        autohide: true
    });
    
    toast.show();
    
    // 토스트 클릭 시 해당 페이지로 이동
    $toast.on('click', function() {
        if (notification.url) {
            window.location.href = notification.url;
        }
    });
    
    // 토스트 숨겨진 후 DOM에서 제거
    $toast.on('hidden.bs.toast', function() {
        $(this).remove();
    });
}

/**
 * 일반 토스트 메시지 표시
 */
function showToast(type, message) {
    const toastHtml = `
        <div class="toast" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header">
                <strong class="me-auto">${type === 'success' ? '성공' : '오류'}</strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;
    
    const $toast = $(toastHtml);
    $('body').append($toast);
    
    const toast = new bootstrap.Toast($toast[0], {
        delay: 3000,
        autohide: true
    });
    
    toast.show();
    
    $toast.on('hidden.bs.toast', function() {
        $(this).remove();
    });
}