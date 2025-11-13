package com.example.silkmall.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(requestedWith);
        boolean expectsJson = accept != null && accept.contains("application/json");

        if (isAjax || expectsJson) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未授权访问");
        } else {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/login");
        }
    }
}