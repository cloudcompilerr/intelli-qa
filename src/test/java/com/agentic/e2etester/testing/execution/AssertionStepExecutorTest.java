package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AssertionStepExecutorTest {
    
    private AssertionStepExecutor stepExecutor;
    
    @BeforeEach
    void setUp() {
        stepExecutor = new AssertionStepExecutor();
    }
    
    @Test
    void canExecute_AssertionStep_ReturnsTrue() {
        // Arrange
        TestStep step = new TestStep("step-1", StepType.ASSERTION, "assertion-service", 30000L);
        
        // Act & Assert
        assertTrue(stepExecutor.canExecute(step));
    }
    
    @Test
    void canExecute_NonAssertionStep_ReturnsFalse() {
        // Arrange
        TestStep step = new TestStep("step-1", StepType.REST_CALL, "test-service", 30000L);
        
        // Act & Assert
        assertFalse(stepExecutor.canExecute(step));
    }
    
    @Test
    void executeStep_EqualsAssertion_Passed() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("EQUALS", "expected", "expected");
        TestContext context = new TestContext("correlation-123");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
        assertTrue(result.getOutput().toString().contains("PASSED"));
        assertTrue(result.getOutput().toString().contains("expected=expected"));
    }
    
    @Test
    void executeStep_EqualsAssertion_Failed() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("EQUALS", "expected", "actual");
        TestContext context = new TestContext("correlation-456");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertTrue(result.getOutput().toString().contains("FAILED"));
        assertTrue(result.getOutput().toString().contains("expected=expected"));
        assertTrue(result.getOutput().toString().contains("actual=actual"));
    }
    
    @Test
    void executeStep_NotNullAssertion_Passed() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("NOT_NULL", null, "some value");
        TestContext context = new TestContext("correlation-789");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
    }
    
    @Test
    void executeStep_NotNullAssertion_Failed() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("NOT_NULL", null, null);
        TestContext context = new TestContext("correlation-null");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
    }
    
    @Test
    void executeStep_ContainsAssertion_Passed() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("CONTAINS", "test", "this is a test string");
        TestContext context = new TestContext("correlation-contains");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
    }
    
    @Test
    void executeStep_GreaterThanAssertion_Passed() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("GREATER_THAN", 5, 10);
        TestContext context = new TestContext("correlation-gt");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
    }
    
    @Test
    void executeStep_LessThanAssertion_Passed() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("LESS_THAN", 10, 5);
        TestContext context = new TestContext("correlation-lt");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
    }
    
    @Test
    void executeStep_UnsupportedAssertionType_ThrowsException() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("UNSUPPORTED", "value1", "value2");
        TestContext context = new TestContext("correlation-error");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Unsupported assertion type"));
    }
    
    @Test
    void executeStep_NumericComparisonWithNonNumbers_ThrowsException() throws Exception {
        // Arrange
        TestStep step = createAssertionStep("GREATER_THAN", "not a number", "also not a number");
        TestContext context = new TestContext("correlation-numeric-error");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Cannot compare non-numeric values"));
    }
    
    private TestStep createAssertionStep(String assertionType, Object expectedValue, Object actualValue) {
        TestStep step = new TestStep("step-1", StepType.ASSERTION, "assertion-service", 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("assertionType", assertionType);
        inputData.put("expectedValue", expectedValue);
        inputData.put("actualValue", actualValue);
        
        step.setInputData(inputData);
        return step;
    }
}