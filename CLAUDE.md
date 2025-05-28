Claude Code Instructions - Unibook Project
🎯 Project Overview
Unibook: 대학생 맞춤형 교재 거래 플랫폼

핵심 기능: 학교-학과-교수-과목별 교재 연관 검색 및 거래
개발 기간: 2주 (1주차: 핵심 기능, 2주차: 고도화)
기술 스택: Spring Boot 3.5.0, Java 21, MySQL 8.0+, JPA, Thymeleaf, Bootstrap 5

📅 프로젝트 시작일: 2025년 5월 25일
👤 개발자: ramyeonzoa
📍 GitHub: https://github.com/ramyeonzoa/unibook

🔧 Exact Version Configuration
Critical: Use these exact versions to avoid conflicts

Spring Boot: 3.5.0
Java: 21 (LTS)
Gradle: 8.4+
MySQL: 8.0+ (Windows localhost:3306)
Lombok: 필수 (IntelliJ Annotation Processing 활성화 필요)
Thymeleaf Security: thymeleaf-extras-springsecurity6

📍 Development Environment

Path: /mnt/c/dev/unibook (Windows C:\dev\unibook)
IDE: IntelliJ IDEA (Windows) - ⚠️ MUST run from IntelliJ, NOT WSL terminal
Database: MySQL on Windows (localhost:3306, username: root, password: 1234)
WSL: Claude Code와 git 작업용으로만 사용
Execution: gradlew bootRun은 반드시 IntelliJ 또는 Windows 터미널에서 실행

✅ Day 1-4 COMPLETED (2025년 1월 25-27일)

📋 Day 1 완료:
- Spring Boot 프로젝트 초기 설정
- 8개 Entity 클래스 생성
- Repository 인터페이스 생성
- MySQL 데이터베이스 생성
- GitHub 저장소 생성

📋 Day 2 완료:
- Service 계층 구현 (4개 서비스)
- HomeController + 메인 페이지
- CSV 데이터 로드 (학교 400개, 학과 12,870개)
- 성능 최적화 (Fetch Join, 캐싱)
- 보안 강화 (환경별 설정 분리)

📋 Day 3 완료:
- BaseEntity 생성 (JPA Auditing)
- 모든 Entity 재구성 (DB 스키마 정합성)
- Spring Security 설정
- DTO 패턴 전면 도입
- 회원가입/로그인 시스템 구현
- 실시간 비밀번호 검증
- 로그인 상태별 UI 분기

📋 Day 4 완료:
- Email Verification System 구현
  - Gmail SMTP 설정 (unibooknotify@gmail.com)
  - EmailVerificationToken Entity 및 Repository
  - EmailService 구현 (비동기 처리 @Async)
  - 이메일 인증/비밀번호 재설정 토큰 관리
- Email Templates 개선
  - 미니멀 모던 디자인 (그라데이션, 카드 스타일)
  - 반응형 이메일 템플릿 (verification.html, password-reset.html)
  - 인증 과정 시각화 (1-2-3 단계)
  - 브랜드 일관성 (📚 / 🔐 아이콘)
- Security & UX 개선
  - 비밀번호 재설정 시 이전 비밀번호 검증 (UserService.resetPassword)
  - 토큰 만료 시간 통일 (1시간)
  - Rate Limiting 구현 (RateLimitService - 1분 쿨다운, 시간당 5회)
  - 이메일 인증 플로우 개선
- Spring Retry 적용
  - 이메일 발송 실패 시 자동 재시도 (3회)
  - 지수 백오프 (1초 → 2초 → 4초)
  - @Retryable + @Recover 패턴
- UI/UX 개선
  - 인증된 사용자 재로그인 유도 (세션 갱신)
  - 로그인 시 이메일 자동 입력 (autoEmail)
  - 인증 메일 재발송: 로그인 사용자는 메인 페이지에서만 가능
  - 전용 토큰 에러 페이지 (/token-error)
  - 예외 처리 일관성 (ResourceNotFoundException, ValidationException 구분)
- 코드 리팩터링
  - Messages 클래스로 하드코딩된 메시지 상수화 (28개)
  - BookRepository: findTop8 → Pageable 방식으로 변경
  - 중복 비밀번호 검증 로직은 유지 (복잡도 고려)
- 추가 구현 세부사항
  - VerificationInterceptor: 모든 요청에 isEmailVerified 상태 자동 추가
  - WebMvcConfig: 인증 필요 경로 인터셉터 설정 (/posts/new, /wishlist, /chat 등)
  - AsyncConfig: 이메일 전용 스레드풀 (core 2, max 5, queue 100)
  - RateLimitService: 스케줄러로 1시간마다 2시간 이상된 기록 정리
  - AppConstants: 재시도/Rate Limit 관련 상수 추가
  - verification-required.html: 대화형 도움말 아코디언, AJAX 재발송

📋 Day 5 완료 (2025년 5월 28일):
- PostController 생성 (완전한 CRUD 엔드포인트)
  - 권한 체크: 작성자/관리자 구분
  - 이미지 업로드/삭제 처리
  - AJAX 상태 변경 API (/posts/{id}/status)
  - 이메일 미인증 사용자 차단 로직 추가
- PostRequestDto 생성 (게시글 폼 바인딩용 DTO)
  - Entity ↔ DTO 변환 메서드
  - 검증 어노테이션 포함
  - bookId, removeBook 필드 추가 (네이버 API 연동용)
- PostService 확장
  - 조회수 중복 방지 (30분 간격, 비동기 처리)
  - 이미지 처리 로직 (저장/삭제/순서)
  - 트랜잭션 격리 수준 설정 (READ_COMMITTED)
- 게시글 템플릿 구현
  - form.html: 드래그앤드롭 이미지 업로드, 책 검색 모달 통합
  - list.html: 카드 레이아웃, 페이지네이션, 필터 UI (백엔드 미구현)
  - detail.html: Bootstrap Carousel 이미지 갤러리, 책 정보 표시
- 파일 업로드 시스템
  - uploads/images/posts/ 자동 생성 (FileUploadUtil)
  - UUID 파일명, 정적 리소스 제공 (WebMvcConfig)
  - 파일 검증: 확장자, 크기, 빈 파일 체크
  - SecurityConfig에 /uploads/** 경로 허용 추가
- 공통 헤더/푸터 Fragment 구현
  - fragments/header.html 생성
  - 모든 페이지에 일관된 네비게이션 적용
  - Bootstrap 버전 통일 (5.3.0)
  - Thymeleaf 3.1+ 제약사항 대응 (#request 객체 사용 불가)
- 권한별 UI 개선
  - 작성자: 수정/삭제/상태변경 가능
  - 로그인 사용자: 문의하기(예약중도 가능)/찜하기
  - 비로그인 사용자: 게시글 조회만 가능, 로그인 유도
- 게시글 상태 표시 개선
  - Enum 비교 문제 해결 (toString() 사용)
  - 모든 페이지에서 일관된 배지 표시
  - 예약중 배지: bg-warning text-dark
- 다중 이미지 업로드 구현 완료
  - 최대 5개 이미지 동시 업로드
  - 드래그앤드롭으로 이미지 순서 변경 (jQuery UI Sortable)
  - 수정 시 기존/새 이미지 통합 관리
  - 이미지별 삭제 기능
  - 첫 번째 이미지가 대표 이미지 (썸네일)
- Bootstrap Carousel로 이미지 갤러리 구현
  - 수동 제어 (자동 슬라이드 제거)
  - 개선된 네비게이션 버튼
  - 반응형 디자인
- 네이버 책 검색 API 연동 완료
  - Client ID/Secret 설정 (application-local.yml)
  - BookSearchService 구현 (@Cacheable, @Retryable 포함)
  - 책 검색 모달 UI (명시적 검색, API 최적화)
  - 선택한 책 정보를 Book 엔티티로 저장 (ISBN 중복 방지)
  - 게시글과 Book 연동 처리
- Book 연동 고도화
  - ProductType.isTextbookType() 헬퍼 메서드 추가
  - removeBook 플래그로 명시적 책 연결 해제
  - 수정 시 기존 책 정보 유지 로직
  - 교재 타입 변경 시 자동 해제
  - ValidationException으로 사용자 친화적 에러 메시지
- Book Entity 확장
  - imageUrl 필드 추가 (네이버 API 썸네일 URL 저장)
  - publicationYear nullable 처리 (API 데이터 일관성)
  - BookService.findOrCreateBook() 중복 방지 로직
- UI/UX 성능 최적화
  - 자동완성 검색 디바운싱 (300ms)
  - 로딩 인디케이터 개선 (검색 제외)
  - 이미지 placeholder 404 루프 해결
  - CSRF 토큰 통합 관리
- 책 표지 이미지 시스템
  - 메인 페이지: 인기 도서 썸네일 표시
  - 게시글 폼: 선택한 책 미리보기
  - 게시글 상세: 책 정보 섹션에 표지 이미지
  - 조건부 표시: 이미지 URL 존재 시만

📋 Development Schedule

Week 1: Core Features
✅ Day 1-2: Project setup + Entity classes + Basic CRUD
✅ Day 3: Authentication system (signup/login)
✅ Day 4: Email verification with university domain validation
✅ Day 5: Post CRUD with image upload + Naver Book API (완료)
☐ Day 6: Advanced search functionality (PROJECT CORE)
☐ Day 7: Integration testing and UI improvement

Week 2: Advanced Features
☐ Day 8: Wishlist + Notification system
☐ Day 9-10: Firebase real-time chat (결정됨: Firebase 사용)
☐ Day 11: Advanced features (view count, user profile)
☐ Day 12: UI/UX improvements
☐ Day 13: Testing and bug fixes
☐ Day 14: Deployment preparation (플랫폼 미정 - 구현 후 결정)

📚 Day 5 네이버 책 검색 API 통합 세부사항:

**1. BookSearchDto 구조**
```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BookSearchDto {
    public static class Request {
        private String query;
        @Builder.Default private int display = 10;
        @Builder.Default private int start = 1;
    }
    
    public static class Response {
        private int total;
        private int start;
        private int display;
        private List<Item> items;
    }
    
    public static class Item {
        private String title;        // HTML 태그 제거 필요
        private String author;       // HTML 태그 제거 필요
        private String publisher;
        private String isbn;         // ISBN13 우선 사용
        private String image;        // 책 표지 URL
        private String pubdate;      // YYYYMMDD 형식
        private String description;  // HTML 태그 제거 필요
        
        // cleanData() 메서드로 HTML 태그 제거
    }
}
```

**2. BookSearchService 구현**
```java
@Service
@Slf4j
public class BookSearchService {
    @Cacheable(value = "bookSearch", key = "#query + '_' + #page + '_' + #size")
    @Retryable(value = {ResourceAccessException.class, HttpServerErrorException.class}, 
              maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public BookSearchDto.Response searchBooks(String query, int page, int size)
    
    @Recover
    public BookSearchDto.Response recover(Exception ex, String query, int page, int size)
}
```

**3. Book Entity 확장**
```java
@Entity
public class Book extends BaseEntity {
    @Column(length = 500)
    private String imageUrl; // 네이버 API 썸네일 URL
    
    private Integer publicationYear; // nullable로 변경 (was @NotNull)
    
    // ProductType에 헬퍼 메서드 추가
    public enum ProductType {
        TEXTBOOK, CERTBOOK, NOTE, PASTEXAM, ETC;
        
        public boolean isTextbookType() {
            return this == TEXTBOOK || this == CERTBOOK;
        }
    }
}
```

**4. BookService.findOrCreateBook() 로직**
```java
@Transactional
public Book findOrCreateBook(BookSearchDto.Item bookItem) {
    String isbn13 = extractISBN13(bookItem.getIsbn());
    
    // 1. ISBN으로 기존 책 조회
    Optional<Book> existing = bookRepository.findByIsbn(isbn13);
    if (existing.isPresent()) {
        Book book = existing.get();
        // imageUrl이 없으면 업데이트
        if (book.getImageUrl() == null && bookItem.getImage() != null) {
            book.setImageUrl(bookItem.getImage());
            return bookRepository.save(book);
        }
        return book;
    }
    
    // 2. 새 책 생성
    return Book.builder()
        .title(bookItem.getTitle())
        .author(bookItem.getAuthor())
        .publisher(bookItem.getPublisher())
        .isbn(isbn13)
        .imageUrl(bookItem.getImage())
        .publicationYear(parsePublicationYear(bookItem.getPubdate()))
        .build();
}
```

**5. API 엔드포인트**
```java
@RestController
@RequestMapping("/api/books")
public class BookApiController {
    
    @GetMapping("/search")
    public ResponseEntity<BookSearchDto.Response> searchBooks(
        @RequestParam String query,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size)
    
    @PostMapping("/select") 
    public ResponseEntity<Map<String, Object>> selectBook(
        @RequestBody BookSearchDto.Item bookItem)
}
```

**6. 프론트엔드 통합 (book-search.js)**
```javascript
// CSRF 토큰 설정
const token = $('meta[name="_csrf"]').attr('content');
const header = $('meta[name="_csrf_header"]').attr('content');

$.ajaxSetup({
    beforeSend: function(xhr) {
        if (header && token) {
            xhr.setRequestHeader(header, token);
        }
    }
});

// 책 검색 및 선택 로직
function searchBooks() {
    const query = $('#bookSearchQuery').val().trim();
    if (!query) return;
    
    $.get('/api/books/search', { query: query, size: 10 })
        .done(function(data) {
            displaySearchResults(data.items);
        });
}

function selectBook(bookData) {
    $.post('/api/books/select', JSON.stringify(bookData), function(response) {
        updateSelectedBook(response.book);
        $('#bookSearchModal').modal('hide');
    }, 'json');
}
```

**7. 설정 파일**
```yaml
# application.yml
naver:
  book:
    api:
      url: https://openapi.naver.com/v1/search/book.json

# application-local.yml  
naver:
  book:
    api:
      client-id: ${NAVER_CLIENT_ID}
      client-secret: ${NAVER_CLIENT_SECRET}
```

**8. RestTemplateConfig**
```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
```

**9. SecurityConfig 수정**
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/**") // API 엔드포인트 CSRF 제외
)
```

**10. 성능 최적화 구현**
- 캐싱: 동일 검색어 10분간 캐시
- 재시도: 네트워크 오류 시 3회 재시도 (지수 백오프)
- 디바운싱: 자동완성 검색 300ms 지연
- API 호출 최소화: 명시적 검색만 (자동 검색 제거)

🏗️ Current Project Structure
unibook/
├── src/main/java/com/unibook/
│   ├── common/          # AppConstants, Messages
│   ├── config/          # SecurityConfig, JpaAuditConfig, DataInitializer, 
│   │                   # AsyncConfig, VerificationInterceptor, WebMvcConfig
│   ├── controller/      # HomeController, AuthController, GlobalExceptionHandler,
│   │   │               # VerificationController, PostController
│   │   └── api/        # SchoolApiController, DepartmentApiController
│   ├── domain/
│   │   ├── entity/     # 13개 Entity (모두 BaseEntity 상속)
│   │   │               # EmailVerificationToken 포함
│   │   └── dto/        # DTO 클래스들 (PostRequestDto, PostResponseDto 포함)
│   ├── exception/       # 커스텀 예외 클래스들
│   │   ├── BusinessException (기본)
│   │   ├── ValidationException (검증)
│   │   ├── ResourceNotFoundException (404)
│   │   ├── AuthenticationException (인증)
│   │   ├── DataInitializationException (초기화)
│   │   ├── EmailException (이메일)
│   │   └── RateLimitException (Rate Limiting)
│   ├── repository/      # JPA Repository 인터페이스
│   │                   # EmailVerificationTokenRepository 포함
│   ├── security/        # UserPrincipal, CustomUserDetailsService
│   ├── service/         # 비즈니스 로직 서비스
│   │                   # EmailService, RateLimitService 포함
│   └── util/           # FileUploadUtil 등
└── src/main/resources/
    ├── static/         # 정적 리소스
    │   ├── css/       # loading.css
    │   └── js/        # loading.js
    ├── templates/       # Thymeleaf 템플릿
    │   ├── auth/       # signup.html, login.html, resend-verification.html,
    │   │               # forgot-password.html, reset-password.html,
    │   │               # verification-required.html
    │   ├── email/      # verification.html, password-reset.html
    │   ├── error/      # token-error.html
    │   ├── fragments/  # header.html (공통 헤더/푸터/메시지)
    │   └── posts/      # list.html, form.html, detail.html
    ├── data/           # CSV 파일들
    └── application.yml # 설정 파일

🔑 Critical Entity Structure (Day 3 확정)

1. **BaseEntity (모든 Entity의 부모)**
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private Long createdBy;
    
    @LastModifiedBy
    private Long updatedBy;
}
```

1. **User Entity 핵심 변경사항**
- nickname → name으로 변경
- phoneNumber 필드 추가 (필수)
- User는 School 직접 참조 없음, Department를 통해서만 접근
- verified 필드 (boolean, 이메일 인증용)
- UserRole: ADMIN, USER (STUDENT 아님)
- UserStatus: ACTIVE, SUSPENDED, WITHDRAWN (BANNED 아님)

1. **Post Entity 필수 필드**
- productType (TEXTBOOK, CERTBOOK, NOTE, PASTEXAM, ETC)
- status → PostStatus (AVAILABLE, RESERVED, COMPLETED)
- transactionMethod, campusLocation, description 추가
- postImages (List<PostImage>) - 이미지는 PostImage 엔티티로 관리

1. **Book Entity**
- isbn, publicationYear, originalPrice 필드 필수
- year → publicationYear으로 변경

1. **PostImage Entity**
- postImageId (imageId 아님)
- imageUrl (imagePath 아님)

🗄️ Database Configuration

**application.yml (공개 설정)**
```yaml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:mysql://localhost:3306/unibook_db?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: ${DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update  # 개발 중에는 필요시 create로 변경
    show-sql: true
  cache:
    type: simple
    cache-names:
      - schools
      - schoolSearch

app:
  home:
    popular-books-limit: 8
    recent-posts-limit: 5
```

**application-local.yml (gitignore, 개발자 로컬 설정)**
```yaml
spring:
  datasource:
    password: 1234

logging:
  level:
    com.unibook: DEBUG
    org.hibernate.SQL: DEBUG
```

🔐 Spring Security & Authentication (Day 3 완료)

1. **SecurityConfig 핵심 설정**
- `/`, `/home`, `/signup`, `/login`, `/api/**` - permitAll()
- `/search/**`, `/posts/**`, `/books/**` - permitAll() (검색은 로그인 없이 가능)
- CustomUserDetailsService와 DaoAuthenticationProvider 설정 필수
- 로그인 시 email 사용 (username 파라미터로 받음)

2. **회원가입 검증**
- 이메일: 대학 이메일 형식 (현재는 경고만, Day 4에서 엄격 적용)
- 비밀번호: 8-20자, 영문+숫자+특수문자(@$!%*#?&_) 필수
- 전화번호: 자동 포맷팅 (010-1234-5678)
- 학과: 필수 선택 (자동완성 검색)

3. **UserPrincipal**
- Spring Security UserDetails 구현
- 개발 단계: verified=false여도 로그인 가능
- isEnabled()는 status == ACTIVE만 체크

📊 DTO Pattern Implementation (Day 3 완료)

1. **필수 DTO 클래스들**
- SignupRequestDto: 회원가입 요청
- LoginRequestDto: 로그인 요청  
- UserResponseDto: 사용자 정보 응답
- PostResponseDto: 게시글 응답 (listFrom() 메서드 포함)
- SchoolDto, BookDto: 목록 표시용

2. **Service 메서드 규칙**
- Entity 반환: getAllSchools(), getRecentPosts()
- DTO 반환: getAllSchoolDtos(), getRecentPostDtos()
- Controller는 항상 DTO 메서드 사용

🎨 Frontend Implementation (Day 3 완료)

1. **Thymeleaf Security**
- xmlns:sec 네임스페이스 필수
- sec:authorize="isAuthenticated()" - 로그인 사용자만
- sec:authorize="!isAuthenticated()" - 비로그인 사용자만
- sec:authentication="principal.name" - 사용자 정보 접근

2. **자동완성 검색**
- jQuery UI Autocomplete 사용
- 학교: 2글자 이상 입력 시 작동
- 학과: 학교명 입력 → 해당 학교의 모든 학과 표시 (limit 200)
- 선택 시 hidden input에 ID 저장

3. **실시간 검증**
- 이메일 중복 체크: 500ms 디바운스
- 비밀번호 규칙: 각 조건별 ✅/❌ 표시
- 비밀번호 확인: 실시간 일치 여부 체크

⚠️ CONFIRMATIONS & DECISIONS

✅ 확정된 사항:
- 이메일 인증 방식: User.verified boolean 필드 사용 (권한 기반 X)
- 파일 업로드 경로: uploads/images/posts/ (프로필 이미지는 uploads/images/profiles/)
- 검색 엔진: MySQL Full-text search (Elasticsearch 대신)
- 책 정보 입력: 네이버 책 검색 API를 통한 검색 → 선택 → DB 자동 저장

📌 Day 5 구현 완료 - 최종 점검 사항:
✅ 책 검색 및 선택 기능 (네이버 API 통합)
✅ 게시글 생성 시 책 연동 (ISBN 중복 방지)
✅ 게시글 수정 시 책 정보 유지/변경/삭제
✅ 다중 이미지 업로드 및 드래그앤드롭 순서 변경
✅ 교재 타입 변경 시 책 연결 자동 해제
✅ 네이버 API 에러 처리 (재시도, 캐싱)
✅ 책 표지 이미지 전체 시스템 적용
✅ UI/UX 성능 최적화 (디바운싱, 로딩 개선)
✅ 모든 템플릿에서 일관된 디자인

☐ Day 6 계획 - Advanced Search System (PROJECT CORE):
- MySQL Full-text search 인덱스 설정
- 책 상세 페이지 구현
- "이 책을 사용하는 과목" 기능
- 학교 → 학과 → 교수 → 과목 계층 구조 검색
- "우리 학교만 보기" 필터
- 검색 히스토리 관리

☐ 미정 사항:
- 채팅 시스템: Firebase 확정 (Day 9-10)
- 배포 플랫폼: AWS, NCP 등 (Day 14에서 결정)

🎯 Key Features to Implement (Day 5-14)

1. **✅ Day 5: Post CRUD with Image Upload (완료)**
- 게시글 작성 폼 ✅
- 다중 이미지 업로드 (최대 5개) ✅
- 드래그앤드롭 이미지 순서 변경 ✅
- Bootstrap Carousel 갤러리 ✅
- 네이버 책 검색 API 연동 ✅
- 게시글 수정/삭제 ✅

2. **Day 6: Advanced Search System (PROJECT CORE)**
- 교재 상세 페이지
- "이 책을 사용하는 과목" 섹션
- 학교 → 학과 → 교수 → 과목 계층 구조
- "우리 학교만 보기" 필터
- 클릭 가능한 네비게이션
- 검색 히스토리

3. **Day 7: Integration Testing & UI**
- 전체 기능 통합 테스트
- UI/UX 개선
- 반응형 디자인 점검
- 성능 최적화

4. **Day 8: Wishlist + Notification**
- 찜하기 기능
- 실시간 알림 (SSE 또는 WebSocket)
- 알림 설정 페이지

5. **Day 9-10: Real-time Chat**
- Firebase 설정 (결정됨: Firebase 사용)
- 1:1 채팅
- 채팅방 목록
- 읽음 표시
- 이미지 전송

6. **Day 11: Advanced Features**
- 조회수 증가 (중복 방지)
- 사용자 프로필 페이지
- 거래 후기
- 신고 기능

7. **Day 12: UI/UX Improvements**
- 디자인 시스템 통일
- 다크 모드
- 접근성 개선
- 로딩 상태 표시

8. **Day 13: Testing & Bug Fixes**
- 단위 테스트
- 통합 테스트
- 버그 수정
- 보안 점검

9. **Day 14: Deployment Preparation**
- 프로덕션 설정
- 도커라이징
- CI/CD 파이프라인
- 모니터링 설정

🚨 Common Pitfalls & Solutions (Day 1-3 경험)

1. **Lombok 관련**
- 문제: @ToString 순환 참조
- 해결: @ToString.Exclude 사용 (연관관계 필드에)
- 주의: @Builder.Default 필수 (컬렉션 초기화)

2. **Entity 필드명 불일치**
- User: nickname → name
- Book: year → publicationYear  
- PostImage: imageId → postImageId, imagePath → imageUrl
- 해결: DTO에서 정확한 getter 메서드명 사용

3. **Spring Security 로그인 실패**
- 원인: CustomUserDetailsService 미연결
- 해결: SecurityConfig에 authenticationProvider Bean 설정
- 주의: HTML form의 username 필드명 유지 (email 입력받아도)
- 추가: 세션 고정 공격 방어, 동시 로그인 차단 설정됨

4. **학과 자동완성 일부만 표시**
- 원인: API에서 20개 제한
- 해결: limit을 200으로 증가, 정렬 추가

5. **폼 검증 실패 시 데이터 유실**
- 원인: 선택한 학과 정보 미복원
- 해결: Model에 selectedDepartmentText 추가

6. **N+1 쿼리 문제**
- 해결: Fetch Join 사용 + @BatchSize 추가
```java
@Query("SELECT p FROM Post p " +
       "LEFT JOIN FETCH p.user u " +
       "LEFT JOIN FETCH u.department d " +
       "LEFT JOIN FETCH d.school")

// PostImage는 BatchSize로 해결
@BatchSize(size = 10)
private List<PostImage> postImages;
```

7. **리팩터링 후 새로운 구조**
- AppConstants: Magic Number 상수화
- Messages: 모든 문자열 메시지 중앙화
- 커스텀 예외: IllegalArgumentException → 구체적 예외
- 인덱스 추가: 주요 검색 필드에 DB 인덱스

8. **Day 4 추가 문제 해결**
- CSRF 토큰 누락: AJAX 요청에 헤더 추가 필요
- 토큰 에러 표시: 전용 에러 페이지로 해결
- 세션 갱신: 이메일 인증 후 재로그인 유도
- Rate Limit 메모리 누수: 스케줄러로 주기적 정리

9. **Day 5 문제 해결**
- Thymeleaf 3.1+ 제약: #request 객체 사용 불가 → active 클래스 제거 또는 JS 처리
- Enum 비교: 문자열 비교 시 toString() 메서드 사용 필요
- 중복 URL 매핑: 동일한 경로에 여러 컨트롤러 메서드 매핑 금지
- SecurityConfig 패턴: 정규식 대신 와일드카드 사용 (/posts/* not /posts/[0-9]+)
- 정적 리소스 접근: /uploads/** 경로를 SecurityConfig에 permitAll() 추가
- MVP 우선: 복잡한 기능은 나중에, 먼저 동작하는 코드 작성

📧 Gmail SMTP Configuration (Day 4 완료)
- Gmail: unibooknotify@gmail.com
- App Password: application-local.yml에 설정
- Spring Mail + Spring Retry 구성 완료
- 비동기 처리 (@Async + @EnableAsync)
- 재시도 로직: 3회, 지수 백오프

📊 CSV Data Processing (완료)
- univ-email-250411-final.csv: 학교 + 이메일 도메인
- univ-dept-mapped.csv: 학교별 학과 정보
- DataInitializer에서 자동 로드
- 도메인 없는 학교도 저장 (primaryDomain nullable)

🔍 Key Queries to Implement (Day 6)
```java
// 특정 책을 사용하는 과목 찾기
List<Subject> findSubjectsByBooks_BookId(Long bookId);

// 교수별 게시글
List<Post> findByBook_Subjects_Professor_ProfessorId(Long professorId);

// 학과별 게시글
List<Post> findByBook_Subjects_Professor_Department_DepartmentId(Long departmentId);

// 학교별 복합 검색
List<Post> findByBook_Subjects_Professor_Department_School_SchoolId(Long schoolId);
```

🛠️ 유용한 코드 스니펫

**자동완성 검색 구현**
```javascript
$("#departmentSearch").autocomplete({
    source: "/api/departments/search",
    minLength: 2,
    select: function(event, ui) {
        $("#departmentId").val(ui.item.id);
        $("#selectedDepartment").html(
            '<div class="alert alert-info">' + ui.item.text + '</div>'
        );
    }
});
```

**실시간 비밀번호 검증**
```javascript
$("#password").on("input", function() {
    const password = $(this).val();
    
    // 각 규칙별 체크
    if (password.length >= 8) {
        $("#lengthCheck").addClass("valid").find(".requirement-icon").text("✅");
    } else {
        $("#lengthCheck").removeClass("valid").find(".requirement-icon").text("❌");
    }
    // ... 다른 규칙들
});
```

**파일 업로드 유틸리티 (Day 5)**
```java
public void validateFile(MultipartFile file) {
    if (file.isEmpty()) {
        throw new IllegalArgumentException("파일이 비어있습니다.");
    }
    if (file.getSize() > maxFileSize) {
        throw new IllegalArgumentException("파일 크기가 10MB를 초과합니다.");
    }
    String extension = getFileExtension(file.getOriginalFilename());
    if (!allowedExtensions.contains(extension.toLowerCase())) {
        throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
    }
}
```

🔄 Development Workflow

1. **Entity 변경 시**
   - ddl-auto: create로 변경
   - 애플리케이션 실행 (테이블 재생성)
   - 데이터 확인 후 ddl-auto: update로 복원

2. **빌드 에러 시**
   - gradlew clean build
   - IntelliJ: File → Invalidate Caches and Restart
   - Annotation Processing 확인

3. **실행 방법**
   - IntelliJ에서 UnibookApplication 실행 (권장)
   - 또는 Windows 터미널에서 gradlew bootRun
   - WSL에서는 실행하지 말 것!

📌 현재 프로젝트 상태 (Day 5 진행중)

✅ Day 1-3 완료된 기능:
- 전체 인증 시스템 (회원가입/로그인/로그아웃)
- DTO 패턴 전면 적용
- 실시간 폼 검증
- 로그인 상태별 UI 분기
- 학교-학과 자동완성 검색
- BaseEntity 기반 감사(Audit) 기능
- 보안 강화: 세션 고정 공격 방어, 동시 로그인 차단
- 성능 개선: BookService 쿼리 최적화, 인덱스 추가, N+1 해결
- 예외 처리: 커스텀 예외 클래스 체계 구축
- 트랜잭션: 동시성 제어 (SERIALIZABLE)
- AuditorAware: 0L = 시스템 사용자 정의
- 코드 정리: Magic Number/String → 상수화

✅ Day 4 완료된 기능:
- 이메일 인증 시스템 (Gmail SMTP)
- 비밀번호 재설정 기능
- 이메일 템플릿 디자인 개선
- Spring Retry 자동 재시도
- 비동기 이메일 발송
- 토큰 만료 시간 관리
- UI/UX 일관성 개선

✅ Day 5 완료된 기능:
- 게시글 기본 CRUD ✅
- 다중 이미지 업로드 (최대 5개, 드래그앤드롭 순서 변경) ✅
- 네이버 책 검색 API 연동 (ISBN 중복 방지, 캐싱, 재시도) ✅
- Book Entity 확장 (imageUrl, nullable publicationYear) ✅
- 공통 헤더/푸터 (Bootstrap 5.3.0) ✅
- 권한별 UI (작성자/로그인/비로그인 구분) ✅
- 상태 변경 기능 (AJAX) ✅
- Bootstrap Carousel 이미지 갤러리 ✅
- 책 표지 이미지 시스템 (메인/폼/상세 페이지) ✅
- UI/UX 성능 최적화 (디바운싱, 로딩 개선) ✅
- CSRF 토큰 통합 관리 ✅

💡 핵심 원칙
1. Entity는 View에 직접 노출하지 않음 (항상 DTO 사용)
2. 모든 설정값은 application.yml에서 관리
3. 비밀번호 등 민감정보는 application-local.yml에
4. 성능 문제는 처음부터 고려 (Fetch Join, 캐싱, 인덱스)
5. 사용자 경험 우선 (실시간 검증, 자동완성)
6. 예외는 구체적으로 (커스텀 예외 사용)
7. 상수는 중앙 관리 (AppConstants, Messages)

🚀 Day 6 시작 명령어
```bash
cd /mnt/c/dev/unibook
claude-code "Day 5까지 완료된 상태야. CLAUDE.md 참고해서 Day 6 작업을 시작해줘:
1. MySQL Full-text search 인덱스 설정
2. 책 상세 페이지 구현 (/books/{id})
3. '이 책을 사용하는 과목' 기능
4. 학교→학과→교수→과목 계층 구조 검색
5. '우리 학교만 보기' 필터 구현"
```

📝 추가 고려사항
- 모바일 반응형 디자인 (Day 7, 12)
- SEO 최적화 (Day 12)
- 접근성 (WCAG 2.1) 준수 (Day 12)
- 성능 모니터링 도구 (Day 14)
- 에러 추적 시스템 (Day 14)