Claude Code Instructions - Unibook Project
🎯 Project Overview
Unibook: 대학생 맞춤형 교재 거래 플랫폼

핵심 기능: 학교-학과-교수-과목별 교재 연관 검색 및 거래
개발 기간: 2주 (1주차: 핵심 기능, 2주차: 고도화)
기술 스택: Spring Boot 3.4.6, Java 21, MySQL 8.0+, JPA, Thymeleaf, Bootstrap 5

🔧 Exact Version Configuration
Critical: Use these exact versions to avoid conflicts

Spring Boot: 3.4.6 (latest stable)
Java: 21 (LTS, optimized for Spring Boot 3.4.6)
Gradle: 8.4+ (compatible with Spring Boot 3.4.6)
MySQL: 8.0+ (tested compatibility)

📍 Development Environment

Path: /mnt/c/dev/unibook (Industry standard structure)
IDE: IntelliJ IDEA (Windows) - Project already created
Database: MySQL running on Windows/WSL

✅ CONFIRMED PROJECT SETUP (Already Completed via IntelliJ)
Spring Initializr Configuration Used:

Location: C:\dev
Name: unibook
Language: Java
Type: Gradle - Groovy
Group: com.unibook
Artifact: unibook
Package: com.unibook
Packaging: Jar (Spring Boot standard)
Java: 21
Spring Boot: 3.4.6

Dependencies Included:

Spring Web
Spring Data JPA
MySQL Driver
Spring Security
Thymeleaf
Validation
Spring Boot DevTools
Java Mail Sender

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

Day 1-2: Project setup + Entity classes + Basic CRUD
Day 3: Authentication system (signup/login)
Day 4: Email verification with university domain validation
Day 5: Post CRUD with image upload
Day 6: Advanced search functionality (PROJECT CORE)
Day 7: Integration testing and UI improvement

Week 2: Advanced Features

Day 8: Wishlist + Notification system
Day 9-10: Firebase real-time chat
Day 11: Advanced features (view count, user profile)
Day 12: UI/UX improvements
Day 13: Testing and bug fixes
Day 14: Deployment preparation

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
sqlCREATE DATABASE unibook_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
application.yml template:
yamlspring:
  datasource:
    url: jdbc:mysql://localhost:3306/unibook_db
    username: [TO_BE_CONFIRMED]
    password: [TO_BE_CONFIRMED]
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop  # Change to 'update' after first run
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
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
Dependencies (already configured)
gradledependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    runtimeOnly 'com.mysql:mysql-connector-j'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
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
🚨 Common Issues & Solutions
If these errors occur:

"Could not resolve dependencies"

Check internet connection
Try ./gradlew clean build --refresh-dependencies


MySQL Connection Error

Verify MySQL is running: sudo service mysql status
Check database exists: SHOW DATABASES;
Verify user permissions


Java Version Mismatch

Confirm Java 21: java -version
Set JAVA_HOME if needed


Port Already in Use (8080)

Kill process: sudo lsof -t -i tcp:8080 | xargs kill -9
Or change port in application.yml



📋 Daily Checkpoints - Verify Completion
Day 1 Success Criteria:

 Spring Boot project runs without errors
 MySQL connection successful
 Basic Entity classes created
 Can access http://localhost:8080

Day 2-3 Success Criteria:

 User registration works
 Password encryption working
 Login/logout functional
 Basic Thymeleaf pages render

Day 4 Success Criteria:

 University email validation working
 Actual email sent and received
 Email verification completes

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
After IntelliJ project creation, use this command:
bashcd /mnt/c/dev/unibook
claude-code "IntelliJ로 Spring Boot 프로젝트가 생성된 상태야. Day 1 작업을 시작해줘: 
1. application.properties를 application.yml로 변경하고 MySQL 설정 추가
2. 패키지 구조 생성 (controller, service, repository, domain/entity)  
3. 기본 Entity 클래스들 생성 (User, School, Department, Professor, Subject, Book, Post)
4. MySQL 연결 테스트까지 완료해줘"
📋 Day 1 Success Criteria

 application.yml 설정 완료 (MySQL 연결)
 기본 패키지 구조 생성
 핵심 Entity 클래스 7개 생성
 기본 Repository 인터페이스 생성
 Spring Boot 애플리케이션 정상 실행 (포트 8080)
 MySQL 데이터베이스 연결 확인

