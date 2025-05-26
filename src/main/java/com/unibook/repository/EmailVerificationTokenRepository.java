package com.unibook.repository;

import com.unibook.domain.entity.EmailVerificationToken;
import com.unibook.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    List<EmailVerificationToken> findByUserAndTokenTypeAndUsedFalse(User user, EmailVerificationToken.TokenType tokenType);
    
    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user = :user AND t.tokenType = :tokenType AND t.used = false AND t.expiryDate > :now")
    List<EmailVerificationToken> findValidTokensByUserAndType(User user, EmailVerificationToken.TokenType tokenType, LocalDateTime now);
    
    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.user = :user AND t.tokenType = :tokenType AND t.used = false")
    void invalidateAllUserTokensByType(User user, EmailVerificationToken.TokenType tokenType);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiryDate < :expiredDate")
    void deleteExpiredTokens(LocalDateTime expiredDate);
}