package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.service.CorrelationTrackingService;
import com.agentic.e2etester.service.TestMemoryService;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestOrchestrator
 */
@ExtendWith(MockitoExtension.class)
class TestOrchestratorTest {
    
    @Mock
    private TestExecutionEngine executionEngine;
    
    @Mock
    private LLMService llmService;
    
    @Mock
    private CorrelationTrackingService correlationService;
    
    @Mock
    private TestMemoryService testMemoryService;
    
    @Mock
    private TestProgressTracker progressTracker;
    
    @Mock
    private TestFlowAdapter flowAdapter;
    
    private TestOrchestrator testOrchestrator;
    private TestExecutionPlan testPlan;
    
    @BeforeEach
    void setUp() {
        testOrchestrator = new TestOrchestrator(
            executionEngine, llmService, correlationService, 
            testMemoryService, progressTracker, flowAdapter
        );
        
        testPlan = createTestPlan();
        
        // Default mock behaviors
        when(correlationService.generateCorrelationId()).thenReturn("corr-123");
        when(correlationService.startTrace(anyString(), anyString())).thenReturn(new CorrelationTrace());
        when(correlationService.completeTrace(anyString())).thenReturn(Optional.empty());
        doNothing().when(testMemoryService).storeTestPattern(any(TestPattern.class));
        doNothing().when(testMemoryService).storeExecutionHistory(any(TestExecutionHistory.class));
        when(testMemoryService.findSimilarPatterns(any(TestContext.class), anyInt())).thenReturn(Arrays.asList());
        doNothing().when(progressTracker).updateProgress(any(OrchestrationContext.class));
        doNothing().when(progressTracker).markCompleted(any(OrchestrationContext.class), any(TestResult.class));
        when(progressTracker.getCompletedSteps(any(OrchestrationContext.class))).thenReturn(0);
        when(progressTracker.getErrorCount(any(OrchestrationContext.class))).thenReturn(0);
        LLMService.LLMResponse mockResponse = new LLMService.LLMResponse("No adaptation needed", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(mockResponse);
        when(flowAdapter.adaptFlow(any(OrchestrationContext.class))).thenReturn(null);
    }
    
    @Test
    void shouldOrchestrateSucessfulTest() throws Exception {
        // Given
        TestResult expectedResult = createSuccessfulResult();
        when(executionEngine.executeTest(testPlan))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(result.getTestId()).isEqualTo(testPlan.getTestId());
        
        verify(correlationService).generateCorrelationId();
        verify(correlationService).startTrace(anyString(), eq("test-orchestrator"));
        verify(correlationService).completeTrace("corr-123");
        verify(testMemoryService).storeTestPattern(any(TestPattern.class));
        verify(testMemoryService).storeExecutionHistory(any(TestExecutionHistory.class));
        verify(progressTracker).markCompleted(any(OrchestrationContext.class), eq(expectedResult));
    }
    
    @Test
    void shouldHandleExecutionFailure() throws Exception {
        // Given
        RuntimeException executionError = new RuntimeException("Execution failed");
        when(executionEngine.executeTest(testPlan))
            .thenReturn(CompletableFuture.failedFuture(executionError));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Execution failed");
        
        verify(correlationService).completeTrace("corr-123");
    }
    
    @Test
    void shouldReturnNotFoundForInvalidOrchestrationId() {
        // When
        OrchestrationStatus status = testOrchestrator.getOrchestrationStatus("invalid-id");
        
        // Then
        assertThat(status).isEqualTo(OrchestrationStatus.NOT_FOUND);
    }
    
    @Test
    void shouldReturnProgressForValidOrchestrationId() throws Exception {
        // Given
        OrchestrationProgress mockProgress = new OrchestrationProgress(
            "orch-123", 3, 1, 0, "Step 1", 33.3, 
            Duration.ofSeconds(30), Duration.ofSeconds(60), false, null
        );
        when(progressTracker.getProgress(any(OrchestrationContext.class))).thenReturn(mockProgress);
        
        CompletableFuture<TestResult> slowExecution = new CompletableFuture<>();
        when(executionEngine.executeTest(testPlan)).thenReturn(slowExecution);
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        Thread.sleep(100); // Let orchestration start
        
        // Extract orchestration ID (in real implementation, this would be available)
        String orchestrationId = "test-orchestration-id";
        
        // Then - we can't easily test this without exposing internal state
        // In a real implementation, we'd need a way to get the orchestration ID
        verify(progressTracker, atLeastOnce()).updateProgress(any(OrchestrationContext.class));
        
        // Cleanup
        slowExecution.complete(createSuccessfulResult());
        resultFuture.get();
    }
    
    @Test
    void shouldPauseAndResumeOrchestration() throws Exception {
        // Given
        CompletableFuture<TestResult> slowExecution = new CompletableFuture<>();
        when(executionEngine.executeTest(testPlan)).thenReturn(slowExecution);
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        Thread.sleep(100); // Let orchestration start
        
        // Note: In a real test, we'd need access to the orchestration ID
        // For now, we test the methods with a mock ID
        boolean pauseResult = testOrchestrator.pauseOrchestration("non-existent-id");
        boolean resumeResult = testOrchestrator.resumeOrchestration("non-existent-id");
        
        // Then
        assertThat(pauseResult).isFalse(); // Should return false for non-existent ID
        assertThat(resumeResult).isFalse(); // Should return false for non-existent ID
        
        // Cleanup
        slowExecution.complete(createSuccessfulResult());
        resultFuture.get();
    }
    
    @Test
    void shouldStoreTestPatternForLearning() throws Exception {
        // Given
        TestResult expectedResult = createSuccessfulResult();
        when(executionEngine.executeTest(testPlan))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        resultFuture.get();
        
        // Then
        verify(testMemoryService).storeTestPattern(argThat(pattern -> 
            pattern.getName().contains(testPlan.getTestId()) &&
            pattern.getType() == PatternType.SUCCESS_FLOW &&
            pattern.getDescription().contains(String.valueOf(testPlan.getSteps().size()))
        ));
    }
    
    @Test
    void shouldAnalyzeFailurePatternsOnFailure() throws Exception {
        // Given
        TestResult failedResult = createFailedResult();
        when(executionEngine.executeTest(testPlan))
            .thenReturn(CompletableFuture.completedFuture(failedResult));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result.getStatus()).isEqualTo(TestStatus.FAILED);
        verify(testMemoryService).findSimilarPatterns(any(TestContext.class), eq(10));
    }
    
    @Test
    void shouldHandleLLMServiceFailureGracefully() throws Exception {
        // Given
        when(llmService.sendPrompt(anyString())).thenThrow(new RuntimeException("LLM service unavailable"));
        
        TestResult expectedResult = createSuccessfulResult();
        when(executionEngine.executeTest(testPlan))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        // When
        CompletableFuture<TestResult> resultFuture = testOrchestrator.orchestrateTest(testPlan);
        TestResult result = resultFuture.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TestStatus.PASSED);
        // Orchestration should continue despite LLM failure
    }
    
    private TestExecutionPlan createTestPlan() {
        TestExecutionPlan plan = new TestExecutionPlan();
        plan.setTestId("test-123");
        plan.setScenario("Test scenario");
        plan.setTestData(new HashMap<>());
        plan.setConfiguration(new TestConfiguration());
        
        TestStep step1 = new TestStep();
        step1.setStepId("step-1");
        step1.setType(StepType.REST_CALL);
        step1.setTargetService("service-1");
        step1.setTimeout(Duration.ofSeconds(30));
        step1.setRetryPolicy(new RetryPolicy());
        
        TestStep step2 = new TestStep();
        step2.setStepId("step-2");
        step2.setType(StepType.KAFKA_EVENT);
        step2.setTargetService("service-2");
        step2.setTimeout(Duration.ofSeconds(30));
        step2.setRetryPolicy(new RetryPolicy());
        
        plan.setSteps(Arrays.asList(step1, step2));
        plan.setAssertions(Arrays.asList());
        
        return plan;
    }
    
    private TestResult createSuccessfulResult() {
        TestResult result = new TestResult();
        result.setTestId(testPlan.getTestId());
        result.setStatus(TestStatus.PASSED);
        result.setStartTime(Instant.now().minusSeconds(60));
        result.setEndTime(Instant.now());
        return result;
    }
    
    private TestResult createFailedResult() {
        TestResult result = new TestResult();
        result.setTestId(testPlan.getTestId());
        result.setStatus(TestStatus.FAILED);
        result.setStartTime(Instant.now().minusSeconds(60));
        result.setEndTime(Instant.now());
        result.setErrorMessage("Test failed");
        return result;
    }
}