# Unibook 채팅 알림 시스템 구현 가이드

## 📌 개요
Unibook 프로젝트의 실시간 채팅 알림 시스템 구현 내역과 트러블슈팅 과정을 정리한 문서입니다.
이 시스템은 기존 일반 알림과 완전히 분리된 채팅 전용 알림 시스템으로 구현되었습니다.

## 🎯 핵심 설계 원칙
1. **채팅 알림과 일반 알림의 완전한 분리**
   - 채팅 알림은 헤더의 채팅 아이콘에만 표시
   - 일반 알림(벨 아이콘)과 독립적으로 작동
   
2. **Firebase 실시간 리스너 사용**
   - 10초 폴링 방식에서 Firebase 실시간 리스너로 전환
   - 모든 페이지에서 실시간 채팅 알림 수신 가능

3. **중복 알림 방지**
   - 타임스탬프 기반 추적으로 이미 본 메시지 재알림 방지
   - 채팅방 진입/이탈 시 정확한 읽음 상태 관리

## 🏗️ 시스템 아키텍처

### 1. Frontend 구성요소

#### chat-notification.js
- **역할**: 채팅 알림 UI 관리 (헤더 배지, 토스트)
- **주요 기능**:
  - 전체 읽지 않은 채팅 수 관리
  - 채팅방별 읽지 않은 메시지 수 추적
  - 헤더 채팅 배지 업데이트
  - 채팅 토스트 알림 표시

#### firebase-global-chat-listener.js
- **역할**: 모든 페이지에서 실시간 채팅 메시지 감지
- **주요 기능**:
  - 사용자가 참여한 모든 채팅방 모니터링
  - 새 메시지 감지 시 chat-notification.js에 알림
  - 타임스탬프 기반 중복 알림 방지
  - 채팅방 진입/이탈 시간 추적

#### firebase-chat.js
- **역할**: 채팅방 내부 메시지 관리
- **주요 기능**:
  - 메시지 송수신
  - 읽음 상태 업데이트
  - Firebase Firestore와 실시간 동기화

#### chat-list.js
- **역할**: 채팅 목록 페이지 실시간 업데이트
- **주요 기능**:
  - 채팅방별 최신 메시지 실시간 표시
  - 읽지 않은 메시지 배지 업데이트
  - 채팅방 카드 애니메이션 효과

### 2. Backend 구성요소

#### ChatApiController
- `/api/chat/unread-count`: 전체 읽지 않은 메시지 수
- `/api/chat/rooms`: 채팅방 목록 (읽지 않은 수 포함)
- `/api/chat/rooms/{chatRoomId}/unread-count`: 특정 채팅방 읽지 않은 수

#### ChatService
- `getTotalUnreadCount()`: 전체 읽지 않은 메시지 수 계산
- `getChatRoomUnreadCount()`: 특정 채팅방 읽지 않은 수 조회

## 🔧 구현 세부사항

### 1. 읽지 않은 메시지 수 관리

```javascript
// chat-notification.js
class ChatNotificationManager {
    constructor() {
        this.unreadChats = new Map(); // chatRoomId -> unreadCount
        this.totalUnreadCount = 0;
    }
    
    async clearChatRoomUnread(chatRoomId) {
        // 서버에서 정확한 읽지 않은 수 조회
        const response = await $.get(`/api/chat/rooms/${chatRoomId}/unread-count`);
        const unreadCount = response.data;
        
        // 전체 카운트에서 차감
        this.totalUnreadCount = Math.max(0, this.totalUnreadCount - unreadCount);
        this.updateChatBadge(this.totalUnreadCount);
    }
}
```

### 2. 중복 알림 방지 메커니즘

```javascript
// firebase-global-chat-listener.js
class GlobalChatListener {
    constructor(currentUserId) {
        this.lastSeenTimestamps = new Map(); // 채팅방별 마지막 확인 시간
    }
    
    setupSimpleIndividualListener(firebaseRoomId, chatRoomId) {
        // 현재 시점을 기준으로 설정
        const startTime = firebase.firestore.Timestamp.now();
        this.lastSeenTimestamps.set(chatRoomId, startTime.toDate());
        
        // 이후 메시지만 알림 대상으로 처리
    }
}
```

### 3. Firebase 실시간 리스너 설정

```javascript
// 각 채팅방의 최신 메시지 1개만 모니터링
const messagesRef = this.db.collection('chatrooms')
    .doc(firebaseRoomId)
    .collection('messages')
    .orderBy('timestamp', 'desc')
    .limit(1);
```

## 🐛 주요 트러블슈팅

### 1. 중복 알림 문제
**문제**: 채팅방에서 이미 본 메시지가 채팅방 이탈 시 다시 알림
**원인**: `isFirstLoad` 플래그만으로는 부족
**해결**: 타임스탬프 기반 추적 시스템 구현
- 리스너 초기화 시 현재 시간을 기준점으로 설정
- 메시지 수신 시 기준 시간과 비교
- 내가 보낸 메시지도 기준 시간 업데이트

### 2. 읽지 않은 메시지 수 부정확 문제
**문제**: 채팅방 진입 시 헤더 배지가 1씩만 감소
**원인**: 로컬 Map이 비어있어서 정확한 수를 모름
**해결**: 채팅방 진입 시 서버에서 정확한 수 조회
```javascript
// clearChatRoomUnread 메서드 개선
const response = await $.get(`/api/chat/rooms/${chatRoomId}/unread-count`);
```

### 3. Firebase collectionGroup 인덱스 오류
**문제**: `collectionGroup('messages')` 사용 시 인덱스 오류
**해결**: 개별 채팅방 리스너 방식으로 변경

### 4. 사용자 메타 태그 누락
**문제**: 일부 페이지에서 user-id 메타 태그 없음
**해결**: 모든 주요 템플릿에 user-meta fragment 추가

## 📝 페이지별 동작 흐름

### 1. 메인 페이지에서 메시지 수신
1. Firebase 글로벌 리스너가 새 메시지 감지
2. `sendGlobalNotification()` 호출
3. `chatNotificationManager.onNewMessage()` 호출
4. 헤더 배지 업데이트 + 토스트 표시

### 2. 채팅방 진입 시
1. `clearChatRoomUnread(chatRoomId)` 호출
2. 서버에서 정확한 읽지 않은 수 조회
3. 헤더 배지 한 번에 업데이트 (중간 단계 없이)
4. Firebase에서 메시지 읽음 처리

### 3. 채팅 목록 페이지
1. 초기 로드 시 `loadChatRoomUnreadCounts()` 호출
2. 각 채팅방별 실시간 리스너 설정
3. 새 메시지 시 채팅방 카드 실시간 업데이트

## 🚀 성능 최적화

1. **리스너 최적화**
   - 각 채팅방당 최신 메시지 1개만 모니터링
   - `limit(1)`로 데이터 전송량 최소화

2. **중복 처리 제거**
   - `onMessageRead()` 호출 제거로 불필요한 업데이트 방지
   - 채팅방 진입 시 한 번만 읽음 처리

3. **메모리 관리**
   - 페이지 언로드 시 모든 리스너 정리
   - `cleanup()` 메서드로 메모리 누수 방지

## ⚠️ 주의사항

1. **Firebase 초기화 순서**
   - firebase-config.js가 먼저 로드되어야 함
   - 2초 대기 후 글로벌 리스너 초기화

2. **채팅방 ID vs Firebase Room ID**
   - chatRoomId: Spring Boot DB의 ID
   - firebaseRoomId: Firebase Firestore의 문서 ID
   - 항상 구분해서 사용

3. **읽음 상태 필드**
   - `isReadByBuyer`: 구매자가 읽었는지
   - `isReadBySeller`: 판매자가 읽었는지
   - 사용자 역할에 따라 적절한 필드 업데이트

## 🔍 디버깅 팁

1. **콘솔 로그 확인 포인트**
   - "ChatNotificationManager 초기화" 
   - "GlobalChatListener 생성"
   - "채팅방 기준 시간 설정/업데이트"
   - "채팅 배지 업데이트"

2. **일반적인 문제 해결**
   - 알림이 안 올 때: 글로벌 리스너 초기화 확인
   - 중복 알림: lastSeenTimestamps 확인
   - 배지 수 부정확: 서버 API 응답 확인

## 📅 구현 일자
- 2025년 6월 1일: 채팅 알림 시스템 완전 분리 및 실시간 구현 완료