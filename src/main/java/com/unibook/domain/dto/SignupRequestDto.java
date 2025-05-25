package com.unibook.domain.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequestDto {
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
             message = "대학교 이메일을 사용해주세요")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&_])[A-Za-z\\d@$!%*#?&_]+$",
             message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;
    
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String passwordConfirm;
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글, 영문만 가능합니다")
    private String name;
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", 
             message = "올바른 전화번호 형식이 아닙니다")
    private String phoneNumber;
    
    @NotNull(message = "학과를 선택해주세요")
    @Positive(message = "올바른 학과를 선택해주세요")
    private Long departmentId;
    
    @AssertTrue(message = "비밀번호가 일치하지 않습니다")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(passwordConfirm);
    }
}