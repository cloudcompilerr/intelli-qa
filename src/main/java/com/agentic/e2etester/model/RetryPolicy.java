package com.agentic.e2etester.model;

import jakarta.validation.constraints.PositiveOrZero;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Defines retry behavior for test steps.
 */
public class RetryPolicy {
    
    @PositiveOrZero(message = "Max attempts must be positive or zero")
    @JsonProperty("maxAttempts")
    private Integer maxAttempts;
    
    @PositiveOrZero(message = "Initial delay must be positive or zero")
    @JsonProperty("initialDelayMs")
    private Long initialDelayMs;
    
    @PositiveOrZero(message = "Max delay must be positive or zero")
    @JsonProperty("maxDelayMs")
    private Long maxDelayMs;
    
    @JsonProperty("backoffMultiplier")
    private Double backoffMultiplier;
    
    @JsonProperty("retryOnExceptions")
    private String[] retryOnExceptions;
    
    // Default constructor
    public RetryPolicy() {
        this.maxAttempts = 3;
        this.initialDelayMs = 1000L;
        this.maxDelayMs = 30000L;
        this.backoffMultiplier = 2.0;
    }
    
    // Constructor with basic settings
    public RetryPolicy(Integer maxAttempts, Long initialDelayMs) {
        this();
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
    }
    
    // Getters and setters
    public Integer getMaxAttempts() {
        return maxAttempts;
    }
    
    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public Long getInitialDelayMs() {
        return initialDelayMs;
    }
    
    public void setInitialDelayMs(Long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }
    
    public Long getMaxDelayMs() {
        return maxDelayMs;
    }
    
    public void setMaxDelayMs(Long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }
    
    public Double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public void setBackoffMultiplier(Double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }
    
    public String[] getRetryOnExceptions() {
        return retryOnExceptions;
    }
    
    public void setRetryOnExceptions(String[] retryOnExceptions) {
        this.retryOnExceptions = retryOnExceptions;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetryPolicy that = (RetryPolicy) o;
        return Objects.equals(maxAttempts, that.maxAttempts) &&
               Objects.equals(initialDelayMs, that.initialDelayMs) &&
               Objects.equals(backoffMultiplier, that.backoffMultiplier);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(maxAttempts, initialDelayMs, backoffMultiplier);
    }
    
    @Override
    public String toString() {
        return "RetryPolicy{" +
               "maxAttempts=" + maxAttempts +
               ", initialDelayMs=" + initialDelayMs +
               ", maxDelayMs=" + maxDelayMs +
               ", backoffMultiplier=" + backoffMultiplier +
               '}';
    }
}