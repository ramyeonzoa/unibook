# 추천 시스템 고도화 로드맵

## 📊 현재 상태 (Phase 1 완료 + 다중 행동 추천 시스템)

### ✅ Phase 1: MVP 추천 시스템 (완료)
- ✅ 적응형 하이브리드 추천 알고리즘 (콘텐츠 기반 + 협업 필터링)
- ✅ 맞춤 추천 (FOR_YOU): 사용자 행동 기반
- ✅ 비슷한 게시글 (SIMILAR): 콘텐츠 유사도 기반
- ✅ 클릭 추적 시스템 (RecommendationClick 엔티티)
- ✅ 기본 메트릭 대시보드 (클릭 수, 일별 추이, 타입별 분포)

### ✅ Phase 1.5: 다중 행동 추천 시스템 (완료 - 2025-01-25)

#### 구현된 기능
1. **Implicit Feedback 통합**
   - 클릭 (가중치 1.0): 가장 강한 관심 신호
   - 찜 (가중치 0.7): 중간 강도 관심 신호
   - 조회 (가중치 0.3): 약한 관심 신호

2. **시간 감쇠 시스템**
   - 7일 이내: 원본 가중치 유지
   - 7일 이후: 지수 감쇠 (λ=0.1)
   - 최근 클릭에 더 높은 가중치 부여

3. **성능 최적화**
   - N+1 쿼리 문제 해결: 6,800번 → 7번 (99% 감소)
   - 사용자 상호작용 이력 1회 조회
   - Post 일괄 조회 (IN 쿼리)
   - Map 기반 O(1) 조회

4. **신규 엔티티 및 DTO**
   - `InteractionWeight` enum: 행동별 가중치 정의
   - `UserInteractionHistory` DTO: 통합 상호작용 이력 관리

#### 기술적 개선
```java
// Before: 단순 조회 이력 기반
List<Long> viewedPostIds = postViewRepository.findRecent(...);
for (Long postId : viewedPostIds) {
    // 단일 행동만 고려
}

// After: 다중 행동 + 가중치 + 시간 감쇠
UserInteractionHistory history = getUserInteractionHistory(userId);
for (InteractionRecord click : history.getClicks()) {
    double decayedWeight = click.getDecayedWeight(lambda, threshold);
    totalScore += similarity * decayedWeight;
}
```

#### 예상 효과
- ✅ **추천 정확도 향상**: 클릭 데이터 활용으로 더 정확한 선호도 파악
- ✅ **시간적 관련성**: 최근 관심사에 더 높은 가중치
- ✅ **성능 개선**: 대규모 트래픽 대비 완료 (99% 쿼리 감소)
- ✅ **확장성**: 새로운 행동 타입 쉽게 추가 가능

### 현재 메트릭
- **총 클릭 수**: 전체 추천 클릭 수
- **타입별 클릭 수**: FOR_YOU vs SIMILAR 비교
- **일별 클릭 추이**: 시계열 차트
- **타입별 클릭 비율**: 도넛 차트

### 현재 제한사항
1. **노출 수(Impression) 추적 없음**
   - CTR(Click-Through Rate) 계산 불가능
   - 추천 품질을 정확히 측정할 수 없음
   - 10개 추천 중 3개 클릭 vs 10000개 추천 중 3개 클릭 구분 불가

2. **세션 개념 없음**
   - 사용자가 한 번에 여러 개 클릭한 것과 여러 번 방문해서 클릭한 것 구분 불가
   - 탐색형 사용자 vs 신중형 사용자 행동 패턴 분석 불가

3. **A/B 테스트 불가능**
   - 알고리즘 개선 효과를 정량적으로 측정할 수 없음
   - 실험군/대조군 분리 없음

4. **다양성 보장 메커니즘 없음**
   - Filter Bubble 문제 가능성
   - 같은 카테고리/학과만 추천될 위험

---

## 🎯 Phase 2: 노출 추적 시스템 (추천 품질 측정)

### 목표
> 추천이 얼마나 매력적이고 유용한지 정량적으로 측정 가능하게 만들기

### 구현 내용

#### 2.1. RecommendationImpression 엔티티 생성
```java
@Entity
@Table(name = "recommendation_impressions")
public class RecommendationImpression {
    @Id @GeneratedValue
    private Long impressionId;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;  // Nullable for anonymous

    private String sessionId;  // 세션 식별자
    private RecommendationType type;  // FOR_YOU, SIMILAR
    private Integer count;  // 해당 세션에서 노출된 추천 개수
    private LocalDateTime impressedAt;

    // 노출 위치 정보
    private String pageType;  // "main", "detail", "list"
    private Long sourcePostId;  // SIMILAR인 경우 기준 게시글
}
```

#### 2.2. 노출 추적 JavaScript (Option 1: API 호출 기준)
```javascript
// API에서 추천 받아올 때 노출로 기록
async function loadRecommendations(type) {
  const response = await fetch(`/api/recommendations/${type}`);
  const data = await response.json();

  // 추천 받은 개수를 노출로 기록
  trackImpressions({
    type: type,
    count: data.posts.length,
    sessionId: getSessionId(),
    pageType: getCurrentPageType()
  });

  return data.posts;
}

function trackImpressions(impressionData) {
  fetch('/api/recommendations/track-impression', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(impressionData)
  }).catch(err => console.debug('노출 추적 실패:', err));
}
```

#### 2.3. 정확한 CTR 계산
```java
@Service
public class RecommendationMetricsService {

    /**
     * 세션 기반 CTR 계산
     * CTR = (추천을 1번 이상 클릭한 세션 수) / (추천이 노출된 총 세션 수)
     */
    public double calculateSessionCTR(LocalDateTime start, LocalDateTime end) {
        long totalSessions = impressionRepository.countDistinctSessionsByPeriod(start, end);
        long clickedSessions = clickRepository.countDistinctSessionsWithClicksByPeriod(start, end);

        return totalSessions > 0 ? (clickedSessions * 100.0 / totalSessions) : 0.0;
    }

    /**
     * 전통적 CTR 계산 (클릭 수 / 노출 수)
     */
    public double calculateTraditionalCTR(LocalDateTime start, LocalDateTime end) {
        long totalImpressions = impressionRepository.sumCountByPeriod(start, end);
        long totalClicks = clickRepository.countByPeriod(start, end);

        return totalImpressions > 0 ? (totalClicks * 100.0 / totalImpressions) : 0.0;
    }

    /**
     * 평균 클릭 수 (명확한 표현)
     */
    public double calculateAvgClicksPerImpression(LocalDateTime start, LocalDateTime end) {
        long totalImpressions = impressionRepository.sumCountByPeriod(start, end);
        long totalClicks = clickRepository.countByPeriod(start, end);

        return totalImpressions > 0 ? ((double) totalClicks / totalImpressions) : 0.0;
    }
}
```

### 예상 개발 시간
- 엔티티 및 Repository: **30분**
- JavaScript 추적 코드: **30분**
- Service 로직: **1시간**
- 테스트 및 디버깅: **1시간**
- **총 3시간**

### 예상 효과
- ✅ 정확한 CTR 측정 가능
- ✅ 추천 품질을 정량적으로 평가 가능
- ✅ 알고리즘 개선 효과를 숫자로 확인 가능
- ✅ "10개 중 3개 클릭" vs "10000개 중 3개 클릭" 구분 가능

---

## 🔬 Phase 3: 정밀 노출 추적 (Intersection Observer)

### 목표
> 실제로 사용자가 본 추천만 노출로 카운트하여 더 정확한 메트릭 제공

### 구현 내용

#### 3.1. Viewport 기반 노출 추적
```javascript
// Intersection Observer로 실제 화면에 보인 것만 추적
const recommendationObserver = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting && !entry.target.dataset.impressed) {
      // 화면에 50% 이상 노출되고, 1초 이상 유지된 경우만
      setTimeout(() => {
        if (entry.isIntersecting) {
          trackSingleImpression({
            postId: entry.target.dataset.postId,
            type: entry.target.dataset.type,
            position: entry.target.dataset.position
          });
          entry.target.dataset.impressed = 'true';
        }
      }, 1000);
    }
  });
}, {
  threshold: 0.5,  // 50% 이상 보일 때
  rootMargin: '0px'
});

// 모든 추천 카드에 Observer 연결
document.querySelectorAll('.recommendation-card').forEach(card => {
  recommendationObserver.observe(card);
});
```

#### 3.2. 개선된 메트릭
- **Viewability Rate**: 추천된 것 중 실제로 본 비율
- **Attention Time**: 각 추천 카드를 본 시간
- **Scroll Depth**: 추천 목록을 얼마나 깊이 봤는지

### 예상 개발 시간
- JavaScript Observer 구현: **2시간**
- 서버사이드 처리: **1시간**
- 테스트: **1시간**
- **총 4시간**

### 예상 효과
- ✅ 가장 정확한 노출 데이터
- ✅ 사용자 주의(Attention) 측정 가능
- ✅ 추천 배치 최적화 (위치별 효과 분석)

### 주의사항
⚠️ 성능 영향 고려 필요
- 많은 추천 카드가 있을 경우 Observer 오버헤드
- 모바일 환경에서 배터리 소모 증가 가능
- 네트워크 요청 증가 (배치 처리로 완화 가능)

---

## 🧪 Phase 4: A/B 테스트 프레임워크

### 목표
> 추천 알고리즘 개선 효과를 과학적으로 검증

### 구현 내용

#### 4.1. 실험 관리 시스템
```java
@Entity
public class RecommendationExperiment {
    @Id
    private String experimentId;
    private String name;
    private String description;

    // 실험 설정
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double trafficAllocation;  // 0.0 ~ 1.0

    // 알고리즘 변형
    @Enumerated(EnumType.STRING)
    private AlgorithmVariant controlVariant;  // 기존 알고리즘

    @Enumerated(EnumType.STRING)
    private AlgorithmVariant treatmentVariant;  // 새 알고리즘

    // 결과
    private ExperimentStatus status;
    private String results;  // JSON
}

@Entity
public class ExperimentAssignment {
    @Id
    private Long assignmentId;

    @ManyToOne
    private User user;

    @ManyToOne
    private RecommendationExperiment experiment;

    @Enumerated(EnumType.STRING)
    private ExperimentGroup group;  // CONTROL, TREATMENT

    private LocalDateTime assignedAt;
}
```

#### 4.2. A/B 테스트 Service
```java
@Service
public class ABTestService {

    /**
     * 사용자를 실험 그룹에 할당
     */
    public AlgorithmVariant assignUserToExperiment(User user, String experimentId) {
        // 이미 할당된 경우 기존 그룹 반환
        ExperimentAssignment existing = findAssignment(user, experimentId);
        if (existing != null) {
            return existing.getGroup() == CONTROL
                ? controlVariant : treatmentVariant;
        }

        // 해시 기반 일관된 랜덤 할당
        boolean isControl = hashUserId(user.getId(), experimentId) % 2 == 0;

        saveAssignment(user, experimentId, isControl ? CONTROL : TREATMENT);

        return isControl ? controlVariant : treatmentVariant;
    }

    /**
     * 실험 결과 분석
     */
    public ExperimentResult analyzeExperiment(String experimentId) {
        // Control vs Treatment 비교
        MetricsSummary controlMetrics = calculateMetrics(experimentId, CONTROL);
        MetricsSummary treatmentMetrics = calculateMetrics(experimentId, TREATMENT);

        // 통계적 유의성 검정 (t-test)
        double pValue = calculatePValue(controlMetrics, treatmentMetrics);

        return ExperimentResult.builder()
            .controlCTR(controlMetrics.getCtr())
            .treatmentCTR(treatmentMetrics.getCtr())
            .improvement((treatmentMetrics.getCtr() - controlMetrics.getCtr())
                        / controlMetrics.getCtr() * 100)
            .pValue(pValue)
            .isSignificant(pValue < 0.05)
            .build();
    }
}
```

### 예상 개발 시간
- 엔티티 및 테이블 설계: **1시간**
- 할당 로직 구현: **2시간**
- 분석 로직 구현: **3시간**
- Admin UI: **2시간**
- 테스트: **2시간**
- **총 10시간**

### 예상 효과
- ✅ 데이터 기반 의사결정
- ✅ 알고리즘 개선의 실제 효과 측정
- ✅ 리스크 최소화 (점진적 롤아웃)

---

## 📈 Phase 5: 고급 메트릭 및 개인화

### 목표
> 추천 시스템의 전체적인 건강도와 비즈니스 임팩트 측정

### 추가 메트릭

#### 5.1. 사용자 참여도 메트릭
- **Engagement Rate**: 추천 클릭 → 게시글 조회 → 찜하기/채팅 전환율
- **Dwell Time**: 추천을 통해 들어간 게시글에서 머문 시간
- **Bounce Rate**: 추천 클릭 후 즉시 떠난 비율

#### 5.2. 비즈니스 메트릭
- **Conversion Rate**: 추천 → 거래 완료까지 전환율
- **GMV (Gross Merchandise Value)**: 추천을 통한 거래 금액
- **User Retention**: 추천 사용자의 재방문율

#### 5.3. 다양성 메트릭
- **Coverage**: 전체 게시글 중 추천에 포함된 비율
- **Diversity**: 추천 결과의 다양성 (카테고리, 학과, 가격대)
- **Novelty**: 사용자가 본 적 없는 새로운 게시글 비율

### 구현 내용
```java
@Service
public class AdvancedMetricsService {

    /**
     * 추천의 비즈니스 임팩트 계산
     */
    public BusinessImpact calculateBusinessImpact(LocalDateTime start, LocalDateTime end) {
        // 추천을 통한 거래 추적
        List<Post> recommendationDrivenPosts = findPostsFromRecommendation(start, end);

        long totalTransactions = recommendationDrivenPosts.stream()
            .filter(p -> p.getStatus() == COMPLETED)
            .count();

        long totalGMV = recommendationDrivenPosts.stream()
            .filter(p -> p.getStatus() == COMPLETED)
            .mapToLong(Post::getPrice)
            .sum();

        return BusinessImpact.builder()
            .transactions(totalTransactions)
            .gmv(totalGMV)
            .avgTransactionValue(totalTransactions > 0 ? totalGMV / totalTransactions : 0)
            .build();
    }

    /**
     * 추천 다양성 분석
     */
    public DiversityMetrics analyzeDiversity(List<Post> recommendations) {
        Set<String> categories = recommendations.stream()
            .map(Post::getCategory)
            .collect(Collectors.toSet());

        Set<Department> departments = recommendations.stream()
            .map(p -> p.getSubject().getDepartment())
            .collect(Collectors.toSet());

        // Gini 계수로 가격 분포 균등도 측정
        double priceGini = calculateGiniCoefficient(recommendations);

        return DiversityMetrics.builder()
            .uniqueCategories(categories.size())
            .uniqueDepartments(departments.size())
            .priceDistributionGini(priceGini)
            .diversityScore(calculateOverallDiversityScore(categories, departments, priceGini))
            .build();
    }
}
```

### 예상 개발 시간
- 참여도 메트릭: **4시간**
- 비즈니스 메트릭: **3시간**
- 다양성 메트릭: **3시간**
- Dashboard 업데이트: **3시간**
- **총 13시간**

---

## 🛣️ 전체 로드맵 요약

| Phase | 목표 | 개발 시간 | 우선순위 | 비즈니스 임팩트 |
|-------|------|----------|----------|----------------|
| **Phase 1** | MVP 추천 시스템 | - | ✅ 완료 | 기본 추천 제공 |
| **Phase 1.5** | 다중 행동 추천 + 성능 최적화 | 4시간 | ✅ 완료 | 정확도↑ 성능↑ |
| **Phase 2** | 노출 추적 & CTR | 3시간 | 🔴 High | 품질 측정 가능 |
| **Phase 3** | 정밀 노출 추적 | 4시간 | 🟡 Medium | 정확도 향상 |
| **Phase 4** | A/B 테스트 | 10시간 | 🟢 Low (추후) | 과학적 개선 |
| **Phase 5** | 고급 메트릭 | 13시간 | 🟢 Low (추후) | 비즈니스 인사이트 |

---

## 💡 권장 사항

### 현재 (3주차 완료 상태)
- ✅ **Phase 1.5 완료**: 다중 행동 추천 시스템 운영 중
- ✅ 클릭 수 중심의 간단한 메트릭으로 시스템 모니터링
- ✅ 클릭/찜/조회 데이터 수집 중
- 📊 **모니터링 포인트**:
  - 추천 클릭률 증가 추이 관찰
  - 사용자별 상호작용 이력 축적 상태 확인
  - 성능 메트릭 모니터링 (쿼리 수, 응답 시간)

### 4주차 이후 (안정화 기간)
- 🎯 **Phase 2 구현 권장** (3시간 투자로 큰 효과)
  - API 호출 기준 노출 추적 (간단하고 실용적)
  - 정확한 CTR로 추천 품질 측정
  - 다중 행동 추천 시스템의 효과 정량적 측정 가능

### 중기 (1-2개월 후)
- 📈 **데이터 분석 기반 개선**:
  - 클릭 데이터로 가중치 튜닝 (현재 1.0/0.7/0.3 조정)
  - 시간 감쇠 λ 값 최적화 (현재 0.1)
  - Cold Start 사용자 처리 개선

### 장기 확장
- 사용자 수가 증가하고 데이터가 충분히 쌓이면
- Phase 3, 4, 5를 단계적으로 도입
- A/B 테스트로 과학적 개선

---

## 📝 참고 자료

### 업계 벤치마크
- **Netflix**: CTR 2-10% (개인화 추천)
- **YouTube**: CTR 5-15% (홈 추천)
- **Amazon**: Conversion Rate 1-3% (추천 제품)

### 기술 참고
- [Intersection Observer API](https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API)
- [A/B Testing Best Practices](https://www.optimizely.com/optimization-glossary/ab-testing/)
- [Recommendation System Metrics](https://www.microsoft.com/en-us/research/publication/diversity-in-recommender-systems/)

---

## 📜 변경 이력

### v2.0 (2025-01-25)
- ✅ Phase 1.5 완료: 다중 행동 추천 시스템
- ✅ Implicit Feedback 통합 (클릭/찜/조회)
- ✅ 시간 감쇠 시스템 구현
- ✅ 성능 최적화 (N+1 쿼리 해결)
- 📊 로드맵 테이블 업데이트
- 📝 권장 사항 업데이트

### v1.0 (2025-01-18)
- 📄 초기 로드맵 작성
- Phase 1-5 계획 수립

---

**최종 수정일**: 2025-01-25
**버전**: 2.0
**작성자**: Claude (with User guidance)
