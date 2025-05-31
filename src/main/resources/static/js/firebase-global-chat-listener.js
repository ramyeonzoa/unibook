/**
 * Firebase Global Chat Listener
 * 모든 페이지에서 실시간 채팅 알림을 감지하고 처리
 */

let globalChatListener = null;

/**
 * 글로벌 채팅 리스너 클래스
 */
class GlobalChatListener {
    constructor(currentUserId) {
        this.currentUserId = currentUserId;
        this.db = null;
        this.unsubscribe = null;
        this.isInitialized = false;
        this.processedMessages = new Set(); // 중복 처리 방지
        this.lastSeenTimestamps = new Map(); // 채팅방별 마지막 확인 시간
        this.unsubscribes = []; // 개별 리스너들 관리
        
        console.log('GlobalChatListener 생성:', { currentUserId: this.currentUserId });
        
        this.init();
    }
    
    /**
     * 글로벌 리스너 초기화
     */
    async init() {
        try {
            // Firebase 초기화 대기
            await this.waitForFirebase();
            
            // Firebase 초기화 확인 후 Firestore 연결
            if (!initializeFirebase()) {
                throw new Error('Firebase 초기화 실패');
            }
            
            this.db = getFirestore();
            console.log('Firebase Firestore 연결 완료');
            
            // 간단한 방식: 채팅 목록 스타일의 개별 리스너 설정
            this.setupSimpleUserChatRoomListeners();
            
            this.isInitialized = true;
            console.log('GlobalChatListener 초기화 완료');
            
        } catch (error) {
            console.error('GlobalChatListener 초기화 실패:', error);
        }
    }
    
    /**
     * Firebase 초기화 대기
     */
    async waitForFirebase() {
        return new Promise((resolve, reject) => {
            let attempts = 0;
            const maxAttempts = 30; // 15초 대기
            
            const checkFirebase = () => {
                attempts++;
                
                // Firebase 및 관련 함수들이 모두 로드되었는지 확인
                if (typeof firebase !== 'undefined' && 
                    typeof initializeFirebase === 'function' && 
                    typeof getFirestore === 'function') {
                    console.log('Firebase 및 관련 함수들 준비 완료');
                    resolve();
                } else if (attempts >= maxAttempts) {
                    reject(new Error('Firebase 초기화 시간 초과'));
                } else {
                    console.log(`Firebase 대기 중... (${attempts}/${maxAttempts})`, {
                        firebase: typeof firebase !== 'undefined',
                        initializeFirebase: typeof initializeFirebase === 'function',
                        getFirestore: typeof getFirestore === 'function'
                    });
                    setTimeout(checkFirebase, 500);
                }
            };
            
            checkFirebase();
        });
    }
    
    /**
     * 글로벌 메시지 리스너 설정
     * 시간 기반 필터링으로 실제 새 메시지만 감지
     */
    setupGlobalMessageListener() {
        console.log('글로벌 메시지 리스너 설정 시작');
        
        try {
            // 현재 시간을 기준점으로 설정 (이후 메시지만 감지)
            const cutoffTime = firebase.firestore.Timestamp.now();
            this.startTime = cutoffTime.toDate();
            console.log('글로벌 리스너 기준 시간:', this.startTime);
            
            // 현재 시점 이후의 메시지만 감지하도록 필터 추가
            const messagesQuery = this.db.collectionGroup('messages')
                .where('timestamp', '>', cutoffTime)
                .orderBy('timestamp', 'asc');
            
            this.unsubscribe = messagesQuery.onSnapshot((snapshot) => {
                console.log('글로벌 메시지 스냅샷 수신:', snapshot.size, '개 새 메시지');
                
                // 변경된 문서만 처리 (실제 새 메시지만)
                snapshot.docChanges().forEach((change) => {
                    if (change.type === 'added') {
                        console.log('글로벌 새 메시지 감지:', change.doc.data().content?.substring(0, 20));
                        this.handleNewMessage(change.doc);
                    }
                });
                
            }, (error) => {
                console.error('글로벌 메시지 리스너 오류:', error);
                
                // 인덱스 오류인 경우 다른 방식으로 시도
                if (error.code === 'failed-precondition' || error.message.includes('index')) {
                    console.log('인덱스 오류 감지, 대안 방식으로 재시도');
                    this.setupFallbackGlobalListener();
                } else {
                    // 기타 오류 시 재시도
                    setTimeout(() => {
                        console.log('글로벌 메시지 리스너 재연결 시도');
                        this.setupGlobalMessageListener();
                    }, 5000);
                }
            });
            
            console.log('글로벌 메시지 리스너 설정 완료 (시간 필터링)');
            
        } catch (error) {
            console.error('글로벌 메시지 리스너 설정 실패:', error);
            this.setupFallbackGlobalListener();
        }
    }
    
    /**
     * 사용자 참여 채팅방들에 대한 개별 리스너 설정
     */
    async setupUserChatRoomListeners() {
        console.log('사용자 채팅방별 개별 리스너 설정 시작');
        
        try {
            // 서버에서 사용자가 참여한 채팅방 목록 조회
            const response = await $.get('/api/chat/rooms');
            if (!response.success || !response.data) {
                console.log('채팅방 목록이 없음, 글로벌 리스너 설정 안 함');
                return;
            }
            
            console.log('사용자 채팅방 목록:', response.data.length, '개');
            
            // 각 채팅방에 대해 개별 리스너 설정
            response.data.forEach(chatRoom => {
                if (chatRoom.firebaseRoomId) {
                    this.setupIndividualChatRoomListener(chatRoom.firebaseRoomId, chatRoom.chatRoomId);
                }
            });
            
            console.log('모든 채팅방 리스너 설정 완료');
            
        } catch (error) {
            console.error('사용자 채팅방 리스너 설정 실패:', error);
        }
    }
    
    /**
     * 개별 채팅방 리스너 설정
     */
    setupIndividualChatRoomListener(firebaseRoomId, chatRoomId) {
        console.log('개별 채팅방 리스너 설정:', firebaseRoomId);
        
        try {
            // 현재 시점 이후의 메시지만 감지
            const cutoffTime = firebase.firestore.Timestamp.now();
            
            const messagesRef = this.db.collection('chatrooms')
                .doc(firebaseRoomId)
                .collection('messages')
                .where('timestamp', '>', cutoffTime)
                .orderBy('timestamp', 'asc');
            
            const unsubscribe = messagesRef.onSnapshot((snapshot) => {
                console.log(`채팅방 ${firebaseRoomId} 새 메시지:`, snapshot.size, '개');
                
                snapshot.docChanges().forEach((change) => {
                    if (change.type === 'added') {
                        const messageData = change.doc.data();
                        console.log(`채팅방 ${firebaseRoomId} 새 메시지:`, messageData.content?.substring(0, 20));
                        this.handleNewMessage(change.doc);
                    }
                });
                
            }, (error) => {
                console.error(`채팅방 ${firebaseRoomId} 리스너 오류:`, error);
                
                // 인덱스 오류 시 대안 방식
                if (error.code === 'failed-precondition' || error.message.includes('index')) {
                    console.log(`채팅방 ${firebaseRoomId} 대안 방식으로 재설정`);
                    this.setupFallbackIndividualListener(firebaseRoomId, chatRoomId);
                }
            });
            
            this.unsubscribes.push(unsubscribe);
            
        } catch (error) {
            console.error(`채팅방 ${firebaseRoomId} 리스너 설정 실패:`, error);
            this.setupFallbackIndividualListener(firebaseRoomId, chatRoomId);
        }
    }
    
    /**
     * 간단한 방식: 채팅 목록과 동일한 개별 리스너 설정
     */
    async setupSimpleUserChatRoomListeners() {
        console.log('간단한 채팅방 리스너 설정 시작');
        
        try {
            // 서버에서 사용자가 참여한 채팅방 목록 조회
            const response = await $.get('/api/chat/rooms');
            if (!response.success || !response.data) {
                console.log('채팅방 목록이 없음, 리스너 설정 안 함');
                return;
            }
            
            console.log('간단한 방식 채팅방 목록:', response.data.length, '개');
            
            // 각 채팅방에 대해 최신 메시지 1개만 모니터링 (chat-list.js와 동일)
            response.data.forEach(chatRoom => {
                if (chatRoom.firebaseRoomId) {
                    this.setupSimpleIndividualListener(chatRoom.firebaseRoomId, chatRoom.chatRoomId);
                }
            });
            
            console.log('간단한 방식 모든 채팅방 리스너 설정 완료');
            
        } catch (error) {
            console.error('간단한 방식 채팅방 리스너 설정 실패:', error);
        }
    }
    
    /**
     * 간단한 개별 채팅방 리스너 (최신 메시지 1개만 감지)
     */
    setupSimpleIndividualListener(firebaseRoomId, chatRoomId) {
        console.log('간단한 개별 채팅방 리스너 설정:', firebaseRoomId);
        
        try {
            // 현재 시점을 기준점으로 설정 (이후 메시지만 알림 대상)
            const startTime = firebase.firestore.Timestamp.now();
            this.lastSeenTimestamps.set(chatRoomId, startTime.toDate());
            
            console.log(`채팅방 ${chatRoomId} 기준 시간 설정:`, startTime.toDate());
            
            // chat-list.js와 동일한 방식: 최신 메시지 1개만 모니터링
            const messagesRef = this.db.collection('chatrooms')
                .doc(firebaseRoomId)
                .collection('messages')
                .orderBy('timestamp', 'desc')
                .limit(1);
            
            let isFirstLoad = true;
            
            const unsubscribe = messagesRef.onSnapshot((snapshot) => {
                if (!snapshot.empty) {
                    const latestMessage = snapshot.docs[0].data();
                    const messageTime = latestMessage.timestamp ? latestMessage.timestamp.toDate() : new Date();
                    
                    // 첫 로드가 아닐 때만 새 메시지로 처리
                    if (!isFirstLoad) {
                        console.log(`간단한 리스너 채팅방 ${firebaseRoomId} 새 메시지:`, latestMessage.content?.substring(0, 20));
                        
                        // 메시지 시간으로 기준 시간 업데이트 (내가 보낸 메시지든 받은 메시지든 상관없이)
                        this.lastSeenTimestamps.set(chatRoomId, messageTime);
                        console.log(`채팅방 ${chatRoomId} 기준 시간 업데이트 (새 메시지):`, messageTime);
                        
                        // 내가 보낸 메시지가 아니고, 현재 해당 채팅방에 있지 않을 때만 알림
                        if (latestMessage.senderId !== this.currentUserId) {
                            const currentPath = window.location.pathname;
                            const isInSameChatRoom = currentPath.includes(`/chat/rooms/${chatRoomId}`);
                            
                            if (!isInSameChatRoom) {
                                console.log(`채팅방 ${chatRoomId} 글로벌 알림 전송`);
                                this.sendGlobalNotification(latestMessage, chatRoomId);
                            } else {
                                console.log(`채팅방 ${chatRoomId}에 있어서 글로벌 알림 안 함`);
                            }
                        } else {
                            console.log(`채팅방 ${chatRoomId} 내가 보낸 메시지라 알림 안 함`);
                        }
                    } else {
                        console.log(`채팅방 ${firebaseRoomId} 초기 메시지 로드:`, latestMessage.content?.substring(0, 20));
                        
                        // 첫 로드 시 메시지 시간을 기준 시간으로 업데이트 (이미 본 메시지는 알림 안 함)
                        if (latestMessage.timestamp) {
                            this.lastSeenTimestamps.set(chatRoomId, latestMessage.timestamp.toDate());
                            console.log(`채팅방 ${chatRoomId} 기준 시간 업데이트:`, latestMessage.timestamp.toDate());
                        }
                    }
                    
                    isFirstLoad = false;
                }
            }, (error) => {
                console.error(`간단한 리스너 채팅방 ${firebaseRoomId} 오류:`, error);
            });
            
            this.unsubscribes.push(unsubscribe);
            
        } catch (error) {
            console.error(`간단한 리스너 채팅방 ${firebaseRoomId} 설정 실패:`, error);
        }
    }
    
    /**
     * 글로벌 알림 전송 (헤더 배지, 토스트)
     */
    sendGlobalNotification(messageData, chatRoomId) {
        console.log('글로벌 알림 전송:', messageData.content?.substring(0, 20), 'chatRoomId:', chatRoomId);
        
        // 채팅 알림 매니저를 통해 알림 처리
        if (window.chatNotificationManager) {
            window.chatNotificationManager.onNewMessage(
                messageData.senderName,
                messageData.content,
                chatRoomId
            );
        } else {
            // 채팅 알림 매니저가 없어도 기본 토스트 알림은 표시
            this.showBasicChatToast(messageData.senderName, messageData.content, chatRoomId);
        }
    }
    
    // handleNewMessage 메서드는 더 이상 사용하지 않음 (간단한 방식으로 대체)
    
    /**
     * 문서 참조에서 채팅방 ID 추출
     */
    async extractChatRoomId(messageRef) {
        try {
            // messageRef.parent는 messages 컬렉션
            // messageRef.parent.parent는 chatroom 문서
            const chatroomRef = messageRef.parent.parent;
            const firebaseRoomId = chatroomRef.id;
            
            // Firebase room ID를 사용하여 실제 채팅방 ID 조회
            const chatRoomId = await this.getChatRoomIdFromFirebaseId(firebaseRoomId);
            
            return chatRoomId;
            
        } catch (error) {
            console.error('채팅방 ID 추출 실패:', error);
            return null;
        }
    }
    
    /**
     * Firebase Room ID로부터 실제 채팅방 ID 조회
     */
    async getChatRoomIdFromFirebaseId(firebaseRoomId) {
        try {
            const response = await $.get(`/api/chat/rooms/by-firebase-id/${firebaseRoomId}`);
            
            if (response.success && response.data) {
                return response.data.chatRoomId;
            } else {
                console.warn('채팅방 정보 조회 실패:', firebaseRoomId);
                return null;
            }
            
        } catch (error) {
            console.error('채팅방 ID 조회 API 오류:', error);
            // API 오류 시 Firebase Room ID를 그대로 사용 (fallback)
            return firebaseRoomId;
        }
    }
    
    /**
     * 기본 채팅 토스트 알림 (채팅 알림 매니저가 없을 때)
     */
    showBasicChatToast(senderName, message, chatRoomId) {
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
        if ($('.global-chat-toast-container').length === 0) {
            $('body').append('<div class="global-chat-toast-container position-fixed bottom-0 end-0 p-3" style="z-index: 1090;"></div>');
        }
        
        const $toast = $(toastHtml);
        $('.global-chat-toast-container').append($toast);
        
        const toast = new bootstrap.Toast($toast[0], {
            delay: 5000,
            autohide: true
        });
        
        toast.show();
        
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
     * HTML 이스케이프
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    /**
     * 리스너 정리
     */
    cleanup() {
        if (this.unsubscribe) {
            console.log('글로벌 채팅 리스너 정리');
            this.unsubscribe();
            this.unsubscribe = null;
        }
        
        // 개별 리스너들 정리
        this.unsubscribes.forEach(unsubscribe => {
            if (typeof unsubscribe === 'function') {
                unsubscribe();
            }
        });
        this.unsubscribes = [];
        
        this.isInitialized = false;
        this.processedMessages.clear();
        this.lastSeenTimestamps.clear();
    }
    
    /**
     * 현재 채팅방 변경 알림 (채팅방 진입 시 호출)
     */
    notifyCurrentChatRoomChanged(chatRoomId) {
        console.log('현재 채팅방 변경:', chatRoomId);
        
        // 채팅방 진입 시 현재 시간을 마지막 확인 시간으로 설정
        if (chatRoomId) {
            this.lastSeenTimestamps.set(chatRoomId, new Date());
            console.log(`채팅방 ${chatRoomId} 진입 시간 업데이트:`, new Date());
        }
    }
    
    /**
     * 메시지를 처리됨으로 표시 (중복 알림 방지)
     */
    markMessageAsProcessed(messageId) {
        this.processedMessages.add(messageId);
        console.log('메시지 처리됨으로 표시:', messageId);
    }
    
    /**
     * 특정 채팅방의 모든 메시지를 처리됨으로 표시
     */
    async markChatRoomMessagesAsProcessed(firebaseRoomId) {
        try {
            if (!this.db) {
                console.warn('Firebase 연결되지 않음, 채팅방 메시지 처리 표시 불가');
                return;
            }
            
            console.log('채팅방의 모든 메시지를 처리됨으로 표시:', firebaseRoomId);
            
            // 채팅방 ID 조회
            const chatRoomId = await this.getChatRoomIdFromFirebaseId(firebaseRoomId);
            if (chatRoomId) {
                // 현재 시간을 마지막 확인 시간으로 설정
                this.lastSeenTimestamps.set(chatRoomId, new Date());
                console.log(`채팅방 ${chatRoomId} 마지막 확인 시간 업데이트:`, new Date());
            }
            
            const messagesRef = this.db.collection('chatrooms')
                .doc(firebaseRoomId)
                .collection('messages');
            
            const snapshot = await messagesRef.get();
            
            snapshot.forEach(doc => {
                this.processedMessages.add(doc.id);
            });
            
            console.log('채팅방 메시지 처리 완료:', firebaseRoomId, snapshot.size, '개 메시지');
            
        } catch (error) {
            console.error('채팅방 메시지 처리 표시 오류:', error);
        }
    }
}

/**
 * 글로벌 채팅 리스너 초기화
 */
function initializeGlobalChatListener() {
    // 로그인한 사용자만 초기화 - 여러 방법으로 사용자 ID 확인
    let currentUserId = null;
    
    // 1. 메타 태그에서 사용자 ID 확인
    const userIdMeta = document.querySelector('meta[name="user-id"]');
    if (userIdMeta) {
        currentUserId = parseInt(userIdMeta.getAttribute('content'));
        console.log('메타 태그에서 사용자 ID 발견:', currentUserId);
    }
    
    // 2. 메타 태그가 없으면 AJAX로 현재 사용자 정보 확인
    if (!currentUserId || isNaN(currentUserId)) {
        console.log('메타 태그에서 사용자 ID를 찾을 수 없음, AJAX로 확인 시도');
        
        // 동기 AJAX로 사용자 정보 확인 (초기화 단계에서만 사용)
        $.ajax({
            url: '/api/users/me',
            method: 'GET',
            async: false, // 초기화 단계에서만 동기 호출
            success: function(response) {
                if (response.success && response.data && response.data.userId) {
                    currentUserId = response.data.userId;
                    console.log('AJAX에서 사용자 ID 확인:', currentUserId);
                } else {
                    console.log('AJAX 응답에서 사용자 정보 없음');
                }
            },
            error: function() {
                console.log('사용자 정보 조회 실패 - 로그인하지 않은 사용자');
            }
        });
    }
    
    // 여전히 사용자 ID가 없으면 로그인하지 않은 사용자
    if (!currentUserId || isNaN(currentUserId)) {
        console.log('로그인하지 않은 사용자, 글로벌 채팅 리스너 초기화 안 함');
        return;
    }
    
    // 기존 리스너 정리
    if (globalChatListener) {
        globalChatListener.cleanup();
    }
    
    // 새 리스너 생성
    console.log('글로벌 채팅 리스너 초기화 시작:', currentUserId);
    globalChatListener = new GlobalChatListener(currentUserId);
    
    // 전역 변수로 설정
    window.globalChatListener = globalChatListener;
}

/**
 * 페이지 로드 시 자동 초기화
 */
$(document).ready(function() {
    console.log('🚀 Firebase 글로벌 채팅 리스너 스크립트 로드됨');
    console.log('글로벌 채팅 리스너 자동 초기화 시작');
    
    // Firebase 스크립트들이 모두 로드된 후 초기화
    setTimeout(() => {
        initializeGlobalChatListener();
    }, 2000); // Firebase 초기화를 위해 더 긴 대기 시간
});

/**
 * 페이지 언로드 시 정리
 */
$(window).on('beforeunload', function() {
    if (globalChatListener) {
        globalChatListener.cleanup();
    }
});

/**
 * 페이지 포커스/블러 이벤트 처리
 */
$(window).on('focus', function() {
    if (globalChatListener && !globalChatListener.isInitialized) {
        console.log('페이지 포커스, 글로벌 리스너 재초기화 시도');
        initializeGlobalChatListener();
    }
});

/**
 * 현재 페이지가 채팅방인지 확인하는 헬퍼 함수
 */
function getCurrentChatRoomId() {
    const path = window.location.pathname;
    const match = path.match(/\/chat\/rooms\/(\d+)/);
    return match ? parseInt(match[1]) : null;
}

/**
 * 전역 함수: 채팅방 변경 시 호출
 */
function notifyCurrentChatRoomChanged(chatRoomId) {
    if (globalChatListener) {
        globalChatListener.notifyCurrentChatRoomChanged(chatRoomId);
    }
}