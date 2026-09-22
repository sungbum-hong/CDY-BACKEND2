package com.cdy.cdy.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {


    private final JwtUtil jwtUtil;

    // 인증 없이 접근 가능한 경로는 JWTFilter 자체를 건너뜀
    // (만료된 토큰이 헤더에 있어도 401을 반환하지 않도록)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        // OPTIONS preflight 요청은 JWT 검증 없이 통과
        if (HttpMethod.OPTIONS.name().equals(method)) return true;
        return HttpMethod.GET.name().equals(method) && "/api/v1/study/members".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.info("jwt필터 호출");

        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            throw new ServletException("Invalid JWT token");
        }

        // 토큰 파싱
        String accessToken = authorization.substring(7);

        if (jwtUtil.isValid(accessToken, true)) {

            String username = jwtUtil.getUsername(accessToken);
            String role = jwtUtil.getRole(accessToken);

            List<GrantedAuthority> authorities = Collections.singletonList
                    (new SimpleGrantedAuthority(toAuthority(role)));

            Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"토큰 만료 또는 유효하지 않은 토큰\"}");
            return;
        }

    }

    /**
     * JWT의 role 클레임을 Spring Security 권한 문자열로 변환한다.
     * LoginSuccessHandler가 getAuthority() 결과("ROLE_ADMIN")를 그대로 클레임에 담기 때문에
     * 접두사를 무조건 붙이면 "ROLE_ROLE_ADMIN"이 되어 hasRole('ADMIN')이 실패한다.
     * 이미 발급된 토큰과 순수 enum 이름("ADMIN") 둘 다 처리한다.
     */
    private String toAuthority(String role) {
        if (role == null || role.isBlank()) return "ROLE_USER";
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

}