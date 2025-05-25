package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_dept_school", columnList = "school_id"),
    @Index(name = "idx_dept_school_name", columnList = "school_id, department_name")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Department extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    @NotNull(message = "학교는 필수입니다")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @NotBlank(message = "학과명은 필수입니다")
    @Size(max = 100, message = "학과명은 100자 이하여야 합니다")
    @Column(nullable = false, length = 100)
    private String departmentName;

    @OneToMany(mappedBy = "department")
    @Builder.Default
    @ToString.Exclude
    private List<Professor> professors = new ArrayList<>();

    @OneToMany(mappedBy = "department")
    @Builder.Default
    @ToString.Exclude
    private List<User> users = new ArrayList<>();
}