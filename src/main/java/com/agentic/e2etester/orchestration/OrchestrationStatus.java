package com.agentic.e2etester.orchestration;

/**
 * Status enumeration for test orchestration lifecycle
 */
public enum OrchestrationStatus {
    
    /**
     * Orchestration has been created but not yet started
     */
    INITIALIZED,
    
    /**
     * Orchestration is actively running
     */
    RUNNING,
    
    /**
     * Orchestration has been paused and can be resumed
     */
    PAUSED,
    
    /**
     * Orchestration completed successfully
     */
    COMPLETED,
    
    /**
     * Orchestration failed with errors
     */
    FAILED,
    
    /**
     * Orchestration was cancelled by user
     */
    CANCELLED,
    
    /**
     * Orchestration ID not found
     */
    NOT_FOUND
}