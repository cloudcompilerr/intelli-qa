package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TestDashboardServiceTest {
    
    private TestDashboardService dashboardService;
    
    @BeforeEach
    void setUp() {
        dashboardService = new TestDashboardService();
    }
    
    @Test
    void shouldUpdateTestStatus() {
        // When
        dashboardService.updateTestStatus("test-123", "RUNNING");
        
        // Then
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        assertThat(dashboardData.getTestStatuses()).hasSize(1);
        
        TestDashboardService.TestStatusInfo statusInfo = dashboardData.getTestStatuses().get(0);
        assertThat(statusInfo.getTestId()).isEqualTo("test-123");
        assertThat(statusInfo.getStatus()).isEqualTo("RUNNING");
    }
    
    @Test
    void shouldUpdateServiceMetrics() {
        // Given
        ServiceInteraction interaction = new ServiceInteraction(
                "order-service",
                InteractionType.HTTP_REQUEST,
                InteractionStatus.SUCCESS
        );
        interaction.setTimestamp(Instant.now());
        interaction.setRequest("request");
        interaction.setResponse("response");
        interaction.setResponseTime(Duration.ofMillis(150));
        
        // When
        dashboardService.updateServiceMetrics(interaction);
        
        // Then
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        assertThat(dashboardData.getServiceMetrics()).hasSize(1);
        
        TestDashboardService.ServiceMetricsInfo metricsInfo = dashboardData.getServiceMetrics().get(0);
        assertThat(metricsInfo.getServiceId()).isEqualTo("order-service");
        assertThat(metricsInfo.getTotalInteractions()).isEqualTo(1);
        assertThat(metricsInfo.getSuccessfulInteractions()).isEqualTo(1);
    }
    
    @Test
    void shouldUpdateAssertionMetrics() {
        // Given
        dashboardService.updateTestStatus("test-123", "RUNNING");
        AssertionResult result = new AssertionResult("test-rule", true);
        result.setStepId("test-123-step-1");
        
        // When
        dashboardService.updateAssertionMetrics(result);
        
        // Then
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        TestDashboardService.TestStatusInfo statusInfo = dashboardData.getTestStatuses().get(0);
        assertThat(statusInfo.getTotalAssertions()).isEqualTo(1);
        assertThat(statusInfo.getPassedAssertions()).isEqualTo(1);
    }
    
    @Test
    void shouldTriggerAlert() {
        // Given
        TestFailure failure = new TestFailure();
        failure.setFailureType(FailureType.SERVICE_FAILURE);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage("Service is unavailable");
        
        // When
        dashboardService.triggerAlert(failure);
        
        // Then
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        assertThat(dashboardData.getAlerts()).hasSize(1);
        
        TestDashboardService.AlertInfo alert = dashboardData.getAlerts().get(0);
        assertThat(alert.getType()).isEqualTo("SERVICE_FAILURE");
        assertThat(alert.getSeverity()).isEqualTo("HIGH");
        assertThat(alert.getMessage()).isEqualTo("Service is unavailable");
    }
    
    @Test
    void shouldGetTestStatusSummary() {
        // Given
        dashboardService.updateTestStatus("test-123", "RUNNING");
        dashboardService.updateTestStatus("test-456", "PASSED");
        dashboardService.updateTestStatus("test-789", "FAILED");
        
        // When
        TestStatusSummary summary = dashboardService.getTestStatusSummary();
        
        // Then
        assertThat(summary.getTotalTests()).isEqualTo(3);
        assertThat(summary.getRunningTests()).isEqualTo(1);
        assertThat(summary.getPassedTests()).isEqualTo(1);
        assertThat(summary.getFailedTests()).isEqualTo(1);
    }
    
    @Test
    void shouldGetServiceHealthSummary() {
        // Given
        ServiceInteraction successfulInteraction = new ServiceInteraction(
                "order-service",
                InteractionType.HTTP_REQUEST,
                InteractionStatus.SUCCESS
        );
        successfulInteraction.setTimestamp(Instant.now());
        successfulInteraction.setRequest("request");
        successfulInteraction.setResponse("response");
        successfulInteraction.setResponseTime(Duration.ofMillis(150));
        
        ServiceInteraction failedInteraction = new ServiceInteraction(
                "payment-service",
                InteractionType.HTTP_REQUEST,
                InteractionStatus.FAILURE
        );
        failedInteraction.setTimestamp(Instant.now());
        failedInteraction.setRequest("request");
        failedInteraction.setResponse("error");
        failedInteraction.setResponseTime(Duration.ofMillis(5000));
        
        dashboardService.updateServiceMetrics(successfulInteraction);
        dashboardService.updateServiceMetrics(failedInteraction);
        
        // When
        ServiceHealthSummary summary = dashboardService.getServiceHealthSummary();
        
        // Then
        assertThat(summary.getServiceHealth()).hasSize(2);
        assertThat(summary.getServiceHealth()).containsKeys("order-service", "payment-service");
        
        // order-service should be healthy (100% success rate)
        ServiceHealthSummary.ServiceHealthInfo orderServiceHealth = 
                summary.getServiceHealth().get("order-service");
        assertThat(orderServiceHealth.isHealthy()).isTrue();
        
        // payment-service should be unhealthy (0% success rate)
        ServiceHealthSummary.ServiceHealthInfo paymentServiceHealth = 
                summary.getServiceHealth().get("payment-service");
        assertThat(paymentServiceHealth.isHealthy()).isFalse();
    }
    
    @Test
    void shouldClearCompletedTests() {
        // Given
        dashboardService.updateTestStatus("test-123", "PASSED");
        dashboardService.updateTestStatus("test-456", "RUNNING");
        
        // When
        dashboardService.clearCompletedTests(0); // Clear immediately
        
        // Then
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        assertThat(dashboardData.getTestStatuses()).hasSize(1);
        assertThat(dashboardData.getTestStatuses().get(0).getStatus()).isEqualTo("RUNNING");
    }
    
    @Test
    void shouldLimitAlerts() {
        // Given - Create more than 100 alerts
        for (int i = 0; i < 150; i++) {
            TestFailure failure = new TestFailure();
            failure.setFailureType(FailureType.NETWORK_FAILURE);
            failure.setSeverity(FailureSeverity.MEDIUM);
            failure.setErrorMessage("Test failure " + i);
            dashboardService.triggerAlert(failure);
        }
        
        // When
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        
        // Then - Should keep only the last 100 alerts
        assertThat(dashboardData.getAlerts()).hasSize(100);
    }
}