package com.agentic.e2etester.security;

import java.util.Optional;

/**
 * Result of an authorization check.
 */
public class AuthorizationResult {
    private final boolean authorized;
    private final String reason;
    private final String requiredRole;
    private final String requiredPermission;
    
    private AuthorizationResult(boolean authorized, String reason, 
                               String requiredRole, String requiredPermission) {
        this.authorized = authorized;
        this.reason = reason;
        this.requiredRole = requiredRole;
        this.requiredPermission = requiredPermission;
    }
    
    public static AuthorizationResult authorized() {
        return new AuthorizationResult(true, null, null, null);
    }
    
    public static AuthorizationResult denied(String reason) {
        return new AuthorizationResult(false, reason, null, null);
    }
    
    public static AuthorizationResult deniedMissingRole(String requiredRole) {
        return new AuthorizationResult(false, "Missing required role", requiredRole, null);
    }
    
    public static AuthorizationResult deniedMissingPermission(String requiredPermission) {
        return new AuthorizationResult(false, "Missing required permission", null, requiredPermission);
    }
    
    public boolean isAuthorized() {
        return authorized;
    }
    
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }
    
    public Optional<String> getRequiredRole() {
        return Optional.ofNullable(requiredRole);
    }
    
    public Optional<String> getRequiredPermission() {
        return Optional.ofNullable(requiredPermission);
    }
}