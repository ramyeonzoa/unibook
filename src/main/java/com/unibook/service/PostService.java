package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.PostRequestDto;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.dto.PriceTrendDto;
import com.unibook.domain.entity.*;
import com.unibook.exception.BusinessException;
import com.unibook.exception.DuplicateResourceException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.repository.BookRepository;
import com.unibook.repository.ChatRoomRepository;
import com.unibook.repository.PostRepository;
import com.unibook.repository.ReportRepository;
import com.unibook.repository.SubjectRepository;
import com.unibook.repository.WishlistRepository;
import com.unibook.domain.entity.Report;
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
import jakarta.persistence.EntityManager;

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
    private final ChatRoomRepository chatRoomRepository;
    private final ReportRepository reportRepository;
    private final FileUploadUtil fileUploadUtil;
    private final SubjectBookService subjectBookService;
    private final NotificationService notificationService;
    private final KeywordAlertService keywordAlertService;
    private final EntityManager entityManager;
    
    // 조회수 중복 방지를 위한 캐시 (userId/sessionId -> postId -> lastViewTime)
    private final Map<String, Map<Long, LocalDateTime>> viewCache = new ConcurrentHashMap<>();
    private static final long VIEW_COUNT_INTERVAL_MINUTES = 30; // 30분마다 조회수 증가 가능
    
    /**
     * 게시글 페이지 조회 (필터링 포함)
     * Full-text 검색 및 필터링 지원 - 모든 조건이 함께 적용됨
     */
    public Page<Post> getPostsPage(Pageable pageable, String search, 
                                  Post.ProductType productType, Post.PostStatus status, Long schoolId, String sortBy,
                                  Integer minPrice, Integer maxPrice, Long subjectId, Long professorId, String bookTitle, Long bookId, Long departmentId) {
        
        String trimmedBookTitle = (bookTitle != null && !bookTitle.trim().isEmpty()) ? bookTitle.trim() : null;
        
        // 로깅을 위한 조건 확인
        boolean hasSpecificFilters = subjectId != null || professorId != null || trimmedBookTitle != null || bookId != null || departmentId != null;
        boolean hasSearch = search != null && !search.trim().isEmpty();
        
        if (hasSpecificFilters) {
            if (subjectId != null) {
                log.info("과목 ID로 검색: subjectId={}, search='{}', status={}, productType={}", 
                        subjectId, search, status, productType);
            } else if (professorId != null) {
                log.info("교수 ID로 검색: professorId={}, search='{}', status={}, productType={}", 
                        professorId, search, status, productType);
            } else if (departmentId != null) {
                log.info("학과 ID로 검색: departmentId={}, search='{}', status={}, productType={}", 
                        departmentId, search, status, productType);
            } else if (bookId != null) {
                log.info("책 ID로 검색: bookId={}, search='{}', status={}, productType={}", 
                        bookId, search, status, productType);
            } else {
                log.info("책 제목으로 검색: bookTitle='{}', search='{}', status={}, productType={}", 
                        trimmedBookTitle, search, status, productType);
            }
        }
        
        // 검색어가 있는 경우 Full-text 검색 우선 실행
        if (hasSearch) {
            String normalized = QueryNormalizer.normalize(search);
            
            // 최소 2글자 이상만 검색 (ngram_token_size=2)
            if (normalized.length() >= 2) {
                log.info("Full-text 검색 실행: query='{}', normalized='{}', 추가 필터={}", 
                        search, normalized, hasSpecificFilters);
                
                String booleanQuery = normalized;
                
                // Full-text 검색 실행 (기본 필터만 적용)
                Page<PostSearchProjection> searchResults = postRepository.searchPostsWithFulltext(
                        booleanQuery,
                        status != null ? status.name() : null,
                        productType != null ? productType.name() : null,
                        schoolId,
                        minPrice,
                        maxPrice,
                        pageable
                );
                
                // 검색 결과가 있으면 Post 엔티티로 변환 및 추가 필터링
                if (!searchResults.isEmpty()) {
                    List<Long> postIds = searchResults.getContent().stream()
                            .map(PostSearchProjection::getPostId)
                            .collect(Collectors.toList());
                    
                    // ID로 Post 엔티티들을 조회 (Fetch Join)
                    List<Post> posts = postRepository.findAllByIdInWithDetails(postIds);
                    
                    // 추가 필터링 적용 (subjectId, professorId, bookTitle, departmentId)
                    List<Post> filteredPosts = applyAdditionalFilters(posts, subjectId, professorId, trimmedBookTitle, departmentId);
                    
                    // 정렬 적용
                    List<Post> orderedPosts = applySorting(filteredPosts, postIds, sortBy);
                    
                    // 페이징 처리 (추가 필터링으로 인한 결과 수 변화 반영)
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), orderedPosts.size());
                    
                    if (start >= orderedPosts.size()) {
                        return Page.empty(pageable);
                    }
                    
                    List<Post> pagedPosts = orderedPosts.subList(start, end);
                    
                    // 전체 결과 수는 추가 필터링 후의 결과 수로 설정
                    return new PageImpl<>(pagedPosts, pageable, orderedPosts.size());
                }
                
                return Page.empty(pageable);
            } else {
                log.info("검색어가 너무 짧음: '{}' (최소 2글자), 필터링만 적용", normalized);
            }
        }
        
        // 검색어가 없거나 너무 짧은 경우 - 모든 필터링 적용
        log.info("통합 필터링 조회: subjectId={}, professorId={}, departmentId={}, bookTitle='{}', bookId={}, status={}, productType={}", 
                subjectId, professorId, departmentId, trimmedBookTitle, bookId, status, productType);
        return postRepository.findPostsWithOptionalFilters(
            subjectId, professorId, departmentId, trimmedBookTitle, bookId,
            status, productType, schoolId, minPrice, maxPrice, pageable);
    }
    
    /**
     * Full-text 검색 결과에 추가 필터 적용
     */
    private List<Post> applyAdditionalFilters(List<Post> posts, Long subjectId, Long professorId, String bookTitle, Long departmentId) {
        return posts.stream()
                .filter(post -> {
                    // 과목 필터
                    if (subjectId != null) {
                        return post.getSubject() != null && subjectId.equals(post.getSubject().getSubjectId());
                    }
                    
                    // 교수 필터
                    if (professorId != null) {
                        return post.getSubject() != null && 
                               post.getSubject().getProfessor() != null && 
                               professorId.equals(post.getSubject().getProfessor().getProfessorId());
                    }
                    
                    // 학과 필터
                    if (departmentId != null) {
                        return post.getSubject() != null && 
                               post.getSubject().getProfessor() != null &&
                               post.getSubject().getProfessor().getDepartment() != null && 
                               departmentId.equals(post.getSubject().getProfessor().getDepartment().getDepartmentId());
                    }
                    
                    // 책 제목 필터
                    if (bookTitle != null) {
                        return post.getBook() != null && 
                               post.getBook().getTitle() != null &&
                               post.getBook().getTitle().toLowerCase().contains(bookTitle.toLowerCase());
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 정렬 적용
     */
    private List<Post> applySorting(List<Post> posts, List<Long> originalOrder, String sortBy) {
        if ("RELEVANCE".equals(sortBy)) {
            // 관련도순 - 원래 검색 결과 순서 유지
            Map<Long, Post> postMap = posts.stream()
                    .collect(Collectors.toMap(Post::getPostId, post -> post));
            
            return originalOrder.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            // 다른 정렬 옵션 적용
            Stream<Post> postStream = posts.stream();
            
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
            
            return postStream.collect(Collectors.toList());
        }
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
        return postRepository.findByBook_BookIdAndStatusNot(bookId, Post.PostStatus.BLOCKED).stream()
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
        return postRepository.findUserPostsByUserUnified(userId, null, null, pageable);
    }
    
    /**
     * 사용자별 게시글 조회 (가격 필터링 포함)
     */
    public Page<Post> getPostsByUserId(Long userId, Pageable pageable, Integer minPrice, Integer maxPrice) {
        return postRepository.findUserPostsByUserUnified(userId, minPrice, maxPrice, pageable);
    }
    
    /**
     * 과목 정보를 조회해서 페이지 제목용 문자열 반환
     */
    public String getSubjectInfoForTitle(Long subjectId) {
        Optional<Subject> subject = subjectRepository.findById(subjectId);
        if (subject.isPresent()) {
            Subject s = subject.get();
            if (s.getProfessor() != null) {
                return s.getSubjectName() + " (" + s.getProfessor().getProfessorName() + " 교수님)";
            } else {
                return s.getSubjectName();
            }
        }
        return "과목";
    }
    
    /**
     * 교수 정보를 조회해서 페이지 제목용 문자열 반환
     */
    public String getProfessorInfoForTitle(Long professorId) {
        return postRepository.findProfessorNameById(professorId)
                .map(name -> name + " 교수님")
                .orElse("교수님");
    }
    
    /**
     * 학과 정보를 조회해서 페이지 제목용 문자열 반환 (학교명 + 학과명)
     */
    public String getDepartmentInfoForTitle(Long departmentId) {
        return postRepository.findDepartmentInfoById(departmentId)
                .orElse("학과");
    }
    
    /**
     * 학교 정보를 조회해서 페이지 제목용 문자열 반환
     */
    public String getSchoolInfoForTitle(Long schoolId) {
        return postRepository.findSchoolNameById(schoolId)
                .orElse("학교");
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
            setupBookConnection(post, postDto);
            
            // 3. Subject 연결 (모든 상품 타입에서 가능)
            setupSubjectConnection(post, postDto);
            
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
            
            // 7. 키워드 매칭 및 알림 발송 (비동기)
            keywordAlertService.checkKeywordMatching(savedPost);
            
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
            // 가격 변동 감지를 위한 이전 가격 저장
            Integer oldPrice = post.getPrice();
            
            // 상태 변경 감지를 위한 이전 상태 저장
            Post.PostStatus oldStatus = post.getStatus();
            
            // 기존 SubjectBook 연결 정보 저장 (reference count 관리용)
            Long oldSubjectId = post.getSubject() != null ? post.getSubject().getSubjectId() : null;
            Long oldBookId = post.getBook() != null ? post.getBook().getBookId() : null;
            boolean hadSubjectBookConnection = (oldSubjectId != null && oldBookId != null);
            
            // 1. 기본 정보 업데이트
            postDto.updateEntity(post);
            
            // 2. Book 연결 업데이트
            updateBookConnection(post, postDto);
            
            // 3. Subject 연결 업데이트
            updateSubjectConnection(post, postDto);
            
            // 4. SubjectBook reference count 관리
            updateSubjectBookReferenceCount(post, oldSubjectId, oldBookId, hadSubjectBookConnection);
            
            // 5. 이미지 처리 (삭제, 추가, 순서 업데이트)
            handleImageUpdates(post, newImages, deleteImageIds, imageOrders, newImageOrders);
            
            // Note: SubjectBook reference count는 위에서 이미 처리됨
            
            Post updatedPost = postRepository.save(post);
            
            // 6. 변경사항 감지 및 알림 발송  
            handleChangeNotifications(updatedPost, oldPrice, oldStatus, postId);
            
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
        
        // 게시글 삭제 - JPA의 기본 delete 사용
        // ON DELETE SET NULL이 설정되어 있으면 자동으로 ChatRoom의 post_id가 NULL로 변경됨
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
        
        // BLOCKED 상태 게시글의 상태 변경 방지 (관리자 제외)
        if (post.getStatus() == Post.PostStatus.BLOCKED && status != Post.PostStatus.BLOCKED) {
            throw new ValidationException("블라인드 처리된 게시글의 상태는 관리자만 변경할 수 있습니다.");
        }
        
        // 신고가 많은 게시글의 상태 변경 방지 (BLOCKED로 변경하는 경우 제외)
        if (status != Post.PostStatus.BLOCKED) {
            long uniqueReporters = reportRepository.countUniqueReportersForTarget(
                    Report.ReportType.POST, postId);
            if (uniqueReporters >= 3) {
                throw new ValidationException("다수의 신고가 접수된 게시글의 상태는 관리자만 변경할 수 있습니다.");
            }
        }
        
        // 상태가 실제로 변경되는 경우에만 알림 발송
        Post.PostStatus oldStatus = post.getStatus();
        if (!oldStatus.equals(status)) {
            post.setStatus(status);
            postRepository.save(post);
            
            log.info("게시글 상태 변경: postId={}, oldStatus={}, newStatus={}", postId, oldStatus, status);
            
            // 찜한 사용자들에게 알림 발송 (비동기)
            publishWishlistStatusChangeNotifications(postId, status);
        } else {
            log.info("상태 변경 없음 - updatePostStatus: postId={}, status={}", postId, status);
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
        // 이 메서드는 이미 상태가 변경된 후 호출되므로 post.getStatus() == newStatus가 항상 true
        // 따라서 이 체크는 불필요함 - 호출하는 쪽에서 이미 상태 변경을 확인했음

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
     * 게시글을 찜한 사용자들에게 가격 변동 알림 발송
     */
    private void notifyWishlistUsersOfPriceChange(Post post, Integer oldPrice, Integer newPrice) {
        try {
            // 해당 게시글을 찜한 모든 사용자 조회
            List<Wishlist> wishlists = wishlistRepository.findByPostIdWithUser(post.getPostId());
            
            if (wishlists.isEmpty()) {
                log.debug("게시글을 찜한 사용자가 없음: postId={}", post.getPostId());
                return;
            }
            
            log.info("찜한 게시글 가격 변동 알림 발송 시작: postId={}, userCount={}, {}원 -> {}원", 
                    post.getPostId(), wishlists.size(), oldPrice, newPrice);
            
            // 각 사용자에게 비동기로 알림 발송
            for (Wishlist wishlist : wishlists) {
                try {
                    Long recipientUserId = wishlist.getUser().getUserId();
                    notificationService.createWishlistPriceChangeNotificationAsync(
                            recipientUserId, post.getPostId(), oldPrice, newPrice);
                } catch (Exception e) {
                    // 개별 사용자 알림 실패는 로그만 남기고 계속 진행
                    log.warn("개별 가격 변동 알림 발송 실패: userId={}, postId={}, error={}", 
                            wishlist.getUser().getUserId(), post.getPostId(), e.getMessage());
                }
            }
            
            log.info("찜한 게시글 가격 변동 알림 발송 완료: postId={}", post.getPostId());
            
        } catch (Exception e) {
            // 알림 발송 실패가 게시글 수정을 방해하면 안 됨
            log.warn("가격 변동 알림 발송 중 오류 발생: postId={}, oldPrice={}, newPrice={}, error={}", 
                    post.getPostId(), oldPrice, newPrice, e.getMessage());
        }
    }
    
    /**
     * 조회수 증가 (세션 기반으로 변경됨)
     */
    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
            log.debug("조회수 증가: postId={}, viewCount={}", postId, post.getViewCount());
        }
    }
    
    /**
     * 게시글의 상태 변경 가능 여부 확인
     */
    public boolean canChangePostStatus(Long postId) {
        try {
            Post post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                return false;
            }
            
            // BLOCKED 상태인 경우 변경 불가
            if (post.getStatus() == Post.PostStatus.BLOCKED) {
                return false;
            }
            
            // 3개 이상 신고된 경우 변경 불가
            long uniqueReporters = reportRepository.countUniqueReportersForTarget(
                    Report.ReportType.POST, postId);
            return uniqueReporters < 3;
            
        } catch (Exception e) {
            log.warn("게시글 상태 변경 가능 여부 확인 실패: postId={}", postId, e);
            return false;
        }
    }
    
    /**
     * 조회수 증가 (비동기, 중복 방지) - Deprecated
     * @deprecated 세션 기반 방식으로 변경됨. Controller에서 직접 처리
     */
    @Deprecated
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
     * 전체 게시글 수 조회 (관리자용)
     */
    public long getTotalPostCount() {
        return postRepository.count();
    }
    
    /**
     * 게시글 검색 (관리자용)
     */
    public Page<PostResponseDto> searchPostsForAdmin(String keyword, Post.PostStatus status, Pageable pageable) {
        Page<Post> posts;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (status != null) {
                posts = postRepository.findByTitleContainingOrDescriptionContainingAndStatus(
                        keyword, keyword, status, pageable);
            } else {
                posts = postRepository.findByTitleContainingOrDescriptionContaining(
                        keyword, keyword, pageable);
            }
        } else {
            if (status != null) {
                posts = postRepository.findByStatus(status, pageable);
            } else {
                posts = postRepository.findAll(pageable);
            }
        }
        
        return posts.map(PostResponseDto::from);
    }
    
    /**
     * 게시글 상태별 통계 조회 (관리자용)
     */
    public Map<String, Long> getPostStatusStats() {
        Map<String, Long> stats = new HashMap<>();
        for (Post.PostStatus status : Post.PostStatus.values()) {
            stats.put(status.name(), postRepository.countByStatus(status));
        }
        return stats;
    }
    
    /**
     * 게시글 차단 (관리자용)
     */
    @Transactional
    public void blockPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        if (post.getStatus() == Post.PostStatus.BLOCKED) {
            throw new ValidationException("이미 차단된 게시글입니다.");
        }
        
        post.setStatus(Post.PostStatus.BLOCKED);
        postRepository.save(post);
        
        log.info("게시글 차단 완료: postId={}", postId);
    }
    
    /**
     * 게시글 차단 해제 (관리자용)
     */
    @Transactional
    public void unblockPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        if (post.getStatus() != Post.PostStatus.BLOCKED) {
            throw new ValidationException("차단된 게시글이 아닙니다.");
        }
        
        // 차단 해제 시 판매중 상태로 변경
        post.setStatus(Post.PostStatus.AVAILABLE);
        postRepository.save(post);
        
        log.info("게시글 차단 해제 완료: postId={}", postId);
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
    
    /**
     * 책의 가격 시세 데이터 조회 (차트용)
     */
    public PriceTrendDto.ChartData getBookPriceTrend(Long bookId) {
        log.info("시세 데이터 조회 시작: bookId={}", bookId);
        
        try {
            // 동일한 책의 모든 게시글 조회 (시간순)
            List<Post> posts = postRepository.findByBookIdForPriceTrend(bookId);
            log.info("조회된 게시글 수: {}", posts.size());
            
            if (posts.isEmpty()) {
                log.info("해당 책의 게시글이 없음: bookId={}", bookId);
                return PriceTrendDto.ChartData.builder()
                        .availableAndReserved(List.of())
                        .completed(List.of())
                        .hasData(false)
                        .build();
            }
            
            // 책 정보 추출 (첫 번째 게시글에서)
            Post firstPost = posts.get(0);
            log.info("첫 번째 게시글 정보: postId={}, title={}", firstPost.getPostId(), firstPost.getTitle());
            
            PriceTrendDto.BookInfo bookInfo = PriceTrendDto.BookInfo.builder()
                    .bookId(firstPost.getBook().getBookId())
                    .title(firstPost.getBook().getTitle())
                    .author(firstPost.getBook().getAuthor())
                    .isbn(firstPost.getBook().getIsbn())
                    .build();
            
            // 상태별로 데이터 분리
            List<PriceTrendDto.DataPoint> availableAndReserved = posts.stream()
                    .filter(post -> post.getStatus() == Post.PostStatus.AVAILABLE || 
                                   post.getStatus() == Post.PostStatus.RESERVED)
                    .map(this::convertToDataPoint)
                    .collect(Collectors.toList());
            
            List<PriceTrendDto.DataPoint> completed = posts.stream()
                    .filter(post -> post.getStatus() == Post.PostStatus.COMPLETED)
                    .map(this::convertToDataPoint)
                    .collect(Collectors.toList());
            
            log.info("데이터 분류 완료: available={}, completed={}", 
                    availableAndReserved.size(), completed.size());
            
            return PriceTrendDto.ChartData.builder()
                    .availableAndReserved(availableAndReserved)
                    .completed(completed)
                    .bookInfo(bookInfo)
                    .hasData(!availableAndReserved.isEmpty() || !completed.isEmpty())
                    .build();
                    
        } catch (Exception e) {
            log.error("책 시세 데이터 조회 실패: bookId={}", bookId, e);
            return PriceTrendDto.ChartData.builder()
                    .availableAndReserved(List.of())
                    .completed(List.of())
                    .hasData(false)
                    .build();
        }
    }
    
    /**
     * 게시글 생성시 Book 연결 설정
     */
    private void setupBookConnection(Post post, PostRequestDto postDto) {
        if (postDto.getProductType().isTextbookType()) {
            if (postDto.getBookId() != null) {
                Book book = bookRepository.findById(postDto.getBookId())
                        .orElseThrow(() -> new ValidationException("선택하신 책 정보를 찾을 수 없습니다. 다시 검색해주세요."));
                post.setBook(book);
                log.debug("책 정보 연결: bookId={}, title={}", book.getBookId(), book.getTitle());
            }
            // TODO: 향후 수동 입력 기능 추가 시 else if 분기 추가
        }
    }
    
    /**
     * 게시글 생성시 Subject 연결 설정
     */
    private void setupSubjectConnection(Post post, PostRequestDto postDto) {
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
    }
    
    /**
     * 변경사항 감지 및 알림 발송 처리
     */
    private void handleChangeNotifications(Post updatedPost, Integer oldPrice, Post.PostStatus oldStatus, Long postId) {
        // 가격 변동 감지 및 알림 발송
        Integer newPrice = updatedPost.getPrice();
        if (oldPrice != null && newPrice != null && !oldPrice.equals(newPrice)) {
            log.info("가격 변동 감지: postId={}, {}원 -> {}원", postId, oldPrice, newPrice);
            
            // 가격이 변경된 경우에만 위시리스트 사용자들에게 알림 발송
            notifyWishlistUsersOfPriceChange(updatedPost, oldPrice, newPrice);
        }
        
        // 상태 변경 감지 및 알림 발송
        Post.PostStatus newStatus = updatedPost.getStatus();
        if (oldStatus != null && newStatus != null && !oldStatus.equals(newStatus)) {
            log.info("상태 변경 감지: postId={}, {} -> {}", postId, oldStatus, newStatus);
            
            // 상태가 변경된 경우에만 위시리스트 사용자들에게 알림 발송
            publishWishlistStatusChangeNotifications(updatedPost, newStatus);
        }
    }
    
    /**
     * 이미지 처리 (삭제, 추가, 순서 업데이트)
     */
    private void handleImageUpdates(Post post, List<MultipartFile> newImages, List<Long> deleteImageIds,
                                  List<String> imageOrders, List<String> newImageOrders) throws IOException {
        // 이미지 삭제 처리
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            deleteImages(post, deleteImageIds);
        }
        
        // 새 이미지 추가 (순서 정보와 함께)
        if (newImages != null && !newImages.isEmpty()) {
            processImagesWithOrder(post, newImages, newImageOrders);
        }
        
        // 이미지 순서 업데이트
        if (imageOrders != null && !imageOrders.isEmpty()) {
            updateImageOrders(post, imageOrders);
        }
    }
    
    /**
     * SubjectBook reference count 관리
     */
    private void updateSubjectBookReferenceCount(Post post, Long oldSubjectId, Long oldBookId, 
                                               boolean hadSubjectBookConnection) {
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
    }
    
    /**
     * Subject 연결 업데이트 처리  
     */
    private void updateSubjectConnection(Post post, PostRequestDto postDto) {
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
    }
    
    /**
     * Book 연결 업데이트 처리
     */
    private void updateBookConnection(Post post, PostRequestDto postDto) {
        // 교재 타입인 경우만 처리
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
    }
    
    /**
     * Post를 Chart.js용 DataPoint로 변환
     */
    private PriceTrendDto.DataPoint convertToDataPoint(Post post) {
        String statusText = switch (post.getStatus()) {
            case AVAILABLE -> "판매중";
            case RESERVED -> "예약중";
            case COMPLETED -> "거래완료";
            default -> post.getStatus().name();
        };
        
        return PriceTrendDto.DataPoint.builder()
                .date(post.getCreatedAt().toString()) // ISO 형태로 전달
                .price(post.getPrice())
                .status(statusText)
                .postId(post.getPostId())
                .build();
    }
}