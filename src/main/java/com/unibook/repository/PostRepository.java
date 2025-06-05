package com.unibook.repository;

import com.unibook.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.unibook.repository.projection.PostSearchProjection;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // ===== 공통 JOIN 패턴 상수 (중복 제거용) =====
    String JOIN_USER_DETAILS = "LEFT JOIN FETCH p.user u " +
                              "LEFT JOIN FETCH u.department d " +
                              "LEFT JOIN FETCH d.school ";
    
    String JOIN_BOOK = "LEFT JOIN FETCH p.book ";
    
    String JOIN_SUBJECT = "LEFT JOIN FETCH p.subject ";
    
    String JOIN_POST_IMAGES = "LEFT JOIN FETCH p.postImages ";
    
    // 조합된 JOIN 패턴들
    String JOIN_ALL_DETAILS = JOIN_USER_DETAILS + JOIN_BOOK + JOIN_SUBJECT;
    
    String JOIN_BASIC_DETAILS = JOIN_USER_DETAILS + JOIN_BOOK;
    
    String JOIN_USER_WITH_IMAGES = JOIN_USER_DETAILS + JOIN_POST_IMAGES;
    
    // 공통 WHERE 절 조건들
    String EXCLUDE_BLOCKED = "p.status != 'BLOCKED' ";
    
    String FILTER_CONDITIONS = "(:status IS NULL OR p.status = :status) " +
                              "AND (:productType IS NULL OR p.productType = :productType) " +
                              "AND (:schoolId IS NULL OR d.school.schoolId = :schoolId) " +
                              "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                              "AND (:maxPrice IS NULL OR p.price <= :maxPrice)";
    
    // ===== Native Query 전용 상수 (Full-text 검색용) =====
    String NATIVE_FROM_CLAUSE = "FROM posts p " +
                               "LEFT JOIN post_descriptions pd ON p.post_id = pd.post_id " +
                               "LEFT JOIN books b ON p.book_id = b.book_id " +
                               "LEFT JOIN subjects s ON p.subject_id = s.subject_id " +
                               "LEFT JOIN professors pr ON s.professor_id = pr.professor_id " +
                               "LEFT JOIN users u ON p.user_id = u.user_id " +
                               "LEFT JOIN departments d ON u.department_id = d.department_id ";
    
    String NATIVE_FULLTEXT_SEARCH = "(" +
                                   "  MATCH(p.title) AGAINST(:searchQuery IN BOOLEAN MODE) " +
                                   "  OR MATCH(pd.description) AGAINST(:searchQuery IN BOOLEAN MODE) " +
                                   "  OR MATCH(b.title, b.author) AGAINST(:searchQuery IN BOOLEAN MODE) " +
                                   "  OR MATCH(s.subject_name) AGAINST(:searchQuery IN BOOLEAN MODE) " +
                                   "  OR MATCH(pr.professor_name) AGAINST(:searchQuery IN BOOLEAN MODE)" +
                                   ") ";
    
    String NATIVE_FILTER_CONDITIONS = "AND p.status != 'BLOCKED' " +
                                     "AND (:status IS NULL OR p.status = :status) " +
                                     "AND (:productType IS NULL OR p.product_type = :productType) " +
                                     "AND (:schoolId IS NULL OR d.school_id = :schoolId) " +
                                     "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                                     "AND (:maxPrice IS NULL OR p.price <= :maxPrice)";
    
    String NATIVE_SCORE_CALCULATION = "(" +
                                     "  COALESCE(MATCH(p.title) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
                                     "  COALESCE(MATCH(pd.description) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
                                     "  COALESCE(MATCH(b.title, b.author) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
                                     "  COALESCE(MATCH(s.subject_name) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
                                     "  COALESCE(MATCH(pr.professor_name) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0)" +
                                     ") AS totalScore ";
    
    // ==============================================
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Fetch Join으로 N+1 문제 해결
    @Query("SELECT p FROM Post p " +
           JOIN_BASIC_DETAILS +
           "WHERE " + EXCLUDE_BLOCKED +
           "ORDER BY p.createdAt DESC")
    List<Post> findRecentPostsWithDetails(Pageable pageable);
    
    @Query("SELECT p FROM Post p " +
           JOIN_BASIC_DETAILS +
           "WHERE d.school.schoolId = :schoolId")
    List<Post> findBySchoolIdWithDetails(Long schoolId);
    
    @Query("SELECT p FROM Post p " +
           JOIN_ALL_DETAILS +
           "WHERE p.postId = :postId")
    Optional<Post> findByIdWithDetails(Long postId);
    
    List<Post> findByUser_Department_School_SchoolId(Long schoolId);
    List<Post> findByStatus(Post.PostStatus status);
    
    @Query("SELECT p FROM Post p " +
           JOIN_ALL_DETAILS +
           "WHERE p.status = :status " +
           "ORDER BY p.createdAt DESC")
    List<Post> findByStatusWithDetails(@Param("status") Post.PostStatus status);
    List<Post> findByBook_BookId(Long bookId);
    List<Post> findByBook_BookIdAndStatusNot(Long bookId, Post.PostStatus status);
    
    @Query("SELECT p FROM Post p " +
           JOIN_ALL_DETAILS +
           "ORDER BY p.createdAt DESC")
    List<Post> findAllWithDetails();
    
    // Subject 관련 조회 메서드 추가
    List<Post> findBySubject_SubjectId(Long subjectId);
    List<Post> findBySubject_SubjectIdAndStatus(Long subjectId, Post.PostStatus status);
    
    @Query("SELECT p FROM Post p " +
           JOIN_ALL_DETAILS +
           "WHERE p.subject.subjectId = :subjectId " +
           "AND " + EXCLUDE_BLOCKED +
           "ORDER BY p.createdAt DESC")
    List<Post> findBySubject_SubjectIdWithDetails(@Param("subjectId") Long subjectId);
    
    // ===== Full-text Search 쿼리 =====
    
    /**
     * 게시글 제목, 설명, 책 정보, 과목명, 교수명에서 키워드 검색 (D안)
     * - Boolean Mode: WHERE 절 필터링용
     * - Natural Language Mode: SELECT 절 관련도 점수용
     * - 관련도순 정렬 → 같으면 최신순
     */
    @Query(value = "SELECT p.post_id AS postId, " +
           NATIVE_SCORE_CALCULATION +
           NATIVE_FROM_CLAUSE +
           "WHERE " + NATIVE_FULLTEXT_SEARCH +
           NATIVE_FILTER_CONDITIONS + " " +
           "ORDER BY totalScore DESC, p.created_at DESC",
           countQuery = "SELECT COUNT(DISTINCT p.post_id) " +
           NATIVE_FROM_CLAUSE +
           "WHERE " + NATIVE_FULLTEXT_SEARCH +
           NATIVE_FILTER_CONDITIONS,
           nativeQuery = true)
    Page<PostSearchProjection> searchPostsWithFulltext(@Param("searchQuery") String searchQuery,
                                                      @Param("status") String status,
                                                      @Param("productType") String productType,
                                                      @Param("schoolId") Long schoolId,
                                                      @Param("minPrice") Integer minPrice,
                                                      @Param("maxPrice") Integer maxPrice,
                                                      Pageable pageable);
    
    /**
     * 게시글 ID 목록으로 Post 조회 (Fetch Join)
     * 순서는 Service 레이어에서 처리
     */
    @Query("SELECT DISTINCT p FROM Post p " +
           JOIN_ALL_DETAILS +
           "WHERE p.postId IN :ids")
    List<Post> findAllByIdInWithDetails(@Param("ids") List<Long> ids);
    
    /**
     * 필터링만 적용 (검색어 없을 때)
     * Pageable의 정렬 정보를 사용하도록 ORDER BY 제거
     * BLOCKED 상태 게시글은 목록에서 제외
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findPostsWithOptionalFilters(Long, Long, String, Post.PostStatus, Post.ProductType, Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query("SELECT p FROM Post p " +
           JOIN_USER_DETAILS +
           "WHERE " + EXCLUDE_BLOCKED +
           "AND " + FILTER_CONDITIONS)
    Page<Post> findByFilters(@Param("status") Post.PostStatus status,
                            @Param("productType") Post.ProductType productType,
                            @Param("schoolId") Long schoolId,
                            @Param("minPrice") Integer minPrice,
                            @Param("maxPrice") Integer maxPrice,
                            Pageable pageable);
    
    /**
     * 사용자가 찜한 게시글 목록 조회 (Fetch Join으로 N+1 방지)
     * BLOCKED 상태 게시글은 제외
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findWishlistedPostsByUserUnified(Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query(value = "SELECT p FROM Wishlist w " +
                   "JOIN w.post p " +
                   "JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH u.department d " +
                   "LEFT JOIN FETCH d.school " +
                   "LEFT JOIN FETCH p.postImages " +
                   "WHERE w.user.userId = :userId AND p.status != 'BLOCKED'",
           countQuery = "SELECT COUNT(w) FROM Wishlist w JOIN w.post p WHERE w.user.userId = :userId AND p.status != 'BLOCKED'")
    Page<Post> findWishlistedPostsByUser(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 사용자가 찜한 게시글 목록 조회 (가격 필터링 포함)
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findWishlistedPostsByUserUnified(Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query(value = "SELECT p FROM Wishlist w " +
                   "JOIN w.post p " +
                   "JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH u.department d " +
                   "LEFT JOIN FETCH d.school " +
                   "LEFT JOIN FETCH p.postImages " +
                   "WHERE w.user.userId = :userId AND p.status != 'BLOCKED' " +
                   "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                   "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
           countQuery = "SELECT COUNT(w) FROM Wishlist w JOIN w.post p WHERE w.user.userId = :userId AND p.status != 'BLOCKED' " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findWishlistedPostsByUserWithPriceFilter(@Param("userId") Long userId, 
                                                         @Param("minPrice") Integer minPrice, 
                                                         @Param("maxPrice") Integer maxPrice, 
                                                         Pageable pageable);
    
    /**
     * 사용자가 작성한 게시글 목록 조회 (Fetch Join으로 N+1 방지)
     * 작성자에게는 BLOCKED 상태 게시글도 표시 (상태 확인 가능하도록)
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findUserPostsByUserUnified(Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query(value = "SELECT p FROM Post p " +
                   "JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH u.department d " +
                   "LEFT JOIN FETCH d.school " +
                   "LEFT JOIN FETCH p.postImages " +
                   "WHERE p.user.userId = :userId",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.userId = :userId")
    Page<Post> findByUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 사용자가 작성한 게시글 목록 조회 (가격 필터링 포함)
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findUserPostsByUserUnified(Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query(value = "SELECT p FROM Post p " +
                   "JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH u.department d " +
                   "LEFT JOIN FETCH d.school " +
                   "LEFT JOIN FETCH p.postImages " +
                   "WHERE p.user.userId = :userId " +
                   "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                   "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.userId = :userId " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findByUserIdWithDetailsAndPriceFilter(@Param("userId") Long userId, 
                                                      @Param("minPrice") Integer minPrice, 
                                                      @Param("maxPrice") Integer maxPrice, 
                                                      Pageable pageable);
    
    // ===== 통합 메서드들 (중복 제거용) =====
    
    /**
     * 사용자가 찜한 게시글 목록 조회 (통합 - 선택적 가격 필터링)
     * BLOCKED 상태 게시글은 제외
     * minPrice, maxPrice가 null이면 가격 필터링 무시
     */
    @Query(value = "SELECT p FROM Wishlist w " +
                   "JOIN w.post p " +
                   "JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH u.department d " +
                   "LEFT JOIN FETCH d.school " +
                   "LEFT JOIN FETCH p.postImages " +
                   "WHERE w.user.userId = :userId AND p.status != 'BLOCKED' " +
                   "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                   "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
           countQuery = "SELECT COUNT(w) FROM Wishlist w JOIN w.post p WHERE w.user.userId = :userId AND p.status != 'BLOCKED' " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findWishlistedPostsByUserUnified(@Param("userId") Long userId, 
                                               @Param("minPrice") Integer minPrice, 
                                               @Param("maxPrice") Integer maxPrice, 
                                               Pageable pageable);
    
    /**
     * 사용자가 작성한 게시글 목록 조회 (통합 - 선택적 가격 필터링)
     * BLOCKED 상태 게시글도 포함 (작성자가 자신의 차단된 게시글을 볼 수 있도록)
     * minPrice, maxPrice가 null이면 가격 필터링 무시
     */
    @Query(value = "SELECT p FROM Post p " +
                   "JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH u.department d " +
                   "LEFT JOIN FETCH d.school " +
                   "LEFT JOIN FETCH p.postImages " +
                   "WHERE p.user.userId = :userId " +
                   "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                   "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
           countQuery = "SELECT COUNT(p) FROM Post p WHERE p.user.userId = :userId " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findUserPostsByUserUnified(@Param("userId") Long userId, 
                                         @Param("minPrice") Integer minPrice, 
                                         @Param("maxPrice") Integer maxPrice, 
                                         Pageable pageable);
    
    // ===== 필터링 메서드 통합 (중복 제거용) =====
    
    /**
     * 통합 필터링 메서드 - 모든 필터 조건을 선택적으로 적용
     * 
     * @param subjectId 과목 ID (null이면 과목 필터 무시)
     * @param professorId 교수 ID (null이면 교수 필터 무시)  
     * @param bookTitle 책 제목 (null이면 책제목 필터 무시)
     * @param status 게시글 상태 (null이면 상태 필터 무시)
     * @param productType 상품 타입 (null이면 타입 필터 무시)
     * @param schoolId 학교 ID (null이면 학교 필터 무시)
     * @param minPrice 최소 가격 (null이면 최소가격 필터 무시)
     * @param maxPrice 최대 가격 (null이면 최대가격 필터 무시)
     * @param pageable 페이징 정보
     * @return 필터링된 게시글 페이지
     */
    @Query(value = "SELECT p FROM Post p " +
                   "LEFT JOIN FETCH p.user u " +
                   "LEFT JOIN FETCH u.department d " +
                   "LEFT JOIN FETCH d.school " +
                   "LEFT JOIN FETCH p.book b " +
                   "LEFT JOIN FETCH p.subject s " +
                   "LEFT JOIN FETCH s.professor " +
                   "WHERE 1=1 " +
                   "AND (:subjectId IS NULL OR s.subjectId = :subjectId) " +
                   "AND (:professorId IS NULL OR s.professor.professorId = :professorId) " +
                   "AND (:bookTitle IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :bookTitle, '%'))) " +
                   "AND p.status != 'BLOCKED' " +
                   "AND (:status IS NULL OR p.status = :status) " +
                   "AND (:productType IS NULL OR p.productType = :productType) " +
                   "AND (:schoolId IS NULL OR d.school.schoolId = :schoolId) " +
                   "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                   "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
           countQuery = "SELECT COUNT(p) FROM Post p " +
                        "LEFT JOIN p.user u " +
                        "LEFT JOIN u.department d " +
                        "LEFT JOIN p.subject s " +
                        "LEFT JOIN s.professor pr " +
                        "LEFT JOIN p.book b " +
                        "WHERE 1=1 " +
                        "AND (:subjectId IS NULL OR p.subject.subjectId = :subjectId) " +
                        "AND (:professorId IS NULL OR pr.professorId = :professorId) " +
                        "AND (:bookTitle IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :bookTitle, '%'))) " +
                        "AND p.status != 'BLOCKED' " +
                        "AND (:status IS NULL OR p.status = :status) " +
                        "AND (:productType IS NULL OR p.productType = :productType) " +
                        "AND (:schoolId IS NULL OR d.school.schoolId = :schoolId) " +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findPostsWithOptionalFilters(@Param("subjectId") Long subjectId,
                                           @Param("professorId") Long professorId,
                                           @Param("bookTitle") String bookTitle,
                                           @Param("status") Post.PostStatus status,
                                           @Param("productType") Post.ProductType productType,
                                           @Param("schoolId") Long schoolId,
                                           @Param("minPrice") Integer minPrice,
                                           @Param("maxPrice") Integer maxPrice,
                                           Pageable pageable);
    
    // ==============================================
    
    /**
     * Native Query로 직접 Post 삭제 (외래키 제약 회피)
     */
    @Modifying
    @Query(value = "DELETE FROM posts WHERE post_id = :postId", nativeQuery = true)
    void deleteByIdNative(@Param("postId") Long postId);
    
    // 관리자용 검색 메서드들
    Page<Post> findByTitleContainingOrDescriptionContaining(String title, String description, Pageable pageable);
    Page<Post> findByTitleContainingOrDescriptionContainingAndStatus(String title, String description, Post.PostStatus status, Pageable pageable);
    Page<Post> findByStatus(Post.PostStatus status, Pageable pageable);
    long countByStatus(Post.PostStatus status);
    
    /**
     * 과목 ID로 게시글 검색 (필터링 포함)
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findPostsWithOptionalFilters(Long, Long, String, Post.PostStatus, Post.ProductType, Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query("SELECT p FROM Post p " +
           JOIN_ALL_DETAILS +
           "WHERE p.subject.subjectId = :subjectId " +
           "AND " + EXCLUDE_BLOCKED +
           "AND " + FILTER_CONDITIONS)
    Page<Post> findBySubjectIdWithFilters(@Param("subjectId") Long subjectId,
                                         @Param("status") Post.PostStatus status,
                                         @Param("productType") Post.ProductType productType,
                                         @Param("schoolId") Long schoolId,
                                         @Param("minPrice") Integer minPrice,
                                         @Param("maxPrice") Integer maxPrice,
                                         Pageable pageable);
    
    /**
     * 교수 ID로 게시글 검색 (필터링 포함)
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findPostsWithOptionalFilters(Long, Long, String, Post.PostStatus, Post.ProductType, Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query("SELECT p FROM Post p " +
           JOIN_USER_DETAILS + JOIN_BOOK +
           "LEFT JOIN FETCH p.subject s " +
           "LEFT JOIN FETCH s.professor " +
           "WHERE s.professor.professorId = :professorId " +
           "AND " + EXCLUDE_BLOCKED +
           "AND " + FILTER_CONDITIONS)
    Page<Post> findByProfessorIdWithFilters(@Param("professorId") Long professorId,
                                           @Param("status") Post.PostStatus status,
                                           @Param("productType") Post.ProductType productType,
                                           @Param("schoolId") Long schoolId,
                                           @Param("minPrice") Integer minPrice,
                                           @Param("maxPrice") Integer maxPrice,
                                           Pageable pageable);
    
    /**
     * 책 제목으로 게시글 검색 (필터링 포함)
     * 
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. 
     *             {@link #findPostsWithOptionalFilters(Long, Long, String, Post.PostStatus, Post.ProductType, Long, Integer, Integer, Pageable)} 를 사용하세요.
     */
    @Deprecated
    @Query("SELECT p FROM Post p " +
           JOIN_USER_DETAILS +
           "LEFT JOIN FETCH p.book b " +
           JOIN_SUBJECT +
           "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :bookTitle, '%')) " +
           "AND " + EXCLUDE_BLOCKED +
           "AND " + FILTER_CONDITIONS)
    Page<Post> findByBookTitleWithFilters(@Param("bookTitle") String bookTitle,
                                         @Param("status") Post.PostStatus status,
                                         @Param("productType") Post.ProductType productType,
                                         @Param("schoolId") Long schoolId,
                                         @Param("minPrice") Integer minPrice,
                                         @Param("maxPrice") Integer maxPrice,
                                         Pageable pageable);
    
    /**
     * 교수 ID로 교수명 조회
     */
    @Query("SELECT pr.professorName FROM Professor pr WHERE pr.professorId = :professorId")
    Optional<String> findProfessorNameById(@Param("professorId") Long professorId);
    
    /**
     * 동일한 책의 모든 게시글 조회 (시세 그래프용)
     * BLOCKED 상태 제외, 시간순 정렬
     */
    @Query("SELECT p FROM Post p " +
           JOIN_USER_DETAILS +
           "WHERE p.book.bookId = :bookId " +
           "AND " + EXCLUDE_BLOCKED +
           "ORDER BY p.createdAt ASC")
    List<Post> findByBookIdForPriceTrend(@Param("bookId") Long bookId);
}