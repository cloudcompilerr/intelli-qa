package com.agentic.e2etester.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Security filter that validates sessions and sets up security context.
 */
@Component
public class SecurityContextFilter extends OncePerRequestFilter {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String sessionId = extractSessionId(request);
        String apiKey = extractApiKey(request);
        
        if (sessionId != null) {
            Optional<SecurityContext> contextOpt = authenticationService.validateSession(sessionId);
            if (contextOpt.isPresent()) {
                SecurityContextHolder.setContext(contextOpt.get());
            }
        } else if (apiKey != null) {
            AuthenticationResult result = authenticationService.authenticateWithApiKey(
                apiKey, 
                getClientIpAddress(request), 
                request.getHeader("User-Agent")
            );
            if (result.isSuccessful()) {
                result.getSecurityContext().ifPresent(SecurityContextHolder::setContext);
            }
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    private String extractSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return request.getHeader("X-Session-ID");
    }
    
    private String extractApiKey(HttpServletRequest request) {
        return request.getHeader("X-API-Key");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}