package com.agentic.e2etester.security;

import java.util.Optional;

/**
 * Service for handling authentication operations.
 */
public interface AuthenticationService {
    
    /**
     * Authenticates a user with username and password.
     *
     * @param username the username
     * @param password the password
     * @param clientIpAddress the client IP address
     * @param userAgent the user agent string
     * @return authentication result
     */
    AuthenticationResult authenticate(String username, String password, 
                                    String clientIpAddress, String userAgent);
    
    /**
     * Authenticates using an API key.
     *
     * @param apiKey the API key
     * @param clientIpAddress the client IP address
     * @param userAgent the user agent string
     * @return authentication result
     */
    AuthenticationResult authenticateWithApiKey(String apiKey, 
                                               String clientIpAddress, String userAgent);
    
    /**
     * Authenticates using a JWT token.
     *
     * @param token the JWT token
     * @param clientIpAddress the client IP address
     * @param userAgent the user agent string
     * @return authentication result
     */
    AuthenticationResult authenticateWithToken(String token, 
                                              String clientIpAddress, String userAgent);
    
    /**
     * Validates an existing session.
     *
     * @param sessionId the session ID
     * @return the security context if session is valid
     */
    Optional<SecurityContext> validateSession(String sessionId);
    
    /**
     * Invalidates a session.
     *
     * @param sessionId the session ID
     */
    void invalidateSession(String sessionId);
    
    /**
     * Refreshes a session extending its validity.
     *
     * @param sessionId the session ID
     * @return updated security context
     */
    Optional<SecurityContext> refreshSession(String sessionId);
    
    /**
     * Checks if a user account is locked.
     *
     * @param username the username
     * @return true if account is locked
     */
    boolean isAccountLocked(String username);
    
    /**
     * Records a failed authentication attempt.
     *
     * @param username the username
     * @param clientIpAddress the client IP address
     */
    void recordFailedAttempt(String username, String clientIpAddress);
}