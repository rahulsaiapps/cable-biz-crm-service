package com.cablepulse.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);

    private final FirebaseAuth firebaseAuth;
    private final EmployeeRoleResolver employeeRoleResolver;
    private final JwtTokenService jwtTokenService;
    private final boolean allowFirebaseBearer;

    public FirebaseAuthenticationFilter(
            FirebaseAuth firebaseAuth,
            EmployeeRoleResolver employeeRoleResolver,
            JwtTokenService jwtTokenService,
            @Value("${cablepulse.security.allow-firebase-bearer:false}") boolean allowFirebaseBearer) {
        this.firebaseAuth = firebaseAuth;
        this.employeeRoleResolver = employeeRoleResolver;
        this.jwtTokenService = jwtTokenService;
        this.allowFirebaseBearer = allowFirebaseBearer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String bearerToken = authHeader.substring(7);
            boolean authenticated = authenticateBackendJwt(request, bearerToken);
            if (!authenticated) {
                authenticated = authenticateFirebaseToken(request, bearerToken);
            }
            if (!authenticated) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean authenticateBackendJwt(HttpServletRequest request, String bearerToken) {
        try {
            Claims claims = jwtTokenService.parseClaims(bearerToken);
            if (!jwtTokenService.isAccessToken(claims)) {
                return false;
            }

            String uid = claims.getSubject();
            String role = claims.get(JwtTokenService.CLAIM_ROLE, String.class);
            if (role == null || role.isBlank()) {
                role = employeeRoleResolver.resolveRoleForUserId(uid);
            }

            List<GrantedAuthority> authorities =
                    employeeRoleResolver.resolveAuthoritiesForUserId(uid, role);
            setAuthentication(request, uid, bearerToken, authorities);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean authenticateFirebaseToken(HttpServletRequest request, String bearerToken) {
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(bearerToken);
            String uid = decodedToken.getUid();
            List<GrantedAuthority> authorities = employeeRoleResolver.resolveAuthorities(decodedToken);
            setAuthentication(request, uid, bearerToken, authorities);
            return true;
        } catch (Exception e) {
            logger.debug("Firebase token verification failed");
            return false;
        }
    }

    private void setAuthentication(
            HttpServletRequest request,
            String uid,
            String credentials,
            List<GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                uid,
                credentials,
                authorities
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
