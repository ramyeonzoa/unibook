package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.PostRequestDto;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.entity.*;
import com.unibook.exception.BusinessException;
import com.unibook.exception.DuplicateResourceException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.BookRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.SubjectRepository;
import com.unibook.repository.WishlistRepository;
import com.unibook.repository.projection.PostSearchProjection;
import com.unibook.util.FileUploadUtil;
import com.unibook.util.QueryNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    
    private final PostRepository postRepository;
    private final BookRepository bookRepository;
    private final SubjectRepository subjectRepository;
    private final WishlistRepository wishlistRepository;
    private final FileUploadUtil fileUploadUtil;
    private final SubjectBookService subjectBookService;
    private final NotificationService notificationService;
    
    // 조회수 중복 방지를 위한 캐시 (userId/sessionId -> postId -> lastViewTime)
    private final Map<String, Map<Long, LocalDateTime>> viewCache = new ConcurrentHashMap<>();
    private static final long VIEW_COUNT_INTERVAL_MINUTES = 30; // 30분마다 조회수 증가 가능
    
    /**
     * 게시글 페이지 조회 (필터링 포함)
     * Full-text 검색 및 필터링 지원
     */
    public Page<Post> getPostsPage(Pageable pageable, String search, 
                                  Post.ProductType productType, Post.PostStatus status, Long schoolId, String sortBy) {
        
        // 검색어가 있는 경우
        if (search != null && !search.trim().isEmpty()) {
            String normalized = QueryNormalizer.normalize(search);
            
            // 최소 2글자 이상만 검색 (ngram_token_size=2)
            if (normalized.length() >= 2) {
                log.info("Full-text 검색 실행: query='{}', normalized='{}', productType={}, status={}, schoolId={}", 
                        search, normalized, productType, status, schoolId);
                
                // OR 검색을 기본으로 사용 (더 유연한 검색 결과)
                // "선형대수학 김선형" → "선형대수학 김선형" (각 단어 중 하나라도 포함)
                // QueryNormalizer가 이미 특수문자를 제거했으므로 안전함
                String booleanQuery = normalized;
                
                // Full-text 검색 실행
                Page<PostSearchProjection> searchResults = postRepository.searchPostsWithFulltext(
                        booleanQuery,
                        status != null ? status.name() : null,
                        productType != null ? productType.name() : null,
                        schoolId,
                        pageable
                );
                
                // 검색 결과가 있으면 Post 엔티티로 변환
                if (!searchResults.isEmpty()) {
                    // 관련도 점수 로깅 (디버그용)
                    if (log.isDebugEnabled()) {
                        searchResults.getContent().forEach(result -> 
                            log.debug("Post ID: {}, Score: {}", result.getPostId(), result.getTotalScore())
                        );
                    }
                    List<Long> postIds = searchResults.getContent().stream()
                            .map(PostSearchProjection::getPostId)
                            .collect(Collectors.toList());
                    
                    // ID로 Post 엔티티들을 조회 (Fetch Join)
                    List<Post> posts = postRepository.findAllByIdInWithDetails(postIds);
                    
                    // 검색 결과의 순서를 유지하기 위해 Map으로 변환 후 정렬
                    Map<Long, Post> postMap = posts.stream()
                            .collect(Collectors.toMap(Post::getPostId, post -> post));
                    
                    List<Post> orderedPosts;
                    
                    // 정렬 옵션에 따라 재정렬
                    if ("RELEVANCE".equals(sortBy)) {
                        // 관련도순 - 검색 결과 순서 유지
                        orderedPosts = postIds.stream()
                                .map(postMap::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    } else {
                        // 다른 정렬 옵션 적용
                        Stream<Post> postStream = postMap.values().stream();
                        
                        switch (sortBy) {
                            case "PRICE_ASC":
                                postStream = postStream.sorted(Comparator.comparing(Post::getPrice));
                                break;
                            case "PRICE_DESC":
                                postStream = postStream.sorted(Comparator.comparing(Post::getPrice).reversed());
                                break;
                            case "VIEW_COUNT":
                                postStream = postStream.sorted(Comparator.comparing(Post::getViewCount, 
                                        Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                                break;
                            case "NEWEST":
                            default:
                                postStream = postStream.sorted(Comparator.comparing(Post::getCreatedAt).reversed());
                                break;
                        }
                        
                        orderedPosts = postStream.collect(Collectors.toList());
                    }
                    
                    // 페이징 처리
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), orderedPosts.size());
                    
                    List<Post> pagedPosts = orderedPosts.subList(start, Math.min(end, orderedPosts.size()));
                    
                    // Page 객체로 변환하여 반환
                    return new PageImpl<>(pagedPosts, pageable, searchResults.getTotalElements());
                }
                
                return Page.empty(pageable);
            } else {
                log.info("검색어가 너무 짧음: '{}' (최소 2글자)", normalized);
                // 검색어가 짧을 때도 필터링은 적용
            }
        }
        
        // 검색어가 없거나 너무 짧은 경우 - 필터링만 적용
        log.info("필터링 조회: productType={}, status={}, schoolId={}", productType, status, schoolId);
        return postRepository.findByFilters(status, productType, schoolId, pageable);
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
     * 같은 과목의 다른 게시글 조회 (현재 게시글 제외)
     */
    public List<Post> getRelatedPostsBySubject(Long subjectId, Long excludePostId, int limit) {
        if (subjectId == null) {
            return List.of();
        }
        return postRepository.findBySubject_SubjectIdWithDetails(subjectId).stream()
                .filter(post -> !post.getPostId().equals(excludePostId))
                .filter(post -> post.getStatus() == Post.PostStatus.AVAILABLE)
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자별 게시글 조회 (Fetch Join으로 N+1 방지)
     */
    public Page<Post> getPostsByUserId(Long userId, Pageable pageable) {
        return postRepository.findByUserIdWithDetails(userId, pageable);
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
            
            // 3. Subject 연결 (모든 상품 타입에서 가능)
            if (postDto.getSubjectId() != null) {
                Subject subject = subjectRepository.findById(postDto.getSubjectId())
                        .orElseThrow(() -> new ValidationException("선택하신 과목 정보를 찾을 수 없습니다."));
                post.setSubject(subject);
                post.setTakenYear(postDto.getTakenYear());
                post.setTakenSemester(postDto.getTakenSemester());
                log.debug("과목 정보 연결: subjectId={}, name={}, year={}, semester={}", 
                        subject.getSubjectId(), subject.getSubjectName(), 
                        postDto.getTakenYear(), postDto.getTakenSemester());
            }
            
            // 4. Post 저장
            Post savedPost = postRepository.save(post);
            
            // 5. SubjectBook 연결 처리 (과목과 책이 모두 연결된 경우)
            if (savedPost.getSubject() != null && savedPost.getBook() != null) {
                handleSubjectBookConnection(savedPost);
            }
            
            // 6. 이미지 처리
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
            // 기존 SubjectBook 연결 정보 저장 (reference count 관리용)
            Long oldSubjectId = post.getSubject() != null ? post.getSubject().getSubjectId() : null;
            Long oldBookId = post.getBook() != null ? post.getBook().getBookId() : null;
            boolean hadSubjectBookConnection = (oldSubjectId != null && oldBookId != null);
            
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
            
            // 3. Subject 연결 업데이트 (모든 상품 타입에서 가능)
            if (postDto.isRemoveSubject()) {
                // 명시적으로 과목 연결 해제 요청
                log.debug("과목 연결 해제 요청");
                post.setSubject(null);
                post.setTakenYear(null);
                post.setTakenSemester(null);
            } else if (postDto.getSubjectId() != null) {
                // 새로운 과목으로 변경
                Subject subject = subjectRepository.findById(postDto.getSubjectId())
                        .orElseThrow(() -> new ValidationException("선택하신 과목 정보를 찾을 수 없습니다."));
                post.setSubject(subject);
                post.setTakenYear(postDto.getTakenYear());
                post.setTakenSemester(postDto.getTakenSemester());
                log.debug("과목 정보 업데이트: subjectId={}, name={}, year={}, semester={}", 
                        subject.getSubjectId(), subject.getSubjectName(),
                        postDto.getTakenYear(), postDto.getTakenSemester());
            }
            
            // 4. SubjectBook reference count 관리
            Long newSubjectId = post.getSubject() != null ? post.getSubject().getSubjectId() : null;
            Long newBookId = post.getBook() != null ? post.getBook().getBookId() : null;
            boolean hasNewSubjectBookConnection = (newSubjectId != null && newBookId != null);
            
            // 기존 연결과 새 연결이 다른 경우 reference count 업데이트
            if (hadSubjectBookConnection && 
                (!hasNewSubjectBookConnection || !oldSubjectId.equals(newSubjectId) || !oldBookId.equals(newBookId))) {
                // 기존 연결 해제 (reference count 감소)
                subjectBookService.decrementPostCount(oldSubjectId, oldBookId);
            }
            
            if (hasNewSubjectBookConnection && 
                (!hadSubjectBookConnection || !oldSubjectId.equals(newSubjectId) || !oldBookId.equals(newBookId))) {
                // 새 연결 생성 (reference count 증가)
                subjectBookService.incrementPostCount(newSubjectId, newBookId);
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
            
            // Note: SubjectBook reference count는 위에서 이미 처리됨
            
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
        
        // SubjectBook reference count 감소 (게시글 삭제 전에 처리)
        if (post.getSubject() != null && post.getBook() != null) {
            try {
                subjectBookService.decrementPostCount(
                    post.getSubject().getSubjectId(), 
                    post.getBook().getBookId()
                );
                log.info("게시글 삭제로 인한 SubjectBook 참조 카운트 감소 완료: postId={}, subjectId={}, bookId={}", 
                        postId, post.getSubject().getSubjectId(), post.getBook().getBookId());
            } catch (Exception e) {
                log.warn("SubjectBook 참조 카운트 감소 실패 (게시글 삭제는 계속 진행): postId={}, error={}", 
                        postId, e.getMessage());
            }
        }
        
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
        
        // 상태가 실제로 변경되는 경우에만 알림 발송
        Post.PostStatus oldStatus = post.getStatus();
        if (oldStatus != status) {
            post.setStatus(status);
            postRepository.save(post);
            
            log.info("게시글 상태 변경: postId={}, oldStatus={}, newStatus={}", postId, oldStatus, status);
            
            // 찜한 사용자들에게 알림 발송 (비동기)
            publishWishlistStatusChangeNotifications(postId, status);
        }
    }
    
    /**
     * 게시글을 찜한 사용자들에게 상태 변경 알림 발송 (postId 버전)
     */
    private void publishWishlistStatusChangeNotifications(Long postId, Post.PostStatus newStatus) {
        try {
            // 해당 게시글을 찜한 모든 사용자 조회
            List<Wishlist> wishlists = wishlistRepository.findByPostIdWithUser(postId);
            
            if (wishlists.isEmpty()) {
                log.debug("게시글을 찜한 사용자가 없음: postId={}", postId);
                return;
            }
            
            log.info("찜 상태 변경 알림 발송 시작: postId={}, userCount={}", postId, wishlists.size());
            
            // 각 사용자에게 비동기로 알림 발송
            for (Wishlist wishlist : wishlists) {
                try {
                    Long recipientUserId = wishlist.getUser().getUserId();
                    notificationService.createWishlistStatusNotificationAsync(recipientUserId, postId, newStatus);
                } catch (Exception e) {
                    // 개별 사용자 알림 실패는 로그만 남기고 계속 진행
                    log.warn("개별 알림 발송 실패: userId={}, postId={}, error={}", 
                            wishlist.getUser().getUserId(), postId, e.getMessage());
                }
            }
            
            log.info("찜 상태 변경 알림 발송 완료: postId={}", postId);
            
        } catch (Exception e) {
            // 알림 발송 실패가 게시글 상태 변경을 방해하면 안 됨
            log.warn("알림 발송 중 오류 발생: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 게시글을 찜한 사용자들에게 상태 변경 알림 발송 (Post 객체 버전)
     * 테스트 및 직접 호출용
     */
    public void publishWishlistStatusChangeNotifications(Post post, Post.PostStatus newStatus) {
        // 현재 상태와 새 상태가 같으면 알림 발송하지 않음
        if (post.getStatus() == newStatus) {
            log.debug("현재 상태와 동일하여 알림 발송 생략: postId={}, status={}", post.getPostId(), newStatus);
            return;
        }

        try {
            // 해당 게시글을 찜한 모든 사용자 조회
            List<Wishlist> wishlists = wishlistRepository.findByPostIdWithUser(post.getPostId());
            
            if (wishlists.isEmpty()) {
                log.debug("게시글을 찜한 사용자가 없음: postId={}", post.getPostId());
                return;
            }
            
            log.info("찜 상태 변경 알림 발송 시작: postId={}, userCount={}", post.getPostId(), wishlists.size());
            
            // 각 사용자에게 비동기로 알림 발송
            for (Wishlist wishlist : wishlists) {
                try {
                    Long recipientUserId = wishlist.getUser().getUserId();
                    notificationService.createWishlistStatusNotificationAsync(recipientUserId, post.getPostId(), newStatus);
                } catch (Exception e) {
                    // 개별 사용자 알림 실패는 로그만 남기고 계속 진행
                    log.warn("개별 알림 발송 실패: userId={}, postId={}, error={}", 
                            wishlist.getUser().getUserId(), post.getPostId(), e.getMessage());
                }
            }
            
            log.info("찜 상태 변경 알림 발송 완료: postId={}", post.getPostId());
            
        } catch (Exception e) {
            // 알림 발송 실패가 게시글 상태 변경을 방해하면 안 됨
            log.warn("알림 발송 중 오류 발생: postId={}, error={}", post.getPostId(), e.getMessage());
        }
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
    
    /**
     * SubjectBook 연결 처리 및 reference count 증가
     * 책임 분리: productType 체크는 호출부에서, 여기서는 순수 매핑 처리만
     */
    private void handleSubjectBookConnection(Post post) {
        // 사전 조건: 과목과 책이 모두 연결되어 있음
        // (호출부에서 체크 완료)
        
        try {
            // SubjectBook 연결 찾기/생성 및 reference count 증가
            subjectBookService.incrementPostCount(
                post.getSubject().getSubjectId(), 
                post.getBook().getBookId()
            );
            
            log.info("SubjectBook 연결 및 참조 카운트 증가 완료: postId={}, subjectId={}, bookId={}", 
                    post.getPostId(), post.getSubject().getSubjectId(), post.getBook().getBookId());
                    
        } catch (ResourceNotFoundException | ValidationException | DuplicateResourceException e) {
            // 예상 가능한 비즈니스 예외만 처리 - 경고 로그 후 계속 진행
            log.warn("SubjectBook 연결 실패 (게시글 처리는 계속 진행): postId={}, subjectId={}, bookId={}, error={}", 
                    post.getPostId(), post.getSubject().getSubjectId(), post.getBook().getBookId(), e.getMessage());
        }
        // NPE, 프로그래밍 오류 등은 상위로 전파하여 개발자가 인지할 수 있도록 함
    }
}