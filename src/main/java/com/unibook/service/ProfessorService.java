package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.ProfessorDto;
import com.unibook.domain.entity.Department;
import com.unibook.domain.entity.Professor;
import com.unibook.exception.DuplicateResourceException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.DepartmentRepository;
import com.unibook.repository.ProfessorRepository;
import com.unibook.util.QueryNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 교수 관련 비즈니스 로직을 처리하는 서비스
 * - 네이밍 컨벤션: find… 통일 (get 제거)
 * - 방어 코드: 페이징 파라미터 검증, null 안전성
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {
    
    private final ProfessorRepository professorRepository;
    private final DepartmentRepository departmentRepository;
    private final UserService userService;
    
    /**
     * 교수명으로 검색 (학교 제한 없음 - 모든 학교에서 검색)
     * 읽기 전용 트랜잭션은 클래스 레벨에서 상속됨
     * 
     * @param query 검색어 (컨트롤러에서 검증됨)
     * @param departmentId 우선순위를 적용할 학과 ID (nullable)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 교수 목록
     */
    public Page<ProfessorDto> findProfessorsByName(String query, Long departmentId, int page, int size) {
        // normalize()에서 null 방어 처리됨
        String normalizedQuery = QueryNormalizer.normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Page.empty();
        }
        
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("교수 검색: query='{}', normalized='{}', departmentId={}, page={}, size={}", 
                 query, normalizedQuery, departmentId, page, size);
        
        return professorRepository.findProfessorsByName(normalizedQuery, departmentId, pageable);
    }
    
    /**
     * 교수명으로 검색 (특정 학교 내에서만)
     * 읽기 전용 트랜잭션은 클래스 레벨에서 상속됨
     * 
     * @param query 검색어 (컨트롤러에서 검증됨)
     * @param schoolId 학교 ID (컨트롤러에서 필수 검증됨)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 교수 목록
     */
    public Page<ProfessorDto> findProfessorsBySchool(String query, Long schoolId, int page, int size) {
        // normalize()에서 null 방어 처리됨
        String normalizedQuery = QueryNormalizer.normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Page.empty();
        }
        
        // schoolId null 체크는 컨트롤러에서 처리됨
        
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("학교별 교수 검색: query='{}', normalized='{}', schoolId={}, page={}, size={}", 
                 query, normalizedQuery, schoolId, page, size);
        
        return professorRepository.findProfessorsByNameAndSchool(normalizedQuery, schoolId, pageable);
    }
    
    /**
     * 특정 학과의 교수 목록 조회
     * 컨벤션 통일: find… (get 제거)
     * 
     * @param departmentId 학과 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 교수 목록
     */
    public Page<ProfessorDto> findProfessorsByDepartment(Long departmentId, int page, int size) {
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("학과별 교수 조회: departmentId={}, page={}, size={}", departmentId, page, size);
        
        return professorRepository.findProfessorsByDepartment(departmentId, pageable);
    }
    
    /**
     * 교수 존재 여부 확인
     * 
     * @param professorId 교수 ID
     * @return 존재 여부
     */
    public boolean existsById(Long professorId) {
        return professorId != null && professorRepository.existsById(professorId);
    }
    
    /**
     * 교수 정보 조회 (DTO 프로젝션)
     * 
     * @param professorId 교수 ID
     * @return 교수 정보 (Optional)
     */
    public Optional<ProfessorDto> findProfessorById(Long professorId) {
        if (professorId == null) {
            return Optional.empty();
        }
        return professorRepository.findProfessorDtoById(professorId);
    }
    
    /**
     * 교수 중복 체크 (정규화된 이름으로 비교)
     * 
     * @param professorName 교수명
     * @param departmentId 학과 ID
     * @return 중복 여부
     */
    public boolean isDuplicateProfessor(String professorName, Long departmentId) {
        if (departmentId == null) {
            return false;
        }
        
        // normalize()에서 null 방어 처리됨
        String normalizedName = QueryNormalizer.normalize(professorName);
        if (normalizedName.isEmpty()) {
            return false;
        }
        
        return professorRepository.existsByProfessorNameIgnoreCaseAndDepartment_DepartmentId(
                normalizedName, departmentId);
    }
    
    // ===== 쓰기 작업용 메서드들 =====
    
    /**
     * 새 교수 생성
     * 
     * @param professorName 교수명
     * @param departmentId 학과 ID
     * @return 생성된 교수 정보
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 학과를 찾을 수 없는 경우
     * @throws DuplicateResourceException 이미 존재하는 교수인 경우
     */
    @Transactional(readOnly = false)
    public ProfessorDto createProfessor(String professorName, Long departmentId) {
        String normalizedName = QueryNormalizer.normalize(professorName);
        
        if (normalizedName.isEmpty()) {
            throw new ValidationException("교수명은 필수입니다.");
        }
        
        if (departmentId == null) {
            throw new ValidationException("학과 ID는 필수입니다.");
        }
        
        // 중복 체크
        if (isDuplicateProfessor(normalizedName, departmentId)) {
            log.warn("교수 생성 실패 - 중복: {} (학과 ID: {})", professorName, departmentId);
            throw DuplicateResourceException.professor(professorName);
        }
        
        // 학과 조회
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("학과를 찾을 수 없습니다: " + departmentId));
        
        Professor professor = Professor.builder()
                .professorName(normalizedName)
                .department(department)
                .build();
        
        Professor saved = professorRepository.save(professor);
        
        log.info("교수 생성 완료: {} (학과: {})", saved.getProfessorName(), department.getDepartmentName());
        
        return ProfessorDto.from(saved);
    }
    
    /**
     * 새 교수 생성 (사용자의 학교 내에서만)
     * 
     * @param professorName 교수명
     * @param departmentId 학과 ID
     * @param userId 사용자 ID (학교 확인용)
     * @return 생성된 교수 정보
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 학과를 찾을 수 없는 경우
     * @throws DuplicateResourceException 이미 존재하는 교수인 경우
     * @throws ValidationException 사용자의 학교와 학과의 학교가 다른 경우
     */
    @Transactional(readOnly = false)
    public ProfessorDto createProfessorWithSchoolValidation(String professorName, Long departmentId, Long userId) {
        if (userId == null) {
            throw new ValidationException("사용자 ID는 필수입니다.");
        }
        
        // 사용자의 학교 ID 확인 - userService.getSchoolIdByUserId는 Long을 반환
        Long userSchoolId = userService.getSchoolIdByUserId(userId);
        
        // 학과 조회 및 학교 검증
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("학과를 찾을 수 없습니다: " + departmentId));
        
        if (!department.getSchool().getSchoolId().equals(userSchoolId)) {
            throw new ValidationException("선택한 학과가 사용자의 학교에 속하지 않습니다.");
        }
        
        // 기존 createProfessor 메서드 호출
        return createProfessor(professorName, departmentId);
    }
    
    /**
     * 교수 찾기 또는 생성 (없으면 자동 생성)
     * 개선: 단일 조회 메서드 사용으로 오버헤드 제거
     * 
     * @param professorName 교수명
     * @param departmentId 학과 ID
     * @return 교수 정보 (기존 또는 새로 생성된)
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 학과를 찾을 수 없는 경우
     */
    @Transactional(readOnly = false)
    public ProfessorDto findOrCreateProfessor(String professorName, Long departmentId) {
        String normalizedName = QueryNormalizer.normalize(professorName);
        
        if (normalizedName.isEmpty()) {
            throw new ValidationException("교수명은 필수입니다.");
        }
        
        if (departmentId == null) {
            throw new ValidationException("학과 ID는 필수입니다.");
        }
        
        // 단일 조회로 오버헤드 제거 (searchByName 페이징 로직 사용 안 함)
        return professorRepository.findProfessorByNameAndDepartment(normalizedName, departmentId)
                .orElseGet(() -> {
                    log.debug("교수가 존재하지 않아 새로 생성: {} (학과 ID: {})", normalizedName, departmentId);
                    return createProfessor(normalizedName, departmentId);
                });
    }
    
    /**
     * 교수 찾기 또는 생성 (사용자의 학교 내에서만)
     * 
     * @param professorName 교수명
     * @param departmentId 학과 ID
     * @param userId 사용자 ID (학교 확인용)
     * @return 교수 정보 (기존 또는 새로 생성된)
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 학과를 찾을 수 없는 경우
     * @throws ValidationException 사용자의 학교와 학과의 학교가 다른 경우
     */
    @Transactional(readOnly = false)
    public ProfessorDto findOrCreateProfessorWithSchoolValidation(String professorName, Long departmentId, Long userId) {
        String normalizedName = QueryNormalizer.normalize(professorName);
        
        if (normalizedName.isEmpty()) {
            throw new ValidationException("교수명은 필수입니다.");
        }
        
        if (departmentId == null) {
            throw new ValidationException("학과 ID는 필수입니다.");
        }
        
        if (userId == null) {
            throw new ValidationException("사용자 ID는 필수입니다.");
        }
        
        // 사용자의 학교 ID 확인
        Long userSchoolId = userService.getSchoolIdByUserId(userId);
        
        // 학과 조회 및 학교 검증
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("학과를 찾을 수 없습니다: " + departmentId));
        
        if (!department.getSchool().getSchoolId().equals(userSchoolId)) {
            throw new ValidationException("선택한 학과가 사용자의 학교에 속하지 않습니다.");
        }
        
        // 기존 findOrCreateProfessor 메서드 호출
        return findOrCreateProfessor(normalizedName, departmentId);
    }
    
    /**
     * 특정 학교의 모든 학과에서 교수 검색 (교양학부 포함)
     * 
     * @param professorName 교수명
     * @param schoolId 학교 ID
     * @return 교수 목록
     */
    public List<ProfessorDto> findProfessorsByNameAndSchool(String professorName, Long schoolId) {
        String normalizedName = QueryNormalizer.normalize(professorName);
        
        if (normalizedName.isEmpty() || schoolId == null) {
            return List.of();
        }
        
        return professorRepository.findProfessorsByNameAndSchool(normalizedName, schoolId);
    }
}