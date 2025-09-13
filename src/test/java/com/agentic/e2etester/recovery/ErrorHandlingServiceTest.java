package com.agentic.e2etester.recovery;

import com.agentic.e2etester.model.FailureSeverity;
import com.agentic.e2etester.model.FailureType;
import com.agentic.e2etester.model.RetryPolicy;
import com.agentic.e2etester.model.TestFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingServiceTest {
    
    private ErrorHandlingService errorHandlingService;
    private GracefulDegradationManager degradationManager;
    private RollbackManager rollbackManager;
    private ScheduledExecutorService scheduler;
    
    @BeforeEach
    void setUp() {
        scheduler = Executors.newScheduledThreadPool(2);
        degradationManager = new GracefulDegradationManager();
        rollbackManager = new RollbackManager();
        
        errorHandlingService = new ErrorHandlingService(scheduler, degradationManager, rollbackManager);
    }
    
    @Test
    void shouldExecuteSuccessfulOperationWithoutErrorHandling() {
        RetryPolicy retryPolicy = new RetryPolicy(3, 10L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(3, 2, Duration.ofSeconds(1));
        
        CompletableFuture<String> result = errorHandlingService.executeWithErrorHandling(
            "test-operation",
            "test-service",
            () -> CompletableFuture.completedFuture("success"),
            retryPolicy,
            circuitConfig
        );
        
        assertEquals("success", result.join());
    }
    
    @Test
    void shouldRetryFailedOperation() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RetryPolicy retryPolicy = new RetryPolicy(3, 10L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(5, 2, Duration.ofSeconds(1));
        
        CompletableFuture<String> result = errorHandlingService.executeWithErrorHandling(
            "test-operation",
            "test-service",
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    return CompletableFuture.failedFuture(
                        new java.net.ConnectException("Connection failed"));
                }
                return CompletableFuture.completedFuture("success after retries");
            },
            retryPolicy,
            circuitConfig
        );
        
        assertEquals("success after retries", result.join());
        assertEquals(3, attemptCount.get());
    }
    
    @Test
    void shouldTripCircuitBreakerAfterFailures() {
        RetryPolicy retryPolicy = new RetryPolicy(1, 10L); // No retries
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(2, 2, Duration.ofSeconds(1));
        
        // Cause failures to trip circuit breaker
        for (int i = 0; i < 2; i++) {
            try {
                errorHandlingService.executeWithErrorHandling(
                    "test-operation",
                    "test-service",
                    () -> CompletableFuture.failedFuture(new RuntimeException("Service failure")),
                    retryPolicy,
                    circuitConfig
                ).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        CircuitBreaker circuitBreaker = errorHandlingService.getCircuitBreaker("test-service");
        assertNotNull(circuitBreaker);
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }
    
    @Test
    void shouldHandleTestFailureWithRollback() {
        // Register rollback action
        TestRollbackAction rollbackAction = new TestRollbackAction("rollback1", "test-service", 10);
        rollbackManager.registerRollbackAction("test1", rollbackAction);
        
        TestFailure failure = new TestFailure("failure1", "test1", "step1", 
            FailureType.SERVICE_FAILURE, FailureSeverity.HIGH, "Service unavailable");
        failure.setServiceId("test-service");
        
        RecoveryResult result = errorHandlingService.handleTestFailure("test1", failure, true).join();
        
        assertNotNull(result);
        assertEquals("test1", result.getTestId());
        assertTrue(result.hasRollback());
        assertTrue(result.isRollbackSuccessful());
        assertTrue(rollbackAction.wasExecuted());
    }
    
    @Test
    void shouldApplyDegradationBasedOnFailureType() {
        TestFailure networkFailure = new TestFailure("failure1", "test1", "step1",
            FailureType.NETWORK_FAILURE, FailureSeverity.MEDIUM, "Network timeout");
        networkFailure.setServiceId("test-service");
        
        errorHandlingService.handleTestFailure("test1", networkFailure, false).join();
        
        assertEquals(DegradationLevel.MINIMAL, 
            errorHandlingService.getServiceDegradationLevel("test-service"));
        assertTrue(errorHandlingService.isServiceDegraded("test-service"));
    }
    
    @Test
    void shouldApplyMoreSevereDegradationForCriticalFailures() {
        TestFailure authFailure = new TestFailure("failure1", "test1", "step1",
            FailureType.AUTHENTICATION_FAILURE, FailureSeverity.CRITICAL, "Auth failed");
        authFailure.setServiceId("test-service");
        
        errorHandlingService.handleTestFailure("test1", authFailure, false).join();
        
        assertEquals(DegradationLevel.CRITICAL, 
            errorHandlingService.getServiceDegradationLevel("test-service"));
    }
    
    @Test
    void shouldResetCircuitBreaker() {
        RetryPolicy retryPolicy = new RetryPolicy(1, 10L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(1, 2, Duration.ofSeconds(1));
        
        // Trip circuit breaker
        try {
            errorHandlingService.executeWithErrorHandling(
                "test-operation",
                "test-service",
                () -> CompletableFuture.failedFuture(new RuntimeException("Service failure")),
                retryPolicy,
                circuitConfig
            ).join();
        } catch (CompletionException e) {
            // Expected
        }
        
        CircuitBreaker circuitBreaker = errorHandlingService.getCircuitBreaker("test-service");
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Reset circuit breaker
        errorHandlingService.resetCircuitBreaker("test-service");
        
        CircuitBreaker newCircuitBreaker = errorHandlingService.getCircuitBreaker("test-service");
        assertEquals(CircuitBreakerState.CLOSED, newCircuitBreaker.getState());
    }
    
    @Test
    void shouldManageServiceDegradation() {
        assertFalse(errorHandlingService.isServiceDegraded("test-service"));
        assertEquals(DegradationLevel.NONE, errorHandlingService.getServiceDegradationLevel("test-service"));
        
        errorHandlingService.setServiceDegradationLevel("test-service", DegradationLevel.MODERATE);
        
        assertTrue(errorHandlingService.isServiceDegraded("test-service"));
        assertEquals(DegradationLevel.MODERATE, errorHandlingService.getServiceDegradationLevel("test-service"));
        
        errorHandlingService.resetServiceDegradation("test-service");
        
        assertFalse(errorHandlingService.isServiceDegraded("test-service"));
        assertEquals(DegradationLevel.NONE, errorHandlingService.getServiceDegradationLevel("test-service"));
    }
    
    @Test
    void shouldProvideRecoveryStatistics() {
        // Create some circuit breakers and degraded services
        RetryPolicy retryPolicy = new RetryPolicy(1, 10L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(1, 2, Duration.ofSeconds(1));
        
        // Trip one circuit breaker
        try {
            errorHandlingService.executeWithErrorHandling(
                "test-operation",
                "service1",
                () -> CompletableFuture.failedFuture(new RuntimeException("Service failure")),
                retryPolicy,
                circuitConfig
            ).join();
        } catch (CompletionException e) {
            // Expected
        }
        
        // Create another circuit breaker (but don't trip it)
        errorHandlingService.executeWithErrorHandling(
            "test-operation",
            "service2",
            () -> CompletableFuture.completedFuture("success"),
            retryPolicy,
            circuitConfig
        ).join();
        
        // Set degradation for a service
        errorHandlingService.setServiceDegradationLevel("service3", DegradationLevel.MODERATE);
        
        // Register rollback actions
        rollbackManager.registerRollbackAction("test1", new TestRollbackAction("action1", "service1", 10));
        
        RecoveryStatistics stats = errorHandlingService.getRecoveryStatistics();
        
        assertEquals(2, stats.getTotalCircuitBreakers());
        assertEquals(1, stats.getOpenCircuitBreakers());
        assertEquals(1, stats.getDegradedServices());
        assertEquals(1, stats.getTestsWithRollbackActions());
        assertEquals(50.0, stats.getCircuitBreakerHealthPercentage());
    }
    
    @Test
    void shouldGetAllCircuitBreakers() {
        RetryPolicy retryPolicy = new RetryPolicy(3, 10L);
        CircuitBreakerConfiguration circuitConfig = new CircuitBreakerConfiguration(3, 2, Duration.ofSeconds(1));
        
        // Create circuit breakers by executing operations
        errorHandlingService.executeWithErrorHandling(
            "operation1", "service1", 
            () -> CompletableFuture.completedFuture("success"),
            retryPolicy, circuitConfig
        ).join();
        
        errorHandlingService.executeWithErrorHandling(
            "operation2", "service2",
            () -> CompletableFuture.completedFuture("success"),
            retryPolicy, circuitConfig
        ).join();
        
        var circuitBreakers = errorHandlingService.getAllCircuitBreakers();
        
        assertEquals(2, circuitBreakers.size());
        assertTrue(circuitBreakers.containsKey("service1"));
        assertTrue(circuitBreakers.containsKey("service2"));
    }
    
    // Test helper class
    private static class TestRollbackAction implements RollbackAction {
        private final String id;
        private final String serviceId;
        private final int priority;
        private boolean executed = false;
        
        public TestRollbackAction(String id, String serviceId, int priority) {
            this.id = id;
            this.serviceId = serviceId;
            this.priority = priority;
        }
        
        @Override
        public String getId() { return id; }
        
        @Override
        public String getDescription() { return "Test rollback: " + id; }
        
        @Override
        public CompletableFuture<Void> execute() {
            executed = true;
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        public boolean canExecute() { return true; }
        
        @Override
        public int getPriority() { return priority; }
        
        @Override
        public String getServiceId() { return serviceId; }
        
        public boolean wasExecuted() { return executed; }
    }
}