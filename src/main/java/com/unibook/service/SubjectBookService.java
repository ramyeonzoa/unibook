package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.SubjectBookDto;
import com.unibook.domain.entity.Book;
import com.unibook.domain.entity.Subject;
import com.unibook.domain.entity.SubjectBook;
import com.unibook.exception.ErrorCode;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.BookRepository;
import com.unibook.repository.SubjectBookRepository;
import com.unibook.repository.SubjectRepository;
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
 * 과목-책 연결 관계 관리 서비스
 * - 과목별 사용 교재 관리
 * - 동시성 방어 및 통일된 예외 처리
 * - Repository 메서드명과 일치하는 호출 패턴
 * - year, semester는 Subject에서 관리
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectBookService {
    
    private final SubjectBookRepository subjectBookRepository;
    private final SubjectRepository subjectRepository;
    private final BookRepository bookRepository;
    
    // ===== 조회 메서드들 (메서드 오버로드 지원) =====
    
    /**
     * 특정 과목에서 사용하는 책 목록 조회 (기본 페이징)
     */
    public Page<SubjectBookDto> findBooksBySubject(Long subjectId) {
        return findBooksBySubject(subjectId, 0, AppConstants.DEFAULT_PAGE_SIZE);
    }
    
    /**
     * 특정 과목에서 사용하는 책 목록 조회
     * Repository 메서드명과 일치: findBooksBySubject
     * 
     * @param subjectId 과목 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 과목-책 연결 목록
     */
    public Page<SubjectBookDto> findBooksBySubject(Long subjectId, int page, int size) {
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("과목별 책 조회: subjectId={}, page={}, size={}", subjectId, page, size);
        
        return subjectBookRepository.findBooksBySubject(subjectId, pageable);
    }
    
    /**
     * 특정 책을 사용하는 과목 목록 조회 (기본 페이징)
     */
    public Page<SubjectBookDto> findSubjectsByBook(Long bookId) {
        return findSubjectsByBook(bookId, 0, AppConstants.DEFAULT_PAGE_SIZE);
    }
    
    /**
     * 특정 책을 사용하는 과목 목록 조회
     * Repository 메서드명과 일치: findSubjectsByBook
     * 
     * @param bookId 책 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 과목-책 연결 목록
     */
    public Page<SubjectBookDto> findSubjectsByBook(Long bookId, int page, int size) {
        // 페이징 파라미터 방어
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), AppConstants.MAX_PAGE_SIZE);
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.debug("책별 과목 조회: bookId={}, page={}, size={}", bookId, page, size);
        
        return subjectBookRepository.findSubjectsByBook(bookId, pageable);
    }
    
    /**
     * 과목-책 연결 정보 조회 (DTO 프로젝션)
     * Repository 메서드명과 일치: findSubjectBookDtoById
     * 
     * @param subjectBookId 과목-책 연결 ID
     * @return 연결 정보 (Optional)
     */
    public Optional<SubjectBookDto> findSubjectBookById(Long subjectBookId) {
        if (subjectBookId == null) {
            return Optional.empty();
        }
        return subjectBookRepository.findSubjectBookDtoById(subjectBookId);
    }
    
    /**
     * 과목-책 연결 존재 여부 확인
     * 
     * @param subjectId 과목 ID
     * @param bookId 책 ID
     * @return 연결 존재 여부
     */
    public boolean existsSubjectBookConnection(Long subjectId, Long bookId) {
        if (subjectId == null || bookId == null) {
            return false;
        }
        return subjectBookRepository.existsBySubject_SubjectIdAndBook_BookId(subjectId, bookId);
    }
    
    /**
     * 책 사용 횟수 조회 (인기도 측정용)
     * Repository 메서드명과 일치: countByBook_BookId
     * 
     * @param bookId 책 ID
     * @return 사용 과목 수
     */
    public long countSubjectsByBook(Long bookId) {
        if (bookId == null) {
            return 0;
        }
        return subjectBookRepository.countByBook_BookId(bookId);
    }
    
    // ===== 쓰기 작업용 메서드들 (동시성 방어 포함) =====
    
    /**
     * 과목-책 연결 생성 (동시성 방어 포함)
     * saveAndFlush() 사용으로 즉시 INSERT 시점 확정
     * year, semester는 Subject에서 가져옴
     * 
     * @param subjectId 과목 ID
     * @param bookId 책 ID
     * @return 생성된 연결 정보
     * @throws ValidationException 입력값이 유효하지 않은 경우
     * @throws ResourceNotFoundException 과목이나 책을 찾을 수 없는 경우
     */
    @Transactional(readOnly = false)
    public SubjectBookDto createSubjectBookConnection(Long subjectId, Long bookId) {
        if (subjectId == null) {
            throw new ValidationException("과목 ID는 필수입니다.");
        }
        
        if (bookId == null) {
            throw new ValidationException("책 ID는 필수입니다.");
        }
        
        // 과목과 책 존재 확인 (명확한 에러 코드로)
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.SUBJECT_NOT_FOUND, "과목을 찾을 수 없습니다: " + subjectId));
        
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.BOOK_NOT_FOUND, "책을 찾을 수 없습니다: " + bookId));
        
        SubjectBook subjectBook = SubjectBook.builder()
                .subject(subject)
                .book(book)
                .build();
        
        try {
            // 동시성 방어: saveAndFlush()로 즉시 INSERT 확정
            SubjectBook saved = subjectBookRepository.saveAndFlush(subjectBook);
            
            log.info("과목-책 연결 생성 완료: {} ↔ {}", 
                    subject.getSubjectName(), book.getTitle());
            
            return SubjectBookDto.from(saved);
            
        } catch (DataIntegrityViolationException e) {
            // 이미 연결이 존재하는 경우 - 기존 연결 반환 (DTO 프로젝션 직접 사용)
            log.warn("과목-책 연결 실패 - 이미 존재: subjectId={}, bookId={}", subjectId, bookId);
            
            return subjectBookRepository.findDtoBySubject_SubjectIdAndBook_BookId(subjectId, bookId)
                    .orElseThrow(() -> {
                        log.error("동시성 충돌 후 재조회 실패: subjectId={}, bookId={}", subjectId, bookId);
                        return new ResourceNotFoundException(
                            ErrorCode.RESOURCE_NOT_FOUND, 
                            "과목-책 연결 생성 중 오류가 발생했습니다");
                    });
        }
    }
    
    /**
     * 과목-책 연결 찾기 또는 생성 (Subject에서 연도/학기 참조)
     * 
     * @param subjectId 과목 ID
     * @param bookId 책 ID
     * @return 연결 정보 (기존 또는 새로 생성된)
     */
    @Transactional(readOnly = false)
    public SubjectBookDto findOrCreateSubjectBookConnection(Long subjectId, Long bookId) {
        if (subjectId == null) {
            throw new ValidationException("과목 ID는 필수입니다.");
        }
        
        if (bookId == null) {
            throw new ValidationException("책 ID는 필수입니다.");
        }
        
        // 기존 연결 확인 (DTO 프로젝션 방식)
        Optional<SubjectBookDto> existing = subjectBookRepository.findDtoBySubject_SubjectIdAndBook_BookId(
                subjectId, bookId);
        
        if (existing.isPresent()) {
            log.debug("기존 과목-책 연결 반환: subjectId={}, bookId={}", subjectId, bookId);
            return existing.get();
        }
        
        // 새 연결 생성
        log.debug("새 과목-책 연결 생성 시작: subjectId={}, bookId={}", subjectId, bookId);
        return createSubjectBookConnection(subjectId, bookId);
    }
    
    // ===== Reference Count 관리 메서드들 =====
    
    /**
     * 게시글 생성 시 reference count 증가
     * 
     * @param subjectId 과목 ID
     * @param bookId 책 ID
     * @return 업데이트된 연결 정보
     */
    @Transactional(readOnly = false)
    public SubjectBookDto incrementPostCount(Long subjectId, Long bookId) {
        if (subjectId == null || bookId == null) {
            throw new ValidationException("과목 ID와 책 ID는 필수입니다.");
        }
        
        // 기존 연결이 있으면 count 증가, 없으면 생성
        SubjectBook subjectBook = subjectBookRepository.findBySubject_SubjectIdAndBook_BookId(subjectId, bookId)
                .orElseGet(() -> {
                    // 새 연결 생성
                    Subject subject = subjectRepository.findById(subjectId)
                            .orElseThrow(() -> new ResourceNotFoundException("과목을 찾을 수 없습니다: " + subjectId));
                    Book book = bookRepository.findById(bookId)
                            .orElseThrow(() -> new ResourceNotFoundException("책을 찾을 수 없습니다: " + bookId));
                    
                    SubjectBook newConnection = SubjectBook.builder()
                            .subject(subject)
                            .book(book)
                            .activePostCount(0)
                            .build();
                    
                    try {
                        return subjectBookRepository.saveAndFlush(newConnection);
                    } catch (DataIntegrityViolationException e) {
                        // 동시성 충돌 시 다시 조회
                        return subjectBookRepository.findBySubject_SubjectIdAndBook_BookId(subjectId, bookId)
                                .orElseThrow(() -> new ResourceNotFoundException("과목-책 연결을 찾을 수 없습니다."));
                    }
                });
        
        subjectBook.incrementActivePostCount();
        SubjectBook saved = subjectBookRepository.save(subjectBook);
        
        log.info("SubjectBook 참조 카운트 증가: {} ↔ {} (count: {})", 
                subjectBook.getSubject().getSubjectName(), 
                subjectBook.getBook().getTitle(), 
                saved.getActivePostCount());
        
        return SubjectBookDto.from(saved);
    }
    
    /**
     * 게시글 삭제 시 reference count 감소, 0이 되면 SubjectBook 삭제
     * 
     * @param subjectId 과목 ID
     * @param bookId 책 ID
     */
    @Transactional(readOnly = false)
    public void decrementPostCount(Long subjectId, Long bookId) {
        if (subjectId == null || bookId == null) {
            log.warn("SubjectBook 참조 카운트 감소 실패: subjectId 또는 bookId가 null");
            return;
        }
        
        Optional<SubjectBook> subjectBookOpt = subjectBookRepository.findBySubject_SubjectIdAndBook_BookId(subjectId, bookId);
        
        if (subjectBookOpt.isEmpty()) {
            log.warn("SubjectBook 참조 카운트 감소 실패: 연결을 찾을 수 없음 (subjectId={}, bookId={})", subjectId, bookId);
            return;
        }
        
        SubjectBook subjectBook = subjectBookOpt.get();
        subjectBook.decrementActivePostCount();
        
        if (subjectBook.hasActivePosts()) {
            // 아직 활성 게시글이 있으면 count만 업데이트
            subjectBookRepository.save(subjectBook);
            log.info("SubjectBook 참조 카운트 감소: {} ↔ {} (count: {})", 
                    subjectBook.getSubject().getSubjectName(), 
                    subjectBook.getBook().getTitle(), 
                    subjectBook.getActivePostCount());
        } else {
            // 활성 게시글이 없으면 SubjectBook 삭제
            subjectBookRepository.delete(subjectBook);
            log.info("SubjectBook 삭제 (참조 카운트 0): {} ↔ {}", 
                    subjectBook.getSubject().getSubjectName(), 
                    subjectBook.getBook().getTitle());
        }
    }
    
    /**
     * 과목-책 연결 삭제
     * 
     * @param subjectBookId 과목-책 연결 ID
     * @throws ResourceNotFoundException 연결을 찾을 수 없는 경우
     */
    @Transactional(readOnly = false)
    public void deleteSubjectBookConnection(Long subjectBookId) {
        if (subjectBookId == null) {
            throw new ValidationException("과목-책 연결 ID는 필수입니다.");
        }
        
        SubjectBook subjectBook = subjectBookRepository.findById(subjectBookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.RESOURCE_NOT_FOUND, "과목-책 연결을 찾을 수 없습니다: " + subjectBookId));
        
        subjectBookRepository.delete(subjectBook);
        
        log.info("과목-책 연결 삭제 완료: {} ↔ {}", 
                subjectBook.getSubject().getSubjectName(), 
                subjectBook.getBook().getTitle());
    }
    
    /**
     * 특정 과목의 모든 책 연결 삭제 (성능 최적화 - 배치 삭제)
     * 
     * @param subjectId 과목 ID
     * @return 삭제된 연결 수
     */
    @Transactional(readOnly = false)
    public int deleteAllConnectionsBySubject(Long subjectId) {
        if (subjectId == null) {
            throw new ValidationException("과목 ID는 필수입니다.");
        }
        
        // 성능 최적화: @Modifying 쿼리로 배치 삭제
        int deletedCount = subjectBookRepository.deleteBySubject_SubjectId(subjectId);
        
        if (deletedCount > 0) {
            log.info("과목의 모든 책 연결 배치 삭제 완료: subjectId={}, 삭제 수={}", subjectId, deletedCount);
        }
        
        return deletedCount;
    }
}