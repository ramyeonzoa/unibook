package com.unibook.repository;

import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DepartmentRepository 캐싱 안전성 검증 테스트
 * 
 * 검증 항목:
 * 1. findBySchool_SchoolId() 메서드 정상 동작
 * 2. @Cacheable 적용으로 캐시 재사용 확인
 * 3. @EntityGraph 정상 동작 (N+1 문제 해결)
 * 4. 기존 API들의 정상 동작
 */
@DataJpaTest
@ContextConfiguration(classes = {DepartmentRepositoryCacheTest.CacheTestConfig.class})
@TestPropertySource(properties = {
    "spring.cache.type=simple",
    "spring.jpa.show-sql=false"
})
@Transactional
class DepartmentRepositoryCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("departments");
        }
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CacheManager cacheManager;

    private School testSchool;
    private Department testDepartment1;
    private Department testDepartment2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testSchool = new School();
        testSchool.setSchoolName("테스트대학교");
        testSchool.setPrimaryDomain("test.ac.kr");
        testSchool.getAllDomains().add("test.ac.kr");
        testSchool = entityManager.persistAndFlush(testSchool);

        testDepartment1 = new Department();
        testDepartment1.setDepartmentName("컴퓨터공학과");
        testDepartment1.setSchool(testSchool);
        testDepartment1 = entityManager.persistAndFlush(testDepartment1);

        testDepartment2 = new Department();
        testDepartment2.setDepartmentName("소프트웨어학과");
        testDepartment2.setSchool(testSchool);
        testDepartment2 = entityManager.persistAndFlush(testDepartment2);

        entityManager.clear();
        
        // 캐시 초기화
        if (cacheManager.getCache("departments") != null) {
            cacheManager.getCache("departments").clear();
        }
    }

    @Test
    @DisplayName("findBySchool_SchoolId() 메서드 정상 동작 검증")
    void testFindBySchoolSchoolIdBasicFunctionality() {
        // given
        Long schoolId = testSchool.getSchoolId();

        // when
        List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);

        // then
        assertThat(departments).hasSize(2);
        assertThat(departments)
                .extracting(Department::getDepartmentName)
                .containsExactlyInAnyOrder("컴퓨터공학과", "소프트웨어학과");
    }

    @Test
    @DisplayName("@EntityGraph 정상 동작 검증 - School 정보 즉시 로딩")
    void testEntityGraphLoadsSchoolEagerly() {
        // given
        Long schoolId = testSchool.getSchoolId();

        // when
        List<Department> departments = departmentRepository.findBySchool_SchoolId(schoolId);

        // then
        assertThat(departments).isNotEmpty();
        
        // EntityGraph로 School이 즉시 로딩되어야 함 (LazyInitializationException 발생하지 않음)
        Department department = departments.get(0);
        assertThat(department.getSchool()).isNotNull();
        assertThat(department.getSchool().getSchoolName()).isEqualTo("테스트대학교");
        assertThat(department.getSchool().getPrimaryDomain()).isEqualTo("test.ac.kr");
    }

    @Test
    @DisplayName("@Cacheable 적용 검증 - 두 번째 호출 시 캐시 사용")
    void testCacheableAnnotationWorks() {
        // given
        Long schoolId = testSchool.getSchoolId();
        
        // 캐시가 비어있는지 확인
        assertThat(cacheManager.getCache("departments").get(schoolId)).isNull();

        // when - 첫 번째 호출 (데이터베이스 조회)
        List<Department> firstCall = departmentRepository.findBySchool_SchoolId(schoolId);
        
        // 캐시에 저장되었는지 확인
        assertThat(cacheManager.getCache("departments").get(schoolId)).isNotNull();

        // when - 두 번째 호출 (캐시에서 조회)
        List<Department> secondCall = departmentRepository.findBySchool_SchoolId(schoolId);

        // then
        assertThat(firstCall).hasSize(2);
        assertThat(secondCall).hasSize(2);
        
        // 같은 결과가 반환되어야 함
        assertThat(firstCall)
                .extracting(Department::getDepartmentName)
                .containsExactlyInAnyOrderElementsOf(
                        secondCall.stream()
                                .map(Department::getDepartmentName)
                                .toList()
                );
    }

    @Test
    @DisplayName("존재하지 않는 학교 ID로 조회 시 빈 리스트 반환")
    void testFindByNonExistentSchoolId() {
        // given
        Long nonExistentSchoolId = 99999L;

        // when
        List<Department> departments = departmentRepository.findBySchool_SchoolId(nonExistentSchoolId);

        // then
        assertThat(departments).isEmpty();
    }

    @Test
    @DisplayName("기존 API 정상 동작 검증 - existsByDepartmentNameAndSchool_SchoolId")
    void testExistsByDepartmentNameAndSchoolSchoolId() {
        // given
        Long schoolId = testSchool.getSchoolId();

        // when & then
        assertThat(departmentRepository.existsByDepartmentNameAndSchool_SchoolId("컴퓨터공학과", schoolId))
                .isTrue();
        assertThat(departmentRepository.existsByDepartmentNameAndSchool_SchoolId("존재하지않는학과", schoolId))
                .isFalse();
    }

    @Test
    @DisplayName("기존 API 정상 동작 검증 - findBySchool_SchoolIdAndDepartmentName")
    void testFindBySchoolSchoolIdAndDepartmentName() {
        // given
        Long schoolId = testSchool.getSchoolId();

        // when
        var found = departmentRepository.findBySchool_SchoolIdAndDepartmentName(schoolId, "컴퓨터공학과");
        var notFound = departmentRepository.findBySchool_SchoolIdAndDepartmentName(schoolId, "존재하지않는학과");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getDepartmentName()).isEqualTo("컴퓨터공학과");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("기존 API 정상 동작 검증 - countBySchool_SchoolId")
    void testCountBySchoolSchoolId() {
        // given
        Long schoolId = testSchool.getSchoolId();

        // when
        long count = departmentRepository.countBySchool_SchoolId(schoolId);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("캐시 키 독립성 검증 - 다른 학교별로 독립적 캐싱")
    void testCacheKeyIndependence() {
        // given
        School anotherSchool = new School();
        anotherSchool.setSchoolName("다른대학교");
        anotherSchool.setPrimaryDomain("another.ac.kr");
        anotherSchool.getAllDomains().add("another.ac.kr");
        anotherSchool = entityManager.persistAndFlush(anotherSchool);

        Department anotherDepartment = new Department();
        anotherDepartment.setDepartmentName("경영학과");
        anotherDepartment.setSchool(anotherSchool);
        entityManager.persistAndFlush(anotherDepartment);

        entityManager.clear();

        // when
        List<Department> school1Departments = departmentRepository.findBySchool_SchoolId(testSchool.getSchoolId());
        List<Department> school2Departments = departmentRepository.findBySchool_SchoolId(anotherSchool.getSchoolId());

        // then
        assertThat(school1Departments).hasSize(2);
        assertThat(school2Departments).hasSize(1);
        
        // 각각 독립적으로 캐시되어야 함
        assertThat(cacheManager.getCache("departments").get(testSchool.getSchoolId())).isNotNull();
        assertThat(cacheManager.getCache("departments").get(anotherSchool.getSchoolId())).isNotNull();
    }

    @Test
    @DisplayName("성능 검증 - 캐시된 결과의 일관성")
    void testCacheConsistency() {
        // given
        Long schoolId = testSchool.getSchoolId();

        // when - 여러 번 호출하여 일관성 확인
        List<Department> call1 = departmentRepository.findBySchool_SchoolId(schoolId);
        List<Department> call2 = departmentRepository.findBySchool_SchoolId(schoolId);
        List<Department> call3 = departmentRepository.findBySchool_SchoolId(schoolId);

        // then - 모든 호출 결과가 동일해야 함
        assertThat(call1).hasSize(2);
        assertThat(call2).hasSize(2);
        assertThat(call3).hasSize(2);

        assertThat(call1)
                .extracting(Department::getDepartmentName)
                .containsExactlyInAnyOrderElementsOf(
                        call2.stream().map(Department::getDepartmentName).toList()
                )
                .containsExactlyInAnyOrderElementsOf(
                        call3.stream().map(Department::getDepartmentName).toList()
                );
    }
}