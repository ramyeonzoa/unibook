package com.unibook.controller.api;

import com.unibook.controller.dto.PagedResponse;
import com.unibook.domain.dto.ProfessorDto;
import com.unibook.domain.dto.SubjectDto;
import com.unibook.service.ProfessorService;
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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;

/**
 * 교수 검색 API 컨트롤러
 * RESTful 설계 - /api/professors
 * 전역 예외 처리로 try-catch 제거
 */
@Tag(name = "Professor API", description = "교수 검색 및 조회 API")
@RestController
@RequestMapping("/api/professors")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProfessorApiController {
    
    private final ProfessorService professorService;
    private final SubjectService subjectService;
    private final com.unibook.service.UserService userService;
    
    /**
     * 교수명으로 검색
     */
    @Operation(summary = "교수명으로 검색", description = "교수명을 입력받아 관련 교수들을 검색합니다. 같은 학과 교수가 우선 정렬됩니다.")
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProfessorDto>> searchProfessors(
            @Parameter(description = "검색어 (교수명)", required = true, example = "김영희")
            @RequestParam @NotBlank @Size(min = 1, max = 50, message = "검색어는 1자 이상 50자 이하여야 합니다") String query,
            
            @Parameter(description = "우선순위를 적용할 학과 ID (선택사항)", example = "1")
            @RequestParam(required = false) Long departmentId,
            
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            
            @Parameter(description = "페이지 크기 (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        // 프론트엔드는 1부터 시작하지만 Spring Data는 0부터 시작
        int zeroBasedPage = page - 1;
        
        Page<ProfessorDto> professors = professorService.findProfessorsByName(
                query, departmentId, zeroBasedPage, size);
        
        PagedResponse<ProfessorDto> response = PagedResponse.of(professors, page);
        
        log.debug("교수 검색 완료: query='{}', departmentId={}, 결과 수={}", 
                 query, departmentId, professors.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 교수 단건 조회
     */
    @Operation(summary = "교수 정보 조회", description = "교수 ID로 특정 교수의 상세 정보를 조회합니다.")
    @GetMapping("/{professorId}")
    public ResponseEntity<ProfessorDto> getProfessor(
            @Parameter(description = "교수 ID", required = true, example = "1")
            @PathVariable Long professorId) {
        
        Optional<ProfessorDto> professor = professorService.findProfessorById(professorId);
        
        if (professor.isPresent()) {
            log.debug("교수 조회 완료: professorId={}", professorId);
            return ResponseEntity.ok(professor.get());
        } else {
            log.debug("교수를 찾을 수 없음: professorId={}", professorId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 특정 학과의 교수 목록 조회
     */
    @Operation(summary = "학과별 교수 목록 조회", description = "특정 학과에 소속된 모든 교수들을 조회합니다.")
    @GetMapping("/by-department/{departmentId}")
    public ResponseEntity<PagedResponse<ProfessorDto>> getProfessorsByDepartment(
            @Parameter(description = "학과 ID", required = true, example = "1")
            @PathVariable Long departmentId,
            
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            
            @Parameter(description = "페이지 크기 (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        int zeroBasedPage = page - 1;
        
        Page<ProfessorDto> professors = professorService.findProfessorsByDepartment(
                departmentId, zeroBasedPage, size);
        
        PagedResponse<ProfessorDto> response = PagedResponse.of(professors, page);
        
        log.debug("학과별 교수 조회 완료: departmentId={}, 결과 수={}", 
                 departmentId, professors.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 교수가 담당하는 과목 목록 조회 (서브리소스)
     */
    @Operation(summary = "교수별 담당 과목 조회", description = "특정 교수가 담당하는 모든 과목들을 조회합니다.")
    @GetMapping("/{professorId}/subjects")
    public ResponseEntity<PagedResponse<SubjectDto>> getSubjectsByProfessor(
            @Parameter(description = "교수 ID", required = true, example = "1")
            @PathVariable Long professorId,
            
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            
            @Parameter(description = "페이지 크기 (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        int zeroBasedPage = page - 1;
        
        Page<SubjectDto> subjects = subjectService.findSubjectsByProfessor(
                professorId, zeroBasedPage, size);
        
        PagedResponse<SubjectDto> response = PagedResponse.of(subjects, page);
        
        log.debug("교수별 과목 조회 완료: professorId={}, 결과 수={}", 
                 professorId, subjects.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 학교 내 교수명으로 검색 (Phase 1 추가: 학교 경계 적용)
     * 사용자의 소속 학교 내에서만 검색
     */
    @Operation(summary = "학교 내 교수 검색", description = "사용자의 소속 학교 내에서만 교수를 검색합니다.")
    @GetMapping("/search/my-school")
    public ResponseEntity<PagedResponse<ProfessorDto>> searchProfessorsInMySchool(
            @Parameter(description = "검색어 (교수명)", required = true, example = "김영희")
            @RequestParam @NotBlank @Size(min = 1, max = 50, message = "검색어는 1자 이상 50자 이하여야 합니다") String query,
            
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
        
        Page<ProfessorDto> professors = professorService.findProfessorsBySchool(
                query, schoolId, zeroBasedPage, size);
        
        PagedResponse<ProfessorDto> response = PagedResponse.of(professors, page);
        
        log.debug("학교 내 교수 검색 완료: userId={}, schoolId={}, query='{}', 결과 수={}", 
                 userId, schoolId, query, professors.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }
}