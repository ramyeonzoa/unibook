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

Spring Boot: 3.5.0 (실제 사용 중인 버전)
Java: 21 (LTS, optimized for Spring Boot 3.5.0)
Gradle: 8.4+ (compatible with Spring Boot 3.5.0)
MySQL: 8.0+ (Windows localhost:3306)
Lombok: 추가됨 (build.gradle에 compileOnly, annotationProcessor)

📍 Development Environment

Path: /mnt/c/dev/unibook (Windows C:\dev\unibook)
IDE: IntelliJ IDEA (Windows) - ⚠️ MUST run from IntelliJ, NOT WSL terminal
Database: MySQL running on Windows (localhost:3306, username: root, password: 1234)
WSL: Used for Claude Code and git operations only
Execution: Spring Boot MUST be run from IntelliJ IDEA

✅ Day 1-2 COMPLETED (2025년 1월 25-26일)

📋 Day 1 완료 사항:
1. Spring Boot 프로젝트 초기 설정
2. 8개 Entity 클래스 생성 (User, School, Department, Professor, Subject, Book, Post, SubjectBook)
3. 8개 Repository 인터페이스 생성
4. MySQL 데이터베이스 생성 (unibook_db)
5. application.yml 설정 완료
6. GitHub 저장소 생성 및 초기 커밋

📋 Day 2 완료 사항:
1. Lombok 의존성 추가 (Annotation Processing 활성화 필요)
2. Service 계층 구현 (UserService, SchoolService, PostService, BookService)
3. HomeController + index.html 메인 페이지 (Bootstrap 5)
4. DataInitializer로 CSV 데이터 로드:
   - 학교: 400개 (univ-email-250411-final.csv)
   - 학과: 12,870개 (univ-dept-mapped.csv)
5. 개선사항 적용:
   - N+1 문제 해결 (Fetch Join)
   - 데이터 무결성 보장 (@Transactional 강화)
   - 매직넘버 제거 (application.yml 설정)
   - 자동완성 검색 UI (jQuery UI Autocomplete)
   - 캐싱 적용 (@EnableCaching)
   - 파일 업로드 준비 (FileUploadConfig, FileUploadUtil)

🎯 Claude Code Starting Point
Current Status: Spring Boot project structure created by IntelliJ
Next Action: Add Entity classes, configure application.yml, set up basic structure
Project structure should now exist:
/mnt/c/dev/unibook/
├── src/main/java/com/unibook/
│   └── UnibookApplication.java
├── src/main/resources/
│   └── application.properties  # Need to convert to .yml
├── build.gradle
└── gradlew
🏗️ Project Structure (Spring Boot Best Practices)
unibook/
├── src/main/java/com/unibook/
│   ├── config/           # Security, Database config
│   ├── controller/       # @Controller classes
│   ├── service/          # @Service classes (business logic)
│   ├── repository/       # @Repository interfaces  
│   ├── domain/
│   │   ├── entity/       # JPA @Entity classes
│   │   └── dto/          # Data Transfer Objects
│   └── util/            # Utility classes
├── src/main/resources/
│   ├── templates/        # Thymeleaf .html files
│   ├── static/
│   │   ├── css/         # Bootstrap + custom CSS
│   │   ├── js/          # JavaScript files
│   │   └── images/      # Static images
│   └── application.yml   # Configuration
└── uploads/             # User uploaded files
    └── images/posts/    # Post images
📋 Development Schedule
Week 1: Core Features

✅ Day 1-2: Project setup + Entity classes + Basic CRUD
☐ Day 3: Authentication system (signup/login)
☐ Day 4: Email verification with university domain validation
☐ Day 5: Post CRUD with image upload
☐ Day 6: Advanced search functionality (PROJECT CORE)
☐ Day 7: Integration testing and UI improvement

Week 2: Advanced Features

☐ Day 8: Wishlist + Notification system
☐ Day 9-10: Firebase real-time chat
☐ Day 11: Advanced features (view count, user profile)
☐ Day 12: UI/UX improvements
☐ Day 13: Testing and bug fixes
☐ Day 14: Deployment preparation

⚠️ REQUIRED CONFIRMATIONS - Ask Before Proceeding
Claude Code should ask user to confirm these before starting:

MySQL Database Setup

Is MySQL installed and running?
Database name: unibook_db
Username/password for connection
Port: 3306 (default)


Email Service Setup (Day 4 needed)

Gmail account for SMTP (app password required)
User has university domain CSV file ready


File Paths

Confirm project location: /mnt/c/Users/[username]/Desktop/unibook
Where to store uploaded images: /uploads/images/


Spring Initializr Configuration

Group: com.unibook
Artifact: unibook
Package: com.unibook
Dependencies: Web, JPA, MySQL, Security, Thymeleaf, Validation, Mail, DevTools



🗄️ MySQL Database Schema
Create database first:
```sql
CREATE DATABASE unibook_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

✅ Current application.yml (Day 2 완료):
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/unibook_db
    username: root
    password: 1234
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update  # Day 2부터 update로 변경됨
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=300s
      
app:
  home:
    popular-books-limit: 8
    recent-posts-limit: 5
  file:
    upload-dir: uploads/
    max-size: 10485760  # 10MB
    allowed-extensions: jpg,jpeg,png,gif
```
🎯 Key Features to Implement
1. University Email Verification

CSV file with university email domains (e.g., snu.ac.kr)
Validate email domain before sending verification
Only allow verified university students

2. Advanced Search System (PROJECT CORE)
Book detail page should show:

"Courses using this book" section
Grouped by: School → Department → Professor → Subject
"Show only my university" filter option
Clickable navigation:

Click department → show all textbooks for that department
Click professor → show all textbooks for that professor
Click subject → show all textbooks for that subject



3. Transaction History

Show previous transaction prices for same textbook
Display completed transaction history

🔧 Technical Requirements
Dependencies (Day 2 업데이트됨):
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.boot:spring-boot-starter-cache'  // Day 2 추가
    implementation 'com.github.ben-manes.caffeine:caffeine'  // Day 2 추가
    runtimeOnly 'com.mysql:mysql-connector-j'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    
    // Lombok - Day 2 추가
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```
Package Structure
src/main/java/com/unibook/
├── config/          # Security, Database config
├── domain/
│   ├── entity/      # JPA entities
│   └── dto/         # Data transfer objects
├── repository/      # JPA repositories
├── service/         # Business logic
├── controller/      # Web controllers
└── util/           # Utility classes
🎨 Frontend Approach

Template Engine: Thymeleaf
CSS Framework: Bootstrap 5
JavaScript: Vanilla JS + AJAX for dynamic content
Image Upload: Local file storage (path stored in DB)

🚨 Priority Guidelines

MUST HAVE: Authentication + Post CRUD + Advanced search
SHOULD HAVE: Wishlist + Basic notifications
NICE TO HAVE: Real-time chat + Advanced UI

📝 Development Notes

Focus on robust search functionality - this is the core differentiator
Use Spring Data JPA for complex queries with multiple joins
Implement proper validation for university email domains
Plan for scalability but implement MVP first

🔍 Key Queries to Implement
java// Find subjects that use a specific book
List<Subject> findSubjectsByBooks_BookId(Long bookId);

// Find posts by professor
List<Post> findByBook_Subjects_Professor_ProfessorId(Long professorId);

// Find posts by department
List<Post> findByBook_Subjects_Professor_Department_DepartmentId(Long departmentId);

// Complex search with multiple filters
List<Post> findByBook_Subjects_Professor_Department_School_SchoolId(Long schoolId);
🚨 Common Issues & Solutions (Day 1-2 경험 기반)

1. **Lombok 컴파일 오류** ⚠️ 자주 발생
   - 문제: cannot find symbol (getter/setter)
   - 해결: IntelliJ → Settings → Build → Compiler → Annotation Processors → Enable annotation processing
   - 추가: build.gradle에 compileOnly와 annotationProcessor 둘 다 필요

2. **Spring Boot 실행 위치 문제** ⚠️ 중요
   - 문제: WSL에서 실행 시 Windows MySQL 연결 불가
   - 해결: 반드시 IntelliJ IDEA에서 실행 (WSL은 git/Claude Code 작업만)
   - Windows MySQL은 localhost:3306에서 실행 중

3. **application.yml 구조 오류**
   - 문제: mapping values are not allowed here (duplicate key)
   - 해결: YAML 들여쓰기 확인, 중복 키 제거
   ```yaml
   spring:  # 이 키가 중복되지 않도록 주의
     datasource:
       url: ...
     jpa:
       hibernate:
   ```

4. **Map.of() 타입 추론 오류**
   - 문제: Java cannot infer type arguments for Map<>
   - 해결: HashMap 사용으로 변경
   ```java
   // 문제: Map.of("id", id, "text", text)
   // 해결:
   Map<String, Object> item = new HashMap<>();
   item.put("id", school.getSchoolId());
   item.put("text", school.getSchoolName());
   ```

5. **N+1 쿼리 문제**
   - 문제: Lazy Loading으로 인한 추가 쿼리 발생
   - 해결: Fetch Join 사용
   ```java
   @Query("SELECT p FROM Post p " +
          "LEFT JOIN FETCH p.user u " +
          "LEFT JOIN FETCH u.school")
   ```



📋 Daily Checkpoints - Verify Completion
Day 1 Success Criteria:

✅ Spring Boot project runs without errors
✅ MySQL connection successful
✅ Basic Entity classes created (8개)
✅ Can access http://localhost:8080
✅ GitHub repository 생성 및 push 완료

Day 2 Success Criteria:

✅ Service 계층 구현 (4개 서비스)
✅ HomeController + index.html 메인 페이지
✅ CSV 데이터 로드 (학교 400개, 학과 12,870개)
✅ Bootstrap 5 UI 적용
✅ 성능 최적화 (Fetch Join, 캐싱)
✅ 자동완성 검색 구현
✅ 파일 업로드 준비 완료

Day 3 Success Criteria:

☐ Spring Security 설정 (메인 페이지 public 접근)
☐ User registration works
☐ Password encryption working (BCrypt)
☐ Login/logout functional
☐ Basic Thymeleaf pages render

Day 4 Success Criteria:

☐ University email validation working
☐ Actual email sent and received
☐ Email verification completes

🎯 Key Implementation Notes
Entity Relationships (Critical for Search Feature)
java// Subject_Book junction table is KEY for advanced search
@Entity
public class SubjectBook {
    @ManyToOne
    private Subject subject;
    
    @ManyToOne  
    private Book book;
    
    // This enables: "What subjects use this book?"
    // And: "What books are used in this subject?"
}
📧 Gmail App Password Setup (Day 4)
Gmail 보안 설정으로 인해 일반 비밀번호로는 SMTP 접근 불가
Day 4에 필요한 설정:

Google 계정 → 보안 → 2단계 인증 활성화
앱 비밀번호 생성 → "Mail" 선택
생성된 16자리 비밀번호를 application.yml에 설정

📊 CSV Data Processing Strategy
School Entity Initialization
java// Handle multiple domains per school
@Entity
public class School {
    private String schoolName;
    private String primaryDomain;  // 첫 번째 도메인
    
    @ElementCollection
    private Set<String> allDomains;  // 모든 도메인들
}

// Email validation logic
public boolean isValidUniversityEmail(String email) {
    String domain = email.substring(email.indexOf("@") + 1);
    return schoolRepository.existsByAllDomainsContaining(domain);
}
Data Loading Priority

Day 2: Load univ-email-250411-final.csv → School entities
Day 2: Load univ-department-mapped.csv → Department entities
Day 4: Use domains for email validation before sending verification

File Upload Structure
/uploads/
  /images/
    /posts/
      /{postId}/
        - main.jpg (대표 이미지)
        - sub1.jpg, sub2.jpg... (추가 이미지)
🔄 Development Flow

Always test after each major feature
Commit frequently with clear messages
Ask user for confirmation before major changes
If stuck, provide multiple solution options

🚀 Claude Code Starting Commands

Day 3 시작 명령어:
```bash
cd /mnt/c/dev/unibook
claude-code "Day 2까지 완료된 상태야. Day 3 작업을 시작해줘:
1. Spring Security 설정 - 메인 페이지는 public 접근 허용
2. User 회원가입 기능 (비밀번호 BCrypt 암호화)
3. 로그인/로그아웃 구현
4. 회원가입 페이지와 로그인 페이지 생성
5. 학교 선택은 자동완성 검색 활용"
```

📝 Day 1-2 핵심 교훈 (Future Sessions 필독)

1. **Lombok은 IntelliJ 설정 필수**
   - Annotation Processing 활성화하지 않으면 컴파일 에러
   - @Data, @Getter, @Setter 사용으로 코드 간결화

2. **성능은 처음부터 고려**
   - N+1 문제는 Fetch Join으로 해결
   - 400개 학교는 드롭다운보다 자동완성 검색이 적합
   - 캐싱으로 반복 조회 최적화

3. **설정값은 하드코딩 금지**
   - application.yml에 설정값 분리
   - @Value 또는 @ConfigurationProperties 활용

4. **데이터 초기화는 트랜잭션으로**
   - @Transactional(rollbackFor = Exception.class)
   - 부분 실패 방지, 데이터 무결성 보장

5. **파일 업로드는 보안 우선**
   - 확장자 검증, 파일 크기 제한
   - 업로드 경로는 웹루트 외부에

💡 Day 3 주의사항
- Spring Security 추가 시 모든 페이지가 로그인 필요하게 됨
- 메인 페이지("/")는 permitAll() 설정 필수
- 정적 리소스(/css, /js, /images)도 permitAll() 필요
- CSRF 토큰 처리 주의 (Thymeleaf는 자동 처리)

🛠️ Day 2 구현 세부사항 (참고용)

**자동완성 검색 구현:**
```javascript
// index.html
$("#schoolSearch").autocomplete({
    source: "/api/schools/search",
    minLength: 2,
    select: function(event, ui) {
        window.location.href = "/schools/" + ui.item.id;
    }
});
```

**Fetch Join 쿼리:**
```java
// PostRepository.java
@Query("SELECT p FROM Post p " +
       "LEFT JOIN FETCH p.user u " +
       "LEFT JOIN FETCH u.school " +
       "LEFT JOIN FETCH p.book " +
       "ORDER BY p.createdAt DESC")
List<Post> findRecentPostsWithDetails(Pageable pageable);
```

**캐싱 설정:**
```java
// UnibookApplication.java
@EnableCaching
@SpringBootApplication
public class UnibookApplication {
    // ...
}

// SchoolService.java
@Cacheable("schools")
public List<School> searchSchools(String query) {
    // ...
}
```

**파일 업로드 유틸리티:**
```java
// FileUploadUtil.java
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

📌 프로젝트 현재 상태 요약
- Spring Boot 3.5.0 + Java 21 + MySQL 8.0
- 8개 Entity, 8개 Repository, 4개 Service 구현 완료
- 메인 페이지 UI 완성 (Bootstrap 5)
- 학교/학과 데이터 12,000+ 로드 완료
- 성능 최적화 및 보안 기초 작업 완료
- Day 3부터 인증/인가 시스템 구현 예정

