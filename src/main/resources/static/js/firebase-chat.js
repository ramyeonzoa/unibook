/**
 * Firebase Firestore 채팅 로직
 */

let chatManager = null;

/**
 * 채팅 매니저 클래스
 */
class FirebaseChatManager {
    constructor(firebaseRoomId, currentUserId, currentUserName, isBuyer, otherUserId, otherUserName, chatRoomId) {
        this.firebaseRoomId = firebaseRoomId;
        this.currentUserId = currentUserId;
        this.currentUserName = currentUserName;
        this.db = getFirestore();
        this.messagesRef = null;
        this.unsubscribe = null;
        this.isBuyer = isBuyer || false;
        this.otherUserId = otherUserId || null;
        this.otherUserName = otherUserName || null;
        this.chatRoomId = chatRoomId || null;
        
        console.log('FirebaseChatManager 생성자 호출:', {
            firebaseRoomId: this.firebaseRoomId,
            currentUserId: this.currentUserId,
            isBuyer: this.isBuyer,
            otherUserId: this.otherUserId,
            otherUserName: this.otherUserName,
            chatRoomId: this.chatRoomId
        });
        
        this.initializeChat();
    }
    
    /**
     * 채팅 초기화
     */
    async initializeChat() {
        try {
            console.log('채팅 초기화 시작:', {
                firebaseRoomId: this.firebaseRoomId,
                currentUserId: this.currentUserId,
                currentUserName: this.currentUserName,
                isBuyer: this.isBuyer
            });
            
            // Firestore 컬렉션 참조
            this.messagesRef = this.db.collection('chatrooms')
                .doc(this.firebaseRoomId)
                .collection('messages');
            
            console.log('Firestore 참조 생성 완료:', this.messagesRef.path);
            
            // 실시간 메시지 리스너 설정
            this.setupMessageListener();
            
            console.log('Firebase 채팅 초기화 완료:', this.firebaseRoomId);
        } catch (error) {
            console.error('채팅 초기화 실패:', error);
            alert('채팅을 초기화할 수 없습니다: ' + error.message);
            throw error;
        }
    }
    
    /**
     * 실시간 메시지 리스너 설정
     */
    setupMessageListener() {
        console.log('메시지 리스너 설정 시작');
        
        let isFirstLoad = true;
        
        this.unsubscribe = this.messagesRef
            .orderBy('timestamp', 'asc')
            .onSnapshot((snapshot) => {
                console.log('메시지 스냅샷 수신:', snapshot.size, '개 메시지');
                
                const messages = [];
                const realNewMessages = []; // 실제 새로운 메시지만
                
                snapshot.forEach((doc) => {
                    const data = doc.data();
                    // Firebase timestamp를 Date 객체로 변환
                    let timestamp = new Date();
                    if (data.timestamp) {
                        timestamp = data.timestamp.toDate();
                    }
                    
                    const message = {
                        messageId: doc.id,
                        ...data,
                        timestamp: timestamp
                    };
                    messages.push(message);
                });
                
                // 실제 새로운 메시지 감지 (snapshot 변경사항 기반)
                if (!isFirstLoad) {
                    snapshot.docChanges().forEach((change) => {
                        if (change.type === 'added') {
                            const data = change.doc.data();
                            // 내가 보내지 않은 메시지만 새 메시지로 처리
                            if (data.senderId !== this.currentUserId) {
                                let timestamp = new Date();
                                if (data.timestamp) {
                                    timestamp = data.timestamp.toDate();
                                }
                                
                                const newMessage = {
                                    messageId: change.doc.id,
                                    ...data,
                                    timestamp: timestamp
                                };
                                realNewMessages.push(newMessage);
                                console.log('실제 새 메시지 감지:', newMessage);
                            }
                        }
                    });
                }
                
                console.log('총 메시지 수:', messages.length, '실제 새 메시지 수:', realNewMessages.length);
                
                if (messages.length === 0) {
                    console.log('메시지가 없습니다. 빈 상태 표시');
                    this.displayEmptyState();
                } else {
                    this.displayMessages(messages);
                }
                
                this.scrollToBottom();
                
                // 읽지 않은 메시지 처리
                this.markMessagesAsRead(messages);
                
                // 실제 새 메시지가 있으면 채팅 알림 매니저에게 알림
                if (realNewMessages.length > 0 && typeof window.chatNotificationManager !== 'undefined' && window.chatNotificationManager !== null) {
                    realNewMessages.forEach(message => {
                        console.log('새 메시지 알림 전송:', message);
                        
                        // 시스템 메시지 처리
                        if (message.type === 'SYSTEM' && message.content) {
                            // 거래 상태 변경 메시지 처리
                            if (message.content.includes('거래 상태가')) {
                                this.handleStatusChangeMessage(message.content);
                            }
                            // 채팅방 나가기 메시지 처리
                            else if (message.content.includes('[LEAVE:')) {
                                this.handleLeaveMessage(message.content);
                            }
                        }
                        
                        window.chatNotificationManager.onNewMessage(message.senderName, message.content, this.chatRoomId);
                    });
                } else if (realNewMessages.length > 0) {
                    console.log('채팅 알림 매니저가 없음, 새 메시지:', realNewMessages);
                    
                    // 알림 매니저가 없어도 시스템 메시지 처리는 수행
                    realNewMessages.forEach(message => {
                        if (message.type === 'SYSTEM' && message.content) {
                            // 거래 상태 변경 메시지 처리
                            if (message.content.includes('거래 상태가')) {
                                this.handleStatusChangeMessage(message.content);
                            }
                            // 채팅방 나가기 메시지 처리
                            else if (message.content.includes('[LEAVE:')) {
                                this.handleLeaveMessage(message.content);
                            }
                        }
                    });
                }
                
                // 첫 로드 완료 표시
                isFirstLoad = false;
                
            }, (error) => {
                console.error('메시지 리스너 오류:', error);
                alert('실시간 메시지 연결에 실패했습니다: ' + error.message);
            });
    }
    
    /**
     * 메시지 전송
     */
    async sendMessage(content, type = 'TEXT') {
        if (!content.trim()) return;
        
        try {
            const message = {
                senderId: this.currentUserId,
                senderName: this.currentUserName,
                content: content.trim(),
                type: type,
                timestamp: firebase.firestore.FieldValue.serverTimestamp(),
                isReadByBuyer: this.isBuyer, // 본인이 보낸 메시지는 읽음 처리
                isReadBySeller: !this.isBuyer
            };
            
            // Firestore에 메시지 추가
            const docRef = await this.messagesRef.add(message);
            
            // 상대방의 읽지 않은 메시지 수 업데이트를 위해 Spring Boot 호출 (현재 메시지 내용 전달)
            this.incrementOtherUserUnreadCount(content);
            
            // Spring Boot에 마지막 메시지 정보 업데이트
            this.updateLastMessageInDB(content);
            
            // 더 이상 서버 알림 필요 없음 (Firebase에서 직접 처리)
            
            console.log('메시지 전송 완료:', docRef.id);
            
            return docRef.id;
        } catch (error) {
            console.error('메시지 전송 실패:', error);
            throw error;
        }
    }
    
    /**
     * 이미지 메시지 전송
     */
    async sendImageMessage(imageUrl) {
        if (!imageUrl) return;
        
        try {
            const message = {
                senderId: this.currentUserId,
                senderName: this.currentUserName,
                content: '📷 이미지',
                imageUrl: imageUrl,
                type: 'IMAGE',
                timestamp: firebase.firestore.FieldValue.serverTimestamp(),
                isReadByBuyer: this.isBuyer,
                isReadBySeller: !this.isBuyer
            };
            
            // Firestore에 메시지 추가
            const docRef = await this.messagesRef.add(message);
            
            // 상대방의 읽지 않은 메시지 수 업데이트
            this.incrementOtherUserUnreadCount('📷 이미지');
            
            // Spring Boot에 마지막 메시지 정보 업데이트
            this.updateLastMessageInDB('📷 이미지');
            
            console.log('이미지 메시지 전송 완료:', docRef.id);
            
            return docRef.id;
        } catch (error) {
            console.error('이미지 메시지 전송 실패:', error);
            throw error;
        }
    }
    
    /**
     * 시스템 메시지 전송
     */
    async sendSystemMessage(content) {
        try {
            const message = {
                senderId: null,
                senderName: '시스템',
                content: content,
                type: 'SYSTEM',
                timestamp: firebase.firestore.FieldValue.serverTimestamp(),
                isReadByBuyer: true,
                isReadBySeller: true
            };
            
            // Firestore에 메시지 추가
            const docRef = await this.messagesRef.add(message);
            
            // Spring Boot에 마지막 메시지 정보 업데이트
            this.updateLastMessageInDB(content);
            
            console.log('시스템 메시지 전송 완료:', docRef.id);
            
            return docRef.id;
        } catch (error) {
            console.error('시스템 메시지 전송 실패:', error);
            throw error;
        }
    }
    
    /**
     * 거래 상태 변경 시스템 메시지 처리
     */
    handleStatusChangeMessage(content) {
        try {
            console.log('거래 상태 변경 메시지 처리:', content);
            
            // 메시지에서 상태 코드 추출: "[STATUS:RESERVED]"
            const statusMatch = content.match(/\[STATUS:([A-Z]+)\]/);
            if (statusMatch && statusMatch[1]) {
                const newStatus = statusMatch[1];
                console.log('상태 변경 감지:', newStatus);
                
                // 전역 함수가 있으면 UI 업데이트
                if (typeof window.updatePostStatusBadge === 'function') {
                    window.updatePostStatusBadge(newStatus);
                    console.log('상태 배지 업데이트 완료');
                }
                
                if (typeof window.updateStatusDropdown === 'function') {
                    window.updateStatusDropdown(newStatus);
                    console.log('드롭다운 메뉴 업데이트 완료');
                }
            } else {
                console.warn('상태 변경 메시지에서 상태를 추출할 수 없음:', content);
            }
        } catch (error) {
            console.error('거래 상태 변경 메시지 처리 실패:', error);
        }
    }
    
    /**
     * 채팅방 나가기 시스템 메시지 처리
     */
    handleLeaveMessage(content) {
        try {
            console.log('채팅방 나가기 메시지 처리:', content);
            
            // 메시지에서 사용자 ID 추출: "[LEAVE:123]"
            const leaveMatch = content.match(/\[LEAVE:(\d+)\]/);
            if (leaveMatch && leaveMatch[1]) {
                const leftUserId = parseInt(leaveMatch[1]);
                console.log('나간 사용자 ID:', leftUserId);
                
                // 상대방이 나간 경우 (내가 아닌 다른 사용자)
                if (leftUserId !== this.currentUserId) {
                    console.log('상대방이 채팅방을 나감, UI 업데이트');
                    
                    // 페이지 새로고침하여 상대방 나간 상태 반영
                    setTimeout(() => {
                        console.log('상대방 나가기로 인한 페이지 새로고침');
                        location.reload();
                    }, 1000); // 시스템 메시지가 표시된 후 새로고침
                } else {
                    console.log('본인이 나간 메시지, UI 업데이트 안 함');
                }
            } else {
                console.warn('나가기 메시지에서 사용자 ID를 추출할 수 없음:', content);
            }
        } catch (error) {
            console.error('채팅방 나가기 메시지 처리 실패:', error);
        }
    }
    
    /**
     * 빈 상태 표시
     */
    displayEmptyState() {
        const messagesContainer = document.getElementById('messagesContainer');
        if (!messagesContainer) return;
        
        messagesContainer.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="bi bi-chat-dots fs-1 mb-3 d-block"></i>
                <p>채팅을 시작해보세요!</p>
            </div>
        `;
    }
    
    /**
     * 메시지 목록 표시
     */
    displayMessages(messages) {
        const messagesContainer = document.getElementById('messagesContainer');
        if (!messagesContainer) return;
        
        let html = '';
        
        messages.forEach((message, index) => {
            // 시스템 메시지 처리
            if (message.type === 'SYSTEM') {
                // 상태 코드 및 나가기 코드 부분 제거하여 표시
                let displayContent = message.content;
                if (displayContent.includes('[STATUS:')) {
                    displayContent = displayContent.replace(/\s*\[STATUS:[A-Z]+\]/, '');
                }
                if (displayContent.includes('[LEAVE:')) {
                    displayContent = displayContent.replace(/\s*\[LEAVE:\d+\]/, '');
                }
                
                html += `
                    <div class="text-center my-3">
                        <div class="d-inline-block px-3 py-2 bg-light rounded-pill">
                            <small class="text-muted">
                                <i class="bi bi-info-circle me-1"></i>
                                ${this.escapeHtml(displayContent)}
                            </small>
                        </div>
                    </div>
                `;
                return;
            }
            
            const isMyMessage = message.senderId === this.currentUserId;
            const messageClass = isMyMessage ? 'message-sent' : 'message-received';
            const alignClass = isMyMessage ? 'text-end' : 'text-start';
            
            // 시간 포맷팅
            const timeString = this.formatTime(message.timestamp);
            
            // 메시지 타입별 내용
            let messageContent = '';
            if (message.type === 'IMAGE') {
                messageContent = `<img src="${message.imageUrl}" class="message-image" alt="이미지 메시지" onclick="showImageModal('${message.imageUrl}')">`;
            } else {
                messageContent = this.escapeHtml(message.content);
            }
            
            // 읽음 상태 확인
            let readStatus = '';
            if (isMyMessage) {
                // 내가 보낸 메시지의 경우, 상대방이 읽었는지 확인
                const isReadByOther = this.isBuyer ? message.isReadBySeller : message.isReadByBuyer;
                
                // 읽음 상태 표시 (시스템 메시지는 제외)
                if (message.type !== 'SYSTEM') {
                    if (isReadByOther) {
                        readStatus = ' • 읽음';
                    } else {
                        // 읽지 않은 경우 숫자 1 표시
                        readStatus = ' • <span class="unread-count">1</span>';
                    }
                }
            }
            
            html += `
                <div class="message-wrapper ${alignClass} mb-3">
                    <div class="message ${messageClass}">
                        <div class="message-content">
                            ${messageContent}
                        </div>
                        <div class="message-info">
                            <small class="text-muted">
                                ${!isMyMessage ? message.senderName + ' • ' : ''}${timeString}${readStatus}
                            </small>
                        </div>
                    </div>
                </div>
            `;
        });
        
        messagesContainer.innerHTML = html;
    }
    
    /**
     * 읽지 않은 메시지를 읽음으로 처리
     */
    async markMessagesAsRead(messages) {
        const unreadMessages = messages.filter(msg => {
            if (this.isBuyer) {
                return msg.senderId !== this.currentUserId && !msg.isReadByBuyer;
            } else {
                return msg.senderId !== this.currentUserId && !msg.isReadBySeller;
            }
        });
        
        if (unreadMessages.length === 0) return;
        
        const batch = this.db.batch();
        
        unreadMessages.forEach(msg => {
            const msgRef = this.messagesRef.doc(msg.messageId);
            const updateField = this.isBuyer ? 'isReadByBuyer' : 'isReadBySeller';
            batch.update(msgRef, { [updateField]: true });
        });
        
        try {
            await batch.commit();
            
            // Spring Boot에 읽지 않은 메시지 수 업데이트
            this.updateUnreadCountInDB(0);
            
            // 채팅 알림 매니저의 읽음 처리는 이미 채팅방 진입 시 clearChatRoomUnread()에서 처리됨
            // 중복 호출 제거
            
            // 글로벌 채팅 리스너에게 읽은 메시지 ID들 전달 (중복 알림 방지)
            if (typeof window.globalChatListener !== 'undefined' && window.globalChatListener !== null) {
                unreadMessages.forEach(msg => {
                    window.globalChatListener.markMessageAsProcessed(msg.messageId);
                });
                console.log('글로벌 리스너에 읽은 메시지 ID 전달:', unreadMessages.length, '개');
            }
            
            console.log(`${unreadMessages.length}개 메시지를 읽음 처리했습니다.`);
        } catch (error) {
            console.error('읽음 처리 실패:', error);
        }
    }
    
    /**
     * Spring Boot에 마지막 메시지 정보 업데이트
     */
    async updateLastMessageInDB(lastMessage) {
        try {
            const timestamp = new Date().toISOString();
            
            await $.ajax({
                url: `/api/chat/rooms/${this.firebaseRoomId}/last-message`,
                method: 'PUT',
                data: {
                    lastMessage: lastMessage,
                    timestamp: timestamp
                }
            });
        } catch (error) {
            console.error('마지막 메시지 업데이트 실패:', error);
        }
    }
    
    /**
     * Spring Boot에 읽지 않은 메시지 수 업데이트
     */
    async updateUnreadCountInDB(unreadCount) {
        try {
            await $.ajax({
                url: `/api/chat/rooms/${this.firebaseRoomId}/unread-count`,
                method: 'PUT',
                data: {
                    unreadCount: unreadCount
                }
            });
        } catch (error) {
            console.error('읽지 않은 메시지 수 업데이트 실패:', error);
        }
    }
    
    /**
     * 상대방의 읽지 않은 메시지 수 증가
     */
    async incrementOtherUserUnreadCount(currentMessage) {
        try {
            // 상대방의 읽지 않은 메시지 수를 증가시키기 위한 API 호출
            // 현재 전송한 메시지 내용을 함께 전달하여 정확한 알림 생성
            await $.ajax({
                url: `/api/chat/rooms/${this.firebaseRoomId}/increment-unread`,
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    currentMessage: currentMessage || '새 메시지'
                })
            });
        } catch (error) {
            console.error('상대방 읽지 않은 메시지 수 증가 실패:', error);
        }
    }
    
    /**
     * 채팅 알림 전송
     */
    async sendChatNotification(message) {
        if (!this.otherUserId || !this.chatRoomId) {
            console.log('채팅 알림을 위한 정보가 부족합니다:', {
                otherUserId: this.otherUserId,
                chatRoomId: this.chatRoomId
            });
            return;
        }
        
        try {
            await $.ajax({
                url: '/api/chat/notify',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    recipientId: this.otherUserId,
                    senderId: this.currentUserId,
                    senderName: this.currentUserName,
                    chatRoomId: this.chatRoomId,
                    message: message
                })
            });
            console.log('채팅 알림 전송 완료');
        } catch (error) {
            console.error('채팅 알림 전송 실패:', error);
        }
    }
    
    /**
     * 채팅 창 맨 아래로 스크롤
     */
    scrollToBottom() {
        const container = document.getElementById('messagesContainer');
        if (container) {
            setTimeout(() => {
                container.scrollTop = container.scrollHeight;
            }, 100);
        }
    }
    
    /**
     * 시간 포맷팅
     */
    formatTime(date) {
        if (!date) return '';
        
        const now = new Date();
        const messageDate = new Date(date);
        
        // 오늘인지 확인
        const isToday = now.toDateString() === messageDate.toDateString();
        
        if (isToday) {
            return messageDate.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: true
            });
        } else {
            return messageDate.toLocaleDateString('ko-KR', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                hour12: true
            });
        }
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
     * 채팅 정리
     */
    cleanup() {
        if (this.unsubscribe) {
            this.unsubscribe();
            this.unsubscribe = null;
        }
    }
}

/**
 * 채팅 초기화 (채팅방 페이지에서 호출)
 */
function initializeChat(firebaseRoomId, currentUserId, currentUserName, isBuyer, otherUserId, otherUserName, chatRoomId) {
    try {
        // Firebase 초기화 확인
        if (!initializeFirebase()) {
            throw new Error('Firebase 초기화 실패');
        }
        
        // 기존 채팅 매니저 정리
        if (chatManager) {
            chatManager.cleanup();
        }
        
        console.log('initializeChat 호출:', {
            firebaseRoomId, currentUserId, currentUserName, isBuyer, otherUserId, otherUserName, chatRoomId
        });
        
        // 새 채팅 매니저 생성 (모든 매개변수를 생성자에 전달)
        chatManager = new FirebaseChatManager(firebaseRoomId, currentUserId, currentUserName, isBuyer, otherUserId, otherUserName, chatRoomId);
        
        return chatManager;
    } catch (error) {
        console.error('채팅 초기화 실패:', error);
        alert('채팅을 불러올 수 없습니다. 페이지를 새로고침해주세요.');
        return null;
    }
}

/**
 * 메시지 전송 (폼에서 호출)
 */
async function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content) return;
    
    if (!chatManager) {
        alert('채팅이 초기화되지 않았습니다.');
        return;
    }
    
    try {
        // 버튼 비활성화
        const sendBtn = document.getElementById('sendBtn');
        if (sendBtn) sendBtn.disabled = true;
        
        await chatManager.sendMessage(content);
        
        // 입력창 초기화
        input.value = '';
        input.focus();
        
    } catch (error) {
        console.error('메시지 전송 실패:', error);
        alert('메시지 전송에 실패했습니다.');
    } finally {
        // 버튼 활성화
        const sendBtn = document.getElementById('sendBtn');
        if (sendBtn) sendBtn.disabled = false;
    }
}

/**
 * 이미지 모달 표시
 */
function showImageModal(imageUrl) {
    document.getElementById('modalImage').src = imageUrl;
    const modal = new bootstrap.Modal(document.getElementById('imageModal'));
    modal.show();
}

/**
 * 페이지 언로드 시 정리
 */
window.addEventListener('beforeunload', function() {
    if (chatManager) {
        chatManager.cleanup();
    }
});