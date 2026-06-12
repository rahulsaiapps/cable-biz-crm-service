package com.cablepulse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for unauthenticated auth endpoints.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/v1/auth/";

    private final int maxRequestsPerMinute;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${cablepulse.security.auth-rate-limit-per-minute:60}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = Math.max(10, maxRequestsPerMinute);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(AUTH_PREFIX)) {
            return true;
        }
        return "/api/v1/auth/health".equals(path)
                || "/api/v1/auth/app-version-check".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        long minuteBucket = Instant.now().getEpochSecond() / 60L;
        String bucketKey = clientKey + ":" + minuteBucket;

        WindowCounter counter = counters.computeIfAbsent(bucketKey, ignored -> new WindowCounter(minuteBucket));
        if (counter.minute != minuteBucket) {
            counter = new WindowCounter(minuteBucket);
            counters.put(bucketKey, counter);
        }

        int count = counter.count.incrementAndGet();
        if (count > maxRequestsPerMinute) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"status\":\"ERROR\",\"error\":\"Too many authentication attempts. Try again later.\"}");
            return;
        }

        pruneOldBuckets(minuteBucket);
        filterChain.doFilter(request, response);
    }

    private static String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void pruneOldBuckets(long currentMinute) {
        if (counters.size() < 500) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 2);
    }

    private static final class WindowCounter {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger(0);

        private WindowCounter(long minute) {
            this.minute = minute;
        }
    }
}
