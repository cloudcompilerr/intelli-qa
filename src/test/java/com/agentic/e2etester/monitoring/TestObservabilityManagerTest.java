package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestObservabilityManagerTest {
    
    @Mock
    private TestMetricsCollector metricsCollector;
    
    @Mock
    private TestDashboardService dashboardService;
    
    private MeterRegistry meterRegistry;
    private TestObservabilityManager observabilityManager;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        observabilityManager = new TestObservabilityManager(
                meterRegistry, metricsCollector, dashboardService);
    }
    
    @Test
    void shouldTrackTestStart() {
        // Given
        TestContext context = new TestContext("test-123");
        context.getExecutionState().put("environment", "test");
        
        // When
        observabilityManager.trackTestStart(context);
        
        // Then
        verify(metricsCollector).recordTestStart(context);
        verify(dashboardService).updateTestStatus("test-123", "RUNNING");
        
        // Verify metrics
        assertThat(meterRegistry.counter("test_executions_total").count()).isEqualTo(1.0);
        assertThat(observabilityManager.getSystemHealthMetrics().get("active_tests")).isEqualTo(1L);
    }
    
    @Test
    void shouldTrackTestCompletion() {
        // Given
        TestContext context = new TestContext("test-123");
        TestResult result = new TestResult("test-123", TestStatus.PASSED);
        result.setStartTime(Instant.now().minusSeconds(60));
        result.setEndTime(Instant.now());
        
        observabilityManager.trackTestStart(context);
        
        // When
        observabilityManager.trackTestCompletion(result);
        
        // Then
        verify(metricsCollector).recordTestCompletion(result);
        verify(dashboardService).updateTestStatus("test-123", "PASSED");
        
        // Verify metrics
        assertThat(observabilityManager.getSystemHealthMetrics().get("active_tests")).isEqualTo(0L);
        assertThat(meterRegistry.timer("test_execution_duration").count()).isEqualTo(1L);
    }
    
    @Test
    void shouldTrackServiceInteraction() {
        // Given
        ServiceInteraction interaction = new ServiceInteraction(
                "order-service",
                InteractionType.HTTP_REQUEST,
                InteractionStatus.SUCCESS
        );
        interaction.setTimestamp(Instant.now());
        interaction.setRequest("request-data");
        interaction.setResponse("response-data");
        interaction.setResponseTime(Duration.ofMillis(150));
        
        // When
        observabilityManager.trackServiceInteraction(interaction);
        
        // Then
        verify(metricsCollector).recordServiceInteraction(interaction);
        verify(dashboardService).updateServiceMetrics(interaction);
        
        // Verify metrics
        assertThat(meterRegistry.counter("service_interactions_total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer("service_response_time").count()).isEqualTo(1L);
    }
    
    @Test
    void shouldTrackAssertionExecution() {
        // Given
        AssertionResult result = new AssertionResult("test-rule", true);
        
        // When
        observabilityManager.trackAssertionExecution(result);
        
        // Then
        verify(metricsCollector).recordAssertionExecution(result);
        verify(dashboardService).updateAssertionMetrics(result);
        
        // Verify metrics
        assertThat(meterRegistry.counter("assertion_executions_total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("assertion_failures_total").count()).isEqualTo(0.0);
    }
    
    @Test
    void shouldTrackAssertionFailure() {
        // Given
        AssertionResult result = new AssertionResult("test-rule", false);
        
        // When
        observabilityManager.trackAssertionExecution(result);
        
        // Then
        verify(metricsCollector).recordAssertionExecution(result);
        
        // Verify failure metrics
        assertThat(meterRegistry.counter("assertion_failures_total").count()).isEqualTo(1.0);
    }
    
    @Test
    void shouldCollectMetrics() {
        // Given
        String testId = "test-123";
        TestMetrics metrics = new TestMetrics();
        metrics.getCustomMetrics().put("custom_metric", 100);
        
        // When
        observabilityManager.collectMetrics(testId, metrics);
        
        // Then
        verify(metricsCollector).collectDetailedMetrics(testId, metrics);
        verify(dashboardService).updateMetrics(testId, metrics);
    }
    
    @Test
    void shouldAlertOnCriticalFailure() {
        // Given
        TestFailure failure = new TestFailure();
        failure.setFailureType(FailureType.SERVICE_FAILURE);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage("Service is unavailable");
        failure.setServiceId("order-service");
        
        // When
        observabilityManager.alertOnCriticalFailure(failure);
        
        // Then
        verify(dashboardService).triggerAlert(failure);
        verify(metricsCollector).recordCriticalFailure(failure);
        
        // Verify critical failure metrics
        assertThat(meterRegistry.counter("critical_failures_total").count()).isEqualTo(1.0);
    }
    
    @Test
    void shouldGetSystemHealthMetrics() {
        // Given
        TestContext context = new TestContext("test-123");
        observabilityManager.trackTestStart(context);
        
        // When
        Map<String, Object> healthMetrics = observabilityManager.getSystemHealthMetrics();
        
        // Then
        assertThat(healthMetrics).containsKeys(
                "active_tests", "total_executions", "total_failures", 
                "success_rate", "average_execution_time"
        );
        assertThat(healthMetrics.get("active_tests")).isEqualTo(1L);
        assertThat(healthMetrics.get("success_rate")).isEqualTo(100.0);
    }
}