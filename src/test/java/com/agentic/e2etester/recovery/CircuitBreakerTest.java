package com.agentic.e2etester.recovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {
    
    private CircuitBreakerConfiguration config;
    private CircuitBreaker circuitBreaker;
    
    @BeforeEach
    void setUp() {
        config = new CircuitBreakerConfiguration(3, 2, Duration.ofSeconds(1));
        config.setRecoveryTimeout(Duration.ofMillis(100));
        circuitBreaker = new CircuitBreaker("test-service", config);
    }
    
    @Test
    void shouldStartInClosedState() {
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }
    
    @Test
    void shouldExecuteSuccessfulOperationInClosedState() {
        CompletableFuture<String> result = circuitBreaker.execute(() -> 
            CompletableFuture.completedFuture("success"));
        
        assertEquals("success", result.join());
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
    }
    
    @Test
    void shouldTripToOpenStateAfterFailureThreshold() {
        // Cause failures to reach threshold
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> 
                    CompletableFuture.failedFuture(new RuntimeException("test failure"))).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }
    
    @Test
    void shouldBlockRequestsInOpenState() {
        // Trip the circuit breaker
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> 
                    CompletableFuture.failedFuture(new RuntimeException("test failure"))).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        // Now requests should be blocked
        CompletableFuture<String> result = circuitBreaker.execute(() -> 
            CompletableFuture.completedFuture("should not execute"));
        
        assertThrows(CompletionException.class, result::join);
        assertTrue(result.isCompletedExceptionally());
    }
    
    @Test
    void shouldTransitionToHalfOpenAfterRecoveryTimeout() throws InterruptedException {
        // Trip the circuit breaker
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> 
                    CompletableFuture.failedFuture(new RuntimeException("test failure"))).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Wait for recovery timeout
        Thread.sleep(150);
        
        // Next request should attempt to transition to half-open
        CompletableFuture<String> result = circuitBreaker.execute(() -> 
            CompletableFuture.completedFuture("recovery test"));
        
        assertEquals("recovery test", result.join());
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void shouldCloseAfterSuccessfulRequestsInHalfOpenState() throws InterruptedException {
        // Trip the circuit breaker
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> 
                    CompletableFuture.failedFuture(new RuntimeException("test failure"))).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        // Wait for recovery timeout
        Thread.sleep(150);
        
        // Execute successful requests to close the circuit
        for (int i = 0; i < 2; i++) {
            CompletableFuture<String> result = circuitBreaker.execute(() -> 
                CompletableFuture.completedFuture("success " + i));
            assertEquals("success " + i, result.join());
        }
        
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void shouldResetFailureCountOnSuccess() {
        // Cause some failures (but not enough to trip)
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.execute(() -> 
                    CompletableFuture.failedFuture(new RuntimeException("test failure"))).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        assertEquals(2, circuitBreaker.getFailureCount());
        
        // Execute successful request
        CompletableFuture<String> result = circuitBreaker.execute(() -> 
            CompletableFuture.completedFuture("success"));
        
        assertEquals("success", result.join());
        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void shouldHandleConcurrentRequests() {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Execute multiple concurrent requests
        CompletableFuture<?>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures[i] = circuitBreaker.execute(() -> {
                if (index % 2 == 0) {
                    return CompletableFuture.completedFuture("success " + index);
                } else {
                    return CompletableFuture.failedFuture(new RuntimeException("failure " + index));
                }
            }).handle((result, throwable) -> {
                if (throwable != null) {
                    failureCount.incrementAndGet();
                } else {
                    successCount.incrementAndGet();
                }
                return null;
            });
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures).join();
        
        assertEquals(5, successCount.get());
        assertEquals(5, failureCount.get());
    }
    
    @Test
    void shouldRespectDisabledConfiguration() {
        config.setEnabled(false);
        CircuitBreaker disabledBreaker = new CircuitBreaker("disabled-service", config);
        
        // Should execute even with failures
        for (int i = 0; i < 5; i++) {
            try {
                disabledBreaker.execute(() -> 
                    CompletableFuture.failedFuture(new RuntimeException("test failure"))).join();
            } catch (CompletionException e) {
                // Expected
            }
        }
        
        // Should still be closed since circuit breaker is disabled
        assertEquals(CircuitBreakerState.CLOSED, disabledBreaker.getState());
    }
}