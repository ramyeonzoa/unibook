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
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Fetch Join으로 N+1 문제 해결
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "WHERE p.status != 'BLOCKED' " +
           "ORDER BY p.createdAt DESC")
    List<Post> findRecentPostsWithDetails(Pageable pageable);
    
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school s " +
           "LEFT JOIN FETCH p.book " +
           "WHERE s.schoolId = :schoolId")
    List<Post> findBySchoolIdWithDetails(Long schoolId);
    
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "LEFT JOIN FETCH p.subject " +
           "WHERE p.postId = :postId")
    Optional<Post> findByIdWithDetails(Long postId);
    
    List<Post> findByUser_Department_School_SchoolId(Long schoolId);
    List<Post> findByStatus(Post.PostStatus status);
    
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "LEFT JOIN FETCH p.subject " +
           "WHERE p.status = :status " +
           "ORDER BY p.createdAt DESC")
    List<Post> findByStatusWithDetails(@Param("status") Post.PostStatus status);
    List<Post> findByBook_BookId(Long bookId);
    List<Post> findByBook_BookIdAndStatusNot(Long bookId, Post.PostStatus status);
    
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "LEFT JOIN FETCH p.subject " +
           "ORDER BY p.createdAt DESC")
    List<Post> findAllWithDetails();
    
    // Subject 관련 조회 메서드 추가
    List<Post> findBySubject_SubjectId(Long subjectId);
    List<Post> findBySubject_SubjectIdAndStatus(Long subjectId, Post.PostStatus status);
    
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "LEFT JOIN FETCH p.subject " +
           "WHERE p.subject.subjectId = :subjectId " +
           "AND p.status != 'BLOCKED' " +
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
           "(" +
           "  COALESCE(MATCH(p.title) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
           "  COALESCE(MATCH(pd.description) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
           "  COALESCE(MATCH(b.title, b.author) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
           "  COALESCE(MATCH(s.subject_name) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0) + " +
           "  COALESCE(MATCH(pr.professor_name) AGAINST(:searchQuery IN NATURAL LANGUAGE MODE), 0)" +
           ") AS totalScore " +
           "FROM posts p " +
           "LEFT JOIN post_descriptions pd ON p.post_id = pd.post_id " +
           "LEFT JOIN books b ON p.book_id = b.book_id " +
           "LEFT JOIN subjects s ON p.subject_id = s.subject_id " +
           "LEFT JOIN professors pr ON s.professor_id = pr.professor_id " +
           "LEFT JOIN users u ON p.user_id = u.user_id " +
           "LEFT JOIN departments d ON u.department_id = d.department_id " +
           "WHERE (" +
           "  MATCH(p.title) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(pd.description) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(b.title, b.author) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(s.subject_name) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(pr.professor_name) AGAINST(:searchQuery IN BOOLEAN MODE)" +
           ") " +
           "AND p.status != 'BLOCKED' " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:productType IS NULL OR p.product_type = :productType) " +
           "AND (:schoolId IS NULL OR d.school_id = :schoolId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "ORDER BY totalScore DESC, p.created_at DESC",
           countQuery = "SELECT COUNT(DISTINCT p.post_id) " +
           "FROM posts p " +
           "LEFT JOIN post_descriptions pd ON p.post_id = pd.post_id " +
           "LEFT JOIN books b ON p.book_id = b.book_id " +
           "LEFT JOIN subjects s ON p.subject_id = s.subject_id " +
           "LEFT JOIN professors pr ON s.professor_id = pr.professor_id " +
           "LEFT JOIN users u ON p.user_id = u.user_id " +
           "LEFT JOIN departments d ON u.department_id = d.department_id " +
           "WHERE (" +
           "  MATCH(p.title) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(pd.description) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(b.title, b.author) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(s.subject_name) AGAINST(:searchQuery IN BOOLEAN MODE) " +
           "  OR MATCH(pr.professor_name) AGAINST(:searchQuery IN BOOLEAN MODE)" +
           ") " +
           "AND p.status != 'BLOCKED' " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:productType IS NULL OR p.product_type = :productType) " +
           "AND (:schoolId IS NULL OR d.school_id = :schoolId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
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
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "LEFT JOIN FETCH p.subject " +
           "WHERE p.postId IN :ids")
    List<Post> findAllByIdInWithDetails(@Param("ids") List<Long> ids);
    
    /**
     * 필터링만 적용 (검색어 없을 때)
     * Pageable의 정렬 정보를 사용하도록 ORDER BY 제거
     * BLOCKED 상태 게시글은 목록에서 제외
     */
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "WHERE p.status != 'BLOCKED' " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:productType IS NULL OR p.productType = :productType) " +
           "AND (:schoolId IS NULL OR d.school.schoolId = :schoolId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findByFilters(@Param("status") Post.PostStatus status,
                            @Param("productType") Post.ProductType productType,
                            @Param("schoolId") Long schoolId,
                            @Param("minPrice") Integer minPrice,
                            @Param("maxPrice") Integer maxPrice,
                            Pageable pageable);
    
    /**
     * 사용자가 찜한 게시글 목록 조회 (Fetch Join으로 N+1 방지)
     * BLOCKED 상태 게시글은 제외
     */
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
    Page<Post> findWishlistedPostsByUserWithPriceFilter(@Param("userId") Long userId, 
                                                         @Param("minPrice") Integer minPrice, 
                                                         @Param("maxPrice") Integer maxPrice, 
                                                         Pageable pageable);
    
    /**
     * 사용자가 작성한 게시글 목록 조회 (Fetch Join으로 N+1 방지)
     * 작성자에게는 BLOCKED 상태 게시글도 표시 (상태 확인 가능하도록)
     */
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
    Page<Post> findByUserIdWithDetailsAndPriceFilter(@Param("userId") Long userId, 
                                                      @Param("minPrice") Integer minPrice, 
                                                      @Param("maxPrice") Integer maxPrice, 
                                                      Pageable pageable);
    
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
     */
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "LEFT JOIN FETCH p.subject " +
           "WHERE p.subject.subjectId = :subjectId " +
           "AND p.status != 'BLOCKED' " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:productType IS NULL OR p.productType = :productType) " +
           "AND (:schoolId IS NULL OR d.school.schoolId = :schoolId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findBySubjectIdWithFilters(@Param("subjectId") Long subjectId,
                                         @Param("status") Post.PostStatus status,
                                         @Param("productType") Post.ProductType productType,
                                         @Param("schoolId") Long schoolId,
                                         @Param("minPrice") Integer minPrice,
                                         @Param("maxPrice") Integer maxPrice,
                                         Pageable pageable);
    
    /**
     * 교수 ID로 게시글 검색 (필터링 포함)
     */
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "LEFT JOIN FETCH p.subject s " +
           "LEFT JOIN FETCH s.professor " +
           "WHERE s.professor.professorId = :professorId " +
           "AND p.status != 'BLOCKED' " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:productType IS NULL OR p.productType = :productType) " +
           "AND (:schoolId IS NULL OR d.school.schoolId = :schoolId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findByProfessorIdWithFilters(@Param("professorId") Long professorId,
                                           @Param("status") Post.PostStatus status,
                                           @Param("productType") Post.ProductType productType,
                                           @Param("schoolId") Long schoolId,
                                           @Param("minPrice") Integer minPrice,
                                           @Param("maxPrice") Integer maxPrice,
                                           Pageable pageable);
    
    /**
     * 책 제목으로 게시글 검색 (필터링 포함)
     */
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book b " +
           "LEFT JOIN FETCH p.subject " +
           "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :bookTitle, '%')) " +
           "AND p.status != 'BLOCKED' " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:productType IS NULL OR p.productType = :productType) " +
           "AND (:schoolId IS NULL OR d.school.schoolId = :schoolId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
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
}