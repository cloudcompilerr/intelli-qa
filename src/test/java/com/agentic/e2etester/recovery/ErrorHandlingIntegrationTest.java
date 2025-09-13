package com.agentic.e2etester.recovery;

import com.agentic.e2etester.model.FailureSeverity;
import com.agentic.e2etester.model.FailureType;
import com.agentic.e2etester.model.RetryPolicy;
import com.agentic.e2etester.model.TestFailure;
import com.agentic.e2etester.recovery.actions.DatabaseRollbackAction;
import com.agentic.e2etester.recovery.actions.KafkaRollbackAction;
import com.agentic.e2etester.recovery.strategies.CacheBasedDegradationStrategy;
import com.agentic.e2etester.recovery.strategies.SkipNonCriticalDegradationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete error handling and recovery system
 */
class ErrorHandlingIntegrationTest {
    
    private ErrorHandlingService errorHandlingService;
    private GracefulDegradationManager degradationManager;
    private RollbackManager rollbackManager;
    private CacheBasedDegradationStrategy cacheStrategy;
    private SkipNonCriticalDegradationStrategy skipStrategy;
    
    @BeforeEach
    void setUp() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        degradationManager = new GracefulDegradationManager();
        rollbackManager = new RollbackManager();
        
        // Set up degradation strategies
        cacheStrategy = new CacheBasedDegradationStrategy();
        skipStrategy = new SkipNonCriticalDegradationStrategy();
        
        degradationManager.registerStrategy(cacheStrategy);
        degradationManager.registerStrategy(skipStrategy);
        
        errorHandlingService = new ErrorHandlingService(scheduler, degradationManager, rollbackManager);
    }
    
    @Test
    void shouldHandleCompleteOrderFulfillmentFailureScenario() {
        String testId = "order-fulfillment-test";
        
        // Register rollback actions for different services
        rollbackManager.registerRollbackAction(testId, 
            new DatabaseRollbackAction("db-rollback-1", "order-service", "INSERT", 
                "order-data", 100));
        rollbackManager.registerRollbackAction(testId,
            new KafkaRollbackAction("kafka-rollback-1", "inventory-service", 
                "inventory-updates", "msg-123", 90));
        rollbackManager.registerRollbackAction(testId,
            new DatabaseRollbackAction("db-rollback-2", "payment-service", "UPDATE",
                "payment-status", 80));
        
        // Simulate a critical failure in the payment service
        TestFailure paymentFailure = new TestFailure("payment-failure", testId, "payment-step",
            FailureType.BUSINESS_LOGIC_FAILURE, FailureSeverity.CRITICAL, 
            "Payment processing failed - insufficient funds");
        paymentFailure.setServiceId("payment-service");
        
        // Handle the failure with rollback
        RecoveryResult result = errorHandlingService.handleTestFailure(testId, paymentFailure, true).join();
        
        // Verify recovery was successful
        assertTrue(result.hasRollback());
        assertTrue(result.isRollbackSuccessful());
        assertEquals(3, result.getRollbackResult().getSuccessfulActionCount());
        assertEquals(0, result.getRollbackResult().getFailedActionCount());
        
        // Verify degradation was applied
        assertEquals(DegradationLevel.SEVERE, 
            errorHandlingService.getServiceDegradationLevel("payment-service"));
    }
    
    @Test
    void shouldHandleNetworkFailuresWithRetryAndDegradation() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryPolicy retryPolicy = new RetryPolicy(3, 50L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(5, 2, Duration.ofSeconds(2));
        
        // Cache a response for degradation
        cacheStrategy.cacheResponse("inventory-service", "getServiceHealth", 
            java.util.Map.of("status", "CACHED", "serviceId", "inventory-service"));
        
        CompletableFuture<Object> result = errorHandlingService.executeWithErrorHandling(
            "getServiceHealth",
            "inventory-service",
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt <= 3) {
                    // Fail all retry attempts
                    return CompletableFuture.failedFuture(
                        new java.net.ConnectException("Connection refused"));
                }
                return CompletableFuture.completedFuture("should not reach here");
            },
            retryPolicy,
            circuitConfig
        );
        
        // Should get cached response through degradation
        Object response = result.join();
        assertNotNull(response);
        assertTrue(response instanceof java.util.Map);
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> responseMap = (java.util.Map<String, Object>) response;
        assertEquals("CACHED", responseMap.get("status"));
        
        // Verify retries were attempted
        assertEquals(3, attemptCount.get());
        
        // Verify degradation was applied
        assertEquals(DegradationLevel.MINIMAL, 
            errorHandlingService.getServiceDegradationLevel("inventory-service"));
    }
    
    @Test
    void shouldSkipNonCriticalOperationsUnderModerateDegradation() {
        // Set moderate degradation level
        errorHandlingService.setServiceDegradationLevel("notification-service", DegradationLevel.MODERATE);
        
        RetryPolicy retryPolicy = new RetryPolicy(2, 10L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(3, 2, Duration.ofSeconds(1));
        
        CompletableFuture<Boolean> result = errorHandlingService.executeWithErrorHandling(
            "sendNotification",
            "notification-service",
            () -> CompletableFuture.failedFuture(new RuntimeException("Service unavailable")),
            retryPolicy,
            circuitConfig
        );
        
        // Should get "skipped" response (true) instead of failure
        assertTrue(result.join());
    }
    
    @Test
    void shouldHandleCascadingFailuresAcrossMultipleServices() {
        String testId = "cascading-failure-test";
        
        // Register rollback actions for multiple services
        rollbackManager.registerRollbackAction(testId,
            new DatabaseRollbackAction("order-rollback", "order-service", "INSERT", "order", 100));
        rollbackManager.registerRollbackAction(testId,
            new DatabaseRollbackAction("inventory-rollback", "inventory-service", "UPDATE", "stock", 90));
        rollbackManager.registerRollbackAction(testId,
            new KafkaRollbackAction("shipping-rollback", "shipping-service", "shipping-events", "ship-123", 80));
        
        // Simulate cascading failures
        TestFailure orderFailure = new TestFailure("order-failure", testId, "order-creation",
            FailureType.SERVICE_FAILURE, FailureSeverity.HIGH, "Order service unavailable");
        orderFailure.setServiceId("order-service");
        
        TestFailure inventoryFailure = new TestFailure("inventory-failure", testId, "inventory-check",
            FailureType.DATA_FAILURE, FailureSeverity.MEDIUM, "Inventory data inconsistent");
        inventoryFailure.setServiceId("inventory-service");
        
        // Handle first failure
        RecoveryResult orderResult = errorHandlingService.handleTestFailure(testId, orderFailure, false).join();
        assertTrue(orderResult.isDegradationApplied());
        
        // Handle second failure
        RecoveryResult inventoryResult = errorHandlingService.handleTestFailure(testId, inventoryFailure, false).join();
        assertTrue(inventoryResult.isDegradationApplied());
        
        // Execute final rollback for all affected services
        RollbackResult rollbackResult = rollbackManager.executeRollback(testId).join();
        assertTrue(rollbackResult.isSuccessful());
        assertEquals(3, rollbackResult.getSuccessfulActionCount());
        
        // Verify degradation levels
        assertEquals(DegradationLevel.MODERATE, 
            errorHandlingService.getServiceDegradationLevel("order-service"));
        assertEquals(DegradationLevel.SEVERE, 
            errorHandlingService.getServiceDegradationLevel("inventory-service"));
    }
    
    @Test
    void shouldRecoverFromCircuitBreakerOpenState() throws InterruptedException {
        RetryPolicy retryPolicy = new RetryPolicy(1, 10L); // No retries to trip circuit faster
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(2, 1, Duration.ofSeconds(1));
        circuitConfig.setRecoveryTimeout(Duration.ofMillis(100));
        
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        // Trip the circuit breaker
        for (int i = 0; i < 2; i++) {
            try {
                errorHandlingService.executeWithErrorHandling(
                    "test-operation",
                    "flaky-service",
                    () -> {
                        attemptCount.incrementAndGet();
                        return CompletableFuture.failedFuture(new RuntimeException("Service failure"));
                    },
                    retryPolicy,
                    circuitConfig
                ).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        // Verify circuit is open
        CircuitBreaker circuitBreaker = errorHandlingService.getCircuitBreaker("flaky-service");
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Wait for recovery timeout
        Thread.sleep(150);
        
        // Next request should succeed and close the circuit
        CompletableFuture<String> result = errorHandlingService.executeWithErrorHandling(
            "test-operation",
            "flaky-service",
            () -> CompletableFuture.completedFuture("service recovered"),
            retryPolicy,
            circuitConfig
        );
        
        assertEquals("service recovered", result.join());
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void shouldProvideComprehensiveRecoveryStatistics() {
        // Create various error conditions
        
        // 1. Trip some circuit breakers
        RetryPolicy retryPolicy = new RetryPolicy(1, 10L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(1, 1, Duration.ofSeconds(1));
        
        try {
            errorHandlingService.executeWithErrorHandling(
                "operation", "service1",
                () -> CompletableFuture.failedFuture(new RuntimeException("failure")),
                retryPolicy, circuitConfig
            ).join();
        } catch (CompletionException e) { /* expected */ }
        
        try {
            errorHandlingService.executeWithErrorHandling(
                "operation", "service2",
                () -> CompletableFuture.failedFuture(new RuntimeException("failure")),
                retryPolicy, circuitConfig
            ).join();
        } catch (CompletionException e) { /* expected */ }
        
        // 2. Create a healthy circuit breaker
        errorHandlingService.executeWithErrorHandling(
            "operation", "service3",
            () -> CompletableFuture.completedFuture("success"),
            retryPolicy, circuitConfig
        ).join();
        
        // 3. Set degradation levels
        errorHandlingService.setServiceDegradationLevel("service4", DegradationLevel.MODERATE);
        errorHandlingService.setServiceDegradationLevel("service5", DegradationLevel.SEVERE);
        
        // 4. Register rollback actions
        rollbackManager.registerRollbackAction("test1", 
            new DatabaseRollbackAction("action1", "service1", "INSERT", "data", 10));
        rollbackManager.registerRollbackAction("test2",
            new KafkaRollbackAction("action2", "service2", "topic", "msg", 10));
        
        // Get statistics
        RecoveryStatistics stats = errorHandlingService.getRecoveryStatistics();
        
        assertEquals(3, stats.getTotalCircuitBreakers());
        assertEquals(2, stats.getOpenCircuitBreakers());
        assertEquals(2, stats.getDegradedServices());
        assertEquals(2, stats.getTestsWithRollbackActions());
        
        // Health percentage should be 33.33% (1 healthy out of 3 total)
        assertEquals(33.33, stats.getCircuitBreakerHealthPercentage(), 0.01);
    }
    
    @Test
    void shouldHandleComplexErrorRecoveryWorkflow() {
        String testId = "complex-workflow-test";
        
        // Set up a complex scenario with multiple failure points
        rollbackManager.registerRollbackAction(testId,
            new DatabaseRollbackAction("user-rollback", "user-service", "INSERT", "user-data", 100));
        rollbackManager.registerRollbackAction(testId,
            new KafkaRollbackAction("event-rollback", "event-service", "user-events", "event-456", 90));
        
        // Cache some responses for degradation
        cacheStrategy.cacheResponse("user-service", "validateData", true);
        
        RetryPolicy retryPolicy = new RetryPolicy(2, 20L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(3, 2, Duration.ofSeconds(1));
        
        AtomicInteger userServiceCalls = new AtomicInteger(0);
        
        // Execute operation that will fail and trigger all recovery mechanisms
        CompletableFuture<Boolean> result = errorHandlingService.executeWithErrorHandling(
            "validateData",
            "user-service",
            () -> {
                int call = userServiceCalls.incrementAndGet();
                if (call <= 2) {
                    // Fail first two attempts (will trigger retries)
                    return CompletableFuture.failedFuture(
                        new java.net.SocketTimeoutException("Service timeout"));
                }
                // Third attempt will also fail, triggering degradation
                return CompletableFuture.failedFuture(
                    new RuntimeException("Service completely unavailable"));
            },
            retryPolicy,
            circuitConfig
        );
        
        // Should get cached response through degradation
        assertTrue(result.join());
        
        // Verify retries were attempted
        assertEquals(2, userServiceCalls.get());
        
        // Verify degradation was applied
        assertEquals(DegradationLevel.MINIMAL, 
            errorHandlingService.getServiceDegradationLevel("user-service"));
        
        // Simulate a critical failure that requires rollback
        TestFailure criticalFailure = new TestFailure("critical-failure", testId, "final-step",
            FailureType.BUSINESS_LOGIC_FAILURE, FailureSeverity.CRITICAL, "Critical business rule violation");
        criticalFailure.setServiceId("user-service");
        
        RecoveryResult recoveryResult = errorHandlingService.handleTestFailure(testId, criticalFailure, true).join();
        
        // Verify complete recovery
        assertTrue(recoveryResult.hasRollback());
        assertTrue(recoveryResult.isRollbackSuccessful());
        assertEquals(2, recoveryResult.getRollbackResult().getSuccessfulActionCount());
        
        // Verify final degradation level was upgraded due to critical failure
        assertEquals(DegradationLevel.SEVERE, 
            errorHandlingService.getServiceDegradationLevel("user-service"));
    }
}