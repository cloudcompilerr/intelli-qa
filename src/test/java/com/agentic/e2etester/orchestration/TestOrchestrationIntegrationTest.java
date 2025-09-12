package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.service.CorrelationTrackingService;
import com.agentic.e2etester.service.TestMemoryService;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for complete test orchestration workflows
 */
@SpringBootTest
@ActiveProfiles("test")
class TestOrchestrationIntegrationTest {
    
    @Autowired
    private TestOrchestrator testOrchestrator;
    
    @MockBean
    private TestExecutionEngine executionEngine;
    
    @MockBean
    private LLMService llmService;
    
    @MockBean
    private CorrelationTrackingService correlationService;
    
    @MockBean
    private TestMemoryService testMemoryService;
    
    private TestExecutionPlan testPlan;
    
    @BeforeEach
    void setUp() {
        testPlan = createSampleTestPlan();
        
        // Mock correlation service
        when(correlationService.generateCorrelationId()).thenReturn("corr-123");
        when(correlationService.startTrace(anyString(), anyString())).thenReturn(new CorrelationTrace());
        when(correlationService.completeTrace(anyString())).thenReturn(Optional.empty());
        
        // Mock test memory service
        doNothing().when(testMemoryService).storeTestPattern(any(TestPattern.class));
        doNothing().when(testMemoryService).storeExecutionHistory(any(TestExecutionHistory.class));
        when(testMemoryService.findSimilarPatterns(any(TestContext.class), anyInt())).thenReturn(Arrays.asList());
        
        // Mock LLM service for flow adaptation decisions
        LLMService.LLMResponse mockResponse = new LLMService.LLMResponse("No adaptation needed", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(mockResponse);
    }
    
    @Test
    void shouldOrchestrateSucessfulTestExecution() throws Exception {
        // Given
        TestResult expectedResult = createSuccessfulTestResult();
        when(executionEngine.executeTest(any(TestExecutionPlan.class)))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(result.getTestId()).isEqualTo(testPlan.getTestId());
        
        // Verify interactions
        verify(correlationService).generateCorrelationId();
        verify(correlationService).startTrace(anyString(), eq("test-orchestrator"));
        verify(correlationService).completeTrace("corr-123");
        verify(testMemoryService).storeTestPattern(any(TestPattern.class));
        verify(testMemoryService).storeExecutionHistory(any(TestExecutionHistory.class));
        verify(executionEngine).executeTest(testPlan);
    }
    
    @Test
    void shouldHandleFailedTestExecution() throws Exception {
        // Given
        TestResult expectedResult = createFailedTestResult();
        when(executionEngine.executeTest(any(TestExecutionPlan.class)))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestStatus.FAILED);
        assertThat(result.getErrorMessage()).isNotNull();
        
        // Verify failure analysis was triggered
        verify(testMemoryService).findSimilarPatterns(any(TestContext.class), eq(10));
    }
    
    @Test
    void shouldTrackOrchestrationProgress() throws Exception {
        // Given
        TestResult expectedResult = createSuccessfulTestResult();
        CompletableFuture<TestResult> slowExecution = new CompletableFuture<>();
        when(executionEngine.executeTest(any(TestExecutionPlan.class))).thenReturn(slowExecution);
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        
        // Wait a bit for orchestration to start
        Thread.sleep(100);
        
        // Check progress while running
        String orchestrationId = extractOrchestrationId(resultFuture);
        OrchestrationStatus status = testOrchestrator.getOrchestrationStatus(orchestrationId);
        OrchestrationProgress progress = testOrchestrator.getOrchestrationProgress(orchestrationId);
        
        // Then
        assertThat(status).isIn(OrchestrationStatus.RUNNING, OrchestrationStatus.INITIALIZED);
        assertThat(progress).isNotNull();
        assertThat(progress.getOrchestrationId()).isEqualTo(orchestrationId);
        assertThat(progress.getTotalSteps()).isEqualTo(testPlan.getSteps().size());
        
        // Complete the execution
        slowExecution.complete(expectedResult);
        TestResult result = resultFuture.get();
        
        assertThat(result.getStatus()).isEqualTo(TestStatus.PASSED);
    }
    
    @Test
    void shouldSupportPauseAndResume() throws Exception {
        // Given
        CompletableFuture<TestResult> slowExecution = new CompletableFuture<>();
        when(executionEngine.executeTest(any(TestExecutionPlan.class))).thenReturn(slowExecution);
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        Thread.sleep(100); // Let orchestration start
        
        String orchestrationId = extractOrchestrationId(resultFuture);
        
        // Pause orchestration
        boolean paused = testOrchestrator.pauseOrchestration(orchestrationId);
        assertThat(paused).isTrue();
        assertThat(testOrchestrator.getOrchestrationStatus(orchestrationId))
            .isEqualTo(OrchestrationStatus.PAUSED);
        
        // Resume orchestration
        boolean resumed = testOrchestrator.resumeOrchestration(orchestrationId);
        assertThat(resumed).isTrue();
        assertThat(testOrchestrator.getOrchestrationStatus(orchestrationId))
            .isEqualTo(OrchestrationStatus.RUNNING);
        
        // Complete execution
        slowExecution.complete(createSuccessfulTestResult());
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result.getStatus()).isEqualTo(TestStatus.PASSED);
    }
    
    @Test
    void shouldHandleExecutionEngineFailure() throws Exception {
        // Given
        when(executionEngine.executeTest(any(TestExecutionPlan.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Execution engine failure")));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Execution engine failure");
        
        // Verify cleanup was performed
        verify(correlationService).completeTrace("corr-123");
    }
    
    @Test
    void shouldAdaptFlowWhenNeeded() throws Exception {
        // Given
        LLMService.LLMResponse adaptResponse = new LLMService.LLMResponse("Yes, adapt the flow for better performance", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(adaptResponse);
        
        TestResult expectedResult = createSuccessfulTestResult();
        CompletableFuture<TestResult> slowExecution = new CompletableFuture<>();
        when(executionEngine.executeTest(any(TestExecutionPlan.class))).thenReturn(slowExecution);
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        Thread.sleep(2000); // Wait for adaptation check
        
        // Complete execution
        slowExecution.complete(expectedResult);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result.getStatus()).isEqualTo(TestStatus.PASSED);
        
        // Verify AI was consulted for adaptation decision
        verify(llmService, atLeastOnce()).sendPrompt(contains("should we adapt"));
    }
    
    @Test
    void shouldReturnNotFoundForInvalidOrchestrationId() {
        // When
        OrchestrationStatus status = testOrchestrator.getOrchestrationStatus("invalid-id");
        OrchestrationProgress progress = testOrchestrator.getOrchestrationProgress("invalid-id");
        
        // Then
        assertThat(status).isEqualTo(OrchestrationStatus.NOT_FOUND);
        assertThat(progress.getOrchestrationId()).isEqualTo("invalid-id");
        assertThat(progress.getCurrentStep()).isEqualTo("Not Found");
    }
    
    private TestExecutionPlan createSampleTestPlan() {
        TestExecutionPlan plan = new TestExecutionPlan();
        plan.setTestId("test-123");
        plan.setScenario("Order fulfillment test scenario");
        plan.setTestData(new HashMap<>());
        plan.setConfiguration(new TestConfiguration());
        
        // Create test steps
        TestStep step1 = createTestStep("step-1", StepType.REST_CALL, "order-service");
        TestStep step2 = createTestStep("step-2", StepType.KAFKA_EVENT, "payment-service");
        TestStep step3 = createTestStep("step-3", StepType.DATABASE_CHECK, "inventory-service");
        
        plan.setSteps(Arrays.asList(step1, step2, step3));
        plan.setAssertions(Arrays.asList(createAssertionRule()));
        
        return plan;
    }
    
    private TestStep createTestStep(String stepId, StepType type, String targetService) {
        TestStep step = new TestStep();
        step.setStepId(stepId);
        step.setType(type);
        step.setTargetService(targetService);
        step.setTimeout(Duration.ofSeconds(30));
        step.setInputData(new HashMap<>());
        step.setExpectedOutcomes(Arrays.asList(createExpectedOutcome()));
        step.setRetryPolicy(createRetryPolicy());
        return step;
    }
    
    private ExpectedOutcome createExpectedOutcome() {
        ExpectedOutcome outcome = new ExpectedOutcome();
        outcome.setType(OutcomeType.SUCCESS_RESPONSE);
        outcome.setDescription("Successful execution");
        return outcome;
    }
    
    private RetryPolicy createRetryPolicy() {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxAttempts(3);
        policy.setBackoffMultiplier(1.5);
        return policy;
    }
    
    private AssertionRule createAssertionRule() {
        AssertionRule rule = new AssertionRule();
        rule.setRuleId("rule-1");
        rule.setType(AssertionType.EQUALS);
        rule.setSeverity(AssertionSeverity.CRITICAL);
        rule.setDescription("Order status should be confirmed");
        rule.setExpectedValue("CONFIRMED");
        return rule;
    }
    
    private TestResult createSuccessfulTestResult() {
        TestResult result = new TestResult();
        result.setTestId(testPlan.getTestId());
        result.setStatus(TestStatus.PASSED);
        result.setStartTime(Instant.now().minusSeconds(60));
        result.setEndTime(Instant.now());
        result.setStepResults(Arrays.asList(createStepResult()));
        result.setAssertionResults(Arrays.asList(createAssertionResult()));
        return result;
    }
    
    private TestResult createFailedTestResult() {
        TestResult result = new TestResult();
        result.setTestId(testPlan.getTestId());
        result.setStatus(TestStatus.FAILED);
        result.setStartTime(Instant.now().minusSeconds(60));
        result.setEndTime(Instant.now());
        result.setErrorMessage("Test execution failed");
        result.setStepResults(Arrays.asList(createFailedStepResult()));
        return result;
    }
    
    private StepResult createStepResult() {
        StepResult stepResult = new StepResult();
        stepResult.setStepId("step-1");
        stepResult.setStatus(TestStatus.PASSED);
        stepResult.setStartTime(Instant.now().minusSeconds(30));
        stepResult.setEndTime(Instant.now().minusSeconds(25));
        return stepResult;
    }
    
    private StepResult createFailedStepResult() {
        StepResult stepResult = new StepResult();
        stepResult.setStepId("step-1");
        stepResult.setStatus(TestStatus.FAILED);
        stepResult.setStartTime(Instant.now().minusSeconds(30));
        stepResult.setEndTime(Instant.now().minusSeconds(25));
        stepResult.setErrorMessage("Step execution failed");
        return stepResult;
    }
    
    private AssertionResult createAssertionResult() {
        AssertionResult assertionResult = new AssertionResult();
        assertionResult.setRuleId("rule-1");
        assertionResult.setPassed(true);
        assertionResult.setActualValue("CONFIRMED");
        assertionResult.setExpectedValue("CONFIRMED");
        return assertionResult;
    }
    
    private String extractOrchestrationId(CompletableFuture<TestResult> resultFuture) {
        // In a real implementation, this would extract the orchestration ID
        // For testing purposes, we'll generate a mock ID
        return "orch-" + System.currentTimeMillis();
    }
}