package com.unibook.repository;

import com.unibook.domain.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findBySchoolName(String schoolName);
    List<School> findBySchoolNameContainingOrderBySchoolNameAsc(String keyword);
    boolean existsBySchoolName(String schoolName);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM School s " +
           "JOIN s.allDomains d WHERE d = :domain")
    boolean existsByDomain(String domain);
    
    Optional<School> findByPrimaryDomain(String primaryDomain);
    List<School> findBySchoolNameContainingIgnoreCase(String schoolName);
}