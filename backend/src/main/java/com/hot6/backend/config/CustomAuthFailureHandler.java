package com.hot6.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hot6.backend.common.BaseResponseStatus;
import com.hot6.backend.common.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

                                        @Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

        @Value("${frontend-server}")
        private String frontendServer;

        @Override
        public void onAuthenticationFailure(HttpServletRequest request,
                HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        Throwable cause = exception.getCause();

        BaseResponseStatus status;

        if (exception instanceof InternalAuthenticationServiceException
                && exception.getCause() instanceof BaseException baseEx) {
            status = baseEx.getStatus();
        } else if (exception instanceof BadCredentialsException) {
            status = BaseResponseStatus.INVALID_CREDENTIALS;
        }  else {
            status = BaseResponseStatus.LOGIN_FAILED;
        }

        if (cause instanceof BaseException baseEx) {
            status = baseEx.getStatus();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");

        String redirectUrl = frontendServer + "/user/login/error/deleted";

        Map<String, Object> res = new HashMap<>();
        res.put("redirectUrl", redirectUrl);
        res.put("isSuccess", false);
        res.put("code", status.getCode());
        res.put("message", status.getMessage());

        response.getWriter().write(new ObjectMapper().writeValueAsString(res));

    }
}
