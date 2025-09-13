package com.agentic.e2etester.recovery;

import com.agentic.e2etester.model.RetryPolicy;
import com.agentic.e2etester.model.TestFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Comprehensive error handling and recovery service
 */
@Service
public class ErrorHandlingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingService.class);
    
    private final RetryExecutor retryExecutor;
    private final GracefulDegradationManager degradationManager;
    private final RollbackManager rollbackManager;
    private final Map<String, CircuitBreaker> circuitBreakers;
    
    public ErrorHandlingService(
            ScheduledExecutorService scheduledExecutorService,
            GracefulDegradationManager degradationManager,
            RollbackManager rollbackManager) {
        
        this.retryExecutor = new RetryExecutor(scheduledExecutorService);
        this.degradationManager = degradationManager;
        this.rollbackManager = rollbackManager;
        this.circuitBreakers = new ConcurrentHashMap<>();
    }
    
    /**
     * Execute an operation with comprehensive error handling
     */
    public <T> CompletableFuture<T> executeWithErrorHandling(
            String operationName,
            String serviceId,
            Supplier<CompletableFuture<T>> operation,
            RetryPolicy retryPolicy,
            CircuitBreakerConfiguration circuitBreakerConfig) {
        
        // Get or create circuit breaker for the service
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceId, circuitBreakerConfig);
        
        // Execute with circuit breaker protection
        return circuitBreaker.execute(() -> 
            // Execute with retry logic
            retryExecutor.executeWithRetry(operation, retryPolicy, operationName)
        ).exceptionallyCompose(throwable -> {
            logger.warn("Operation {} failed on service {} after retries and circuit breaker", 
                operationName, serviceId, throwable);
            
            // Attempt graceful degradation
            return degradationManager.executeWithDegradation(operationName, serviceId, throwable);
        });
    }
    
    /**
     * Handle test failure with rollback
     */
    public CompletableFuture<RecoveryResult> handleTestFailure(
            String testId,
            TestFailure failure,
            boolean performRollback) {
        
        logger.error("Handling test failure for test {}: {}", testId, failure.getErrorMessage());
        
        RecoveryResult.Builder resultBuilder = new RecoveryResult.Builder(testId, failure);
        
        CompletableFuture<RecoveryResult> future = CompletableFuture.completedFuture(resultBuilder.build());
        
        // Perform rollback if requested and actions are available
        if (performRollback && rollbackManager.hasRollbackActions(testId)) {
            future = future.thenCompose(result -> 
                rollbackManager.executeRollback(testId)
                    .thenApply(rollbackResult -> 
                        resultBuilder.withRollbackResult(rollbackResult).build()
                    )
            );
        }
        
        // Apply degradation based on failure type
        future = future.thenApply(result -> {
            applyDegradationForFailure(failure);
            return resultBuilder.withDegradationApplied(true).build();
        });
        
        return future;
    }
    
    /**
     * Get circuit breaker for a service
     */
    public CircuitBreaker getCircuitBreaker(String serviceId) {
        return circuitBreakers.get(serviceId);
    }
    
    /**
     * Get all circuit breakers
     */
    public Map<String, CircuitBreaker> getAllCircuitBreakers() {
        return Map.copyOf(circuitBreakers);
    }
    
    /**
     * Reset circuit breaker for a service
     */
    public void resetCircuitBreaker(String serviceId) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceId);
        if (circuitBreaker != null) {
            // Create a new circuit breaker to reset state
            CircuitBreakerConfiguration config = circuitBreaker.getConfiguration();
            circuitBreakers.put(serviceId, new CircuitBreaker(serviceId, config));
            logger.info("Reset circuit breaker for service {}", serviceId);
        }
    }
    
    /**
     * Check if a service is currently degraded
     */
    public boolean isServiceDegraded(String serviceId) {
        return degradationManager.getCurrentDegradationLevel(serviceId) != DegradationLevel.NONE;
    }
    
    /**
     * Get current degradation level for a service
     */
    public DegradationLevel getServiceDegradationLevel(String serviceId) {
        return degradationManager.getCurrentDegradationLevel(serviceId);
    }
    
    /**
     * Manually set degradation level for a service
     */
    public void setServiceDegradationLevel(String serviceId, DegradationLevel level) {
        degradationManager.setServiceDegradationLevel(serviceId, level);
    }
    
    /**
     * Reset degradation for a service
     */
    public void resetServiceDegradation(String serviceId) {
        degradationManager.resetServiceDegradationLevel(serviceId);
    }
    
    /**
     * Get recovery statistics
     */
    public RecoveryStatistics getRecoveryStatistics() {
        return new RecoveryStatistics(
            circuitBreakers.size(),
            (int) circuitBreakers.values().stream().filter(cb -> cb.getState() == CircuitBreakerState.OPEN).count(),
            degradationManager.getDegradedServices().size(),
            rollbackManager.getTestsWithRollbackActions().size()
        );
    }
    
    private CircuitBreaker getOrCreateCircuitBreaker(String serviceId, CircuitBreakerConfiguration config) {
        return circuitBreakers.computeIfAbsent(serviceId, id -> {
            logger.info("Creating circuit breaker for service {} with config: failure threshold={}, timeout={}s", 
                id, config.getFailureThreshold(), config.getTimeout().getSeconds());
            return new CircuitBreaker(id, config);
        });
    }
    
    private void applyDegradationForFailure(TestFailure failure) {
        String serviceId = failure.getServiceId();
        if (serviceId == null) return;
        
        // Determine degradation level based on failure type and severity
        DegradationLevel level = determineDegradationLevel(failure);
        
        if (level != DegradationLevel.NONE) {
            degradationManager.setServiceDegradationLevel(serviceId, level);
            logger.info("Applied degradation level {} to service {} due to failure: {}", 
                level, serviceId, failure.getErrorMessage());
        }
    }
    
    private DegradationLevel determineDegradationLevel(TestFailure failure) {
        return switch (failure.getFailureType()) {
            case NETWORK_FAILURE, TIMEOUT_FAILURE -> DegradationLevel.MINIMAL;
            case SERVICE_FAILURE, INFRASTRUCTURE_FAILURE -> DegradationLevel.MODERATE;
            case DATA_FAILURE, BUSINESS_LOGIC_FAILURE -> DegradationLevel.SEVERE;
            case AUTHENTICATION_FAILURE, CONFIGURATION_FAILURE -> DegradationLevel.CRITICAL;
            default -> DegradationLevel.MINIMAL;
        };
    }
}