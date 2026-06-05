package com.cablepulse.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7);
            try {
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
                String uid = decodedToken.getUid();
                Map<String, Object> claims = decodedToken.getClaims();

                List<GrantedAuthority> authorities = new ArrayList<>();

                // Map roles based on claims
                if (claims.containsKey("role")) {
                    String role = String.valueOf(claims.get("role")).toUpperCase();
                    if (role.equals("OWNER") || role.equals("ROLE_OWNER")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
                    } else if (role.equals("COLLECTION_BOY") || role.equals("ROLE_COLLECTION_BOY")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_COLLECTION_BOY"));
                    }
                }

                // Check boolean claim mappings
                if (Boolean.TRUE.equals(claims.get("owner"))) {
                    if (authorities.stream().noneMatch(a -> a.getAuthority().equals("ROLE_OWNER"))) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
                    }
                }
                if (Boolean.TRUE.equals(claims.get("collection_boy"))) {
                    if (authorities.stream().noneMatch(a -> a.getAuthority().equals("ROLE_COLLECTION_BOY"))) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_COLLECTION_BOY"));
                    }
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        uid,
                        idToken,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // Clear the authentication context on validation failures
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
