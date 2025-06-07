package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.BookDto;
import com.unibook.domain.dto.BookSearchDto;
import com.unibook.domain.entity.Book;
import com.unibook.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findByOrderByCreatedAtDesc(pageable);
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
    
    /**
     * 네이버 API에서 받은 책 정보로 Book 엔티티 생성 또는 조회
     * 동시성 이슈 방지를 위해 트랜잭션으로 처리
     */
    @Transactional
    public Book findOrCreateBook(BookSearchDto.Item bookItem) {
        String isbn = bookItem.getIsbn();
        
        // ISBN으로 기존 책 조회
        Optional<Book> existingBook = bookRepository.findByIsbn(isbn);
        if (existingBook.isPresent()) {
            Book book = existingBook.get();
            log.debug("기존 책 발견: ISBN={}, 제목={}", isbn, book.getTitle());
            
            // 기존 책에 이미지 URL이 없다면 업데이트
            if (book.getImageUrl() == null && bookItem.getImage() != null && !bookItem.getImage().trim().isEmpty()) {
                book.setImageUrl(bookItem.getImage());
                book = bookRepository.save(book);
                log.debug("기존 책에 이미지 URL 업데이트: ISBN={}", isbn);
            }
            
            return book;
        }
        
        // 새 책 생성
        Book newBook = Book.builder()
                .isbn(isbn)
                .title(bookItem.getCleanTitle())
                .author(bookItem.getCleanAuthor())
                .publisher(bookItem.getCleanPublisher())
                .publicationYear(bookItem.getPublicationYear())  // nullable
                .originalPrice(bookItem.getPrice())  // nullable
                .imageUrl(bookItem.getImage())  // 네이버 API 이미지 URL
                .build();
        
        Book savedBook = bookRepository.save(newBook);
        log.info("새 책 생성: ISBN={}, 제목={}", isbn, savedBook.getTitle());
        
        return savedBook;
    }
    
    /**
     * 책 ID로 제목 조회 (페이지 제목 표시용)
     * 
     * @param bookId 책 ID
     * @return 책 제목 또는 null
     */
    public String getBookTitleById(Long bookId) {
        if (bookId == null) {
            return null;
        }
        
        return bookRepository.findById(bookId)
                .map(Book::getTitle)
                .orElse(null);
    }
}