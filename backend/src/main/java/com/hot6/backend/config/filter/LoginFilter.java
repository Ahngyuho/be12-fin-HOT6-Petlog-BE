package com.hot6.backend.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hot6.backend.common.BaseResponse;
import com.hot6.backend.common.BaseResponseStatus;
//import com.hot6.backend.redis.RefreshTokenRepository;
import com.hot6.backend.user.model.User;
import com.hot6.backend.user.model.UserDto;
import com.hot6.backend.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler failureHandler;
//    private final RefreshTokenRepository refreshTokenRepository;

    private final long REFRESH_EXP = Duration.ofDays(7).toMillis(); // 7일

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        System.out.println("LoginFilter 실행됐다.");
        UsernamePasswordAuthenticationToken authToken;

        try {
            UserDto.LoginRequest userDto  = new ObjectMapper().readValue(request.getInputStream(), UserDto.LoginRequest.class);

            authToken =
                    new UsernamePasswordAuthenticationToken(userDto.getEmail(), userDto.getPassword(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return authenticationManager.authenticate(authToken);
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        User user = (User) authResult.getPrincipal();

        if (!user.getEnabled()) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            new ObjectMapper().writeValue(response.getWriter(), new BaseResponse<>(BaseResponseStatus.EMAIL_VERIFY_FAIL));
            return;
        }

        String jwtToken = JwtUtil.generateToken(user);
        String refreshToken = JwtUtil.generateRefreshToken(user, REFRESH_EXP);

//        refreshTokenRepository.save(user.getEmail(), refreshToken, REFRESH_EXP);

        ResponseCookie cookie = ResponseCookie
                .from("ATOKEN", jwtToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(Duration.ofHours(1L))
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 응답 객체 통일 (BaseResponse 사용)
        Map<String, Object> userData = Map.of(
                "userId", user.getNickname(),
                "email", user.getEmail(),
                "userType", user.getUserType(),
                "enabled", user.isEnabled()
        );

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), new BaseResponse<>(userData));
    }


    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
