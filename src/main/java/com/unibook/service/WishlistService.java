package com.unibook.service;

import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.domain.entity.Wishlist;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.repository.PostRepository;
import com.unibook.repository.UserRepository;
import com.unibook.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    /**
     * 찜하기 추가/제거 토글
     */
    @Transactional
    public boolean toggleWishlist(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다"));
        
        // 자신의 게시글은 찜할 수 없음
        if (post.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("자신의 게시글은 찜할 수 없습니다");
        }
        
        Optional<Wishlist> existingWishlist = wishlistRepository.findByUserUserIdAndPostPostId(userId, postId);
        
        if (existingWishlist.isPresent()) {
            // 찜 제거
            wishlistRepository.delete(existingWishlist.get());
            post.setWishlistCount(Math.max(0, post.getWishlistCount() - 1));
            log.info("찜 제거: userId={}, postId={}", userId, postId);
            return false;
        } else {
            // 찜 추가
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .post(post)
                    .build();
            wishlistRepository.save(wishlist);
            post.setWishlistCount(post.getWishlistCount() + 1);
            log.info("찜 추가: userId={}, postId={}", userId, postId);
            return true;
        }
    }
    
    /**
     * 사용자의 찜 여부 확인
     */
    public boolean isWishlisted(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return false;
        }
        return wishlistRepository.existsByUserUserIdAndPostPostId(userId, postId);
    }
    
    /**
     * 사용자의 찜 목록 조회 (페이징)
     */
    public Page<Wishlist> getUserWishlists(Long userId, Pageable pageable) {
        return wishlistRepository.findByUserIdWithPost(userId, pageable);
    }
    
    /**
     * 찜하기 제거 (특정)
     */
    @Transactional
    public void removeWishlist(Long userId, Long postId) {
        Wishlist wishlist = wishlistRepository.findByUserUserIdAndPostPostId(userId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("찜 정보를 찾을 수 없습니다"));
        
        Post post = wishlist.getPost();
        wishlistRepository.delete(wishlist);
        post.setWishlistCount(Math.max(0, post.getWishlistCount() - 1));
        
        log.info("찜 제거: userId={}, postId={}", userId, postId);
    }
}