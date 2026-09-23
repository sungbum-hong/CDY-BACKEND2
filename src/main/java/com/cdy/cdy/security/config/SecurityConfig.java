package com.cdy.cdy.security.config;

import com.cdy.cdy.domain.users.entity.UserRole;
import com.cdy.cdy.security.handler.CustomLogoutHandler;
import com.cdy.cdy.security.jwt.JWTFilter;
import com.cdy.cdy.security.jwt.JwtService;
import com.cdy.cdy.security.jwt.JwtUtil;
import com.cdy.cdy.security.jwt.LoginFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@EnableWebSecurity
@EnableMethodSecurity   // 이게 없으면 @PreAuthorize 가 무시된다
@Component
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final JwtService jwtService;
    private final JwtUtil jwtUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf((auth) -> auth.disable());
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.httpBasic((auth) -> auth.disable());
        http.formLogin((auth) -> auth.disable());

        http.addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), authenticationSuccessHandler),
                UsernamePasswordAuthenticationFilter.class);

        http.logout(logout -> logout.addLogoutHandler(new CustomLogoutHandler(jwtService, jwtUtil)));

        http.addFilterAfter(new JWTFilter(jwtUtil), LoginFilter.class);

        http.authorizeHttpRequests((auth) -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/login", "/jwt/refresh", "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/apply").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/study/members").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/study/findByUser/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/projects").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/projects/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/services").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/services/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/contests").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/partners").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/bootstrap").permitAll()
                // 크루 전용 혜택몰 / 쇼핑몰 - 로그인 필수
                .requestMatchers("/api/v1/benefits/**").authenticated()
                .requestMatchers("/api/v1/shop/**").authenticated()
                // 어드민 API - ADMIN 권한 필수 (@PreAuthorize 와 이중 방어)
                .requestMatchers("/api/v1/admin/**").hasRole(UserRole.ADMIN.name())
                .anyRequest().authenticated()
        );

        // 미인증은 401, 인증됐지만 권한 부족은 403으로 구분한다.
        // (프론트 axios 인터셉터가 401을 토큰 재발급 신호로 사용)
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> writeError(res, 401, "로그인이 필요합니다."))
                .accessDeniedHandler((req, res, e) -> writeError(res, 403, "접근 권한이 없습니다."))
        );

        return http.build();
    }

    /** GlobalExceptionHandler의 ErrorResponse와 동일한 형태로 내려준다. */
    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":%d,\"messages\":\"%s\",\"timestamp\":\"%s\"}"
                        .formatted(status, message, LocalDateTime.now()));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://www.codiyoung.com",
                "https://codiyoung.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                .role(UserRole.ADMIN.name()).implies(UserRole.USER.name())
                .build();
    }
}
