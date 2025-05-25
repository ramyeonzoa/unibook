package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books", indexes = {
    @Index(name = "idx_book_isbn", columnList = "isbn"),
    @Index(name = "idx_book_title", columnList = "title"),
    @Index(name = "idx_book_author", columnList = "author"),
    @Index(name = "idx_book_created_at", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Book extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @NotBlank(message = "ISBN은 필수입니다")
    @Size(max = 20, message = "ISBN은 20자 이하여야 합니다")
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다")
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "저자는 필수입니다")
    @Size(max = 100, message = "저자는 100자 이하여야 합니다")
    @Column(nullable = false, length = 100)
    private String author;

    @NotBlank(message = "출판사는 필수입니다")
    @Size(max = 255, message = "출판사는 255자 이하여야 합니다")
    @Column(nullable = false, length = 255)
    private String publisher;

    @NotNull(message = "발행년도는 필수입니다")
    @Column(nullable = false)
    private Integer publicationYear;
    
    @Column
    private Integer originalPrice;

    @OneToMany(mappedBy = "book")
    @Builder.Default
    @ToString.Exclude
    private List<SubjectBook> subjectBooks = new ArrayList<>();

    @OneToMany(mappedBy = "book")
    @Builder.Default
    @ToString.Exclude
    private List<Post> posts = new ArrayList<>();
}