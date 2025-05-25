package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "schools", indexes = {
    @Index(name = "idx_school_name", columnList = "school_name"),
    @Index(name = "idx_school_primary_domain", columnList = "primary_domain")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class School extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long schoolId;

    @NotBlank(message = "학교명은 필수입니다")
    @Size(max = 100, message = "학교명은 100자 이하여야 합니다")
    @Column(nullable = false, length = 100)
    private String schoolName;

    @Size(max = 255, message = "도메인은 255자 이하여야 합니다")
    @Column(length = 255)
    private String primaryDomain;

    @ElementCollection
    @CollectionTable(name = "school_domains", joinColumns = @JoinColumn(name = "school_id"))
    @Column(name = "domain")
    @Builder.Default
    private Set<String> allDomains = new HashSet<>();

    @OneToMany(mappedBy = "school")
    @Builder.Default
    @ToString.Exclude
    private List<Department> departments = new ArrayList<>();
}