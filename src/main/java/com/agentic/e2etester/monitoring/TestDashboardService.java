package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating real-time test execution dashboards and reports.
 * Provides live updates on test status, metrics, and system health.
 */
@Service
public class TestDashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDashboardService.class);
    
    private final Map<String, TestStatusInfo> testStatuses = new ConcurrentHashMap<>();
    private final Map<String, ServiceMetricsInfo> serviceMetrics = new ConcurrentHashMap<>();
    private final List<AlertInfo> activeAlerts = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, TestMetrics> currentMetrics = new ConcurrentHashMap<>();
    
    /**
     * Update test status for dashboard display
     */
    public void updateTestStatus(String testId, String status) {
        TestStatusInfo statusInfo = testStatuses.computeIfAbsent(testId, 
                id -> new TestStatusInfo(id, Instant.now()));
        
        statusInfo.setStatus(status);
        statusInfo.setLastUpdated(Instant.now());
        
        logger.debug("Updated test status: {} -> {}", testId, status);
    }
    
    /**
     * Update service metrics for dashboard display
     */
    public void updateServiceMetrics(ServiceInteraction interaction) {
        String serviceId = interaction.getServiceId();
        ServiceMetricsInfo metricsInfo = serviceMetrics.computeIfAbsent(serviceId,
                id -> new ServiceMetricsInfo(id));
        
        metricsInfo.recordInteraction(interaction);
        
        logger.debug("Updated service metrics for: {}", serviceId);
    }
    
    /**
     * Update assertion metrics for dashboard display
     */
    public void updateAssertionMetrics(AssertionResult result) {
        // Update global assertion statistics
        String testId = extractTestIdFromAssertion(result);
        if (testId != null) {
            TestStatusInfo statusInfo = testStatuses.get(testId);
            if (statusInfo != null) {
                statusInfo.recordAssertion(result);
            }
        }
        
        logger.debug("Updated assertion metrics: {} ({})", 
                result.getRuleId(), result.getPassed() ? "PASSED" : "FAILED");
    }
    
    /**
     * Update test metrics for dashboard display
     */
    public void updateMetrics(String testId, TestMetrics metrics) {
        currentMetrics.put(testId, metrics);
        
        TestStatusInfo statusInfo = testStatuses.get(testId);
        if (statusInfo != null) {
            statusInfo.setMetrics(metrics);
        }
        
        logger.debug("Updated metrics for test: {}", testId);
    }
    
    /**
     * Trigger alert for dashboard display
     */
    public void triggerAlert(TestFailure failure) {
        AlertInfo alert = new AlertInfo(
                UUID.randomUUID().toString(),
                failure.getFailureType().toString(),
                failure.getSeverity().toString(),
                failure.getErrorMessage(),
                failure.getServiceId(),
                Instant.now()
        );
        
        activeAlerts.add(alert);
        
        // Keep only recent alerts (last 100)
        if (activeAlerts.size() > 100) {
            activeAlerts.subList(0, activeAlerts.size() - 100).clear();
        }
        
        logger.warn("Triggered alert: {} - {}", failure.getFailureType(), failure.getErrorMessage());
    }
    
    /**
     * Generate comprehensive test execution report
     */
    public TestExecutionReport generateReport(String testId, Object metrics) {
        TestStatusInfo statusInfo = testStatuses.get(testId);
        TestMetrics testMetrics = currentMetrics.get(testId);
        
        return new TestExecutionReport(
                testId,
                statusInfo != null ? statusInfo.getStatus() : "UNKNOWN",
                statusInfo != null ? statusInfo.getStartTime() : Instant.now(),
                statusInfo != null ? statusInfo.getLastUpdated() : Instant.now(),
                testMetrics,
                getServiceMetricsForTest(testId),
                getAlertsForTest(testId)
        );
    }
    
    /**
     * Get current dashboard data
     */
    public DashboardData getCurrentDashboardData() {
        return new DashboardData(
                new ArrayList<>(testStatuses.values()),
                new ArrayList<>(serviceMetrics.values()),
                new ArrayList<>(activeAlerts),
                calculateSystemSummary()
        );
    }
    
    /**
     * Get test status summary
     */
    public TestStatusSummary getTestStatusSummary() {
        Map<String, Long> statusCounts = new HashMap<>();
        
        testStatuses.values().forEach(status -> {
            statusCounts.merge(status.getStatus(), 1L, Long::sum);
        });
        
        return new TestStatusSummary(
                testStatuses.size(),
                statusCounts.getOrDefault("RUNNING", 0L),
                statusCounts.getOrDefault("PASSED", 0L),
                statusCounts.getOrDefault("FAILED", 0L),
                statusCounts.getOrDefault("CANCELLED", 0L)
        );
    }
    
    /**
     * Get service health summary
     */
    public ServiceHealthSummary getServiceHealthSummary() {
        Map<String, ServiceHealthSummary.ServiceHealthInfo> healthInfo = new HashMap<>();
        
        serviceMetrics.values().forEach(metrics -> {
            ServiceHealthSummary.ServiceHealthInfo health = new ServiceHealthSummary.ServiceHealthInfo(
                    metrics.getServiceId(),
                    metrics.getTotalInteractions(),
                    metrics.getSuccessfulInteractions(),
                    metrics.getAverageResponseTime(),
                    metrics.getLastInteractionTime()
            );
            healthInfo.put(metrics.getServiceId(), health);
        });
        
        return new ServiceHealthSummary(healthInfo);
    }
    
    /**
     * Clear completed test data
     */
    public void clearCompletedTests(long retentionHours) {
        Instant cutoff = Instant.now().minusSeconds(retentionHours * 3600);
        
        testStatuses.entrySet().removeIf(entry -> {
            TestStatusInfo status = entry.getValue();
            return !status.getStatus().equals("RUNNING") && 
                   status.getLastUpdated().isBefore(cutoff);
        });
        
        currentMetrics.entrySet().removeIf(entry -> 
                !testStatuses.containsKey(entry.getKey()));
        
        logger.debug("Cleared completed tests older than {} hours", retentionHours);
    }
    
    private String extractTestIdFromAssertion(AssertionResult result) {
        // Extract test ID from step ID or use a correlation mechanism
        return result.getStepId() != null ? result.getStepId().split("-")[0] : null;
    }
    
    private List<ServiceMetricsInfo> getServiceMetricsForTest(String testId) {
        // In a real implementation, this would filter service metrics by test ID
        return new ArrayList<>(serviceMetrics.values());
    }
    
    private List<AlertInfo> getAlertsForTest(String testId) {
        // In a real implementation, this would filter alerts by test ID
        return new ArrayList<>(activeAlerts);
    }
    
    private SystemSummary calculateSystemSummary() {
        TestStatusSummary testSummary = getTestStatusSummary();
        ServiceHealthSummary serviceSummary = getServiceHealthSummary();
        
        return new SystemSummary(
                testSummary.getTotalTests(),
                testSummary.getRunningTests(),
                serviceSummary.getHealthyServices(),
                activeAlerts.size()
        );
    }
    
    /**
     * Test status information for dashboard display
     */
    public static class TestStatusInfo {
        private final String testId;
        private final Instant startTime;
        private String status;
        private Instant lastUpdated;
        private TestMetrics metrics;
        private int totalAssertions = 0;
        private int passedAssertions = 0;
        
        public TestStatusInfo(String testId, Instant startTime) {
            this.testId = testId;
            this.startTime = startTime;
            this.status = "INITIALIZING";
            this.lastUpdated = startTime;
        }
        
        public void recordAssertion(AssertionResult result) {
            totalAssertions++;
            if (result.isSuccessful()) {
                passedAssertions++;
            }
        }
        
        // Getters and setters
        public String getTestId() { return testId; }
        public Instant getStartTime() { return startTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
        public TestMetrics getMetrics() { return metrics; }
        public void setMetrics(TestMetrics metrics) { this.metrics = metrics; }
        public int getTotalAssertions() { return totalAssertions; }
        public int getPassedAssertions() { return passedAssertions; }
    }
    
    /**
     * Service metrics information for dashboard display
     */
    public static class ServiceMetricsInfo {
        private final String serviceId;
        private long totalInteractions = 0;
        private long successfulInteractions = 0;
        private double totalResponseTime = 0.0;
        private Instant lastInteractionTime;
        
        public ServiceMetricsInfo(String serviceId) {
            this.serviceId = serviceId;
        }
        
        public void recordInteraction(ServiceInteraction interaction) {
            totalInteractions++;
            if (interaction.getStatus() == InteractionStatus.SUCCESS) {
                successfulInteractions++;
            }
            if (interaction.getResponseTime() != null) {
                totalResponseTime += interaction.getResponseTime().toMillis();
            }
            lastInteractionTime = interaction.getTimestamp();
        }
        
        public double getAverageResponseTime() {
            return totalInteractions > 0 ? totalResponseTime / totalInteractions : 0.0;
        }
        
        // Getters
        public String getServiceId() { return serviceId; }
        public long getTotalInteractions() { return totalInteractions; }
        public long getSuccessfulInteractions() { return successfulInteractions; }
        public Instant getLastInteractionTime() { return lastInteractionTime; }
    }
    
    /**
     * Alert information for dashboard display
     */
    public static class AlertInfo {
        private final String alertId;
        private final String type;
        private final String severity;
        private final String message;
        private final String serviceId;
        private final Instant timestamp;
        
        public AlertInfo(String alertId, String type, String severity, String message, 
                        String serviceId, Instant timestamp) {
            this.alertId = alertId;
            this.type = type;
            this.severity = severity;
            this.message = message;
            this.serviceId = serviceId;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getAlertId() { return alertId; }
        public String getType() { return type; }
        public String getSeverity() { return severity; }
        public String getMessage() { return message; }
        public String getServiceId() { return serviceId; }
        public Instant getTimestamp() { return timestamp; }
    }
}