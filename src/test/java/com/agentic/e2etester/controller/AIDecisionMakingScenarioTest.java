package com.agentic.e2etester.controller;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.ai.TestScenarioParser;
import com.agentic.e2etester.analysis.FailureAnalyzer;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.orchestration.TestOrchestrator;
import com.agentic.e2etester.service.TestMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for AI decision-making scenarios in the AgenticTestController.
 * Tests various decision-making situations that the AI agent might encounter during test execution.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI Decision-Making Scenarios")
class AIDecisionMakingScenarioTest {
    
    @Mock
    private LLMService llmService;
    
    @Mock
    private TestScenarioParser scenarioParser;
    
    @Mock
    private TestOrchestrator testOrchestrator;
    
    @Mock
    private TestMemoryService testMemoryService;
    
    @Mock
    private FailureAnalyzer failureAnalyzer;
    
    private AgenticTestController agenticTestController;
    
    @BeforeEach
    void setUp() {
        agenticTestController = new AgenticTestController(
            llmService,
            scenarioParser,
            testOrchestrator,
            testMemoryService,
            failureAnalyzer
        );
    }
    
    @Test
    @DisplayName("AI decides to continue when services are healthy and patterns are positive")
    void testDecisionMaking_ContinueWithHealthyServices() throws Exception {
        // Given - Healthy system state
        TestContext context = createHealthySystemContext();
        List<TestPattern> successPatterns = createSuccessfulPatterns();
        List<TestExecutionHistory> positiveHistory = createPositiveExecutionHistory();
        
        when(testMemoryService.findSimilarPatterns(context, 3))
            .thenReturn(successPatterns);
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(positiveHistory);
        when(llmService.sendPrompt(contains("Make a test execution decision")))
            .thenReturn(new LLMService.LLMResponse(
                "CONTINUE - All services are healthy, similar patterns show 95% success rate, " +
                "system load is optimal. High confidence to proceed with current execution plan.", 
                800, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.CONTINUE, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.8);
        assertTrue(decision.getReasoning().contains("healthy"));
        assertTrue(decision.getReasoning().contains("95% success rate"));
        
        verify(testMemoryService).findSimilarPatterns(context, 3);
        verify(testMemoryService).findRecentExecutionHistory(10);
        verify(llmService).sendPrompt(anyString());
    }
    
    @Test
    @DisplayName("AI decides to retry when encountering transient failures")
    void testDecisionMaking_RetryOnTransientFailure() throws Exception {
        // Given - Context with transient failure
        TestContext context = createTransientFailureContext();
        List<TestPattern> retryPatterns = createRetrySuccessPatterns();
        
        when(testMemoryService.findSimilarPatterns(context, 3))
            .thenReturn(retryPatterns);
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(createMixedExecutionHistory());
        when(llmService.sendPrompt(contains("Make a test execution decision")))
            .thenReturn(new LLMService.LLMResponse(
                "RETRY - Detected transient network timeout. Similar patterns show 80% success " +
                "rate on retry with exponential backoff. Recommend retry with 2-second delay.", 
                1200, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.RETRY, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.7);
        assertTrue(decision.getReasoning().contains("transient"));
        assertTrue(decision.getReasoning().contains("exponential backoff"));
    }
    
    @Test
    @DisplayName("AI decides to adapt execution when system is under high load")
    void testDecisionMaking_AdaptForHighLoad() throws Exception {
        // Given - High load system context
        TestContext context = createHighLoadSystemContext();
        List<TestPattern> loadPatterns = createHighLoadPatterns();
        
        when(testMemoryService.findSimilarPatterns(context, 3))
            .thenReturn(loadPatterns);
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(createHighLoadHistory());
        when(llmService.sendPrompt(contains("Make a test execution decision")))
            .thenReturn(new LLMService.LLMResponse(
                "ADAPT - System load at 92%, response times degraded. Historical patterns show " +
                "success with reduced parallelism and increased timeouts. Adapt execution strategy.", 
                1500, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.ADAPT, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.6);
        assertTrue(decision.getReasoning().contains("92%"));
        assertTrue(decision.getReasoning().contains("reduced parallelism"));
    }
    
    @Test
    @DisplayName("AI decides to abort when critical services are down")
    void testDecisionMaking_AbortOnCriticalFailure() throws Exception {
        // Given - Critical service failure context
        TestContext context = createCriticalFailureContext();
        List<TestPattern> failurePatterns = createCriticalFailurePatterns();
        
        when(testMemoryService.findSimilarPatterns(context, 3))
            .thenReturn(failurePatterns);
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(createFailureHistory());
        when(llmService.sendPrompt(contains("Make a test execution decision")))
            .thenReturn(new LLMService.LLMResponse(
                "ABORT - Critical payment service is down for 15+ minutes. Historical data shows " +
                "0% success rate during payment service outages. Recommend aborting and alerting.", 
                900, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.ABORT, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.9);
        assertTrue(decision.getReasoning().contains("Critical payment service"));
        assertTrue(decision.getReasoning().contains("0% success rate"));
    }
    
    @Test
    @DisplayName("AI decides to investigate when encountering unknown failure patterns")
    void testDecisionMaking_InvestigateUnknownPattern() throws Exception {
        // Given - Unknown failure pattern context
        TestContext context = createUnknownFailureContext();
        
        when(testMemoryService.findSimilarPatterns(context, 3))
            .thenReturn(Arrays.asList()); // No similar patterns found
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(createNormalHistory());
        when(llmService.sendPrompt(contains("Make a test execution decision")))
            .thenReturn(new LLMService.LLMResponse(
                "INVESTIGATE - Encountered unknown error pattern: 'COUCHBASE_VECTOR_SEARCH_TIMEOUT'. " +
                "No similar patterns in memory. Recommend collecting additional diagnostic information.", 
                1800, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.INVESTIGATE, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.5);
        assertTrue(decision.getReasoning().contains("unknown error pattern"));
        assertTrue(decision.getReasoning().contains("diagnostic information"));
    }
    
    @Test
    @DisplayName("AI decides to switch path when primary flow is consistently failing")
    void testDecisionMaking_SwitchPathOnConsistentFailure() throws Exception {
        // Given - Consistent failure in primary path
        TestContext context = createConsistentFailureContext();
        List<TestPattern> alternativePatterns = createAlternativePathPatterns();
        
        when(testMemoryService.findSimilarPatterns(context, 3))
            .thenReturn(alternativePatterns);
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(createConsistentFailureHistory());
        when(llmService.sendPrompt(contains("Make a test execution decision")))
            .thenReturn(new LLMService.LLMResponse(
                "SWITCH_PATH - Primary payment gateway failing consistently (5/5 attempts). " +
                "Alternative path via backup gateway shows 85% success rate. Switch to backup path.", 
                1300, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.SWITCH_PATH, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.7);
        assertTrue(decision.getReasoning().contains("Primary payment gateway"));
        assertTrue(decision.getReasoning().contains("backup gateway"));
    }
    
    @Test
    @DisplayName("AI learns from successful execution patterns")
    void testLearning_SuccessfulPatternAnalysis() throws Exception {
        // Given - Diverse successful patterns
        List<TestPattern> successPatterns = createDiverseSuccessPatterns();
        
        when(llmService.sendPrompt(contains("Analyze these")))
            .thenReturn(new LLMService.LLMResponse(
                "Analysis of 5 successful patterns reveals: " +
                "1. Parallel execution of independent services reduces total time by 35% " +
                "2. Payment validation before inventory check prevents 60% of rollbacks " +
                "3. Circuit breaker pattern prevents cascade failures in 90% of cases " +
                "4. Optimal timeout values: REST calls 15s, Kafka events 10s, DB queries 5s " +
                "5. Morning executions (8-10 AM) show 15% better performance than afternoon", 
                4000, true));
        
        // When
        CompletableFuture<Void> result = agenticTestController.learnFromTestPatterns(successPatterns);
        result.get(10, TimeUnit.SECONDS);
        
        // Then
        verify(llmService).sendPrompt(contains("Analyze these 5 test patterns"));
        // In a real implementation, we would verify that insights were stored
    }
    
    @Test
    @DisplayName("AI provides intelligent failure analysis with remediation suggestions")
    void testFailureAnalysis_IntelligentRemediation() throws Exception {
        // Given - Complex failure scenario
        TestFailure complexFailure = createComplexFailure();
        TestContext failureContext = createComplexFailureContext();
        
        FailureAnalysis intelligentAnalysis = createIntelligentAnalysis();
        when(failureAnalyzer.analyzeFailure(complexFailure, failureContext))
            .thenReturn(CompletableFuture.completedFuture(intelligentAnalysis));
        
        // When
        CompletableFuture<FailureAnalysis> result = 
            agenticTestController.analyzeTestFailure(complexFailure, failureContext);
        FailureAnalysis analysis = result.get(10, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(analysis);
        assertEquals("BUSINESS_LOGIC_FAILURE", analysis.getRootCauseCategory());
        assertTrue(analysis.getConfidenceScore() > 0.8);
        assertFalse(analysis.getRemediationSuggestions().isEmpty());
        
        // Verify intelligent suggestions
        List<RemediationSuggestion> suggestions = analysis.getRemediationSuggestions();
        assertTrue(suggestions.stream().anyMatch(s -> s.getType() == RemediationType.DATA_REPAIR));
        assertTrue(suggestions.stream().anyMatch(s -> s.getType() == RemediationType.CONFIGURATION_CHANGE));
        assertTrue(suggestions.stream().anyMatch(s -> s.getType() == RemediationType.CODE_FIX));
        
        verify(failureAnalyzer).analyzeFailure(complexFailure, failureContext);
    }
    
    // Helper methods for creating test contexts and patterns
    
    private TestContext createHealthySystemContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("healthy-system-test");
        context.getExecutionState().put("systemLoad", 0.3);
        context.getExecutionState().put("servicesHealthy", true);
        context.getExecutionState().put("avgResponseTime", 150);
        return context;
    }
    
    private TestContext createTransientFailureContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("transient-failure-test");
        context.getExecutionState().put("lastError", "Connection timeout");
        context.getExecutionState().put("errorType", "TRANSIENT");
        context.getExecutionState().put("retryCount", 0);
        return context;
    }
    
    private TestContext createHighLoadSystemContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("high-load-test");
        context.getExecutionState().put("systemLoad", 0.92);
        context.getExecutionState().put("avgResponseTime", 4500);
        context.getExecutionState().put("activeConnections", 2000);
        return context;
    }
    
    private TestContext createCriticalFailureContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("critical-failure-test");
        context.getExecutionState().put("paymentServiceStatus", "DOWN");
        context.getExecutionState().put("outageStartTime", Instant.now().minusSeconds(900)); // 15 minutes ago
        context.getExecutionState().put("criticalService", true);
        return context;
    }
    
    private TestContext createUnknownFailureContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("unknown-failure-test");
        context.getExecutionState().put("errorCode", "COUCHBASE_VECTOR_SEARCH_TIMEOUT");
        context.getExecutionState().put("errorCategory", "UNKNOWN");
        context.getExecutionState().put("firstOccurrence", true);
        return context;
    }
    
    private TestContext createConsistentFailureContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("consistent-failure-test");
        context.getExecutionState().put("primaryGatewayFailures", 5);
        context.getExecutionState().put("consecutiveFailures", 5);
        context.getExecutionState().put("alternativeAvailable", true);
        return context;
    }
    
    private TestContext createComplexFailureContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("complex-failure-test");
        context.getExecutionState().put("transactionId", "tx-12345");
        context.getExecutionState().put("involvedServices", Arrays.asList("payment", "inventory", "order"));
        context.getExecutionState().put("partialSuccess", true);
        return context;
    }
    
    private List<TestPattern> createSuccessfulPatterns() {
        TestPattern pattern1 = new TestPattern();
        pattern1.setPatternId("success-1");
        pattern1.setName("Optimal Order Flow");
        pattern1.setType(PatternType.SUCCESS_FLOW);
        pattern1.setSuccessRate(0.95);
        
        TestPattern pattern2 = new TestPattern();
        pattern2.setPatternId("success-2");
        pattern2.setName("Fast Payment Processing");
        pattern2.setType(PatternType.PERFORMANCE_BASELINE);
        pattern2.setSuccessRate(0.92);
        
        return Arrays.asList(pattern1, pattern2);
    }
    
    private List<TestPattern> createRetrySuccessPatterns() {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId("retry-success-1");
        pattern.setName("Successful Retry Pattern");
        pattern.setType(PatternType.RECOVERY_STRATEGY);
        pattern.setSuccessRate(0.80);
        return Arrays.asList(pattern);
    }
    
    private List<TestPattern> createHighLoadPatterns() {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId("high-load-1");
        pattern.setName("High Load Adaptation");
        pattern.setType(PatternType.PERFORMANCE_BASELINE);
        pattern.setSuccessRate(0.70);
        return Arrays.asList(pattern);
    }
    
    private List<TestPattern> createCriticalFailurePatterns() {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId("critical-fail-1");
        pattern.setName("Payment Service Outage");
        pattern.setType(PatternType.FAILURE_PATTERN);
        pattern.setSuccessRate(0.0);
        return Arrays.asList(pattern);
    }
    
    private List<TestPattern> createAlternativePathPatterns() {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId("alt-path-1");
        pattern.setName("Backup Gateway Success");
        pattern.setType(PatternType.SUCCESS_FLOW);
        pattern.setSuccessRate(0.85);
        return Arrays.asList(pattern);
    }
    
    private List<TestPattern> createDiverseSuccessPatterns() {
        return Arrays.asList(
            createPatternWithType(PatternType.SUCCESS_FLOW, 0.95),
            createPatternWithType(PatternType.PERFORMANCE_BASELINE, 0.88),
            createPatternWithType(PatternType.RECOVERY_STRATEGY, 0.82),
            createPatternWithType(PatternType.SUCCESS_FLOW, 0.91),
            createPatternWithType(PatternType.PERFORMANCE_BASELINE, 0.87)
        );
    }
    
    private TestPattern createPatternWithType(PatternType type, double successRate) {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId("pattern-" + type.name().toLowerCase());
        pattern.setName(type.name() + " Pattern");
        pattern.setType(type);
        pattern.setSuccessRate(successRate);
        pattern.setCreatedAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
        return pattern;
    }
    
    private List<TestExecutionHistory> createPositiveExecutionHistory() {
        return Arrays.asList(
            createHistoryWithStatus(TestStatus.PASSED, 2000L),
            createHistoryWithStatus(TestStatus.PASSED, 1800L),
            createHistoryWithStatus(TestStatus.PASSED, 2200L)
        );
    }
    
    private List<TestExecutionHistory> createMixedExecutionHistory() {
        return Arrays.asList(
            createHistoryWithStatus(TestStatus.PASSED, 2000L),
            createHistoryWithStatus(TestStatus.FAILED, 5000L),
            createHistoryWithStatus(TestStatus.PASSED, 1900L)
        );
    }
    
    private List<TestExecutionHistory> createHighLoadHistory() {
        return Arrays.asList(
            createHistoryWithStatus(TestStatus.PASSED, 8000L),
            createHistoryWithStatus(TestStatus.TIMEOUT, 30000L),
            createHistoryWithStatus(TestStatus.PASSED, 7500L)
        );
    }
    
    private List<TestExecutionHistory> createFailureHistory() {
        return Arrays.asList(
            createHistoryWithStatus(TestStatus.FAILED, 1000L),
            createHistoryWithStatus(TestStatus.FAILED, 1200L),
            createHistoryWithStatus(TestStatus.FAILED, 900L)
        );
    }
    
    private List<TestExecutionHistory> createNormalHistory() {
        return Arrays.asList(
            createHistoryWithStatus(TestStatus.PASSED, 2000L),
            createHistoryWithStatus(TestStatus.PASSED, 2100L)
        );
    }
    
    private List<TestExecutionHistory> createConsistentFailureHistory() {
        return Arrays.asList(
            createHistoryWithStatus(TestStatus.FAILED, 3000L),
            createHistoryWithStatus(TestStatus.FAILED, 3200L),
            createHistoryWithStatus(TestStatus.FAILED, 2800L),
            createHistoryWithStatus(TestStatus.FAILED, 3100L),
            createHistoryWithStatus(TestStatus.FAILED, 2900L)
        );
    }
    
    private TestExecutionHistory createHistoryWithStatus(TestStatus status, long executionTime) {
        TestExecutionHistory history = new TestExecutionHistory();
        history.setExecutionId("exec-" + System.nanoTime());
        history.setStatus(status);
        history.setExecutionTimeMs(executionTime);
        history.setStartTime(Instant.now().minus((long)(Math.random() * 60), java.time.temporal.ChronoUnit.MINUTES));
        return history;
    }
    
    private TestFailure createComplexFailure() {
        TestFailure failure = new TestFailure();
        failure.setFailureId("complex-failure-1");
        failure.setFailureType(FailureType.BUSINESS_LOGIC_FAILURE);
        failure.setErrorMessage("Distributed transaction failed: payment succeeded but inventory reservation failed");
        failure.setTimestamp(Instant.now());
        failure.setServiceId("transaction-coordinator");
        return failure;
    }
    
    private FailureAnalysis createIntelligentAnalysis() {
        FailureAnalysis analysis = new FailureAnalysis();
        analysis.setRootCauseCategory("BUSINESS_LOGIC_FAILURE");
        analysis.setRootCause("Inventory service timeout during reservation phase of distributed transaction");
        analysis.setConfidenceScore(0.88);
        
        RemediationSuggestion rollback = new RemediationSuggestion();
        rollback.setType(RemediationType.DATA_REPAIR);
        rollback.setDescription("Rollback payment transaction to maintain data consistency");
        rollback.setPriority(1);
        
        RemediationSuggestion retry = new RemediationSuggestion();
        retry.setType(RemediationType.CONFIGURATION_CHANGE);
        retry.setDescription("Retry with increased timeout for inventory service");
        retry.setPriority(2);
        
        RemediationSuggestion circuitBreaker = new RemediationSuggestion();
        circuitBreaker.setType(RemediationType.CODE_FIX);
        circuitBreaker.setDescription("Implement circuit breaker for inventory service");
        circuitBreaker.setPriority(3);
        
        analysis.setRemediationSuggestions(Arrays.asList(rollback, retry, circuitBreaker));
        return analysis;
    }
}