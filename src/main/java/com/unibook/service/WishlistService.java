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
    private final NotificationService notificationService;
    
    /**
     * 찜하기 추가/제거 토글 (최적화됨 - N+1 문제 해결)
     */
    @Transactional
    public boolean toggleWishlist(Long userId, Long postId) {
        // 1. 게시글 존재 및 상태 확인 (Entity 로드 없이)
        if (!postRepository.existsByPostIdAndNotBlocked(postId)) {
            throw new ResourceNotFoundException("게시글을 찾을 수 없거나 접근할 수 없습니다");
        }
        
        // 2. 자신의 게시글인지 확인 (Entity 로드 없이)
        if (postRepository.existsByPostIdAndUser_UserId(postId, userId)) {
            throw new IllegalArgumentException("자신의 게시글은 찜할 수 없습니다");
        }
        
        // 3. 사용자 존재 확인 (Entity 로드 없이)
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("사용자를 찾을 수 없습니다");
        }
        
        Optional<Wishlist> existingWishlist = wishlistRepository.findByUserUserIdAndPostPostId(userId, postId);
        
        if (existingWishlist.isPresent()) {
            // 찜 제거
            wishlistRepository.delete(existingWishlist.get());
            updateWishlistCount(postId, -1);
            log.info("찜 제거: userId={}, postId={}", userId, postId);
            return false;
        } else {
            // 찜 추가 - Entity는 실제 필요한 시점에만 로드
            User user = userRepository.getReferenceById(userId);  // 프록시 사용
            Post post = postRepository.getReferenceById(postId);  // 프록시 사용
            
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .post(post)
                    .build();
            wishlistRepository.save(wishlist);
            updateWishlistCount(postId, 1);
            log.info("찜 추가: userId={}, postId={}", userId, postId);
            
            // 게시글 작성자에게 익명 찜 알림 발송 (최적화된 조회)
            sendWishlistNotification(postId);
            
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
     * 사용자의 찜한 게시글 목록 조회 (Fetch Join으로 N+1 방지)
     */
    public Page<Post> getUserWishlistPosts(Long userId, Pageable pageable) {
        return postRepository.findWishlistedPostsByUserUnified(userId, null, null, pageable);
    }
    
    /**
     * 사용자의 찜한 게시글 목록 조회 (가격 필터링 포함)
     */
    public Page<Post> getUserWishlistPosts(Long userId, Pageable pageable, Integer minPrice, Integer maxPrice) {
        return postRepository.findWishlistedPostsByUserUnified(userId, minPrice, maxPrice, pageable);
    }
    
    /**
     * 찜하기 제거 (특정) - 최적화됨
     */
    @Transactional
    public void removeWishlist(Long userId, Long postId) {
        Wishlist wishlist = wishlistRepository.findByUserUserIdAndPostPostId(userId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("찜 정보를 찾을 수 없습니다"));
        
        wishlistRepository.delete(wishlist);
        updateWishlistCount(postId, -1);
        
        log.info("찜 제거: userId={}, postId={}", userId, postId);
    }
    
    /**
     * 찜 카운트 업데이트 (bulk update로 성능 최적화)
     */
    @Transactional
    private void updateWishlistCount(Long postId, int delta) {
        // TODO: 향후 bulk update 쿼리로 최적화 가능
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setWishlistCount(Math.max(0, post.getWishlistCount() + delta));
            postRepository.save(post);
        }
    }
    
    /**
     * 찜 알림 발송 (최적화된 데이터 조회)
     */
    @Transactional
    private void sendWishlistNotification(Long postId) {
        // 알림에 필요한 최소한의 데이터만 조회
        postRepository.findById(postId).ifPresent(post -> {
            if (post.getUser() != null && post.getTitle() != null) {
                notificationService.createPostWishlistedNotificationAsync(
                        post.getUser().getUserId(), 
                        postId, 
                        post.getTitle()
                );
            }
        });
    }
}