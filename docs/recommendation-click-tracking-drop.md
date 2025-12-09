## Recommendation Click Tracking Drop on Navigation

### 문제 요약
- 위치: `src/main/resources/static/js/recommendation.js`
- 동작: 카드/링크 `onclick`에서 `trackRecommendationClick(...)` 호출 후 즉시 `window.location.href`로 이동.
- `trackRecommendationClick`은 일반 `fetch`만 호출(`keepalive`/`sendBeacon` 없음)하고 응답을 기다리지 않음.
- 페이지 언로드(네비게이션/새로고침/뒤로가기) 시 진행 중인 비동기 `fetch`가 취소될 수 있어 클릭 로그가 서버에 도달하지 않고 드롭될 가능성이 높음.

### 영향
- 실제 클릭 대비 수집된 클릭 로그가 과소 집계되어 CTR 등 추천 메트릭이 왜곡됨.
- 비로그인/로그인과 무관하게 브라우저 공통 동작(언로드 중 fetch 취소)이라 재현 가능성이 높음.

### 해결 방안
- 네비게이션 시에도 전송을 끝까지 시도하도록 전송 방식 변경:
  - 적용 완료: `navigator.sendBeacon('/api/recommendations/track-click', Blob(JSON, 'application/json'))` 우선 사용.
  - 폴백: `fetch(url, { method: 'POST', headers, body, keepalive: true })` (현재 코드에 적용).
- 네비게이션 지연 없이 비동기 전송을 유지하므로 UX 영향은 최소화.

### 리스크/사이드이펙트
- 부하 증가: 지금까지 드롭되던 클릭이 정상 집계되어 요청 수가 “실제 클릭 수”만큼 증가(예상된 수준).
- 브라우저 제약: `sendBeacon`/`keepalive`는 소량 페이로드에 적합(현재는 소형 JSON이라 영향 없음).
- 100% 보장은 아님: 탭 강제 종료·네트워크 단절 등은 여전히 실패 가능. 실패 시 콘솔 디버그 로그만 남기며, 기능 영향 없음.
