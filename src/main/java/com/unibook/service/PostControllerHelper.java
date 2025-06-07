package com.unibook.service;

import com.unibook.controller.dto.PostSearchRequest;
import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.entity.Post;
import com.unibook.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * PostController의 복잡한 로직을 분리하는 헬퍼 서비스
 * Controller에서 Service로 옮기기 애매한 UI 관련 로직들을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostControllerHelper {
    
    private final PostService postService;
    private final UserService userService;
    private final BookService bookService;
    
    /**
     * 로그인한 사용자의 학교 ID 조회
     * 
     * @param userPrincipal 로그인한 사용자 정보
     * @return 학교 ID 또는 null
     */
    public Long getUserSchoolId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return null;
        }
        
        try {
            return userService.getSchoolIdByUserId(userPrincipal.getUserId());
        } catch (Exception e) {
            // 학교 정보 없음 (정상 케이스)
            log.debug("사용자의 학교 정보 없음: userId={}", userPrincipal.getUserId());
            return null;
        }
    }
    
    /**
     * 게시글 목록 조회 및 DTO 변환
     * 
     * @param request 검색 요청
     * @param pageable 페이징 정보
     * @return 변환된 게시글 페이지
     */
    public Page<PostResponseDto> getPostsWithDto(PostSearchRequest request, Pageable pageable) {
        Page<Post> posts = postService.getPostsPage(
                pageable, 
                request.getSearch(), 
                request.getProductType(), 
                request.getStatus(), 
                request.getSchoolId(), 
                request.getSortBy(),
                request.getMinPrice(), 
                request.getMaxPrice(), 
                request.getSubjectId(), 
                request.getProfessorId(), 
                request.getBookTitle(),
                request.getBookId()
        );
        
        // Post 엔티티를 PostResponseDto로 변환하여 Hibernate proxy 문제 방지
        return posts.map(PostResponseDto::listFrom);
    }
    
    /**
     * Model에 검색 관련 데이터 설정
     * 
     * @param model Model 객체
     * @param request 검색 요청
     * @param posts 게시글 페이지
     * @param userSchoolId 사용자 학교 ID
     */
    public void enrichModelWithSearchData(Model model, PostSearchRequest request, 
                                        Page<PostResponseDto> posts, Long userSchoolId) {
        // 게시글 데이터
        model.addAttribute("posts", posts);
        
        // 검색 조건들
        model.addAttribute("search", request.getSearch());
        model.addAttribute("productType", request.getProductType());
        model.addAttribute("status", request.getStatus());
        model.addAttribute("schoolId", request.getSchoolId());
        model.addAttribute("sortBy", request.getSortBy());
        model.addAttribute("minPrice", request.getMinPrice());
        model.addAttribute("maxPrice", request.getMaxPrice());
        model.addAttribute("subjectId", request.getSubjectId());
        model.addAttribute("professorId", request.getProfessorId());
        model.addAttribute("bookTitle", request.getBookTitle());
        
        // 사용자 및 선택 옵션
        model.addAttribute("userSchoolId", userSchoolId);
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        
        // 검색어 하이라이팅용 키워드
        if (request.hasSearchKeyword()) {
            model.addAttribute("searchKeywords", request.getSearchKeywords());
        }
    }
    
    /**
     * 페이지 제목 및 설명 설정
     * 
     * @param model Model 객체
     * @param request 검색 요청
     */
    public void setPageTitleAndDescription(Model model, PostSearchRequest request) {
        String pageTitle = "게시글 둘러보기";
        String pageDescription = "다양한 교재와 학습 자료를 찾아보세요";
        
        if (request.getSubjectId() != null) {
            // 과목 ID로 검색하는 경우
            try {
                String subjectInfo = postService.getSubjectInfoForTitle(request.getSubjectId());
                pageTitle = subjectInfo + " 관련 게시글";
                pageDescription = "해당 과목의 교재와 학습 자료를 확인하세요";
            } catch (Exception e) {
                log.warn("과목 정보 조회 실패: subjectId={}", request.getSubjectId(), e);
            }
        } else if (request.getProfessorId() != null) {
            // 교수 ID로 검색하는 경우
            try {
                String professorInfo = postService.getProfessorInfoForTitle(request.getProfessorId());
                pageTitle = professorInfo + " 관련 게시글";
                pageDescription = "해당 교수님의 모든 과목 교재와 학습 자료를 확인하세요";
            } catch (Exception e) {
                log.warn("교수 정보 조회 실패: professorId={}", request.getProfessorId(), e);
            }
        } else if (request.getBookId() != null) {
            // 책 ID로 검색하는 경우
            try {
                String bookTitle = bookService.getBookTitleById(request.getBookId());
                if (bookTitle != null) {
                    pageTitle = "'" + bookTitle + "' 교재 게시글";
                    pageDescription = "해당 교재의 판매 및 구매 게시글을 확인하세요";
                } else {
                    pageTitle = "교재별 게시글";
                    pageDescription = "선택하신 교재의 게시글을 확인하세요";
                }
            } catch (Exception e) {
                log.warn("책 정보 조회 실패: bookId={}", request.getBookId(), e);
                pageTitle = "교재별 게시글";
                pageDescription = "선택하신 교재의 게시글을 확인하세요";
            }
        } else if (request.getBookTitle() != null && !request.getBookTitle().trim().isEmpty()) {
            pageTitle = "'" + request.getBookTitle() + "' 검색 결과";
            pageDescription = "해당 책과 관련된 게시글을 확인하세요";
        } else if (request.hasSearchKeyword()) {
            pageTitle = "'" + request.getSearch() + "' 검색 결과";
            pageDescription = "검색어와 관련된 게시글을 확인하세요";
        }
        
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
    }
}