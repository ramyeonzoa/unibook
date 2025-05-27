package com.unibook.config;

import com.unibook.common.AppConstants;
import com.unibook.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // 정적 리소스 허용
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/uploads/**").permitAll()
                // 메인 페이지 허용
                .requestMatchers("/", "/home").permitAll()
                // 검색 관련 페이지 허용 (로그인 없이 가능)
                .requestMatchers("/search", "/search/**").permitAll()
                // 게시글 목록과 상세보기는 모두 허용
                .requestMatchers("/posts").permitAll()
                .requestMatchers("/posts/*").permitAll()
                // 게시글 작성/수정/삭제는 인증 필요
                .requestMatchers("/posts/new", "/posts/*/edit", "/posts/*/delete", "/posts/*/status").authenticated()
                .requestMatchers("/books", "/books/**").permitAll()
                // 인증이 필요한 API 엔드포인트
                .requestMatchers("/api/auth/resend-verification").authenticated()
                // 나머지 API 엔드포인트 허용
                .requestMatchers("/api/**").permitAll()
                // 회원가입, 로그인, 이메일 인증 관련 페이지 허용
                .requestMatchers("/signup", "/login", "/error", "/token-error").permitAll()
                .requestMatchers("/verify-email", "/resend-verification", "/forgot-password", "/reset-password").permitAll()
                .requestMatchers("/verification-required").permitAll()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")  // HTML에서 username 필드 사용
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().newSession()  // 로그인 시 새 세션 ID 발급
                .maximumSessions(AppConstants.MAX_CONCURRENT_SESSIONS)
                .maxSessionsPreventsLogin(true)  // 동시 로그인 차단
            );

        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String returnUrl = request.getParameter("returnUrl");
            if (returnUrl != null && !returnUrl.isEmpty() && returnUrl.startsWith("/")) {
                // 보안을 위해 내부 URL만 허용 (/ 로 시작)
                response.sendRedirect(returnUrl);
            } else {
                response.sendRedirect("/");
            }
        };
    }
}