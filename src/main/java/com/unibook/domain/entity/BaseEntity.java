package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseEntity {
    
    /**
     * 시스템 사용자 ID (회원가입, 배치 작업, 스케줄러 등)
     * 실제 사용자 ID는 1부터 시작하므로 충돌하지 않음
     * 
     * 사용 예시:
     * - 시스템이 생성한 데이터 조회: WHERE created_by = 0
     * - 사용자가 생성한 데이터 조회: WHERE created_by > 0
     */
    public static final Long SYSTEM_USER_ID = 0L;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(nullable = false)
    private Long updatedBy;
}