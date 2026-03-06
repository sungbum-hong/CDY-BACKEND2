package com.cdy.cdy.security.config;

import com.cdy.cdy.domain.users.entity.UserRole;
import com.cdy.cdy.security.handler.CustomLogoutHandler;
import com.cdy.cdy.security.jwt.JWTFilter;
import com.cdy.cdy.security.jwt.JwtService;
import com.cdy.cdy.security.jwt.JwtUtil;
import com.cdy.cdy.security.jwt.LoginFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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

import java.util.List;

@EnableWebSecurity
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
    public AuthenticationManager authenticationManager
            (AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf((auth) -> auth.disable());

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http
                .httpBasic((auth) -> auth.disable());

        http
                .formLogin((auth) -> auth.disable());


        // 필터 등록
        http
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), authenticationSuccessHandler),
                        UsernamePasswordAuthenticationFilter.class);

         http
                .logout(logout ->
                logout.addLogoutHandler
                        (new CustomLogoutHandler(jwtService, jwtUtil)));

        http
                .addFilterAfter(new JWTFilter(jwtUtil), LoginFilter.class);

        http
                .authorizeHttpRequests((auth) -> auth

                        .requestMatchers("/login").permitAll()
                );

        return http.build();
    }

    // CORS Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173","https://www.codiyoung.com/"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 권한 계층
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                .role(UserRole.ADMIN.name()).implies(UserRole.USER.name())
                .build();
    }

}





