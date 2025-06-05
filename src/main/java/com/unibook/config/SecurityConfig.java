package com.unibook.config;

import com.unibook.common.AppConstants;
import com.unibook.domain.entity.User;
import com.unibook.security.CustomAuthenticationFailureHandler;
import com.unibook.security.CustomUserDetailsService;
import com.unibook.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

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
                // 찜 목록과 내 게시글은 인증 필요
                .requestMatchers("/posts/wishlist", "/posts/my").authenticated()
                .requestMatchers("/books", "/books/**").permitAll()
                // 채팅 관련 페이지는 인증 필요
                .requestMatchers("/chat", "/chat/**").authenticated()
                // 인증이 필요한 API 엔드포인트
                .requestMatchers("/api/auth/resend-verification").authenticated()
                .requestMatchers("/api/wishlist/**").authenticated()
                // 글로벌 채팅 리스너용 엔드포인트는 인증 없이 허용
                .requestMatchers("/api/chat/rooms/by-firebase-id/**").permitAll()
                .requestMatchers("/api/chat/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                // 책 API는 명시적으로 허용
                .requestMatchers("/api/books/**").permitAll()
                // 나머지 API 엔드포인트 허용
                .requestMatchers("/api/**").permitAll()
                // 회원가입, 로그인, 이메일 인증 관련 페이지 허용
                .requestMatchers("/signup", "/login", "/error", "/token-error").permitAll()
                .requestMatchers("/verify-email", "/resend-verification", "/forgot-password", "/reset-password").permitAll()
                .requestMatchers("/verification-required").permitAll()
                // 정적 정보 페이지들 허용 (로그인 불필요)
                .requestMatchers("/about", "/guide", "/faq", "/privacy", "/terms").permitAll()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")  // HTML에서 username 필드 사용
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(customAuthenticationFailureHandler)
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
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.ALWAYS)
                .sessionFixation().newSession()  // 로그인 시 새 세션 ID 발급
                .maximumSessions(AppConstants.MAX_CONCURRENT_SESSIONS)
                .maxSessionsPreventsLogin(true)  // 동시 로그인 차단
            )
            // CSRF 설정 - API 엔드포인트에서는 비활성화
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            // 접근 거부 시 처리
            .exceptionHandling(exceptions -> exceptions
                // 인증되지 않은 사용자가 보호된 리소스에 접근 시
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect("/login?returnUrl=" + request.getRequestURI());
                })
                // 인증은 되었지만 권한이 없는 경우
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect("/access-denied");
                })
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
            // 정지 체크는 이제 CustomUserDetailsService에서 처리되므로 제거
            
            // 정상 로그인 처리
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