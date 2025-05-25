package com.unibook.repository;

import com.unibook.domain.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findBySchoolName(String schoolName);
    List<School> findBySchoolNameContaining(String keyword);
    boolean existsBySchoolName(String schoolName);
    boolean existsByAllDomainsContaining(String domain);
}