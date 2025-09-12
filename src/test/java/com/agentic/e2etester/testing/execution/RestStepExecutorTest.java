package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.integration.rest.RestApiAdapter;
import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestStepExecutorTest {
    
    @Mock
    private RestApiAdapter restApiAdapter;
    
    private RestStepExecutor stepExecutor;
    
    @BeforeEach
    void setUp() {
        stepExecutor = new RestStepExecutor(restApiAdapter);
    }
    
    @Test
    void canExecute_RestCallStep_ReturnsTrue() {
        // Arrange
        TestStep step = new TestStep("step-1", StepType.REST_CALL, "test-service", 30000L);
        
        // Act & Assert
        assertTrue(stepExecutor.canExecute(step));
    }
    
    @Test
    void canExecute_NonRestStep_ReturnsFalse() {
        // Arrange
        TestStep step = new TestStep("step-1", StepType.KAFKA_EVENT, "test-service", 30000L);
        
        // Act & Assert
        assertFalse(stepExecutor.canExecute(step));
    }
    
    @Test
    void executeStep_GetRequest_ReturnsSuccessfulResult() throws Exception {
        // Arrange
        TestStep step = createRestStep("GET", "/api/test", null);
        TestContext context = new TestContext("correlation-123");
        
        ServiceInteraction successfulInteraction = new ServiceInteraction(
            "test-service", InteractionType.HTTP_REQUEST, InteractionStatus.SUCCESS);
        successfulInteraction.setResponse("Success response");
        
        when(restApiAdapter.get(eq("test-service"), eq("/api/test"), eq("correlation-123"), isNull()))
            .thenReturn(CompletableFuture.completedFuture(successfulInteraction));
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertNotNull(result);
        assertEquals("step-1", result.getStepId());
        assertEquals(TestStatus.PASSED, result.getStatus());
        assertEquals("Success response", result.getOutput());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        
        verify(restApiAdapter).get("test-service", "/api/test", "correlation-123", null);
    }
    
    @Test
    void executeStep_PostRequest_ReturnsSuccessfulResult() throws Exception {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", "test");
        
        TestStep step = createRestStep("POST", "/api/create", requestBody);
        TestContext context = new TestContext("correlation-456");
        
        ServiceInteraction successfulInteraction = new ServiceInteraction(
            "test-service", InteractionType.HTTP_REQUEST, InteractionStatus.SUCCESS);
        successfulInteraction.setResponse("Created");
        
        when(restApiAdapter.post(eq("test-service"), eq("/api/create"), eq(requestBody), eq("correlation-456"), isNull()))
            .thenReturn(CompletableFuture.completedFuture(successfulInteraction));
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
        assertEquals("Created", result.getOutput());
        
        verify(restApiAdapter).post("test-service", "/api/create", requestBody, "correlation-456", null);
    }
    
    @Test
    void executeStep_UnsupportedMethod_ReturnsFailedResult() throws Exception {
        // Arrange
        TestStep step = createRestStep("PATCH", "/api/test", null);
        TestContext context = new TestContext("correlation-789");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Unsupported HTTP method: PATCH"));
        
        verifyNoInteractions(restApiAdapter);
    }
    
    @Test
    void executeStep_RestAdapterThrowsException_ReturnsFailedResult() throws Exception {
        // Arrange
        TestStep step = createRestStep("GET", "/api/test", null);
        TestContext context = new TestContext("correlation-error");
        
        CompletableFuture<ServiceInteraction> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Connection timeout"));
        
        when(restApiAdapter.get(anyString(), anyString(), anyString(), isNull()))
            .thenReturn(failedFuture);
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertEquals("Connection timeout", result.getErrorMessage());
    }
    
    @Test
    void executeStep_FailedInteraction_ReturnsFailedResult() throws Exception {
        // Arrange
        TestStep step = createRestStep("GET", "/api/test", null);
        TestContext context = new TestContext("correlation-fail");
        
        ServiceInteraction failedInteraction = new ServiceInteraction(
            "test-service", InteractionType.HTTP_REQUEST, InteractionStatus.FAILURE);
        failedInteraction.setResponse("Error response");
        
        when(restApiAdapter.get(anyString(), anyString(), anyString(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(failedInteraction));
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertEquals("Error response", result.getOutput());
    }
    
    private TestStep createRestStep(String method, String endpoint, Object body) {
        TestStep step = new TestStep("step-1", StepType.REST_CALL, "test-service", 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("method", method);
        inputData.put("endpoint", endpoint);
        if (body != null) {
            inputData.put("body", body);
        }
        
        step.setInputData(inputData);
        return step;
    }
}