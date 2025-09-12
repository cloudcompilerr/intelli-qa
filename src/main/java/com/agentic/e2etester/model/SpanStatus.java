package com.agentic.e2etester.model;

/**
 * Enumeration of possible statuses for a trace span.
 */
public enum SpanStatus {
    /**
     * Span is currently active
     */
    ACTIVE,
    
    /**
     * Span has finished successfully
     */
    FINISHED,
    
    /**
     * Span has finished with an error
     */
    ERROR,
    
    /**
     * Span was cancelled
     */
    CANCELLED
}