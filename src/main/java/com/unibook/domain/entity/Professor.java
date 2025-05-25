package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "professors")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Professor extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long professorId;

    @NotBlank(message = "교수명은 필수입니다")
    @Size(max = 50, message = "교수명은 50자 이하여야 합니다")
    @Column(nullable = false, length = 50)
    private String professorName;

    @NotNull(message = "학과는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @ToString.Exclude
    private Department department;

    @OneToMany(mappedBy = "professor")
    @Builder.Default
    @ToString.Exclude
    private List<Subject> subjects = new ArrayList<>();
}