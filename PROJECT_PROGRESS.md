# Unibook 프로젝트 진행 현황

## 📅 프로젝트 개요
- **시작일**: 2025년 1월 25일
- **개발자**: ramyeonzoa
- **GitHub**: https://github.com/ramyeonzoa/unibook
- **목표**: 대학생 맞춤형 교재 거래 플랫폼 (학교-학과-교수-과목별 연관 검색)

## 🏗️ 기술 스택
- **Backend**: Spring Boot 3.4.6, Java 21
- **Database**: MySQL 8.0+ (Windows localhost:3306)
- **Frontend**: Thymeleaf, Bootstrap 5
- **Build Tool**: Gradle 8.4+
- **Version Control**: Git, GitHub

## ✅ Day 1 완료 내역 (2025년 1월 25일)

### 1. 프로젝트 초기 설정
- Spring Boot 프로젝트 생성 (IntelliJ IDEA)
- MySQL 데이터베이스 생성: `unibook_db`
- application.yml 설정 완료
  ```yaml
  datasource:
    url: jdbc:mysql://localhost:3306/unibook_db
    username: root
    password: 1234
  ```

### 2. 패키지 구조 생성
```
src/main/java/com/unibook/
├── config/          # 설정 클래스
├── controller/      # 컨트롤러
├── service/         # 서비스 계층
├── repository/      # JPA 레포지토리
├── domain/
│   ├── entity/      # 엔티티 클래스
│   └── dto/         # DTO 클래스
└── util/           # 유틸리티
```

### 3. Entity 클래스 생성 (8개)
1. **User**: 사용자 정보 (학생)
2. **School**: 대학교 정보 (이메일 도메인 포함)
3. **Department**: 학과 정보
4. **Professor**: 교수 정보
5. **Subject**: 과목 정보
6. **Book**: 교재 정보 (ISBN, 제목, 저자 등)
7. **Post**: 거래 게시글
8. **SubjectBook**: 과목-교재 연결 테이블 (핵심!)

### 4. Repository 인터페이스 생성
- 모든 Entity에 대한 JpaRepository 인터페이스 생성
- 기본 CRUD 메서드 자동 제공

### 5. 테스트 및 검증
- Spring Boot 애플리케이션 정상 실행 확인
- MySQL 연결 성공
- 테이블 자동 생성 확인 (JPA DDL-auto)
- http://localhost:8080 접속 확인

### 6. GitHub 저장소 생성
- Repository 생성 및 초기 커밋 완료
- Personal Access Token으로 인증 설정

## 📊 Entity 관계도
```
School (1) ─── (N) Department
   │                    │
   │                    │
   └─── (N) User       (N) Professor
             │                │
             │                │
             └─── Post ───────┘
                   │
                   │
                  Book ──── SubjectBook ──── Subject
```

## 🔥 핵심 기능 포인트
1. **SubjectBook 연결 테이블**: 어떤 과목에서 어떤 교재를 사용하는지 매핑
2. **다단계 검색**: 학교 → 학과 → 교수 → 과목별 교재 검색
3. **대학 이메일 인증**: School 엔티티의 도메인으로 검증

## 📋 Day 2 예정 작업 (2025년 1월 26일)

### 1. 기본 Service 계층 구현
- UserService, PostService 등 핵심 서비스
- 기본 CRUD 로직 구현

### 2. Controller 및 View 생성
- HomeController 생성
- Thymeleaf 템플릿 설정
- Bootstrap 5 CDN 추가

### 3. CSV 데이터 로딩
- DataInitializer 클래스 생성
- 대학교 이메일 도메인 데이터 로드
- 학과 정보 데이터 로드

### 4. 기본 페이지 구성
- 메인 페이지 (index.html)
- 공통 레이아웃 템플릿
- 네비게이션 바

## 🚨 주의사항 및 해결된 이슈

### 1. 실행 환경
- ⚠️ **반드시 IntelliJ IDEA에서 실행** (WSL 터미널 X)
- MySQL은 Windows에 설치됨 (localhost:3306)
- WSL은 git 명령어와 파일 편집용으로만 사용

### 2. GitHub Push 문제 해결
- Personal Access Token 생성 필요
- 기존 README와 충돌 시 `--force` 옵션 사용

### 3. MySQL 연결
- WSL에서 mysql 명령어 사용 불가
- Windows CMD 또는 MySQL Workbench 사용

## 📈 프로젝트 진행률
- Week 1 (핵심 기능): 14% 완료 (1/7일)
- 전체 프로젝트: 7% 완료 (1/14일)

## 🎯 다음 마일스톤
- Day 3: 회원가입/로그인 구현
- Day 4: 대학 이메일 인증
- Day 6: **핵심 기능** - 고급 검색 시스템

---
*마지막 업데이트: 2025년 1월 25일*