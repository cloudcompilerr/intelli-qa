package com.agentic.e2etester.model;

/**
 * Enumeration of service health statuses used in decision-making contexts.
 */
public enum ServiceStatus {
    
    /**
     * Service is healthy and responding normally.
     */
    HEALTHY,
    
    /**
     * Service is experiencing degraded performance but still functional.
     */
    DEGRADED,
    
    /**
     * Service is unhealthy but may still be partially functional.
     */
    UNHEALTHY,
    
    /**
     * Service is completely unavailable.
     */
    DOWN,
    
    /**
     * Service status is unknown or cannot be determined.
     */
    UNKNOWN,
    
    /**
     * Service is in maintenance mode.
     */
    MAINTENANCE,
    
    /**
     * Service is starting up.
     */
    STARTING,
    
    /**
     * Service is shutting down.
     */
    STOPPING
}