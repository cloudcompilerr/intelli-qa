package com.agentic.e2etester.controller;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.ai.TestScenarioParser;
import com.agentic.e2etester.analysis.FailureAnalyzer;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.orchestration.TestOrchestrator;
import com.agentic.e2etester.service.TestMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AgenticTestController focusing on AI decision-making scenarios
 * in realistic test execution contexts.
 */
@SpringBootTest
@ActiveProfiles("test")
class AgenticTestControllerIntegrationTest {
    
    @MockBean
    private LLMService llmService;
    
    @MockBean
    private TestScenarioParser scenarioParser;
    
    @MockBean
    private TestOrchestrator testOrchestrator;
    
    @MockBean
    private TestMemoryService testMemoryService;
    
    @MockBean
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
    void testCompleteOrderFulfillmentScenario_AIDecisionMaking() throws Exception {
        // Given - Complex order fulfillment scenario
        String scenario = """
            Test complete order fulfillment journey:
            1. Customer adds items to cart
            2. Proceeds to checkout
            3. Payment processing
            4. Inventory reservation
            5. Order confirmation
            6. Shipping notification
            7. Delivery tracking
            Validate all microservice interactions and data consistency.
            """;
        
        // Mock AI enhancement
        when(llmService.sendPrompt(contains("enhance")))
            .thenReturn(new LLMService.LLMResponse(
                "Enhanced scenario with detailed microservice interactions and validation points", 
                1500, true));
        
        // Mock scenario parsing
        TestExecutionPlan complexPlan = createComplexOrderFulfillmentPlan();
        when(scenarioParser.parseScenario(anyString()))
            .thenReturn(complexPlan);
        
        // Mock similar patterns for optimization
        when(testMemoryService.findSimilarPatterns(any(TestContext.class), eq(5)))
            .thenReturn(createSimilarOrderPatterns());
        
        // Mock AI optimization suggestions
        when(llmService.sendPrompt(contains("Optimize this test execution plan")))
            .thenReturn(new LLMService.LLMResponse(
                "Suggest parallel execution for independent services and retry logic for payment", 
                2000, true));
        
        // Mock orchestration
        TestResult successResult = createSuccessfulOrderResult();
        when(testOrchestrator.orchestrateTest(any(TestExecutionPlan.class)))
            .thenReturn(CompletableFuture.completedFuture(successResult));
        
        // Mock result analysis
        when(llmService.sendPrompt(contains("Analyze test execution result")))
            .thenReturn(new LLMService.LLMResponse(
                "Test completed successfully with optimal performance across all services", 
                1000, true));
        
        // When
        CompletableFuture<TestResult> result = agenticTestController.executeTestScenario(scenario);
        TestResult testResult = result.get(30, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(testResult);
        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify AI decision-making flow
        verify(llmService, times(3)).sendPrompt(anyString()); // Enhancement, optimization, analysis
        verify(scenarioParser).parseScenario(anyString());
        verify(testMemoryService).findSimilarPatterns(any(TestContext.class), eq(5));
        verify(testOrchestrator).orchestrateTest(any(TestExecutionPlan.class));
    }
    
    @Test
    void testFailureScenario_AIAnalysisAndRecovery() throws Exception {
        // Given - Scenario that will encounter failures
        String scenario = "Test payment processing with service failures";
        
        TestExecutionPlan plan = createPaymentTestPlan();
        when(scenarioParser.parseScenario(anyString()))
            .thenReturn(plan);
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Enhanced scenario", 1000, true));
        
        // Mock failure during execution
        TestResult failureResult = createFailedPaymentResult();
        when(testOrchestrator.orchestrateTest(any(TestExecutionPlan.class)))
            .thenReturn(CompletableFuture.completedFuture(failureResult));
        
        // When
        CompletableFuture<TestResult> result = agenticTestController.executeTestScenario(scenario);
        TestResult testResult = result.get(30, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(testResult);
        assertEquals(TestStatus.FAILED, testResult.getStatus());
        
        // Test AI failure analysis
        TestFailure failure = createPaymentServiceFailure();
        TestContext context = createPaymentTestContext();
        
        FailureAnalysis mockAnalysis = createIntelligentFailureAnalysis();
        when(failureAnalyzer.analyzeFailure(failure, context))
            .thenReturn(CompletableFuture.completedFuture(mockAnalysis));
        
        CompletableFuture<FailureAnalysis> analysisResult = 
            agenticTestController.analyzeTestFailure(failure, context);
        FailureAnalysis analysis = analysisResult.get(10, TimeUnit.SECONDS);
        
        assertNotNull(analysis);
        assertEquals("SERVICE_FAILURE", analysis.getRootCauseCategory());
        assertFalse(analysis.getRemediationSuggestions().isEmpty());
        
        verify(failureAnalyzer).analyzeFailure(failure, context);
    }
    
    @Test
    void testAdaptiveDecisionMaking_HighLoadScenario() throws Exception {
        // Given - High system load scenario
        TestContext highLoadContext = createHighLoadContext();
        
        // Mock memory service responses
        when(testMemoryService.findSimilarPatterns(highLoadContext, 3))
            .thenReturn(createHighLoadPatterns());
        when(testMemoryService.findRecentExecutionHistory(10))
            .thenReturn(createRecentFailureHistory());
        
        // Mock AI decision for high load
        when(llmService.sendPrompt(contains("Make a test execution decision")))
            .thenReturn(new LLMService.LLMResponse(
                "ADAPT execution strategy due to high system load. Reduce parallel execution and increase timeouts.", 
                1200, true));
        
        // When
        CompletableFuture<TestDecision> result = agenticTestController.makeExecutionDecision(highLoadContext);
        TestDecision decision = result.get(10, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(decision);
        assertEquals(DecisionType.ADAPT, decision.getDecisionType());
        assertTrue(decision.getConfidence() > 0.5);
        assertTrue(decision.getReasoning().contains("high system load"));
        
        verify(testMemoryService).findSimilarPatterns(highLoadContext, 3);
        verify(testMemoryService).findRecentExecutionHistory(10);
        verify(llmService).sendPrompt(anyString());
    }
    
    @Test
    void testLearningFromPatterns_ContinuousImprovement() throws Exception {
        // Given - Multiple test patterns for learning
        List<TestPattern> learningPatterns = createDiverseTestPatterns();
        
        // Mock AI pattern analysis
        when(llmService.sendPrompt(contains("Analyze these")))
            .thenReturn(new LLMService.LLMResponse(
                "Key insights: 1) Parallel execution improves performance by 40% " +
                "2) Payment service has 15% failure rate during peak hours " +
                "3) Retry with exponential backoff reduces overall failure rate", 
                3000, true));
        
        // When
        CompletableFuture<Void> result = agenticTestController.learnFromTestPatterns(learningPatterns);
        result.get(15, TimeUnit.SECONDS);
        
        // Then
        verify(llmService).sendPrompt(contains("Analyze these " + learningPatterns.size() + " test patterns"));
    }
    
    @Test
    void testSessionManagement_MultipleActiveSessions() throws Exception {
        // Given - Multiple concurrent test scenarios
        String scenario1 = "Test order creation flow";
        String scenario2 = "Test payment processing flow";
        
        TestExecutionPlan plan1 = createOrderCreationPlan();
        TestExecutionPlan plan2 = createPaymentProcessingPlan();
        
        when(scenarioParser.parseScenario(contains("order creation")))
            .thenReturn(plan1);
        when(scenarioParser.parseScenario(contains("payment processing")))
            .thenReturn(plan2);
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Enhanced scenario", 1000, true));
        
        when(testOrchestrator.orchestrateTest(any(TestExecutionPlan.class)))
            .thenReturn(CompletableFuture.completedFuture(createSuccessfulOrderResult()));
        
        // When - Start multiple sessions
        CompletableFuture<TestResult> result1 = agenticTestController.executeTestScenario(scenario1);
        CompletableFuture<TestResult> result2 = agenticTestController.executeTestScenario(scenario2);
        
        // Check active sessions during execution
        List<TestSession> activeSessions = agenticTestController.getActiveSessions();
        
        // Wait for completion
        TestResult testResult1 = result1.get(30, TimeUnit.SECONDS);
        TestResult testResult2 = result2.get(30, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(testResult1);
        assertNotNull(testResult2);
        assertEquals(TestStatus.PASSED, testResult1.getStatus());
        assertEquals(TestStatus.PASSED, testResult2.getStatus());
        
        // Sessions should be cleaned up after completion
        List<TestSession> finalSessions = agenticTestController.getActiveSessions();
        assertTrue(finalSessions.isEmpty());
    }
    
    // Helper methods for creating complex test scenarios
    
    private TestExecutionPlan createComplexOrderFulfillmentPlan() {
        TestConfiguration config = new TestConfiguration();
        config.setMaxRetries(3);
        config.setDefaultTimeoutMs(30000L);
        config.setParallelExecution(true);
        
        List<TestStep> steps = Arrays.asList(
            new TestStep("cart-add", StepType.REST_CALL, "cart-service", 15000L),
            new TestStep("checkout-init", StepType.REST_CALL, "checkout-service", 20000L),
            new TestStep("payment-process", StepType.REST_CALL, "payment-service", 30000L),
            new TestStep("inventory-reserve", StepType.KAFKA_EVENT, "inventory-service", 25000L),
            new TestStep("order-confirm", StepType.REST_CALL, "order-service", 15000L),
            new TestStep("shipping-notify", StepType.KAFKA_EVENT, "shipping-service", 20000L),
            new TestStep("tracking-setup", StepType.DATABASE_CHECK, "tracking-service", 10000L)
        );
        
        return new TestExecutionPlan("complex-order-plan", "Complete Order Fulfillment", steps, config);
    }
    
    private List<TestPattern> createSimilarOrderPatterns() {
        TestPattern pattern1 = new TestPattern();
        pattern1.setPatternId("order-pattern-1");
        pattern1.setName("Successful Order Flow");
        pattern1.setType(PatternType.SUCCESS_FLOW);
        pattern1.setCreatedAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
        
        TestPattern pattern2 = new TestPattern();
        pattern2.setPatternId("order-pattern-2");
        pattern2.setName("Order with Payment Retry");
        pattern2.setType(PatternType.RECOVERY_STRATEGY);
        pattern2.setCreatedAt(Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS));
        
        return Arrays.asList(pattern1, pattern2);
    }
    
    private TestResult createSuccessfulOrderResult() {
        TestResult result = new TestResult();
        result.setTestId("order-test-1");
        result.setStatus(TestStatus.PASSED);
        result.setStartTime(Instant.now().minus(5, java.time.temporal.ChronoUnit.MINUTES));
        result.setEndTime(Instant.now());
        result.setExecutionTimeMs(300000L); // 5 minutes
        return result;
    }
    
    private TestExecutionPlan createPaymentTestPlan() {
        TestConfiguration config = new TestConfiguration();
        config.setMaxRetries(2);
        config.setDefaultTimeoutMs(20000L);
        
        List<TestStep> steps = Arrays.asList(
            new TestStep("payment-validate", StepType.REST_CALL, "payment-service", 15000L),
            new TestStep("payment-process", StepType.REST_CALL, "payment-service", 25000L),
            new TestStep("payment-confirm", StepType.KAFKA_EVENT, "payment-service", 10000L)
        );
        
        return new TestExecutionPlan("payment-test-plan", "Payment Processing Test", steps, config);
    }
    
    private TestResult createFailedPaymentResult() {
        TestResult result = new TestResult();
        result.setTestId("payment-test-1");
        result.setStatus(TestStatus.FAILED);
        result.setStartTime(Instant.now().minus(2, java.time.temporal.ChronoUnit.MINUTES));
        result.setEndTime(Instant.now());
        result.setErrorMessage("Payment service unavailable");
        return result;
    }
    
    private TestFailure createPaymentServiceFailure() {
        TestFailure failure = new TestFailure();
        failure.setFailureId("payment-failure-1");
        failure.setFailureType(FailureType.SERVICE_FAILURE);
        failure.setErrorMessage("Payment service returned 503 Service Unavailable");
        failure.setTimestamp(Instant.now());
        failure.setServiceId("payment-service");
        return failure;
    }
    
    private TestContext createPaymentTestContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("payment-correlation-1");
        context.getExecutionState().put("testId", "payment-test-1");
        context.getExecutionState().put("currentStep", "payment-process");
        return context;
    }
    
    private FailureAnalysis createIntelligentFailureAnalysis() {
        FailureAnalysis analysis = new FailureAnalysis();
        analysis.setRootCauseCategory("SERVICE_FAILURE");
        analysis.setRootCause("Payment service is experiencing high load and circuit breaker is open");
        analysis.setConfidenceScore(0.85);
        
        RemediationSuggestion suggestion1 = new RemediationSuggestion();
        suggestion1.setType(RemediationType.SERVICE_RESTART);
        suggestion1.setDescription("Retry with exponential backoff after 30 seconds");
        suggestion1.setPriority(1);
        
        RemediationSuggestion suggestion2 = new RemediationSuggestion();
        suggestion2.setType(RemediationType.CONFIGURATION_CHANGE);
        suggestion2.setDescription("Wait for circuit breaker to reset");
        suggestion2.setPriority(2);
        
        analysis.setRemediationSuggestions(Arrays.asList(suggestion1, suggestion2));
        return analysis;
    }
    
    private TestContext createHighLoadContext() {
        TestContext context = new TestContext();
        context.setCorrelationId("high-load-test-1");
        context.getExecutionState().put("systemLoad", 0.95);
        context.getExecutionState().put("activeConnections", 1500);
        context.getExecutionState().put("responseTimeMs", 5000);
        return context;
    }
    
    private List<TestPattern> createHighLoadPatterns() {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId("high-load-pattern-1");
        pattern.setName("High Load Adaptation");
        pattern.setType(PatternType.PERFORMANCE_BASELINE);
        pattern.setCreatedAt(Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS));
        return Arrays.asList(pattern);
    }
    
    private List<TestExecutionHistory> createRecentFailureHistory() {
        TestExecutionHistory history1 = new TestExecutionHistory();
        history1.setExecutionId("exec-fail-1");
        history1.setStatus(TestStatus.FAILED);
        history1.setExecutionTimeMs(45000L);
        
        TestExecutionHistory history2 = new TestExecutionHistory();
        history2.setExecutionId("exec-fail-2");
        history2.setStatus(TestStatus.FAILED);
        history2.setExecutionTimeMs(60000L);
        
        return Arrays.asList(history1, history2);
    }
    
    private List<TestPattern> createDiverseTestPatterns() {
        TestPattern successPattern = new TestPattern();
        successPattern.setPatternId("success-pattern-1");
        successPattern.setName("Optimal Success Flow");
        successPattern.setType(PatternType.SUCCESS_FLOW);
        
        TestPattern failurePattern = new TestPattern();
        failurePattern.setPatternId("failure-pattern-1");
        failurePattern.setName("Common Payment Failure");
        failurePattern.setType(PatternType.FAILURE_PATTERN);
        
        TestPattern performancePattern = new TestPattern();
        performancePattern.setPatternId("perf-pattern-1");
        performancePattern.setName("High Performance Execution");
        performancePattern.setType(PatternType.PERFORMANCE_BASELINE);
        
        return Arrays.asList(successPattern, failurePattern, performancePattern);
    }
    
    private TestExecutionPlan createOrderCreationPlan() {
        TestConfiguration config = new TestConfiguration();
        config.setMaxRetries(2);
        config.setDefaultTimeoutMs(20000L);
        
        List<TestStep> steps = Arrays.asList(
            new TestStep("create-order", StepType.REST_CALL, "order-service", 15000L),
            new TestStep("validate-order", StepType.DATABASE_CHECK, "order-service", 10000L)
        );
        
        return new TestExecutionPlan("order-creation-plan", "Order Creation Test", steps, config);
    }
    
    private TestExecutionPlan createPaymentProcessingPlan() {
        TestConfiguration config = new TestConfiguration();
        config.setMaxRetries(3);
        config.setDefaultTimeoutMs(25000L);
        
        List<TestStep> steps = Arrays.asList(
            new TestStep("process-payment", StepType.REST_CALL, "payment-service", 20000L),
            new TestStep("confirm-payment", StepType.KAFKA_EVENT, "payment-service", 15000L)
        );
        
        return new TestExecutionPlan("payment-processing-plan", "Payment Processing Test", steps, config);
    }
}