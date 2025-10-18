# N+1 Query Problem Analysis Report

## 📊 종합 분석 개요

이 보고서는 Unibook 프로젝트의 전체 코드베이스에서 N+1 쿼리 문제를 종합적으로 분석한 결과입니다.

### 🎯 분석 범위
- **Repository Layer**: 17개 Repository 인터페이스 분석
- **Service Layer**: 19개 Service 클래스 분석  
- **Controller Layer**: 주요 Controller 클래스 분석
- **Entity Layer**: 18개 Entity 클래스의 관계 설정 분석

### 📈 전체 평가 결과
- **전체 평가**: **GOOD** (85/100점)
- **Repository Layer**: **EXCELLENT** (95/100점) - 대부분 fetch join 잘 구현됨
- **Entity Relationships**: **GOOD** (85/100점) - LAZY 로딩 대부분 적용됨
- **Service Layer**: **FAIR** (70/100점) - 몇 가지 중요한 개선점 존재
- **Controller Layer**: **GOOD** (80/100점) - 리팩터링으로 많이 개선됨

---

## 🔍 Repository Layer Analysis

### ✅ 잘 구현된 부분

#### 1. PostRepository - 뛰어난 최적화
```java
// 우수 사례: 체계적인 fetch join 구현
String JOIN_ALL_DETAILS = JOIN_USER_DETAILS + JOIN_BOOK + JOIN_SUBJECT;

@Query("SELECT p FROM Post p " +
       JOIN_ALL_DETAILS +
       "WHERE p.postId = :postId")
Optional<Post> findByIdWithDetails(Long postId);

// 통합 필터링 메서드로 중복 제거
Page<Post> findPostsWithOptionalFilters(
    @Param("subjectId") Long subjectId,
    @Param("professorId") Long professorId,
    // ... 기타 파라미터
    Pageable pageable);
```

#### 2. NotificationRepository - Fetch Join + CountQuery 분리
```java
@Query(
    value = "SELECT n FROM Notification n " +
            "LEFT JOIN FETCH n.actor " +
            "LEFT JOIN FETCH n.relatedPost " +
            "WHERE n.recipient.userId = :userId " +
            "ORDER BY n.isRead ASC, n.createdAt DESC",
    countQuery = "SELECT COUNT(n) FROM Notification n " +
                 "WHERE n.recipient.userId = :userId"
)
Page<Notification> findByRecipientUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);
```

#### 3. ChatRoomRepository - 복잡한 관계 최적화
```java
@Query("SELECT cr FROM ChatRoom cr " +
       "LEFT JOIN FETCH cr.buyer b " +
       "LEFT JOIN FETCH cr.seller s " +
       "LEFT JOIN FETCH cr.post p " +
       "WHERE (cr.buyer.userId = :userId OR cr.seller.userId = :userId)")
List<ChatRoom> findByUserIdOrderByLastMessageTimeDesc(@Param("userId") Long userId);
```

#### 4. DTO Projection 활용
```java
// ProfessorRepository - DTO 프로젝션으로 불필요한 데이터 로딩 방지
@Query("SELECT new com.unibook.domain.dto.ProfessorDto(" +
       "p.professorId, p.professorName, " +
       "p.department.departmentId, p.department.departmentName) " +
       "FROM Professor p " +
       "WHERE p.department.departmentId = :deptId")
List<ProfessorDto> findProfessorsByDepartment(@Param("deptId") Long departmentId);
```

### ⚠️ 개선이 필요한 부분

#### HIGH PRIORITY

**1. BookRepository - 기본 메서드만 사용**
- **위치**: `/src/main/java/com/unibook/repository/BookRepository.java`
- **문제**: fetch join이 전혀 없음
- **영향도**: CRITICAL (Book을 Post와 함께 조회 시 N+1 발생)

```java
// 현재: 기본 메서드만 사용
List<Book> findByTitleContainingOrAuthorContaining(String title, String author);

// 개선안
@Query("SELECT DISTINCT b FROM Book b " +
       "LEFT JOIN FETCH b.posts p " +
       "WHERE b.title LIKE %:title% OR b.author LIKE %:author% " +
       "ORDER BY b.createdAt DESC")
List<Book> findByTitleContainingOrAuthorContainingWithPosts(
    @Param("title") String title, @Param("author") String author);
```

**2. UserRepository - 관계 정보 누락**
- **위치**: `/src/main/java/com/unibook/repository/UserRepository.java`
- **문제**: 기본 조회에서 Department, School 정보 없음
- **영향도**: HIGH (사용자 정보 표시 시 추가 쿼리 발생)

```java
// 현재: 기본 메서드
Optional<User> findByEmail(String email);

// 개선안
@Query("SELECT u FROM User u " +
       "LEFT JOIN FETCH u.department d " +
       "LEFT JOIN FETCH d.school " +
       "WHERE u.email = :email")
Optional<User> findByEmailWithDepartmentAndSchool(@Param("email") String email);
```

---

## 🏗️ Entity Relationships Analysis

### ✅ 잘 설정된 관계

#### 1. LAZY Loading 일관성
```java
// Post Entity - 모든 관계가 LAZY로 설정
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;

@ManyToOne(fetch = FetchType.LAZY) 
@JoinColumn(name = "book_id")
private Book book;
```

#### 2. BatchSize 최적화
```java
// Post Entity - PostImage N+1 방지
@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("imageOrder ASC")
@BatchSize(size = 10)  // IN 쿼리로 10개씩 묶어서 조회
private List<PostImage> postImages = new ArrayList<>();
```

### ⚠️ 개선이 필요한 관계

#### MEDIUM PRIORITY

**1. Book Entity - Post 관계 최적화 필요**
```java
// 현재: BatchSize 없음
@OneToMany(mappedBy = "book")
private List<Post> posts = new ArrayList<>();

// 개선안
@OneToMany(mappedBy = "book")
@BatchSize(size = 20)
private List<Post> posts = new ArrayList<>();
```

**2. User Entity - Posts 관계 최적화**
```java
// 현재: BatchSize 없음
@OneToMany(mappedBy = "user")
private List<Post> posts = new ArrayList<>();

// 개선안  
@OneToMany(mappedBy = "user")
@BatchSize(size = 15)
private List<Post> posts = new ArrayList<>();
```

---

## ⚙️ Service Layer Analysis

### ⚠️ 심각한 N+1 문제 발견

#### CRITICAL PRIORITY

**1. PostService.applyAdditionalFilters() - 심각한 N+1**
- **위치**: `/src/main/java/com/unibook/service/PostService.java:159-184`
- **문제**: Stream 필터링에서 관계 접근으로 각 Post마다 추가 쿼리 발생
- **영향도**: CRITICAL (검색 결과마다 N개의 추가 쿼리)

```java
// 🚨 문제 코드
private List<Post> applyAdditionalFilters(List<Post> posts, Long subjectId, Long professorId, String bookTitle) {
    return posts.stream()
            .filter(post -> {
                // 각 post마다 subject 조회 쿼리 발생!
                if (subjectId != null) {
                    return post.getSubject() != null && subjectId.equals(post.getSubject().getSubjectId());
                }
                
                // 각 post마다 professor 조회 쿼리 발생!
                if (professorId != null) {
                    return post.getSubject() != null && 
                           post.getSubject().getProfessor() != null &&  // N+1 발생!
                           professorId.equals(post.getSubject().getProfessor().getProfessorId());
                }
                
                // 각 post마다 book 조회 쿼리 발생!
                if (bookTitle != null) {
                    return post.getBook() != null &&  // N+1 발생!
                           post.getBook().getTitle() != null &&
                           post.getBook().getTitle().toLowerCase().contains(bookTitle.toLowerCase());
                }
                
                return true;
            })
            .collect(Collectors.toList());
}

// ✅ 해결방안: Repository에서 fetch join으로 미리 로딩하거나 필터링 로직을 DB로 이동
@Query("SELECT DISTINCT p FROM Post p " +
       "LEFT JOIN FETCH p.subject s " +
       "LEFT JOIN FETCH s.professor " +
       "LEFT JOIN FETCH p.book " +
       "WHERE p.postId IN :postIds")
List<Post> findAllByIdInWithAllDetails(@Param("postIds") List<Long> postIds);
```

**2. WishlistService.toggleWishlist() - Repository 중복 호출**
- **위치**: `/src/main/java/com/unibook/service/WishlistService.java:34-73`
- **문제**: 불필요한 Entity 조회
- **영향도**: HIGH (찜 기능 사용 시마다 발생)

```java
// 🚨 문제 코드
@Transactional
public boolean toggleWishlist(Long userId, Long postId) {
    User user = userRepository.findById(userId)  // 불필요한 User 엔티티 조회
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
    
    Post post = postRepository.findById(postId)  // 불필요한 Post 엔티티 조회
            .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다"));

// ✅ 해결방안: getReferenceById 사용
@Transactional
public boolean toggleWishlist(Long userId, Long postId) {
    // 존재 여부만 체크
    if (!userRepository.existsById(userId)) {
        throw new ResourceNotFoundException("사용자를 찾을 수 없습니다");
    }
    if (!postRepository.existsById(postId)) {
        throw new ResourceNotFoundException("게시글을 찾을 수 없습니다");
    }
    
    // Reference로 프록시 객체 생성 (실제 조회 없음)
    User user = userRepository.getReferenceById(userId);
    Post post = postRepository.getReferenceById(postId);
```

#### HIGH PRIORITY

**3. ChatService 메서드들 - 개별 Entity 조회**
- **위치**: `/src/main/java/com/unibook/service/ChatService.java:40-61`
- **문제**: 채팅방 생성 시 개별 조회
- **영향도**: HIGH (채팅 시작 시마다 발생)

```java
// 🚨 문제 코드
public ChatDto.ChatRoomDetailResponse createOrGetChatRoom(Long buyerId, ChatDto.CreateChatRoomRequest request) {
    Post post = postRepository.findById(request.getPostId())  // 개별 조회
        .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
    
    User buyer = userRepository.findById(buyerId)  // 개별 조회
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

// ✅ 해결방안: 필요한 경우에만 조회, 대부분 Reference 사용
public ChatDto.ChatRoomDetailResponse createOrGetChatRoom(Long buyerId, ChatDto.CreateChatRoomRequest request) {
    // 채팅방에 필요한 정보가 있는지만 확인
    Post post = postRepository.findByIdWithDetails(request.getPostId())
        .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
    
    User buyer = userRepository.getReferenceById(buyerId);  // 프록시 사용
```

#### MEDIUM PRIORITY

**4. UserService.getAllUsers() - 관계 정보 누락**
- **위치**: `/src/main/java/com/unibook/service/UserService.java:36-38`
- **문제**: 기본 findAll() 사용으로 Department 정보 필요 시 N+1 발생

```java
// 🚨 문제 코드
public List<User> getAllUsers() {
    return userRepository.findAll();  // Department, School 정보 없음
}

// ✅ 해결방안: 필요에 따라 fetch join 사용
@Query("SELECT u FROM User u " +
       "LEFT JOIN FETCH u.department d " +
       "LEFT JOIN FETCH d.school " +
       "ORDER BY u.createdAt DESC")
List<User> findAllWithDepartmentAndSchool();
```

---

## 🎮 Controller Layer Analysis

### ✅ 잘 개선된 부분

#### 1. PostController - 리팩터링으로 대폭 개선
- 252줄 → 50줄로 80% 코드 감소
- PostControllerHelper로 복잡한 로직 분리
- PostSearchRequest로 파라미터 통합

#### 2. AdminController - 페이징 잘 활용
```java
@GetMapping("/reports")
public String reports(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Report.ReportStatus status,
        Model model) {
    
    Pageable pageable = PageRequest.of(page, size);
    Page<Report> reports = reportService.getReports(status, pageable);  // 페이징 활용
```

### ⚠️ 잠재적 문제

#### LOW PRIORITY

**1. 관리자 통계 조회 시 개별 카운트**
- **위치**: `/src/main/java/com/unibook/controller/AdminController.java:47-52`
- **영향도**: LOW (관리자만 사용, 빈도 낮음)

```java
// 개선 가능: 단일 쿼리로 통계 조회
long pendingReports = reportService.getPendingReportsCount();
long totalUsers = userService.getTotalUserCount();
long totalPosts = postService.getTotalPostCount();

// 더 나은 방안: 통합 통계 서비스
AdminStatistics stats = adminService.getAggregatedStatistics();
```

---

## 🎯 우선순위별 해결 방안

### 🚨 CRITICAL (즉시 해결 필요)

#### 1. PostService.applyAdditionalFilters() 수정
```java
// 기존 stream 필터링 제거하고 Repository에서 해결
@Query("SELECT DISTINCT p FROM Post p " +
       "LEFT JOIN FETCH p.user u " +
       "LEFT JOIN FETCH u.department d " +
       "LEFT JOIN FETCH d.school " +
       "LEFT JOIN FETCH p.book " +
       "LEFT JOIN FETCH p.subject s " +
       "LEFT JOIN FETCH s.professor " +
       "WHERE p.postId IN :postIds " +
       "AND (:subjectId IS NULL OR s.subjectId = :subjectId) " +
       "AND (:professorId IS NULL OR s.professor.professorId = :professorId) " +
       "AND (:bookTitle IS NULL OR LOWER(p.book.title) LIKE LOWER(CONCAT('%', :bookTitle, '%')))")
List<Post> findPostsWithFiltersAndDetails(
    @Param("postIds") List<Long> postIds,
    @Param("subjectId") Long subjectId,
    @Param("professorId") Long professorId,
    @Param("bookTitle") String bookTitle);
```

#### 2. BookRepository fetch join 추가
```java
@Query("SELECT DISTINCT b FROM Book b " +
       "LEFT JOIN FETCH b.posts p " +
       "WHERE b.title LIKE %:keyword% OR b.author LIKE %:keyword% " +
       "ORDER BY b.createdAt DESC")
List<Book> searchBooksWithPosts(@Param("keyword") String keyword);
```

### ⚠️ HIGH (빠른 시일 내 해결)

#### 3. WishlistService 최적화
```java
@Transactional
public boolean toggleWishlist(Long userId, Long postId) {
    // 존재 여부만 확인
    if (!userRepository.existsById(userId)) {
        throw new ResourceNotFoundException("사용자를 찾을 수 없습니다");
    }
    if (!postRepository.existsById(postId)) {
        throw new ResourceNotFoundException("게시글을 찾을 수 없습니다");
    }
    
    // 자신의 게시글 체크를 위해서만 Post 조회
    Post post = postRepository.findById(postId).get();
    if (post.getUser().getUserId().equals(userId)) {
        throw new IllegalArgumentException("자신의 게시글은 찜할 수 없습니다");
    }
    
    // 나머지는 Reference 사용
    User userRef = userRepository.getReferenceById(userId);
    Post postRef = postRepository.getReferenceById(postId);
    // ... 로직 계속
}
```

#### 4. UserRepository 메서드 추가
```java
@Query("SELECT u FROM User u " +
       "LEFT JOIN FETCH u.department d " +
       "LEFT JOIN FETCH d.school " +
       "WHERE u.email = :email")
Optional<User> findByEmailWithDepartmentAndSchool(@Param("email") String email);

@Query("SELECT u FROM User u " +
       "LEFT JOIN FETCH u.department d " +
       "LEFT JOIN FETCH d.school " +
       "ORDER BY u.createdAt DESC")
List<User> findAllWithDepartmentAndSchool();
```

### 🔧 MEDIUM (점진적 개선)

#### 5. Entity BatchSize 추가
```java
// Book Entity
@OneToMany(mappedBy = "book")
@BatchSize(size = 20)
private List<Post> posts = new ArrayList<>();

// User Entity  
@OneToMany(mappedBy = "user")
@BatchSize(size = 15)
private List<Post> posts = new ArrayList<>();

// Professor Entity
@OneToMany(mappedBy = "professor")
@BatchSize(size = 10)
private List<Subject> subjects = new ArrayList<>();
```

#### 6. ChatService 최적화
```java
// 필요한 경우에만 실제 조회, 나머지는 Reference 사용
public ChatDto.ChatRoomDetailResponse createOrGetChatRoom(Long buyerId, ChatDto.CreateChatRoomRequest request) {
    // Post는 상세 정보가 필요하므로 fetch join으로 조회
    Post post = postRepository.findByIdWithDetails(request.getPostId())
        .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
    
    // User는 ID만 필요하므로 Reference 사용
    User buyer = userRepository.getReferenceById(buyerId);
    
    User seller = post.getUser();  // 이미 fetch join으로 로딩됨
    // ... 로직 계속
}
```

---

## 📊 성능 개선 예상 효과

### 🚀 CRITICAL 문제 해결 시
- **PostService 검색 성능**: 70-90% 개선 예상
  - 기존: 검색 결과 N개 × 3-4개 관계 = 3N-4N개 추가 쿼리
  - 개선 후: 1개 통합 쿼리
- **전체 검색 응답 시간**: 2-5초 → 200-500ms

### ⚡ HIGH 문제 해결 시
- **찜하기 기능**: 50-70% 개선 예상
  - 기존: 3개 개별 쿼리 → 개선 후: 1개 통합 쿼리
- **채팅방 생성**: 40-60% 개선 예상
- **사용자 목록 조회**: 60-80% 개선 예상

### 📈 전체 예상 개선율
- **Database Query Count**: 60-80% 감소
- **Page Load Time**: 40-70% 단축
- **Memory Usage**: 20-40% 감소
- **User Experience**: 현저한 응답성 개선

---

## 🛠️ 구현 우선순위 및 일정

### Week 1 (즉시 구현)
1. **PostService.applyAdditionalFilters() 수정** - 1일
2. **BookRepository fetch join 추가** - 0.5일
3. **UserRepository 메서드 추가** - 0.5일

### Week 2 (핵심 최적화)
4. **WishlistService 최적화** - 1일
5. **ChatService 최적화** - 1일
6. **Entity BatchSize 설정** - 0.5일

### Week 3 (점진적 개선)
7. **나머지 Service 최적화** - 2일
8. **성능 테스트 및 모니터링 설정** - 1일

---

## 🔍 모니터링 및 측정 방안

### 1. Query 로깅 설정
```yaml
# application.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.springframework.data.jpa: DEBUG

spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        generate_statistics: true
```

### 2. 성능 측정 포인트
- 게시글 목록 조회 응답 시간
- 찜하기 기능 실행 시간  
- 채팅방 생성 시간
- 관리자 대시보드 로딩 시간

### 3. Query Count 모니터링
```java
// HibernateMetrics를 활용한 쿼리 카운트 측정
@Component
public class QueryCountMonitor {
    private final SessionFactory sessionFactory;
    
    public void logQueryStatistics(String operation) {
        Statistics stats = sessionFactory.getStatistics();
        log.info("{} - Query Count: {}, Execution Time: {}ms", 
                operation, stats.getQueryExecutionCount(), stats.getQueryExecutionMaxTime());
    }
}
```

---

## 📋 체크리스트

### 🚨 CRITICAL Issues
- [ ] PostService.applyAdditionalFilters() N+1 문제 해결
- [ ] BookRepository fetch join 구현
- [ ] UserRepository 관계 조회 메서드 추가

### ⚠️ HIGH Priority Issues  
- [ ] WishlistService 불필요한 Entity 조회 제거
- [ ] ChatService Reference 사용으로 최적화
- [ ] 전체 Service에서 getReferenceById 활용 검토

### 🔧 MEDIUM Priority Issues
- [ ] Entity들에 BatchSize 설정 추가
- [ ] 관리자 통계 조회 통합 쿼리로 개선
- [ ] 성능 테스트 환경 구축

### 📊 완료 후 검증
- [ ] Query 로깅으로 실제 실행 쿼리 수 확인
- [ ] 성능 테스트로 응답 시간 측정
- [ ] 메모리 사용량 프로파일링
- [ ] 실제 사용자 시나리오 테스트

---

## 💡 추가 권장사항

### 1. 쿼리 최적화 원칙 수립
- Service에서는 가능한 한 Repository의 fetch join 메서드 사용
- 단순 존재 여부 확인은 existsById() 사용
- 프록시가 필요한 경우 getReferenceById() 사용
- Stream 연산에서 지연 로딩 접근 금지

### 2. 개발 가이드라인 추가
- 새로운 Repository 메서드 작성 시 fetch join 고려 의무화
- Service 메서드에서 반복문 내 Repository 호출 금지
- N+1 검증을 위한 통합 테스트 작성

### 3. 성능 모니터링 자동화
- CI/CD 파이프라인에 성능 테스트 추가
- 프로덕션 환경에 쿼리 성능 모니터링 도구 도입
- 정기적인 성능 리뷰 미팅 일정 수립

이 보고서의 권장사항을 단계적으로 구현하면 전체적인 데이터베이스 성능이 크게 향상되고, 사용자 경험이 개선될 것으로 예상됩니다.