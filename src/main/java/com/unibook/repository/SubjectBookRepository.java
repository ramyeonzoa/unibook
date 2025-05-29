package com.unibook.repository;

import com.unibook.domain.entity.SubjectBook;
import com.unibook.domain.dto.SubjectBookDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectBookRepository extends JpaRepository<SubjectBook, Long> {
    
    // 중복 체크 - Subject와 Book 조합
    boolean existsBySubject_SubjectIdAndBook_BookId(Long subjectId, Long bookId);
    
    // Subject와 Book으로 조회 (엔티티)
    Optional<SubjectBook> findBySubject_SubjectIdAndBook_BookId(Long subjectId, Long bookId);
    
    // Subject와 Book으로 조회 (DTO 프로젝션)
    @Query("SELECT new com.unibook.domain.dto.SubjectBookDto(" +
           "sb.subjectBookId, " +
           "sb.subject.subjectId, sb.subject.subjectName, sb.subject.type, " +
           "sb.subject.professor.professorId, sb.subject.professor.professorName, " +
           "sb.subject.professor.department.departmentId, sb.subject.professor.department.departmentName, " +
           "sb.book.bookId, sb.book.title) " +
           "FROM SubjectBook sb " +
           "WHERE sb.subject.subjectId = :subjectId AND sb.book.bookId = :bookId")
    Optional<SubjectBookDto> findDtoBySubject_SubjectIdAndBook_BookId(@Param("subjectId") Long subjectId, 
                                                                     @Param("bookId") Long bookId);
    
    // ID로 DTO 조회
    @Query("SELECT new com.unibook.domain.dto.SubjectBookDto(" +
           "sb.subjectBookId, " +
           "sb.subject.subjectId, sb.subject.subjectName, sb.subject.type, " +
           "sb.subject.professor.professorId, sb.subject.professor.professorName, " +
           "sb.subject.professor.department.departmentId, sb.subject.professor.department.departmentName, " +
           "sb.book.bookId, sb.book.title) " +
           "FROM SubjectBook sb " +
           "WHERE sb.subjectBookId = :subjectBookId")
    Optional<SubjectBookDto> findSubjectBookDtoById(@Param("subjectBookId") Long subjectBookId);
    
    // 책 사용 과목 수 카운트
    long countByBook_BookId(Long bookId);
    
    // 과목별 모든 연결 조회 (삭제용)
    List<SubjectBook> findAllBySubject_SubjectId(Long subjectId);
    
    // 성능 최적화: 배치 삭제
    @Modifying
    @Query("DELETE FROM SubjectBook sb WHERE sb.subject.subjectId = :subjectId")
    int deleteBySubject_SubjectId(@Param("subjectId") Long subjectId);
    
    // 특정 책을 사용하는 과목들 조회 (이 책을 사용하는 과목)
    @Query(value = "SELECT new com.unibook.domain.dto.SubjectBookDto(" +
                   "sb.subjectBookId, " +
                   "sb.subject.subjectId, sb.subject.subjectName, sb.subject.type, " +
                   "sb.subject.professor.professorId, sb.subject.professor.professorName, " +
                   "sb.subject.professor.department.departmentId, sb.subject.professor.department.departmentName, " +
                   "sb.book.bookId, sb.book.title) " +
                   "FROM SubjectBook sb " +
                   "WHERE sb.book.bookId = :bookId " +
                   "ORDER BY sb.subject.professor.department.departmentName, sb.subject.subjectName",
           countQuery = "SELECT COUNT(sb) FROM SubjectBook sb WHERE sb.book.bookId = :bookId")
    Page<SubjectBookDto> findSubjectsByBook(@Param("bookId") Long bookId, Pageable pageable);
    
    // 특정 과목의 교재들 조회 (이 과목에서 사용하는 교재)
    @Query(value = "SELECT new com.unibook.domain.dto.SubjectBookDto(" +
                   "sb.subjectBookId, " +
                   "sb.subject.subjectId, sb.subject.subjectName, sb.subject.type, " +
                   "sb.subject.professor.professorId, sb.subject.professor.professorName, " +
                   "sb.subject.professor.department.departmentId, sb.subject.professor.department.departmentName, " +
                   "sb.book.bookId, sb.book.title) " +
                   "FROM SubjectBook sb " +
                   "WHERE sb.subject.subjectId = :subjectId " +
                   "ORDER BY sb.book.title",
           countQuery = "SELECT COUNT(sb) FROM SubjectBook sb WHERE sb.subject.subjectId = :subjectId")
    Page<SubjectBookDto> findBooksBySubject(@Param("subjectId") Long subjectId, Pageable pageable);
    
    // 학과별 교재-과목 연결 조회
    @Query(value = "SELECT new com.unibook.domain.dto.SubjectBookDto(" +
                   "sb.subjectBookId, " +
                   "sb.subject.subjectId, sb.subject.subjectName, sb.subject.type, " +
                   "sb.subject.professor.professorId, sb.subject.professor.professorName, " +
                   "sb.subject.professor.department.departmentId, sb.subject.professor.department.departmentName, " +
                   "sb.book.bookId, sb.book.title) " +
                   "FROM SubjectBook sb " +
                   "WHERE sb.subject.professor.department.departmentId = :departmentId " +
                   "ORDER BY sb.subject.subjectName, sb.book.title",
           countQuery = "SELECT COUNT(sb) FROM SubjectBook sb " +
                       "WHERE sb.subject.professor.department.departmentId = :departmentId")
    Page<SubjectBookDto> findSubjectBooksByDepartment(@Param("departmentId") Long departmentId, Pageable pageable);
}