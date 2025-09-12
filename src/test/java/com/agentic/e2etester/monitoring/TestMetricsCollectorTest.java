package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestMetricsCollectorTest {
    
    private MeterRegistry meterRegistry;
    private TestMetricsCollector metricsCollector;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsCollector = new TestMetricsCollector(meterRegistry);
    }
    
    @Test
    void shouldRecordTestStart() {
        // Given
        TestContext context = new TestContext("test-123");
        context.getExecutionState().put("order_fulfillment", true);
        
        // When
        metricsCollector.recordTestStart(context);
        
        // Then
        TestMetricsData metricsData = metricsCollector.getTestMetrics("test-123");
        assertThat(metricsData).isNotNull();
        assertThat(metricsData.getTestId()).isEqualTo("test-123");
        assertThat(metricsData.getTestType()).isEqualTo("order_fulfillment");
        assertThat(metricsData.getStartTime()).isNotNull();
    }
    
    @Test
    void shouldRecordTestCompletion() {
        // Given
        TestContext context = new TestContext("test-123");
        TestResult result = new TestResult("test-123", TestStatus.PASSED);
        result.setStartTime(Instant.now().minusSeconds(30));
        result.setEndTime(Instant.now());
        
        metricsCollector.recordTestStart(context);
        
        // When
        metricsCollector.recordTestCompletion(result);
        
        // Then
        TestMetricsData metricsData = metricsCollector.getTestMetrics("test-123");
        assertThat(metricsData.getEndTime()).isNotNull();
        assertThat(metricsData.getFinalResult()).isEqualTo(result);
        assertThat(metricsData.getTotalExecutionTime()).isNotNull();
    }
    
    @Test
    void shouldRecordServiceInteraction() {
        // Given
        TestContext context = new TestContext("test-123");
        ServiceInteraction interaction = new ServiceInteraction(
                "order-service",
                InteractionType.HTTP_REQUEST,
                InteractionStatus.SUCCESS
        );
        interaction.setTimestamp(Instant.now());
        interaction.setRequest("request");
        interaction.setResponse("response");
        interaction.setResponseTime(Duration.ofMillis(150));
        
        metricsCollector.recordTestStart(context);
        
        // When
        metricsCollector.recordServiceInteraction(interaction);
        
        // Then
        assertThat(meterRegistry.timer("service_interaction_duration").count()).isEqualTo(1L);
    }
    
    @Test
    void shouldRecordAssertionExecution() {
        // Given
        AssertionResult result = new AssertionResult("test-rule", true);
        
        // When
        metricsCollector.recordAssertionExecution(result);
        
        // Then
        assertThat(meterRegistry.counter("assertion_execution_count").count()).isEqualTo(1.0);
    }
    
    @Test
    void shouldCollectDetailedMetrics() {
        // Given
        TestContext context = new TestContext("test-123");
        TestMetrics metrics = new TestMetrics();
        metrics.getCustomMetrics().put("custom_metric_1", 100);
        metrics.getCustomMetrics().put("custom_metric_2", 200.5);
        
        metricsCollector.recordTestStart(context);
        
        // When
        metricsCollector.collectDetailedMetrics("test-123", metrics);
        
        // Then
        TestMetricsData metricsData = metricsCollector.getTestMetrics("test-123");
        assertThat(metricsData.getDetailedMetrics()).isEqualTo(metrics);
    }
    
    @Test
    void shouldRecordCriticalFailure() {
        // Given
        TestFailure failure = new TestFailure();
        failure.setFailureType(FailureType.SERVICE_FAILURE);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage("Service is unavailable");
        failure.setTimestamp(Instant.now());
        
        // When
        metricsCollector.recordCriticalFailure(failure);
        
        // Then
        assertThat(meterRegistry.counter("critical_failure_count").count()).isEqualTo(1.0);
    }
    
    @Test
    void shouldGetAggregatedMetrics() {
        // Given
        TestContext context1 = new TestContext("test-123");
        TestContext context2 = new TestContext("test-456");
        TestResult result1 = new TestResult("test-123", TestStatus.PASSED);
        TestResult result2 = new TestResult("test-456", TestStatus.PASSED);
        
        metricsCollector.recordTestStart(context1);
        metricsCollector.recordTestStart(context2);
        metricsCollector.recordTestCompletion(result1);
        metricsCollector.recordTestCompletion(result2);
        
        // When
        AggregatedMetrics aggregated = metricsCollector.getAggregatedMetrics();
        
        // Then
        assertThat(aggregated.getTotalTests()).isEqualTo(2);
        assertThat(aggregated.getSuccessRate()).isEqualTo(100.0);
        assertThat(aggregated.getAverageExecutionTime()).isNotNull();
    }
    
    @Test
    void shouldCleanupOldMetrics() {
        // Given
        TestContext context = new TestContext("test-123");
        TestResult result = new TestResult("test-123", TestStatus.PASSED);
        result.setEndTime(Instant.now());
        
        metricsCollector.recordTestStart(context);
        metricsCollector.recordTestCompletion(result);
        
        // When
        metricsCollector.cleanupOldMetrics(Duration.ofHours(1));
        
        // Then
        // Metrics should still be there since they're recent
        assertThat(metricsCollector.getTestMetrics("test-123")).isNotNull();
    }
}