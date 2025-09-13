package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration settings for test execution.
 */
public class TestConfiguration {
    
    @NotNull(message = "Default timeout cannot be null")
    @Positive(message = "Default timeout must be positive")
    @JsonProperty("defaultTimeoutMs")
    private Long defaultTimeoutMs;
    
    @PositiveOrZero(message = "Max retries must be positive or zero")
    @JsonProperty("maxRetries")
    private Integer maxRetries;
    
    @JsonProperty("parallelExecution")
    private Boolean parallelExecution;
    
    @JsonProperty("failFast")
    private Boolean failFast;
    
    @JsonProperty("environment")
    private String environment;
    
    @JsonProperty("properties")
    private Map<String, String> properties;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Default constructor
    public TestConfiguration() {
        this.defaultTimeoutMs = 30000L; // 30 seconds default
        this.maxRetries = 3;
        this.parallelExecution = false;
        this.failFast = true;
    }
    
    // Getters and setters
    public Long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }
    
    public void setDefaultTimeoutMs(Long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Boolean getParallelExecution() {
        return parallelExecution;
    }
    
    public void setParallelExecution(Boolean parallelExecution) {
        this.parallelExecution = parallelExecution;
    }
    
    public Boolean getFailFast() {
        return failFast;
    }
    
    public void setFailFast(Boolean failFast) {
        this.failFast = failFast;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    // Utility methods
    public boolean isStopOnFirstFailure() {
        return failFast != null && failFast;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestConfiguration that = (TestConfiguration) o;
        return Objects.equals(defaultTimeoutMs, that.defaultTimeoutMs) &&
               Objects.equals(maxRetries, that.maxRetries) &&
               Objects.equals(parallelExecution, that.parallelExecution) &&
               Objects.equals(failFast, that.failFast) &&
               Objects.equals(environment, that.environment);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(defaultTimeoutMs, maxRetries, parallelExecution, failFast, environment);
    }
    
    @Override
    public String toString() {
        return "TestConfiguration{" +
               "defaultTimeoutMs=" + defaultTimeoutMs +
               ", maxRetries=" + maxRetries +
               ", parallelExecution=" + parallelExecution +
               ", failFast=" + failFast +
               ", environment='" + environment + '\'' +
               '}';
    }
}