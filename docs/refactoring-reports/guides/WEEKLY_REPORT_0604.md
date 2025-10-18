# Unibook 프로젝트 주간 개발 보고서
**기간**: 2025년 5월 28일 ~ 6월 4일  
**프로젝트명**: Unibook - 대학생 맞춤형 교재 거래 플랫폼  
**작성일**: 2025년 6월 4일

## 1. 개발 개요
이번 주는 Unibook 프로젝트의 2주차로, 핵심 기능 구현을 완료하고 사용자 경험 개선에 집중했습니다. 총 30개의 커밋을 통해 실시간 채팅, 고급 알림 시스템, 관리자 기능, UI/UX 전면 개선 등을 구현했습니다.

## 2. 주요 개발 내용

### 2.1 핵심 기능 구현 (5/30)
- **MySQL Full-text Search 도입**: 네이티브 전문 검색으로 한글 교재명 검색 성능 향상
- **찜하기(Wishlist) 기능**: 관심 교재 저장 및 상태 변경 알림 기반 구축
- **과목-교수 연동 시스템**: 학교별 과목-교수 매핑으로 정확한 교재 검색 지원
- **마이페이지 구현**: 사용자 활동 내역 통합 관리 (내 게시글, 찜 목록, 거래 내역)
- **N+1 쿼리 최적화**: Fetch Join을 활용한 쿼리 성능 개선

**기술적 구현**:
```sql
-- Full-text Index 생성
CREATE FULLTEXT INDEX ft_idx_title_content 
ON posts(title, content) WITH PARSER ngram;
```

### 2.2 실시간 알림 시스템 구축 (5/31)
- **Server-Sent Events (SSE) 기반 실시간 알림**: 새 메시지, 거래 상태 변경 등 즉시 전달
- **알림 타입 다양화**: MESSAGE, WISHLIST_STATUS_CHANGED, WISHLIST_PRICE_CHANGED, KEYWORD_MATCH
- **비동기 처리**: Spring의 @Async를 활용한 알림 발송 성능 최적화

### 2.3 실시간 채팅 시스템 고도화 (6/1)
- **Firebase Firestore 기반 실시간 메시징**: WebSocket 대신 Firebase를 활용한 실시간 동기화
- **이미지 전송 기능**: Firebase Storage와 연동한 채팅 내 이미지 업로드
- **읽음 표시 기능**: `isReadByBuyer`, `isReadBySeller` 필드를 통한 실시간 읽음 상태 관리
- **채팅방 나가기**: 거래 종료 시 채팅방 자동 종료 및 상태 동기화

**기술적 구현**:
```java
// ChatRoom 엔티티에 Firebase 통합
@Entity
public class ChatRoom extends BaseEntity {
    private int buyerUnreadCount;
    private int sellerUnreadCount;
    // Firebase roomId와 동기화
}
```

### 2.4 고급 알림 시스템 확장 (6/3)
- **키워드 알림**: 사용자가 설정한 키워드와 매칭되는 게시글 등록 시 실시간 알림
- **가격 변동 알림**: 찜한 게시글의 가격 변경 시 증감률과 함께 알림
- **비동기 처리**: `@Async` 어노테이션을 활용한 성능 최적화

**구현 코드 예시**:
```java
@Async
public void checkAndSendKeywordAlerts(Post post) {
    List<KeywordAlert> alerts = keywordAlertRepository.findActiveAlerts();
    // 키워드 매칭 로직
}
```

### 2.5 관리자 시스템 구축 (6/3)
- **관리자 대시보드**: Chart.js를 활용한 통계 시각화
- **사용자 정지 기능**: `AdminAction` 엔티티를 통한 정지 이력 관리
- **신고 처리 시스템**: 게시글 신고 접수 및 자동 차단 기능

### 2.6 UI/UX 전면 개선 (6/2 ~ 6/4)
- **다크모드 완전 지원**: Bootstrap 5의 `data-bs-theme` 활용
- **Enhanced Header**: Glassmorphism 디자인, 통합 검색바, 사용자 아바타
- **Footer 개선**: Wave 애니메이션, 앱 다운로드 버튼, 정책 페이지 링크
- **폼 UI 재설계**: Progress steps, 드래그앤드롭 이미지 업로드

### 2.7 배포 및 성능 최적화 (6/3)
- **Railway 배포**: Java 21 환경 설정 및 메모리 최적화
- **OOM 문제 해결**: 데이터 로딩 최소화를 통한 메모리 사용량 개선

## 3. 기술 스택 활용
- **Backend**: Spring Boot 3.5.0, Java 21, JPA/Hibernate
- **Database**: MySQL 8.0 (Full-text Search 활용)
- **Frontend**: Thymeleaf, Bootstrap 5, jQuery
- **실시간 통신**: Firebase Firestore, Server-Sent Events (SSE)
- **배포**: Railway Platform

## 4. 성과 및 한계

### 4.1 성과
- 2주 만에 MVP(Minimum Viable Product) 구현 완료
- 실시간 기능(채팅, 알림) 안정적 작동
- 반응형 디자인으로 모바일 환경 지원
- 18개 엔티티, 30+ 서비스 클래스로 구성된 확장 가능한 아키텍처

### 4.2 현재의 한계점

#### 4.2.1 코드 일관성 부족
- DTO 명명 규칙 불일치 (예: `PostResponseDto`의 `images` vs 엔티티의 `postImages`)
- JavaScript 코드 스타일 혼재 (jQuery vs Vanilla JS)
- 에러 처리 방식의 비일관성

#### 4.2.2 성능 최적화 미흡
```java
// N+1 문제가 있는 쿼리 예시
List<Post> posts = postRepository.findAll();
for (Post post : posts) {
    post.getImages().size(); // 추가 쿼리 발생
}
```
- Lazy Loading으로 인한 N+1 쿼리 문제
- 일부 페이지에서 불필요한 Join 쿼리 사용
- 캐싱 전략 부재

#### 4.2.3 테스트 및 검증 부족
- 단위 테스트 커버리지 20% 미만
- 통합 테스트 미구현
- 보안 취약점 검증 미수행 (XSS, CSRF 등)

#### 4.2.4 확장성 고려 부족
- 하드코딩된 설정값들
- 국제화(i18n) 미지원
- API 버저닝 미고려

## 5. 향후 개선 계획

### 5.1 단기 과제 (1주)
1. **성능 최적화**
   - Fetch Join을 활용한 N+1 문제 해결
   - Redis 캐싱 도입
   - 데이터베이스 인덱스 최적화

2. **코드 품질 개선**
   - 코드 리팩토링 및 일관성 확보
   - 테스트 코드 작성 (목표: 60% 커버리지)
   - SonarQube 등 정적 분석 도구 도입

### 5.2 중장기 과제 (2-4주)
1. **기능 고도화**
   - 추천 시스템 구현
   - 결제 시스템 통합
   - 모바일 앱 개발

2. **아키텍처 개선**
   - 마이크로서비스 전환 검토
   - Event Sourcing 패턴 도입
   - CI/CD 파이프라인 구축

## 6. 결론
이번 주는 Unibook의 핵심 기능을 완성하고 사용자 경험을 크게 개선한 의미 있는 기간이었습니다. 하지만 급속한 개발로 인해 코드 품질과 성능 최적화 측면에서 개선이 필요한 부분들이 발견되었습니다. 

향후 리팩토링과 최적화를 통해 더욱 안정적이고 확장 가능한 플랫폼으로 발전시킬 계획입니다. 특히 테스트 커버리지 향상과 성능 최적화에 중점을 두어 프로덕션 환경에 적합한 수준으로 개선하겠습니다.

## 7. 참고 자료
- GitHub Repository: https://github.com/[repository-url]
- 배포 URL: https://unibook.up.railway.app
- 기술 문서: /docs 디렉토리 참조