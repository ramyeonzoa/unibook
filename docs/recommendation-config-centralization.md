## Recommendation Settings Centralization Plan

### 문제 정의
- 추천 관련 상수(가중치, 임계값, 조회 상한 등)가 `RecommendationService` 내부에 하드코딩되어 있어 값을 조정하려면 빌드/배포가 필요함.
- 실험(슬롯 비율, ε 등)이나 튜닝이 어려우며, 설정이 코드 곳곳에 흩어져 관리가 불편함.

### 해결 방향
- `@ConfigurationProperties(prefix = "recommendation")` 기반 `RecommendationProperties` 클래스로 관련 설정을 한 곳에 모음.
- 기존 상수 값을 기본값/프로퍼티로 옮기고, `RecommendationService`는 프로퍼티를 주입받아 사용(로직 변경 없음).
- 향후 슬롯 믹스/ε-greedy 등 실험용 값도 동일 프로퍼티에 추가하여 설정만으로 조정 가능하게 함.

### 단계별 계획
1) `RecommendationProperties` 생성 (`com.unibook.config`), 기존 상수와 동일한 기본값 정의.
2) `application.yml`에 `recommendation.*` 설정으로 기본값 명시.
3) `RecommendationService`에서 하드코딩 상수 제거, 프로퍼티 참조로 치환(동작 동일).
4) 스모크 검증: 추천 API가 정상 응답/결과 반환 여부 확인.
5) 필요 시 슬롯 비율/ε 등 추가 설정 항목을 같은 프로퍼티에 확장.

### 리스크/사이드이펙트
- 잘못된 설정값 입력 시 동작이 달라질 수 있음 → 기본값을 기존 상수와 동일하게 두고, 누락 시 안전한 디폴트로 동작하도록 방어.
- 프로퍼티 바인딩 실패 방지: 필드에 기본값 지정, `@PostConstruct`에서 로깅/검증 가능.

### 구현 현황 (적용 완료)
- `RecommendationProperties` 추가: 가중치/임계값/최신성/행동 조회 상한/유사도 가중치/협업 후보 수 등을 설정으로 묶음.
- `application.yml`에 동일한 기본값을 `recommendation.*`로 정의.
- `RecommendationService`에서 하드코딩 상수 제거 후 프로퍼티 참조로 치환(로직 동일).
