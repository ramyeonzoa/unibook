package com.unibook.performance;

import com.unibook.domain.entity.Department;
import com.unibook.repository.DepartmentRepository;
import com.unibook.utils.PerformanceResultSaver;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Department 조회 성능 종합 테스트
 * 
 * 현재 시스템(캐싱 없음)의 기준 성능을 측정하여
 * 향후 Caffeine Cache 도입 후 성능 비교의 기준점을 제공
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class DepartmentPerformanceTest extends PerformanceTestBase {
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private PerformanceResultSaver resultSaver;
    
    @Override
    protected void performWarmup() {
        log.info("🔥 JVM 워밍업 시작...");
        
        int warmupCount = benchmarkConfig.getWarmupIterations();
        
        // 각 학교별로 워밍업
        for (int i = 0; i < warmupCount; i++) {
            for (Long schoolId : benchmarkConfig.getTestSchoolIds()) {
                try {
                    List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                    
                    // 결과 사용 (JIT 컴파일러 최적화 유도)
                    if (!departments.isEmpty()) {
                        Department firstDept = departments.get(0);
                        String schoolName = firstDept.getSchool().getSchoolName(); // Lazy loading 트리거
                        @SuppressWarnings("unused")
                        String deptName = firstDept.getDepartmentName();
                    }
                } catch (Exception e) {
                    log.warn("워밍업 중 오류: schoolId={}", schoolId, e);
                }
            }
            
            if (i % 5 == 0 && benchmarkConfig.isEnableDetailedLogging()) {
                log.debug("워밍업 진행: {}/{}", i + 1, warmupCount);
            }
        }
        
        // 워밍업 후 GC
        performGarbageCollection();
        
        log.info("✅ JVM 워밍업 완료 ({} iterations)", warmupCount);
    }
    
    @Test
    @Order(1)
    @DisplayName("🔍 단일 학교 조회 성능 측정 - 기준점")
    void measureSingleSchoolPerformance() {
        log.info("=== 단일 학교 조회 성능 측정 시작 (Stressful 버전) ===");
        
        Long[] allSchoolIds = benchmarkConfig.getTestSchoolIds();
        List<Long> executionTimes = new ArrayList<>();
        int iterations = benchmarkConfig.getMeasurementIterations();
        
        // 측정 실행 - 더 많은 부하
        for (int i = 0; i < iterations; i++) {
            // 매번 캐시 클리어 (실제 상황 시뮬레이션)
            clearAllCaches();
            
            long startTime = System.nanoTime();
            
            // 10개 학교 모두 조회 (피크 시간 시뮬레이션)
            int totalDepartments = 0;
            StringBuilder resultBuilder = new StringBuilder();
            
            for (Long schoolId : allSchoolIds) {
                List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                
                // 실제 사용 패턴 시뮬레이션 - 모든 데이터 접근
                for (Department dept : departments) {
                    // Lazy Loading 강제 트리거
                    String schoolName = dept.getSchool().getSchoolName();
                    String schoolDomain = dept.getSchool().getPrimaryDomain();
                    String deptName = dept.getDepartmentName();
                    Long deptId = dept.getDepartmentId();
                    
                    // 실제 비즈니스 로직 시뮬레이션 (드롭다운 메뉴 생성)
                    String option = String.format(
                        "<option value='%d' data-school='%s'>%s (%s)</option>",
                        deptId, schoolName, deptName, schoolDomain
                    );
                    resultBuilder.append(option);
                    
                    totalDepartments++;
                }
            }
            
            // 결과 사용 (JIT 최적화 방지)
            String result = resultBuilder.toString();
            if (result.isEmpty()) {
                throw new IllegalStateException("No departments found");
            }
            
            long endTime = System.nanoTime();
            long executionTime = endTime - startTime; // 나노초 그대로
            
            executionTimes.add(executionTime);
            
            // 결과 검증
            assertTrue(totalDepartments > 0, "학과가 조회되지 않음");
            
            // 진행상황 로깅
            if (i % 20 == 0 && benchmarkConfig.isEnableDetailedLogging()) {
                log.debug("측정 진행: {}/{} (현재: {}ms)", i + 1, iterations, executionTime);
            }
        }
        
        // 통계 계산 및 출력
        StatisticsCalculator.PerformanceStatistics stats = 
            StatisticsCalculator.calculateStatistics(executionTimes, "단일 학교 조회 (기준)");
        stats.printDetailedReport();
        
        // 성능 기준값 검증
        validatePerformanceThreshold(stats.getMean(), "단일 학교 조회");
        
        // 결과 저장
        resultSaver.saveStatistics("single_school_query_baseline", stats);
        
        log.info("✅ 단일 학교 조회 성능 측정 완료 - 평균: {:.2f}ms", stats.getMean());
    }
    
    @Test
    @Order(2)
    @DisplayName("🔄 반복 조회 성능 측정 - 캐싱 효과 확인")
    void measureRepeatedQueryPerformance() {
        log.info("=== 반복 조회 성능 측정 시작 (Stressful 버전) ===");
        
        Long[] allSchoolIds = benchmarkConfig.getTestSchoolIds();
        
        // 첫 번째 조회 (콜드 스타트) 측정 - 더 많은 데이터
        List<Long> coldStartTimes = new ArrayList<>();
        for (int i = 0; i < 20; i++) { // 더 많은 샘플
            clearAllCaches(); // 매번 캐시 클리어
            
            long startTime = System.nanoTime();
            
            // 모든 학교 조회 (실제 시나리오: 관리자 대시보드)
            Map<String, List<String>> schoolDeptMap = new HashMap<>();
            
            for (Long schoolId : allSchoolIds) {
                List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                
                // 실제 사용 패턴 - 학교별 학과 목록 구성
                List<String> deptList = new ArrayList<>();
                String schoolName = "";
                
                for (Department dept : departments) {
                    // Lazy Loading 모두 트리거
                    schoolName = dept.getSchool().getSchoolName();
                    String deptName = dept.getDepartmentName();
                    String fullInfo = String.format("%s (%d)", deptName, dept.getDepartmentId());
                    deptList.add(fullInfo);
                }
                
                if (!schoolName.isEmpty()) {
                    schoolDeptMap.put(schoolName, deptList);
                }
            }
            
            // 결과 검증 (JIT 최적화 방지)
            if (schoolDeptMap.size() != allSchoolIds.length) {
                throw new IllegalStateException("Not all schools processed");
            }
            
            long endTime = System.nanoTime();
            coldStartTimes.add(endTime - startTime); // 나노초 그대로 저장
        }
        
        // 연속 조회 (웜 스타트) 측정 - 캐시 있는 상태에서의 연속 조회
        List<Long> warmStartTimes = new ArrayList<>();
        
        // 한 번 전체 조회로 캐시 워밍
        for (Long schoolId : allSchoolIds) {
            departmentRepository.findBySchool_SchoolId(schoolId);
        }
        
        // 실제 측정
        for (int i = 0; i < benchmarkConfig.getMeasurementIterations(); i++) {
            long startTime = System.nanoTime();
            
            // 동일한 패턴으로 전체 학교 조회 (캐시 활용)
            Map<String, List<String>> schoolDeptMap = new HashMap<>();
            
            for (Long schoolId : allSchoolIds) {
                List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                
                // 동일한 사용 패턴 적용
                List<String> deptList = new ArrayList<>();
                String schoolName = "";
                
                for (Department dept : departments) {
                    schoolName = dept.getSchool().getSchoolName();
                    String deptName = dept.getDepartmentName();
                    String fullInfo = String.format("%s (%d)", deptName, dept.getDepartmentId());
                    deptList.add(fullInfo);
                }
                
                if (!schoolName.isEmpty()) {
                    schoolDeptMap.put(schoolName, deptList);
                }
            }
            
            // 결과 검증
            if (schoolDeptMap.size() != allSchoolIds.length) {
                throw new IllegalStateException("Not all schools processed");
            }
            
            long endTime = System.nanoTime();
            warmStartTimes.add(endTime - startTime); // 나노초 그대로 저장
        }
        
        // 통계 계산
        StatisticsCalculator.PerformanceStatistics coldStats = 
            StatisticsCalculator.calculateStatistics(coldStartTimes, "첫 번째 조회 (콜드)");
        StatisticsCalculator.PerformanceStatistics warmStats = 
            StatisticsCalculator.calculateStatistics(warmStartTimes, "반복 조회 (웜)");
        
        coldStats.printDetailedReport();
        warmStats.printDetailedReport();
        
        // 현재 시스템에서의 캐싱 효과 분석 (거의 없을 것으로 예상)
        double improvementRatio = coldStats.getMean() / warmStats.getMean();
        log.info("🔄 현재 시스템 반복 조회 효과: {:.2f}배 (JPA 1차 캐시 등)", improvementRatio);
        
        // 결과 저장
        resultSaver.saveStatistics("cold_start_query_baseline", coldStats);
        resultSaver.saveStatistics("repeated_query_baseline", warmStats);
        
        log.info("✅ 반복 조회 성능 측정 완료");
    }
    
    @Test
    @Order(3)
    @DisplayName("🌐 다중 학교 조회 성능 측정")
    void measureMultipleSchoolPerformance() {
        log.info("=== 다중 학교 조회 성능 측정 시작 (Stressful 버전) ===");
        
        Map<Long, StatisticsCalculator.PerformanceStatistics> schoolStats = new HashMap<>();
        List<Long> allExecutionTimes = new ArrayList<>();
        
        // 더 많은 학교 ID 생성 (실제 부하 시뮬레이션)
        List<Long> extendedSchoolIds = new ArrayList<>();
        for (int i = 1; i <= 30; i++) { // 30개 학교
            extendedSchoolIds.add((long) i);
        }
        
        for (Long schoolId : extendedSchoolIds) {
            List<Long> executionTimes = new ArrayList<>();
            
            log.debug("학교 ID {} 측정 시작", schoolId);
            
            for (int i = 0; i < 50; i++) { // 학교별 50회 측정 (더 많은 샘플)
                // 매번 캐시 클리어 (최악의 상황 시뮬레이션)
                clearAllCaches();
                
                long startTime = System.nanoTime();
                
                try {
                    List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                    
                    // 실제 사용 패턴: 학과 선택 드롭다운 생성
                    StringBuilder htmlBuilder = new StringBuilder();
                    htmlBuilder.append("<select name='department'>");
                    
                    for (Department dept : departments) {
                        // 모든 lazy loading 트리거
                        String schoolName = dept.getSchool().getSchoolName();
                        String schoolDomain = dept.getSchool().getPrimaryDomain();
                        String deptName = dept.getDepartmentName();
                        Long deptId = dept.getDepartmentId();
                        
                        // HTML 생성 (실제 UI 렌더링 시뮬레이션)
                        String option = String.format(
                            "<option value='%d' data-school='%s' data-domain='%s'>%s</option>",
                            deptId, schoolName, schoolDomain, deptName
                        );
                        htmlBuilder.append(option);
                    }
                    
                    htmlBuilder.append("</select>");
                    
                    // 결과 사용 (JIT 최적화 방지)
                    String html = htmlBuilder.toString();
                    if (html.length() < 50) { // 최소한의 HTML 길이 체크
                        throw new IllegalStateException("Invalid HTML generated");
                    }
                    
                } catch (Exception e) {
                    log.warn("학교 {} 조회 중 오류 발생: {}", schoolId, e.getMessage());
                }
                
                long endTime = System.nanoTime();
                long executionTime = endTime - startTime; // 나노초
                
                executionTimes.add(executionTime);
                allExecutionTimes.add(executionTime);
            }
            
            StatisticsCalculator.PerformanceStatistics stats = 
                StatisticsCalculator.calculateStatistics(executionTimes, "학교 " + schoolId);
            schoolStats.put(schoolId, stats);
            
            log.debug("학교 ID {} 완료 - 평균: {:.2f}ms", schoolId, stats.getMean());
        }
        
        // 결과 출력
        System.out.println("\n" + "=".repeat(90));
        System.out.println("📊 학교별 조회 성능 비교");
        System.out.println("=".repeat(90));
        System.out.printf("%-12s %-15s %-12s %-12s %-12s %-15s\n", 
                "학교 ID", "평균 (ms)", "중앙값", "P95", "표준편차", "학과 수");
        System.out.println("-".repeat(90));
        
        schoolStats.forEach((schoolId, stats) -> {
            long deptCount = databaseStateManager.getDepartmentCount(schoolId);
            System.out.printf("%-12d %-15.2f %-12.2f %-12.2f %-12.2f %-15d\n",
                    schoolId, stats.getMean(), stats.getMedian(), stats.getP95(), 
                    stats.getStandardDeviation(), deptCount);
        });
        
        // 전체 통계
        StatisticsCalculator.PerformanceStatistics overallStats = 
            StatisticsCalculator.calculateStatistics(allExecutionTimes, "전체 학교 평균 (기준)");
        
        System.out.println("-".repeat(90));
        System.out.printf("전체 평균: %.2f ms (P95: %.2f ms, 표준편차: %.2f)\n", 
                overallStats.getMean(), overallStats.getP95(), overallStats.getStandardDeviation());
        System.out.println("=".repeat(90));
        
        overallStats.printDetailedReport();
        
        // 결과 저장
        resultSaver.saveStatistics("multiple_schools_baseline", overallStats);
        
        log.info("✅ 다중 학교 조회 성능 측정 완료");
    }
    
    @Test
    @Order(4)
    @DisplayName("⚡ 동시 접근 성능 측정")
    void measureConcurrentPerformance() throws InterruptedException {
        log.info("=== 동시 접근 성능 측정 시작 ===");
        
        int threadCount = benchmarkConfig.getConcurrentThreads();
        int operationsPerThread = benchmarkConfig.getOperationsPerThread();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        List<Long> allExecutionTimes = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        // 워커 스레드 생성
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기
                    
                    // 각 스레드가 다른 학교들을 조회 (실제 동시 사용자 시뮬레이션)
                    Random random = new Random(threadId);
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        // 랜덤하게 학교 선택 (캐시 미스 증가)
                        Long schoolId = (long) (random.nextInt(50) + 1);
                        
                        long startTime = System.nanoTime();
                        
                        try {
                            List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                            
                            // 실제 사용 패턴: 회원가입 폼 학과 검증
                            boolean foundTarget = false;
                            String targetDeptName = "컴퓨터공학과"; // 자주 검색되는 학과
                            
                            for (Department dept : departments) {
                                // 모든 필드 접근 (Lazy Loading)
                                String schoolName = dept.getSchool().getSchoolName();
                                String schoolDomain = dept.getSchool().getPrimaryDomain();
                                String deptName = dept.getDepartmentName();
                                Long deptId = dept.getDepartmentId();
                                
                                // 검색 로직 시뮬레이션
                                if (deptName.contains(targetDeptName)) {
                                    foundTarget = true;
                                    // 선택된 학과 정보 구성
                                    String selectedInfo = String.format(
                                        "Selected: [%d] %s - %s (%s)",
                                        deptId, schoolName, deptName, schoolDomain
                                    );
                                    // 결과 사용
                                    if (selectedInfo.isEmpty()) {
                                        throw new IllegalStateException("Empty selection");
                                    }
                                }
                            }
                            
                            // 실제 비즈니스 로직 검증
                            if (departments.isEmpty()) {
                                throw new RuntimeException("No departments for schoolId: " + schoolId);
                            }
                            
                        } catch (Exception e) {
                            // 학교가 존재하지 않을 수 있음 (정상적인 상황)
                            log.debug("School {} not found or error: {}", schoolId, e.getMessage());
                        }
                        
                        long endTime = System.nanoTime();
                        allExecutionTimes.add(endTime - startTime); // 나노초
                        
                        // 실제 사용자 행동 패턴 시뮬레이션 (빠른 연속 클릭 방지)
                        if (i % 5 == 0) {
                            Thread.sleep(random.nextInt(5) + 1); // 1-5ms 랜덤 대기
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("스레드 {} 오류", threadId, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 실행 시작
        long totalStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // 완료 대기
        boolean completed = endLatch.await(5, TimeUnit.MINUTES); // 5분 타임아웃
        long totalEndTime = System.currentTimeMillis();
        
        executor.shutdown();
        
        if (!completed) {
            log.error("❌ 동시 접근 테스트 타임아웃");
            Assertions.fail("테스트 타임아웃");
        }
        
        // 예외 확인
        if (!exceptions.isEmpty()) {
            log.error("동시 접근 테스트 중 {} 개의 예외 발생", exceptions.size());
            exceptions.forEach(e -> log.error("예외: ", e));
        }
        
        // 결과 분석
        long totalExecutionTime = totalEndTime - totalStartTime;
        int totalOperations = threadCount * operationsPerThread - exceptions.size();
        double throughput = (totalOperations * 1000.0) / totalExecutionTime;
        
        StatisticsCalculator.PerformanceStatistics stats = 
            StatisticsCalculator.calculateStatistics(allExecutionTimes, "동시 접근 (기준)");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("⚡ 동시 접근 성능 결과");
        System.out.println("=".repeat(80));
        System.out.printf("동시 스레드 수: %d\n", threadCount);
        System.out.printf("스레드당 작업 수: %d\n", operationsPerThread);
        System.out.printf("총 성공 작업 수: %,d\n", totalOperations);
        System.out.printf("총 실행 시간: %,d ms\n", totalExecutionTime);
        System.out.printf("처리량: %.2f ops/sec\n", throughput);
        System.out.printf("에러 발생: %d건 (%.2f%%)\n", 
                exceptions.size(), (exceptions.size() * 100.0) / (threadCount * operationsPerThread));
        System.out.println("=".repeat(80));
        
        stats.printDetailedReport();
        
        // 동시성 관련 추가 지표 저장
        Map<String, Object> concurrentMetrics = new HashMap<>();
        concurrentMetrics.put("threadCount", threadCount);
        concurrentMetrics.put("throughput", throughput);
        concurrentMetrics.put("totalExecutionTime", totalExecutionTime);
        concurrentMetrics.put("errorCount", exceptions.size());
        concurrentMetrics.put("errorRate", (exceptions.size() * 100.0) / (threadCount * operationsPerThread));
        concurrentMetrics.put("successfulOperations", totalOperations);
        
        resultSaver.saveStatistics("concurrent_access_baseline", stats);
        resultSaver.saveAdditionalMetrics("concurrent_access_baseline", concurrentMetrics);
        
        log.info("✅ 동시 접근 성능 측정 완료 - 처리량: {:.2f} ops/sec", throughput);
    }
    
    @Test
    @Order(5)
    @DisplayName("🎭 실제 사용 시나리오 시뮬레이션")
    void measureRealWorldScenario() {
        log.info("=== 실제 사용 시나리오 측정 시작 (Stressful 버전) ===");
        
        List<Long> signupScenarioTimes = new ArrayList<>();
        List<Long> postCreateScenarioTimes = new ArrayList<>();
        List<Long> profileEditScenarioTimes = new ArrayList<>();
        
        // 시나리오 1: 회원가입 (학교 선택 → 학과 선택 → 검증)
        log.debug("시나리오 1: 회원가입 프로세스 (복잡한 버전)");
        for (int i = 0; i < 50; i++) {
            clearAllCaches(); // 각 회원가입은 새로운 세션
            long startTime = System.nanoTime();
            
            // 1단계: 사용자가 여러 학교를 탐색 (평균 3-5개 학교)
            Random random = new Random();
            int schoolsToExplore = random.nextInt(3) + 3;
            Map<Long, List<Department>> exploredSchools = new HashMap<>();
            
            for (int j = 0; j < schoolsToExplore; j++) {
                Long schoolId = (long) (random.nextInt(30) + 1);
                List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                
                // 각 학교의 학과 목록을 모두 확인
                List<Department> deptList = new ArrayList<>();
                for (Department dept : departments) {
                    // 모든 정보 로딩 (드롭다운 렌더링)
                    String schoolName = dept.getSchool().getSchoolName();
                    String schoolDomain = dept.getSchool().getPrimaryDomain();
                    String deptName = dept.getDepartmentName();
                    
                    // 사용자가 관심있는 학과 찾기
                    if (deptName.contains("공학") || deptName.contains("컴퓨터") || 
                        deptName.contains("경영") || deptName.contains("경제")) {
                        deptList.add(dept);
                    }
                }
                
                if (!deptList.isEmpty()) {
                    exploredSchools.put(schoolId, deptList);
                }
            }
            
            // 2단계: 최종 학교/학과 선택 및 검증
            if (!exploredSchools.isEmpty()) {
                Map.Entry<Long, List<Department>> selected = 
                    exploredSchools.entrySet().iterator().next();
                
                Department finalChoice = selected.getValue().get(0);
                
                // 선택한 학과 상세 정보 확인 (회원가입 폼 제출 전 검증)
                String validationInfo = String.format(
                    "Selected: %s - %s (%s) [ID: %d]",
                    finalChoice.getSchool().getSchoolName(),
                    finalChoice.getDepartmentName(),
                    finalChoice.getSchool().getPrimaryDomain(),
                    finalChoice.getDepartmentId()
                );
                
                // 이메일 도메인 검증 시뮬레이션
                String email = "user@" + finalChoice.getSchool().getPrimaryDomain();
                if (!email.contains(finalChoice.getSchool().getPrimaryDomain())) {
                    throw new IllegalStateException("Email domain mismatch");
                }
            }
            
            long endTime = System.nanoTime();
            signupScenarioTimes.add(endTime - startTime); // 나노초
        }
        
        // 시나리오 2: 게시글 작성 (사용자 정보 기반 학과 조회 + 교수 정보)
        log.debug("시나리오 2: 게시글 작성 프로세스 (복잡한 버전)");
        for (int i = 0; i < 50; i++) {
            clearAllCaches(); // 각 게시글 작성은 새로운 요청
            long startTime = System.nanoTime();
            
            // 사용자의 학교 정보로 학과 목록 조회
            Long userSchoolId = (long) (new Random().nextInt(10) + 1);
            List<Department> departments = departmentRepository.findBySchool_SchoolId(userSchoolId);
            
            // 폼 데이터 준비 - SELECT 박스 렌더링
            StringBuilder formHtml = new StringBuilder();
            formHtml.append("<form id='postForm'>\n");
            formHtml.append("  <label>학과 선택:</label>\n");
            formHtml.append("  <select name='department' id='deptSelect'>\n");
            
            // 모든 학과 옵션 생성
            for (Department dept : departments) {
                String schoolName = dept.getSchool().getSchoolName();
                String deptName = dept.getDepartmentName();
                Long deptId = dept.getDepartmentId();
                
                formHtml.append(String.format(
                    "    <option value='%d' data-school='%s'>%s</option>\n",
                    deptId, schoolName, deptName
                ));
            }
            
            formHtml.append("  </select>\n");
            
            // 추가: 교수 선택 드롭다운도 준비한다고 가정
            formHtml.append("  <label>교수 선택:</label>\n");
            formHtml.append("  <select name='professor' id='profSelect'>\n");
            formHtml.append("    <option value=''>선택하세요</option>\n");
            formHtml.append("  </select>\n");
            formHtml.append("</form>\n");
            
            // 결과 검증
            String html = formHtml.toString();
            if (html.length() < 100) {
                throw new IllegalStateException("Form HTML too short");
            }
            
            long endTime = System.nanoTime();
            postCreateScenarioTimes.add(endTime - startTime); // 나노초
        }
        
        // 시나리오 3: 프로필 수정 (학교/학과 변경 - 복잡한 탐색)
        log.debug("시나리오 3: 프로필 수정 프로세스 (복잡한 버전)");
        for (int i = 0; i < 40; i++) {
            clearAllCaches(); // 각 프로필 수정은 새로운 세션
            long startTime = System.nanoTime();
            
            // 현재 사용자 정보 시뮬레이션
            Long currentSchoolId = (long) (new Random().nextInt(10) + 1);
            Department currentDept = null;
            
            // 현재 학과 정보 조회
            List<Department> currentDepts = departmentRepository.findBySchool_SchoolId(currentSchoolId);
            if (!currentDepts.isEmpty()) {
                currentDept = currentDepts.get(0);
                // 현재 정보 확인
                String currentInfo = String.format(
                    "Current: %s - %s",
                    currentDept.getSchool().getSchoolName(),
                    currentDept.getDepartmentName()
                );
            }
            
            // 다른 학교들 탐색 (학교 변경 고려)
            Map<String, List<String>> schoolOptions = new HashMap<>();
            for (int j = 0; j < 5; j++) { // 5개 학교 탐색
                Long schoolId = (long) (new Random().nextInt(20) + 1);
                List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);
                
                if (!departments.isEmpty()) {
                    String schoolName = departments.get(0).getSchool().getSchoolName();
                    List<String> deptNames = new ArrayList<>();
                    
                    // 각 학교의 모든 학과 확인
                    for (Department dept : departments) {
                        deptNames.add(dept.getDepartmentName());
                        // 추가 정보도 확인
                        String domain = dept.getSchool().getPrimaryDomain();
                        if (domain.isEmpty()) {
                            throw new IllegalStateException("Empty domain");
                        }
                    }
                    
                    schoolOptions.put(schoolName, deptNames);
                }
            }
            
            // 최종 선택 시뮬레이션
            if (!schoolOptions.isEmpty() && currentDept != null) {
                String decision = String.format(
                    "Change from %s to one of %d options",
                    currentDept.getSchool().getSchoolName(),
                    schoolOptions.size()
                );
                if (decision.isEmpty()) {
                    throw new IllegalStateException("Decision failed");
                }
            }
            
            long endTime = System.nanoTime();
            profileEditScenarioTimes.add(endTime - startTime); // 나노초
        }
        
        // 결과 분석
        StatisticsCalculator.PerformanceStatistics signupStats = 
            StatisticsCalculator.calculateStatistics(signupScenarioTimes, "회원가입 시나리오 (기준)");
        StatisticsCalculator.PerformanceStatistics postCreateStats = 
            StatisticsCalculator.calculateStatistics(postCreateScenarioTimes, "게시글 작성 시나리오 (기준)");
        StatisticsCalculator.PerformanceStatistics profileEditStats = 
            StatisticsCalculator.calculateStatistics(profileEditScenarioTimes, "프로필 수정 시나리오 (기준)");
        
        // 상세 리포트 출력
        signupStats.printDetailedReport();
        postCreateStats.printDetailedReport();
        profileEditStats.printDetailedReport();
        
        // 시나리오별 요약
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎭 실제 사용 시나리오 성능 요약");
        System.out.println("=".repeat(80));
        signupStats.printSummary();
        postCreateStats.printSummary();
        profileEditStats.printSummary();
        System.out.println("=".repeat(80));
        
        // 성능 기준값 검증
        validatePerformanceThreshold(signupStats.getMean(), "회원가입 시나리오");
        validatePerformanceThreshold(postCreateStats.getMean(), "게시글 작성 시나리오");
        validatePerformanceThreshold(profileEditStats.getMean(), "프로필 수정 시나리오");
        
        // 결과 저장
        resultSaver.saveStatistics("signup_scenario_baseline", signupStats);
        resultSaver.saveStatistics("post_create_scenario_baseline", postCreateStats);
        resultSaver.saveStatistics("profile_edit_scenario_baseline", profileEditStats);
        
        log.info("✅ 실제 사용 시나리오 측정 완료");
    }
    
    @Test
    @Order(6)
    @DisplayName("📊 종합 성능 리포트 생성")
    void generateFinalReport() {
        log.info("=== 종합 성능 리포트 생성 ===");
        
        // 시스템 리소스 상태 출력
        printSystemResources();
        
        // 전체 요약 리포트 생성
        resultSaver.generateSummaryReport();
        
        // 성능 기준점 설정 권장사항
        System.out.println("\n" + "=".repeat(80));
        System.out.println("💡 Caffeine Cache 도입 시 성능 개선 예상치");
        System.out.println("=".repeat(80));
        System.out.println("📈 예상 개선 효과:");
        System.out.println("   • 단일 학교 조회: 90-95% 향상 (캐시 히트 시)");
        System.out.println("   • 반복 조회: 98% 향상 (거의 즉시 응답)");
        System.out.println("   • 동시 접근: 70-80% 향상 (DB 부하 감소)");
        System.out.println("   • 실제 시나리오: 85-90% 향상 (전체적 UX 개선)");
        System.out.println("\n📊 성능 기준점 (현재):");
        System.out.printf("   • 허용 가능한 응답시간: %.1f ms 이하\n", benchmarkConfig.getPerformanceThresholdMs());
        System.out.println("   • 목표 캐시 히트율: 95% 이상");
        System.out.println("   • 메모리 사용량 제한: 50MB 이하");
        System.out.println("=".repeat(80));
        
        log.info("✅ 종합 성능 리포트 생성 완료");
    }
}