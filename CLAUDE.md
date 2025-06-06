# Unibook Project Instructions

## 🎯 Project Overview
**Unibook**: 대학생 맞춤형 교재 거래 플랫폼
- **핵심 기능**: 학교-학과-교수-과목별 교재 연관 검색 및 거래
- **개발 기간**: 3주 (2025년 5월 25일 시작)
- **기술 스택**: Spring Boot 3.5.0, Java 21, MySQL 8.0+, JPA, Thymeleaf, Bootstrap 5, Firebase

## 🔧 Technical Configuration
```yaml
Spring Boot: 3.5.0
Java: 21 (LTS)
MySQL: 8.0+ (localhost:3306, root/1234)
Firebase: Firestore + Storage
Bootstrap: 5.3.0
```

## 📍 Development Environment
- **Path**: /mnt/c/dev/unibook (Windows C:\dev\unibook)
- **IDE**: IntelliJ IDEA (Windows) - Run from IntelliJ, not WSL
- **WSL**: For git and Claude Code only

## ✅ Completed Features (Day 1-17)

### Week 1: Core Features
- **Day 1-2**: Project setup, Entity design, Basic CRUD
- **Day 3**: Authentication system (Spring Security)
- **Day 4**: Email verification with SMTP
- **Day 5**: Post CRUD with multi-image upload, Naver Book API
- **Day 6**: Subject-Professor system (학교 내 제한)
- **Day 7**: MySQL Full-text Search

### Week 2: Advanced Features
- **Day 8**: Wishlist, Notifications (SSE), My Page
- **Day 9-10**: Firebase Chat System
  - Real-time messaging with Firestore
  - Image upload support
  - Read receipts (1/읽음)
  - Leave chat functionality
  - Trade status updates in chat
  - ChatRoom entity with Firebase integration
- **Day 11**: Dark Mode Support
  - Bootstrap 5 native dark mode
  - Theme persistence in localStorage
  - Smooth transitions without flashing
- **Day 12**: UI/UX Improvements
  - Homepage redesign with carousel
  - Enhanced footer with links
  - Hero section with gradient patterns
  - Quick search links
  - Card hover effects
- **Day 13**: Posts List Page Complete Redesign
  - Hero section with gradient background
  - Floating search card with enhanced filters
  - Quick filter pills for categories
  - Improved card design with animations
  - Lazy loading for images
  - Smooth page transitions
  - Loading states and skeleton UI
  - Enhanced pagination design

### Week 3: Admin & Advanced Features
- **Day 14**: Report System & Post Status Management
  - Report entity and service implementation
  - Admin report management with detail pages
  - Post blocking functionality (BLOCKED status)
  - Status badge consistency across all pages
  - Report processing with post auto-blocking
- **Day 15**: Admin Dashboard & Enhanced Header
  - Complete admin dashboard with statistics
  - User management with search and pagination
  - Post management with status filters
  - Report statistics and charts (Chart.js)
  - Enhanced header with glassmorphism design
  - User avatar system with colored initials
  - Integrated search bar in header
  - Advanced accessibility features
- **Day 16**: Advanced Notification & UI Systems
  - Wishlist price change alert system with dynamic messaging
  - Keyword alert system for post matching notifications
  - User suspension system with admin controls
  - Enhanced post list with price range filters
  - Enhanced Footer with wave animation and static pages
  - Post form UI complete redesign with progress steps
  - Railway deployment optimization and memory fixes
- **Day 17**: 대규모 코드 리팩터링 및 관리자 대시보드 개선
  - PostController 252줄 → 50줄로 80% 코드 감소
  - PostService, PostRepository, NotificationService 대폭 리팩터링
  - AuthorizationService 신규 생성으로 권한 검증 로직 중앙집중화
  - PostControllerHelper, PostFormDataBuilder 유틸리티 클래스 추가
  - PostSearchRequest DTO로 11개 파라미터 통합 관리
  - 관리자 대시보드에서 사용자별 게시글 조회 기능 구현
  - 책별 가격 시세 차트 시스템 구현
  - 코드 품질 대폭 향상 (기능 100% 보존)

## 🏗️ Project Structure
```
unibook/
├── src/main/java/com/unibook/
│   ├── controller/
│   │   ├── PostController.java (대폭 리팩터링 완료)
│   │   ├── AdminController.java
│   │   ├── ChatController.java
│   │   ├── SuspensionController.java
│   │   └── dto/
│   │       └── PostSearchRequest.java (NEW - 파라미터 통합)
│   │   └── api/
│   │       ├── ChatApiController.java
│   │       ├── ReportApiController.java
│   │       ├── KeywordAlertApiController.java
│   │       └── [other controllers]
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── ChatRoom.java
│   │   │   ├── Report.java
│   │   │   ├── KeywordAlert.java
│   │   │   ├── AdminAction.java
│   │   │   └── [18 total entities]
│   │   └── dto/
│   │       ├── ChatDto.java
│   │       ├── ReportDto.java
│   │       ├── PriceTrendDto.java (NEW - 가격 시세용)
│   │       └── PostResponseDto.java (images, not postImages)
│   ├── service/
│   │   ├── PostService.java (리팩터링 완료)
│   │   ├── NotificationService.java (리팩터링 완료)
│   │   ├── AuthorizationService.java (NEW - 권한 검증 중앙화)
│   │   ├── PostControllerHelper.java (NEW - 복잡한 로직 분리)
│   │   ├── ChatService.java
│   │   ├── ReportService.java
│   │   ├── KeywordAlertService.java
│   │   └── [other services]
│   ├── repository/
│   │   ├── PostRepository.java (리팩터링 완료 - 통합 메서드)
│   │   ├── ChatRoomRepository.java
│   │   ├── ReportRepository.java
│   │   ├── KeywordAlertRepository.java
│   │   └── [other repositories]
│   └── util/
│       ├── PostFormDataBuilder.java (NEW - 폼 데이터 빌딩)
│       ├── AvatarUtil.java
│       ├── FileUploadUtil.java
│       └── [other utilities]
└── src/main/resources/
    ├── static/
    │   ├── css/
    │   │   ├── dark-mode.css
    │   │   ├── enhanced-header.css
    │   │   ├── notification.css
    │   │   ├── status-badge.css
    │   │   └── [other styles]
    │   └── js/
    │       ├── firebase-config.js
    │       ├── firebase-chat.js
    │       ├── chat-notification.js
    │       ├── enhanced-header.js
    │       ├── notification.js
    │       └── [other scripts]
    └── templates/
        ├── admin/
        │   ├── dashboard.html
        │   ├── reports.html
        │   ├── report-detail.html
        │   ├── users.html (사용자별 게시글 조회 기능)
        │   ├── posts.html
        │   └── statistics.html
        ├── chat/
        │   ├── list.html
        │   └── room.html
        ├── fragments/
        │   └── header.html (enhanced with user-meta fragment)
        ├── posts/
        │   ├── form.html (redesigned with progress steps)
        │   ├── list.html (가격 필터 1억원 상한)
        │   ├── detail.html (가격 시세 차트)
        │   └── form-original.html (backup)
        ├── about.html
        ├── faq.html
        ├── guide.html
        ├── privacy.html
        ├── terms.html
        └── index.html
```

## 🔑 Key Technical Details

### Entities (18 total, all extend BaseEntity)
1. **User** - verified field for email auth, role-based access (ADMIN/USER), suspension system
2. **Post** - status (AVAILABLE/RESERVED/COMPLETED/DELETED/BLOCKED), price change tracking
3. **PostImage** - imageUrl field (not imagePath)
4. **Book** - imageUrl for cover images, price trend tracking
5. **ChatRoom** - Firebase integration, buyer/seller unread counts
6. **Notification** - SSE real-time, multiple types (MESSAGE/WISHLIST_STATUS_CHANGED/WISHLIST_PRICE_CHANGED/KEYWORD_MATCH)
7. **Report** - post reporting system with status tracking
8. **KeywordAlert** - user keyword notifications for post matching
9. **AdminAction** - user suspension tracking with reasons and durations
10. [Others: School, Department, Professor, Subject, Wishlist, etc.]

### 리팩터링된 아키텍처
#### PostController 리팩터링 (80% 코드 감소)
```java
// Before: 252줄의 복잡한 컨트롤러
// After: 50줄의 간결한 컨트롤러

// 파라미터 통합
PostSearchRequest request = PostSearchRequest.from(page, size, search, ...);

// 헬퍼 클래스 활용
Long userSchoolId = postControllerHelper.getUserSchoolId(userPrincipal);
Page<PostResponseDto> postDtos = postControllerHelper.getPostsWithDto(request, pageable);

// 폼 데이터 빌딩 분리
postFormDataBuilder.addFormAttributes(model, true);
```

#### AuthorizationService (권한 검증 중앙화)
```java
// 권한 검증 로직 통합
authorizationService.requireCanEdit(post, userPrincipal, "게시글 수정 권한이 없습니다.");
authorizationService.requireOwnerOrAdmin(post, userPrincipal, "권한이 없습니다.");

// 상세 권한 정보 계산
AuthorizationInfo authInfo = authorizationService.calculateDetailPageAuth(post, userPrincipal);
```

#### PostRepository 통합 메서드
```java
// 기존 중복 메서드들을 통합
Page<Post> findPostsWithOptionalFilters(
    Long subjectId, Long professorId, String bookTitle,
    Post.PostStatus status, Post.ProductType productType,
    Long schoolId, Integer minPrice, Integer maxPrice, Pageable pageable);

// 사용자별 조회 (가격 필터 포함)
Page<Post> findUserPostsByUserUnified(Long userId, Integer minPrice, Integer maxPrice, Pageable pageable);
```

### PostResponseDto Structure
```java
// Use 'images' not 'postImages'
private List<ImageDto> images;

// ImageDto has 'imagePath' not 'imageUrl'
public static class ImageDto {
    private String imagePath;
}
```

### Firebase Chat Architecture
- **Firestore Structure**: /chatrooms/{roomId}/messages/{messageId}
- **Message Types**: TEXT, IMAGE, SYSTEM
- **Real-time Sync**: onSnapshot listeners
- **Read Status**: isReadByBuyer, isReadBySeller fields

### Dark Mode Implementation
- Uses Bootstrap 5's `data-bs-theme` attribute
- Theme saved in localStorage
- Immediate application to prevent flashing
- All components have dark mode styles

### Enhanced Header System
- **Glassmorphism Design**: Backdrop blur, transparency effects
- **User Avatar System**: Colored circular avatars with initials
- **Integrated Search**: Centered desktop search with Ctrl+K shortcut
- **Accessibility**: Skip-to-content link, keyboard navigation
- **Responsive**: Mobile-first design with collapsible navigation
- **User Meta Fragment**: Essential for dropdown functionality

### Admin Dashboard Features
- **Statistics Dashboard**: Real-time charts with Chart.js
- **Report Management**: Post blocking with detailed tracking
- **User Management**: Search, pagination, role management, suspension system
- **Post Management**: Status filtering and bulk operations
- **User-specific Posts**: 관리자가 특정 사용자의 모든 게시글 조회 가능

### Advanced Notification System
- **Wishlist Price Alerts**: Dynamic messaging with emoji for price changes
- **Keyword Alerts**: Post matching notifications for user keywords
- **Status Change Alerts**: Real-time notifications for wishlist item status changes
- **Payload Storage**: JSON payload for detailed notification data
- **Asynchronous Processing**: @Async for performance optimization

### Price Trend Chart System
- **Chart.js Integration**: Interactive price trend visualization
- **Data Processing**: Available/Reserved vs Completed price comparison
- **Real-time Updates**: Dynamic chart rendering based on book data

### Enhanced UI Components
- **Wave Footer**: Animated wave with gradient background, app download buttons
- **Post Form**: Progress steps, drag-and-drop image upload, enhanced animations
- **Price Range Filter**: Advanced filtering (최대 1억원 상한)
- **Static Pages**: Complete about, FAQ, guide, privacy, terms pages
- **Status Badges**: Consistent design across all pages

### User Suspension System
- **Admin Controls**: Suspension duration and reason tracking
- **Login Prevention**: Security config blocks suspended users
- **Audit Trail**: AdminAction entity for suspension history

## 💡 Key Principles
1. **DTO Pattern**: Always use DTOs, never expose entities to views
2. **SRP (Single Responsibility)**: 각 클래스는 하나의 책임만 가짐
3. **DRY (Don't Repeat Yourself)**: 중복 코드 완전 제거
4. **Performance**: Fetch Join, caching, indexes, unified repository methods
5. **Security**: Spring Security, CSRF protection, rate limiting, centralized authorization
6. **UX**: Real-time validation, AJAX interactions, loading states
7. **Code Style**: 2-space indentation (except Java)

## 🚨 Common Issues & Solutions

### Recent Fixes (Day 17)
1. **PostController 리팩터링 완료**
   - 252줄 → 50줄로 80% 코드 감소
   - 중복 코드 완전 제거
   - 파라미터 객체 패턴 적용
   - 헬퍼 클래스로 복잡한 로직 분리

2. **AuthorizationService 도입**
   - 권한 검증 로직 중앙집중화
   - BLOCKED 게시글 접근 제어 개선
   - 일관된 권한 체크 로직

3. **Repository 통합 메서드**
   - 중복된 메서드들 통합
   - 선택적 필터링 지원
   - N+1 문제 해결

4. **관리자 대시보드 개선**
   - 사용자별 게시글 조회 기능 추가
   - userId 파라미터 지원

5. **가격 시세 차트 시스템**
   - Chart.js로 인터랙티브 차트 구현
   - 실시간 데이터 업데이트

### Previous Fixes
6. **PostResponseDto field names**
   - Use `post.images` not `post.postImages`
   - Use `imagePath` not `imageUrl` in templates

7. **Dark mode flashing**
   - Apply theme before DOM load
   - Use immediate theme application script

8. **Firebase chat read receipts**
   - Batch update for performance
   - Real-time UI updates via onSnapshot

9. **Chat notifications**
   - Integrated with main notification system
   - Uses SSE for real-time updates

10. **Admin system compilation errors**
    - Use `Post.PostStatus.BLOCKED` not `Post.Status.BLOCKED`
    - Use `post.getPostId()` not `post.getId()`
    - Use `PostResponseDto::from` not `this::convertToDto`

11. **Status badge transparency issues**
    - Removed rgba with opacity for better visibility
    - Consistent status colors across all pages

12. **Enhanced header accessibility**
    - Added main content wrapper with id="main-content"
    - Skip-to-content link for screen readers
    - User-meta fragment for dropdown functionality

13. **Notification system bugs**
    - Fixed status change notifications from edit form
    - Resolved publishWishlistStatusChangeNotifications logic error
    - Added price change detection with proper comparison

14. **Post form UI improvements**
    - Fixed drag-and-drop image upload click handler
    - Optimized sortable performance with 'original' helper
    - Enhanced form responsiveness and animations

15. **User suspension system**
    - SecurityConfig blocks suspended users at login
    - Admin action tracking for audit trail
    - Proper suspension duration handling

16. **Railway deployment optimization**
    - Memory usage optimization for OOM prevention
    - Java 21 configuration for deployment

### Development Tips
- Run from IntelliJ, not WSL terminal
- Check field names in DTOs vs entities
- Use toString() for enum comparisons in Thymeleaf
- Test dark mode on all pages
- Firebase rules must allow authenticated access
- Admin pages require consistent styling approach
- Status badges need proper color contrast
- Enhanced header requires main content wrapper for accessibility
- Always backup original files before major UI changes (e.g., form-original.html)
- Use @Async for notification processing to avoid blocking
- Test suspension system thoroughly with different user roles
- Verify price change detection with various numeric edge cases
- Post form drag-and-drop requires careful JavaScript event handling
- **리팩터링 시 기능 100% 보존 원칙 준수**
- **Extract Method 패턴으로 메서드 길이 단축**
- **Parameter Object 패턴으로 파라미터 수 줄이기**
- **Helper 클래스로 복잡한 비즈니스 로직 분리**

## 📝 Next Steps
- **Refactoring**: 나머지 컨트롤러들 리팩터링 (AuthController, ChatApiController 등)
- **Performance**: View count system, caching improvements, notification batching
- **Features**: Email notifications, mobile app, advanced search filters
- **Testing**: Unit tests for refactored components, integration tests
- **Deployment**: Docker containerization, CI/CD pipeline improvement
- **Analytics**: User behavior tracking, conversion metrics, price change analytics
- **UI/UX**: Mobile optimization, remaining page redesigns
- **Security**: Rate limiting improvements, enhanced validation

## 🛠️ Quick Commands
```bash
# Git operations (from WSL)
git add -A && git status
git commit -m "feat: description"

# Run application (from IntelliJ terminal)
./gradlew bootRun

# MySQL Full-text index
mysql -u root -p1234 unibook_db < create_fulltext_indexes.sql
```

## 📊 리팩터링 성과 요약
- **PostController**: 252줄 → 50줄 (80% 감소)
- **코드 중복**: 105줄 완전 제거
- **파라미터 수**: 11개 → 1개 DTO 객체
- **새로운 클래스**: 4개 (PostSearchRequest, AuthorizationService, PostControllerHelper, PostFormDataBuilder)
- **기능 보존율**: 100% (모든 기존 기능 정상 동작)
- **코드 품질**: 대폭 향상 (SRP, DRY 원칙 적용)