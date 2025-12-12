package com.hot6.backend.config;

import com.hot6.backend.common.BaseResponseStatus;
import com.hot6.backend.common.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${frontend-server}")
    private String frontendServer;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        Throwable cause = exception.getCause();
        BaseResponseStatus status = BaseResponseStatus.LOGIN_FAILED;

        if (cause instanceof BaseException baseEx) {
            status = baseEx.getStatus();
        }

        String redirectUrl = switch (status) {
            case USER_DELETED_LOGIN -> frontendServer + "/user/login/error/deleted";
            case INVALID_CREDENTIALS -> frontendServer + "/user/login/error/credentials";
            default -> frontendServer + "/user/login/error/general";
        };

        // 전체 페이지 요청이므로 진짜 redirect 사용 가능
        response.sendRedirect(redirectUrl);
    }
}
