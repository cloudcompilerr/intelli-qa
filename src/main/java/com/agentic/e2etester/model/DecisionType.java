package com.agentic.e2etester.model;

/**
 * Enumeration of decision types that the AI agent can make during test execution.
 */
public enum DecisionType {
    
    /**
     * Continue with the current test execution plan.
     */
    CONTINUE,
    
    /**
     * Retry the current step or operation.
     */
    RETRY,
    
    /**
     * Adapt the test flow based on current conditions.
     */
    ADAPT,
    
    /**
     * Skip the current step and proceed to the next.
     */
    SKIP,
    
    /**
     * Pause the test execution temporarily.
     */
    PAUSE,
    
    /**
     * Abort the test execution due to critical issues.
     */
    ABORT,
    
    /**
     * Escalate the issue to human intervention.
     */
    ESCALATE,
    
    /**
     * Rollback to a previous state or checkpoint.
     */
    ROLLBACK,
    
    /**
     * Switch to an alternative execution path.
     */
    SWITCH_PATH,
    
    /**
     * Collect additional information before proceeding.
     */
    INVESTIGATE
}