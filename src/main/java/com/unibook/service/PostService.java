package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.PostRequestDto;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.entity.*;
import com.unibook.exception.BusinessException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.BookRepository;
import com.unibook.repository.PostRepository;
import com.unibook.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    
    private final PostRepository postRepository;
    private final BookRepository bookRepository;
    private final FileUploadUtil fileUploadUtil;
    
    // 조회수 중복 방지를 위한 캐시 (userId/sessionId -> postId -> lastViewTime)
    private final Map<String, Map<Long, LocalDateTime>> viewCache = new ConcurrentHashMap<>();
    private static final long VIEW_COUNT_INTERVAL_MINUTES = 30; // 30분마다 조회수 증가 가능
    
    /**
     * 게시글 페이지 조회 (필터링 포함)
     */
    public Page<Post> getPostsPage(Pageable pageable, String search, 
                                  Post.ProductType productType, Post.PostStatus status, Long schoolId) {
        // TODO: QueryDSL 또는 Specification을 사용한 동적 쿼리 구현
        // 현재는 기본 페이징만 제공
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    /**
     * 게시글 단건 조회
     */
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }
    
    /**
     * 게시글 단건 조회 (연관 데이터 포함)
     */
    public Optional<Post> getPostByIdWithDetails(Long id) {
        return postRepository.findByIdWithDetails(id);
    }
    
    /**
     * 최근 게시글 조회
     */
    public List<Post> getRecentPosts(int limit) {
        return postRepository.findRecentPostsWithDetails(PageRequest.of(0, limit));
    }
    
    /**
     * 관련 게시글 조회 (같은 책, 현재 게시글 제외)
     */
    public List<Post> getRelatedPosts(Long bookId, Long excludePostId, int limit) {
        return postRepository.findByBook_BookId(bookId).stream()
                .filter(post -> !post.getPostId().equals(excludePostId))
                .filter(post -> post.getStatus() == Post.PostStatus.AVAILABLE)
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자별 게시글 조회
     */
    public Page<Post> getPostsByUserId(Long userId, Pageable pageable) {
        // TODO: 사용자별 게시글 조회 쿼리 구현
        return Page.empty();
    }
    
    /**
     * 학교별 게시글 조회
     */
    public List<Post> findPostsBySchoolId(Long schoolId) {
        return postRepository.findBySchoolIdWithDetails(schoolId);
    }
    
    /**
     * 상태별 게시글 조회
     */
    public List<Post> findPostsByStatus(Post.PostStatus status) {
        return postRepository.findByStatusWithDetails(status);
    }
    
    /**
     * 게시글 생성
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Post createPost(PostRequestDto postDto, User user, List<MultipartFile> images) {
        log.info("게시글 생성 시작: userId={}, title={}", user.getUserId(), postDto.getTitle());
        
        try {
            // 1. Post 엔티티 생성
            Post post = postDto.toEntity();
            post.setUser(user);
            
            // 2. Book 연결 (교재인 경우)
            if (postDto.getBookId() != null) {
                Book book = bookRepository.findById(postDto.getBookId())
                        .orElseThrow(() -> new ResourceNotFoundException("선택한 책을 찾을 수 없습니다."));
                post.setBook(book);
            }
            
            // 3. Post 저장
            Post savedPost = postRepository.save(post);
            
            // 4. 이미지 처리
            if (images != null && !images.isEmpty()) {
                processImages(savedPost, images);
            }
            
            log.info("게시글 생성 완료: postId={}", savedPost.getPostId());
            return savedPost;
            
        } catch (IOException e) {
            log.error("이미지 업로드 중 오류 발생", e);
            throw new ValidationException("이미지 업로드에 실패했습니다.");
        }
    }
    
    /**
     * 게시글 수정
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Post updatePost(Long postId, PostRequestDto postDto, 
                          List<MultipartFile> newImages, List<Long> deleteImageIds,
                          Map<Long, Integer> imageOrders) {
        log.info("게시글 수정 시작: postId={}", postId);
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        try {
            // 1. 기본 정보 업데이트
            postDto.updateEntity(post);
            
            // 2. Book 연결 업데이트
            if (postDto.getBookId() != null) {
                Book book = bookRepository.findById(postDto.getBookId())
                        .orElseThrow(() -> new ResourceNotFoundException("선택한 책을 찾을 수 없습니다."));
                post.setBook(book);
            } else {
                post.setBook(null);
            }
            
            // 3. 이미지 삭제 처리
            if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
                deleteImages(post, deleteImageIds);
            }
            
            // 4. 새 이미지 추가
            if (newImages != null && !newImages.isEmpty()) {
                processImages(post, newImages);
            }
            
            // 5. 이미지 순서 업데이트
            if (imageOrders != null && !imageOrders.isEmpty()) {
                updateImageOrders(post, imageOrders);
            }
            
            Post updatedPost = postRepository.save(post);
            log.info("게시글 수정 완료: postId={}", postId);
            
            return updatedPost;
            
        } catch (IOException e) {
            log.error("이미지 처리 중 오류 발생", e);
            throw new ValidationException("이미지 처리에 실패했습니다.");
        }
    }
    
    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long postId) {
        log.info("게시글 삭제 시작: postId={}", postId);
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        // 이미지 파일 삭제
        for (PostImage image : post.getPostImages()) {
            fileUploadUtil.deleteFile(image.getImageUrl());
        }
        
        // 게시글 삭제 (CASCADE로 PostImage, PostDescription도 함께 삭제됨)
        postRepository.delete(post);
        
        log.info("게시글 삭제 완료: postId={}", postId);
    }
    
    /**
     * 게시글 상태 변경
     */
    @Transactional
    public void updatePostStatus(Long postId, Post.PostStatus status) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        post.setStatus(status);
        postRepository.save(post);
        
        log.info("게시글 상태 변경: postId={}, status={}", postId, status);
    }
    
    /**
     * 조회수 증가 (비동기, 중복 방지)
     */
    @Async
    @Transactional
    public void incrementViewCountAsync(Long postId, Long userId) {
        String cacheKey = userId != null ? "user_" + userId : "session_" + UUID.randomUUID();
        
        // 캐시 확인
        Map<Long, LocalDateTime> userViewHistory = viewCache.computeIfAbsent(cacheKey, k -> new ConcurrentHashMap<>());
        LocalDateTime lastViewTime = userViewHistory.get(postId);
        LocalDateTime now = LocalDateTime.now();
        
        // 마지막 조회 시간으로부터 지정된 시간이 지났는지 확인
        if (lastViewTime == null || lastViewTime.plusMinutes(VIEW_COUNT_INTERVAL_MINUTES).isBefore(now)) {
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null) {
                post.setViewCount(post.getViewCount() + 1);
                postRepository.save(post);
                userViewHistory.put(postId, now);
                
                log.debug("조회수 증가: postId={}, viewCount={}", postId, post.getViewCount());
            }
        }
        
        // 오래된 캐시 정리 (1시간 이상 된 기록)
        cleanupViewCache();
    }
    
    /**
     * 이미지 처리
     */
    private void processImages(Post post, List<MultipartFile> images) throws IOException {
        int currentOrder = post.getPostImages().stream()
                .mapToInt(PostImage::getImageOrder)
                .max()
                .orElse(-1);
        
        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                // 파일 유효성 검증
                fileUploadUtil.validateFile(image);
                
                // 이미지 저장
                String imagePath = fileUploadUtil.saveFile(image, "/images/posts/");
                
                // PostImage 엔티티 생성
                PostImage postImage = PostImage.builder()
                        .post(post)
                        .imageUrl(imagePath)
                        .imageOrder(++currentOrder)
                        .build();
                
                post.getPostImages().add(postImage);
            }
        }
    }
    
    /**
     * 이미지 삭제
     */
    private void deleteImages(Post post, List<Long> imageIds) {
        List<PostImage> imagesToDelete = post.getPostImages().stream()
                .filter(img -> imageIds.contains(img.getPostImageId()))
                .collect(Collectors.toList());
        
        for (PostImage image : imagesToDelete) {
            // 파일 삭제
            fileUploadUtil.deleteFile(image.getImageUrl());
            // 엔티티에서 제거
            post.getPostImages().remove(image);
        }
    }
    
    /**
     * 이미지 순서 업데이트
     */
    private void updateImageOrders(Post post, Map<Long, Integer> imageOrders) {
        for (PostImage image : post.getPostImages()) {
            Integer newOrder = imageOrders.get(image.getPostImageId());
            if (newOrder != null) {
                image.setImageOrder(newOrder);
            }
        }
    }
    
    /**
     * 조회수 캐시 정리
     */
    private void cleanupViewCache() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        viewCache.forEach((userKey, postHistory) -> {
            postHistory.entrySet().removeIf(entry -> entry.getValue().isBefore(oneHourAgo));
        });
        
        // 빈 사용자 기록 제거
        viewCache.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    // DTO 반환 메서드
    public List<PostResponseDto> getRecentPostDtos(int limit) {
        return getRecentPosts(limit).stream()
                .map(PostResponseDto::listFrom)
                .collect(Collectors.toList());
    }
}