package com.hot6.backend.config.filter;


import com.hot6.backend.user.model.User;
import com.hot6.backend.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("JwtFilter 실행됐다.");
        Cookie[] cookies = request.getCookies();
        String jwtToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ATOKEN".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        // ✅ validate 체크 후에만 SecurityContext 설정
        if (jwtToken != null && JwtUtil.validate(jwtToken)) {
            User user = JwtUtil.getUser(jwtToken);

            if (user != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else if (user != null && !user.isEnabled()) {
                System.out.println("이메일 인증이 완료되지 않은 사용자");
                SecurityContextHolder.clearContext();
            } else if(user != null && user.getProvider() != null) {
                System.out.println("OAuth2로 회원가입한 유저");
                SecurityContextHolder.clearContext();
            }
                else {
                // 유저 객체가 없으면 인증 제거
                SecurityContextHolder.clearContext();
            }
        } else {
            // ❗ 토큰이 없거나 유효하지 않으면 인증 제거
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/login") || path.startsWith("/user/signup") || path.startsWith("/user/token/refresh");
    }
}
