package com.agentic.e2etester.recovery;

import com.agentic.e2etester.model.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Retry executor with exponential backoff support
 */
public class RetryExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryExecutor.class);
    
    private final ScheduledExecutorService scheduler;
    
    public RetryExecutor(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
    
    /**
     * Execute a supplier with retry logic based on retry policy
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> supplier,
            RetryPolicy retryPolicy,
            String operationName) {
        
        return executeWithRetry(supplier, retryPolicy, operationName, this::isRetryableException);
    }
    
    /**
     * Execute a supplier with retry logic and custom retry predicate
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> supplier,
            RetryPolicy retryPolicy,
            String operationName,
            Predicate<Throwable> retryPredicate) {
        
        return executeAttempt(supplier, retryPolicy, operationName, retryPredicate, 1);
    }
    
    private <T> CompletableFuture<T> executeAttempt(
            Supplier<CompletableFuture<T>> supplier,
            RetryPolicy retryPolicy,
            String operationName,
            Predicate<Throwable> retryPredicate,
            int attemptNumber) {
        
        logger.debug("Executing {} - attempt {}/{}", operationName, attemptNumber, retryPolicy.getMaxAttempts());
        
        return supplier.get()
            .handle((result, throwable) -> {
                if (throwable == null) {
                    if (attemptNumber > 1) {
                        logger.info("Operation {} succeeded on attempt {}", operationName, attemptNumber);
                    }
                    return CompletableFuture.completedFuture(result);
                }
                
                if (attemptNumber >= retryPolicy.getMaxAttempts() || !retryPredicate.test(throwable)) {
                    logger.warn("Operation {} failed after {} attempts", operationName, attemptNumber, throwable);
                    return CompletableFuture.<T>failedFuture(throwable);
                }
                
                long delay = calculateDelay(retryPolicy, attemptNumber);
                logger.warn("Operation {} failed on attempt {}, retrying in {}ms", 
                    operationName, attemptNumber, delay, throwable);
                
                CompletableFuture<T> retryFuture = new CompletableFuture<>();
                scheduler.schedule(() -> {
                    executeAttempt(supplier, retryPolicy, operationName, retryPredicate, attemptNumber + 1)
                        .whenComplete((retryResult, retryThrowable) -> {
                            if (retryThrowable != null) {
                                retryFuture.completeExceptionally(retryThrowable);
                            } else {
                                retryFuture.complete(retryResult);
                            }
                        });
                }, delay, TimeUnit.MILLISECONDS);
                
                return retryFuture;
            })
            .thenCompose(future -> future);
    }
    
    private long calculateDelay(RetryPolicy retryPolicy, int attemptNumber) {
        long baseDelay = retryPolicy.getInitialDelayMs();
        double multiplier = retryPolicy.getBackoffMultiplier();
        long maxDelay = retryPolicy.getMaxDelayMs();
        
        // Calculate exponential backoff with jitter
        long delay = (long) (baseDelay * Math.pow(multiplier, attemptNumber - 1));
        
        // Add jitter (±25% randomization)
        double jitter = 0.25 * Math.random() - 0.125; // -12.5% to +12.5%
        delay = (long) (delay * (1 + jitter));
        
        // Cap at max delay
        return Math.min(delay, maxDelay);
    }
    
    private boolean isRetryableException(Throwable throwable) {
        // Default retryable exceptions
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.net.SocketTimeoutException ||
               throwable instanceof java.util.concurrent.TimeoutException ||
               throwable instanceof org.springframework.web.client.ResourceAccessException ||
               (throwable instanceof RuntimeException && 
                throwable.getMessage() != null && 
                throwable.getMessage().contains("timeout"));
    }
    
    /**
     * Create a retry predicate based on exception types
     */
    public static Predicate<Throwable> retryOn(Class<? extends Throwable>... exceptionTypes) {
        return throwable -> {
            for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                if (exceptionType.isInstance(throwable)) {
                    return true;
                }
            }
            return false;
        };
    }
    
    /**
     * Create a retry predicate that excludes certain exception types
     */
    public static Predicate<Throwable> retryExcept(Class<? extends Throwable>... exceptionTypes) {
        return throwable -> {
            for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                if (exceptionType.isInstance(throwable)) {
                    return false;
                }
            }
            return true;
        };
    }
}