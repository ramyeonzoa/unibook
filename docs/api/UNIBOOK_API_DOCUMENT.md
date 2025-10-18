# Unibook API Documentation
* Unibook 프로젝트의 모든 API 엔드포인트를 기능별로 분류하여 정리한 문서
* 총 **12개 기능 그룹**, **81개의 API 엔드포인트**
* Last updated: Jun. 11. 2025.
* Author: 컴퓨터시스템공학과(심화) 202546016 최민혁

## Auth Types

| Level | Description | Implementation |
|-------|-------------|----------------|
| **Public** | 인증 불필요 | 누구나 접근 가능 |
| **User** | 로그인 필요 | `@PreAuthorize("isAuthenticated()")` |
| **Admin** | 관리자 권한 | `@PreAuthorize("hasRole('ADMIN')")` |

## API Groups Overview

| Group | Endpoints | Auth Types | Main Purpose |
|-------|-----------|------------|--------------|
| [Authentication & User](#1-authentication--user-management-apis) | 4 | Public, User | 사용자 인증 및 관리 |
| [Post Management](#2-post-management-apis) | 4 | User, Admin | 게시글 CRUD 및 상태 관리 |
| [Book Management](#3-book-management-apis) | 2 | Public | Naver API 연동 도서 검색 |
| [School & Department](#4-school--department-apis) | 4 | Public | 학교/학과 검색 및 조회 |
| [Professor & Subject](#5-professor--subject-apis) | 11 | Public, User | 교수/과목 검색 및 관리 |
| [Wishlist](#6-wishlist-apis) | 3 | User, Public | 찜하기 기능 |
| [Chat System](#7-chat-system-apis) | 16 | User, Public | Firebase 실시간 채팅 |
| [Notifications](#8-notification-system-apis) | 7 | User | SSE 실시간 알림 |
| [Reports](#9-report-system-apis) | 8 | User, Admin | 신고 시스템 |
| [Keyword Alerts](#10-keyword-alert-apis) | 4 | User | 키워드 알림 구독 |
| [Admin Actions](#11-admin-action-apis) | 8 | Admin | 사용자/게시글 관리 |
| [Cache Management](#12-cache-management-apis) | 2 | Public | 성능 모니터링 |

---

## 1. Authentication & User Management APIs

### AuthController (`/api/auth/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/auth/check-email` | Public | 이메일 중복 확인 | **Params**: `email: String`<br/>**Response**: `boolean` |
| POST | `/api/auth/resend-verification` | User | 이메일 인증 재발송 | **Response**: `{message: String, email: String}` |

### UserApiController (`/api/users/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/users/me` | User | 현재 사용자 정보 조회 | **Response**: `{userId, name, email, department: {departmentId, departmentName}, school: {schoolId, schoolName}}` |
| GET | `/api/users/verification-status` | User | 이메일 인증 상태 확인 | **Response**: `{verified: boolean, email: String}` |

---

## 2. Post Management APIs

### PostController (`/posts/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| POST | `/posts/{id}/status` | User | 게시글 상태 변경 (AJAX) | **Body**: `status: String`<br/>**Response**: `{success: boolean, message: String, newStatus: String}` |
| PUT | `/posts/{id}/block` | Admin | 게시글 차단 | **Response**: `{success: boolean, message: String}` |
| PUT | `/posts/{id}/unblock` | Admin | 게시글 차단 해제 | **Response**: `{success: boolean, message: String}` |
| GET | `/posts/price-trend/{bookId}` | Public | 도서 가격 시세 조회 | **Response**: `PriceTrendDto.ChartData` |

---

## 3. Book Management APIs

### BookApiController (`/api/books/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/books/search` | Public | Naver API 도서 검색 | **Params**: `query: String, page: int=1, size: int=10`<br/>**Response**: `BookSearchDto.Response` |
| POST | `/api/books/select` | Public | 도서 선택/생성 | **Body**: `BookSearchDto.Item`<br/>**Response**: `{bookId: Long}` or `{error: -1}` |

---

## 4. School & Department APIs

### SchoolApiController (`/api/schools/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/schools/search` | Public | 학교 검색 (자동완성) | **Params**: `keyword: String, limit: int=10`<br/>**Response**: `[{id: Long, text: String, value: String}]` |
| GET | `/api/schools/popular` | Public | 인기 학교 목록 | **Response**: `[{id: Long, text: String, value: String}]` |

### DepartmentApiController (`/api/departments/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/departments/by-school/{schoolId}` | Public | 학교별 학과 목록 | **Response**: `[{id: Long, name: String}]` |
| GET | `/api/departments/search` | Public | 학과 검색 (자동완성) | **Params**: `query: String, limit: int=20`<br/>**Response**: `[{id, text, schoolId, schoolName, departmentName}]` |

---

## 5. Professor & Subject APIs

### ProfessorApiController (`/api/professors/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/professors/search` | Public | 교수 검색 | **Params**: `query: String, departmentId?: Long, page: int=1, size: int=10`<br/>**Response**: `PagedResponse<ProfessorDto>` |
| GET | `/api/professors/{professorId}` | Public | 교수 상세 정보 | **Response**: `ProfessorDto` or `404` |
| GET | `/api/professors/by-department/{departmentId}` | Public | 학과별 교수 목록 | **Params**: `page: int=1, size: int=10`<br/>**Response**: `PagedResponse<ProfessorDto>` |
| GET | `/api/professors/{professorId}/subjects` | Public | 교수별 과목 목록 | **Params**: `page: int=1, size: int=20`<br/>**Response**: `PagedResponse<SubjectDto>` |
| GET | `/api/professors/search/my-school` | User | 내 학교 교수 검색 | **Params**: `query: String, page: int=1, size: int=10`<br/>**Response**: `PagedResponse<ProfessorDto>` |

### SubjectApiController (`/api/subjects/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/subjects/search` | Public | 과목 검색 | **Params**: `query: String, professorId?: Long, page: int=1, size: int=10`<br/>**Response**: `PagedResponse<SubjectDto>` |
| GET | `/api/subjects/{subjectId}` | Public | 과목 상세 정보 | **Response**: `SubjectDto` or `404` |
| POST | `/api/subjects/select` | Public | 과목 선택/생성 | **Body**: `SubjectSelectionRequest`<br/>**Response**: `SubjectDto` (201 Created or 200 OK) |
| GET | `/api/subjects/search/my-school` | User | 내 학교 과목 검색 | **Params**: `query: String, page: int=1, size: int=10`<br/>**Response**: `PagedResponse<SubjectDto>` |
| POST | `/api/subjects/create-with-professor` | User | 교수와 함께 과목 생성 | **Body**: `SubjectWithProfessorRequest`<br/>**Response**: `SubjectDto` |

---

## 6. Wishlist APIs

### WishlistApiController (`/api/wishlist/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| POST | `/api/wishlist/toggle/{postId}` | User | 찜하기 토글 | **Response**: `{success: boolean, isWishlisted: boolean, message: String, needVerification?: boolean}` |
| GET | `/api/wishlist/check/{postId}` | User/Public | 찜 상태 확인 | **Response**: `{success: boolean, isWishlisted: boolean}` |
| DELETE | `/api/wishlist/{postId}` | User | 찜 해제 | **Response**: `{success: boolean, message: String}` |

---

## 7. Chat System APIs

### ChatApiController (`/api/chat/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| POST | `/api/chat/rooms` | User | 채팅방 생성/조회 | **Body**: `ChatDto.CreateChatRoomRequest`<br/>**Response**: `ApiResponse<ChatDto.ChatRoomDetailResponse>` |
| GET | `/api/chat/rooms` | User | 내 채팅방 목록 | **Response**: `ApiResponse<List<ChatDto.ChatRoomListResponse>>` |
| GET | `/api/chat/rooms/{chatRoomId}` | User | 채팅방 상세 정보 | **Response**: `ApiResponse<ChatDto.ChatRoomDetailResponse>` |
| GET | `/api/chat/rooms/firebase/{firebaseRoomId}` | User | Firebase ID로 채팅방 조회 | **Response**: `ApiResponse<ChatDto.ChatRoomDetailResponse>` |
| GET | `/api/chat/rooms/by-firebase-id/{firebaseRoomId}` | Public | Firebase ID로 기본 정보 조회 | **Response**: `ApiResponse<{chatRoomId, firebaseRoomId}>` |
| GET | `/api/chat/rooms/post/{postId}` | User | 게시글별 채팅방 목록 (판매자용) | **Response**: `ApiResponse<List<ChatDto.ChatRoomListResponse>>` |
| PUT | `/api/chat/rooms/{firebaseRoomId}/last-message` | Public | 마지막 메시지 업데이트 | **Params**: `lastMessage: String, timestamp: String`<br/>**Response**: `ApiResponse<Void>` |
| PUT | `/api/chat/rooms/{firebaseRoomId}/unread-count` | User | 읽지 않음 카운트 업데이트 | **Params**: `unreadCount: int`<br/>**Response**: `ApiResponse<Void>` |
| GET | `/api/chat/unread-count` | User | 전체 읽지 않음 카운트 | **Response**: `ApiResponse<Long>` |
| PUT | `/api/chat/rooms/{chatRoomId}/status` | User | 채팅방 상태 변경 | **Params**: `status: String`<br/>**Response**: `ApiResponse<Void>` |
| POST | `/api/chat/rooms/{firebaseRoomId}/increment-unread` | User | 상대방 읽지 않음 카운트 증가 | **Body**: `{currentMessage?: String}`<br/>**Response**: `ApiResponse<Void>` |
| DELETE | `/api/chat/rooms/{chatRoomId}` | User | 채팅방 삭제 | **Response**: `ApiResponse<Void>` |
| POST | `/api/chat/rooms/{chatRoomId}/leave` | User | 채팅방 나가기 | **Response**: `ApiResponse<Void>` |
| PUT | `/api/chat/rooms/{chatRoomId}/post-status` | User | 채팅방 내 게시글 상태 업데이트 | **Params**: `newStatus: String`<br/>**Response**: `ApiResponse<Void>` |
| POST | `/api/chat/notify` | User | 채팅 알림 전송 | **Body**: `ChatDto.ChatNotificationRequest`<br/>**Response**: `ApiResponse<Void>` |
| GET | `/api/chat/rooms/{chatRoomId}/unread-count` | User | 채팅방별 읽지 않음 카운트 | **Response**: `ApiResponse<Integer>` |

---

## 8. Notification System APIs

### NotificationApiController (`/api/notifications/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/notifications/stream` | User | SSE 실시간 알림 스트림 | **Response**: `text/event-stream` |
| GET | `/api/notifications` | User | 알림 목록 (페이징) | **Params**: `page: int=0, size: int=20`<br/>**Response**: `ApiResponse<Page<NotificationDto.Response>>` |
| GET | `/api/notifications/unread` | User | 읽지 않은 알림 목록 | **Params**: `limit: int=10`<br/>**Response**: `ApiResponse<Page<NotificationDto.Response>>` |
| GET | `/api/notifications/count` | User | 알림 카운트 정보 | **Response**: `ApiResponse<NotificationDto.CountResponse>` |
| POST | `/api/notifications/{notificationId}/read` | User | 알림 읽음 처리 | **Response**: `ApiResponse<Void>` |
| POST | `/api/notifications/read-all` | User | 모든 알림 읽음 처리 | **Response**: `ApiResponse<Integer>` |
| GET | `/api/notifications/connections` | User | SSE 연결 정보 (디버그용) | **Response**: `ApiResponse<Map<String, Object>>` |

---

## 9. Report System APIs

### ReportApiController (`/api/reports/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| POST | `/api/reports` | User | 신고 생성 | **Body**: `ReportDto.Request`<br/>**Response**: `ApiResponse<ReportDto.Response>` |
| GET | `/api/reports` | Admin | 신고 목록 (필터링) | **Params**: `status?: String, page: int=0, size: int=20`<br/>**Response**: `ApiResponse<Page<ReportDto.ListResponse>>` |
| GET | `/api/reports/{reportId}` | Admin | 신고 상세 정보 | **Response**: `ApiResponse<ReportDto.Response>` |
| PUT | `/api/reports/{reportId}/process` | Admin | 신고 처리 | **Body**: `ReportDto.ProcessRequest`<br/>**Response**: `ApiResponse<ReportDto.Response>` |
| GET | `/api/reports/my` | User | 내 신고 목록 | **Params**: `page: int=0, size: int=10`<br/>**Response**: `ApiResponse<Page<ReportDto.ListResponse>>` |
| GET | `/api/reports/categories` | Public | 신고 카테고리 목록 | **Response**: `ApiResponse<List<CategoryInfo>>` |
| GET | `/api/reports/pending/count` | Admin | 대기 중인 신고 수 | **Response**: `ApiResponse<Long>` |
| GET | `/api/reports/check` | User | 신고 가능 여부 확인 | **Params**: `reportType: String, targetId: Long`<br/>**Response**: `ApiResponse<ReportDto.CheckResponse>` |

---

## 10. Keyword Alert APIs

### KeywordAlertApiController (`/api/keyword-alerts/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| POST | `/api/keyword-alerts` | User | 키워드 알림 추가 | **Body**: `{keyword: String}`<br/>**Response**: `{success: boolean, message: String, keyword?: String, needVerification?: boolean}` |
| GET | `/api/keyword-alerts/my` | User | 내 키워드 알림 목록 | **Response**: `{success: boolean, keywords: String[], count: int}` |
| DELETE | `/api/keyword-alerts` | User | 키워드 알림 삭제 | **Params**: `keyword: String`<br/>**Response**: `{success: boolean, message: String}` |
| GET | `/api/keyword-alerts/count` | User | 키워드 알림 수 | **Response**: `{success: boolean, count: int}` |

---

## 11. Admin Action APIs

### AdminActionApiController (`/admin/api/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| POST | `/admin/api/users/{userId}/suspend` | Admin | 사용자 정지 | **Body**: `SuspensionRequest`<br/>**Response**: `ApiResponse<Void>` |
| POST | `/admin/api/users/{userId}/unsuspend` | Admin | 사용자 정지 해제 | **Body**: `UnsuspendRequest`<br/>**Response**: `ApiResponse<Void>` |
| POST | `/admin/api/posts/{postId}/unblock` | Admin | 게시글 차단 해제 | **Body**: `UnsuspendRequest`<br/>**Response**: `ApiResponse<Void>` |
| GET | `/admin/api/users/{userId}/actions` | Admin | 사용자 조치 이력 | **Response**: `ApiResponse<List<AdminActionDto>>` |
| GET | `/admin/api/posts/{postId}/actions` | Admin | 게시글 조치 이력 | **Response**: `ApiResponse<List<AdminActionDto>>` |
| GET | `/admin/api/admins/{adminId}/actions` | Admin | 관리자 조치 이력 | **Response**: `ApiResponse<List<AdminActionDto>>` |
| GET | `/admin/api/actions/recent` | Admin | 최근 조치 이력 | **Params**: `limit: int=10`<br/>**Response**: `ApiResponse<List<AdminActionDto>>` |
| GET | `/admin/api/users/{userId}/suspension-status` | Admin | 사용자 정지 상태 확인 | **Response**: `ApiResponse<Boolean>` |

---

## 12. Cache Management APIs

### CacheStatsApiController (`/api/cache/`)
| Method | Endpoint | Auth | Description | Request/Response |
|--------|----------|------|-------------|------------------|
| GET | `/api/cache/departments/stats` | Public | 학과 캐시 상세 통계 | **Response**: `{cacheName, hitRate, currentSize, performanceGrade, ...}` |
| GET | `/api/cache/summary` | Public | 전체 캐시 요약 통계 | **Response**: `{timestamp, cacheNames, departments: {...}}` |

---

## 🔑 Key Features & Security

### 특별 기능
- **이메일 인증 필수**: 찜하기, 키워드 알림 등은 이메일 인증 후 사용 가능
- **Rate Limiting**: 이메일 재발송 등에 속도 제한 적용
- **실시간 기능**: SSE 기반 알림, Firebase 채팅 통합
- **캐시 최적화**: 학과 정보 등 자주 조회되는 데이터 캐싱
- **가격 추적**: 도서별 가격 시세 분석
- **신고 시스템**: 사용자 신고 및 관리자 처리 워크플로우

### 보안 설정
- **CSRF 보호**: `/api/**` 제외하고 모든 엔드포인트에 적용
- **세션 관리**: 동시 세션 수 제한
- **역할 기반 접근**: User/Admin 권한 구분
- **공개 엔드포인트**: 검색, 도서 데이터 등은 인증 없이 접근 가능

### 성능 최적화
- **Caffeine Cache**: 학과, 학교 정보 캐싱으로 95%+ 성능 향상
- **Virtual Thread**: Java 21 기반 비동기 처리
- **Batch Processing**: 알림 처리 최적화
- **Pagination**: 모든 목록 API에 페이징 적용

---

## 📊 API Statistics

- **총 API 엔드포인트**: 81개
- **기능 그룹**: 12개
- **인증 레벨**: 3단계 (Public/User/Admin)
- **실시간 기능**: SSE Stream, Firebase Chat
- **외부 API 연동**: Naver Book API, Gmail SMTP
- **캐시 적용**: Department, School 데이터
- **비동기 처리**: Notification, Email 발송

이 문서는 Unibook 프로젝트의 모든 API 엔드포인트를 완전히 문서화하여, 개발자들이 시스템의 전체 구조를 이해하고 효율적으로 API를 활용할 수 있도록 돕습니다.