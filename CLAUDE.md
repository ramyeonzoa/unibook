Claude Code Instructions - Unibook Project
🎯 Project Overview
Unibook: 대학생 맞춤형 교재 거래 플랫폼

핵심 기능: 학교-학과-교수-과목별 교재 연관 검색 및 거래
개발 기간: 2주 (1주차: 핵심 기능, 2주차: 고도화)
기술 스택: Spring Boot 3.5.0, Java 21, MySQL 8.0+, JPA, Thymeleaf, Bootstrap 5

📅 프로젝트 시작일: 2025년 1월 25일
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

✅ Day 1-3 COMPLETED (2025년 1월 25-26일)

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

📋 Development Schedule

Week 1: Core Features
✅ Day 1-2: Project setup + Entity classes + Basic CRUD
✅ Day 3: Authentication system (signup/login)
☐ Day 4: Email verification with university domain validation
☐ Day 5: Post CRUD with image upload
☐ Day 6: Advanced search functionality (PROJECT CORE)
☐ Day 7: Integration testing and UI improvement

Week 2: Advanced Features
☐ Day 8: Wishlist + Notification system
☐ Day 9-10: Firebase real-time chat (결정됨: Firebase 사용)
☐ Day 11: Advanced features (view count, user profile)
☐ Day 12: UI/UX improvements
☐ Day 13: Testing and bug fixes
☐ Day 14: Deployment preparation (플랫폼 미정 - 구현 후 결정)

🏗️ Current Project Structure
unibook/
├── src/main/java/com/unibook/
│   ├── common/          # AppConstants, Messages (NEW)
│   ├── config/          # SecurityConfig, JpaAuditConfig, DataInitializer
│   ├── controller/      # HomeController, AuthController, GlobalExceptionHandler
│   │   └── api/        # SchoolApiController, DepartmentApiController
│   ├── domain/
│   │   ├── entity/     # 12개 Entity (모두 BaseEntity 상속)
│   │   └── dto/        # DTO 클래스들
│   ├── exception/       # 커스텀 예외 클래스들 (NEW)
│   │   ├── BusinessException (기본)
│   │   ├── ValidationException (검증)
│   │   ├── ResourceNotFoundException (404)
│   │   ├── AuthenticationException (인증)
│   │   └── DataInitializationException (초기화)
│   ├── repository/      # JPA Repository 인터페이스
│   ├── security/        # UserPrincipal, CustomUserDetailsService
│   ├── service/         # 비즈니스 로직 서비스
│   └── util/           # FileUploadUtil 등
└── src/main/resources/
    ├── templates/       # Thymeleaf 템플릿
    │   └── auth/       # signup.html, login.html
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

4. **Book Entity**
- isbn, publicationYear, originalPrice 필드 필수
- year → publicationYear으로 변경

5. **PostImage Entity**
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

⚠️ CONFIRMATIONS - Day 4 이후 필요 사항

☐ Day 4에 필요한 설정:
- Gmail SMTP 계정 (앱 비밀번호 필요)
- 이메일 인증 토큰 저장 방식 결정

☐ Day 5에 필요한 설정:
- 파일 업로드 경로: /uploads/images/posts/ (설정 완료, 폴더 생성 필요)
- 이미지 리사이징 라이브러리 결정

☐ Day 6에 필요한 설정:
- 네이버 책 검색 API (Client ID/Secret)
- MySQL Full-text search 설정 (결정됨: Elasticsearch 대신 MySQL 사용)

🎯 Key Features to Implement (Day 4-14)

1. **Day 4: University Email Verification**
- Gmail SMTP 설정 (앱 비밀번호)
- 인증 토큰 생성 및 저장
- 인증 메일 템플릿
- 토큰 만료 처리
- 재발송 기능

2. **Day 5: Post CRUD with Image Upload**
- 게시글 작성 폼
- 다중 이미지 업로드 (최대 5개)
- 이미지 순서 변경
- 썸네일 생성
- 게시글 수정/삭제

3. **Day 6: Advanced Search System (PROJECT CORE)**
- 교재 상세 페이지
- "이 책을 사용하는 과목" 섹션
- 학교 → 학과 → 교수 → 과목 계층 구조
- "우리 학교만 보기" 필터
- 클릭 가능한 네비게이션
- 검색 히스토리

4. **Day 7: Integration Testing & UI**
- 전체 기능 통합 테스트
- UI/UX 개선
- 반응형 디자인 점검
- 성능 최적화

5. **Day 8: Wishlist + Notification**
- 찜하기 기능
- 실시간 알림 (SSE 또는 WebSocket)
- 알림 설정 페이지

6. **Day 9-10: Real-time Chat**
- Firebase 설정 (결정됨: Firebase 사용)
- 1:1 채팅
- 채팅방 목록
- 읽음 표시
- 이미지 전송

7. **Day 11: Advanced Features**
- 조회수 증가 (중복 방지)
- 사용자 프로필 페이지
- 거래 후기
- 신고 기능

8. **Day 12: UI/UX Improvements**
- 디자인 시스템 통일
- 다크 모드
- 접근성 개선
- 로딩 상태 표시

9. **Day 13: Testing & Bug Fixes**
- 단위 테스트
- 통합 테스트
- 버그 수정
- 보안 점검

10. **Day 14: Deployment Preparation**
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

📧 Gmail App Password Setup (Day 4)
Gmail 보안 설정으로 인해 일반 비밀번호로는 SMTP 접근 불가
1. Google 계정 → 보안 → 2단계 인증 활성화
2. 앱 비밀번호 생성 → "Mail" 선택
3. 생성된 16자리 비밀번호를 application.yml에 설정

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

📌 현재 프로젝트 상태 (Day 3 완료 + 대규모 리팩터링)

✅ 완료된 기능:
- 전체 인증 시스템 (회원가입/로그인/로그아웃)
- DTO 패턴 전면 적용
- 실시간 폼 검증
- 로그인 상태별 UI 분기
- 학교-학과 자동완성 검색
- BaseEntity 기반 감사(Audit) 기능

✅ Day 3 이후 추가 리팩터링:
- 보안 강화: 세션 고정 공격 방어, 동시 로그인 차단
- 성능 개선: BookService 쿼리 최적화, 인덱스 추가, N+1 해결
- 예외 처리: 커스텀 예외 클래스 체계 구축
- 트랜잭션: 동시성 제어 (SERIALIZABLE)
- AuditorAware: 0L = 시스템 사용자 정의
- 코드 정리: Magic Number/String → 상수화

⏳ 다음 단계 (Day 4):
- 이메일 인증 시스템
- 대학 이메일 도메인 엄격 검증
- 비밀번호 재설정 기능

💡 핵심 원칙
1. Entity는 View에 직접 노출하지 않음 (항상 DTO 사용)
2. 모든 설정값은 application.yml에서 관리
3. 비밀번호 등 민감정보는 application-local.yml에
4. 성능 문제는 처음부터 고려 (Fetch Join, 캐싱, 인덱스)
5. 사용자 경험 우선 (실시간 검증, 자동완성)
6. 예외는 구체적으로 (커스텀 예외 사용)
7. 상수는 중앙 관리 (AppConstants, Messages)

🚀 Day 4 시작 명령어
```bash
cd /mnt/c/dev/unibook
claude-code "Day 3까지 완료된 상태야. CLAUDE.md 참고해서 Day 4 작업을 시작해줘:
1. Gmail SMTP 설정
2. 이메일 인증 토큰 Entity 생성
3. 인증 메일 발송 서비스
4. 인증 링크 처리 Controller
5. 대학 이메일 도메인 검증 강화"
```

📝 추가 고려사항
- 모바일 반응형 디자인 (Day 7, 12)
- SEO 최적화 (Day 12)
- 접근성 (WCAG 2.1) 준수 (Day 12)
- 성능 모니터링 도구 (Day 14)
- 에러 추적 시스템 (Day 14)