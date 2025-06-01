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
    
    // 채팅 목록 페이지에서 초기 동기화 및 클릭 이벤트 설정
    if (window.location.pathname === '/chat') {
        console.log('채팅 목록 페이지 감지, 동기화 시작');
        initChatListSync();
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
        
        // NEW_MESSAGE 타입은 채팅 시스템에서만 처리
        if (notification.type === 'NEW_MESSAGE') {
            // 현재 채팅방에 있는 경우, 해당 채팅방의 알림은 즉시 읽음 처리
            if (notification.url) {
                const currentPath = window.location.pathname;
                if (currentPath === notification.url) {
                    // 알림을 즉시 읽음으로 표시
                    markAsRead(notification.notificationId, function() {
                        // 현재 채팅방 알림 읽음 처리 완료
                    });
                    
                    // 토스트는 표시하지 않고 리턴
                    return;
                }
            }
            
            // 채팅 배지 업데이트
            incrementChatBadgeCount();
            
            // 토스트는 표시 (현재 채팅방이 아닌 경우)
            showNotificationToast(notification);
            
            // 채팅 목록 페이지에서 채팅방별 배지 업데이트
            if (window.location.pathname === '/chat') {
                updateChatListBadge(notification);
            }
            
            return; // NEW_MESSAGE는 일반 알림 처리 건너뜀
        }
        
        // 다른 타입의 알림들은 정상 처리
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
        
        // 전체 카운트 업데이트 시 채팅 배지도 다시 계산
        loadUnreadNotificationsForChatBadge();
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
                
                // 읽지 않은 알림 목록도 로드하여 채팅 배지 업데이트
                loadUnreadNotificationsForChatBadge();
            }
        })
        .fail(function(xhr) {
            // 알림 카운트 로드 실패
        });
}

/**
 * 채팅 배지 업데이트를 위한 읽지 않은 알림 로드
 */
function loadUnreadNotificationsForChatBadge() {
    $.get('/api/notifications/unread?limit=100')
        .done(function(response) {
            if (response.success) {
                updateChatBadgeFromNotifications(response.data.content);
            }
        })
        .fail(function(xhr) {
            // 채팅 배지용 알림 로드 실패:
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
            // 알림 목록 로드 실패
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
    
    // NEW_MESSAGE 타입 제외
    const filteredNotifications = notifications.filter(function(notification) {
        return notification.type !== 'NEW_MESSAGE';
    });
    
    if (filteredNotifications.length === 0) {
        $lists.html(
            '<li class="empty-state">' +
            '<i class="bi bi-bell-slash"></i>' +
            '<p>새로운 알림이 없습니다</p>' +
            '</li>'
        );
        return;
    }
    
    let html = '';
    filteredNotifications.forEach(function(notification) {
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

// ===============================
// 채팅 배지 관리 함수들 (알림 시스템 통합)
// ===============================

/**
 * 읽지 않은 알림에서 NEW_MESSAGE 타입만 필터링하여 채팅 배지 업데이트
 */
function updateChatBadgeFromNotifications(notifications) {
    // NEW_MESSAGE 타입 알림만 필터링
    const chatNotifications = notifications.filter(n => n.type === 'NEW_MESSAGE' && !n.isRead);
    const chatCount = chatNotifications.length;
    
    console.log('채팅 배지 업데이트:', {
        totalNotifications: notifications.length,
        chatNotifications: chatCount
    });
    
    updateChatBadge(chatCount);
}

/**
 * 채팅 배지 카운트 증가
 */
function incrementChatBadgeCount() {
    const $chatCounts = $('.chat-count');
    if ($chatCounts.length > 0) {
        const currentCount = parseInt($chatCounts.first().text()) || 0;
        updateChatBadge(currentCount + 1);
    }
}

/**
 * 채팅 배지 업데이트 (chat-notification.js에서 가져온 로직)
 */
function updateChatBadge(count) {
    // 클래스 기반으로 모든 채팅 배지 업데이트
    const $badges = $('.chat-badge');
    const $counts = $('.chat-count');
    
    console.log('채팅 배지 업데이트 실행:', { count, badgeFound: $badges.length, countFound: $counts.length });
    
    if ($badges.length === 0 || $counts.length === 0) {
        console.error('채팅 배지 엘리먼트를 찾을 수 없습니다:', {
            badges: $badges.length,
            counts: $counts.length
        });
        return;
    }
    
    console.log('채팅 배지 업데이트 적용:', count);
    
    if (count > 0) {
        $counts.text(count > 99 ? '99+' : count);
        $badges.show();
        $badges.css('display', 'inline-block'); // 강제 표시
        
        console.log('채팅 배지 표시됨:', $badges.length, '개');
        
        // 애니메이션 효과
        $badges.addClass('pulse');
        setTimeout(() => $badges.removeClass('pulse'), 1000);
    } else {
        $badges.hide();
        console.log('채팅 배지 숨김');
    }
}

/**
 * 채팅방 진입 시 채팅 배지에서 해당 채팅방의 알림 수 차감
 * (ChatController에서 호출하는 markChatNotificationsAsRead와 연동)
 */
function decrementChatBadgeForChatRoom(chatRoomId) {
    // 서버에서 읽음 처리가 완료된 후 전체 카운트를 다시 로드
    setTimeout(() => {
        loadUnreadNotificationsForChatBadge();
    }, 500);
}

/**
 * 채팅 목록 페이지에서 개별 채팅방 배지 업데이트
 */
function updateChatListBadge(notification) {
    // URL에서 채팅방 ID 추출 (/chat/rooms/123)
    const urlMatch = notification.url.match(/\/chat\/rooms\/(\d+)/);
    if (!urlMatch) return;
    
    const chatRoomId = urlMatch[1];
    console.log('채팅 목록 배지 업데이트:', { chatRoomId, notification });
    
    // 해당 채팅방의 배지 증가
    const $chatItem = $(`.chat-item[data-chat-room-id="${chatRoomId}"]`);
    if ($chatItem.length > 0) {
        let $badge = $chatItem.find('.unread-badge');
        
        if ($badge.length > 0) {
            // 기존 배지가 있으면 숫자 증가
            const currentCount = parseInt($badge.text()) || 0;
            const newCount = currentCount + 1;
            $badge.text(newCount);
        } else {
            // 배지가 없으면 새로 생성
            const $metaDiv = $chatItem.find('.chat-meta');
            if ($metaDiv.length > 0) {
                $badge = $('<div class="unread-badge">1</div>');
                $metaDiv.append($badge);
            }
        }
        
        if ($badge.length > 0) {
            $badge.show();
            
            // 애니메이션 효과
            $badge.addClass('pulse');
            setTimeout(() => $badge.removeClass('pulse'), 1000);
            
            console.log(`채팅방 ${chatRoomId} 배지 업데이트: ${$badge.text()}`);
        }
    }
    
    // 전체 "읽지 않은 메시지 n개" 카운트 업데이트
    updateTotalUnreadCountDisplay();
}

/**
 * 채팅 목록 페이지의 전체 읽지 않은 메시지 카운트 업데이트
 */
function updateTotalUnreadCountDisplay() {
    let totalCount = 0;
    
    // 모든 채팅방의 읽지 않은 메시지 수 합계
    $('.unread-badge:visible').each(function() {
        const count = parseInt($(this).text()) || 0;
        totalCount += count;
    });
    
    // "읽지 않은 메시지 n개" 텍스트 업데이트
    const $totalUnreadDiv = $('.total-unread');
    const $countSpan = $totalUnreadDiv.find('strong');
    
    if (totalCount > 0) {
        if ($countSpan.length > 0) {
            $countSpan.text(totalCount);
        }
        $totalUnreadDiv.show();
    } else {
        $totalUnreadDiv.hide();
    }
    
    console.log('전체 읽지 않은 메시지 수 업데이트:', totalCount);
}

/**
 * 채팅방 진입 시 해당 채팅방의 배지 감소 (채팅 목록 페이지용)
 */
function decrementChatListBadge(chatRoomId) {
    const $chatItem = $(`.chat-item[data-chat-room-id="${chatRoomId}"]`);
    if ($chatItem.length > 0) {
        const $badge = $chatItem.find('.unread-badge');
        
        if ($badge.length > 0) {
            $badge.remove(); // 배지 완전 제거
            console.log(`채팅방 ${chatRoomId} 배지 제거`);
        }
    }
    
    // 전체 카운트도 업데이트
    updateTotalUnreadCountDisplay();
}

/**
 * 채팅 목록 페이지 초기 동기화 및 이벤트 설정
 */
function initChatListSync() {
    console.log('채팅 목록 페이지 동기화 초기화');
    
    // 1. 페이지 로드 시 읽지 않은 NEW_MESSAGE 알림들로 배지 동기화
    syncChatListWithNotifications();
    
    // 2. 시간 정규화 (서버 시간을 일관된 포맷으로 변환)
    normalizeInitialTimestamps();
    
    // 3. 채팅방 클릭 시 배지 감소 이벤트 등록
    $(document).on('click', '.chat-item', function() {
        const chatRoomId = $(this).data('chat-room-id');
        if (chatRoomId) {
            console.log('채팅방 클릭:', chatRoomId);
            
            // 약간의 지연 후 배지 감소 (페이지 이동 후 서버에서 읽음 처리 완료될 시간)
            setTimeout(() => {
                decrementChatListBadge(chatRoomId);
            }, 500);
        }
    });
}

/**
 * 읽지 않은 알림과 채팅 목록 동기화
 */
function syncChatListWithNotifications() {
    $.get('/api/notifications/unread?limit=100')
        .done(function(response) {
            if (response.success && response.data && response.data.content) {
                console.log('채팅 목록 동기화 시작, 총 알림:', response.data.content.length);
                
                // NEW_MESSAGE 타입 알림만 필터링
                const chatNotifications = response.data.content.filter(n => n.type === 'NEW_MESSAGE');
                
                // 채팅방별 알림 개수 계산
                const chatRoomCounts = {};
                chatNotifications.forEach(notification => {
                    const urlMatch = notification.url.match(/\/chat\/rooms\/(\d+)/);
                    if (urlMatch) {
                        const chatRoomId = urlMatch[1];
                        chatRoomCounts[chatRoomId] = (chatRoomCounts[chatRoomId] || 0) + 1;
                    }
                });
                
                console.log('채팅방별 알림 개수:', chatRoomCounts);
                
                // 각 채팅방의 배지 업데이트
                Object.keys(chatRoomCounts).forEach(chatRoomId => {
                    const count = chatRoomCounts[chatRoomId];
                    const $chatItem = $(`.chat-item[data-chat-room-id="${chatRoomId}"]`);
                    
                    if ($chatItem.length > 0) {
                        let $badge = $chatItem.find('.unread-badge');
                        
                        if ($badge.length > 0) {
                            // 기존 배지 업데이트
                            $badge.text(count);
                        } else {
                            // 새 배지 생성
                            const $metaDiv = $chatItem.find('.chat-meta');
                            if ($metaDiv.length > 0) {
                                $badge = $(`<div class="unread-badge">${count}</div>`);
                                $metaDiv.append($badge);
                            }
                        }
                        
                        console.log(`채팅방 ${chatRoomId} 배지 동기화: ${count}`);
                    }
                });
                
                // 전체 카운트 업데이트 (채팅 목록 페이지의 "읽지 않은 메시지 n개" 표시용)
                updateTotalUnreadCountDisplay();
                
                // 헤더의 채팅 배지도 업데이트
                updateChatBadgeFromNotifications(response.data.content);
                
                console.log('채팅 목록 동기화 완료');
            }
        })
        .fail(function(xhr) {
            console.error('채팅 목록 동기화 실패:', xhr);
        });
}

/**
 * 초기 로드 시 서버 시간을 일관된 포맷으로 정규화
 * 서버에서 이미 한국 시간으로 저장하므로 단순히 포맷팅만 수행
 */
function normalizeInitialTimestamps() {
    $('.chat-time[data-timestamp]').each((index, element) => {
        const $timeElement = $(element);
        const timestamp = $timeElement.data('timestamp');
        
        if (timestamp) {
            try {
                // 서버에서 온 timestamp는 이미 한국 시간
                const date = new Date(timestamp);
                const formattedTime = formatChatTime(date);
                $timeElement.text(formattedTime);
            } catch (error) {
                console.error('시간 변환 실패:', timestamp, error);
            }
        }
    });
    
    // 정규화 완료 플래그 설정
    window.chatTimestampsNormalized = true;
    window.notificationTimeNormalizationDone = true;
}


/**
 * 채팅 시간 포맷팅 (서버와 동일한 형식)
 */
function formatChatTime(date) {
    const messageDate = new Date(date);
    
    // 서버와 동일한 포맷 사용: MM/dd HH:mm
    const month = String(messageDate.getMonth() + 1).padStart(2, '0');
    const day = String(messageDate.getDate()).padStart(2, '0');
    const hours = String(messageDate.getHours()).padStart(2, '0');
    const minutes = String(messageDate.getMinutes()).padStart(2, '0');
    
    return `${month}/${day} ${hours}:${minutes}`;
}

// 전역에서 접근 가능하도록 함수 노출
window.updateChatBadgeFromNotifications = updateChatBadgeFromNotifications;
window.decrementChatBadgeForChatRoom = decrementChatBadgeForChatRoom;
window.updateChatListBadge = updateChatListBadge;
window.decrementChatListBadge = decrementChatListBadge;
window.initChatListSync = initChatListSync;
window.formatChatTime = formatChatTime;
window.parseServerTimeAsKST = parseServerTimeAsKST;