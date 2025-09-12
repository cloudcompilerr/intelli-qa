package com.agentic.e2etester.monitoring;

/**
 * High-level system summary for dashboard overview.
 */
public class SystemSummary {
    
    private final int totalTests;
    private final long activeTests;
    private final long healthyServices;
    private final int activeAlerts;
    
    public SystemSummary(int totalTests, long activeTests, long healthyServices, int activeAlerts) {
        this.totalTests = totalTests;
        this.activeTests = activeTests;
        this.healthyServices = healthyServices;
        this.activeAlerts = activeAlerts;
    }
    
    // Getters
    public int getTotalTests() {
        return totalTests;
    }
    
    public long getActiveTests() {
        return activeTests;
    }
    
    public long getHealthyServices() {
        return healthyServices;
    }
    
    public int getActiveAlerts() {
        return activeAlerts;
    }
}