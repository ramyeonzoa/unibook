package com.unibook.repository;

import com.unibook.domain.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByTitleContainingOrAuthorContaining(String title, String author);
    boolean existsByIsbn(String isbn);
    
    // 임시: 최신 책 조회 (Day 6에서 viewCount 기반으로 변경 예정)
    List<Book> findByOrderByCreatedAtDesc(Pageable pageable);
}