package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "subject_books")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
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
}