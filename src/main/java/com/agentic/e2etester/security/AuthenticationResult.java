package com.agentic.e2etester.security;

import java.time.Instant;
import java.util.Optional;

/**
 * Result of an authentication attempt.
 */
public class AuthenticationResult {
    private final boolean successful;
    private final SecurityContext securityContext;
    private final String failureReason;
    private final Instant authenticatedAt;
    private final String sessionId;
    
    private AuthenticationResult(boolean successful, SecurityContext securityContext,
                                String failureReason, Instant authenticatedAt, String sessionId) {
        this.successful = successful;
        this.securityContext = securityContext;
        this.failureReason = failureReason;
        this.authenticatedAt = authenticatedAt;
        this.sessionId = sessionId;
    }
    
    public static AuthenticationResult success(SecurityContext securityContext, String sessionId) {
        return new AuthenticationResult(true, securityContext, null, Instant.now(), sessionId);
    }
    
    public static AuthenticationResult failure(String reason) {
        return new AuthenticationResult(false, null, reason, Instant.now(), null);
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public Optional<SecurityContext> getSecurityContext() {
        return Optional.ofNullable(securityContext);
    }
    
    public Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }
    
    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }
    
    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId);
    }
}