# Unibook 프로젝트 진행 현황

## 📅 프로젝트 개요
- **시작일**: 2025년 1월 25일
- **개발자**: ramyeonzoa
- **GitHub**: https://github.com/ramyeonzoa/unibook
- **목표**: 대학생 맞춤형 교재 거래 플랫폼 (학교-학과-교수-과목별 연관 검색)

## 🏗️ 기술 스택
- **Backend**: Spring Boot 3.5.0, Java 21
- **Database**: MySQL 8.0+ (Windows localhost:3306)
- **Frontend**: Thymeleaf, Bootstrap 5.3.0
- **Build Tool**: Gradle 8.4+
- **Version Control**: Git, GitHub
- **External APIs**: Naver Book Search API
- **Email**: Gmail SMTP (unibooknotify@gmail.com)

## ✅ Day 1-8 완료 내역 (2025년 1월 25일 - 1월 30일)

### Day 1: 프로젝트 초기 설정 ✅
- Spring Boot 프로젝트 생성 및 MySQL 연동
- 8개 Entity 클래스 생성 (User, School, Department, Professor, Subject, Book, Post, SubjectBook)
- Repository 인터페이스 생성
- GitHub 저장소 생성 및 초기 커밋

### Day 2: Service 계층 및 데이터 초기화 ✅
- Service 계층 구현 (UserService, PostService, SchoolService, BookService)
- HomeController + 메인 페이지 구현
- CSV 데이터 로드 (학교 400개, 학과 12,870개)
- 성능 최적화 (Fetch Join, 캐싱)
- 환경별 설정 분리 (application-local.yml)

### Day 3: 인증 시스템 및 DTO 패턴 ✅
- BaseEntity 생성 (JPA Auditing)
- 모든 Entity 재구성 (DB 스키마 정합성)
- Spring Security 설정 (DaoAuthenticationProvider, CustomUserDetailsService)
- DTO 패턴 전면 도입
- 회원가입/로그인 시스템 구현
- 실시간 비밀번호 검증
- 로그인 상태별 UI 분기

### Day 4: 이메일 인증 시스템 ✅
- Email Verification System 구현
  - Gmail SMTP 설정 (unibooknotify@gmail.com)
  - EmailVerificationToken Entity 및 Repository
  - EmailService 구현 (비동기 처리 @Async)
- Email Templates 개선 (미니멀 모던 디자인)
- Security & UX 개선
  - 비밀번호 재설정 기능
  - Rate Limiting 구현 (1분 쿨다운, 시간당 5회)
  - 토큰 만료 시간 관리 (1시간)
- Spring Retry 적용 (3회 재시도, 지수 백오프)
- VerificationInterceptor 구현 (모든 요청에 isEmailVerified 상태 추가)

### Day 5: 게시글 CRUD 및 네이버 책 API ✅
- PostController 생성 (완전한 CRUD 엔드포인트)
  - 권한 체크: 작성자/관리자 구분
  - 다중 이미지 업로드 (최대 5개, 드래그앤드롭)
  - AJAX 상태 변경 API
- 네이버 책 검색 API 연동
  - BookSearchService 구현 (@Cacheable, @Retryable)
  - 책 검색 모달 UI
  - ISBN 중복 방지 로직
- Book Entity 확장 (imageUrl 필드 추가)
- 파일 업로드 시스템 (uploads/images/posts/)
- Bootstrap Carousel 이미지 갤러리
- 공통 헤더 Fragment 구현

### Day 6: 과목-교수 연동 시스템 ✅
- **핵심 설계 확정**:
  - 과목 선택 시에만 학교 제약
  - 사용자 중심 UX: 과목명 우선 → 교수명 보조
  - Subject Entity 정규화 (year, semester 필드 추가)
  - Post → Subject 직접 연결
- Entity 및 Service 구현
  - ProfessorService, SubjectService, SubjectBookService
  - 학교별 과목/교수 검색 제한
- API 컨트롤러 구현
  - /api/professors/search/my-school
  - /api/subjects/search/my-school
  - /api/subjects/create-with-professor
- 프론트엔드 UX 개편 (subject-search-v2.js)
- 교양과목 특별 처리 시스템

### Day 7: MySQL Full-text Search 구현 ✅
- **검색 시스템 구현**:
  - MySQL Full-text Search with ngram parser (한글 지원)
  - 통합 검색: 제목, 내용, 책 제목/저자, 과목명, 교수명
  - OR 검색 로직으로 유연성 확보
- **검색 결과 하이라이팅**:
  - JavaScript 기반 검색어 강조
  - CSS 클래스로 스타일링
- **정렬 기능 구현**:
  - 관련도순 (Full-text score)
  - 최신순, 가격순 (오름차순/내림차순), 조회수순
  - 검색 시 기본: 관련도순, 일반 목록: 최신순
- **UI/UX 개선**:
  - 메인 페이지 검색 기능 활성화
  - 정렬 옵션 텍스트 링크화
  - 거래 방법 필수 필드화
- **버그 수정**:
  - Enum 비교 문제 해결
  - Native Query 정렬 충돌 해결
  - 거래 장소 조건부 표시

### Day 8: Wishlist(찜하기) 기능 ✅
- **Backend 구현**:
  - Wishlist Entity 생성 (User-Post 다대다 관계)
  - WishlistRepository: 사용자별 찜 목록 조회
  - WishlistService: 찜하기 토글, 자기 게시글 방지
  - WishlistApiController: REST API 엔드포인트
- **Frontend 구현**:
  - 게시글 목록: 하트 아이콘 표시
  - 게시글 상세: 찜하기 버튼
  - 실시간 UI 업데이트 (AJAX)
  - Toast 알림 메시지
- **보안 및 UX**:
  - 이메일 미인증 사용자 차단
  - 자기 게시글 찜하기 방지
  - 찜 개수 실시간 표시
  - Post Entity에 wishlistCount 필드 추가 (성능 최적화)

## 📊 현재 프로젝트 구조
```
unibook/
├── src/main/java/com/unibook/
│   ├── common/              # AppConstants, Messages
│   ├── config/              # Security, JPA, Async, WebMvc 설정
│   ├── controller/          # 웹 컨트롤러
│   │   ├── api/            # REST API 컨트롤러
│   │   └── dto/            # 요청/응답 DTO
│   ├── domain/
│   │   ├── entity/         # 13개 Entity (BaseEntity 상속)
│   │   └── dto/            # 도메인 DTO
│   ├── exception/          # 커스텀 예외
│   ├── repository/         # JPA Repository
│   │   └── projection/     # PostSearchProjection
│   ├── security/           # Spring Security 관련
│   ├── service/            # 비즈니스 로직
│   └── util/              # 유틸리티 클래스
└── src/main/resources/
    ├── static/
    │   ├── css/           # 커스텀 CSS
    │   └── js/            # JavaScript 파일
    ├── templates/
    │   ├── auth/          # 인증 관련 페이지
    │   ├── email/         # 이메일 템플릿
    │   ├── error/         # 에러 페이지
    │   ├── fragments/     # 공통 Fragment
    │   └── posts/         # 게시글 관련 페이지
    ├── data/              # CSV 데이터 파일
    └── application.yml    # 설정 파일
```

## 🔑 핵심 기능 구현 현황

### ✅ 완료된 기능
1. **인증 시스템**: 회원가입, 로그인, 이메일 인증, 비밀번호 재설정
2. **게시글 관리**: CRUD, 다중 이미지 업로드, 상태 관리
3. **검색 시스템**: MySQL Full-text Search, 통합 검색, 정렬
4. **과목-교수 연동**: 학교별 제한, 과목명 우선 검색
5. **책 검색**: 네이버 API 연동, ISBN 중복 방지
6. **찜하기**: 토글 기능, 실시간 UI 업데이트
7. **성능 최적화**: 캐싱, Fetch Join, 인덱싱

### 🚧 개발 예정 기능 (Day 9-14)
- **Day 9**: 알림 시스템 (SSE/WebSocket)
- **Day 10**: 마이페이지 (내 게시글/찜 목록/거래 내역)
- **Day 11-12**: Firebase 실시간 채팅
- **Day 13**: 거래 후기 및 평점 시스템
- **Day 14**: 성능 최적화 및 보안 강화
- **Day 15**: 배포 준비 또는 UI/UX 개선

## 🎯 기술적 특징
1. **DTO 패턴**: Entity와 View 분리
2. **비동기 처리**: @Async 이메일 발송
3. **캐싱**: @Cacheable 검색 결과
4. **재시도 로직**: @Retryable 외부 API 호출
5. **인터셉터**: 모든 요청에 인증 상태 주입
6. **Full-text Search**: ngram parser로 한글 검색
7. **트랜잭션 격리**: 동시성 제어

## 📈 프로젝트 진행률
- Week 1 (핵심 기능): **100% 완료** (7/7일)
- Week 2 (고급 기능): **14% 완료** (1/7일)
- 전체 프로젝트: **57% 완료** (8/14일)

## 🚨 중요 결정 사항
- ✅ 검색 엔진: MySQL Full-text Search (Elasticsearch 대신)
- ✅ 채팅 시스템: Firebase 사용 예정
- ✅ 이메일 인증: boolean 필드 방식
- ✅ 파일 업로드: 로컬 저장 (uploads/)
- ⏳ 배포 플랫폼: 미정 (AWS, NCP 등)

## 📝 최근 커밋
- `13c3f7f`: 찜하기 기능 완전 구현
- `f53d6c6`: Day 7: MySQL Full-text Search 구현 및 검색 기능 고도화
- `7b87f34`: Day 6 완료: 과목-교수 연동 시스템 구현 및 Subject 정규화
- `313301e`: Day 5 완료: 네이버 책 검색 API 통합 및 책 표지 이미지 시스템 구현
- `c1158ac`: Day 5 완료: 다중 이미지 업로드 및 드래그 앤 드롭 순서 관리

---
*마지막 업데이트: 2025년 1월 30일*