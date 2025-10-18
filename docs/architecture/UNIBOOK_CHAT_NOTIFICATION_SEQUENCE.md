# Unibook 채팅 및 알림 시스템 Sequence Diagram

## 📊 시스템 개요
Unibook의 채팅 시스템은 Firebase Firestore를 통한 실시간 메시징과 Spring Boot SSE를 통한 알림을 결합한 하이브리드 아키텍처입니다. 이 문서는 메시지 전송부터 알림 수신까지의 전체 흐름을 상세히 도식화합니다.

## 🔄 주요 시나리오

### 1. 메시지 전송 및 알림 흐름

```mermaid
sequenceDiagram
    participant SB as 발신자 브라우저
    participant RB as 수신자 브라우저
    participant FS as Firebase Firestore
    participant API as Spring Boot API
    participant CS as ChatService
    participant NS as NotificationService
    participant NES as NotificationEmitterService
    participant DB as MySQL Database
    participant SSE as SSE Connection

    %% 스타일 정의
    rect
        Note over SB,SSE: 메시지 전송 및 알림 프로세스
    end

    %% 1. 메시지 전송
    SB->>SB: sendMessage() 호출
    activate SB
    
    %% Firebase 저장
    SB->>FS: add({text, senderId, timestamp, type})
    activate FS
    FS-->>SB: DocumentReference (messageId)
    deactivate FS
    
    %% 서버 알림 (병렬 처리)
    SB->>API: POST /api/chat/rooms/{roomId}/increment-unread
    activate API
    API->>CS: incrementOtherUserUnreadCount(roomId, senderId)
    activate CS
    
    %% 읽지 않음 카운트 업데이트
    CS->>DB: UPDATE ChatRoom SET buyerUnreadCount++
    activate DB
    DB-->>CS: 업데이트 완료
    deactivate DB
    
    %% 비동기 알림 생성
    CS->>NS: createChatNotificationAsync(receiverId, roomId, message)
    activate NS
    Note right of NS: @Async 처리
    CS-->>API: ResponseEntity.ok()
    deactivate CS
    API-->>SB: 200 OK
    deactivate API
    deactivate SB
    
    %% 알림 처리 (비동기)
    NS->>DB: INSERT Notification (type=NEW_MESSAGE)
    activate DB
    DB-->>NS: Notification Entity
    deactivate DB
    
    NS->>NES: sendNotificationToUser(receiverId, notificationDto)
    activate NES
    
    %% SSE 전송
    NES->>SSE: eventBuilder.data(notificationDto)
    activate SSE
    SSE->>RB: EventSource.onmessage (NEW_MESSAGE)
    activate RB
    
    %% 수신자 UI 업데이트
    RB->>RB: updateChatBadge(+1)
    RB->>RB: showToastNotification()
    RB->>RB: updateChatListIfVisible()
    deactivate RB
    deactivate SSE
    deactivate NES
    deactivate NS
    
    %% Firebase 실시간 동기화 (독립적)
    FS->>RB: onSnapshot() 트리거
    activate RB
    RB->>RB: appendMessageToChat()
    RB->>RB: updateReadStatus("1")
    deactivate RB
```

### 2. 채팅방 입장 및 읽음 상태 동기화

```mermaid
sequenceDiagram
    participant U as 사용자 브라우저
    participant API as Spring Boot API
    participant CS as ChatService
    participant NS as NotificationService
    participant DB as MySQL Database
    participant FS as Firebase Firestore
    participant SSE as SSE Connection

    %% 스타일 정의
    rect rgb(255, 248, 240)
        Note over U,SSE: 👀 채팅방 입장 및 읽음 상태 동기화
    end

    %% 채팅방 페이지 로드
    U->>API: GET /chat/room/{roomId}
    activate API
    API->>CS: getChatRoomForUser(roomId, userId)
    activate CS
    CS->>DB: SELECT ChatRoom WHERE id = roomId
    activate DB
    DB-->>CS: ChatRoom Entity
    deactivate DB
    CS-->>API: ChatRoom DTO
    deactivate CS
    API-->>U: chat/room.html 렌더링
    deactivate API

    %% Firebase 초기화 및 메시지 로드
    U->>FS: onSnapshot(messages.orderBy('timestamp'))
    activate FS
    FS-->>U: 기존 메시지 목록
    U->>U: renderMessages()
    
    %% 읽지 않은 메시지 일괄 읽음 처리
    U->>U: markAllMessagesAsRead()
    U->>FS: batch.update({isReadBySeller: true})
    FS-->>U: 배치 업데이트 완료
    deactivate FS

    %% 서버 읽음 상태 동기화
    U->>API: POST /api/chat/rooms/{roomId}/mark-read
    activate API
    API->>CS: markMessagesAsRead(roomId, userId)
    activate CS
    
    %% DB 읽지 않음 카운트 리셋
    CS->>DB: UPDATE ChatRoom SET sellerUnreadCount = 0
    activate DB
    DB-->>CS: 업데이트 완료
    deactivate DB
    
    %% 알림 읽음 처리
    CS->>NS: markChatNotificationsAsRead(userId, roomId)
    activate NS
    NS->>DB: UPDATE Notification SET isRead = true WHERE chatRoomId = roomId
    activate DB
    DB-->>NS: 업데이트 완료
    deactivate DB
    NS-->>CS: 처리 완료
    deactivate NS
    
    CS-->>API: ResponseEntity.ok()
    deactivate CS
    API-->>U: 200 OK
    deactivate API

    %% SSE로 배지 업데이트
    NS->>SSE: 알림 카운트 업데이트 이벤트
    SSE->>U: EventSource.onmessage (BADGE_UPDATE)
    U->>U: updateHeaderBadge(-unreadCount)
```

### 3. 오프라인 사용자 알림 처리

```mermaid
sequenceDiagram
    participant S as 발신자
    participant API as Spring Boot API
    participant NS as NotificationService
    participant DB as MySQL Database
    participant R as 수신자 (오프라인→온라인)
    participant SSE as SSE Connection

    %% 스타일 정의
    rect rgb(255, 240, 245)
        Note over S,SSE: 📴 오프라인 사용자 알림 처리
    end

    %% 오프라인 상태에서 메시지 전송
    S->>API: 채팅 메시지 전송
    API->>NS: createChatNotificationAsync()
    activate NS
    
    NS->>DB: INSERT Notification (isRead=false)
    activate DB
    DB-->>NS: Notification 저장 완료
    deactivate DB
    
    NS->>NS: checkUserOnline(receiverId)
    Note right of NS: SSE 연결 없음 확인
    NS-->>API: 알림 저장만 완료
    deactivate NS

    %% 수신자 온라인 전환
    R->>API: GET /api/notifications/stream
    activate API
    API->>SSE: SseEmitter 생성 및 저장
    activate SSE
    
    %% 읽지 않은 알림 전송
    API->>NS: getUnreadNotifications(userId)
    activate NS
    NS->>DB: SELECT * FROM Notification WHERE userId = ? AND isRead = false
    activate DB
    DB-->>NS: List<Notification>
    deactivate DB
    
    NS->>SSE: 미읽 알림 일괄 전송
    SSE->>R: EventSource.onmessage (BULK_NOTIFICATIONS)
    activate R
    R->>R: processBulkNotifications()
    R->>R: updateAllBadges()
    R->>R: showMissedMessageIndicators()
    deactivate R
    
    deactivate NS
    deactivate SSE
    deactivate API
```

### 4. 실시간 메시지 수신 흐름 (Firebase)

```mermaid
sequenceDiagram
    participant S as 발신자
    participant FS as Firebase Firestore
    participant R1 as 수신자 (채팅방 내)
    participant R2 as 수신자 (채팅방 밖)

    %% 스타일 정의
    rect rgb(240, 255, 240)
        Note over S,R2: 🔥 Firebase 실시간 동기화
    end

    %% Firestore 메시지 저장
    S->>FS: messages.add(messageData)
    activate FS
    
    %% onSnapshot 트리거 (채팅방 내 사용자)
    FS->>R1: onSnapshot 이벤트 (docChanges)
    activate R1
    R1->>R1: if (change.type === 'added')
    R1->>R1: appendMessage(newMessage)
    R1->>R1: scrollToBottom()
    R1->>R1: playMessageSound()
    deactivate R1
    
    %% onSnapshot 트리거 (채팅방 밖 사용자)
    FS->>R2: onSnapshot 이벤트 (채팅 목록 페이지)
    activate R2
    R2->>R2: updateLastMessage(roomId, message)
    R2->>R2: reorderChatList()
    R2->>R2: highlightUnreadRoom()
    deactivate R2
    
    deactivate FS
```

## 🔧 기술적 세부사항

### 비동기 처리 구조
```
@Async("notificationTaskExecutor")
public void createChatNotificationAsync(Long receiverId, String roomId, String message) {
    // 1. Notification 엔티티 생성 및 저장
    // 2. NotificationDto 변환
    // 3. SSE를 통한 실시간 전송
}
```

### 3중 읽음 상태 관리
1. **Firebase Level**: `isReadByBuyer`, `isReadBySeller` (메시지별)
2. **Database Level**: `buyerUnreadCount`, `sellerUnreadCount` (채팅방별)
3. **Notification Level**: `isRead` (알림별)

### SSE 연결 관리
- 연결 타임아웃: 180초
- 재연결 메커니즘: exponential backoff (1s → 2s → 4s → 8s)
- 연결당 메모리: ~1KB
- 동시 연결 제한: 사용자당 최대 3개

### 성능 최적화
- Firebase batch update: 최대 500개 문서 동시 업데이트
- SSE 버퍼링: 100ms 디바운싱으로 알림 그룹화
- 알림 페이징: 최근 50개만 초기 로드
- 메시지 lazy loading: 스크롤 기반 추가 로드

## 📌 주요 엔드포인트

### Chat API
- `POST /api/chat/rooms/{roomId}/increment-unread` - 읽지 않음 카운트 증가
- `POST /api/chat/rooms/{roomId}/mark-read` - 메시지 읽음 처리
- `GET /api/chat/rooms` - 채팅방 목록 조회

### Notification API
- `GET /api/notifications/stream` - SSE 스트림 연결
- `GET /api/notifications/unread-count` - 읽지 않은 알림 수
- `POST /api/notifications/{id}/read` - 알림 읽음 처리

## 🎯 핵심 특징

1. **하이브리드 아키텍처**: Firebase (채팅) + SSE (알림) 결합
2. **실시간 동기화**: 메시지와 알림의 즉각적인 전달
3. **오프라인 지원**: 미읽 알림 저장 및 재전송
4. **확장성**: 비동기 처리와 효율적인 연결 관리
5. **일관성**: 3중 읽음 상태 관리로 데이터 정합성 보장