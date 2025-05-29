package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "subject_books")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@BatchSize(size = 10)
public class SubjectBook extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subjectBookId;

    @NotNull(message = "과목은 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @ToString.Exclude
    private Subject subject;

    @NotNull(message = "책은 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @ToString.Exclude
    private Book book;

    @Column(name = "active_post_count", nullable = false)
    @Builder.Default
    private Integer activePostCount = 0;
    
    // Reference count 관리 메서드
    public void incrementActivePostCount() {
        this.activePostCount++;
    }
    
    public void decrementActivePostCount() {
        if (this.activePostCount > 0) {
            this.activePostCount--;
        }
    }
    
    public boolean hasActivePosts() {
        return this.activePostCount > 0;
    }
}