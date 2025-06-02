package com.unibook.repository;

import com.unibook.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    /**
     * 사용자 조회 (Department, School 정보 포함) - N+1 방지
     */
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.department d " +
           "LEFT JOIN FETCH d.school " +
           "WHERE u.userId = :userId")
    Optional<User> findByIdWithDepartmentAndSchool(@Param("userId") Long userId);
    
    /**
     * 사용자 검색 (이름 또는 이메일로)
     */
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);
}