package com.agentic.e2etester.controller;

import com.agentic.e2etester.monitoring.*;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST controller for monitoring and observability endpoints.
 * Provides access to real-time metrics, dashboards, and system health information.
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    private final TestObservabilityManager observabilityManager;
    private final TestMetricsCollector metricsCollector;
    private final TestDashboardService dashboardService;
    private final PrometheusMeterRegistry prometheusMeterRegistry;
    
    public MonitoringController(TestObservabilityManager observabilityManager,
                              TestMetricsCollector metricsCollector,
                              TestDashboardService dashboardService,
                              PrometheusMeterRegistry prometheusMeterRegistry) {
        this.observabilityManager = observabilityManager;
        this.metricsCollector = metricsCollector;
        this.dashboardService = dashboardService;
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }
    
    /**
     * Get current dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardData> getDashboardData() {
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        return ResponseEntity.ok(dashboardData);
    }
    
    /**
     * Get system health metrics
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> healthMetrics = observabilityManager.getSystemHealthMetrics();
        return ResponseEntity.ok(healthMetrics);
    }
    
    /**
     * Get test status summary
     */
    @GetMapping("/tests/summary")
    public ResponseEntity<TestStatusSummary> getTestStatusSummary() {
        TestStatusSummary summary = dashboardService.getTestStatusSummary();
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get service health summary
     */
    @GetMapping("/services/health")
    public ResponseEntity<ServiceHealthSummary> getServiceHealthSummary() {
        ServiceHealthSummary summary = dashboardService.getServiceHealthSummary();
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get active test summaries
     */
    @GetMapping("/tests/active")
    public ResponseEntity<List<TestExecutionSummary>> getActiveTests() {
        List<TestExecutionSummary> activeTests = observabilityManager.getActiveTestSummaries();
        return ResponseEntity.ok(activeTests);
    }
    
    /**
     * Get detailed test execution report
     */
    @GetMapping("/tests/{testId}/report")
    public ResponseEntity<TestExecutionReport> getTestReport(@PathVariable String testId) {
        try {
            TestExecutionReport report = observabilityManager.generateRealTimeReport(testId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get test metrics data
     */
    @GetMapping("/tests/{testId}/metrics")
    public ResponseEntity<TestMetricsData> getTestMetrics(@PathVariable String testId) {
        TestMetricsData metrics = metricsCollector.getTestMetrics(testId);
        if (metrics != null) {
            return ResponseEntity.ok(metrics);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get aggregated metrics across all tests
     */
    @GetMapping("/metrics/aggregated")
    public ResponseEntity<AggregatedMetrics> getAggregatedMetrics() {
        AggregatedMetrics metrics = metricsCollector.getAggregatedMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get Prometheus metrics
     */
    @GetMapping(value = "/metrics", produces = "text/plain")
    public ResponseEntity<String> getPrometheusMetrics() {
        String metrics = prometheusMeterRegistry.scrape();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Clear completed test data
     */
    @DeleteMapping("/tests/completed")
    public ResponseEntity<Void> clearCompletedTests(@RequestParam(defaultValue = "24") long retentionHours) {
        dashboardService.clearCompletedTests(retentionHours);
        metricsCollector.cleanupOldMetrics(Duration.ofHours(retentionHours));
        return ResponseEntity.ok().build();
    }
}