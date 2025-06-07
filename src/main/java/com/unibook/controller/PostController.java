package com.unibook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibook.common.AppConstants;
import com.unibook.common.Messages;
import com.unibook.domain.dto.PostRequestDto;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.dto.PriceTrendDto;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.exception.BusinessException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.security.UserPrincipal;
import com.unibook.service.AuthorizationService;
import com.unibook.service.BookService;
import com.unibook.service.PostControllerHelper;
import com.unibook.service.PostService;
import com.unibook.service.UserService;
import com.unibook.service.WishlistService;
import com.unibook.util.PostFormDataBuilder;
import com.unibook.controller.dto.PostSearchRequest;
import com.unibook.repository.ReportRepository;
import com.unibook.domain.entity.Report;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Slf4j
@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final UserService userService;
    private final BookService bookService;
    private final WishlistService wishlistService;
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final AuthorizationService authorizationService;
    private final PostFormDataBuilder postFormDataBuilder;
    private final PostControllerHelper postControllerHelper;
    
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_IMAGES = 5;
    
    /**
     * 게시글 목록 조회
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Post.ProductType productType,
            @RequestParam(required = false) Post.PostStatus status,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long professorId,
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long userId,
            Model model,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // userId가 있으면 특정 사용자의 게시글만 조회 (관리자 전용)
        if (userId != null) {
            // 관리자 권한 체크
            if (userPrincipal == null || !userPrincipal.getRole().equals(User.UserRole.ADMIN)) {
                throw new AccessDeniedException("관리자만 접근 가능한 기능입니다.");
            }
            return handleUserSpecificPosts(userId, page, size, sortBy, minPrice, maxPrice, model);
        }
        
        // 검색 요청 처리 및 결과 설정
        PostSearchRequest request = PostSearchRequest.from(page, size, search, productType, status, 
                                                          schoolId, sortBy, minPrice, maxPrice, 
                                                          subjectId, professorId, bookTitle, bookId);
        request.normalizeForController();
        log.debug("검색어: '{}', 정렬: '{}'", request.getSearch(), request.getSortBy());
        
        // 헬퍼를 통한 데이터 조회 및 모델 설정
        Long userSchoolId = postControllerHelper.getUserSchoolId(userPrincipal);
        Page<PostResponseDto> postDtos = postControllerHelper.getPostsWithDto(request, request.toPageable());
        postControllerHelper.enrichModelWithSearchData(model, request, postDtos, userSchoolId);
        postControllerHelper.setPageTitleAndDescription(model, request);
        
        return "posts/list";
    }
    
    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        HttpSession session) {
        
        try {
            // 게시글 조회 (연관 데이터 포함)
            Post post = postService.getPostByIdWithDetails(id)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
            
            // BLOCKED 상태 게시글 접근 차단 (작성자/관리자 제외)
            if (post.getStatus() == Post.PostStatus.BLOCKED) {
                if (!authorizationService.isOwnerOrAdmin(post, userPrincipal)) {
                    log.warn("BLOCKED 게시글 비인가 접근 시도: postId={}, userId={}", 
                            id, userPrincipal != null ? userPrincipal.getUserId() : "anonymous");
                    model.addAttribute("blocked", true);
                    return "error/post-deleted";
                }
            }
            
            // 세션 기반 조회수 증가 로직
            Set<Long> viewedPosts = (Set<Long>) session.getAttribute("viewedPosts");
            if (viewedPosts == null) {
                viewedPosts = new HashSet<>();
            }
            
            // 이미 본 게시글이 아니면 조회수 증가
            if (!viewedPosts.contains(id)) {
                postService.incrementViewCount(id);
                viewedPosts.add(id);
                session.setAttribute("viewedPosts", viewedPosts);
            }
            
            // 작성자 여부 확인
            AuthorizationService.AuthorizationInfo authInfo = authorizationService.calculateDetailPageAuth(post, userPrincipal);
            boolean isOwner = authInfo.isOwner();
            boolean canEdit = authInfo.canEdit();
            
            // 같은 책의 다른 게시글 (현재 게시글 제외)
            List<Post> relatedPosts = post.getBook() != null ? 
                    postService.getRelatedPosts(post.getBook().getBookId(), id, 4) : 
                    List.of();
            
            // 같은 과목의 다른 게시글 (현재 게시글 제외)
            List<Post> subjectRelatedPosts = post.getSubject() != null ?
                    postService.getRelatedPostsBySubject(post.getSubject().getSubjectId(), id, 4) :
                    List.of();
            
            // 찜 상태 확인
            boolean isWishlisted = false;
            if (userPrincipal != null && !isOwner) {
                isWishlisted = wishlistService.isWishlisted(userPrincipal.getUserId(), id);
            }
            
            // 상태 변경 가능 여부 확인
            boolean canChangeStatus = isOwner && postService.canChangePostStatus(id);
            
            model.addAttribute("post", post);
            model.addAttribute("isOwner", isOwner);
            model.addAttribute("canEdit", canEdit);
            model.addAttribute("canChangeStatus", canChangeStatus);
            model.addAttribute("isWishlisted", isWishlisted);
            model.addAttribute("relatedPosts", relatedPosts);
            model.addAttribute("subjectRelatedPosts", subjectRelatedPosts);
            
            return "posts/detail";
        
        } catch (ResourceNotFoundException e) {
            // 게시글을 찾을 수 없는 경우 삭제된 게시글 페이지로 이동
            log.info("삭제된 게시글 접근 시도: postId={}", id);
            return "error/post-deleted";
        }
    }
    
    /**
     * 게시글 작성 폼
     */
    @GetMapping("/new")
    public String createForm(Model model, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        // 로그인 체크
        if (userPrincipal == null) {
            return "redirect:/login?returnUrl=/posts/new";
        }
        
        // 이메일 인증 확인
        if (!userPrincipal.isVerified()) {
            log.warn("이메일 미인증 사용자의 게시글 작성 폼 접근 시도: {}", userPrincipal.getEmail());
            return "redirect:/verification-required?returnUrl=/posts/new";
        }
        PostRequestDto postDto = new PostRequestDto();
        
        // 새 게시글 작성 시 빈 Post 객체 생성 (템플릿 오류 방지)
        Post emptyPost = Post.builder()
                .postImages(new ArrayList<>()) // 빈 리스트로 초기화
                .build();
        
        model.addAttribute("postDto", postDto);
        model.addAttribute("post", emptyPost);
        model.addAttribute("isEdit", false);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("transactionMethods", Post.TransactionMethod.values());
        model.addAttribute("selectedBookJson", "null"); // 새 게시글 작성 시에는 null
        model.addAttribute("selectedSubjectJson", "null"); // 새 게시글 작성 시에는 null
        model.addAttribute("maxImages", MAX_IMAGES);
        
        return "posts/form";
    }
    
    /**
     * 게시글 작성 처리
     */
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public String create(@Valid @ModelAttribute("postDto") PostRequestDto postDto,
                        BindingResult bindingResult,
                        @RequestParam(value = "images", required = false) List<MultipartFile> images,
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        // 이메일 인증 확인
        if (!userPrincipal.isVerified()) {
            log.warn("이메일 미인증 사용자의 게시글 작성 시도: {}", userPrincipal.getEmail());
            return "redirect:/verification-required?returnUrl=/posts/new";
        }
        
        // 이미지 개수 검증
        if (images != null && images.size() > MAX_IMAGES) {
            bindingResult.reject("images", "이미지는 최대 " + MAX_IMAGES + "개까지 업로드 가능합니다.");
        }
        
        if (bindingResult.hasErrors()) {
            setupNewPostFormError(model);
            return "posts/form";
        }
        
        try {
            // 작성자 설정
            User user = userService.findById(userPrincipal.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            
            // 게시글 저장 (트랜잭션 내에서 이미지도 함께 처리)
            Post savedPost = postService.createPost(postDto, user, images);
            
            redirectAttributes.addFlashAttribute("successMessage", "게시글이 작성되었습니다.");
            return "redirect:/posts/" + savedPost.getPostId();
            
        } catch (ValidationException e) {
            log.error("게시글 작성 검증 실패: {}", e.getMessage());
            bindingResult.reject("global", e.getMessage());
            setupNewPostFormError(model);
            return "posts/form";
            
        } catch (Exception e) {
            log.error("게시글 작성 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 작성 중 오류가 발생했습니다.");
            return "redirect:/posts/new";
        }
    }
    
    /**
     * 게시글 수정 폼
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editForm(@PathVariable Long id, Model model,
                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Post post = postService.getPostByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        // 게시글 수정 권한 확인 (BLOCKED 게시글은 관리자만 수정 가능)
        authorizationService.requireCanEdit(post, userPrincipal, "게시글 수정 권한이 없습니다.");
        
        // DTO로 변환
        PostRequestDto postDto = PostRequestDto.from(post);
        
        model.addAttribute("postDto", postDto);
        model.addAttribute("post", post); // 기존 이미지 표시용
        
        // 폼 관련 공통 속성 설정 (리팩터링된 부분)
        postFormDataBuilder.addFormAttributes(model, true);
        
        // JSON 데이터 설정 (리팩터링된 부분)
        model.addAttribute("selectedBookJson", postFormDataBuilder.buildBookJson(post.getBook()));
        model.addAttribute("selectedSubjectJson", postFormDataBuilder.buildSubjectJson(
                post.getSubject(), post.getTakenYear(), post.getTakenSemester()));
        
        return "posts/form";
    }
    
    /**
     * 게시글 수정 처리
     */
    @PostMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("postDto") PostRequestDto postDto,
                        BindingResult bindingResult,
                        @RequestParam(value = "images", required = false) List<MultipartFile> images,
                        @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                        @RequestParam(value = "imageOrders", required = false) List<String> imageOrders,
                        @RequestParam(value = "newImageOrders", required = false) List<String> newImageOrders,
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        Post existingPost = postService.getPostByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        // 게시글 수정 권한 확인 (BLOCKED 게시글은 관리자만 수정 가능)
        authorizationService.requireCanEdit(existingPost, userPrincipal, "게시글 수정 권한이 없습니다.");
        
        // 기존 이미지 + 새 이미지 개수 검증
        int currentImageCount = existingPost.getPostImages().size();
        int deleteCount = deleteImageIds != null ? deleteImageIds.size() : 0;
        int newImageCount = images != null ? (int) images.stream().filter(img -> !img.isEmpty()).count() : 0;
        int totalImageCount = currentImageCount - deleteCount + newImageCount;
        
        if (totalImageCount > MAX_IMAGES) {
            bindingResult.reject("images", "이미지는 최대 " + MAX_IMAGES + "개까지 업로드 가능합니다.");
        }
        
        if (bindingResult.hasErrors()) {
            // 에러 처리용 폼 데이터 설정 (리팩터링된 부분)
            postFormDataBuilder.addFormAttributesForError(model, existingPost, true);
            return "posts/form";
        }
        
        try {
            // 게시글 수정 (트랜잭션 내에서 처리)
            Post updatedPost = postService.updatePost(id, postDto, images, deleteImageIds, imageOrders, newImageOrders);
            
            redirectAttributes.addFlashAttribute("successMessage", "게시글이 수정되었습니다.");
            return "redirect:/posts/" + updatedPost.getPostId();
            
        } catch (ValidationException e) {
            log.error("게시글 수정 검증 실패: {}", e.getMessage());
            bindingResult.reject("global", e.getMessage());
            // 에러 처리용 폼 데이터 설정 (리팩터링된 부분)
            postFormDataBuilder.addFormAttributesForError(model, existingPost, true);
            return "posts/form";
            
        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 수정 중 오류가 발생했습니다.");
            return "redirect:/posts/" + id + "/edit";
        }
    }
    
    /**
     * 게시글 삭제
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String delete(@PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.getPostById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
            
            // 권한 확인
            authorizationService.requireOwnerOrAdmin(post, userPrincipal, "게시글 삭제 권한이 없습니다.");
            
            // 삭제 처리 (이미지 파일도 함께 삭제)
            postService.deletePost(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "게시글이 삭제되었습니다.");
            return "redirect:/posts";
            
        } catch (AccessDeniedException e) {
            log.error("게시글 삭제 권한 없음: postId={}, userId={}", id, userPrincipal.getUserId());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/posts/" + id;
            
        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생: postId={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 삭제 중 오류가 발생했습니다.");
            return "redirect:/posts/" + id;
        }
    }
    
    /**
     * 게시글 상태 변경 (AJAX)
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public Map<String, Object> updateStatus(@PathVariable Long id,
                                           @RequestParam Post.PostStatus status,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Post post = postService.getPostById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
            
            // 작성자 확인
            authorizationService.requireOwner(post, userPrincipal, "게시글 상태 변경 권한이 없습니다.");
            
            // BLOCKED 상태 게시글의 상태 변경 차단
            if (post.getStatus() == Post.PostStatus.BLOCKED) {
                throw new ValidationException("신고로 인해 블라인드 처리된 게시글은 상태를 변경할 수 없습니다.");
            }
            
            // 3개 이상 신고된 게시글의 상태 변경 차단 (BLOCKED가 아니어도)
            long uniqueReporters = reportRepository.countUniqueReportersForTarget(
                    Report.ReportType.POST, id);
            if (uniqueReporters >= 3) {
                throw new ValidationException("다수의 신고가 접수된 게시글은 상태를 변경할 수 없습니다. 관리자에게 문의해주세요.");
            }
            
            postService.updatePostStatus(id, status);
            
            response.put("success", true);
            response.put("message", "상태가 변경되었습니다.");
            response.put("newStatus", status.name());
            
        } catch (Exception e) {
            log.error("게시글 상태 변경 실패: postId={}, status={}", id, status, e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 내 게시글 목록
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public String myPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            Model model,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        if (userPrincipal == null) {
            log.warn("My posts 접근 시 UserPrincipal이 null입니다. 로그인 페이지로 리다이렉트합니다.");
            return "redirect:/login?returnUrl=/posts/my";
        }
        
        Pageable pageable = createUserPostsPageable(page, size, sortBy);
        
        // 내 게시글 조회
        Page<Post> posts = postService.getPostsByUserId(userPrincipal.getUserId(), pageable, minPrice, maxPrice);
        Page<PostResponseDto> postDtos = posts.map(PostResponseDto::listFrom);
        
        model.addAttribute("posts", postDtos);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("pageTitle", "내 게시글");
        model.addAttribute("pageType", "my"); // 페이지 타입 구분용
        
        return "posts/list";
    }
    
    /**
     * 찜한 게시글 목록
     */
    @GetMapping("/wishlist")
    @PreAuthorize("isAuthenticated()")
    public String wishlistPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            Model model,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        if (userPrincipal == null) {
            log.warn("Wishlist 접근 시 UserPrincipal이 null입니다. 로그인 페이지로 리다이렉트합니다.");
            return "redirect:/login?returnUrl=/posts/wishlist";
        }
        
        Pageable pageable = createWishlistPageable(page, size, sortBy);
        
        // 찜한 게시글 조회
        Page<Post> posts = wishlistService.getUserWishlistPosts(userPrincipal.getUserId(), pageable, minPrice, maxPrice);
        Page<PostResponseDto> postDtos = posts.map(PostResponseDto::listFrom);
        
        model.addAttribute("posts", postDtos);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("pageTitle", "찜한 게시글");
        model.addAttribute("pageType", "wishlist"); // 페이지 타입 구분용
        
        return "posts/list";
    }
    
    /**
     * 게시글 차단 (관리자용)
     */
    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, Object> blockPost(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            postService.blockPost(id);
            response.put("success", true);
            response.put("message", "게시글이 차단되었습니다.");
        } catch (Exception e) {
            log.error("게시글 차단 실패: postId={}", id, e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 게시글 차단 해제 (관리자용)
     */
    @PutMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, Object> unblockPost(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            postService.unblockPost(id);
            response.put("success", true);
            response.put("message", "게시글 차단이 해제되었습니다.");
        } catch (Exception e) {
            log.error("게시글 차단 해제 실패: postId={}", id, e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 책의 가격 시세 데이터 조회 (차트용 API)
     */
    @GetMapping(value = "/price-trend/{bookId}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<PriceTrendDto.ChartData> getBookPriceTrend(@PathVariable Long bookId) {
        try {
            log.info("책 시세 데이터 조회 요청: bookId={}", bookId);
            PriceTrendDto.ChartData result = postService.getBookPriceTrend(bookId);
            log.info("시세 데이터 조회 결과: hasData={}, available={}, completed={}", 
                    result.isHasData(), 
                    result.getAvailableAndReserved().size(), 
                    result.getCompleted().size());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result);
        } catch (Exception e) {
            log.error("책 시세 데이터 조회 실패: bookId={}", bookId, e);
            PriceTrendDto.ChartData errorResult = PriceTrendDto.ChartData.builder()
                    .availableAndReserved(List.of())
                    .completed(List.of())
                    .hasData(false)
                    .build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResult);
        }
    }
    
    /**
     * 특정 사용자의 게시글 목록 조회 (관리자용)
     */
    private String handleUserSpecificPosts(Long userId, int page, int size, 
                                         String sortBy, Integer minPrice, Integer maxPrice, Model model) {
        Pageable pageable = createUserPostsPageable(page, size, sortBy);
        
        // 특정 사용자의 게시글 조회 (가격 필터 포함)
        Page<Post> posts = postService.getPostsByUserId(userId, pageable, minPrice, maxPrice);
        Page<PostResponseDto> postDtos = posts.map(PostResponseDto::listFrom);
        
        // 사용자 정보 조회
        User targetUser = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        model.addAttribute("posts", postDtos);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("pageTitle", targetUser.getName() + "님의 게시글");
        model.addAttribute("pageType", "user-specific");
        model.addAttribute("targetUserId", userId);
        model.addAttribute("targetUserName", targetUser.getName());
        
        return "posts/list";
    }
    
    /**
     * 사용자 게시글용 Pageable 생성 (공통 로직)
     */
    private Pageable createUserPostsPageable(int page, int size, String sortBy) {
        if (size > 100) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "NEWEST";
        }
        
        Sort sort = switch (sortBy) {
            case "PRICE_ASC" -> Sort.by("price").ascending();
            case "PRICE_DESC" -> Sort.by("price").descending();
            case "VIEW_COUNT" -> Sort.by("viewCount").descending();
            default -> Sort.by("createdAt").descending();
        };
        
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * 찜 목록용 Pageable 생성 (특수한 정렬 옵션 포함)
     */
    private Pageable createWishlistPageable(int page, int size, String sortBy) {
        if (size > 100) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "NEWEST";
        }
        
        Sort sort = switch (sortBy) {
            case "PRICE_ASC" -> Sort.by("p.price").ascending();
            case "PRICE_DESC" -> Sort.by("p.price").descending();
            case "VIEW_COUNT" -> Sort.by("p.viewCount").descending();
            default -> Sort.by("w.createdAt").descending(); // 찜한 시간 기준
        };
        
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * 새 게시글 작성 폼 에러 처리용 모델 설정
     */
    private void setupNewPostFormError(Model model) {
        Post emptyPost = Post.builder()
                .postImages(new ArrayList<>())
                .build();
        model.addAttribute("post", emptyPost);
        
        // PostFormDataBuilder 활용으로 일관성 확보
        postFormDataBuilder.addFormAttributes(model, false);
        model.addAttribute("selectedBookJson", "null");
    }
}