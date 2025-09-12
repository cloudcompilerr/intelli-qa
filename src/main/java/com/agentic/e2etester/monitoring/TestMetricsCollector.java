package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detailed metrics collector for test execution data.
 * Collects granular metrics about test performance, service interactions,
 * and system behavior for analysis and reporting.
 */
@Component
public class TestMetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(TestMetricsCollector.class);
    
    private final MeterRegistry meterRegistry;
    private final Map<String, TestMetricsData> testMetrics = new ConcurrentHashMap<>();
    
    public TestMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Record test start metrics
     */
    public void recordTestStart(TestContext context) {
        String testId = context.getCorrelationId();
        TestMetricsData metricsData = new TestMetricsData(testId, Instant.now());
        
        // Record initial context metrics
        metricsData.setInitialState(new HashMap<>(context.getExecutionState()));
        metricsData.setTestType(determineTestType(context));
        
        testMetrics.put(testId, metricsData);
        
        logger.debug("Recorded test start metrics for: {}", testId);
    }
    
    /**
     * Record test completion metrics
     */
    public void recordTestCompletion(TestResult result) {
        String testId = result.getTestId();
        TestMetricsData metricsData = testMetrics.get(testId);
        
        if (metricsData != null) {
            metricsData.setEndTime(Instant.now());
            metricsData.setFinalResult(result);
            
            // Calculate and record performance metrics
            Duration totalDuration = Duration.between(metricsData.getStartTime(), metricsData.getEndTime());
            metricsData.setTotalExecutionTime(totalDuration);
            
            // Record step-level metrics
            if (result.getStepResults() != null) {
                recordStepMetrics(testId, result.getStepResults());
            }
            
            // Record assertion metrics
            if (result.getAssertionResults() != null) {
                recordAssertionMetrics(testId, result.getAssertionResults());
            }
            
            logger.debug("Recorded test completion metrics for: {} (duration: {} ms)", 
                    testId, totalDuration.toMillis());
        }
    }
    
    /**
     * Record service interaction metrics
     */
    public void recordServiceInteraction(ServiceInteraction interaction) {
        String testId = findTestIdForInteraction(interaction);
        if (testId != null) {
            TestMetricsData metricsData = testMetrics.get(testId);
            if (metricsData != null) {
                metricsData.addServiceInteraction(interaction);
                
                // Record service-specific metrics
                meterRegistry.timer("service_interaction_duration",
                        Tags.of(
                                "service", interaction.getServiceId(),
                                "type", interaction.getType().toString(),
                                "status", interaction.getStatus().toString()
                        )
                ).record(interaction.getResponseTime());
                
                logger.debug("Recorded service interaction metrics: {} -> {}", 
                        interaction.getServiceId(), interaction.getStatus());
            }
        }
    }
    
    /**
     * Record assertion execution metrics
     */
    public void recordAssertionExecution(AssertionResult result) {
        // Record assertion performance metrics
        meterRegistry.counter("assertion_execution_count",
                Tags.of(
                        "rule_id", result.getRuleId(),
                        "passed", String.valueOf(result.isSuccessful())
                )
        ).increment();
        
        // Note: AssertionResult doesn't have execution time in current model
        // This would need to be added to the model if needed
        
        logger.debug("Recorded assertion execution metrics: {} ({})", 
                result.getRuleId(), result.isSuccessful() ? "PASSED" : "FAILED");
    }
    
    /**
     * Collect detailed metrics for a specific test
     */
    public void collectDetailedMetrics(String testId, TestMetrics metrics) {
        TestMetricsData metricsData = testMetrics.get(testId);
        if (metricsData != null) {
            metricsData.setDetailedMetrics(metrics);
            
            // Record custom metrics
            if (metrics.getCustomMetrics() != null) {
                metrics.getCustomMetrics().forEach((key, value) -> {
                    Gauge.builder("test_custom_metric", () -> ((Number) value).doubleValue())
                            .tag("test_id", testId)
                            .tag("metric_name", key)
                            .register(meterRegistry);
                });
            }
            
            // Note: TestMetrics doesn't have performance metrics in current model
            // This would need to be added if needed
            
            logger.debug("Collected detailed metrics for test: {}", testId);
        }
    }
    
    /**
     * Record critical failure metrics
     */
    public void recordCriticalFailure(TestFailure failure) {
        meterRegistry.counter("critical_failure_count",
                Tags.of(
                        "type", failure.getFailureType().toString(),
                        "severity", failure.getSeverity().toString(),
                        "service", failure.getServiceId() != null ? failure.getServiceId() : "unknown"
                )
        ).increment();
        
        // Record failure timing if available
        if (failure.getTimestamp() != null) {
            Gauge.builder("last_critical_failure_timestamp", () -> failure.getTimestamp().toEpochMilli())
                    .tag("type", failure.getFailureType().toString())
                    .register(meterRegistry);
        }
        
        logger.warn("Recorded critical failure metrics: {} - {}", 
                failure.getFailureType(), failure.getErrorMessage());
    }
    
    /**
     * Get metrics data for a specific test
     */
    public TestMetricsData getTestMetrics(String testId) {
        return testMetrics.get(testId);
    }
    
    /**
     * Get aggregated metrics across all tests
     */
    public AggregatedMetrics getAggregatedMetrics() {
        List<TestMetricsData> allMetrics = new ArrayList<>(testMetrics.values());
        
        return new AggregatedMetrics(
                allMetrics.size(),
                calculateAverageExecutionTime(allMetrics),
                calculateSuccessRate(allMetrics),
                calculateServiceInteractionStats(allMetrics),
                calculateAssertionStats(allMetrics)
        );
    }
    
    /**
     * Clean up old metrics data
     */
    public void cleanupOldMetrics(Duration retentionPeriod) {
        Instant cutoff = Instant.now().minus(retentionPeriod);
        
        testMetrics.entrySet().removeIf(entry -> {
            TestMetricsData data = entry.getValue();
            return data.getEndTime() != null && data.getEndTime().isBefore(cutoff);
        });
        
        logger.debug("Cleaned up metrics older than: {}", retentionPeriod);
    }
    
    private void recordStepMetrics(String testId, List<StepResult> stepResults) {
        for (StepResult stepResult : stepResults) {
            if (stepResult.getExecutionTime() != null) {
                meterRegistry.timer("test_step_duration",
                        Tags.of(
                                "test_id", testId,
                                "step_id", stepResult.getStepId(),
                                "status", stepResult.getStatus().toString()
                        )
                ).record(stepResult.getExecutionTime());
            }
        }
    }
    
    private void recordAssertionMetrics(String testId, List<AssertionResult> assertionResults) {
        for (AssertionResult result : assertionResults) {
            meterRegistry.counter("test_assertion_count",
                    Tags.of(
                            "test_id", testId,
                            "rule_id", result.getRuleId(),
                            "passed", String.valueOf(result.isSuccessful())
                    )
            ).increment();
        }
    }
    
    private void recordPerformanceMetrics(String testId, Map<String, Number> performanceMetrics) {
        performanceMetrics.forEach((metric, value) -> {
            meterRegistry.gauge("test_performance_metric",
                    Tags.of(
                            "test_id", testId,
                            "metric", metric
                    ), value.doubleValue());
        });
    }
    
    private String findTestIdForInteraction(ServiceInteraction interaction) {
        // In a real implementation, this would use correlation IDs or other tracking mechanisms
        // For now, we'll use a simple approach
        return testMetrics.keySet().stream()
                .filter(testId -> {
                    TestMetricsData data = testMetrics.get(testId);
                    return data.getEndTime() == null; // Active test
                })
                .findFirst()
                .orElse(null);
    }
    
    private String determineTestType(TestContext context) {
        if (context.getExecutionState().containsKey("order_fulfillment")) {
            return "order_fulfillment";
        } else if (context.getExecutionState().containsKey("integration")) {
            return "integration";
        }
        return "general";
    }
    
    private Duration calculateAverageExecutionTime(List<TestMetricsData> metrics) {
        return metrics.stream()
                .filter(data -> data.getTotalExecutionTime() != null)
                .map(TestMetricsData::getTotalExecutionTime)
                .reduce(Duration.ZERO, Duration::plus)
                .dividedBy(Math.max(1, metrics.size()));
    }
    
    private double calculateSuccessRate(List<TestMetricsData> metrics) {
        long successful = metrics.stream()
                .filter(data -> data.getFinalResult() != null)
                .filter(data -> data.getFinalResult().getStatus() == TestStatus.PASSED)
                .count();
        
        return metrics.isEmpty() ? 100.0 : (double) successful / metrics.size() * 100.0;
    }
    
    private Map<String, Object> calculateServiceInteractionStats(List<TestMetricsData> metrics) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalInteractions = metrics.stream()
                .mapToLong(data -> data.getServiceInteractions().size())
                .sum();
        
        stats.put("total_interactions", totalInteractions);
        stats.put("average_per_test", metrics.isEmpty() ? 0.0 : (double) totalInteractions / metrics.size());
        
        return stats;
    }
    
    private Map<String, Object> calculateAssertionStats(List<TestMetricsData> metrics) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalAssertions = metrics.stream()
                .filter(data -> data.getFinalResult() != null)
                .filter(data -> data.getFinalResult().getAssertionResults() != null)
                .mapToLong(data -> data.getFinalResult().getAssertionResults().size())
                .sum();
        
        stats.put("total_assertions", totalAssertions);
        stats.put("average_per_test", metrics.isEmpty() ? 0.0 : (double) totalAssertions / metrics.size());
        
        return stats;
    }
}