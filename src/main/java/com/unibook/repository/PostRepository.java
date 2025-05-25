package com.unibook.repository;

import com.unibook.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Fetch Join으로 N+1 문제 해결
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
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
           "WHERE p.postId = :postId")
    Optional<Post> findByIdWithDetails(Long postId);
    
    List<Post> findByUser_Department_School_SchoolId(Long schoolId);
    List<Post> findByStatus(Post.PostStatus status);
    
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "WHERE p.status = :status " +
           "ORDER BY p.createdAt DESC")
    List<Post> findByStatusWithDetails(@Param("status") Post.PostStatus status);
    List<Post> findByBook_BookId(Long bookId);
    
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "LEFT JOIN FETCH p.book " +
           "ORDER BY p.createdAt DESC")
    List<Post> findAllWithDetails();
}