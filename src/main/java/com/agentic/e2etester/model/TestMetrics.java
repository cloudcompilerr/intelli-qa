package com.agentic.e2etester.model;

import jakarta.validation.constraints.PositiveOrZero;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Contains metrics and performance data for test execution.
 */
public class TestMetrics {
    
    @PositiveOrZero(message = "Total execution time must be positive or zero")
    @JsonProperty("totalExecutionTimeMs")
    private Long totalExecutionTimeMs;
    
    @PositiveOrZero(message = "Average response time must be positive or zero")
    @JsonProperty("averageResponseTimeMs")
    private Long averageResponseTimeMs;
    
    @PositiveOrZero(message = "Min response time must be positive or zero")
    @JsonProperty("minResponseTimeMs")
    private Long minResponseTimeMs;
    
    @PositiveOrZero(message = "Max response time must be positive or zero")
    @JsonProperty("maxResponseTimeMs")
    private Long maxResponseTimeMs;
    
    @PositiveOrZero(message = "Total requests must be positive or zero")
    @JsonProperty("totalRequests")
    private Integer totalRequests;
    
    @PositiveOrZero(message = "Successful requests must be positive or zero")
    @JsonProperty("successfulRequests")
    private Integer successfulRequests;
    
    @PositiveOrZero(message = "Failed requests must be positive or zero")
    @JsonProperty("failedRequests")
    private Integer failedRequests;
    
    @JsonProperty("throughput")
    private Double throughput;
    
    @JsonProperty("errorRate")
    private Double errorRate;
    
    @JsonProperty("customMetrics")
    private Map<String, Object> customMetrics;
    
    // Default constructor
    public TestMetrics() {}
    
    // Getters and setters
    public Long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public void setTotalExecutionTimeMs(Long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }
    
    public Long getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }
    
    public void setAverageResponseTimeMs(Long averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }
    
    public Long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }
    
    public void setMinResponseTimeMs(Long minResponseTimeMs) {
        this.minResponseTimeMs = minResponseTimeMs;
    }
    
    public Long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }
    
    public void setMaxResponseTimeMs(Long maxResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
    }
    
    public Integer getTotalRequests() {
        return totalRequests;
    }
    
    public void setTotalRequests(Integer totalRequests) {
        this.totalRequests = totalRequests;
    }
    
    public Integer getSuccessfulRequests() {
        return successfulRequests;
    }
    
    public void setSuccessfulRequests(Integer successfulRequests) {
        this.successfulRequests = successfulRequests;
    }
    
    public Integer getFailedRequests() {
        return failedRequests;
    }
    
    public void setFailedRequests(Integer failedRequests) {
        this.failedRequests = failedRequests;
    }
    
    public Double getThroughput() {
        return throughput;
    }
    
    public void setThroughput(Double throughput) {
        this.throughput = throughput;
    }
    
    public Double getErrorRate() {
        return errorRate;
    }
    
    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }
    
    public Map<String, Object> getCustomMetrics() {
        return customMetrics;
    }
    
    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = customMetrics;
    }
    
    // Utility methods
    public double calculateSuccessRate() {
        if (totalRequests == null || totalRequests == 0) {
            return 0.0;
        }
        return (double) (successfulRequests != null ? successfulRequests : 0) / totalRequests;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestMetrics that = (TestMetrics) o;
        return Objects.equals(totalExecutionTimeMs, that.totalExecutionTimeMs) &&
               Objects.equals(totalRequests, that.totalRequests) &&
               Objects.equals(successfulRequests, that.successfulRequests) &&
               Objects.equals(failedRequests, that.failedRequests);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(totalExecutionTimeMs, totalRequests, successfulRequests, failedRequests);
    }
    
    @Override
    public String toString() {
        return "TestMetrics{" +
               "totalExecutionTimeMs=" + totalExecutionTimeMs +
               ", averageResponseTimeMs=" + averageResponseTimeMs +
               ", totalRequests=" + totalRequests +
               ", successfulRequests=" + successfulRequests +
               ", failedRequests=" + failedRequests +
               ", errorRate=" + errorRate +
               '}';
    }
}