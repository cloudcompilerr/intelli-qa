package com.agentic.e2etester.security;

/**
 * Result of an audited security event.
 */
public enum AuditResult {
    /**
     * Operation completed successfully
     */
    SUCCESS,
    
    /**
     * Operation failed due to authentication issues
     */
    AUTHENTICATION_FAILURE,
    
    /**
     * Operation failed due to authorization issues
     */
    AUTHORIZATION_FAILURE,
    
    /**
     * Operation failed due to validation errors
     */
    VALIDATION_FAILURE,
    
    /**
     * Operation failed due to system errors
     */
    SYSTEM_FAILURE,
    
    /**
     * Operation was blocked by security policies
     */
    SECURITY_BLOCKED,
    
    /**
     * Operation was denied due to rate limiting
     */
    RATE_LIMITED
}