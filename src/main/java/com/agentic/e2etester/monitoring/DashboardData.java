package com.agentic.e2etester.monitoring;

import java.util.List;

/**
 * Complete dashboard data containing all information for real-time display.
 */
public class DashboardData {
    
    private final List<TestDashboardService.TestStatusInfo> testStatuses;
    private final List<TestDashboardService.ServiceMetricsInfo> serviceMetrics;
    private final List<TestDashboardService.AlertInfo> alerts;
    private final SystemSummary systemSummary;
    
    public DashboardData(List<TestDashboardService.TestStatusInfo> testStatuses,
                        List<TestDashboardService.ServiceMetricsInfo> serviceMetrics,
                        List<TestDashboardService.AlertInfo> alerts,
                        SystemSummary systemSummary) {
        this.testStatuses = testStatuses;
        this.serviceMetrics = serviceMetrics;
        this.alerts = alerts;
        this.systemSummary = systemSummary;
    }
    
    // Getters
    public List<TestDashboardService.TestStatusInfo> getTestStatuses() {
        return testStatuses;
    }
    
    public List<TestDashboardService.ServiceMetricsInfo> getServiceMetrics() {
        return serviceMetrics;
    }
    
    public List<TestDashboardService.AlertInfo> getAlerts() {
        return alerts;
    }
    
    public SystemSummary getSystemSummary() {
        return systemSummary;
    }
}