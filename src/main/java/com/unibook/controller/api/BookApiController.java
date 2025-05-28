package com.unibook.controller.api;

import com.unibook.domain.dto.BookSearchDto;
import com.unibook.domain.entity.Book;
import com.unibook.service.BookSearchService;
import com.unibook.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookApiController {
    
    private final BookSearchService bookSearchService;
    private final BookService bookService;
    
    @GetMapping("/search")
    public ResponseEntity<BookSearchDto.Response> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("책 검색 요청: query={}, page={}, size={}", query, page, size);
        
        BookSearchDto.Response response = bookSearchService.searchBooks(query, page, size);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/select")
    public ResponseEntity<Map<String, Long>> selectBook(@RequestBody BookSearchDto.Item bookItem) {
        log.debug("책 선택 요청: ISBN={}, 제목={}", bookItem.getIsbn(), bookItem.getCleanTitle());
        
        // ISBN 체크
        if (bookItem.getIsbn() == null || bookItem.getIsbn().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", -1L));
        }
        
        try {
            Book book = bookService.findOrCreateBook(bookItem);
            return ResponseEntity.ok(Collections.singletonMap("bookId", book.getBookId()));
        } catch (Exception e) {
            log.error("책 선택 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", -1L));
        }
    }
}