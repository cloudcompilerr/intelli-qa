package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.integration.kafka.KafkaTestProducer;
import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaStepExecutorTest {
    
    @Mock
    private KafkaTestProducer kafkaTestProducer;
    
    @Mock
    private SendResult<String, Object> sendResult;
    
    private KafkaStepExecutor stepExecutor;
    
    @BeforeEach
    void setUp() {
        stepExecutor = new KafkaStepExecutor(kafkaTestProducer);
    }
    
    @Test
    void canExecute_KafkaEventStep_ReturnsTrue() {
        // Arrange
        TestStep step = new TestStep("step-1", StepType.KAFKA_EVENT, "kafka-service", 30000L);
        
        // Act & Assert
        assertTrue(stepExecutor.canExecute(step));
    }
    
    @Test
    void canExecute_NonKafkaStep_ReturnsFalse() {
        // Arrange
        TestStep step = new TestStep("step-1", StepType.REST_CALL, "test-service", 30000L);
        
        // Act & Assert
        assertFalse(stepExecutor.canExecute(step));
    }
    
    @Test
    void executeStep_SuccessfulSend_ReturnsSuccessfulResult() throws Exception {
        // Arrange
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("orderId", "12345");
        eventData.put("status", "created");
        
        TestStep step = createKafkaStep("order-events", eventData);
        TestContext context = new TestContext("correlation-123");
        
        when(kafkaTestProducer.sendTestEvent(eq("order-events"), eq(eventData), eq(context)))
            .thenReturn(CompletableFuture.completedFuture(sendResult));
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertNotNull(result);
        assertEquals("step-1", result.getStepId());
        assertEquals(TestStatus.PASSED, result.getStatus());
        assertEquals(sendResult, result.getOutput());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        
        verify(kafkaTestProducer).sendTestEvent("order-events", eventData, context);
    }
    
    @Test
    void executeStep_MissingTopic_ReturnsFailedResult() throws Exception {
        // Arrange
        TestStep step = new TestStep("step-1", StepType.KAFKA_EVENT, "kafka-service", 30000L);
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("eventData", "test data");
        // Missing topic
        step.setInputData(inputData);
        
        TestContext context = new TestContext("correlation-456");
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertEquals("Topic is required for Kafka step", result.getErrorMessage());
        
        verifyNoInteractions(kafkaTestProducer);
    }
    
    @Test
    void executeStep_KafkaProducerThrowsException_ReturnsFailedResult() throws Exception {
        // Arrange
        TestStep step = createKafkaStep("error-topic", "test data");
        TestContext context = new TestContext("correlation-error");
        
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
        
        when(kafkaTestProducer.sendTestEvent(anyString(), any(), any()))
            .thenReturn(failedFuture);
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("Failed to send Kafka event"));
        assertTrue(result.getErrorMessage().contains("Kafka broker unavailable"));
    }
    
    @Test
    void executeStep_NullEventData_StillExecutes() throws Exception {
        // Arrange
        TestStep step = createKafkaStep("test-topic", null);
        TestContext context = new TestContext("correlation-null");
        
        when(kafkaTestProducer.sendTestEvent(eq("test-topic"), isNull(), eq(context)))
            .thenReturn(CompletableFuture.completedFuture(sendResult));
        
        // Act
        CompletableFuture<StepResult> resultFuture = stepExecutor.executeStep(step, context);
        StepResult result = resultFuture.get(5, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
        
        verify(kafkaTestProducer).sendTestEvent("test-topic", null, context);
    }
    
    private TestStep createKafkaStep(String topic, Object eventData) {
        TestStep step = new TestStep("step-1", StepType.KAFKA_EVENT, "kafka-service", 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("topic", topic);
        inputData.put("eventData", eventData);
        
        step.setInputData(inputData);
        return step;
    }
}