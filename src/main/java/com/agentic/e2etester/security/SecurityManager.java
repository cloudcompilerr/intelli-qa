package com.agentic.e2etester.security;

/**
 * Central security manager that coordinates authentication, authorization, and auditing.
 */
public interface SecurityManager {
    
    /**
     * Authenticates a user and creates a security context.
     *
     * @param username the username
     * @param password the password
     * @param clientIpAddress the client IP address
     * @param userAgent the user agent
     * @return authentication result
     */
    AuthenticationResult authenticate(String username, String password, 
                                    String clientIpAddress, String userAgent);
    
    /**
     * Authenticates using an API key.
     *
     * @param apiKey the API key
     * @param clientIpAddress the client IP address
     * @param userAgent the user agent
     * @return authentication result
     */
    AuthenticationResult authenticateWithApiKey(String apiKey, 
                                               String clientIpAddress, String userAgent);
    
    /**
     * Validates and authorizes a test execution request.
     *
     * @param sessionId the session ID
     * @param testId the test ID
     * @param testEnvironment the test environment
     * @return authorization result
     */
    AuthorizationResult authorizeTestExecution(String sessionId, String testId, String testEnvironment);
    
    /**
     * Validates and authorizes credential access.
     *
     * @param sessionId the session ID
     * @param credentialId the credential ID
     * @param action the action (READ, WRITE, DELETE)
     * @return authorization result
     */
    AuthorizationResult authorizeCredentialAccess(String sessionId, String credentialId, String action);
    
    /**
     * Gets the current security context for a session.
     *
     * @param sessionId the session ID
     * @return security context if valid
     */
    java.util.Optional<SecurityContext> getSecurityContext(String sessionId);
    
    /**
     * Invalidates a session.
     *
     * @param sessionId the session ID
     */
    void invalidateSession(String sessionId);
    
    /**
     * Records a security event for auditing.
     *
     * @param securityContext the security context
     * @param eventType the event type
     * @param resource the resource
     * @param action the action
     * @param result the result
     * @param additionalData additional data
     */
    void recordSecurityEvent(SecurityContext securityContext, AuditEventType eventType,
                            String resource, String action, AuditResult result,
                            java.util.Map<String, Object> additionalData);
}