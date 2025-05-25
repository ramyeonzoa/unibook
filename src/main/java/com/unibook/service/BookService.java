package com.unibook.service;

import com.unibook.domain.dto.BookDto;
import com.unibook.domain.entity.Book;
import com.unibook.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {
    
    private final BookRepository bookRepository;
    
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    
    public Optional<Book> findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }
    
    @Transactional
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }
    
    public List<Book> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingOrAuthorContaining(keyword, keyword);
    }
    
    public List<Book> getPopularBooks(int limit) {
        // TODO: Day 6에서 viewCount 기반 정렬 구현 예정
        // 현재는 최신 책 반환 (임시)
        return bookRepository.findTop8ByOrderByCreatedAtDesc();
    }
    
    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }
    
    // DTO 반환 메서드
    public List<BookDto> getPopularBookDtos(int limit) {
        return getPopularBooks(limit).stream()
                .map(BookDto::from)
                .collect(Collectors.toList());
    }
}