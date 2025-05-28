package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_created_at", columnList = "created_at"),
    @Index(name = "idx_post_status", columnList = "status"),
    @Index(name = "idx_post_user", columnList = "user_id"),
    @Index(name = "idx_post_book", columnList = "book_id"),
    @Index(name = "idx_post_status_created", columnList = "status, created_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @NotNull(message = "사용자는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotNull(message = "상품 유형은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    @ToString.Exclude
    private Book book;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다")
    @Column(nullable = false, length = 255)
    private String title;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Column(nullable = false)
    private Integer price;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "거래 상태는 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'AVAILABLE'")
    @Builder.Default
    private PostStatus status = PostStatus.AVAILABLE;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionMethod transactionMethod;
    
    @Column(length = 100)
    private String campusLocation;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer wishlistCount = 0;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageOrder ASC")  // 이미지 순서대로 정렬
    @BatchSize(size = 10)  // N+1 문제 해결: IN 쿼리로 10개씩 묶어서 조회
    @Builder.Default
    @ToString.Exclude
    private List<PostImage> postImages = new ArrayList<>();

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private PostDescription postDescription;

    // Enum 정의
    public enum ProductType {
        TEXTBOOK, CERTBOOK, NOTE, PASTEXAM, ETC;
        
        // 교재 타입인지 확인하는 헬퍼 메서드
        public boolean isTextbookType() {
            return this == TEXTBOOK || this == CERTBOOK;
        }
    }

    public enum PostStatus {
        AVAILABLE, RESERVED, COMPLETED
    }
    
    public enum TransactionMethod {
        DIRECT, PARCEL, BOTH
    }
}