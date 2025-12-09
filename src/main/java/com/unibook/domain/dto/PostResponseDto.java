package com.unibook.domain.dto;

import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.PostImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    
    private Long postId;
    private String title;
    private Integer price;
    private Post.ProductType productType;
    private Post.PostStatus status;
    private Integer viewCount;
    private Integer wishlistCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 작성자 정보
    private UserDto user;
    
    // 책 정보 (productType이 TEXTBOOK인 경우)
    private BookDto book;
    
    // 과목 정보
    private SubjectDto subject;
    
    // 이미지 정보
    private List<ImageDto> images;
    
    // 검색 하이라이팅용 Flat 필드
    private Long subjectId;
    private String subjectName;
    private Long professorId;
    private String professorName;
    private String bookTitle;
    private String bookAuthor;

    // 추천 소스 라벨 (예: personalized, popular, fresh, explore)
    private String source;
    
    // User 정보를 담는 내부 DTO
    @Data
    @Builder
    public static class UserDto {
        private Long userId;
        private String name;
        private String email;
        private Long schoolId;
        private String schoolName;
        private String departmentName;
    }
    
    // Book 정보를 담는 내부 DTO
    @Data
    @Builder
    public static class BookDto {
        private Long bookId;
        private String title;
        private String author;
        private String publisher;
        private String isbn;
        private Integer publicationYear;
        private Integer originalPrice;
    }
    
    // Subject 정보를 담는 내부 DTO
    @Data
    @Builder
    public static class SubjectDto {
        private Long subjectId;
        private String subjectName;
        private String professorName;
        private String departmentName;
        private String type;  // MAJOR, GENERAL
    }
    
    // Image 정보를 담는 내부 DTO
    @Data
    @Builder
    public static class ImageDto {
        private Long imageId;
        private String imagePath;
        private Integer imageOrder;
        private boolean isMain; // imageOrder == 0
    }
    
    // Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static PostResponseDto from(Post post) {
        PostResponseDtoBuilder builder = PostResponseDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .price(post.getPrice())
                .productType(post.getProductType())
                .status(post.getStatus())
                .viewCount(post.getViewCount() != null ? post.getViewCount() : 0)
                .wishlistCount(post.getWishlistCount() != null ? post.getWishlistCount() : 0)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());
        
        // User 정보 설정
        if (post.getUser() != null) {
            UserDto.UserDtoBuilder userBuilder = UserDto.builder()
                    .userId(post.getUser().getUserId())
                    .name(post.getUser().getName())
                    .email(post.getUser().getEmail());
            
            if (post.getUser().getDepartment() != null) {
                userBuilder.departmentName(post.getUser().getDepartment().getDepartmentName());
                
                if (post.getUser().getDepartment().getSchool() != null) {
                    userBuilder.schoolId(post.getUser().getDepartment().getSchool().getSchoolId());
                    userBuilder.schoolName(post.getUser().getDepartment().getSchool().getSchoolName());
                }
            }
            
            builder.user(userBuilder.build());
        }
        
        // Book 정보 설정 (교재 타입인 경우만 - TEXTBOOK, CERTBOOK 포함)
        if (post.getProductType().isTextbookType() && post.getBook() != null) {
            BookDto bookDto = BookDto.builder()
                    .bookId(post.getBook().getBookId())
                    .title(post.getBook().getTitle())
                    .author(post.getBook().getAuthor())
                    .publisher(post.getBook().getPublisher())
                    .isbn(post.getBook().getIsbn())
                    .publicationYear(post.getBook().getPublicationYear())
                    .originalPrice(post.getBook().getOriginalPrice())
                    .build();
            builder.book(bookDto);
            
            // 검색용 Flat 필드 설정
            builder.bookTitle(post.getBook().getTitle());
            builder.bookAuthor(post.getBook().getAuthor());
        }
        
        // Subject 정보 설정
        if (post.getSubject() != null) {
            SubjectDto.SubjectDtoBuilder subjectBuilder = SubjectDto.builder()
                    .subjectId(post.getSubject().getSubjectId())
                    .subjectName(post.getSubject().getSubjectName())
                    .type(post.getSubject().getType().name());
            
            if (post.getSubject().getProfessor() != null) {
                subjectBuilder.professorName(post.getSubject().getProfessor().getProfessorName());
                
                if (post.getSubject().getProfessor().getDepartment() != null) {
                    subjectBuilder.departmentName(post.getSubject().getProfessor().getDepartment().getDepartmentName());
                }
                
                // 검색용 Flat 필드 설정
                builder.professorId(post.getSubject().getProfessor().getProfessorId());
                builder.professorName(post.getSubject().getProfessor().getProfessorName());
            }
            
            builder.subject(subjectBuilder.build());
            
            // 검색용 Flat 필드 설정
            builder.subjectId(post.getSubject().getSubjectId());
            builder.subjectName(post.getSubject().getSubjectName());
        }
        
        // Image 정보 설정
        if (post.getPostImages() != null && !post.getPostImages().isEmpty()) {
            List<ImageDto> imageDtos = post.getPostImages().stream()
                    .map(image -> ImageDto.builder()
                            .imageId(image.getPostImageId())
                            .imagePath(image.getImageUrl())
                            .imageOrder(image.getImageOrder())
                            .isMain(image.getImageOrder() == 0)
                            .build())
                    .collect(Collectors.toList());
            builder.images(imageDtos);
        }
        
        return builder.build();
    }
    
    // 목록용 간단한 DTO 변환 (이미지, 상세정보 제외)
    public static PostResponseDto listFrom(Post post) {
        PostResponseDtoBuilder builder = PostResponseDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .price(post.getPrice())
                .productType(post.getProductType())
                .status(post.getStatus())
                .viewCount(post.getViewCount() != null ? post.getViewCount() : 0)
                .wishlistCount(post.getWishlistCount() != null ? post.getWishlistCount() : 0)
                .createdAt(post.getCreatedAt());
        
        // 사용자 기본 정보만
        if (post.getUser() != null) {
            Long schoolId = null;
            String schoolName = null;
            if (post.getUser().getDepartment() != null && 
                post.getUser().getDepartment().getSchool() != null) {
                schoolId = post.getUser().getDepartment().getSchool().getSchoolId();
                schoolName = post.getUser().getDepartment().getSchool().getSchoolName();
            }
            
            builder.user(UserDto.builder()
                    .userId(post.getUser().getUserId())
                    .name(post.getUser().getName())
                    .schoolId(schoolId)
                    .schoolName(schoolName)
                    .build());
        }
        
        // Subject 정보 설정 (목록용이므로 간략하게)
        if (post.getSubject() != null) {
            SubjectDto.SubjectDtoBuilder subjectBuilder = SubjectDto.builder()
                    .subjectId(post.getSubject().getSubjectId())
                    .subjectName(post.getSubject().getSubjectName());
            
            if (post.getSubject().getProfessor() != null) {
                subjectBuilder.professorName(post.getSubject().getProfessor().getProfessorName());
                builder.professorId(post.getSubject().getProfessor().getProfessorId());
                builder.professorName(post.getSubject().getProfessor().getProfessorName());
            }
            
            builder.subject(subjectBuilder.build());
            builder.subjectId(post.getSubject().getSubjectId());
            builder.subjectName(post.getSubject().getSubjectName());
        }
        
        // Book 정보 설정 (검색용 Flat 필드만)
        if (post.getProductType().isTextbookType() && post.getBook() != null) {
            builder.bookTitle(post.getBook().getTitle());
            builder.bookAuthor(post.getBook().getAuthor());
        }
        
        // 대표 이미지만
        if (post.getPostImages() != null && !post.getPostImages().isEmpty()) {
            PostImage mainImage = post.getPostImages().stream()
                    .filter(img -> img.getImageOrder() == 0)
                    .findFirst()
                    .orElse(post.getPostImages().get(0));
            
            builder.images(List.of(ImageDto.builder()
                    .imageId(mainImage.getPostImageId())
                    .imagePath(mainImage.getImageUrl())
                    .imageOrder(mainImage.getImageOrder())
                    .isMain(true)
                    .build()));
        }
        
        return builder.build();
    }
}
