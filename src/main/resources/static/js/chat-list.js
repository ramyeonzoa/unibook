/**
 * 채팅 목록 페이지 실시간 업데이트
 */

class ChatListManager {
    constructor() {
        this.currentUserId = null;
        this.db = null;
        this.unsubscribes = [];
        this.chatRooms = new Map(); // chatRoomId -> chatRoom data
        
        console.log('ChatListManager 초기화 시작');
        this.init();
    }
    
    async init() {
        try {
            // 사용자 ID 가져오기
            this.currentUserId = this.getCurrentUserId();
            if (!this.currentUserId) {
                console.log('사용자 ID를 찾을 수 없음, 채팅 목록 실시간 업데이트 비활성화');
                return;
            }
            
            console.log('채팅 목록 실시간 업데이트 시작, 사용자 ID:', this.currentUserId);
            
            // Firebase 초기화 대기
            await this.waitForFirebase();
            
            this.db = getFirestore();
            console.log('Firebase Firestore 연결 완료');
            
            // 초기 채팅방 목록 로드
            await this.loadInitialChatRooms();
            
            // 실시간 리스너 설정
            this.setupRealtimeListeners();
            
            console.log('채팅 목록 실시간 업데이트 초기화 완료');
            
        } catch (error) {
            console.error('ChatListManager 초기화 실패:', error);
        }
    }
    
    getCurrentUserId() {
        const userIdMeta = document.querySelector('meta[name="user-id"]');
        return userIdMeta ? parseInt(userIdMeta.getAttribute('content')) : null;
    }
    
    async waitForFirebase() {
        return new Promise((resolve, reject) => {
            let attempts = 0;
            const maxAttempts = 20;
            
            const checkFirebase = () => {
                attempts++;
                
                if (typeof firebase !== 'undefined' && 
                    typeof initializeFirebase === 'function' && 
                    typeof getFirestore === 'function') {
                    
                    if (!initializeFirebase()) {
                        reject(new Error('Firebase 초기화 실패'));
                        return;
                    }
                    
                    resolve();
                } else if (attempts >= maxAttempts) {
                    reject(new Error('Firebase 초기화 시간 초과'));
                } else {
                    setTimeout(checkFirebase, 500);
                }
            };
            
            checkFirebase();
        });
    }
    
    /**
     * 초기 채팅방 목록 로드
     */
    async loadInitialChatRooms() {
        try {
            const response = await $.get('/api/chat/rooms');
            if (response.success && response.data) {
                response.data.forEach(chatRoom => {
                    this.chatRooms.set(chatRoom.chatRoomId, chatRoom);
                });
                console.log('초기 채팅방 목록 로드 완료:', this.chatRooms.size, '개');
            }
        } catch (error) {
            console.error('초기 채팅방 목록 로드 실패:', error);
        }
    }
    
    /**
     * 실시간 리스너 설정
     */
    setupRealtimeListeners() {
        // 현재 사용자가 참여한 채팅방들만 모니터링
        this.chatRooms.forEach((chatRoom, chatRoomId) => {
            this.setupChatRoomListener(chatRoom.firebaseRoomId, chatRoomId);
        });
    }
    
    /**
     * 특정 채팅방의 실시간 리스너 설정
     */
    setupChatRoomListener(firebaseRoomId, chatRoomId) {
        if (!firebaseRoomId) {
            console.warn('Firebase Room ID가 없음:', chatRoomId);
            return;
        }
        
        console.log('채팅방 리스너 설정:', firebaseRoomId);
        
        // 해당 채팅방의 메시지들을 모니터링 (새 메시지만 감지)
        const messagesRef = this.db.collection('chatrooms')
            .doc(firebaseRoomId)
            .collection('messages')
            .orderBy('timestamp', 'desc')
            .limit(1); // 최신 메시지 1개만
        
        let isFirstLoad = true;
        
        const unsubscribe = messagesRef.onSnapshot((snapshot) => {
            if (!snapshot.empty) {
                const latestMessage = snapshot.docs[0].data();
                
                // 첫 로드가 아닐 때만 새 메시지로 처리
                if (!isFirstLoad) {
                    console.log('채팅방', chatRoomId, '새 메시지 감지:', latestMessage);
                    
                    // 채팅방 카드 업데이트
                    this.updateChatRoomCard(chatRoomId, latestMessage);
                    
                    // 읽지 않은 메시지 수 업데이트
                    this.updateUnreadCount(chatRoomId, latestMessage);
                } else {
                    console.log('채팅방', chatRoomId, '초기 메시지 로드:', latestMessage.content?.substring(0, 20));
                }
                
                isFirstLoad = false;
            }
        }, (error) => {
            console.error('채팅방 리스너 오류:', firebaseRoomId, error);
        });
        
        this.unsubscribes.push(unsubscribe);
    }
    
    /**
     * 채팅방 카드 업데이트 (최신 메시지)
     */
    updateChatRoomCard(chatRoomId, latestMessage) {
        const $chatItem = $(`.chat-item[onclick*="/chat/rooms/${chatRoomId}"]`);
        if ($chatItem.length === 0) {
            console.warn('채팅방 카드를 찾을 수 없음:', chatRoomId);
            return;
        }
        
        // 최신 메시지 업데이트
        const $lastMessage = $chatItem.find('.chat-last-message');
        if ($lastMessage.length > 0) {
            const messagePreview = this.getMessagePreview(latestMessage);
            $lastMessage.text(messagePreview);
            console.log('채팅방', chatRoomId, '최신 메시지 업데이트:', messagePreview);
        }
        
        // 시간 업데이트
        const $time = $chatItem.find('.chat-time');
        if ($time.length > 0 && latestMessage.timestamp) {
            const timeString = this.formatTime(latestMessage.timestamp.toDate());
            $time.text(timeString);
        }
        
        // 카드에 애니메이션 효과 추가
        $chatItem.addClass('highlight');
        setTimeout(() => {
            $chatItem.removeClass('highlight');
        }, 1000);
    }
    
    /**
     * 읽지 않은 메시지 수 업데이트
     */
    async updateUnreadCount(chatRoomId, latestMessage) {
        // 내가 보낸 메시지면 읽지 않은 수를 업데이트하지 않음
        if (latestMessage.senderId === this.currentUserId) {
            console.log('내가 보낸 메시지, 읽지 않은 수 업데이트 안 함');
            return;
        }
        
        try {
            // 현재 배지의 숫자를 가져와서 +1 증가시키는 간단한 방법 사용
            const $chatItem = $(`.chat-item[onclick*="/chat/rooms/${chatRoomId}"]`);
            const $unreadBadge = $chatItem.find('.unread-badge');
            
            let currentCount = 0;
            if ($unreadBadge.length > 0 && $unreadBadge.is(':visible')) {
                currentCount = parseInt($unreadBadge.text()) || 0;
            }
            
            const newCount = currentCount + 1;
            console.log('읽지 않은 메시지 수 증가:', chatRoomId, currentCount, '->', newCount);
            
            this.updateUnreadBadge(chatRoomId, newCount);
            
            // 전체 읽지 않은 메시지 수도 업데이트
            this.updateTotalUnreadCount();
            
        } catch (error) {
            console.error('읽지 않은 메시지 수 업데이트 실패:', chatRoomId, error);
        }
    }
    
    /**
     * 읽지 않은 메시지 배지 업데이트
     */
    updateUnreadBadge(chatRoomId, unreadCount) {
        const $chatItem = $(`.chat-item[onclick*="/chat/rooms/${chatRoomId}"]`);
        const $unreadBadge = $chatItem.find('.unread-badge');
        
        if (unreadCount > 0) {
            if ($unreadBadge.length > 0) {
                $unreadBadge.text(unreadCount).show();
                
                // 애니메이션 효과 추가
                $unreadBadge.addClass('badge-update');
                setTimeout(() => {
                    $unreadBadge.removeClass('badge-update');
                }, 500);
            } else {
                // 배지가 없으면 새로 생성
                const $meta = $chatItem.find('.chat-meta');
                $meta.append(`<div class="unread-badge badge-new">${unreadCount}</div>`);
                
                // 새 배지 애니메이션
                setTimeout(() => {
                    $meta.find('.unread-badge').removeClass('badge-new');
                }, 300);
            }
            
            console.log('채팅방', chatRoomId, '읽지 않은 메시지 배지 업데이트:', unreadCount);
        } else {
            $unreadBadge.hide();
        }
    }
    
    /**
     * 전체 읽지 않은 메시지 수 업데이트
     */
    async updateTotalUnreadCount() {
        try {
            // 클라이언트에서 직접 계산
            let totalCount = 0;
            $('.chat-item .unread-badge:visible').each(function() {
                const count = parseInt($(this).text()) || 0;
                totalCount += count;
            });
            
            console.log('클라이언트에서 계산된 총 읽지 않은 메시지 수:', totalCount);
            
            const $totalUnread = $('.total-unread');
            if (totalCount > 0) {
                if ($totalUnread.length > 0) {
                    $totalUnread.find('strong').text(totalCount);
                    $totalUnread.show();
                } else {
                    // 전체 읽지 않은 메시지 표시 영역 생성
                    const totalUnreadHtml = `
                        <div class="total-unread">
                            <div class="d-flex align-items-center">
                                <i class="bi bi-bell-fill me-2"></i>
                                <span>읽지 않은 메시지 <strong>${totalCount}</strong>개</span>
                            </div>
                        </div>
                    `;
                    $('.chat-list').before(totalUnreadHtml);
                }
            } else {
                $totalUnread.hide();
            }
            
        } catch (error) {
            console.error('전체 읽지 않은 메시지 수 업데이트 실패:', error);
        }
    }
    
    /**
     * 메시지 미리보기 생성
     */
    getMessagePreview(message) {
        if (message.type === 'IMAGE') {
            return '📷 이미지';
        } else {
            const content = message.content || '';
            return content.length > 30 ? content.substring(0, 30) + '...' : content;
        }
    }
    
    /**
     * 시간 포맷팅
     */
    formatTime(date) {
        const now = new Date();
        const messageDate = new Date(date);
        
        // 오늘인지 확인
        const isToday = now.toDateString() === messageDate.toDateString();
        
        if (isToday) {
            return messageDate.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        } else {
            return messageDate.toLocaleDateString('ko-KR', {
                month: 'numeric',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    }
    
    /**
     * 리스너 정리
     */
    cleanup() {
        console.log('ChatListManager 정리 시작');
        this.unsubscribes.forEach(unsubscribe => {
            if (typeof unsubscribe === 'function') {
                unsubscribe();
            }
        });
        this.unsubscribes = [];
        this.chatRooms.clear();
        console.log('ChatListManager 정리 완료');
    }
}

// 전역 변수
let chatListManager = null;

// 페이지 로드 시 초기화
$(document).ready(function() {
    // 채팅 목록 페이지에서만 실행
    if (window.location.pathname === '/chat') {
        console.log('채팅 목록 페이지 감지, ChatListManager 초기화');
        
        setTimeout(() => {
            chatListManager = new ChatListManager();
            window.chatListManager = chatListManager;
        }, 2000); // Firebase 초기화 대기
    }
});

// 페이지 언로드 시 정리
$(window).on('beforeunload', function() {
    if (chatListManager) {
        chatListManager.cleanup();
    }
});

// 페이지 포커스 이벤트 처리
$(window).on('focus', function() {
    if (chatListManager && window.location.pathname === '/chat') {
        // 채팅 목록 페이지로 돌아왔을 때 전체 읽지 않은 메시지 수 새로고침
        chatListManager.updateTotalUnreadCount();
        console.log('채팅 목록 페이지 포커스, 읽지 않은 메시지 수 새로고침');
    }
});

// CSS 추가 (하이라이트 효과)
$('head').append(`
<style>
.chat-item.highlight {
    background-color: #e7f3ff !important;
    border-color: #b6d7ff !important;
    transform: translateY(-2px) !important;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2) !important;
}

.chat-item {
    transition: all 0.3s ease !important;
}

.unread-badge {
    background: #dc3545;
    color: white;
    border-radius: 50%;
    padding: 4px 8px;
    font-size: 12px;
    font-weight: bold;
    min-width: 20px;
    text-align: center;
    margin-top: 4px;
}

.total-unread {
    animation: fadeIn 0.5s ease-in-out;
}

.badge-update {
    animation: badgeUpdate 0.5s ease-in-out;
}

.badge-new {
    animation: badgeNew 0.3s ease-in-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
}

@keyframes badgeUpdate {
    0% { transform: scale(1); }
    50% { transform: scale(1.2); background: #ff6b6b; }
    100% { transform: scale(1); }
}

@keyframes badgeNew {
    0% { opacity: 0; transform: scale(0); }
    50% { transform: scale(1.1); }
    100% { opacity: 1; transform: scale(1); }
}
</style>
`);