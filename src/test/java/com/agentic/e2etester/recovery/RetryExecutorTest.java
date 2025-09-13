package com.agentic.e2etester.recovery;

import com.agentic.e2etester.model.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {
    
    private ScheduledExecutorService scheduler;
    private RetryExecutor retryExecutor;
    private RetryPolicy retryPolicy;
    
    @BeforeEach
    void setUp() {
        scheduler = Executors.newScheduledThreadPool(2);
        retryExecutor = new RetryExecutor(scheduler);
        retryPolicy = new RetryPolicy(3, 10L); // 3 attempts, 10ms initial delay
        retryPolicy.setBackoffMultiplier(2.0);
        retryPolicy.setMaxDelayMs(100L);
    }
    
    @Test
    void shouldSucceedOnFirstAttempt() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.completedFuture("success");
            },
            retryPolicy,
            "test-operation"
        );
        
        assertEquals("success", result.join());
        assertEquals(1, attemptCount.get());
    }
    
    @Test
    void shouldRetryOnRetryableException() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    return CompletableFuture.failedFuture(
                        new java.net.ConnectException("Connection failed"));
                }
                return CompletableFuture.completedFuture("success after retries");
            },
            retryPolicy,
            "test-operation"
        );
        
        assertEquals("success after retries", result.join());
        assertEquals(3, attemptCount.get());
    }
    
    @Test
    void shouldFailAfterMaxAttempts() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.failedFuture(
                    new java.net.SocketTimeoutException("Timeout"));
            },
            retryPolicy,
            "test-operation"
        );
        
        assertThrows(CompletionException.class, result::join);
        assertEquals(3, attemptCount.get()); // Should attempt 3 times
    }
    
    @Test
    void shouldNotRetryOnNonRetryableException() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Invalid argument"));
            },
            retryPolicy,
            "test-operation"
        );
        
        assertThrows(CompletionException.class, result::join);
        assertEquals(1, attemptCount.get()); // Should only attempt once
    }
    
    @Test
    void shouldUseCustomRetryPredicate() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Custom retryable exception"));
            },
            retryPolicy,
            "test-operation",
            throwable -> throwable instanceof IllegalStateException
        );
        
        assertThrows(CompletionException.class, result::join);
        assertEquals(3, attemptCount.get()); // Should retry with custom predicate
    }
    
    @Test
    void shouldRespectMaxDelay() {
        RetryPolicy longDelayPolicy = new RetryPolicy(3, 1000L);
        longDelayPolicy.setBackoffMultiplier(10.0);
        longDelayPolicy.setMaxDelayMs(50L); // Cap at 50ms
        
        AtomicInteger attemptCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    return CompletableFuture.failedFuture(
                        new java.net.ConnectException("Connection failed"));
                }
                return CompletableFuture.completedFuture("success");
            },
            longDelayPolicy,
            "test-operation"
        );
        
        assertEquals("success", result.join());
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Should complete in reasonable time due to max delay cap
        assertTrue(totalTime < 500, "Total time should be less than 500ms due to max delay cap");
        assertEquals(3, attemptCount.get());
    }
    
    @Test
    void shouldHandleZeroMaxAttempts() {
        RetryPolicy noRetryPolicy = new RetryPolicy(1, 10L); // Only 1 attempt
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.failedFuture(
                    new java.net.ConnectException("Connection failed"));
            },
            noRetryPolicy,
            "test-operation"
        );
        
        assertThrows(CompletionException.class, result::join);
        assertEquals(1, attemptCount.get()); // Should only attempt once
    }
    
    @Test
    void shouldApplyExponentialBackoff() {
        RetryPolicy backoffPolicy = new RetryPolicy(3, 10L);
        backoffPolicy.setBackoffMultiplier(2.0);
        
        AtomicInteger attemptCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<String> result = retryExecutor.executeWithRetry(
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    return CompletableFuture.failedFuture(
                        new java.net.ConnectException("Connection failed"));
                }
                return CompletableFuture.completedFuture("success");
            },
            backoffPolicy,
            "test-operation"
        );
        
        assertEquals("success", result.join());
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Should take at least the sum of delays (10ms + 20ms with jitter)
        assertTrue(totalTime >= 20, "Should apply exponential backoff delays");
        assertEquals(3, attemptCount.get());
    }
    
    @Test
    void shouldCreateRetryPredicates() {
        // Test retryOn predicate
        var retryOnPredicate = RetryExecutor.retryOn(
            IllegalArgumentException.class, 
            IllegalStateException.class
        );
        
        assertTrue(retryOnPredicate.test(new IllegalArgumentException()));
        assertTrue(retryOnPredicate.test(new IllegalStateException()));
        assertFalse(retryOnPredicate.test(new RuntimeException()));
        
        // Test retryExcept predicate
        var retryExceptPredicate = RetryExecutor.retryExcept(
            IllegalArgumentException.class
        );
        
        assertFalse(retryExceptPredicate.test(new IllegalArgumentException()));
        assertTrue(retryExceptPredicate.test(new IllegalStateException()));
        assertTrue(retryExceptPredicate.test(new RuntimeException()));
    }
}