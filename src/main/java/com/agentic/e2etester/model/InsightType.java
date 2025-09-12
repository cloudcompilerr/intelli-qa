package com.agentic.e2etester.model;

/**
 * Enumeration of insight types that the AI agent can discover and learn.
 */
public enum InsightType {
    
    /**
     * Performance-related insights about test execution efficiency.
     */
    PERFORMANCE,
    
    /**
     * Reliability insights about test stability and consistency.
     */
    RELIABILITY,
    
    /**
     * Pattern insights about successful test execution patterns.
     */
    PATTERN,
    
    /**
     * Failure pattern insights about common failure modes.
     */
    FAILURE_PATTERN,
    
    /**
     * Service behavior insights about microservice interactions.
     */
    SERVICE_BEHAVIOR,
    
    /**
     * Data quality insights about test data and validation.
     */
    DATA_QUALITY,
    
    /**
     * Timing insights about optimal execution timing and sequencing.
     */
    TIMING,
    
    /**
     * Resource utilization insights about system resource usage.
     */
    RESOURCE_USAGE,
    
    /**
     * Configuration insights about optimal test configurations.
     */
    CONFIGURATION,
    
    /**
     * Business logic insights about domain-specific test behaviors.
     */
    BUSINESS_LOGIC
}