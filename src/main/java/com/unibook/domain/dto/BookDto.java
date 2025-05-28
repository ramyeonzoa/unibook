package com.unibook.domain.dto;

import com.unibook.domain.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {
    private Long bookId;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private Integer publicationYear;
    private Integer originalPrice;
    private String imageUrl; // 책 표지 이미지 URL
    
    public static BookDto from(Book book) {
        return BookDto.builder()
                .bookId(book.getBookId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publicationYear(book.getPublicationYear())
                .originalPrice(book.getOriginalPrice())
                .imageUrl(book.getImageUrl())
                .build();
    }
}