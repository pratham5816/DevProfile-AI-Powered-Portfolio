package com.portfolio.devprofile.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(2) // Runs right AFTER ApiKeyAuthFilter
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long ONE_MINUTE_IN_MS = 60_000L;

    private record ClientUsage(long windowStartTime, AtomicInteger requestCount) {}

    private final Map<String, ClientUsage> ipUsageMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/chat/")) {
            String clientIp = extractClientIp(request);
            long currentTime = System.currentTimeMillis();

            // Thread-safe update of the IP's 60-second window
            ClientUsage usage = ipUsageMap.compute(clientIp, (ip, currentUsage) -> {
                if (currentUsage == null || (currentTime - currentUsage.windowStartTime()) > ONE_MINUTE_IN_MS) {
                    // New window: 1st request
                    return new ClientUsage(currentTime, new AtomicInteger(1));
                } else {
                    // Existing window: increment request count
                    currentUsage.requestCount().incrementAndGet();
                    return currentUsage;
                }
            });

            if (usage.requestCount().get() > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Rate limit exceeded. Please wait a minute before asking more questions.\"}");
                return; // Stop here! Does not reach Cohere or Qdrant
            }
        }

        // 3. Under limit -> pass to controller
        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
