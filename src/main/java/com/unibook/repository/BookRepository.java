package com.unibook.repository;

import com.unibook.domain.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // ===== 기존 메서드들 (N+1 위험) =====
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByTitleContainingOrAuthorContaining(String title, String author);
    boolean existsByIsbn(String isbn);
    
    // ===== 최적화된 메서드들 (Fetch Join 적용) =====
    
    /**
     * Book과 관련 Posts 함께 조회 (N+1 방지)
     */
    @Query("SELECT DISTINCT b FROM Book b " +
           "LEFT JOIN FETCH b.posts p " +
           "LEFT JOIN FETCH p.user u " +
           "WHERE b.bookId = :bookId")
    Optional<Book> findByIdWithPosts(@Param("bookId") Long bookId);
    
    /**
     * Book과 관련 SubjectBooks 함께 조회 (N+1 방지)  
     */
    @Query("SELECT DISTINCT b FROM Book b " +
           "LEFT JOIN FETCH b.subjectBooks sb " +
           "LEFT JOIN FETCH sb.subject s " +
           "LEFT JOIN FETCH s.professor " +
           "WHERE b.bookId = :bookId")
    Optional<Book> findByIdWithSubjectBooks(@Param("bookId") Long bookId);
    
    /**
     * 최신 책 조회 (Posts 수 기반 인기도 포함)
     */
    @Query("SELECT DISTINCT b FROM Book b " +
           "LEFT JOIN FETCH b.posts p " +
           "ORDER BY b.createdAt DESC")
    List<Book> findLatestBooksWithPosts(Pageable pageable);
    
    /**
     * 인기 책 조회 (Post 수 기반)
     */
    @Query("SELECT b FROM Book b " +
           "LEFT JOIN b.posts p " +
           "GROUP BY b " +
           "ORDER BY COUNT(p) DESC, b.createdAt DESC")
    List<Book> findPopularBooks(Pageable pageable);
    
    // 임시: 최신 책 조회 (Day 6에서 viewCount 기반으로 변경 예정)
    List<Book> findByOrderByCreatedAtDesc(Pageable pageable);
}