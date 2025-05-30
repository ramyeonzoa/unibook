package com.unibook.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlists",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "post_id"})
       },
       indexes = {
           @Index(name = "idx_user_wishlist", columnList = "user_id"),
           @Index(name = "idx_post_wishlist", columnList = "post_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "post"})
public class Wishlist extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wishlistId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}