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
            
            // 2. Book 연결 (교재 타입인 경우만 처리)
            if (postDto.getProductType().isTextbookType()) {
                if (postDto.getBookId() != null) {
                    Book book = bookRepository.findById(postDto.getBookId())
                            .orElseThrow(() -> new ValidationException("선택하신 책 정보를 찾을 수 없습니다. 다시 검색해주세요."));
                    post.setBook(book);
                    log.debug("책 정보 연결: bookId={}, title={}", book.getBookId(), book.getTitle());
                }
                // TODO: 향후 수동 입력 기능 추가 시 else if 분기 추가
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
                          List<String> imageOrders, List<String> newImageOrders) {
        log.info("게시글 수정 시작: postId={}", postId);
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        try {
            // 1. 기본 정보 업데이트
            postDto.updateEntity(post);
            
            // 2. Book 연결 업데이트 (교재 타입인 경우만 처리)
            if (postDto.getProductType().isTextbookType()) {
                if (postDto.isRemoveBook()) {
                    // 명시적으로 책 연결 해제 요청
                    log.debug("책 연결 해제 요청");
                    post.setBook(null);
                } else if (postDto.getBookId() != null) {
                    // 새로운 책으로 변경
                    Book book = bookRepository.findById(postDto.getBookId())
                            .orElseThrow(() -> new ValidationException("선택하신 책 정보를 찾을 수 없습니다. 다시 검색해주세요."));
                    post.setBook(book);
                    log.debug("책 정보 업데이트: bookId={}, title={}", book.getBookId(), book.getTitle());
                } else {
                    // bookId가 null이고 removeBook이 false면 기존 연결 유지
                    log.debug("책 정보 유지: 기존 연결 유지");
                }
                // TODO: 향후 수동 입력 기능 추가 시 else if 분기 추가
            } else {
                // 교재 타입이 아닌 경우 책 연결 해제
                if (post.getBook() != null) {
                    log.debug("상품 타입 변경으로 책 연결 해제");
                    post.setBook(null);
                }
            }
            
            // 3. 이미지 삭제 처리
            if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
                deleteImages(post, deleteImageIds);
            }
            
            // 4. 새 이미지 추가 (순서 정보와 함께)
            if (newImages != null && !newImages.isEmpty()) {
                processImagesWithOrder(post, newImages, newImageOrders);
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
        // 현재 이미지 개수 확인
        int currentImageCount = post.getPostImages().size();
        
        // 추가할 이미지 개수 확인
        long newImageCount = images.stream().filter(img -> !img.isEmpty()).count();
        
        if (currentImageCount + newImageCount > AppConstants.MAX_IMAGES_PER_POST) {
            throw new ValidationException("이미지는 최대 " + AppConstants.MAX_IMAGES_PER_POST + "개까지 업로드 가능합니다.");
        }
        
        // 시작 순서 결정 (기존 이미지가 있으면 최대값+1, 없으면 0부터)
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
                
                log.debug("이미지 추가: order={}, path={}", currentOrder, imagePath);
            }
        }
    }
    
    /**
     * 이미지 처리 (순서 지정)
     */
    private void processImagesWithOrder(Post post, List<MultipartFile> images, List<String> newImageOrders) throws IOException {
        Map<Integer, Integer> fileIndexToOrder = new HashMap<>();
        
        // newImageOrders 파싱 ("fileIndex:desiredOrder" 형태)
        if (newImageOrders != null) {
            for (String orderData : newImageOrders) {
                String[] parts = orderData.split(":");
                if (parts.length == 2) {
                    try {
                        int fileIndex = Integer.parseInt(parts[0]);
                        int desiredOrder = Integer.parseInt(parts[1]);
                        fileIndexToOrder.put(fileIndex, desiredOrder);
                    } catch (NumberFormatException e) {
                        log.warn("새 이미지 순서 파싱 실패: {}", orderData);
                    }
                }
            }
        }
        
        // 현재 이미지 개수 확인
        int currentImageCount = post.getPostImages().size();
        long newImageCount = images.stream().filter(img -> !img.isEmpty()).count();
        
        if (currentImageCount + newImageCount > AppConstants.MAX_IMAGES_PER_POST) {
            throw new ValidationException("이미지는 최대 " + AppConstants.MAX_IMAGES_PER_POST + "개까지 업로드 가능합니다.");
        }
        
        // 각 이미지 처리
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            if (!image.isEmpty()) {
                // 파일 유효성 검증
                fileUploadUtil.validateFile(image);
                
                // 이미지 저장
                String imagePath = fileUploadUtil.saveFile(image, "/images/posts/");
                
                // 지정된 순서가 있으면 사용, 없으면 기본값 (현재 최대값 + 1)
                Integer desiredOrder = fileIndexToOrder.get(i);
                int imageOrder = desiredOrder != null ? desiredOrder : 
                    post.getPostImages().stream()
                        .mapToInt(PostImage::getImageOrder)
                        .max()
                        .orElse(-1) + 1;
                
                // PostImage 엔티티 생성
                PostImage postImage = PostImage.builder()
                        .post(post)
                        .imageUrl(imagePath)
                        .imageOrder(imageOrder)
                        .build();
                
                post.getPostImages().add(postImage);
                
                log.debug("이미지 추가: order={}, path={}", imageOrder, imagePath);
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
    private void updateImageOrders(Post post, List<String> imageOrders) {
        if (imageOrders == null || imageOrders.isEmpty()) {
            return;
        }
        
        // "imageId:order" 형태의 문자열을 파싱
        Map<Long, Integer> orderMap = new HashMap<>();
        for (String orderData : imageOrders) {
            String[] parts = orderData.split(":");
            if (parts.length == 2) {
                try {
                    Long imageId = Long.parseLong(parts[0]);
                    Integer order = Integer.parseInt(parts[1]);
                    orderMap.put(imageId, order);
                } catch (NumberFormatException e) {
                    log.warn("이미지 순서 파싱 실패: {}", orderData);
                }
            }
        }
        
        // PostImage 순서 업데이트
        for (PostImage image : post.getPostImages()) {
            Integer newOrder = orderMap.get(image.getPostImageId());
            if (newOrder != null) {
                image.setImageOrder(newOrder);
                log.debug("이미지 순서 업데이트: imageId={}, newOrder={}", 
                         image.getPostImageId(), newOrder);
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