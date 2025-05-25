package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_department", columnList = "department_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Email(message = "올바른 이메일 형식이어야 합니다")
    @NotBlank(message = "이메일은 필수입니다")
    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @ToString.Exclude
    @Column(nullable = false, length = 255)
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    @Column(nullable = false, length = 50)
    private String name;
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Column(nullable = false, length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @ToString.Exclude
    private Department department;

    // Department를 통해 School에 접근하는 헬퍼 메서드
    public School getSchool() {
        return department != null ? department.getSchool() : null;
    }

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
    
    public boolean isVerified() {
        return verified != null && verified;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime passwordUpdatedAt;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    private List<Post> posts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (passwordUpdatedAt == null) {
            passwordUpdatedAt = LocalDateTime.now();
        }
    }

    // Enum 정의
    public enum UserRole {
        ADMIN, USER  // STUDENT -> USER로 변경
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, WITHDRAWN  // BANNED -> SUSPENDED로 변경
    }
}