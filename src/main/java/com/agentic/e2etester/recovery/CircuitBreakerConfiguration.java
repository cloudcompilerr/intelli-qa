package com.agentic.e2etester.recovery;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;

/**
 * Configuration for circuit breaker behavior
 */
public class CircuitBreakerConfiguration {
    
    @Positive(message = "Failure threshold must be positive")
    private int failureThreshold = 5;
    
    @Positive(message = "Success threshold must be positive")
    private int successThreshold = 3;
    
    @PositiveOrZero(message = "Timeout must be positive or zero")
    private Duration timeout = Duration.ofSeconds(60);
    
    @PositiveOrZero(message = "Recovery timeout must be positive or zero")
    private Duration recoveryTimeout = Duration.ofSeconds(30);
    
    private boolean enabled = true;
    
    public CircuitBreakerConfiguration() {}
    
    public CircuitBreakerConfiguration(int failureThreshold, int successThreshold, Duration timeout) {
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.timeout = timeout;
    }
    
    // Getters and setters
    public int getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
    
    public int getSuccessThreshold() { return successThreshold; }
    public void setSuccessThreshold(int successThreshold) { this.successThreshold = successThreshold; }
    
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    
    public Duration getRecoveryTimeout() { return recoveryTimeout; }
    public void setRecoveryTimeout(Duration recoveryTimeout) { this.recoveryTimeout = recoveryTimeout; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}