package com.agentic.e2etester.testdata;

/**
 * Enumeration of test data isolation levels
 */
public enum TestDataIsolationLevel {
    /**
     * No isolation - shared data across all tests
     */
    NONE,
    
    /**
     * Test-level isolation - each test has its own data space
     */
    TEST_LEVEL,
    
    /**
     * Step-level isolation - each test step has isolated data
     */
    STEP_LEVEL,
    
    /**
     * Service-level isolation - data isolated per service interaction
     */
    SERVICE_LEVEL,
    
    /**
     * Complete isolation - maximum isolation with separate namespaces
     */
    COMPLETE
}