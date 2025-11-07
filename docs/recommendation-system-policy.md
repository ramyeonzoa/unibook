# 추천 시스템 정책 및 개선 방향

## 📅 작성일: 2025-11-01

---

## 1. 비로그인 사용자 조회 기록 정책

### 현재 상태 (v1.0)
**결정 사항: 비로그인 사용자 조회 기록도 수집**

```java
// PostController.java - 현재 구현
if (!viewedPosts.contains(id)) {
    postService.incrementViewCount(id);
    viewedPosts.add(id);
    session.setAttribute("viewedPosts", viewedPosts);

    // 비로그인 유저도 기록 (userId = null)
    Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;
    postViewService.recordView(id, userId);
}
```

### 장점
- ✅ **초기 데이터 수집에 유리** - 사용자가 거의 없는 초기 단계에서 데이터 확보
- ✅ **세션 기반 중복 방지** - 같은 세션에서는 한 번만 기록됨 (어뷰징 1차 방어)
- ✅ **비로그인 사용자도 추천 받을 수 있음** - 개인화는 아니지만 기본 추천 제공
- ✅ **사용자 경험 개선** - 로그인 없이도 추천 시스템 체험 가능

### 단점 및 리스크
- ❌ **어뷰징 가능성** - 여러 브라우저/시크릿 모드로 반복 조회 가능
- ❌ **의미 없는 데이터 축적** - 봇, 크롤러 등의 조회 기록
- ❌ **데이터 품질 저하** - 실제 사용자 행동과 노이즈 구분 어려움

### 왜 현 상태를 유지하는가?
1. **초기 단계 특성**
   - 실제 사용자가 거의 없는 상황
   - 어뷰징보다 **데이터 부족**이 더 큰 문제
   - 추천 시스템 학습을 위한 최소한의 데이터 필요

2. **기존 보호 장치**
   - 세션 기반 중복 방지 이미 구현됨
   - 단기간 내 같은 게시글 반복 조회 차단

3. **점진적 개선 가능**
   - 나중에 데이터 필터링 가능
   - 로그인 유저 데이터만 분석할 수 있음
   - 의심스러운 패턴은 추후 제거 가능

---

## 2. 향후 개선 계획

### Phase 2: 데이터 분석 및 정책 수립 (3개월 후)

#### 2.1 데이터 분석
```sql
-- 로그인 vs 비로그인 비율 확인
SELECT
    CASE WHEN user_id IS NULL THEN '비로그인' ELSE '로그인' END as user_type,
    COUNT(*) as view_count,
    COUNT(DISTINCT post_id) as unique_posts
FROM post_views
GROUP BY user_type;

-- 의심스러운 패턴 감지
SELECT user_id, COUNT(*) as view_count
FROM post_views
WHERE user_id IS NULL
  AND viewed_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY user_id
HAVING view_count > 100;  -- 1시간 내 100회 이상
```

#### 2.2 어뷰징 감지 알고리즘
- IP 기반 중복 체크 (동일 IP에서 단시간 내 과다 조회)
- User-Agent 분석 (봇/크롤러 감지)
- 행동 패턴 분석 (비정상적으로 빠른 페이지 전환)

#### 2.3 데이터 정제
```sql
-- 오래된 비로그인 조회 기록 삭제 (30일 기준)
DELETE FROM post_views
WHERE user_id IS NULL
  AND viewed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 의심스러운 패턴 제거
DELETE FROM post_views
WHERE user_id IS NULL
  AND /* 어뷰징 조건 */;
```

---

### Phase 3: 고급 보호 장치 (6개월 후)

#### 3.1 IP 기반 Rate Limiting
```java
// 예시 코드
@RateLimiter(key = "#request.remoteAddr", rate = "100/hour")
public void recordView(Long postId, Long userId, HttpServletRequest request) {
    // ...
}
```

#### 3.2 Fingerprinting
- 브라우저 fingerprint 기반 중복 체크
- Canvas fingerprinting
- WebGL fingerprinting

#### 3.3 CAPTCHA 통합
- 의심스러운 행동 감지 시 CAPTCHA 요구
- reCAPTCHA v3 (invisible)

---

### Phase 4: 데이터 기반 정책 전환 (1년 후)

#### 4.1 A/B 테스트
- **그룹 A**: 비로그인 유저 기록 수집
- **그룹 B**: 로그인 유저만 기록 수집
- 추천 품질, 사용자 만족도, 전환율 비교

#### 4.2 정책 결정
데이터 분석 결과에 따라:
- **Option A**: 현 상태 유지 (비로그인 포함)
- **Option B**: 로그인 유저만 수집
- **Option C**: 하이브리드 (비로그인은 가중치 낮춤)

---

## 3. 중복 기록 정책

### 현재: 세션 기반 1회 기록
```java
// 같은 세션에서는 한 번만 기록
if (!viewedPosts.contains(id)) {
    postViewService.recordView(id, userId);
}
```

### 대안: 시간 기반 중복 허용
**강한 선호 신호를 캡처하기 위해**

```java
// 예시: 30분 후에는 다시 기록 가능
SELECT * FROM post_views
WHERE user_id = ?
  AND post_id = ?
  AND viewed_at > DATE_SUB(NOW(), INTERVAL 30 MINUTE);

// 없으면 기록
if (count == 0) {
    postViewService.recordView(id, userId);
}
```

**장점:**
- ✅ 반복 조회 = 강한 관심 신호
- ✅ 더 정확한 추천 가능

**단점:**
- ❌ 데이터 증가
- ❌ 어뷰징 위험 증가

**결정:** Phase 2에서 데이터 분석 후 결정

---

## 4. 모니터링 지표

### 수집할 메트릭
- [ ] 일일 조회 기록 수
- [ ] 로그인 vs 비로그인 비율
- [ ] 게시글당 평균 조회수
- [ ] 사용자당 평균 조회수
- [ ] 추천 클릭률 (CTR)
- [ ] 추천 → 거래 전환율

### 알림 기준
- 🚨 **1시간 내 동일 IP에서 1000회 이상 조회**
- 🚨 **비로그인 조회 비율 > 90%**
- 🚨 **특정 게시글에 비정상적 조회 집중**

---

## 5. 데이터베이스 정리 스케줄

### 자동 정리 스크립트 (예정)
```sql
-- 매월 1일 실행
-- 30일 지난 비로그인 조회 기록 삭제
CREATE EVENT cleanup_anonymous_views
ON SCHEDULE EVERY 1 MONTH
DO
  DELETE FROM post_views
  WHERE user_id IS NULL
    AND viewed_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

---

## 6. 결론

### 현재 (2025-11-01)
**비로그인 사용자 조회 기록 수집 유지**
- 초기 데이터 확보 우선
- 세션 기반 중복 방지로 1차 방어
- 점진적 개선 계획 수립

### 향후 방향
1. **3개월 후**: 데이터 분석 및 정책 재검토
2. **6개월 후**: 고급 보호 장치 도입
3. **1년 후**: A/B 테스트 기반 최적 정책 결정

**핵심 원칙: 데이터 기반 의사결정**

---

## 📚 참고 자료
- [YouTube 추천 시스템](https://research.google/pubs/pub45530/)
- [Netflix 추천 알고리즘](https://netflixtechblog.com/netflix-recommendations-beyond-the-5-stars-part-1-55838468f429)
- [Spotify Discovery Weekly](https://engineering.atspotify.com/2015/01/discover-weekly/)

---

## 📝 변경 이력
| 날짜 | 변경 사항 | 담당자 |
|------|----------|--------|
| 2025-11-01 | 초안 작성 | Claude Code |
| | | |
| | | |

---