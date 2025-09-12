package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.TestMetrics;

import java.time.Instant;
import java.util.List;

/**
 * Comprehensive test execution report containing all relevant metrics and status information.
 */
public class TestExecutionReport {
    
    private final String testId;
    private final String status;
    private final Instant startTime;
    private final Instant lastUpdated;
    private final TestMetrics metrics;
    private final List<TestDashboardService.ServiceMetricsInfo> serviceMetrics;
    private final List<TestDashboardService.AlertInfo> alerts;
    
    public TestExecutionReport(String testId, String status, Instant startTime, Instant lastUpdated,
                             TestMetrics metrics, List<TestDashboardService.ServiceMetricsInfo> serviceMetrics,
                             List<TestDashboardService.AlertInfo> alerts) {
        this.testId = testId;
        this.status = status;
        this.startTime = startTime;
        this.lastUpdated = lastUpdated;
        this.metrics = metrics;
        this.serviceMetrics = serviceMetrics;
        this.alerts = alerts;
    }
    
    // Getters
    public String getTestId() {
        return testId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getLastUpdated() {
        return lastUpdated;
    }
    
    public TestMetrics getMetrics() {
        return metrics;
    }
    
    public List<TestDashboardService.ServiceMetricsInfo> getServiceMetrics() {
        return serviceMetrics;
    }
    
    public List<TestDashboardService.AlertInfo> getAlerts() {
        return alerts;
    }
}