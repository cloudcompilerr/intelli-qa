package com.agentic.e2etester.recovery.strategies;

import com.agentic.e2etester.recovery.DegradationLevel;
import com.agentic.e2etester.recovery.DegradationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Degradation strategy that skips non-critical operations
 */
@Component
public class SkipNonCriticalDegradationStrategy implements DegradationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(SkipNonCriticalDegradationStrategy.class);
    
    private final Set<String> supportedOperations = Set.of(
        "collectMetrics", "sendNotification", "updateCache", "logEvent", "validateOptionalData"
    );
    
    @Override
    public DegradationLevel getDegradationLevel() {
        return DegradationLevel.MODERATE;
    }
    
    @Override
    public boolean canHandle(Throwable failure, String serviceId) {
        // Can handle any type of failure for non-critical operations
        return true;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> executeDegraded(
            String operationName, 
            String serviceId, 
            Throwable originalFailure, 
            Object... parameters) {
        
        logger.info("Skipping non-critical operation {} on service {} due to: {}", 
            operationName, serviceId, originalFailure.getMessage());
        
        // Return appropriate "skipped" response based on operation type
        T skippedResponse = getSkippedResponse(operationName);
        return CompletableFuture.completedFuture(skippedResponse);
    }
    
    @Override
    public Set<String> getSupportedOperations() {
        return supportedOperations;
    }
    
    @Override
    public String getDescription() {
        return "Skip non-critical operations - returns success responses for non-essential operations";
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getSkippedResponse(String operationName) {
        return switch (operationName) {
            case "collectMetrics", "sendNotification", "updateCache", "logEvent" -> 
                (T) Boolean.TRUE; // Indicate operation was "successful" (skipped)
            case "validateOptionalData" -> 
                (T) Boolean.TRUE; // Skip validation, assume valid
            default -> (T) Boolean.TRUE;
        };
    }
}