package com.agentic.e2etester.security;

import java.time.Instant;
import java.util.Set;

/**
 * Security context containing authentication and authorization information
 * for the current test execution session.
 */
public class SecurityContext {
    private final String userId;
    private final String sessionId;
    private final Set<SecurityRole> roles;
    private final Set<String> permissions;
    private final Instant authenticatedAt;
    private final String authenticationType;
    private final String clientIpAddress;
    
    public SecurityContext(String userId, String sessionId, Set<SecurityRole> roles, 
                          Set<String> permissions, Instant authenticatedAt, 
                          String authenticationType, String clientIpAddress) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.roles = roles;
        this.permissions = permissions;
        this.authenticatedAt = authenticatedAt;
        this.authenticationType = authenticationType;
        this.clientIpAddress = clientIpAddress;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public Set<SecurityRole> getRoles() {
        return roles;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
    
    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }
    
    public String getAuthenticationType() {
        return authenticationType;
    }
    
    public String getClientIpAddress() {
        return clientIpAddress;
    }
    
    public boolean hasRole(SecurityRole role) {
        return roles.contains(role);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    public boolean hasAnyRole(SecurityRole... roles) {
        for (SecurityRole role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}