package com.portfolio.devprofile.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final String expectedApiKey;
    private final List<String> origins;

    public ApiKeyAuthFilter(@Value("${app.security.allowed-origins}") String origins , @Value("${app.security.api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
        this.origins = Arrays.asList(origins.split(","));
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Ensure CORS headers are present so browser clients receive clean status codes
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-KEY, Authorization");

        // 1. Allow CORS pre-flight OPTIONS requests through unconditionally
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getRequestURI();

        // 2. Protect all chat endpoints from unauthorized access

        String incomingKey = request.getHeader("X-API-KEY");

        if (path.startsWith("/chat/")) {
            if (incomingKey == null || !incomingKey.equals(expectedApiKey)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Unauthorized: Missing or invalid X-API-KEY header\"}");
                return;
            }
        }else{
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Unauthorized: Not allowed to perform this action\"}");
            return;
        }


        // 3. Authorized request -> continue to controller
        filterChain.doFilter(request, response);
    }
}
