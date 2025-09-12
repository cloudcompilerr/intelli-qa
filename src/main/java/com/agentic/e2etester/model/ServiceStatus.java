package com.agentic.e2etester.model;

/**
 * Represents the health status of a discovered service
 */
public enum ServiceStatus {
    /**
     * Service is healthy and responding normally
     */
    HEALTHY,
    
    /**
     * Service is responding but may have degraded performance
     */
    DEGRADED,
    
    /**
     * Service is not responding or returning errors
     */
    UNHEALTHY,
    
    /**
     * Service status is unknown (not yet checked or unreachable)
     */
    UNKNOWN,
    
    /**
     * Service is temporarily unavailable (maintenance, deployment, etc.)
     */
    MAINTENANCE
}