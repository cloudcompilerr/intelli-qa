package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central observability manager for test execution monitoring and metrics collection.
 * Provides comprehensive monitoring capabilities including real-time metrics,
 * performance tracking, and integration with monitoring systems.
 */
@Component
public class TestObservabilityManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestObservabilityManager.class);
    
    private final MeterRegistry meterRegistry;
    private final TestMetricsCollector metricsCollector;
    private final TestDashboardService dashboardService;
    
    // Metrics
    private final Counter testExecutionsTotal;
    private final Counter testFailuresTotal;
    private final Timer testExecutionDuration;
    private final Gauge activeTestsGauge;
    private final Counter serviceInteractionsTotal;
    private final Timer serviceResponseTime;
    private final Counter assertionExecutionsTotal;
    private final Counter assertionFailuresTotal;
    
    // Active test tracking
    private final Map<String, TestExecutionMetrics> activeTests = new ConcurrentHashMap<>();
    private final AtomicLong activeTestCount = new AtomicLong(0);
    
    public TestObservabilityManager(MeterRegistry meterRegistry, 
                                  TestMetricsCollector metricsCollector,
                                  TestDashboardService dashboardService) {
        this.meterRegistry = meterRegistry;
        this.metricsCollector = metricsCollector;
        this.dashboardService = dashboardService;
        
        // Initialize metrics
        this.testExecutionsTotal = Counter.builder("test_executions_total")
                .description("Total number of test executions")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
                
        this.testFailuresTotal = Counter.builder("test_failures_total")
                .description("Total number of test failures")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
                
        this.testExecutionDuration = Timer.builder("test_execution_duration")
                .description("Test execution duration")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
                
        this.activeTestsGauge = Gauge.builder("active_tests", () -> activeTestCount.get())
                .description("Number of currently active tests")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
                
        this.serviceInteractionsTotal = Counter.builder("service_interactions_total")
                .description("Total number of service interactions")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
                
        this.serviceResponseTime = Timer.builder("service_response_time")
                .description("Service response time")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
                
        this.assertionExecutionsTotal = Counter.builder("assertion_executions_total")
                .description("Total number of assertion executions")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
                
        this.assertionFailuresTotal = Counter.builder("assertion_failures_total")
                .description("Total number of assertion failures")
                .tag("system", "agentic-e2e-tester")
                .register(meterRegistry);
    }
    
    /**
     * Track the start of a test execution
     */
    public void trackTestStart(TestContext context) {
        logger.info("Starting test execution tracking for test: {}", context.getCorrelationId());
        
        TestExecutionMetrics metrics = new TestExecutionMetrics(
                context.getCorrelationId(),
                Instant.now(),
                context.getExecutionState()
        );
        
        activeTests.put(context.getCorrelationId(), metrics);
        activeTestCount.incrementAndGet();
        
        testExecutionsTotal.increment();
        
        metricsCollector.recordTestStart(context);
        dashboardService.updateTestStatus(context.getCorrelationId(), "RUNNING");
    }
    
    /**
     * Track test execution completion
     */
    public void trackTestCompletion(TestResult result) {
        String testId = result.getTestId();
        TestExecutionMetrics metrics = activeTests.remove(testId);
        
        if (metrics != null) {
            activeTestCount.decrementAndGet();
            Duration executionTime = Duration.between(metrics.getStartTime(), Instant.now());
            
            testExecutionDuration.record(executionTime);
            
            if (result.getStatus() == TestStatus.FAILED) {
                testFailuresTotal.increment();
            }
            
            logger.info("Test execution completed: {} in {} ms", 
                    testId, executionTime.toMillis());
        }
        
        metricsCollector.recordTestCompletion(result);
        dashboardService.updateTestStatus(testId, result.getStatus().toString());
    }
    
    /**
     * Track service interaction metrics
     */
    public void trackServiceInteraction(ServiceInteraction interaction) {
        serviceInteractionsTotal.increment();
        
        if (interaction.getResponseTime() != null) {
            serviceResponseTime.record(interaction.getResponseTime());
        }
        
        metricsCollector.recordServiceInteraction(interaction);
        dashboardService.updateServiceMetrics(interaction);
    }
    
    /**
     * Track assertion execution metrics
     */
    public void trackAssertionExecution(AssertionResult result) {
        assertionExecutionsTotal.increment();
        
        if (!result.isSuccessful()) {
            assertionFailuresTotal.increment();
        }
        
        metricsCollector.recordAssertionExecution(result);
        dashboardService.updateAssertionMetrics(result);
    }
    
    /**
     * Collect comprehensive metrics for a test execution
     */
    public void collectMetrics(String testId, TestMetrics metrics) {
        logger.debug("Collecting metrics for test: {}", testId);
        
        // Record custom metrics
        if (metrics.getCustomMetrics() != null) {
            metrics.getCustomMetrics().forEach((key, value) -> {
                Gauge.builder("test_custom_metric", () -> ((Number) value).doubleValue())
                        .description("Custom test metric")
                        .tag("metric_name", key)
                        .tag("test_id", testId)
                        .register(meterRegistry);
            });
        }
        
        metricsCollector.collectDetailedMetrics(testId, metrics);
        dashboardService.updateMetrics(testId, metrics);
    }
    
    /**
     * Generate real-time test execution report
     */
    public TestExecutionReport generateRealTimeReport(String testId) {
        logger.debug("Generating real-time report for test: {}", testId);
        
        TestExecutionMetrics metrics = activeTests.get(testId);
        if (metrics == null) {
            throw new IllegalArgumentException("Test not found or not active: " + testId);
        }
        
        return dashboardService.generateReport(testId, metrics);
    }
    
    /**
     * Alert on critical test failure
     */
    public void alertOnCriticalFailure(TestFailure failure) {
        logger.error("Critical test failure detected: {}", failure.getErrorMessage());
        
        // Increment critical failure counter
        Counter.builder("critical_failures_total")
                .description("Total number of critical test failures")
                .tag("failure_type", failure.getFailureType().toString())
                .tag("severity", failure.getSeverity().toString())
                .tag("service", failure.getServiceId() != null ? failure.getServiceId() : "unknown")
                .register(meterRegistry)
                .increment();
        
        // Update dashboard with alert
        dashboardService.triggerAlert(failure);
        
        // Record detailed failure metrics
        metricsCollector.recordCriticalFailure(failure);
    }
    
    /**
     * Get current system health metrics
     */
    public Map<String, Object> getSystemHealthMetrics() {
        Map<String, Object> healthMetrics = new HashMap<>();
        
        healthMetrics.put("active_tests", activeTestCount.get());
        healthMetrics.put("total_executions", testExecutionsTotal.count());
        healthMetrics.put("total_failures", testFailuresTotal.count());
        healthMetrics.put("success_rate", calculateSuccessRate());
        healthMetrics.put("average_execution_time", testExecutionDuration.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        healthMetrics.put("service_interactions", serviceInteractionsTotal.count());
        healthMetrics.put("assertion_executions", assertionExecutionsTotal.count());
        healthMetrics.put("assertion_failures", assertionFailuresTotal.count());
        
        return healthMetrics;
    }
    
    /**
     * Get active test summaries
     */
    public List<TestExecutionSummary> getActiveTestSummaries() {
        return activeTests.values().stream()
                .map(this::createTestSummary)
                .toList();
    }
    
    private TestExecutionSummary createTestSummary(TestExecutionMetrics metrics) {
        Duration runningTime = Duration.between(metrics.getStartTime(), Instant.now());
        
        return new TestExecutionSummary(
                metrics.getTestId(),
                metrics.getStartTime(),
                runningTime,
                "RUNNING",
                metrics.getExecutionState()
        );
    }
    
    private double calculateSuccessRate() {
        double total = testExecutionsTotal.count();
        double failures = testFailuresTotal.count();
        return total > 0 ? ((total - failures) / total) * 100.0 : 100.0;
    }
    
    private String determineTestType(TestContext context) {
        // Analyze context to determine test type
        if (context.getExecutionState().containsKey("order_fulfillment")) {
            return "order_fulfillment";
        } else if (context.getExecutionState().containsKey("integration")) {
            return "integration";
        }
        return "general";
    }
    
    private String determineTestType(TestResult result) {
        // Analyze result to determine test type based on metadata
        if (result.getMetadata() != null && result.getMetadata().containsKey("test_type")) {
            return result.getMetadata().get("test_type").toString();
        }
        return "general";
    }
    
    private String determineEnvironment(TestContext context) {
        return context.getExecutionState().getOrDefault("environment", "unknown").toString();
    }
    
    private String determineFailureType(TestResult result) {
        // Determine failure type from error message or failure reason
        if (result.getFailureReason() != null) {
            return result.getFailureReason();
        }
        return "unknown";
    }
    
    /**
     * Internal class to track test execution metrics
     */
    public static class TestExecutionMetrics {
        private final String testId;
        private final Instant startTime;
        private final Map<String, Object> executionState;
        
        public TestExecutionMetrics(String testId, Instant startTime, Map<String, Object> executionState) {
            this.testId = testId;
            this.startTime = startTime;
            this.executionState = new HashMap<>(executionState);
        }
        
        public String getTestId() { return testId; }
        public Instant getStartTime() { return startTime; }
        public Map<String, Object> getExecutionState() { return executionState; }
    }
}