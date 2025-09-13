package com.agentic.e2etester.security;

/**
 * Types of audit events for security tracking.
 */
public enum AuditEventType {
    /**
     * User authentication events
     */
    AUTHENTICATION,
    
    /**
     * Authorization and access control events
     */
    AUTHORIZATION,
    
    /**
     * Test execution events
     */
    TEST_EXECUTION,
    
    /**
     * Credential management events
     */
    CREDENTIAL_MANAGEMENT,
    
    /**
     * Configuration changes
     */
    CONFIGURATION_CHANGE,
    
    /**
     * Data access events
     */
    DATA_ACCESS,
    
    /**
     * Security policy violations
     */
    SECURITY_VIOLATION,
    
    /**
     * System administration events
     */
    SYSTEM_ADMIN
}