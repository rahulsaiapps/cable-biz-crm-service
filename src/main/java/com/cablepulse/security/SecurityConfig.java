package com.cablepulse.security;

import com.cablepulse.repository.EmployeeRepository;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseAuth firebaseAuth;
    private final EmployeeRepository employeeRepository;

    public SecurityConfig(FirebaseAuth firebaseAuth, EmployeeRepository employeeRepository) {
        this.firebaseAuth = firebaseAuth;
        this.employeeRepository = employeeRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/plans", "/api/v1/plans/**", "/api/v1/employees/**").hasRole("OWNER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(new FirebaseAuthenticationFilter(firebaseAuth, employeeRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
