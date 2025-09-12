package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefaultTestExecutionEngineTest {
    
    @Mock
    private StepExecutor mockStepExecutor;
    
    private DefaultTestExecutionEngine executionEngine;
    
    @BeforeEach
    void setUp() {
        List<StepExecutor> stepExecutors = Arrays.asList(mockStepExecutor);
        executionEngine = new DefaultTestExecutionEngine(stepExecutors);
    }
    
    @Test
    void executeTest_SuccessfulExecution_ReturnsPassedResult() throws Exception {
        // Arrange
        TestExecutionPlan plan = createTestPlan("test-1", Arrays.asList(
            createTestStep("step-1", StepType.REST_CALL),
            createTestStep("step-2", StepType.KAFKA_EVENT)
        ));
        
        when(mockStepExecutor.canExecute(any())).thenReturn(true);
        when(mockStepExecutor.executeStep(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createSuccessfulStepResult("step-1")))
            .thenReturn(CompletableFuture.completedFuture(createSuccessfulStepResult("step-2")));
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        TestResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertNotNull(result);
        assertEquals(TestStatus.PASSED, result.getStatus());
        assertEquals("test-1", result.getTestId());
        assertEquals(2, result.getTotalSteps());
        assertEquals(2, result.getSuccessfulSteps());
        assertEquals(0, result.getFailedSteps());
        assertNotNull(result.getCorrelationId());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        
        verify(mockStepExecutor, times(2)).executeStep(any(), any());
    }
    
    @Test
    void executeTest_StepFailure_ReturnsFailedResult() throws Exception {
        // Arrange
        TestExecutionPlan plan = createTestPlan("test-2", Arrays.asList(
            createTestStep("step-1", StepType.REST_CALL)
        ));
        
        when(mockStepExecutor.canExecute(any())).thenReturn(true);
        when(mockStepExecutor.executeStep(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createFailedStepResult("step-1", "Connection failed")));
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        TestResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertEquals(1, result.getTotalSteps());
        assertEquals(0, result.getSuccessfulSteps());
        assertEquals(1, result.getFailedSteps());
    }
    
    @Test
    void executeTest_MixedResults_ReturnsPartialSuccess() throws Exception {
        // Arrange
        TestExecutionPlan plan = createTestPlan("test-3", Arrays.asList(
            createTestStep("step-1", StepType.REST_CALL),
            createTestStep("step-2", StepType.KAFKA_EVENT)
        ));
        
        // Configure to not stop on first failure
        plan.getConfiguration().setFailFast(false);
        
        when(mockStepExecutor.canExecute(any())).thenReturn(true);
        when(mockStepExecutor.executeStep(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createSuccessfulStepResult("step-1")))
            .thenReturn(CompletableFuture.completedFuture(createFailedStepResult("step-2", "Kafka error")));
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        TestResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PARTIAL_SUCCESS, result.getStatus());
        assertEquals(2, result.getTotalSteps());
        assertEquals(1, result.getSuccessfulSteps());
        assertEquals(1, result.getFailedSteps());
    }
    
    @Test
    void executeTest_NoExecutorFound_ReturnsFailedResult() throws Exception {
        // Arrange
        TestExecutionPlan plan = createTestPlan("test-4", Arrays.asList(
            createTestStep("step-1", StepType.REST_CALL)
        ));
        
        when(mockStepExecutor.canExecute(any())).thenReturn(false);
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        TestResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertEquals(1, result.getFailedSteps());
        assertTrue(result.getStepResults().get(0).getErrorMessage().contains("No executor found"));
    }
    
    @Test
    void getExecutionStatus_ActiveExecution_ReturnsRunningStatus() {
        // Arrange
        TestExecutionPlan plan = createTestPlan("test-5", Arrays.asList(
            createTestStep("step-1", StepType.REST_CALL)
        ));
        
        lenient().when(mockStepExecutor.canExecute(any())).thenReturn(true);
        lenient().when(mockStepExecutor.executeStep(any(), any()))
            .thenReturn(new CompletableFuture<>()); // Never completes
        
        // Act
        executionEngine.executeTest(plan);
        TestStatus status = executionEngine.getExecutionStatus("test-5");
        
        // Assert
        assertEquals(TestStatus.RUNNING, status);
    }
    
    @Test
    void getExecutionStatus_NonExistentTest_ReturnsNotFound() {
        // Act
        TestStatus status = executionEngine.getExecutionStatus("non-existent");
        
        // Assert
        assertEquals(TestStatus.NOT_FOUND, status);
    }
    
    @Test
    void pauseAndResumeExecution_ValidTest_UpdatesStatus() throws Exception {
        // This test would require more complex setup to properly test pause/resume
        // For now, we'll test the basic functionality
        
        // Arrange
        TestExecutionPlan plan = createTestPlan("test-6", Arrays.asList(
            createTestStep("step-1", StepType.REST_CALL)
        ));
        
        when(mockStepExecutor.canExecute(any())).thenReturn(true);
        when(mockStepExecutor.executeStep(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(createSuccessfulStepResult("step-1")));
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        executionEngine.pauseExecution("test-6");
        executionEngine.resumeExecution("test-6");
        
        TestResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertNotNull(result);
        // The test should still complete successfully
        assertEquals(TestStatus.PASSED, result.getStatus());
    }
    
    @Test
    void cancelExecution_ValidTest_CancelsExecution() {
        // Arrange
        TestExecutionPlan plan = createTestPlan("test-7", Arrays.asList(
            createTestStep("step-1", StepType.REST_CALL)
        ));
        
        lenient().when(mockStepExecutor.canExecute(any())).thenReturn(true);
        lenient().when(mockStepExecutor.executeStep(any(), any()))
            .thenReturn(new CompletableFuture<>()); // Never completes
        
        // Act
        executionEngine.executeTest(plan);
        executionEngine.cancelExecution("test-7");
        
        // Assert
        TestStatus status = executionEngine.getExecutionStatus("test-7");
        assertEquals(TestStatus.CANCELLED, status);
    }
    
    private TestExecutionPlan createTestPlan(String testId, List<TestStep> steps) {
        TestConfiguration config = new TestConfiguration();
        config.setDefaultTimeoutMs(30000L);
        config.setFailFast(true);
        
        TestExecutionPlan plan = new TestExecutionPlan(testId, "Test scenario", steps, config);
        return plan;
    }
    
    private TestStep createTestStep(String stepId, StepType type) {
        TestStep step = new TestStep(stepId, type, "test-service", 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("method", "GET");
        inputData.put("endpoint", "/test");
        step.setInputData(inputData);
        
        return step;
    }
    
    private StepResult createSuccessfulStepResult(String stepId) {
        StepResult result = new StepResult(stepId, TestStatus.PASSED);
        result.setOutput("Success");
        return result;
    }
    
    private StepResult createFailedStepResult(String stepId, String errorMessage) {
        StepResult result = new StepResult(stepId, TestStatus.FAILED);
        result.setErrorMessage(errorMessage);
        return result;
    }
}