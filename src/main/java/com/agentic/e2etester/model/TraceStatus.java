package com.agentic.e2etester.model;

/**
 * Enumeration of possible statuses for a correlation trace.
 */
public enum TraceStatus {
    /**
     * Trace is currently active and collecting spans
     */
    ACTIVE,
    
    /**
     * Trace has completed successfully
     */
    COMPLETED,
    
    /**
     * Trace has failed due to an error
     */
    FAILED,
    
    /**
     * Trace was cancelled before completion
     */
    CANCELLED,
    
    /**
     * Trace has timed out
     */
    TIMEOUT
}