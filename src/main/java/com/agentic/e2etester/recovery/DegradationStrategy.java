package com.agentic.e2etester.recovery;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for graceful degradation
 */
public interface DegradationStrategy {
    
    /**
     * Get the degradation level this strategy handles
     */
    DegradationLevel getDegradationLevel();
    
    /**
     * Check if this strategy can handle the given failure
     */
    boolean canHandle(Throwable failure, String serviceId);
    
    /**
     * Execute the degraded operation
     */
    <T> CompletableFuture<T> executeDegraded(
        String operationName,
        String serviceId,
        Throwable originalFailure,
        Object... parameters
    );
    
    /**
     * Get the set of operations this strategy can degrade
     */
    Set<String> getSupportedOperations();
    
    /**
     * Get a description of what this degradation strategy does
     */
    String getDescription();
}