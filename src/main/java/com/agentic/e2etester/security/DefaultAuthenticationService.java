package com.agentic.e2etester.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of authentication service.
 */
@Service
public class DefaultAuthenticationService implements AuthenticationService {
    
    private final Map<String, UserAccount> userAccounts = new ConcurrentHashMap<>();
    private final Map<String, SecurityContext> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<FailedAttempt>> failedAttempts = new ConcurrentHashMap<>();
    private final AuditService auditService;
    
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int SESSION_TIMEOUT_HOURS = 8;
    
    @Autowired
    public DefaultAuthenticationService(AuditService auditService) {
        this.auditService = auditService;
        initializeDefaultUsers();
    }
    
    private void initializeDefaultUsers() {
        // Create default admin user
        UserAccount admin = new UserAccount(
            "admin",
            "admin",
            "$2a$10$N9qo8uLOickgx2ZMRZoMye", // BCrypt hash for "password"
            Set.of(SecurityRole.ADMIN),
            Set.of("*"), // All permissions
            true,
            Instant.now(),
            null
        );
        userAccounts.put("admin", admin);
        
        // Create default test executor
        UserAccount testExecutor = new UserAccount(
            "test-executor",
            "test-executor",
            "$2a$10$N9qo8uLOickgx2ZMRZoMye",
            Set.of(SecurityRole.TEST_EXECUTOR),
            Set.of("test:execute", "test:view", "credential:read"),
            true,
            Instant.now(),
            null
        );
        userAccounts.put("test-executor", testExecutor);
    }
    
    @Override
    public AuthenticationResult authenticate(String username, String password, 
                                           String clientIpAddress, String userAgent) {
        try {
            if (isAccountLocked(username)) {
                auditService.recordAuthenticationEvent(
                    username, AuditResult.AUTHENTICATION_FAILURE, 
                    clientIpAddress, userAgent, "PASSWORD"
                );
                return AuthenticationResult.failure("Account is locked");
            }
            
            UserAccount user = userAccounts.get(username);
            if (user == null || !user.isActive()) {
                recordFailedAttempt(username, clientIpAddress);
                auditService.recordAuthenticationEvent(
                    username, AuditResult.AUTHENTICATION_FAILURE, 
                    clientIpAddress, userAgent, "PASSWORD"
                );
                return AuthenticationResult.failure("Invalid credentials");
            }
            
            // In a real implementation, use BCrypt or similar
            if (!user.getPasswordHash().equals(hashPassword(password))) {
                recordFailedAttempt(username, clientIpAddress);
                auditService.recordAuthenticationEvent(
                    username, AuditResult.AUTHENTICATION_FAILURE, 
                    clientIpAddress, userAgent, "PASSWORD"
                );
                return AuthenticationResult.failure("Invalid credentials");
            }
            
            // Clear failed attempts on successful authentication
            failedAttempts.remove(username);
            
            // Create security context and session
            String sessionId = UUID.randomUUID().toString();
            SecurityContext securityContext = new SecurityContext(
                user.getUserId(),
                sessionId,
                user.getRoles(),
                user.getPermissions(),
                Instant.now(),
                "PASSWORD",
                clientIpAddress
            );
            
            activeSessions.put(sessionId, securityContext);
            
            auditService.recordAuthenticationEvent(
                username, AuditResult.SUCCESS, 
                clientIpAddress, userAgent, "PASSWORD"
            );
            
            return AuthenticationResult.success(securityContext, sessionId);
            
        } catch (Exception e) {
            auditService.recordAuthenticationEvent(
                username, AuditResult.SYSTEM_FAILURE, 
                clientIpAddress, userAgent, "PASSWORD"
            );
            return AuthenticationResult.failure("Authentication system error");
        }
    }
    
    @Override
    public AuthenticationResult authenticateWithApiKey(String apiKey, 
                                                      String clientIpAddress, String userAgent) {
        // Simple API key validation - in production, use proper API key management
        if ("test-api-key-123".equals(apiKey)) {
            String sessionId = UUID.randomUUID().toString();
            SecurityContext securityContext = new SecurityContext(
                "api-user",
                sessionId,
                Set.of(SecurityRole.CICD_SYSTEM),
                Set.of("test:execute", "test:view"),
                Instant.now(),
                "API_KEY",
                clientIpAddress
            );
            
            activeSessions.put(sessionId, securityContext);
            
            auditService.recordAuthenticationEvent(
                "api-user", AuditResult.SUCCESS, 
                clientIpAddress, userAgent, "API_KEY"
            );
            
            return AuthenticationResult.success(securityContext, sessionId);
        }
        
        auditService.recordAuthenticationEvent(
            "unknown", AuditResult.AUTHENTICATION_FAILURE, 
            clientIpAddress, userAgent, "API_KEY"
        );
        return AuthenticationResult.failure("Invalid API key");
    }
    
    @Override
    public AuthenticationResult authenticateWithToken(String token, 
                                                     String clientIpAddress, String userAgent) {
        // JWT token validation would go here
        // For now, return failure
        auditService.recordAuthenticationEvent(
            "unknown", AuditResult.AUTHENTICATION_FAILURE, 
            clientIpAddress, userAgent, "JWT"
        );
        return AuthenticationResult.failure("Token authentication not implemented");
    }
    
    @Override
    public Optional<SecurityContext> validateSession(String sessionId) {
        SecurityContext context = activeSessions.get(sessionId);
        if (context == null) {
            return Optional.empty();
        }
        
        // Check if session has expired
        if (context.getAuthenticatedAt().plus(SESSION_TIMEOUT_HOURS, ChronoUnit.HOURS)
                .isBefore(Instant.now())) {
            activeSessions.remove(sessionId);
            return Optional.empty();
        }
        
        return Optional.of(context);
    }
    
    @Override
    public void invalidateSession(String sessionId) {
        SecurityContext context = activeSessions.remove(sessionId);
        if (context != null) {
            auditService.recordEvent(
                context.getUserId(),
                sessionId,
                AuditEventType.AUTHENTICATION,
                "session",
                "LOGOUT",
                AuditResult.SUCCESS,
                context.getClientIpAddress(),
                null,
                Map.of()
            );
        }
    }
    
    @Override
    public Optional<SecurityContext> refreshSession(String sessionId) {
        SecurityContext existingContext = activeSessions.get(sessionId);
        if (existingContext == null) {
            return Optional.empty();
        }
        
        // Create new context with updated timestamp
        SecurityContext refreshedContext = new SecurityContext(
            existingContext.getUserId(),
            existingContext.getSessionId(),
            existingContext.getRoles(),
            existingContext.getPermissions(),
            Instant.now(),
            existingContext.getAuthenticationType(),
            existingContext.getClientIpAddress()
        );
        
        activeSessions.put(sessionId, refreshedContext);
        return Optional.of(refreshedContext);
    }
    
    @Override
    public boolean isAccountLocked(String username) {
        List<FailedAttempt> attempts = failedAttempts.get(username);
        if (attempts == null || attempts.isEmpty()) {
            return false;
        }
        
        // Remove old attempts
        Instant cutoff = Instant.now().minus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
        attempts.removeIf(attempt -> attempt.timestamp.isBefore(cutoff));
        
        return attempts.size() >= MAX_FAILED_ATTEMPTS;
    }
    
    @Override
    public void recordFailedAttempt(String username, String clientIpAddress) {
        failedAttempts.computeIfAbsent(username, k -> new ArrayList<>())
            .add(new FailedAttempt(Instant.now(), clientIpAddress));
    }
    
    private String hashPassword(String password) {
        // In production, use BCrypt or similar
        return "$2a$10$N9qo8uLOickgx2ZMRZoMye";
    }
    
    private static class UserAccount {
        private final String userId;
        private final String username;
        private final String passwordHash;
        private final Set<SecurityRole> roles;
        private final Set<String> permissions;
        private final boolean active;
        private final Instant createdAt;
        private final Instant lastLoginAt;
        
        public UserAccount(String userId, String username, String passwordHash,
                          Set<SecurityRole> roles, Set<String> permissions,
                          boolean active, Instant createdAt, Instant lastLoginAt) {
            this.userId = userId;
            this.username = username;
            this.passwordHash = passwordHash;
            this.roles = roles;
            this.permissions = permissions;
            this.active = active;
            this.createdAt = createdAt;
            this.lastLoginAt = lastLoginAt;
        }
        
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getPasswordHash() { return passwordHash; }
        public Set<SecurityRole> getRoles() { return roles; }
        public Set<String> getPermissions() { return permissions; }
        public boolean isActive() { return active; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastLoginAt() { return lastLoginAt; }
    }
    
    private static class FailedAttempt {
        private final Instant timestamp;
        private final String clientIpAddress;
        
        public FailedAttempt(Instant timestamp, String clientIpAddress) {
            this.timestamp = timestamp;
            this.clientIpAddress = clientIpAddress;
        }
    }
}