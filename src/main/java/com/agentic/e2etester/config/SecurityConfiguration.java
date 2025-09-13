package com.agentic.e2etester.config;

import com.agentic.e2etester.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Agentic E2E Tester.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/login", "/api/auth/token").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/test/**").hasAnyRole("ADMIN", "TEST_EXECUTOR", "CICD_SYSTEM")
                .requestMatchers("/api/credentials/**").hasAnyRole("ADMIN", "TEST_EXECUTOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(new SecurityContextFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public com.agentic.e2etester.security.SecurityManager securityManager(AuthenticationService authenticationService,
                                          AuthorizationService authorizationService,
                                          AuditService auditService) {
        return new DefaultSecurityManager(authenticationService, authorizationService, auditService);
    }
    
    @Bean
    public CredentialRotationScheduler credentialRotationScheduler(CredentialManager credentialManager,
                                                                  AuditService auditService) {
        return new CredentialRotationScheduler(credentialManager, auditService);
    }
}