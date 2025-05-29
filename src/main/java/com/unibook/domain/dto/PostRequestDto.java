package com.unibook.domain.dto;

import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.Subject;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {
    
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다")
    private String title;
    
    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;
    
    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다")
    private String description;
    
    @NotNull(message = "상품 유형은 필수입니다")
    private Post.ProductType productType;
    
    private Long bookId; // 책 선택 (교재인 경우)
    
    private boolean removeBook; // 책 연결 해제 플래그 (수정 시 사용)
    
    private Long subjectId; // 과목 선택
    private Integer takenYear; // 수강 연도 (UI에서 입력받을 값)
    private Subject.Semester takenSemester; // 수강 학기 (UI에서 입력받을 값)
    
    private boolean removeSubject; // 과목 연결 해제 플래그 (수정 시 사용)
    
    private Post.TransactionMethod transactionMethod;
    
    @Size(max = 100, message = "거래 장소는 100자 이하여야 합니다")
    private String campusLocation;
    
    private Post.PostStatus status; // 수정 시에만 사용
    
    /**
     * Subject 검증: 과목 선택 시 연도와 학기는 필수
     */
    @AssertTrue(message = "과목 선택 시 연도와 학기는 필수입니다")
    public boolean isSubjectDataValid() {
        if (subjectId == null) return true; // 과목 미선택 시 유효
        return takenYear != null && takenSemester != null;
    }
    
    /**
     * Entity로부터 DTO 생성
     */
    public static PostRequestDto from(Post post) {
        return PostRequestDto.builder()
                .title(post.getTitle())
                .price(post.getPrice())
                .description(post.getDescription())
                .productType(post.getProductType())
                .bookId(post.getBook() != null ? post.getBook().getBookId() : null)
                .subjectId(post.getSubject() != null ? post.getSubject().getSubjectId() : null)
                .takenYear(post.getTakenYear())
                .takenSemester(post.getTakenSemester())
                .transactionMethod(post.getTransactionMethod())
                .campusLocation(post.getCampusLocation())
                .status(post.getStatus())
                .build();
    }
    
    /**
     * DTO를 Entity로 변환 (신규 생성용)
     */
    public Post toEntity() {
        return Post.builder()
                .title(this.title)
                .price(this.price)
                .description(this.description)
                .productType(this.productType)
                .transactionMethod(this.transactionMethod)
                .campusLocation(this.campusLocation)
                .status(Post.PostStatus.AVAILABLE) // 신규 게시글은 항상 AVAILABLE
                .build();
    }
    
    /**
     * 기존 Entity 업데이트
     */
    public void updateEntity(Post post) {
        post.setTitle(this.title);
        post.setPrice(this.price);
        post.setDescription(this.description);
        post.setProductType(this.productType);
        post.setTransactionMethod(this.transactionMethod);
        post.setCampusLocation(this.campusLocation);
        
        // 상태는 별도 메서드로 업데이트하거나 수정 폼에서만 변경 가능
        if (this.status != null) {
            post.setStatus(this.status);
        }
    }
}