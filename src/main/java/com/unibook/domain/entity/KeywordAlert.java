package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "keyword_alerts",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "keyword"})
       },
       indexes = {
           @Index(name = "idx_keyword_alert_user", columnList = "user_id"),
           @Index(name = "idx_keyword_alert_keyword", columnList = "keyword")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
public class KeywordAlert extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long keywordAlertId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank(message = "키워드는 필수입니다")
    @Size(min = 2, max = 50, message = "키워드는 2자 이상 50자 이하여야 합니다")
    @Column(nullable = false, length = 50)
    private String keyword;
}