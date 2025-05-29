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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    @Builder.Default
    private SubjectType type = SubjectType.MAJOR;
    
    // year, semester 필드 제거됨 - Post 엔티티에서 관리
    
    // 과목 타입 열거형
    public enum SubjectType {
        MAJOR("전공"),     // 전공과목
        GENERAL("교양");   // 교양과목
        
        private final String displayName;
        
        SubjectType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 학기 열거형 (SubjectBook에서 이동)
    public enum Semester {
        SPRING("1학기"),
        FALL("2학기"),
        SUMMER("여름학기"),
        WINTER("겨울학기");
        
        private final String displayName;
        
        Semester(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}