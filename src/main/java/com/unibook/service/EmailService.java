package com.unibook.service;

import com.unibook.common.Messages;
import com.unibook.domain.entity.EmailVerificationToken;
import com.unibook.domain.entity.User;
import com.unibook.exception.EmailException;
import com.unibook.repository.EmailVerificationTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.email.verification.expiration-hours}")
    private int verificationExpirationHours;
    
    @Value("${app.email.password-reset.expiration-hours}")
    private int passwordResetExpirationHours;
    
    @Value("${app.email.verification.base-url}")
    private String baseUrl;
    
    /**
     * 이메일 인증 메일 발송 (비동기 처리)
     */
    @Async("emailTaskExecutor")
    @Transactional
    public void sendVerificationEmail(User user) {
        try {
            // 기존 미사용 토큰 무효화
            tokenRepository.invalidateAllUserTokensByType(user, EmailVerificationToken.TokenType.EMAIL_VERIFICATION);
            
            // 새 토큰 생성
            EmailVerificationToken verificationToken = EmailVerificationToken.createToken(
                    user, 
                    EmailVerificationToken.TokenType.EMAIL_VERIFICATION, 
                    verificationExpirationHours
            );
            
            tokenRepository.save(verificationToken);
            
            // 이메일 발송
            String subject = "[Unibook] 이메일 인증을 완료해주세요";
            String verificationUrl = baseUrl + "/verify-email?token=" + verificationToken.getToken();
            
            Context context = new Context();
            context.setVariable("userName", user.getName());
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("expirationHours", verificationExpirationHours);
            
            String htmlContent = templateEngine.process("email/verification", context);
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            
            log.info("Verification email sent to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send verification email to: {} - Error: {}", user.getEmail(), e.getMessage(), e);
            throw new EmailException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", "EMAIL_SEND_FAILED");
        }
    }
    
    /**
     * 비밀번호 재설정 메일 발송 (비동기 처리)
     */
    @Async("emailTaskExecutor")
    @Transactional
    public void sendPasswordResetEmail(User user) {
        try {
            // 기존 미사용 토큰 무효화
            tokenRepository.invalidateAllUserTokensByType(user, EmailVerificationToken.TokenType.PASSWORD_RESET);
            
            // 새 토큰 생성
            EmailVerificationToken resetToken = EmailVerificationToken.createToken(
                    user,
                    EmailVerificationToken.TokenType.PASSWORD_RESET,
                    passwordResetExpirationHours
            );
            
            tokenRepository.save(resetToken);
            
            // 이메일 발송
            String subject = "[Unibook] 비밀번호 재설정 안내";
            String resetUrl = baseUrl + "/reset-password?token=" + resetToken.getToken();
            
            Context context = new Context();
            context.setVariable("userName", user.getName());
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expirationHours", passwordResetExpirationHours);
            
            String htmlContent = templateEngine.process("email/password-reset", context);
            
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            
            log.info("Password reset email sent to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new EmailException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", "EMAIL_SEND_FAILED");
        }
    }
    
    /**
     * HTML 이메일 발송 (재시도 로직 포함)
     */
    @Retryable(
        value = {MessagingException.class, EmailException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        log.info("Attempting to send email to: {}", to);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        
        log.info("Email sent successfully to: {}", to);
    }
    
    /**
     * 이메일 발송 최종 실패 시 처리
     */
    @Recover
    public void recoverFromEmailFailure(MessagingException e, String to, String subject, String htmlContent) {
        log.error("All email retry attempts failed for: {}. Subject: {}", to, subject);
        log.error("Final error: ", e);
        
        // TODO: 실패한 이메일 정보를 DB에 저장하거나 관리자에게 알림 발송
        // 현재는 로그만 남기고 EmailException을 발생시켜 상위에서 처리하도록 함
        throw new EmailException("이메일 발송이 계속 실패했습니다. 고객센터에 문의해주세요.", "EMAIL_SEND_FAILED_AFTER_RETRIES");
    }
    
    /**
     * 만료된 토큰 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime expiredDate = LocalDateTime.now().minusDays(7);
        tokenRepository.deleteExpiredTokens(expiredDate);
        log.info("Cleaned up expired tokens older than: {}", expiredDate);
    }
}