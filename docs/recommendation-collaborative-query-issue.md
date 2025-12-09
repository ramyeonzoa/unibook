## Recommendation Collaborative Score Query Duplication

### 문제 요약
- 위치: `src/main/java/com/unibook/service/RecommendationService.java`
- 동작: `getPersonalizedRecommendations`가 후보 게시글마다 `calculateCollaborativeScore`를 호출하며, 이 메서드는 호출 때마다 `postViewRepository.findCollaborativePostsByUserId(userId, PageRequest.of(0, 50))` 쿼리를 실행함.
- 동일한 `userId`와 페이징으로 항상 같은 결과를 돌려주는데, 후보 개수 N(= AVAILABLE 게시글 수)에 비례해 같은 쿼리가 N번 반복되어 DB 부하와 응답 지연이 선형으로 증가.

### 영향
- 불필요한 DB 라운드트립 증가로 추천 응답 시간 지연, 트래픽 상승 시 타임아웃 리스크 확대.
- 추천 품질에는 변화 없음(같은 데이터를 중복 조회).

### 개선 방안
- 쿼리를 한 번만 실행해 결과를 메모리에 캐싱 후 재사용(적용 완료):
  - `findCollaborativePostsByUserId` 결과를 `Map<postId, viewCount>`와 `maxViewCount`로 변환하는 `CollaborativeContext` 추가.
  - 후보별 점수 계산 시 DB 재호출 없이 맵에서 O(1) 조회하여 정규화 점수 계산.
- 추가 선택지:
  - 협업 후보 개수(현재 50) 설정값 외부화.
  - 협업 후보 계산을 배치/캐시로 이전해 호출 시점 쿼리 자체를 제거.

### 리스크/사이드이펙트
- 기능 변화 없음: 같은 데이터로 점수를 계산하므로 추천 결과 동일해야 함.
- 메모리 사용 소폭 증가(수십 건 수준)로 영향 미미.
- `userId == null` 또는 쿼리 실패 시 기존과 동일하게 0점 처리 유지 필요.
