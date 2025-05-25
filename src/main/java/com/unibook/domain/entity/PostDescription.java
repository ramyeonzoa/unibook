package com.unibook.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "post_descriptions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PostDescription extends BaseEntity {
    @Id
    @Column(name = "post_id")
    private Long postId;

    @NotNull(message = "게시글은 필수입니다")
    @OneToOne
    @MapsId
    @JoinColumn(name = "post_id")
    @ToString.Exclude
    private Post post;

    @Column(columnDefinition = "TEXT")
    private String description;
}