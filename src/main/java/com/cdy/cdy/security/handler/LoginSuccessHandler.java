package com.cdy.cdy.security.handler;

import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import com.cdy.cdy.security.jwt.JwtService;
import com.cdy.cdy.security.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Qualifier("LoginSuccessHandler")
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {


        // username, role
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // create JWT
        String accessToken = jwtUtil.createJWT(username, role, true);
        String refreshToken = jwtUtil.createJWT(username, role, false);

        // refresh save
        jwtService.addRefresh(username, refreshToken);

        // terms_agreed 조회
        boolean termsAgreed = userRepository.findByUsername(username)
                .map(Users::getTermsAgreed)
                .map(Boolean.TRUE::equals)
                .orElse(false);

        //response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format(
                "{\"accessToken\":\"%s\", \"refreshToken\":\"%s\", \"termsAgreed\":%b}",
                accessToken, refreshToken, termsAgreed);
        response.getWriter().write(json);
        response.getWriter().flush();

    }
}