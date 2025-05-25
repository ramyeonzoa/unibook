package com.unibook.service;

import com.unibook.domain.entity.Book;
import com.unibook.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
        // For now, just return recent books. Later we can implement popularity logic
        return bookRepository.findAll().stream()
                .limit(limit)
                .toList();
    }
    
    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }
}