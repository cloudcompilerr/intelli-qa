package com.agentic.e2etester.security;

/**
 * Service for handling authorization and access control.
 */
public interface AuthorizationService {
    
    /**
     * Checks if the current security context has permission to perform an action on a resource.
     *
     * @param securityContext the security context
     * @param resource the resource being accessed
     * @param action the action being performed
     * @return true if authorized
     */
    boolean isAuthorized(SecurityContext securityContext, String resource, String action);
    
    /**
     * Checks if the current security context has a specific role.
     *
     * @param securityContext the security context
     * @param role the required role
     * @return true if user has the role
     */
    boolean hasRole(SecurityContext securityContext, SecurityRole role);
    
    /**
     * Checks if the current security context has any of the specified roles.
     *
     * @param securityContext the security context
     * @param roles the required roles
     * @return true if user has any of the roles
     */
    boolean hasAnyRole(SecurityContext securityContext, SecurityRole... roles);
    
    /**
     * Checks if the current security context has a specific permission.
     *
     * @param securityContext the security context
     * @param permission the required permission
     * @return true if user has the permission
     */
    boolean hasPermission(SecurityContext securityContext, String permission);
    
    /**
     * Validates that a test execution is authorized for the current context.
     *
     * @param securityContext the security context
     * @param testId the test ID
     * @param testEnvironment the test environment
     * @return authorization result
     */
    AuthorizationResult authorizeTestExecution(SecurityContext securityContext, 
                                              String testId, String testEnvironment);
    
    /**
     * Validates that credential access is authorized.
     *
     * @param securityContext the security context
     * @param credentialId the credential ID
     * @param action the action (READ, WRITE, DELETE)
     * @return authorization result
     */
    AuthorizationResult authorizeCredentialAccess(SecurityContext securityContext, 
                                                 String credentialId, String action);
    
    /**
     * Validates that configuration access is authorized.
     *
     * @param securityContext the security context
     * @param configurationKey the configuration key
     * @param action the action (READ, WRITE, DELETE)
     * @return authorization result
     */
    AuthorizationResult authorizeConfigurationAccess(SecurityContext securityContext, 
                                                     String configurationKey, String action);
}