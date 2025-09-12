package com.agentic.e2etester.controller;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.ai.TestScenarioParser;
import com.agentic.e2etester.analysis.FailureAnalyzer;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.orchestration.TestOrchestrator;
import com.agentic.e2etester.service.TestMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgenticTestController focusing on AI decision-making capabilities.
 */
@ExtendWith(MockitoExtension.class)
class AgenticTestControllerTest {
    
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
    void testParseTestScenario_Success() throws Exception {
        // Given
        String scenario = "Test order fulfillment flow from cart to delivery";
        String enhancedScenario = "Enhanced: " + scenario;
        TestExecutionPlan mockPlan = createMockTestExecutionPlan();
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse(enhancedScenario, 1000, true));
        when(scenarioParser.parseScenario(enhancedScenario))
            .thenReturn(mockPlan);
        when(testMemoryService.findSimilarPatterns(any(TestContext.class), eq(5)))
            .thenReturn(Arrays.asList(createMockTestPattern()));
        
        // When
        CompletableFuture<TestExecutionPlan> result = agenticTestController.parseTestScenario(scenario);
        TestExecutionPlan plan = result.get();
        
        // Then
        assertNotNull(plan);
        assertEquals("test-plan-1", plan.getTestId());
        verify(llmService).sendPrompt(contains("enhance"));
        verify(scenarioParser).parseScenario(enhancedScenario);
        verify(testMemoryService).findSimilarPatterns(any(TestContext.class), eq(5));
    }
    
    @Test
    void testParseTestScenario_LLMEnhancementFails() throws Exception {
        // Given
        String scenario = "Test order fulfillment flow";
        TestExecutionPlan mockPlan = createMockTestExecutionPlan();
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("", 1000, false, "LLM error"));
        when(scenarioParser.parseScenario(scenario))
            .thenReturn(mockPlan);
        when(testMemoryService.findSimilarPatterns(any(TestContext.class), eq(5)))
            .thenReturn(Arrays.asList());
        
        // When
        CompletableFuture<TestExecutionPlan> result = agenticTestController.parseTestScenario(scenario);
        TestExecutionPlan plan = result.get();
        
        // Then
        assertNotNull(plan);
        verify(scenarioParser).parseScenario(scenario); // Should use original scenario
    }
    
    @Test
    void testMakeExecutionDecision_ContinueDecision() throws Exception {
        // Given
        TestContext context = createMockTestContext();
        List<TestPattern> patterns = Arrays.asList(createMockTestPattern());
        List<TestExecutionHistory> history = Arrays.asList(createMockExecutionHistory());
        
        when(testMemoryService.findSimilarPatterns(context, 3))
            .thenReturn(patterns);
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(history);
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("CONTINUE with high confidence", 800, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get();
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.CONTINUE, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.0);
        assertNotNull(decision.getReasoning());
        verify(llmService).sendPrompt(contains("Make a test execution decision"));
    }
    
    @Test
    void testMakeExecutionDecision_FailsafeDecision() throws Exception {
        // Given
        TestContext context = createMockTestContext();
        
        when(testMemoryService.findSimilarPatterns(any(TestContext.class), anyInt()))
            .thenThrow(new RuntimeException("Memory service error"));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(context);
        TestDecision decision = result.get();
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.ABORT, decision.getDecisionType());
        assertEquals(1.0, decision.getConfidence());
        assertTrue(decision.getReasoning().contains("Decision making failed"));
    }
    
    @Test
    void testExecuteTestScenario_Success() throws Exception {
        // Given
        String scenario = "Complete order fulfillment test";
        TestExecutionPlan mockPlan = createMockTestExecutionPlan();
        TestResult mockResult = createMockTestResult(TestStatus.PASSED);
        
        when(scenarioParser.parseScenario(anyString()))
            .thenReturn(mockPlan);
        when(testOrchestrator.orchestrateTest(mockPlan))
            .thenReturn(CompletableFuture.completedFuture(mockResult));
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Enhanced scenario", 1000, true))
            .thenReturn(new LLMService.LLMResponse("Test completed successfully", 500, true));
        
        // When
        CompletableFuture<TestResult> result = agenticTestController.executeTestScenario(scenario);
        TestResult testResult = result.get();
        
        // Then
        assertNotNull(testResult);
        assertEquals(TestStatus.PASSED, testResult.getStatus());
        verify(testOrchestrator).orchestrateTest(mockPlan);
    }
    
    @Test
    void testExecuteTestScenario_Failure() throws Exception {
        // Given
        String scenario = "Test scenario that will fail";
        
        when(scenarioParser.parseScenario(anyString()))
            .thenThrow(new RuntimeException("Parsing failed"));
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Enhanced scenario", 1000, true));
        
        // When & Then
        CompletableFuture<TestResult> result = agenticTestController.executeTestScenario(scenario);
        
        assertThrows(Exception.class, () -> result.get());
    }
    
    @Test
    void testLearnFromTestPatterns_Success() throws Exception {
        // Given
        List<TestPattern> patterns = Arrays.asList(
            createMockTestPattern(),
            createMockTestPattern()
        );
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Analysis: Found 2 key insights", 2000, true));
        
        // When
        CompletableFuture<Void> result = agenticTestController.learnFromTestPatterns(patterns);
        result.get(); // Wait for completion
        
        // Then
        verify(llmService).sendPrompt(contains("Analyze these 2 test patterns"));
    }
    
    @Test
    void testLearnFromTestPatterns_LLMFailure() throws Exception {
        // Given
        List<TestPattern> patterns = Arrays.asList(createMockTestPattern());
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("", 0, false, "LLM unavailable"));
        
        // When
        CompletableFuture<Void> result = agenticTestController.learnFromTestPatterns(patterns);
        result.get(); // Should complete without throwing
        
        // Then
        verify(llmService).sendPrompt(anyString());
    }
    
    @Test
    void testAnalyzeTestFailure_Success() throws Exception {
        // Given
        TestFailure failure = createMockTestFailure();
        TestContext context = createMockTestContext();
        FailureAnalysis mockAnalysis = createMockFailureAnalysis();
        
        when(failureAnalyzer.analyzeFailure(failure, context))
            .thenReturn(CompletableFuture.completedFuture(mockAnalysis));
        
        // When
        CompletableFuture<FailureAnalysis> result = agenticTestController.analyzeTestFailure(failure, context);
        FailureAnalysis analysis = result.get();
        
        // Then
        assertNotNull(analysis);
        assertEquals("SERVICE_FAILURE", analysis.getRootCauseCategory());
        verify(failureAnalyzer).analyzeFailure(failure, context);
    }
    
    @Test
    void testGetTestSession_ExistingSession() {
        // Given
        String sessionId = "test-session-1";
        // Execute a test to create a session
        when(scenarioParser.parseScenario(anyString()))
            .thenReturn(createMockTestExecutionPlan());
        when(testOrchestrator.orchestrateTest(any()))
            .thenReturn(CompletableFuture.completedFuture(createMockTestResult(TestStatus.PASSED)));
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Enhanced", 1000, true));
        
        agenticTestController.executeTestScenario("test scenario");
        
        // When
        List<TestSession> activeSessions = agenticTestController.getActiveSessions();
        
        // Then
        assertFalse(activeSessions.isEmpty());
    }
    
    @Test
    void testGetTestSession_NonExistentSession() {
        // When
        Optional<TestSession> session = agenticTestController.getTestSession("non-existent");
        
        // Then
        assertFalse(session.isPresent());
    }
    
    @Test
    void testGetActiveSessions_EmptyInitially() {
        // When
        List<TestSession> sessions = agenticTestController.getActiveSessions();
        
        // Then
        assertTrue(sessions.isEmpty());
    }
    
    // Helper methods for creating mock objects
    
    private TestExecutionPlan createMockTestExecutionPlan() {
        TestStep step1 = new TestStep("step-1", StepType.REST_CALL, "order-service", 30000L);
        TestStep step2 = new TestStep("step-2", StepType.KAFKA_EVENT, "payment-service", 30000L);
        
        TestConfiguration config = new TestConfiguration();
        config.setMaxRetries(3);
        config.setDefaultTimeoutMs(30000L);
        
        return new TestExecutionPlan("test-plan-1", "Order fulfillment test", 
                                   Arrays.asList(step1, step2), config);
    }
    
    private TestContext createMockTestContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("test-correlation-1");
        context.getExecutionState().put("testId", "test-1");
        return context;
    }
    
    private TestPattern createMockTestPattern() {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId("pattern-1");
        pattern.setName("Successful Order Flow");
        pattern.setType(PatternType.SUCCESS_FLOW);
        pattern.setCreatedAt(Instant.now());
        return pattern;
    }
    
    private TestExecutionHistory createMockExecutionHistory() {
        TestExecutionHistory history = new TestExecutionHistory();
        history.setExecutionId("exec-1");
        history.setTestPlanId("plan-1");
        history.setStatus(TestStatus.PASSED);
        history.setExecutionTimeMs(5000L);
        return history;
    }
    
    private TestResult createMockTestResult(TestStatus status) {
        TestResult result = new TestResult();
        result.setTestId("test-1");
        result.setStatus(status);
        result.setStartTime(Instant.now().minusSeconds(10));
        result.setEndTime(Instant.now());
        return result;
    }
    
    private TestFailure createMockTestFailure() {
        TestFailure failure = new TestFailure();
        failure.setFailureId("failure-1");
        failure.setFailureType(FailureType.SERVICE_FAILURE);
        failure.setErrorMessage("Service is not responding");
        failure.setTimestamp(Instant.now());
        return failure;
    }
    
    private FailureAnalysis createMockFailureAnalysis() {
        FailureAnalysis analysis = new FailureAnalysis();
        analysis.setRootCauseCategory("SERVICE_FAILURE");
        analysis.setConfidenceScore(0.85);
        analysis.setRootCause("Network connectivity issue");
        
        RemediationSuggestion suggestion = new RemediationSuggestion();
        suggestion.setType(RemediationType.SERVICE_RESTART);
        suggestion.setDescription("Retry with exponential backoff");
        analysis.setRemediationSuggestions(Arrays.asList(suggestion));
        
        return analysis;
    }
}