package com.cablepulse.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class WebHeaderInterceptor implements HandlerInterceptor {

    public static final String E2E_ID_HEADER = "X-E2E-ID";
    public static final String SESSION_ID_HEADER = "X-Session-ID";
    public static final String MDC_E2E_ID_KEY = "e2eId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String e2eId = request.getHeader(E2E_ID_HEADER);
        if (e2eId == null || e2eId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        try {
            UUID.fromString(e2eId.trim());
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        String sessionId = request.getHeader(SESSION_ID_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        try {
            UUID.fromString(sessionId.trim());
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        MDC.put(MDC_E2E_ID_KEY, e2eId.trim());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        MDC.remove(MDC_E2E_ID_KEY);
    }
}
