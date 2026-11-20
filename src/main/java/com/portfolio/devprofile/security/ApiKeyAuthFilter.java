package com.portfolio.devprofile.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final String expectedApiKey;
    private final List<String> origins;
    private static final AtomicInteger apiRequests = new AtomicInteger(0);

    public ApiKeyAuthFilter(@Value("${app.security.allowed-origins}") String origins , @Value("${app.security.api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
        this.origins = Arrays.asList(origins.split(","));
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyApiCounter() {
        apiRequests.set(0);
        System.out.println("API request counter reset to 0 for the new day.");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        String path = request.getRequestURI();
        String incomingKey = request.getHeader("X-API-KEY");

        if (origin != null) {
            if (origins.contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
                response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-API-KEY, Authorization");
                response.setHeader("Vary", "Origin");
            } else {
                // Browser origin is not in the allowed list -> Block
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Forbidden: Origin not allowed\"}");
                return;
            }
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if (path.startsWith("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/chat/")) {
            if (incomingKey == null || !incomingKey.equals(expectedApiKey)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Unauthorized: Missing or invalid X-API-KEY header\"}");
                return;
            }
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Unauthorized: Not allowed to perform this action\"}");
            return;
        }

        if (apiRequests.incrementAndGet() > 100) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Too Many Requests: Daily quota reached. Try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
