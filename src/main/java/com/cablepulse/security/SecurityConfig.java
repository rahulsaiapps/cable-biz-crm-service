package com.cablepulse.security;

import com.cablepulse.service.EmployeeReconciliationService;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseAuth firebaseAuth;
    private final EmployeeReconciliationService employeeReconciliationService;

    public SecurityConfig(
            FirebaseAuth firebaseAuth,
            EmployeeReconciliationService employeeReconciliationService) {
        this.firebaseAuth = firebaseAuth;
        this.employeeReconciliationService = employeeReconciliationService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/employees/profile").authenticated()
                .requestMatchers("/api/v1/plans", "/api/v1/plans/**", "/api/v1/employees/**").hasRole("OWNER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                    new FirebaseAuthenticationFilter(firebaseAuth, employeeReconciliationService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
