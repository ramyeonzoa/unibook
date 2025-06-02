# Unibook Project Instructions

## 🎯 Project Overview
**Unibook**: 대학생 맞춤형 교재 거래 플랫폼
- **핵심 기능**: 학교-학과-교수-과목별 교재 연관 검색 및 거래
- **개발 기간**: 2주 (2025년 5월 25일 시작)
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

## ✅ Completed Features (Day 1-15)

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

## 🏗️ Project Structure
```
unibook/
├── src/main/java/com/unibook/
│   ├── controller/
│   │   ├── AdminController.java (NEW)
│   │   ├── ChatController.java
│   │   └── api/
│   │       ├── ChatApiController.java
│   │       ├── ReportApiController.java (NEW)
│   │       └── [other controllers]
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── ChatRoom.java
│   │   │   ├── Report.java (NEW)
│   │   │   └── [15 other entities]
│   │   └── dto/
│   │       ├── ChatDto.java
│   │       ├── ReportDto.java (NEW)
│   │       └── PostResponseDto.java (images, not postImages)
│   ├── service/
│   │   ├── ChatService.java
│   │   ├── ReportService.java (NEW)
│   │   └── [other services]
│   └── repository/
│       ├── ChatRoomRepository.java
│       ├── ReportRepository.java (NEW)
│       └── [other repositories]
└── src/main/resources/
    ├── static/
    │   ├── css/
    │   │   ├── dark-mode.css
    │   │   ├── enhanced-header.css (NEW)
    │   │   └── [other styles]
    │   └── js/
    │       ├── firebase-config.js
    │       ├── firebase-chat.js
    │       ├── chat-notification.js
    │       ├── chat-list.js
    │       ├── dark-mode.js
    │       ├── enhanced-header.js (NEW)
    │       └── [other scripts]
    └── templates/
        ├── admin/ (NEW)
        │   ├── dashboard.html
        │   ├── reports.html
        │   ├── report-detail.html
        │   ├── users.html
        │   ├── posts.html
        │   └── statistics.html
        ├── chat/
        │   ├── list.html
        │   └── room.html
        ├── fragments/
        │   └── header.html (enhanced)
        └── index.html (with main wrapper)
```

## 🔑 Key Technical Details

### Entities (16 total, all extend BaseEntity)
1. **User** - verified field for email auth, role-based access (ADMIN/USER)
2. **Post** - status (AVAILABLE/RESERVED/COMPLETED/DELETED/BLOCKED)
3. **PostImage** - imageUrl field (not imagePath)
4. **Book** - imageUrl for cover images
5. **ChatRoom** - Firebase integration, buyer/seller unread counts
6. **Notification** - SSE real-time, NEW_MESSAGE type
7. **Report** - post reporting system with status tracking
8. [Others: School, Department, Professor, Subject, etc.]

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

### Admin Dashboard Features
- **Statistics Dashboard**: Real-time charts with Chart.js
- **Report Management**: Post blocking with detailed tracking
- **User Management**: Search, pagination, role management
- **Post Management**: Status filtering and bulk operations

## 💡 Key Principles
1. **DTO Pattern**: Always use DTOs, never expose entities to views
2. **Performance**: Fetch Join, caching, indexes
3. **Security**: Spring Security, CSRF protection, rate limiting
4. **UX**: Real-time validation, AJAX interactions, loading states
5. **Code Style**: 2-space indentation (except Java)

## 🚨 Common Issues & Solutions

### Recent Fixes
1. **PostResponseDto field names**
   - Use `post.images` not `post.postImages`
   - Use `imagePath` not `imageUrl` in templates

2. **Dark mode flashing**
   - Apply theme before DOM load
   - Use immediate theme application script

3. **Firebase chat read receipts**
   - Batch update for performance
   - Real-time UI updates via onSnapshot

4. **Chat notifications**
   - Integrated with main notification system
   - Uses SSE for real-time updates

5. **Admin system compilation errors**
   - Use `Post.PostStatus.BLOCKED` not `Post.Status.BLOCKED`
   - Use `post.getPostId()` not `post.getId()`
   - Use `PostResponseDto::from` not `this::convertToDto`

6. **Status badge transparency issues**
   - Removed rgba with opacity for better visibility
   - Consistent status colors across all pages

7. **Enhanced header accessibility**
   - Added main content wrapper with id="main-content"
   - Skip-to-content link for screen readers

### Development Tips
- Run from IntelliJ, not WSL terminal
- Check field names in DTOs vs entities
- Use toString() for enum comparisons in Thymeleaf
- Test dark mode on all pages
- Firebase rules must allow authenticated access
- Admin pages require consistent styling approach
- Status badges need proper color contrast
- Enhanced header requires main content wrapper for accessibility

## 📝 Next Steps
- **Performance**: View count system, caching improvements
- **Features**: User suspension system, advanced filtering
- **Testing**: Unit tests for services, integration tests
- **Deployment**: Docker, CI/CD pipeline
- **Analytics**: User behavior tracking, conversion metrics

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