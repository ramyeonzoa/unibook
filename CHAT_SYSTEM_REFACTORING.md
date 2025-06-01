# 채팅 시스템 리팩터링 가이드

## 📌 리팩터링 배경
채팅 알림 시스템 구현 과정에서 발생한 문제점들:
- 읽지 않은 메시지 카운트가 여러 곳에서 관리되어 불일치 발생
- 채팅방 진입 시 카운트가 정확하지 않게 감소 (9→7→0)
- 간헐적으로 알림이 도착하지 않는 문제
- 중복 리스너로 인한 성능 저하

## 🎯 핵심 개선 사항

### 1. Single Source of Truth 원칙 적용
**이전**: 서버 DB, 로컬 Map, Firebase에서 각각 카운트 관리
**이후**: 서버 DB만을 신뢰하는 단일 소스 체계

```javascript
// chat-notification.js
class ChatNotificationManager {
    constructor() {
        // 로컬 Map 제거, 서버 중심 관리
        this.totalUnreadCount = 0;
    }
    
    async clearChatRoomUnread(chatRoomId) {
        // 개별 채팅방 카운트 대신 전체 카운트 재조회
        const response = await $.get('/api/chat/unread-count');
        this.totalUnreadCount = response.data || 0;
        this.updateChatBadge(this.totalUnreadCount);
    }
}
```

### 2. 초기화 로직 단순화
**이전**: `/chat` 페이지에서만 특별한 초기화 수행
**이후**: 모든 페이지에서 동일한 초기화

```javascript
// 이전
if (window.location.pathname === '/chat') {
    await this.loadChatRoomUnreadCounts();
}

// 이후 - 제거됨
```

### 3. 채팅방 진입 시 처리 순서 최적화
**이전**: 여러 비동기 작업이 경쟁하며 중간 상태 발생
**이후**: 명확한 순서와 타이밍

```javascript
// chat/room.html
// 1. 글로벌 리스너에 채팅방 진입 알림 (중복 방지)
window.globalChatListener.notifyCurrentChatRoomChanged(chatRoomId);

// 2. Firebase 채팅 초기화
initializeChat(...);

// 3. 읽음 처리 완료 후 서버 동기화 (500ms 지연)
setTimeout(async () => {
    await window.chatNotificationManager.clearChatRoomUnread(chatRoomId);
}, 500);
```

### 4. 리스너 역할 분리
**이전**: 여러 리스너가 동일한 작업 수행
**이후**: 명확한 역할 분담

- **글로벌 리스너**: 메시지 감지 → 알림 발생
- **채팅 목록 리스너**: UI 업데이트만 담당
- **채팅방 내부 리스너**: 메시지 표시 및 읽음 처리

### 5. 중복 메서드 제거
```javascript
// chat-list.js
async updateTotalUnreadCount() {
    // @deprecated - chatNotificationManager가 관리
    return;
}

// firebase-chat.js
// onMessageRead() 호출 제거 - 중복 처리 방지
```

## 🔍 문제 해결 과정

### 문제 1: 채팅방 진입 시 카운트가 단계적으로 감소 (9→7→0)
**원인**: 여러 비동기 작업이 동시에 카운트를 업데이트
**해결**: 
- 채팅방 진입 시 개별 카운트 계산 대신 전체 카운트 재조회
- 타이밍 조정으로 Firebase 읽음 처리 완료 후 서버 동기화

### 문제 2: `/chat` 페이지에서 카운트 부정확
**원인**: `loadChatRoomUnreadCounts()`가 기존 totalCount를 덮어씀
**해결**: 해당 메서드 제거, 모든 페이지에서 동일한 초기화

### 문제 3: 간헐적 알림 미수신
**원인**: 리스너 초기화 타이밍 이슈
**해결**: Firebase 초기화 후 2초 대기, 안정적인 리스너 설정

## 📊 성능 개선 효과
- 불필요한 API 호출 감소
- 중복 리스너 제거로 메모리 사용량 감소
- 더 일관된 사용자 경험

## ⚠️ 주의사항
1. 서버 응답을 항상 신뢰 (로컬 캐시는 보조용)
2. 비동기 작업 순서 준수
3. Firebase 읽음 처리와 서버 동기화 타이밍 유지

## 🚀 향후 개선 방향
1. WebSocket을 통한 실시간 카운트 동기화
2. 읽음 처리 최적화 (배치 처리)
3. 오프라인 지원 추가