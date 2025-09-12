package com.agentic.e2etester.monitoring;

import java.time.Duration;
import java.util.Map;

/**
 * Aggregated metrics across all test executions.
 */
public class AggregatedMetrics {
    
    private final int totalTests;
    private final Duration averageExecutionTime;
    private final double successRate;
    private final Map<String, Object> serviceInteractionStats;
    private final Map<String, Object> assertionStats;
    
    public AggregatedMetrics(int totalTests, Duration averageExecutionTime, double successRate,
                           Map<String, Object> serviceInteractionStats, Map<String, Object> assertionStats) {
        this.totalTests = totalTests;
        this.averageExecutionTime = averageExecutionTime;
        this.successRate = successRate;
        this.serviceInteractionStats = serviceInteractionStats;
        this.assertionStats = assertionStats;
    }
    
    // Getters
    public int getTotalTests() {
        return totalTests;
    }
    
    public Duration getAverageExecutionTime() {
        return averageExecutionTime;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public Map<String, Object> getServiceInteractionStats() {
        return serviceInteractionStats;
    }
    
    public Map<String, Object> getAssertionStats() {
        return assertionStats;
    }
}