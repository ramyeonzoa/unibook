package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.ProfessorDto;
import com.unibook.domain.dto.SubjectDto;
import com.unibook.domain.entity.Professor;
import com.unibook.domain.entity.Subject;
import com.unibook.exception.DuplicateResourceException;
import com.unibook.exception.ErrorCode;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.ProfessorRepository;
import com.unibook.repository.SubjectRepository;
import com.unibook.util.QueryNormalizer;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 과목 관련 비즈니스 로직을 처리하는 서비스
 * - 네이밍 컨벤션: find… 통일
 * - 방어 코드: 페이징 파라미터 검증, null 안전성, 동시성 방어
 * - 커스텀 예외: ErrorCode 기반 통일된 예외 처리
 * - 메서드 오버로드: 기본 페이징 파라미터 지원
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {
    
    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final UserService userService;
    private final ProfessorService professorService;
    
    // ===== 검색 메서드들 (메서드 오버로드 지원) =====
    
    /**
     * 과목명으로 검색 (기본 페이징)
     */
    public Page<SubjectDto> findSubjectsByName(String query, Long professorId) {
        return findSubjectsByName(query, professorId, 0, AppConstants.DEFAULT_PAGE_SIZE);
    }
    
    /**
     * 과목명으로 검색 (교수 우선순위 적용)
     * 읽기 전용 트랜잭션은 클래스 레벨에서 상속됨
     * 
     * @param query 검색어 (컨트롤러에서 검증됨)
     * @param professorId 우선순위를 적용할 교수 ID (nullable)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 과목 목록
     */
    public Page<SubjectDto> findSubjectsByName(String query, Long professorId, int page, int size) {
        // normalize()에서 null 방어 처리됨
        String normalizedQuery = QueryNormalizer.normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Page.empty();
        }
        
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("과목 검색: query='{}', normalized='{}', professorId={}, page={}, size={}", 
                 query, normalizedQuery, professorId, page, size);
        
        return subjectRepository.findSubjectsByName(normalizedQuery, professorId, pageable);
    }
    
    /**
     * 특정 교수의 과목 목록 조회 (기본 페이징)
     */
    public Page<SubjectDto> findSubjectsByProfessor(Long professorId) {
        return findSubjectsByProfessor(professorId, 0, AppConstants.DEFAULT_PAGE_SIZE);
    }
    
    /**
     * 특정 교수의 과목 목록 조회
     * 
     * @param professorId 교수 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 과목 목록
     */
    public Page<SubjectDto> findSubjectsByProfessor(Long professorId, int page, int size) {
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("교수별 과목 조회: professorId={}, page={}, size={}", professorId, page, size);
        
        return subjectRepository.findSubjectsByProfessor(professorId, pageable);
    }
    
    /**
     * 과목 존재 여부 확인
     * 
     * @param subjectId 과목 ID
     * @return 존재 여부
     */
    public boolean existsById(Long subjectId) {
        return subjectId != null && subjectRepository.existsById(subjectId);
    }
    
    /**
     * 과목 정보 조회 (DTO 프로젝션)
     * 
     * @param subjectId 과목 ID
     * @return 과목 정보 (Optional)
     */
    public Optional<SubjectDto> findSubjectById(Long subjectId) {
        if (subjectId == null) {
            return Optional.empty();
        }
        return subjectRepository.findSubjectDtoById(subjectId);
    }
    
    /**
     * 과목 중복 체크 (정규화된 이름으로 비교)
     * 
     * @param subjectName 과목명
     * @param professorId 교수 ID
     * @return 중복 여부
     */
    public boolean isDuplicateSubject(String subjectName, Long professorId) {
        if (professorId == null) {
            return false;
        }
        
        // normalize()에서 null 방어 처리됨
        String normalizedName = QueryNormalizer.normalize(subjectName);
        if (normalizedName.isEmpty()) {
            return false;
        }
        
        return subjectRepository.existsBySubjectNameAndProfessor_ProfessorId(
                normalizedName, professorId);
    }
    
    // ===== 쓰기 작업용 메서드들 (동시성 방어 포함) =====
    
    /**
     * 새 과목 생성 (동시성 방어 포함)
     * 
     * @param subjectName 과목명
     * @param professorId 교수 ID
     * @param type 과목 타입 (MAJOR/GENERAL)
     * @return 생성된 과목 정보
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 교수를 찾을 수 없는 경우
     * @throws DuplicateResourceException 이미 존재하는 과목인 경우
     */
    @Transactional(readOnly = false)
    public SubjectDto createSubject(String subjectName, Long professorId, Subject.SubjectType type) {
        String normalizedName = QueryNormalizer.normalize(subjectName);
        
        if (normalizedName.isEmpty()) {
            throw new ValidationException("과목명은 필수입니다.");
        }
        
        if (professorId == null) {
            throw new ValidationException("교수 ID는 필수입니다.");
        }
        
        if (type == null) {
            type = Subject.SubjectType.MAJOR; // 기본값
        }
        
        // 교수 조회 (먼저 확인하여 의미있는 에러 메시지 제공)
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.PROFESSOR_NOT_FOUND, "교수를 찾을 수 없습니다: " + professorId));
        
        Subject subject = Subject.builder()
                .subjectName(normalizedName)
                .professor(professor)
                .type(type)
                .build();
        
        try {
            // 동시성 방어: DB 유니크 제약조건으로 race condition 방어
            Subject saved = subjectRepository.save(subject);
            
            log.info("과목 생성 완료: {} (교수: {}, 타입: {})", 
                    saved.getSubjectName(), professor.getProfessorName(), type.getDisplayName());
            
            return SubjectDto.from(saved);
            
        } catch (DataIntegrityViolationException e) {
            // 다른 트랜잭션에서 이미 생성됨 - 재조회하여 반환
            log.warn("과목 생성 실패 - 동시성 충돌: {} (교수 ID: {})", 
                    subjectName, professorId);
            
            return subjectRepository.findBySubjectNameAndProfessor_ProfessorId(
                    normalizedName, professorId)
                    .map(SubjectDto::from)
                    .orElseThrow(() -> {
                        log.error("동시성 충돌 후 재조회 실패: {} (교수 ID: {})", normalizedName, professorId);
                        return new ResourceNotFoundException(
                            ErrorCode.SUBJECT_NOT_FOUND, 
                            "과목 생성 중 오류가 발생했습니다: " + subjectName);
                    });
        }
    }
    
    /**
     * 과목 찾기 또는 생성 (없으면 자동 생성, 동시성 안전)
     * 
     * @param subjectName 과목명
     * @param professorId 교수 ID
     * @param type 과목 타입 (null이면 MAJOR로 기본값)
     * @return 과목 정보 (기존 또는 새로 생성된)
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 교수를 찾을 수 없는 경우
     */
    @Transactional(readOnly = false)
    public SubjectDto findOrCreateSubject(String subjectName, Long professorId, Subject.SubjectType type) {
        String normalizedName = QueryNormalizer.normalize(subjectName);
        
        if (normalizedName.isEmpty()) {
            throw new ValidationException("과목명은 필수입니다.");
        }
        
        if (professorId == null) {
            throw new ValidationException("교수 ID는 필수입니다.");
        }
        
        if (type == null) {
            type = Subject.SubjectType.MAJOR; // 기본값
        }
        
        // 단일 조회로 오버헤드 제거
        Optional<SubjectDto> existing = subjectRepository.findBySubjectNameAndProfessor_ProfessorId(
                normalizedName, professorId)
                .map(SubjectDto::from);
        
        if (existing.isPresent()) {
            log.debug("기존 과목 반환: {} (교수 ID: {})", 
                    normalizedName, professorId);
            return existing.get();
        }
        
        // 없으면 생성 (동시성 방어 포함)
        log.debug("과목이 존재하지 않아 새로 생성: {} (교수 ID: {}, 타입: {})", 
                 normalizedName, professorId, type.getDisplayName());
        
        return createSubject(normalizedName, professorId, type);
    }
    
    /**
     * 통합 생성: 과목 + 교수 한번에 처리 (과목명 우선 접근)
     * Phase 1 추가: 새로운 UX 플로우를 위한 메서드
     * 
     * @param subjectName 과목명
     * @param professorName 교수명  
     * @param departmentId 학과 ID
     * @param type 과목 타입 (nullable - 기본값: MAJOR)
     * @return 과목 정보 (기존 또는 새로 생성된)
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 학과를 찾을 수 없는 경우
     */
    @Transactional(readOnly = false)
    public SubjectDto findOrCreateSubjectWithProfessor(String subjectName, String professorName, 
                                                      Long departmentId, Subject.SubjectType type) {
        // 입력값 검증
        String normalizedSubjectName = QueryNormalizer.normalize(subjectName);
        String normalizedProfessorName = QueryNormalizer.normalize(professorName);
        
        if (normalizedSubjectName.isEmpty()) {
            throw new ValidationException("과목명은 필수입니다.");
        }
        
        if (normalizedProfessorName.isEmpty()) {
            throw new ValidationException("교수명은 필수입니다.");
        }
        
        if (departmentId == null) {
            throw new ValidationException("학과 ID는 필수입니다.");
        }
        
        if (type == null) {
            type = Subject.SubjectType.MAJOR; // 기본값
        }
        
        log.debug("통합 과목 생성 시작: 과목='{}', 교수='{}', 학과ID={}, 타입={}", 
                 normalizedSubjectName, normalizedProfessorName, departmentId, 
                 type.getDisplayName());
        
        // 1단계: 교수 찾기 또는 생성
        // Note: ProfessorService is injected via constructor
        ProfessorDto professor = professorService.findOrCreateProfessor(normalizedProfessorName, departmentId);
        
        log.debug("교수 처리 완료: {} (ID: {})", professor.getProfessorName(), professor.getProfessorId());
        
        // 2단계: 과목 찾기 또는 생성 (기존 메서드 활용)
        SubjectDto result = findOrCreateSubject(normalizedSubjectName, professor.getProfessorId(), type);
        
        log.info("통합 과목 생성 완료: {} - {} 교수 ({})", 
                result.getSubjectName(), result.getProfessorName(), result.getDepartmentName());
        
        return result;
    }
    
    /**
     * 과목명 우선 검색 (Phase 1 추가)
     * 기존 findSubjectsByName 메서드 활용
     * 
     * @param query 검색어 (과목명)
     * @param departmentId 우선순위를 적용할 학과 ID (사용자 소속 학과)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 과목 목록
     */
    public Page<SubjectDto> findSubjectsWithFilter(String query, Long departmentId, int page, int size) {
        // normalize()에서 null 방어 처리됨
        String normalizedQuery = QueryNormalizer.normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Page.empty();
        }
        
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("과목명 검색: query='{}', normalized='{}', departmentId={}, page={}, size={}", 
                 query, normalizedQuery, departmentId, page, size);
        
        // 사용자 소속 학과를 우선순위로 설정하여 검색
        return subjectRepository.findSubjectsByName(normalizedQuery, departmentId, pageable);
    }
    
    /**
     * 학교별 과목 검색 (Phase 1 추가: 학교 경계 적용)
     * 읽기 전용 트랜잭션은 클래스 레벨에서 상속됨
     * 
     * @param query 검색어 (컨트롤러에서 검증됨)
     * @param schoolId 학교 ID (컨트롤러에서 필수 검증됨)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 과목 목록
     */
    public Page<SubjectDto> findSubjectsBySchool(String query, Long schoolId, int page, int size) {
        // normalize()에서 null 방어 처리됨
        String normalizedQuery = QueryNormalizer.normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Page.empty();
        }
        
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("학교별 과목 검색: query='{}', normalized='{}', schoolId={}, page={}, size={}", 
                 query, normalizedQuery, schoolId, page, size);
        
        return subjectRepository.findSubjectsByNameAndSchool(normalizedQuery, schoolId, pageable);
    }
}