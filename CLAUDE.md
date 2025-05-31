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

✅ Day 1-8 COMPLETED (2025년 5월 25일-31일)

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

📋 Day 6 완료 (2025년 5월 29일) - 과목-교수 연동 시스템 구현 & Subject-Post 직접 연결:
- **핵심 설계 원칙 확정**:
  - 과목 선택 시에만 학교 제약: 게시글 조회는 전체, 과목 입력은 본인 학교만
  - 사용자 중심 UX: 과목명 우선 → 교수명 보조 (교수 우선 아님)  
  - 데이터 신뢰성: 사용자는 본인 학교 교수/과목만 연결 가능 (타 학교 교재 판매 방지)
  - 교양과목 특별 처리: SubjectType.GENERAL은 "교양학부" 소속으로 자동 관리
  
- **게시글-과목 연동 설계 최종 결정** (2025년 5월 29일):
  - **모든 상품 타입**: 과목 선택 가능 (선택사항, 게시글당 최대 1개)
  - **교재 타입 (전공교재, 자격증교재)**: 과목 + 책 선택 모두 가능 (독립적)
  - **Subject Entity 확장**: year, semester 필드 추가 (학기별 별도 Subject)
  - **Post → Subject 직접 연결**: nullable ManyToOne
  - **SubjectBook 연결**: 과목+책 모두 선택 시에만 생성, year/semester는 Subject에서 참조
  - **정규화 원칙**: "2024년 1학기 데이터구조"와 "2024년 2학기 데이터구조"는 다른 Subject

- **Entity 및 DTO 구현**:
  - Subject Entity에 SubjectType enum 추가 (MAJOR, GENERAL)
  - **Subject Entity에 year, semester 필드 추가** (SubjectBook에서 제거)
  - **Post Entity에 Subject 직접 연결 추가** (nullable ManyToOne)
  - ProfessorDto, SubjectDto, SubjectBookDto 생성
  - SubjectWithProfessorRequest DTO: 과목+교수 통합 생성용

- **Service 계층 구현**:
  - ProfessorService: 학교별 교수 검색/생성, QueryNormalizer 적용
  - SubjectService: 학교별 과목 검색/생성, findOrCreateSubjectWithProfessor
  - SubjectBookService: 연도/학기별 교재 연동 관리
  - UserService: getSchoolIdByUserId 메서드 추가

- **Repository 최적화**:
  - find...ByNameAndSchool 메서드들로 학교 경계 적용
  - LOWER() 함수로 대소문자 무관 검색
  - 메서드 네이밍을 find... 형태로 통일 (Spring Data JPA 컨벤션)
  - DepartmentRepository에 교양학부 조회 메서드 추가

- **API 컨트롤러 구현**:
  - ProfessorApiController: /api/professors/search/my-school
  - SubjectApiController: /api/subjects/search/my-school, /api/subjects/create-with-professor
  - UserApiController: /api/users/me (현재 사용자 정보 조회)
  - 학교 내 검색으로 데이터 신뢰성 확보

- **프론트엔드 UX 전면 개편**:
  - subject-search-v2.js: 과목명 우선 검색 플로우
  - 기존 교수→과목 순서에서 과목→교수 순서로 변경
  - 검색 결과 없을 시 바로 새 과목 생성 폼 표시
  - 교양과목 선택 시 학과 선택 필드 자동 숨김
  - 사용자 소속 학교의 학과 목록 동적 로드

- **교양과목 처리 시스템**:
  - DataInitializer에서 모든 학교에 "교양학부" 자동 생성
  - 교양과목 선택 시 departmentId null → 자동 교양학부 배정
  - 전공과목은 학과 선택 필수, 교양과목은 학과 선택 불필요
  - 교수 검색 범위는 전공/교양 무관하게 사용자 소속 학교 내로 제한

- **Subject-Post 직접 연결 구현** (2025년 5월 29일 Phase 2):
  - Subject Entity 정규화: year, semester 필드를 SubjectBook에서 Subject로 이동
  - Post Entity에 subject 필드 추가 (ManyToOne, nullable)
  - 모든 상품 타입에서 과목 선택 가능 (필기노트, 족보 등도 포함)
  - SubjectBook은 과목+책 모두 선택 시에만 생성
  - posts/form.html: 과목 선택 UI를 모든 상품 타입에서 표시
  - posts/detail.html: 과목 정보 섹션 추가 (과목명, 교수, 학과, 연도/학기)
  - 같은 과목의 다른 자료 섹션 추가 (getRelatedPostsBySubject)
  - SubjectRepository, SubjectBookRepository 쿼리 메서드 업데이트
  - SubjectService, PostService에 year/semester 처리 로직 추가
  - subject-search-v2.js: 연도/학기 자동 설정 및 표시 기능 추가

📋 Day 7 완료 (2025년 5월 30일) - MySQL Full-text Search & 통합 검색 시스템:
- **MySQL Full-text Search 구현**:
  - create_fulltext_indexes.sql 스크립트 생성 (books, posts 테이블)
  - 한글 검색 지원을 위한 ngram 파서 설정 (토큰 최소 길이 2자)
  - 복합 인덱스: posts (title, content), books (title, author, publisher)
  - PostSearchProjection 생성 (MATCH AGAINST 점수 포함)
  - BookRepository, PostRepository에 Full-text 검색 쿼리 추가

- **통합 검색 기능 구현**:
  - HomeController에 /search 엔드포인트 추가
  - 게시글 + 책 통합 검색 결과 표시
  - 관련도순, 최신순, 가격순, 조회수순 정렬 옵션
  - 검색어 하이라이팅 (search-highlight.js/css)
  - 메인 페이지 검색창에서 통합 검색 연결

- **검색 결과 최적화**:
  - QueryNormalizer 활용한 검색어 정규화
  - Fetch Join으로 N+1 쿼리 방지
  - @Query의 countQuery 분리로 페이징 성능 최적화
  - 검색 점수(relevance) 기반 정렬

- **UI/UX 개선**:
  - 검색 결과 페이지 디자인 (게시글 카드 + 책 카드)
  - 정렬 옵션을 버튼에서 텍스트 링크로 변경
  - 검색어 강조 표시 (keyword 매개변수 전달)
  - 빈 검색 결과 안내 메시지

- **버그 수정 및 성능 개선**:
  - Enum 비교 시 toString() 메서드 사용 (Thymeleaf 호환성)
  - null 체크 강화 (게시글 상세 페이지)
  - transactionMethod 필수 입력 처리
  - 검색 하이라이팅 성능 최적화

📋 Day 8 완료 (2025년 5월 31일) - 찜하기 & 마이페이지 & 알림 시스템:
- **찜하기 기능 완전 구현**:
  - Wishlist Entity 및 Repository 구현
  - AJAX 찜하기/취소 기능 (/api/wishlist/toggle)
  - 찜 상태 실시간 UI 업데이트 (하트 아이콘 색상 변경)
  - 중복 찜하기 방지 (unique 제약 조건)
  - 찜 목록 페이지 (/posts/wishlist) - 기존 posts/list.html 재사용

- **마이페이지 구현**:
  - ProfileController 및 profile.html 생성
  - 개인정보 수정 (이름, 전화번호, 학과 변경)
  - 비밀번호 변경 (현재 비밀번호 확인 필수)
  - 실시간 비밀번호 검증 (signup 페이지와 동일한 UX)
  - 탭 기반 UI (정보 수정 / 비밀번호 변경)
  - N+1 쿼리 방지를 위한 최적화 쿼리 사용

- **헤더 메뉴 개선**:
  - "내 게시글" (/posts/my) 메뉴 추가 - 기존 posts/list.html 재사용
  - "찜 목록" (/posts/wishlist) 메뉴 추가
  - "설정" → "마이페이지" 이름 변경, 아이콘 변경 (bi-gear → bi-person-gear)
  - pageType 매개변수로 동일 템플릿 다용도 활용

- **알림 시스템 구현**:
  - **Entity**: Notification, NotificationType enum (WISHLIST_STATUS_CHANGED, POST_WISHLISTED 등)
  - **Repository**: 복합 인덱스 최적화, Fetch Join 쿼리
  - **Service**: NotificationService (비동기 알림 생성), NotificationEmitterService (SSE 연결 관리)
  - **Controller**: NotificationApiController (RESTful API + SSE 스트림)
  - **Frontend**: notification.js (SSE 연결, 실시간 업데이트), notification.css (스타일링)
  - **Features**: 
    - 찜한 게시글 상태 변경 시 실시간 알림
    - 읽지 않은 알림 개수 배지 표시
    - 알림 드롭다운 메뉴
    - 토스트 알림 (새 알림 도착 시)
    - 모든 알림 읽음 처리

- **전역 JavaScript 개선**:
  - email-resend.js 전역 모듈화 (모든 페이지에서 사용 가능)
  - header.html에 공통 스크립트 fragment 추가
  - CSRF 토큰 전역 설정

- **Repository 최적화**:
  - UserRepository.findByIdWithDepartmentAndSchool() - N+1 방지
  - PostRepository.findWishlistedPostsByUser() - Fetch Join 적용
  - PostRepository.findByUserIdWithDetails() - 내 게시글 조회 최적화
  - WishlistRepository.findByPostIdWithUser() - 찜한 사용자 목록 조회

- **테스트 코드 구현**:
  - NotificationServiceTest: 비동기 알림 생성 테스트
  - NotificationEmitterServiceTest: SSE 연결 및 동시성 테스트
  - NotificationApiControllerTest: API 엔드포인트 통합 테스트

📋 Development Schedule

Week 1: Core Features (완료)
✅ Day 1-2: Project setup + Entity classes + Basic CRUD
✅ Day 3: Authentication system (signup/login)
✅ Day 4: Email verification with university domain validation
✅ Day 5: Post CRUD with image upload + Naver Book API
✅ Day 6: Advanced search functionality (PROJECT CORE) - 과목-교수 연동 시스템
✅ Day 7: MySQL Full-text Search & 통합 검색 시스템

Week 2: Advanced Features
✅ Day 8: Wishlist + Notification system + 마이페이지
☐ Day 9-10: Firebase real-time chat (결정됨: Firebase 사용)
☐ Day 11: Advanced features (view count, user profile enhancements)
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
│   │                   # AsyncConfig, VerificationInterceptor, WebMvcConfig, RestTemplateConfig
│   ├── controller/      # HomeController, AuthController, GlobalExceptionHandler,
│   │   │               # VerificationController, PostController, ProfileController
│   │   ├── api/        # SchoolApiController, DepartmentApiController, BookApiController,
│   │   │               # ProfessorApiController, SubjectApiController, UserApiController,
│   │   │               # WishlistApiController, NotificationApiController
│   │   └── dto/        # ErrorResponse, PagedResponse, SubjectSelectionRequest,
│   │                   # SubjectWithProfessorRequest, ApiResponse
│   ├── domain/
│   │   ├── entity/     # 14개 Entity (모두 BaseEntity 상속)
│   │   │               # User, Post, PostImage, PostDescription, Book, School, Department,
│   │   │               # Professor, Subject, SubjectBook, Wishlist, Notification,
│   │   │               # EmailVerificationToken
│   │   └── dto/        # DTO 클래스들 (PostRequestDto, PostResponseDto, BookSearchDto,
│   │                   # NotificationDto, LoginRequestDto, SignupRequestDto,
│   │                   # UserResponseDto, ProfessorDto, SubjectDto, SubjectBookDto 등)
│   ├── exception/       # 커스텀 예외 클래스들
│   │   ├── BusinessException (기본)
│   │   ├── ValidationException (검증)
│   │   ├── ResourceNotFoundException (404)
│   │   ├── AuthenticationException (인증)
│   │   ├── DataInitializationException (초기화)
│   │   ├── DuplicateResourceException (중복)
│   │   ├── EmailException (이메일)
│   │   └── RateLimitException (Rate Limiting)
│   ├── repository/      # JPA Repository 인터페이스
│   │   │               # EmailVerificationTokenRepository, NotificationRepository,
│   │   │               # WishlistRepository, PostSearchProjection 포함
│   │   └── projection/ # PostSearchProjection (Full-text search용)
│   ├── security/        # UserPrincipal, CustomUserDetailsService
│   ├── service/         # 비즈니스 로직 서비스
│   │                   # EmailService, RateLimitService, BookSearchService,
│   │                   # ProfessorService, SubjectService, SubjectBookService,
│   │                   # WishlistService, NotificationService,
│   │                   # NotificationEmitterService 포함
│   └── util/           # FileUploadUtil, PageableUtils, QueryNormalizer
└── src/main/resources/
    ├── static/         # 정적 리소스
    │   ├── css/       # loading.css, search-highlight.css, notification.css
    │   └── js/        # loading.js, book-search.js, email-resend.js,
    │                   # search-highlight.js, subject-search-v2.js,
    │                   # notification.js
    ├── templates/       # Thymeleaf 템플릿
    │   ├── auth/       # signup.html, login.html, resend-verification.html,
    │   │               # forgot-password.html, reset-password.html,
    │   │               # verification-required.html
    │   ├── email/      # verification.html, password-reset.html
    │   ├── error/      # token-error.html
    │   ├── fragments/  # header.html (공통 헤더/푸터/메시지/스크립트)
    │   ├── posts/      # list.html, form.html, detail.html
    │   ├── index.html  # 메인 페이지
    │   └── profile.html # 마이페이지
    ├── data/           # univ-email-250411-final.csv, univ-dept-mapped.csv
    ├── create_database.sql           # DB 초기화 스크립트
    ├── create_fulltext_indexes.sql   # Full-text 검색 인덱스 생성
    ├── mysql_fulltext_config.md      # MySQL 설정 가이드
    └── application.yml # 설정 파일

🔑 Critical Entity Structure (Day 8 최신)

**총 14개 Entity (모두 BaseEntity 상속)**:
1. **User** - 사용자 (이메일 인증, 학과 연결)
2. **Post** - 게시글 (이미지, 책, 과목 연결)
3. **PostImage** - 게시글 이미지 (순서 관리)
4. **PostDescription** - 게시글 상세 설명 (1:1 관계)
5. **Book** - 책 정보 (네이버 API 연동)
6. **School** - 학교
7. **Department** - 학과
8. **Professor** - 교수 (학교별 관리)
9. **Subject** - 과목 (연도/학기별 정규화)
10. **SubjectBook** - 과목-책 연결 (활성 게시글 수 관리)
11. **Wishlist** - 찜하기 (사용자-게시글 연결)
12. **Notification** - 알림 (SSE, JSON 페이로드)
13. **EmailVerificationToken** - 이메일 인증 토큰

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

2. **User Entity 핵심 변경사항**
- nickname → name으로 변경
- phoneNumber 필드 추가 (필수)
- User는 School 직접 참조 없음, Department를 통해서만 접근
- verified 필드 (boolean, 이메일 인증용)
- UserRole: ADMIN, USER (STUDENT 아님)
- UserStatus: ACTIVE, SUSPENDED, WITHDRAWN (BANNED 아님)

3. **Post Entity 필수 필드**
- productType (TEXTBOOK, CERTBOOK, NOTE, PASTEXAM, ETC)
- status → PostStatus (AVAILABLE, RESERVED, COMPLETED)
- transactionMethod, campusLocation, description 추가
- postImages (List<PostImage>) - 이미지는 PostImage 엔티티로 관리
- **subject (ManyToOne, nullable)** - 모든 타입에서 과목 선택 가능
- **book (ManyToOne, nullable)** - 책 정보 연결
- Subject에서 연도/학기 정보 획득 (정규화)

4. **Wishlist Entity (Day 8 추가)**
- user, post 복합 unique 제약 조건
- 찜하기/취소 기능의 핵심

5. **Notification Entity (Day 8 추가)**
- NotificationType enum (WISHLIST_STATUS_CHANGED, POST_WISHLISTED 등)
- JSON payload 지원 (@JdbcTypeCode)
- 복합 인덱스 최적화 (recipient_user_id, is_read), (recipient_user_id, created_at)
- SSE 실시간 알림 시스템

6. **Subject Entity (Day 6 정규화)**
- year, semester 필드 추가 (학기별 별도 Subject)
- SubjectType enum (MAJOR, GENERAL)
- 같은 과목이라도 학기가 다르면 별도 Entity

7. **Book Entity**
- isbn, publicationYear, originalPrice 필드 필수
- imageUrl 필드 추가 (네이버 API 썸네일)
- year → publicationYear으로 변경

8. **PostImage Entity**
- postImageId (imageId 아님)
- imageUrl (imagePath 아님)
- imageOrder 필드로 순서 관리

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

🎯 Next Phase: Real-time Chat & Advanced Features (Day 9+)

**Day 9-10: Firebase Real-time Chat**
- Firebase 프로젝트 설정 및 SDK 통합
- 1:1 채팅 시스템 구현
- 채팅방 목록 및 메시지 히스토리
- 읽음 표시 및 실시간 상태
- 이미지 전송 기능

**Day 11: Advanced Features**
- 조회수 증가 시스템 (중복 방지, 비동기 처리)
- 사용자 프로필 페이지 확장 (거래 히스토리)
- 거래 후기 시스템
- 신고 기능 및 관리자 도구

**Day 12: UI/UX Improvements**
- 디자인 시스템 통일 및 일관성 향상
- 다크 모드 지원
- 접근성 개선 (WCAG 2.1 준수)
- 로딩 상태 및 스켈레톤 UI

**Day 13: Testing & Quality Assurance**
- 단위 테스트 작성 (Service 계층 중심)
- 통합 테스트 (API 엔드포인트)
- 성능 테스트 및 최적화
- 보안 점검 및 취약점 분석

**Day 14: Deployment Preparation**
- 프로덕션 환경 설정
- Docker 컨테이너화
- CI/CD 파이프라인 구축
- 모니터링 및 로깅 시스템 설정

**미정 사항**:
- 배포 플랫폼: AWS/NCP/기타 (Day 14에서 최종 결정)
- 도메인 및 SSL 인증서 설정
- CDN 사용 여부 (이미지 최적화)

🚨 Common Pitfalls & Solutions (Day 1-8 경험)

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

10. **Day 7-8 문제 해결**
- **MySQL Full-text 설정**: ngram 파서 토큰 길이 2자로 설정 (한글 검색 지원)
- **SSE 연결 관리**: CopyOnWriteArrayList로 스레드 안전성 확보
- **JSON 필드 네이밍**: @JsonProperty("isRead")로 프론트엔드 호환성 해결
- **LazyInitializationException**: @Async 메서드에서 findById 사용, getReferenceById 금지
- **테스트 코드 Stubbing**: @MockitoSettings(strictness = Strictness.LENIENT) 사용
- **N+1 쿼리 최적화**: Repository에 Fetch Join 쿼리 추가, countQuery 분리
- **비밀번호 검증 일관성**: 정규식 패턴 전역 상수화, 동일한 UX 적용
- **전역 JavaScript**: 공통 기능은 별도 파일로 분리, header.html에서 로드
- **페이지 재사용**: pageType 매개변수로 동일 템플릿 다용도 활용 (내 게시글/찜 목록)
- **알림 배지 표시**: SSE 연결 전 기존 미읽음 알림 수 조회 필수

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

📌 현재 프로젝트 상태 (Day 8 완료)

✅ **핵심 기능 완성** (Week 1 + Day 8):
- **인증 시스템**: 회원가입/로그인/이메일 인증/비밀번호 재설정 완전 구현
- **게시글 시스템**: CRUD/다중 이미지 업로드/상태 관리/권한 제어 완전 구현
- **책 연동 시스템**: 네이버 API 검색/선택/저장/표지 이미지 완전 구현
- **과목-교수 연동**: 학교 내 제한/과목명 우선 검색/연도-학기 정규화 완전 구현
- **검색 시스템**: MySQL Full-text Search/통합 검색/하이라이팅 완전 구현
- **찜하기 시스템**: AJAX 토글/목록 페이지/상태 알림 완전 구현
- **알림 시스템**: SSE 실시간/드롭다운 UI/토스트 알림 완전 구현
- **마이페이지**: 정보 수정/비밀번호 변경/실시간 검증 완전 구현

✅ **기술 스택 확정**:
- **Backend**: Spring Boot 3.5.0, Java 21, JPA/Hibernate, MySQL 8.0+
- **Frontend**: Thymeleaf, Bootstrap 5.3.0, jQuery, AJAX
- **External API**: 네이버 책 검색 API (캐싱/재시도 포함)
- **Messaging**: Server-Sent Events (SSE) for 실시간 알림
- **Performance**: MySQL Full-text Search, Fetch Join, 복합 인덱스

✅ **아키텍처 패턴**:
- **Entity**: 14개 Entity, BaseEntity 상속, JPA Auditing
- **DTO**: 완전한 DTO 패턴, Entity-DTO 분리
- **Repository**: Spring Data JPA, Custom Query 최적화
- **Service**: 비즈니스 로직 분리, @Async 비동기 처리
- **Controller**: RESTful API + Thymeleaf 템플릿
- **Exception**: 커스텀 예외 체계, 글로벌 예외 처리
- **Security**: Spring Security, 세션 기반 인증
- **Caching**: Spring Cache (Simple), API 결과 캐싱

✅ **성능 최적화 완료**:
- **N+1 Query 해결**: Fetch Join, @BatchSize, 최적화 쿼리
- **DB 인덱스**: Full-text 인덱스, 복합 인덱스, Foreign Key 인덱스
- **캐싱**: 네이버 API 결과 캐싱, 학교/학과 데이터 캐싱
- **페이징**: Spring Data 페이징, countQuery 분리
- **검색 성능**: MySQL ngram 파서, 검색어 정규화

✅ **UI/UX 완성도**:
- **반응형**: Bootstrap Grid, 모바일 친화적 디자인
- **실시간 검증**: 이메일 중복/비밀번호 규칙/폼 검증
- **Ajax 인터랙션**: 찜하기/상태 변경/검색/알림
- **사용자 경험**: 로딩 상태/에러 메시지/성공 피드백
- **접근성**: 키보드 네비게이션/스크린 리더 지원

💡 핵심 원칙 (Day 8 확정)
1. **Entity-DTO 분리**: Entity는 View에 직접 노출하지 않음 (항상 DTO 사용)
2. **설정 관리**: 모든 설정값은 application.yml, 민감정보는 application-local.yml
3. **성능 우선**: N+1 쿼리 방지, Fetch Join, 캐싱, 인덱스 최적화
4. **사용자 경험**: 실시간 검증, AJAX 인터랙션, 로딩 상태 표시
5. **예외 처리**: 구체적인 커스텀 예외, 글로벌 예외 핸들러
6. **코드 일관성**: 상수 중앙 관리 (AppConstants, Messages), 네이밍 컨벤션
7. **보안 강화**: Spring Security, CSRF 보호, 입력 검증, Rate Limiting
8. **테스트 가능성**: Service 계층 단위 테스트, API 통합 테스트

🔧 **Day 8까지의 핵심 패턴**:
- **Repository 패턴**: Fetch Join 쿼리, countQuery 분리, 복합 인덱스
- **비동기 처리**: @Async 서비스, SSE 실시간 통신, 이메일 발송
- **캐싱 전략**: Spring Cache, API 결과 캐싱, 성능 최적화
- **UI 재사용**: pageType 매개변수, 템플릿 다용도 활용
- **전역 모듈**: 공통 JavaScript, CSRF 토큰 관리, 에러 처리
- **검색 최적화**: MySQL Full-text Search, ngram 파서, 검색어 정규화

📝 Next Steps (Day 9+)
- **실시간 채팅**: Firebase SDK 통합, Firestore 실시간 리스너
- **성능 모니터링**: 응답 시간 측정, DB 쿼리 분석, 메모리 사용량
- **테스트 커버리지**: Service 계층 80% 이상, 주요 API 엔드포인트 100%
- **보안 강화**: SQL Injection 방지, XSS 보호, 파일 업로드 검증
- **배포 자동화**: Docker 컨테이너, CI/CD 파이프라인, 모니터링 설정