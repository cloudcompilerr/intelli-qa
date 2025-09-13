package com.agentic.e2etester.cicd;

/**
 * Types of deployment events that can trigger automated testing.
 */
public enum DeploymentEventType {
    
    /**
     * Deployment has started but not yet completed.
     */
    DEPLOYMENT_STARTED,
    
    /**
     * Deployment has completed successfully.
     */
    DEPLOYMENT_COMPLETED,
    
    /**
     * Deployment has failed.
     */
    DEPLOYMENT_FAILED,
    
    /**
     * Deployment has been rolled back.
     */
    DEPLOYMENT_ROLLBACK,
    
    /**
     * Pre-deployment validation phase.
     */
    PRE_DEPLOYMENT,
    
    /**
     * Post-deployment validation phase.
     */
    POST_DEPLOYMENT,
    
    /**
     * Smoke test phase after deployment.
     */
    SMOKE_TEST,
    
    /**
     * Full regression test phase.
     */
    REGRESSION_TEST
}