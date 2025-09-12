package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.ai.ollama.base-url=http://localhost:11434",
        "spring.couchbase.connection-string=couchbase://localhost",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class MonitoringIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TestObservabilityManager observabilityManager;
    
    @Autowired
    private TestMetricsCollector metricsCollector;
    
    @Autowired
    private TestDashboardService dashboardService;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Test
    void shouldExposeMonitoringEndpoints() {
        // Test dashboard endpoint
        ResponseEntity<DashboardData> dashboardResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/monitoring/dashboard", 
                DashboardData.class
        );
        assertThat(dashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboardResponse.getBody()).isNotNull();
        
        // Test health endpoint
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/monitoring/health", 
                Map.class
        );
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).containsKeys("active_tests", "total_executions");
        
        // Test Prometheus metrics endpoint
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/monitoring/metrics", 
                String.class
        );
        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metricsResponse.getBody()).contains("# HELP");
    }
    
    @Test
    void shouldTrackCompleteTestLifecycle() {
        // Given
        TestContext context = new TestContext("integration-test-123");
        context.getExecutionState().put("environment", "integration-test");
        context.getExecutionState().put("order_fulfillment", true);
        
        // When - Start test
        observabilityManager.trackTestStart(context);
        
        // Track service interactions
        ServiceInteraction interaction = new ServiceInteraction(
                "order-service",
                InteractionType.HTTP_REQUEST,
                InteractionStatus.SUCCESS
        );
        interaction.setTimestamp(Instant.now());
        interaction.setRequest("integration-request");
        interaction.setResponse("integration-response");
        interaction.setResponseTime(Duration.ofMillis(200));
        observabilityManager.trackServiceInteraction(interaction);
        
        // Track assertions
        AssertionResult assertion = new AssertionResult("integration-assertion", true);
        observabilityManager.trackAssertionExecution(assertion);
        
        // Complete test
        TestResult result = new TestResult("integration-test-123", TestStatus.PASSED);
        result.setStartTime(Instant.now().minusSeconds(30));
        result.setEndTime(Instant.now());
        observabilityManager.trackTestCompletion(result);
        
        // Then - Verify metrics are recorded
        Map<String, Object> healthMetrics = observabilityManager.getSystemHealthMetrics();
        assertThat(healthMetrics.get("total_executions")).isEqualTo(1.0);
        assertThat(healthMetrics.get("active_tests")).isEqualTo(0L);
        
        // Verify dashboard data
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        assertThat(dashboardData.getTestStatuses()).hasSize(1);
        assertThat(dashboardData.getServiceMetrics()).hasSize(1);
        
        // Verify Micrometer metrics
        assertThat(meterRegistry.counter("test_executions_total").count()).isGreaterThan(0);
        assertThat(meterRegistry.counter("service_interactions_total").count()).isGreaterThan(0);
        assertThat(meterRegistry.counter("assertion_executions_total").count()).isGreaterThan(0);
    }
    
    @Test
    void shouldHandleTestFailureScenario() {
        // Given
        TestContext context = new TestContext("failure-test-456");
        
        // When - Start test and simulate failure
        observabilityManager.trackTestStart(context);
        
        // Simulate failed assertion
        AssertionResult failedAssertion = new AssertionResult("failed-assertion", false);
        observabilityManager.trackAssertionExecution(failedAssertion);
        
        // Simulate critical failure
        TestFailure criticalFailure = new TestFailure();
        criticalFailure.setFailureType(FailureType.SERVICE_FAILURE);
        criticalFailure.setSeverity(FailureSeverity.CRITICAL);
        criticalFailure.setErrorMessage("Integration test service failure");
        criticalFailure.setServiceId("order-service");
        observabilityManager.alertOnCriticalFailure(criticalFailure);
        
        // Complete test with failure
        TestResult result = new TestResult("failure-test-456", TestStatus.FAILED);
        result.setStartTime(Instant.now().minusSeconds(30));
        result.setEndTime(Instant.now());
        observabilityManager.trackTestCompletion(result);
        
        // Then - Verify failure metrics
        Map<String, Object> healthMetrics = observabilityManager.getSystemHealthMetrics();
        assertThat(healthMetrics.get("total_failures")).isEqualTo(1.0);
        
        // Verify alerts
        DashboardData dashboardData = dashboardService.getCurrentDashboardData();
        assertThat(dashboardData.getAlerts()).hasSize(1);
        
        // Verify failure counters
        assertThat(meterRegistry.counter("test_failures_total").count()).isGreaterThan(0);
        assertThat(meterRegistry.counter("assertion_failures_total").count()).isGreaterThan(0);
        assertThat(meterRegistry.counter("critical_failures_total").count()).isGreaterThan(0);
    }
    
    @Test
    void shouldProvideRealTimeReporting() {
        // Given
        TestContext context = new TestContext("realtime-test-789");
        
        // When
        observabilityManager.trackTestStart(context);
        
        // Then - Should be able to get real-time report
        TestExecutionReport report = observabilityManager.generateRealTimeReport("realtime-test-789");
        assertThat(report).isNotNull();
        assertThat(report.getTestId()).isEqualTo("realtime-test-789");
        assertThat(report.getStatus()).isEqualTo("RUNNING");
        
        // Should appear in active tests
        assertThat(observabilityManager.getActiveTestSummaries())
                .extracting(TestExecutionSummary::getTestId)
                .contains("realtime-test-789");
    }
}