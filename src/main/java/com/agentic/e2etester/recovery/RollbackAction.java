package com.agentic.e2etester.recovery;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for rollback actions
 */
public interface RollbackAction {
    
    /**
     * Get a unique identifier for this rollback action
     */
    String getId();
    
    /**
     * Get a description of what this rollback action does
     */
    String getDescription();
    
    /**
     * Execute the rollback action
     */
    CompletableFuture<Void> execute();
    
    /**
     * Check if this rollback action can be safely executed
     */
    boolean canExecute();
    
    /**
     * Get the priority of this rollback action (higher numbers execute first)
     */
    int getPriority();
    
    /**
     * Get the service ID this rollback action affects
     */
    String getServiceId();
}