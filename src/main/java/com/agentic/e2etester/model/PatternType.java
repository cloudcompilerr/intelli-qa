package com.agentic.e2etester.model;

/**
 * Enumeration of different types of test patterns that can be learned and reused.
 */
public enum PatternType {
    /**
     * Successful execution patterns for specific service flows
     */
    SUCCESS_FLOW,
    
    /**
     * Common failure patterns and their characteristics
     */
    FAILURE_PATTERN,
    
    /**
     * Performance patterns for specific load conditions
     */
    PERFORMANCE_BASELINE,
    
    /**
     * Data validation patterns for specific data types
     */
    DATA_VALIDATION,
    
    /**
     * Service interaction patterns for specific protocols
     */
    SERVICE_INTERACTION,
    
    /**
     * Recovery patterns for handling failures
     */
    RECOVERY_STRATEGY,
    
    /**
     * Business flow patterns for end-to-end scenarios
     */
    BUSINESS_FLOW
}