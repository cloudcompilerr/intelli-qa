package com.agentic.e2etester.security;

import org.springframework.stereotype.Service;

/**
 * Default implementation of authorization service.
 */
@Service
public class DefaultAuthorizationService implements AuthorizationService {
    
    @Override
    public boolean isAuthorized(SecurityContext securityContext, String resource, String action) {
        if (securityContext == null) {
            return false;
        }
        
        // Admin has access to everything
        if (securityContext.hasRole(SecurityRole.ADMIN)) {
            return true;
        }
        
        // Check specific permissions
        String permission = resource + ":" + action;
        if (securityContext.hasPermission(permission)) {
            return true;
        }
        
        // Check wildcard permissions
        if (securityContext.hasPermission("*")) {
            return true;
        }
        
        // Check resource-level wildcard
        if (securityContext.hasPermission(resource + ":*")) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean hasRole(SecurityContext securityContext, SecurityRole role) {
        return securityContext != null && securityContext.hasRole(role);
    }
    
    @Override
    public boolean hasAnyRole(SecurityContext securityContext, SecurityRole... roles) {
        return securityContext != null && securityContext.hasAnyRole(roles);
    }
    
    @Override
    public boolean hasPermission(SecurityContext securityContext, String permission) {
        return securityContext != null && securityContext.hasPermission(permission);
    }
    
    @Override
    public AuthorizationResult authorizeTestExecution(SecurityContext securityContext, 
                                                     String testId, String testEnvironment) {
        if (securityContext == null) {
            return AuthorizationResult.denied("No security context");
        }
        
        // Check if user can execute tests
        if (!isAuthorized(securityContext, "test", "execute")) {
            return AuthorizationResult.deniedMissingPermission("test:execute");
        }
        
        // Additional checks for production environment
        if ("production".equalsIgnoreCase(testEnvironment)) {
            if (!securityContext.hasAnyRole(SecurityRole.ADMIN, SecurityRole.CICD_SYSTEM)) {
                return AuthorizationResult.deniedMissingRole("ADMIN or CICD_SYSTEM");
            }
        }
        
        return AuthorizationResult.authorized();
    }
    
    @Override
    public AuthorizationResult authorizeCredentialAccess(SecurityContext securityContext, 
                                                        String credentialId, String action) {
        if (securityContext == null) {
            return AuthorizationResult.denied("No security context");
        }
        
        // Check credential permissions
        if (!isAuthorized(securityContext, "credential", action)) {
            return AuthorizationResult.deniedMissingPermission("credential:" + action);
        }
        
        // Additional checks for sensitive operations
        if ("DELETE".equalsIgnoreCase(action) || "WRITE".equalsIgnoreCase(action)) {
            if (!securityContext.hasRole(SecurityRole.ADMIN)) {
                return AuthorizationResult.deniedMissingRole("ADMIN");
            }
        }
        
        return AuthorizationResult.authorized();
    }
    
    @Override
    public AuthorizationResult authorizeConfigurationAccess(SecurityContext securityContext, 
                                                           String configurationKey, String action) {
        if (securityContext == null) {
            return AuthorizationResult.denied("No security context");
        }
        
        // Check configuration permissions
        if (!isAuthorized(securityContext, "configuration", action)) {
            return AuthorizationResult.deniedMissingPermission("configuration:" + action);
        }
        
        // Additional checks for security-related configurations
        if (configurationKey.contains("security") || configurationKey.contains("credential")) {
            if (!securityContext.hasRole(SecurityRole.ADMIN)) {
                return AuthorizationResult.deniedMissingRole("ADMIN");
            }
        }
        
        return AuthorizationResult.authorized();
    }
}