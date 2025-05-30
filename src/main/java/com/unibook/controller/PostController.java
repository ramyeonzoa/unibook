package com.unibook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibook.common.AppConstants;
import com.unibook.common.Messages;
import com.unibook.domain.dto.PostRequestDto;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.User;
import com.unibook.exception.BusinessException;
import com.unibook.exception.ResourceNotFoundException;
import com.unibook.exception.ValidationException;
import com.unibook.security.UserPrincipal;
import com.unibook.service.BookService;
import com.unibook.service.PostService;
import com.unibook.service.UserService;
import com.unibook.service.WishlistService;
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

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final UserService userService;
    private final BookService bookService;
    private final WishlistService wishlistService;
    private final ObjectMapper objectMapper;
    
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
            Model model,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // 페이지 크기 검증
        if (size > 100) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        // sortBy 기본값 설정: 검색어가 있으면 RELEVANCE, 없으면 NEWEST
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = (search != null && !search.trim().isEmpty()) ? "RELEVANCE" : "NEWEST";
        }
        
        // 로깅 추가 (디버깅용)
        log.debug("검색어: '{}', 정렬: '{}'", search, sortBy);
        
        // 정렬 옵션에 따른 Pageable 생성
        Pageable pageable;
        if (search != null && !search.trim().isEmpty()) {
            // 검색어가 있는 경우 Sort 제거 (서비스 레이어에서 처리)
            pageable = PageRequest.of(page, size);
        } else {
            // 검색어가 없는 경우에만 정렬 옵션 적용
            Sort sort = switch (sortBy) {
                case "PRICE_ASC" -> Sort.by("price").ascending();
                case "PRICE_DESC" -> Sort.by("price").descending();
                case "VIEW_COUNT" -> Sort.by("viewCount").descending();
                case "NEWEST" -> Sort.by("createdAt").descending();
                default -> Sort.by("createdAt").descending();
            };
            pageable = PageRequest.of(page, size, sort);
        }
        
        // 로그인한 사용자의 학교 ID
        Long userSchoolId = null;
        if (userPrincipal != null) {
            try {
                userSchoolId = userService.getSchoolIdByUserId(userPrincipal.getUserId());
            } catch (Exception e) {
                // 학교 정보 없음 (정상 케이스)
                log.debug("사용자의 학교 정보 없음: userId={}", userPrincipal.getUserId());
            }
        }
        
        Page<Post> posts = postService.getPostsPage(pageable, search, productType, status, schoolId, sortBy);
        
        // Post 엔티티를 PostResponseDto로 변환하여 Hibernate proxy 문제 방지
        Page<PostResponseDto> postDtos = posts.map(PostResponseDto::listFrom);
        
        model.addAttribute("posts", postDtos);
        model.addAttribute("search", search);
        model.addAttribute("productType", productType);
        model.addAttribute("status", status);
        model.addAttribute("schoolId", schoolId);
        model.addAttribute("userSchoolId", userSchoolId);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        model.addAttribute("sortBy", sortBy);
        
        // 검색어 하이라이팅을 위해 정규화된 키워드 배열 전달
        if (search != null && !search.trim().isEmpty()) {
            String normalized = search.trim().toLowerCase();
            String[] keywords = normalized.split("\\s+");
            model.addAttribute("searchKeywords", keywords);
        }
        
        return "posts/list";
    }
    
    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // 게시글 조회 (연관 데이터 포함)
        Post post = postService.getPostByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        // 비동기로 조회수 증가 (중복 방지 로직 포함)
        if (userPrincipal != null) {
            postService.incrementViewCountAsync(id, userPrincipal.getUserId());
        } else {
            // 비로그인 사용자는 세션 기반으로 처리
            postService.incrementViewCountAsync(id, null);
        }
        
        // 작성자 여부 확인
        boolean isOwner = false;
        boolean canEdit = false;
        if (userPrincipal != null) {
            isOwner = post.getUser().getUserId().equals(userPrincipal.getUserId());
            // 작성자이거나 관리자인 경우 수정 가능
            canEdit = isOwner || userPrincipal.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        }
        
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
        
        model.addAttribute("post", post);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isWishlisted", isWishlisted);
        model.addAttribute("relatedPosts", relatedPosts);
        model.addAttribute("subjectRelatedPosts", subjectRelatedPosts);
        
        return "posts/detail";
    }
    
    /**
     * 게시글 작성 폼
     */
    @GetMapping("/new")
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public String createForm(Model model, @AuthenticationPrincipal UserPrincipal userPrincipal) {
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
            // 새 게시글 작성 시 빈 Post 객체 생성 (템플릿 오류 방지)
            Post emptyPost = Post.builder()
                    .postImages(new ArrayList<>()) // 빈 리스트로 초기화
                    .build();
            model.addAttribute("post", emptyPost);
            model.addAttribute("isEdit", false);
            model.addAttribute("productTypes", Post.ProductType.values());
            model.addAttribute("transactionMethods", Post.TransactionMethod.values());
            model.addAttribute("selectedBookJson", "null"); // 새 게시글 작성 시에는 null
            model.addAttribute("maxImages", MAX_IMAGES);
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
            model.addAttribute("productTypes", Post.ProductType.values());
            model.addAttribute("transactionMethods", Post.TransactionMethod.values());
            model.addAttribute("books", bookService.getAllBooks());
            model.addAttribute("maxImages", MAX_IMAGES);
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
        
        // 작성자 또는 관리자 확인
        boolean isOwner = post.getUser().getUserId().equals(userPrincipal.getUserId());
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("게시글 수정 권한이 없습니다.");
        }
        
        // DTO로 변환
        PostRequestDto postDto = PostRequestDto.from(post);
        
        model.addAttribute("postDto", postDto);
        model.addAttribute("post", post); // 기존 이미지 표시용
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("transactionMethods", Post.TransactionMethod.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        // model.addAttribute("books", bookService.getAllBooks()); // 임시 제거 - 책 검색 API 사용
        model.addAttribute("maxImages", MAX_IMAGES);
        model.addAttribute("isEdit", true);
        
        // 기존 책 정보 JSON으로 전달 (수정 시 표시용)
        String bookJson = "null"; // 기본값
        if (post.getBook() != null) {
            try {
                Map<String, Object> bookData = new HashMap<>();
                bookData.put("bookId", post.getBook().getBookId());
                bookData.put("title", post.getBook().getTitle());
                bookData.put("author", post.getBook().getAuthor());
                bookData.put("publisher", post.getBook().getPublisher());
                bookData.put("isbn", post.getBook().getIsbn());
                bookData.put("imageUrl", post.getBook().getImageUrl()); // 책 표지 이미지 URL 추가
                
                bookJson = objectMapper.writeValueAsString(bookData);
            } catch (Exception e) {
                log.error("책 정보 JSON 변환 실패", e);
                // bookJson은 이미 "null"로 초기화되어 있음
            }
        }
        model.addAttribute("selectedBookJson", bookJson);
        
        // 기존 과목 정보 JSON으로 전달 (수정 시 표시용)
        String subjectJson = "null"; // 기본값
        if (post.getSubject() != null) {
            try {
                Map<String, Object> subjectData = new HashMap<>();
                subjectData.put("subjectId", post.getSubject().getSubjectId());
                subjectData.put("subjectName", post.getSubject().getSubjectName());
                subjectData.put("professorName", post.getSubject().getProfessor().getProfessorName());
                subjectData.put("departmentName", post.getSubject().getProfessor().getDepartment().getDepartmentName());
                subjectData.put("year", post.getTakenYear());
                subjectData.put("semester", post.getTakenSemester() != null ? post.getTakenSemester().name() : null);
                subjectData.put("type", post.getSubject().getType().name());
                
                subjectJson = objectMapper.writeValueAsString(subjectData);
            } catch (Exception e) {
                log.error("과목 정보 JSON 변환 실패", e);
                // subjectJson은 이미 "null"로 초기화되어 있음
            }
        }
        model.addAttribute("selectedSubjectJson", subjectJson);
        
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
        
        // 권한 확인
        boolean isOwner = existingPost.getUser().getUserId().equals(userPrincipal.getUserId());
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("게시글 수정 권한이 없습니다.");
        }
        
        // 기존 이미지 + 새 이미지 개수 검증
        int currentImageCount = existingPost.getPostImages().size();
        int deleteCount = deleteImageIds != null ? deleteImageIds.size() : 0;
        int newImageCount = images != null ? (int) images.stream().filter(img -> !img.isEmpty()).count() : 0;
        int totalImageCount = currentImageCount - deleteCount + newImageCount;
        
        if (totalImageCount > MAX_IMAGES) {
            bindingResult.reject("images", "이미지는 최대 " + MAX_IMAGES + "개까지 업로드 가능합니다.");
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("post", existingPost);
            model.addAttribute("productTypes", Post.ProductType.values());
            model.addAttribute("transactionMethods", Post.TransactionMethod.values());
            model.addAttribute("statuses", Post.PostStatus.values());
            model.addAttribute("maxImages", MAX_IMAGES);
            model.addAttribute("isEdit", true);
            
            // 기존 책 정보 다시 설정
            String bookJson = "null";
            if (existingPost.getBook() != null) {
                try {
                    Map<String, Object> bookData = new HashMap<>();
                    bookData.put("bookId", existingPost.getBook().getBookId());
                    bookData.put("title", existingPost.getBook().getTitle());
                    bookData.put("author", existingPost.getBook().getAuthor());
                    bookData.put("publisher", existingPost.getBook().getPublisher());
                    bookData.put("isbn", existingPost.getBook().getIsbn());
                    bookData.put("imageUrl", existingPost.getBook().getImageUrl()); // 책 표지 이미지 URL 추가
                    bookJson = objectMapper.writeValueAsString(bookData);
                } catch (Exception e) {
                    log.error("책 정보 JSON 변환 실패", e);
                }
            }
            model.addAttribute("selectedBookJson", bookJson);
            
            // 기존 과목 정보 다시 설정
            String subjectJson = "null";
            if (existingPost.getSubject() != null) {
                try {
                    Map<String, Object> subjectData = new HashMap<>();
                    subjectData.put("subjectId", existingPost.getSubject().getSubjectId());
                    subjectData.put("subjectName", existingPost.getSubject().getSubjectName());
                    subjectData.put("professorName", existingPost.getSubject().getProfessor().getProfessorName());
                    subjectData.put("departmentName", existingPost.getSubject().getProfessor().getDepartment().getDepartmentName());
                    subjectData.put("year", existingPost.getTakenYear());
                    subjectData.put("semester", existingPost.getTakenSemester() != null ? existingPost.getTakenSemester().name() : null);
                    subjectData.put("type", existingPost.getSubject().getType().name());
                    subjectJson = objectMapper.writeValueAsString(subjectData);
                } catch (Exception e) {
                    log.error("과목 정보 JSON 변환 실패", e);
                }
            }
            model.addAttribute("selectedSubjectJson", subjectJson);
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
            model.addAttribute("post", existingPost);
            model.addAttribute("productTypes", Post.ProductType.values());
            model.addAttribute("transactionMethods", Post.TransactionMethod.values());
            model.addAttribute("statuses", Post.PostStatus.values());
            model.addAttribute("maxImages", MAX_IMAGES);
            model.addAttribute("isEdit", true);
            
            // 기존 책 정보 다시 설정
            String bookJson = "null";
            if (existingPost.getBook() != null) {
                try {
                    Map<String, Object> bookData = new HashMap<>();
                    bookData.put("bookId", existingPost.getBook().getBookId());
                    bookData.put("title", existingPost.getBook().getTitle());
                    bookData.put("author", existingPost.getBook().getAuthor());
                    bookData.put("publisher", existingPost.getBook().getPublisher());
                    bookData.put("isbn", existingPost.getBook().getIsbn());
                    bookData.put("imageUrl", existingPost.getBook().getImageUrl()); // 책 표지 이미지 URL 추가
                    bookJson = objectMapper.writeValueAsString(bookData);
                } catch (Exception e2) {
                    log.error("책 정보 JSON 변환 실패", e2);
                }
            }
            model.addAttribute("selectedBookJson", bookJson);
            
            // 기존 과목 정보 다시 설정
            String subjectJson = "null";
            if (existingPost.getSubject() != null) {
                try {
                    Map<String, Object> subjectData = new HashMap<>();
                    subjectData.put("subjectId", existingPost.getSubject().getSubjectId());
                    subjectData.put("subjectName", existingPost.getSubject().getSubjectName());
                    subjectData.put("professorName", existingPost.getSubject().getProfessor().getProfessorName());
                    subjectData.put("departmentName", existingPost.getSubject().getProfessor().getDepartment().getDepartmentName());
                    subjectData.put("year", existingPost.getTakenYear());
                    subjectData.put("semester", existingPost.getTakenSemester() != null ? existingPost.getTakenSemester().name() : null);
                    subjectData.put("type", existingPost.getSubject().getType().name());
                    subjectJson = objectMapper.writeValueAsString(subjectData);
                } catch (Exception e2) {
                    log.error("과목 정보 JSON 변환 실패", e2);
                }
            }
            model.addAttribute("selectedSubjectJson", subjectJson);
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
            boolean isOwner = post.getUser().getUserId().equals(userPrincipal.getUserId());
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!isOwner && !isAdmin) {
                throw new AccessDeniedException("게시글 삭제 권한이 없습니다.");
            }
            
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
            if (!post.getUser().getUserId().equals(userPrincipal.getUserId())) {
                throw new AccessDeniedException("게시글 상태 변경 권한이 없습니다.");
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
            Model model,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // 페이지 크기 검증
        if (size > 100) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        // 정렬 옵션 설정
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "NEWEST";
        }
        
        Sort sort = switch (sortBy) {
            case "PRICE_ASC" -> Sort.by("price").ascending();
            case "PRICE_DESC" -> Sort.by("price").descending();
            case "VIEW_COUNT" -> Sort.by("viewCount").descending();
            default -> Sort.by("createdAt").descending();
        };
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 내 게시글 조회
        Page<Post> posts = postService.getPostsByUserId(userPrincipal.getUserId(), pageable);
        Page<PostResponseDto> postDtos = posts.map(PostResponseDto::listFrom);
        
        model.addAttribute("posts", postDtos);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        model.addAttribute("sortBy", sortBy);
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
            Model model,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // 페이지 크기 검증
        if (size > 100) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        // 정렬 옵션 설정
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "NEWEST";
        }
        
        Sort sort = switch (sortBy) {
            case "PRICE_ASC" -> Sort.by("p.price").ascending();
            case "PRICE_DESC" -> Sort.by("p.price").descending();
            case "VIEW_COUNT" -> Sort.by("p.viewCount").descending();
            default -> Sort.by("w.createdAt").descending(); // 찜한 시간 기준
        };
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 찜한 게시글 조회
        Page<Post> posts = wishlistService.getUserWishlistPosts(userPrincipal.getUserId(), pageable);
        Page<PostResponseDto> postDtos = posts.map(PostResponseDto::listFrom);
        
        model.addAttribute("posts", postDtos);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("pageTitle", "찜한 게시글");
        model.addAttribute("pageType", "wishlist"); // 페이지 타입 구분용
        
        return "posts/list";
    }
}