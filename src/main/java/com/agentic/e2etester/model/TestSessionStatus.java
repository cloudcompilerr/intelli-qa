package com.agentic.e2etester.model;

/**
 * Enumeration of test session statuses managed by the AI agent.
 */
public enum TestSessionStatus {
    
    /**
     * Session has been created but not yet started.
     */
    INITIALIZED,
    
    /**
     * Session is actively running tests.
     */
    RUNNING,
    
    /**
     * Session is temporarily paused.
     */
    PAUSED,
    
    /**
     * Session completed successfully.
     */
    COMPLETED,
    
    /**
     * Session failed due to errors.
     */
    FAILED,
    
    /**
     * Session was cancelled by user or system.
     */
    CANCELLED,
    
    /**
     * Session timed out.
     */
    TIMEOUT,
    
    /**
     * Session is waiting for external input or conditions.
     */
    WAITING
}