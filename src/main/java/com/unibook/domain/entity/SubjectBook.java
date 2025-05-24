package com.unibook.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subject_books")
public class SubjectBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subjectBookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Boolean isRequired = true;

    public Long getSubjectBookId() {
        return subjectBookId;
    }

    public void setSubjectBookId(Long subjectBookId) {
        this.subjectBookId = subjectBookId;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
}