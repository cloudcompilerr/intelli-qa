package com.agentic.e2etester.recovery;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit breaker implementation for service failure protection
 */
public class CircuitBreaker {
    
    private final String name;
    private final CircuitBreakerConfiguration config;
    private final AtomicReference<CircuitBreakerState> state;
    private final AtomicInteger failureCount;
    private final AtomicInteger successCount;
    private final AtomicReference<Instant> lastFailureTime;
    private final AtomicReference<Instant> lastStateChangeTime;
    
    public CircuitBreaker(String name, CircuitBreakerConfiguration config) {
        this.name = name;
        this.config = config;
        this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicReference<>(Instant.now());
        this.lastStateChangeTime = new AtomicReference<>(Instant.now());
    }
    
    /**
     * Execute a supplier with circuit breaker protection
     */
    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> supplier) {
        if (!config.isEnabled()) {
            return supplier.get();
        }
        
        CircuitBreakerState currentState = state.get();
        
        switch (currentState) {
            case OPEN:
                if (shouldAttemptReset()) {
                    return attemptReset(supplier);
                } else {
                    return CompletableFuture.failedFuture(
                        new CircuitBreakerOpenException("Circuit breaker " + name + " is OPEN")
                    );
                }
                
            case HALF_OPEN:
                return executeInHalfOpenState(supplier);
                
            case CLOSED:
            default:
                return executeInClosedState(supplier);
        }
    }
    
    private <T> CompletableFuture<T> executeInClosedState(Supplier<CompletableFuture<T>> supplier) {
        return supplier.get()
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    onFailure();
                } else {
                    onSuccess();
                }
            });
    }
    
    private <T> CompletableFuture<T> executeInHalfOpenState(Supplier<CompletableFuture<T>> supplier) {
        return supplier.get()
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    onFailureInHalfOpen();
                } else {
                    onSuccessInHalfOpen();
                }
            });
    }
    
    private <T> CompletableFuture<T> attemptReset(Supplier<CompletableFuture<T>> supplier) {
        if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
            lastStateChangeTime.set(Instant.now());
            successCount.set(0);
            return executeInHalfOpenState(supplier);
        } else {
            // Another thread changed the state, retry
            return execute(supplier);
        }
    }
    
    private boolean shouldAttemptReset() {
        Instant now = Instant.now();
        Instant lastChange = lastStateChangeTime.get();
        return now.isAfter(lastChange.plus(config.getRecoveryTimeout()));
    }
    
    private void onSuccess() {
        failureCount.set(0);
        successCount.incrementAndGet();
    }
    
    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(Instant.now());
        
        if (failures >= config.getFailureThreshold()) {
            tripCircuit();
        }
    }
    
    private void onSuccessInHalfOpen() {
        int successes = successCount.incrementAndGet();
        if (successes >= config.getSuccessThreshold()) {
            closeCircuit();
        }
    }
    
    private void onFailureInHalfOpen() {
        tripCircuit();
    }
    
    private void tripCircuit() {
        state.set(CircuitBreakerState.OPEN);
        lastStateChangeTime.set(Instant.now());
        failureCount.set(0);
    }
    
    private void closeCircuit() {
        state.set(CircuitBreakerState.CLOSED);
        lastStateChangeTime.set(Instant.now());
        failureCount.set(0);
        successCount.set(0);
    }
    
    // Getters for monitoring
    public String getName() { return name; }
    public CircuitBreakerState getState() { return state.get(); }
    public int getFailureCount() { return failureCount.get(); }
    public int getSuccessCount() { return successCount.get(); }
    public Instant getLastFailureTime() { return lastFailureTime.get(); }
    public Instant getLastStateChangeTime() { return lastStateChangeTime.get(); }
    public CircuitBreakerConfiguration getConfiguration() { return config; }
}