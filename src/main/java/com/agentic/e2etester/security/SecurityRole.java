package com.agentic.e2etester.security;

/**
 * Enumeration of security roles for test execution authorization.
 */
public enum SecurityRole {
    /**
     * Administrator with full system access
     */
    ADMIN,
    
    /**
     * Test executor who can run tests
     */
    TEST_EXECUTOR,
    
    /**
     * Test viewer who can only view test results
     */
    TEST_VIEWER,
    
    /**
     * CI/CD system role for automated test execution
     */
    CICD_SYSTEM,
    
    /**
     * Service account for internal system operations
     */
    SERVICE_ACCOUNT
}