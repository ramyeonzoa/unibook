package com.unibook.domain.dto;

import com.unibook.domain.entity.School;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolDto {
    private Long schoolId;
    private String schoolName;
    private String primaryDomain;
    
    public static SchoolDto from(School school) {
        return SchoolDto.builder()
                .schoolId(school.getSchoolId())
                .schoolName(school.getSchoolName())
                .primaryDomain(school.getPrimaryDomain())
                .build();
    }
}