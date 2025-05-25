package com.unibook.repository;

import com.unibook.domain.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByTitleContainingOrAuthorContaining(String title, String author);
    boolean existsByIsbn(String isbn);
}