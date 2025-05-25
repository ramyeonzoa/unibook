package com.unibook.domain.dto;

import com.unibook.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    
    private Long userId;
    private String email;
    private String name;
    private String phoneNumber;
    private DepartmentDto department;
    private SchoolDto school;
    private boolean verified;
    private User.UserRole role;
    private User.UserStatus status;
    private LocalDateTime createdAt;
    
    // Department 정보를 담는 내부 DTO
    @Data
    @Builder
    public static class DepartmentDto {
        private Long departmentId;
        private String departmentName;
    }
    
    // School 정보를 담는 내부 DTO
    @Data
    @Builder
    public static class SchoolDto {
        private Long schoolId;
        private String schoolName;
        private String primaryDomain;
    }
    
    // Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static UserResponseDto from(User user) {
        UserResponseDtoBuilder builder = UserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .verified(user.isVerified())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt());
        
        // Department 정보 설정
        if (user.getDepartment() != null) {
            builder.department(DepartmentDto.builder()
                    .departmentId(user.getDepartment().getDepartmentId())
                    .departmentName(user.getDepartment().getDepartmentName())
                    .build());
            
            // School 정보 설정 (Department를 통해)
            if (user.getDepartment().getSchool() != null) {
                builder.school(SchoolDto.builder()
                        .schoolId(user.getDepartment().getSchool().getSchoolId())
                        .schoolName(user.getDepartment().getSchool().getSchoolName())
                        .primaryDomain(user.getDepartment().getSchool().getPrimaryDomain())
                        .build());
            }
        }
        
        return builder.build();
    }
}