package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subjects")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Subject extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subjectId;

    @NotBlank(message = "과목명은 필수입니다")
    @Size(max = 100, message = "과목명은 100자 이하여야 합니다")
    @Column(nullable = false, length = 100)
    private String subjectName;

    @NotNull(message = "교수는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    @ToString.Exclude
    private Professor professor;

    @OneToMany(mappedBy = "subject")
    @Builder.Default
    @ToString.Exclude
    private List<SubjectBook> subjectBooks = new ArrayList<>();
}