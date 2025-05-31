/**
 * 채팅 알림 시스템
 * 기존 알림과 분리된 채팅 전용 알림 관리
 */

class ChatNotificationManager {
    constructor() {
        this.currentChatRoomId = null;
        this.unreadChats = new Map(); // chatRoomId -> unreadCount
        this.totalUnreadCount = 0;
        
        // CSRF 토큰 설정
        this.csrfToken = $('meta[name="_csrf"]').attr('content');
        this.csrfHeader = $('meta[name="_csrf_header"]').attr('content');
        
        this.init();
    }
    
    async init() {
        console.log('ChatNotificationManager 초기화 시작');
        
        // 초기 채팅 알림 개수 로드
        await this.loadUnreadChatCount();
        
        // 현재 채팅방 ID 감지
        this.detectCurrentChatRoom();
        
        // 채팅방별 읽지 않은 메시지 수 로드 (채팅 목록 페이지에서)
        if (window.location.pathname === '/chat') {
            await this.loadChatRoomUnreadCounts();
        }
        
        // 주기적 체크는 글로벌 리스너로 대체됨 (더 이상 사용하지 않음)
        // this.startPeriodicCheck();
        
        console.log('ChatNotificationManager 초기화 완료');
    }
    
    /**
     * 현재 채팅방 ID 감지
     */
    detectCurrentChatRoom() {
        // URL에서 채팅방 ID 추출
        const path = window.location.pathname;
        const match = path.match(/\/chat\/rooms\/(\d+)/);
        if (match) {
            this.currentChatRoomId = parseInt(match[1]);
            console.log('현재 채팅방 ID:', this.currentChatRoomId);
        } else {
            this.currentChatRoomId = null;
        }
    }
    
    
    /**
     * 채팅방의 읽지 않은 메시지 수 로드
     */
    async loadUnreadChatCount() {
        try {
            const response = await $.get('/api/chat/unread-count');
            console.log('채팅 읽지 않은 수 응답:', response);
            
            if (response.success) {
                this.totalUnreadCount = response.data || 0;
                this.updateChatBadge(this.totalUnreadCount);
                console.log('채팅 배지 업데이트 완료:', this.totalUnreadCount);
            }
        } catch (error) {
            console.error('채팅 알림 개수 로드 실패:', error);
        }
    }
    
    /**
     * 각 채팅방별 읽지 않은 메시지 수 로드 (채팅 목록 페이지에서 사용)
     */
    async loadChatRoomUnreadCounts() {
        try {
            const response = await $.get('/api/chat/rooms');
            console.log('채팅방 목록 응답:', response);
            
            if (response.success && response.data) {
                // 각 채팅방의 읽지 않은 메시지 수를 Map에 저장
                this.unreadChats.clear();
                let totalUnread = 0;
                
                response.data.forEach(chatRoom => {
                    if (chatRoom.unreadCount > 0) {
                        this.unreadChats.set(chatRoom.chatRoomId, chatRoom.unreadCount);
                        totalUnread += chatRoom.unreadCount;
                    }
                });
                
                this.totalUnreadCount = totalUnread;
                this.updateChatBadge(this.totalUnreadCount);
                
                console.log('채팅방별 읽지 않은 메시지 수 로드 완료:', {
                    unreadChats: Object.fromEntries(this.unreadChats),
                    totalUnreadCount: this.totalUnreadCount
                });
            }
        } catch (error) {
            console.error('채팅방별 읽지 않은 메시지 수 로드 실패:', error);
        }
    }
    
    /**
     * firebase-chat.js에서 호출되는 새 메시지 처리 (채팅방에서 직접 메시지 수신)
     */
    onNewMessage(senderName, message, chatRoomId = null) {
        console.log('새 채팅 메시지 수신:', { senderName, message, chatRoomId, currentChatRoomId: this.currentChatRoomId });
        
        // 현재 채팅방에 있으면 알림 표시 안 함 (채팅방 ID 비교)
        if (this.currentChatRoomId && chatRoomId && this.currentChatRoomId === chatRoomId) {
            console.log('현재 채팅방에서의 메시지, 알림 표시 안 함');
            return;
        }
        
        // 로컬에서 즉시 UI 업데이트
        this.totalUnreadCount++;
        this.updateChatBadge(this.totalUnreadCount);
        
        // 토스트 알림 표시 (현재 채팅방이 아닌 경우에만)
        this.showChatToast(senderName, message, chatRoomId);
    }
    
    /**
     * firebase-chat.js에서 호출되는 메시지 읽음 처리
     */
    onMessageRead(firebaseRoomId) {
        console.log('메시지 읽음 처리:', firebaseRoomId);
        
        // 읽지 않은 채팅 수 감소
        if (this.totalUnreadCount > 0) {
            this.totalUnreadCount--;
            this.updateChatBadge(this.totalUnreadCount);
        }
    }
    
    /**
     * 새 채팅 메시지 알림 처리 (서버 API 호출용)
     */
    async handleNewMessage(senderId, senderName, message, chatRoomId) {
        // 현재 해당 채팅방에 있으면 알림 표시 안 함
        if (this.currentChatRoomId === chatRoomId) {
            console.log('현재 채팅방에서의 메시지, 알림 표시 안 함');
            return;
        }
        
        // 서버에 채팅 알림 전송 요청
        try {
            await $.ajax({
                url: '/api/chat/notify',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    recipientId: getCurrentUserId(), // 나에게 온 메시지
                    senderId: senderId,
                    senderName: senderName,
                    chatRoomId: chatRoomId,
                    message: message
                }),
                beforeSend: (xhr) => {
                    if (this.csrfHeader && this.csrfToken) {
                        xhr.setRequestHeader(this.csrfHeader, this.csrfToken);
                    }
                }
            });
        } catch (error) {
            console.error('채팅 알림 전송 실패:', error);
        }
        
        // 로컬에서 즉시 UI 업데이트
        this.incrementUnreadCount(chatRoomId);
        
        // 토스트 알림 표시
        this.showChatToast(senderName, message, chatRoomId);
    }
    
    /**
     * 읽지 않은 채팅 수 증가
     */
    incrementUnreadCount(chatRoomId) {
        const currentCount = this.unreadChats.get(chatRoomId) || 0;
        this.unreadChats.set(chatRoomId, currentCount + 1);
        
        this.totalUnreadCount++;
        this.updateChatBadge(this.totalUnreadCount);
    }
    
    /**
     * 채팅 배지 업데이트
     */
    updateChatBadge(count) {
        // 클래스 기반으로 모든 채팅 배지 업데이트
        const $badges = $('.chat-badge');
        const $counts = $('.chat-count');
        
        console.log('채팅 배지 업데이트 시도:', { count, badgeFound: $badges.length, countFound: $counts.length });
        
        if ($badges.length === 0 || $counts.length === 0) {
            console.error('채팅 배지 엘리먼트를 찾을 수 없습니다:', {
                badges: $badges.length,
                counts: $counts.length
            });
            return;
        }
        
        console.log('채팅 배지 업데이트 실행:', count);
        
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
     * 채팅 토스트 알림 표시
     */
    showChatToast(senderName, message, chatRoomId) {
        console.log('채팅 토스트 표시 시도:', { 
            senderName, 
            message, 
            chatRoomId, 
            currentChatRoomId: this.currentChatRoomId 
        });
        
        // 현재 채팅방에 있으면 토스트 표시 안 함 (추가 체크)
        if (this.currentChatRoomId && chatRoomId && this.currentChatRoomId === chatRoomId) {
            console.log('현재 채팅방에서의 메시지, 토스트 표시 안 함');
            return;
        }
        
        // 메시지 미리보기 (최대 50자)
        const preview = message.length > 50 ? message.substring(0, 50) + '...' : message;
        
        const toastHtml = `
            <div class="toast chat-notification-toast" role="alert" aria-live="assertive" aria-atomic="true"
                 data-chat-room-id="${chatRoomId || ''}">
                <div class="toast-header bg-primary text-white">
                    <i class="bi bi-chat-dots-fill me-2"></i>
                    <strong class="me-auto">${this.escapeHtml(senderName)}</strong>
                    <small>새 메시지</small>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${this.escapeHtml(preview)}
                </div>
            </div>
        `;
        
        // 토스트 컨테이너가 없으면 생성
        if ($('.chat-toast-container').length === 0) {
            $('body').append('<div class="chat-toast-container position-fixed bottom-0 end-0 p-3" style="z-index: 1080;"></div>');
        }
        
        const $toast = $(toastHtml);
        $('.chat-toast-container').append($toast);
        
        console.log('토스트 DOM 추가됨:', $toast.length);
        
        const toast = new bootstrap.Toast($toast[0], {
            delay: 5000,
            autohide: true
        });
        
        toast.show();
        console.log('토스트 표시 완료');
        
        // 토스트 클릭 시 해당 채팅방으로 이동
        $toast.on('click', function() {
            const roomId = $(this).data('chat-room-id');
            if (roomId) {
                window.location.href = `/chat/rooms/${roomId}`;
            } else {
                window.location.href = '/chat';
            }
        });
        
        // 토스트 숨겨진 후 DOM에서 제거
        $toast.on('hidden.bs.toast', function() {
            $(this).remove();
        });
    }
    
    /**
     * 채팅방 진입 시 해당 채팅방의 읽지 않은 수 초기화
     */
    clearChatRoomUnread(chatRoomId) {
        const unreadCount = this.unreadChats.get(chatRoomId) || 0;
        if (unreadCount > 0) {
            this.unreadChats.set(chatRoomId, 0);
            this.totalUnreadCount = Math.max(0, this.totalUnreadCount - unreadCount);
            this.updateChatBadge(this.totalUnreadCount);
        }
    }
    
    /**
     * 주기적 채팅 알림 체크 시작
     * @deprecated Firebase 글로벌 리스너로 대체됨. 더 이상 사용하지 않음.
     */
    startPeriodicCheck() {
        console.log('[DEPRECATED] 주기적 체크는 Firebase 글로벌 리스너로 대체되었습니다.');
        return; // 더 이상 폴링을 시작하지 않음
        
        // 이전 코드 (주석 처리)
        // this.checkInterval = setInterval(() => {
        //     this.loadUnreadChatCount();
        // }, 10000);
        // console.log('주기적 채팅 알림 체크 시작');
    }
    
    /**
     * 주기적 체크 중지
     */
    stopPeriodicCheck() {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
            console.log('주기적 채팅 알림 체크 중지');
        }
    }
    
    /**
     * 특정 채팅방의 읽지 않은 메시지 모두 클리어
     */
    async clearChatRoomUnread(chatRoomId) {
        console.log('채팅방 읽지 않은 메시지 클리어:', chatRoomId);
        
        // 서버에서 해당 채팅방의 읽지 않은 메시지 수 조회
        try {
            const response = await $.get(`/api/chat/rooms/${chatRoomId}/unread-count`);
            console.log(`채팅방 ${chatRoomId} 읽지 않은 메시지 수 조회:`, response);
            
            let unreadCount = 0;
            if (response.success && response.data) {
                unreadCount = response.data;
            } else {
                // 서버 응답이 없으면 로컬 Map에서 확인
                unreadCount = this.unreadChats.get(chatRoomId) || 0;
            }
            
            if (unreadCount > 0) {
                // 해당 채팅방의 읽지 않은 수를 0으로 설정
                this.unreadChats.set(chatRoomId, 0);
                
                // 전체 읽지 않은 수에서 차감
                this.totalUnreadCount = Math.max(0, this.totalUnreadCount - unreadCount);
                
                console.log(`채팅방 ${chatRoomId} 읽지 않은 메시지 ${unreadCount}개 클리어, 전체 남은 수: ${this.totalUnreadCount}`);
                
                // 배지 업데이트
                this.updateChatBadge(this.totalUnreadCount);
            }
        } catch (error) {
            console.error('채팅방 읽지 않은 메시지 수 조회 실패:', error);
            
            // 에러 시 로컬 Map 사용
            const currentUnreadCount = this.unreadChats.get(chatRoomId) || 0;
            if (currentUnreadCount > 0) {
                this.unreadChats.set(chatRoomId, 0);
                this.totalUnreadCount = Math.max(0, this.totalUnreadCount - currentUnreadCount);
                this.updateChatBadge(this.totalUnreadCount);
            }
        }
    }
    
    /**
     * 메시지 읽음 처리 (Firebase에서 호출)
     */
    onMessageRead(firebaseRoomId) {
        console.log('메시지 읽음 처리:', firebaseRoomId);
        // Firebase room ID로는 직접 처리하기 어려우므로, 
        // 채팅방 진입 시 clearChatRoomUnread를 호출하는 것이 더 정확
    }
    
    /**
     * HTML 이스케이프
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// 전역 채팅 알림 매니저 인스턴스
let chatNotificationManager = null;

// 페이지 로드 시 초기화
$(document).ready(function() {
    // 로그인한 사용자만 채팅 알림 시스템 초기화
    if ($('meta[name="user-id"]').length > 0) {
        console.log('채팅 알림 시스템 초기화 시작');
        chatNotificationManager = new ChatNotificationManager();
        
        // 전역 변수로 설정하여 firebase-chat.js에서 접근 가능하도록 함
        window.chatNotificationManager = chatNotificationManager;
    }
    
    // 페이지 이동 시 현재 채팅방 감지
    $(window).on('popstate', function() {
        if (chatNotificationManager) {
            console.log('페이지 이동 감지, 채팅방 재감지');
            chatNotificationManager.detectCurrentChatRoom();
        }
    });
    
    // hashchange 이벤트도 감지 (SPA 방식 대응)
    $(window).on('hashchange', function() {
        if (chatNotificationManager) {
            console.log('해시 변경 감지, 채팅방 재감지');
            chatNotificationManager.detectCurrentChatRoom();
        }
    });
    
    // 페이지 언로드 시 정리
    $(window).on('beforeunload', function() {
        if (chatNotificationManager) {
            chatNotificationManager.stopPeriodicCheck();
        }
    });
});

// 현재 사용자 ID 가져오기 헬퍼
function getCurrentUserId() {
    const metaUserId = document.querySelector('meta[name="user-id"]');
    if (metaUserId) {
        return parseInt(metaUserId.getAttribute('content'));
    }
    return null;
}