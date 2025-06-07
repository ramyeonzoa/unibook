package com.unibook.controller.api;

import com.unibook.controller.dto.PagedResponse;
import com.unibook.controller.dto.SubjectSelectionRequest;
import com.unibook.controller.dto.SubjectWithProfessorRequest;
import com.unibook.domain.dto.SubjectDto;
import com.unibook.domain.entity.Subject;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;

/**
 * 과목 검색 및 선택 API 컨트롤러
 * RESTful 설계 - /api/subjects
 * 전역 예외 처리로 try-catch 제거, 커스텀 예외 사용
 */
@Tag(name = "Subject API", description = "과목 검색, 조회 및 선택 API")
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SubjectApiController {
    
    private final SubjectService subjectService;
    private final com.unibook.service.UserService userService;
    private final com.unibook.service.ProfessorService professorService;
    private final com.unibook.repository.DepartmentRepository departmentRepository;
    
    /**
     * 과목명으로 검색
     */
    @Operation(summary = "과목명으로 검색", description = "과목명을 입력받아 관련 과목들을 검색합니다. 같은 교수의 과목이 우선 정렬됩니다.")
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<SubjectDto>> searchSubjects(
            @Parameter(description = "검색어 (과목명)", required = true, example = "데이터구조")
            @RequestParam @NotBlank @Size(min = 1, max = 100, message = "검색어는 1자 이상 100자 이하여야 합니다") String query,
            
            @Parameter(description = "우선순위를 적용할 교수 ID (선택사항)", example = "1")
            @RequestParam(required = false) Long professorId,
            
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            
            @Parameter(description = "페이지 크기 (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        int zeroBasedPage = page - 1;
        
        Page<SubjectDto> subjects = subjectService.findSubjectsByName(
                query, professorId, zeroBasedPage, size);
        
        PagedResponse<SubjectDto> response = PagedResponse.of(subjects, page);
        
        log.debug("과목 검색 완료: query='{}', professorId={}, 결과 수={}", 
                 query, professorId, subjects.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 과목 단건 조회 - ResourceNotFoundException으로 통일
     */
    @Operation(summary = "과목 정보 조회", description = "과목 ID로 특정 과목의 상세 정보를 조회합니다.")
    @GetMapping("/{subjectId}")
    public ResponseEntity<SubjectDto> getSubject(
            @Parameter(description = "과목 ID", required = true, example = "1")
            @PathVariable Long subjectId) {
        
        // 존재하지 않으면 ResourceNotFoundException 자동 발생 (404)
        SubjectDto subject = subjectService.findSubjectById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("과목을 찾을 수 없습니다: " + subjectId));
        
        log.debug("과목 조회 완료: subjectId={}", subjectId);
        return ResponseEntity.ok(subject);
    }
    
    /**
     * 과목 선택 (게시글 작성 시 사용)
     * JSR-303 자동 검증, 커스텀 예외 사용
     */
    @Operation(summary = "과목 선택", description = "게시글 작성 시 과목을 선택합니다. 기존 과목 선택 또는 새 과목 생성이 가능합니다.")
    @PostMapping("/select")
    public ResponseEntity<SubjectDto> selectSubject(
            @Parameter(description = "과목 선택 요청 정보", required = true)
            @Valid @RequestBody SubjectSelectionRequest request) {
        
        // @Valid + @AssertTrue로 교차 필드 검증 자동 처리
        // 수동 if (!request.isValid()) 체크 제거
        
        SubjectDto selectedSubject;
        
        if (request.getSubjectId() != null) {
            // 기존 과목 선택 - ResourceNotFoundException으로 통일
            selectedSubject = subjectService.findSubjectById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("선택한 과목을 찾을 수 없습니다: " + request.getSubjectId()));
                    
            log.debug("기존 과목 선택: subjectId={}", request.getSubjectId());
            
        } else {
            // 새 과목 생성 (findOrCreate 패턴)
            Subject.SubjectType type = request.getSubjectType() != null ? 
                                     request.getSubjectType() : Subject.SubjectType.MAJOR;
                                     
            selectedSubject = subjectService.findOrCreateSubject(
                    request.getSubjectName(), request.getProfessorId(), type);
            
            // 신규 생성은 info 레벨 로깅 (운영 추적용)
            log.info("과목 선택 완료 (생성): 과목명='{}', 교수ID={}, 타입={}", 
                    selectedSubject.getSubjectName(), request.getProfessorId(), type);
        }
        
        // RESTful 응답: 201 Created + Location 헤더 (신규 생성 시)
        if (request.getSubjectId() == null) {
            // 새로 생성된 경우 - 201 Created
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/../{id}")  // /api/subjects/select -> /api/subjects/{id}
                    .buildAndExpand(selectedSubject.getSubjectId())
                    .toUri();
            
            return ResponseEntity.created(location).body(selectedSubject);
        } else {
            // 기존 선택의 경우 - 200 OK
            return ResponseEntity.ok(selectedSubject);
        }
    }
    
    /**
     * 학교 내 과목명으로 검색 (Phase 1 추가: 학교 경계 적용)
     * 사용자의 소속 학교 내에서만 검색
     */
    @Operation(summary = "학교 내 과목 검색", description = "사용자의 소속 학교 내에서만 과목을 검색합니다.")
    @GetMapping("/search/my-school")
    public ResponseEntity<PagedResponse<SubjectDto>> searchSubjectsInMySchool(
            @Parameter(description = "검색어 (과목명)", required = true, example = "데이터구조")
            @RequestParam @NotBlank @Size(min = 1, max = 100, message = "검색어는 1자 이상 100자 이하여야 합니다") String query,
            
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            
            @Parameter(description = "페이지 크기 (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            
            @Parameter(hidden = true) // Spring Security에서 주입
            org.springframework.security.core.Authentication authentication) {
        
        // 현재 로그인 사용자의 ID 가져오기
        com.unibook.security.UserPrincipal userPrincipal = 
                (com.unibook.security.UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        
        // 사용자의 소속 학교 ID 조회
        Long schoolId = userService.getSchoolIdByUserId(userId);
        
        // 프론트엔드는 1부터 시작하지만 Spring Data는 0부터 시작
        int zeroBasedPage = page - 1;
        
        Page<SubjectDto> subjects = subjectService.findSubjectsBySchool(
                query, schoolId, zeroBasedPage, size);
        
        PagedResponse<SubjectDto> response = PagedResponse.of(subjects, page);
        
        log.debug("학교 내 과목 검색 완료: userId={}, schoolId={}, query='{}', 결과 수={}", 
                 userId, schoolId, query, subjects.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 통합 과목 생성 (Phase 1 추가: 과목명 우선 UX)
     * 과목명과 교수명을 함께 받아 한 번에 처리
     */
    @Operation(summary = "과목 및 교수 통합 생성", description = "과목명과 교수명을 입력받아 새 과목을 생성하거나 기존 과목을 반환합니다.")
    @PostMapping("/create-with-professor")
    public ResponseEntity<SubjectDto> createSubjectWithProfessor(
            @Valid @RequestBody SubjectWithProfessorRequest request,
            @Parameter(hidden = true) org.springframework.security.core.Authentication authentication) {
        
        // 현재 로그인 사용자의 정보 가져오기
        com.unibook.security.UserPrincipal userPrincipal = 
                (com.unibook.security.UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        
        // 학과 ID 처리 로직
        Long departmentId = request.getDepartmentId();
        
        // 전공과목인데 departmentId가 없으면 사용자의 소속 학과 사용
        if (departmentId == null && request.getSubjectType() != Subject.SubjectType.GENERAL) {
            com.unibook.domain.entity.User user = userService.findByIdWithDepartmentAndSchool(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
            departmentId = user.getDepartment().getDepartmentId();
        }
        
        // 교양과목이고 departmentId가 없으면 해당 학교의 교양학부 찾기
        if (departmentId == null && request.getSubjectType() == Subject.SubjectType.GENERAL) {
            Long schoolId = userService.getSchoolIdByUserId(userId);
            // 교양학부 조회 로직 필요 - DepartmentRepository에 메서드 추가 필요
            departmentId = departmentRepository.findBySchool_SchoolIdAndDepartmentName(
                    schoolId, com.unibook.common.AppConstants.GENERAL_EDUCATION_DEPT_NAME)
                    .orElseThrow(() -> new ResourceNotFoundException("교양학부를 찾을 수 없습니다"))
                    .getDepartmentId();
        }
        
        // 과목 및 교수 통합 생성
        SubjectDto result = subjectService.findOrCreateSubjectWithProfessor(
                request.getSubjectName(),
                request.getProfessorName(),
                departmentId,
                request.getSubjectType()
        );
        
        log.info("통합 과목 생성 완료: userId={}, 과목={}, 교수={}", 
                userId, result.getSubjectName(), result.getProfessorName());
        
        // 새로 생성된 경우 201 Created, 기존에 있던 경우 200 OK
        // 이 부분은 서비스에서 isNew 플래그를 반환하도록 개선 필요
        return ResponseEntity.ok(result);
    }
}