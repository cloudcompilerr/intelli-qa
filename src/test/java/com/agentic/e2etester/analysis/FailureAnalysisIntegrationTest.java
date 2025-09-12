package com.agentic.e2etester.analysis;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class FailureAnalysisIntegrationTest {

    @Autowired
    private LLMFailureAnalyzer failureAnalyzer;

    @MockBean
    private LLMService llmService;

    @BeforeEach
    void setUp() {
        // Mock LLM service responses for consistent testing
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Root cause: Integration test analysis\nConfidence: 0.8\nCategory: TEST\nRemediation suggestions:\n- Fix integration issue\n- Update test configuration", 100, true));
    }

    @Test
    void testCompleteFailureAnalysisWorkflow_NetworkFailureCascade() {
        // Given - Simulate a network failure that cascades to multiple services
        String correlationId = "cascade-test-" + System.currentTimeMillis();
        Instant baseTime = Instant.now();
        
        List<TestFailure> cascadeFailures = Arrays.asList(
            createNetworkFailure(correlationId, baseTime, "load-balancer"),
            createServiceFailure(correlationId, baseTime.plus(30, ChronoUnit.SECONDS), "order-service"),
            createServiceFailure(correlationId, baseTime.plus(45, ChronoUnit.SECONDS), "payment-service"),
            createDataFailure(correlationId, baseTime.plus(60, ChronoUnit.SECONDS), "inventory-service")
        );

        TestContext context = createTestContext(correlationId);

        // When - Analyze each failure individually
        List<CompletableFuture<FailureAnalysis>> analysisFeatures = new ArrayList<>();
        for (TestFailure failure : cascadeFailures) {
            analysisFeatures.add(failureAnalyzer.analyzeFailure(failure, context));
        }

        // Wait for all individual analyses to complete
        List<FailureAnalysis> individualAnalyses = analysisFeatures.stream()
            .map(CompletableFuture::join)
            .toList();

        // Perform correlation analysis
        CompletableFuture<List<FailureAnalysis>> correlationAnalysis = 
            failureAnalyzer.correlateFailures(cascadeFailures);
        List<FailureAnalysis> correlatedAnalyses = correlationAnalysis.join();

        // Then - Verify individual analyses
        assertEquals(4, individualAnalyses.size());
        
        for (FailureAnalysis analysis : individualAnalyses) {
            assertNotNull(analysis);
            assertNotNull(analysis.getAnalysisId());
            assertNotNull(analysis.getRootCause());
            assertTrue(analysis.getConfidenceScore() > 0.0);
            assertFalse(analysis.getRemediationSuggestions().isEmpty());
        }

        // Verify correlation analysis
        assertEquals(1, correlatedAnalyses.size());
        FailureAnalysis correlatedAnalysis = correlatedAnalyses.get(0);
        
        assertNotNull(correlatedAnalysis);
        assertEquals("CORRELATED_FAILURE", correlatedAnalysis.getRootCauseCategory());
        assertEquals(4, correlatedAnalysis.getRelatedFailures().size());
        assertTrue(correlatedAnalysis.getFailureId().startsWith("CORRELATED-"));
    }

    @Test
    void testFailureAnalysisWithPatternLearning_DatabaseConnectionIssues() {
        // Given - Historical database connection failures
        List<TestFailure> historicalFailures = Arrays.asList(
            createDataFailure("hist-1", Instant.now().minus(1, ChronoUnit.DAYS), "user-service"),
            createDataFailure("hist-2", Instant.now().minus(1, ChronoUnit.DAYS), "order-service"),
            createDataFailure("hist-3", Instant.now().minus(2, ChronoUnit.DAYS), "payment-service")
        );

        // Current failure
        TestFailure currentFailure = createDataFailure("current-1", Instant.now(), "inventory-service");
        TestContext context = createTestContext("current-correlation");

        // When - Learn from historical patterns
        CompletableFuture<Void> learningResult = failureAnalyzer.learnFromPatterns(historicalFailures);
        learningResult.join();

        // Analyze current failure
        FailureAnalysis currentAnalysis = failureAnalyzer.analyzeFailure(currentFailure, context).join();

        // Then - Verify analysis incorporates learning
        assertNotNull(currentAnalysis);
        assertNotNull(currentAnalysis.getRootCause());
        assertTrue(currentAnalysis.getConfidenceScore() > 0.0);
        
        // Should have remediation suggestions
        assertFalse(currentAnalysis.getRemediationSuggestions().isEmpty());
        
        // Should have analysis metadata with pattern insights
        assertNotNull(currentAnalysis.getAnalysisMetadata());
    }

    @Test
    void testFailureAnalysisWithDifferentFailureTypes_ComprehensiveScenario() {
        // Given - Various failure types in a complex microservices scenario
        String correlationId = "comprehensive-test-" + System.currentTimeMillis();
        Instant baseTime = Instant.now();
        
        List<TestFailure> diverseFailures = Arrays.asList(
            createAuthenticationFailure(correlationId, baseTime, "auth-service"),
            createTimeoutFailure(correlationId, baseTime.plus(10, ChronoUnit.SECONDS), "gateway-service"),
            createConfigurationFailure(correlationId, baseTime.plus(20, ChronoUnit.SECONDS), "config-service"),
            createInfrastructureFailure(correlationId, baseTime.plus(30, ChronoUnit.SECONDS), "kubernetes-cluster")
        );

        TestContext context = createTestContext(correlationId);

        // When - Analyze all failures
        List<FailureAnalysis> analyses = new ArrayList<>();
        for (TestFailure failure : diverseFailures) {
            FailureAnalysis analysis = failureAnalyzer.analyzeFailure(failure, context).join();
            analyses.add(analysis);
        }

        // Then - Verify each failure type is analyzed appropriately
        assertEquals(4, analyses.size());

        // Verify authentication failure analysis
        FailureAnalysis authAnalysis = analyses.get(0);
        assertNotNull(authAnalysis);
        assertTrue(authAnalysis.getRemediationSuggestions().stream()
            .anyMatch(s -> s.getDescription().toLowerCase().contains("integration")));

        // Verify timeout failure analysis
        FailureAnalysis timeoutAnalysis = analyses.get(1);
        assertNotNull(timeoutAnalysis);
        assertTrue(timeoutAnalysis.getConfidenceScore() > 0.0);

        // Verify configuration failure analysis
        FailureAnalysis configAnalysis = analyses.get(2);
        assertNotNull(configAnalysis);
        assertFalse(configAnalysis.getRemediationSuggestions().isEmpty());

        // Verify infrastructure failure analysis
        FailureAnalysis infraAnalysis = analyses.get(3);
        assertNotNull(infraAnalysis);
        assertNotNull(infraAnalysis.getRootCause());
    }

    @Test
    void testFailureAnalysisErrorHandling_LLMServiceFailure() {
        // Given - A failure when LLM service is unavailable
        TestFailure failure = createNetworkFailure("error-test", Instant.now(), "test-service");
        TestContext context = createTestContext("error-correlation");

        // Mock LLM service to throw exception
        when(llmService.sendPrompt(anyString()))
            .thenThrow(new RuntimeException("LLM service unavailable"));

        // When - Analyze failure with LLM service error
        FailureAnalysis analysis = failureAnalyzer.analyzeFailure(failure, context).join();

        // Then - Should return fallback analysis
        assertNotNull(analysis);
        assertEquals(failure.getFailureId(), analysis.getFailureId());
        assertTrue(analysis.getRootCause().contains("Analysis failed"));
        assertEquals(0.1, analysis.getConfidenceScore(), 0.01);
        assertEquals("UNKNOWN", analysis.getRootCauseCategory());
        assertEquals("Fallback-Analysis", analysis.getAnalysisModel());

        // Should have fallback remediation suggestion
        assertEquals(1, analysis.getRemediationSuggestions().size());
        RemediationSuggestion suggestion = analysis.getRemediationSuggestions().get(0);
        assertEquals(RemediationType.MANUAL_INTERVENTION, suggestion.getType());
        assertTrue(suggestion.getDescription().contains("Manual investigation required"));
    }

    @Test
    void testFailureAnalysisPerformance_MultipleFailuresParallel() {
        // Given - Multiple failures to analyze in parallel
        List<TestFailure> failures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            failures.add(createServiceFailure("perf-test-" + i, Instant.now(), "service-" + i));
        }

        TestContext context = createTestContext("performance-test");

        // When - Analyze all failures in parallel
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<FailureAnalysis>> analysisFeatures = failures.stream()
            .map(failure -> failureAnalyzer.analyzeFailure(failure, context))
            .toList();

        List<FailureAnalysis> analyses = analysisFeatures.stream()
            .map(CompletableFuture::join)
            .toList();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then - Verify all analyses completed successfully
        assertEquals(10, analyses.size());
        
        for (FailureAnalysis analysis : analyses) {
            assertNotNull(analysis);
            assertNotNull(analysis.getAnalysisId());
            assertNotNull(analysis.getRootCause());
            assertTrue(analysis.getConfidenceScore() > 0.0);
        }

        // Verify reasonable performance (should complete within 10 seconds for 10 analyses)
        assertTrue(executionTime < 10000, "Analysis took too long: " + executionTime + "ms");
    }

    // Helper methods to create different types of test failures

    private TestFailure createNetworkFailure(String correlationId, Instant timestamp, String serviceId) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("integration-test");
        failure.setFailureType(FailureType.NETWORK_FAILURE);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage("Network connection timeout");
        failure.setTimestamp(timestamp);
        failure.setServiceId(serviceId);
        failure.setCorrelationId(correlationId);
        failure.setLogEntries(Arrays.asList(
            "ERROR: Connection timeout after 30 seconds",
            "WARN: Retrying connection to " + serviceId
        ));
        return failure;
    }

    private TestFailure createServiceFailure(String correlationId, Instant timestamp, String serviceId) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("integration-test");
        failure.setFailureType(FailureType.SERVICE_FAILURE);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage("Service unavailable - HTTP 503");
        failure.setTimestamp(timestamp);
        failure.setServiceId(serviceId);
        failure.setCorrelationId(correlationId);
        failure.setLogEntries(Arrays.asList(
            "ERROR: Service " + serviceId + " returned HTTP 503",
            "INFO: Circuit breaker opened for " + serviceId
        ));
        return failure;
    }

    private TestFailure createDataFailure(String correlationId, Instant timestamp, String serviceId) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("integration-test");
        failure.setFailureType(FailureType.DATA_FAILURE);
        failure.setSeverity(FailureSeverity.MEDIUM);
        failure.setErrorMessage("Database connection pool exhausted");
        failure.setTimestamp(timestamp);
        failure.setServiceId(serviceId);
        failure.setCorrelationId(correlationId);
        failure.setLogEntries(Arrays.asList(
            "ERROR: Connection pool exhausted for database",
            "WARN: Unable to acquire database connection within timeout"
        ));
        return failure;
    }

    private TestFailure createAuthenticationFailure(String correlationId, Instant timestamp, String serviceId) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("integration-test");
        failure.setFailureType(FailureType.AUTHENTICATION_FAILURE);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage("JWT token validation failed");
        failure.setTimestamp(timestamp);
        failure.setServiceId(serviceId);
        failure.setCorrelationId(correlationId);
        failure.setLogEntries(Arrays.asList(
            "ERROR: Invalid JWT token signature",
            "WARN: Authentication failed for user request"
        ));
        return failure;
    }

    private TestFailure createTimeoutFailure(String correlationId, Instant timestamp, String serviceId) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("integration-test");
        failure.setFailureType(FailureType.TIMEOUT_FAILURE);
        failure.setSeverity(FailureSeverity.MEDIUM);
        failure.setErrorMessage("Request timeout after 60 seconds");
        failure.setTimestamp(timestamp);
        failure.setServiceId(serviceId);
        failure.setCorrelationId(correlationId);
        failure.setLogEntries(Arrays.asList(
            "ERROR: Request timeout exceeded",
            "INFO: Downstream service response time: 60000ms"
        ));
        return failure;
    }

    private TestFailure createConfigurationFailure(String correlationId, Instant timestamp, String serviceId) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("integration-test");
        failure.setFailureType(FailureType.CONFIGURATION_FAILURE);
        failure.setSeverity(FailureSeverity.MEDIUM);
        failure.setErrorMessage("Missing required configuration property");
        failure.setTimestamp(timestamp);
        failure.setServiceId(serviceId);
        failure.setCorrelationId(correlationId);
        failure.setLogEntries(Arrays.asList(
            "ERROR: Configuration property 'database.url' not found",
            "FATAL: Service startup failed due to configuration error"
        ));
        return failure;
    }

    private TestFailure createInfrastructureFailure(String correlationId, Instant timestamp, String serviceId) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("integration-test");
        failure.setFailureType(FailureType.INFRASTRUCTURE_FAILURE);
        failure.setSeverity(FailureSeverity.CRITICAL);
        failure.setErrorMessage("Pod evicted due to resource constraints");
        failure.setTimestamp(timestamp);
        failure.setServiceId(serviceId);
        failure.setCorrelationId(correlationId);
        failure.setLogEntries(Arrays.asList(
            "ERROR: Pod evicted - insufficient memory",
            "WARN: Node resource pressure detected"
        ));
        return failure;
    }

    private TestContext createTestContext(String correlationId) {
        TestContext context = new TestContext();
        context.setTestExecutionPlanId("integration-test-" + System.currentTimeMillis());
        context.setCorrelationId(correlationId);
        context.setExecutionState(new HashMap<>());
        context.setInteractions(new ArrayList<>());
        context.setEvents(new ArrayList<>());
        
        // Add some test metrics
        TestMetrics metrics = new TestMetrics();
        metrics.setTotalExecutionTimeMs(300000L); // 5 minutes
        metrics.setTotalRequests(10);
        metrics.setSuccessfulRequests(7);
        metrics.setFailedRequests(3);
        context.setMetrics(metrics);
        
        return context;
    }
}