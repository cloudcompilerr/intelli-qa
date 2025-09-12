package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.integration.kafka.KafkaTestProducer;
import com.agentic.e2etester.integration.rest.RestApiAdapter;
import com.agentic.e2etester.integration.database.CouchbaseAdapter;
import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the complete test execution engine workflow.
 * Tests the orchestration of multiple step types in a realistic scenario.
 */
@ExtendWith(MockitoExtension.class)
class TestExecutionEngineIntegrationTest {
    
    @Mock
    private RestApiAdapter restApiAdapter;
    
    @Mock
    private KafkaTestProducer kafkaTestProducer;
    
    @Mock
    private CouchbaseAdapter couchbaseAdapter;
    
    @Mock
    private SendResult<String, Object> sendResult;
    
    private DefaultTestExecutionEngine executionEngine;
    
    @BeforeEach
    void setUp() {
        // Create step executors
        List<StepExecutor> stepExecutors = Arrays.asList(
            new RestStepExecutor(restApiAdapter),
            new KafkaStepExecutor(kafkaTestProducer),
            new DatabaseStepExecutor(couchbaseAdapter),
            new AssertionStepExecutor()
        );
        
        executionEngine = new DefaultTestExecutionEngine(stepExecutors);
    }
    
    @Test
    void executeCompleteOrderFulfillmentTest_AllStepsSucceed_ReturnsPassedResult() throws Exception {
        // Arrange
        TestExecutionPlan plan = createOrderFulfillmentTestPlan();
        
        // Mock REST API calls
        ServiceInteraction createOrderInteraction = new ServiceInteraction(
            "order-service", InteractionType.HTTP_REQUEST, InteractionStatus.SUCCESS);
        createOrderInteraction.setResponse(Map.of("orderId", "12345", "status", "created"));
        
        ServiceInteraction inventoryCheckInteraction = new ServiceInteraction(
            "inventory-service", InteractionType.HTTP_REQUEST, InteractionStatus.SUCCESS);
        inventoryCheckInteraction.setResponse(Map.of("available", true, "quantity", 10));
        
        when(restApiAdapter.post(eq("order-service"), eq("/orders"), any(), anyString(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(createOrderInteraction));
        
        when(restApiAdapter.get(eq("inventory-service"), eq("/inventory/item-123"), anyString(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(inventoryCheckInteraction));
        
        // Mock Kafka event publishing
        when(kafkaTestProducer.sendTestEvent(eq("order-events"), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(sendResult));
        
        // Mock database operations
        when(couchbaseAdapter.getDocument(eq("orders"), eq("orders"), eq("12345")))
            .thenReturn(Optional.of(com.couchbase.client.java.json.JsonObject.create()
                .put("orderId", "12345")
                .put("status", "created")));
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        TestResult result = resultFuture.get(30, TimeUnit.SECONDS);
        
        // Assert
        assertNotNull(result);
        assertEquals(TestStatus.PASSED, result.getStatus());
        assertEquals("order-fulfillment-test", result.getTestId());
        assertEquals(5, result.getTotalSteps());
        assertEquals(5, result.getSuccessfulSteps());
        assertEquals(0, result.getFailedSteps());
        
        // Verify all interactions occurred
        verify(restApiAdapter).post(eq("order-service"), eq("/orders"), any(), anyString(), isNull());
        verify(restApiAdapter).get(eq("inventory-service"), eq("/inventory/item-123"), anyString(), isNull());
        verify(kafkaTestProducer).sendTestEvent(eq("order-events"), any(), any());
        verify(couchbaseAdapter).getDocument(eq("orders"), eq("orders"), eq("12345"));
        
        // Verify step results
        List<StepResult> stepResults = result.getStepResults();
        assertEquals(5, stepResults.size());
        
        assertTrue(stepResults.stream().allMatch(StepResult::isSuccessful));
        assertTrue(stepResults.stream().allMatch(sr -> sr.getCorrelationId() != null));
        assertTrue(stepResults.stream().allMatch(sr -> sr.getStartTime() != null));
        assertTrue(stepResults.stream().allMatch(sr -> sr.getEndTime() != null));
    }
    
    @Test
    void executeTestWithRetry_FirstAttemptFails_RetriesAndSucceeds() throws Exception {
        // Arrange
        TestExecutionPlan plan = createRetryTestPlan();
        
        // First call fails, second succeeds
        ServiceInteraction failedInteraction = new ServiceInteraction(
            "flaky-service", InteractionType.HTTP_REQUEST, InteractionStatus.FAILURE);
        ServiceInteraction successInteraction = new ServiceInteraction(
            "flaky-service", InteractionType.HTTP_REQUEST, InteractionStatus.SUCCESS);
        successInteraction.setResponse("Success on retry");
        
        when(restApiAdapter.get(eq("flaky-service"), eq("/health"), anyString(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(failedInteraction))
            .thenReturn(CompletableFuture.completedFuture(successInteraction));
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        TestResult result = resultFuture.get(30, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.PASSED, result.getStatus());
        assertEquals(1, result.getSuccessfulSteps());
        assertEquals(0, result.getFailedSteps());
        
        // Verify retry occurred
        verify(restApiAdapter, times(2)).get(eq("flaky-service"), eq("/health"), anyString(), isNull());
        
        // Check that attempt count is recorded
        StepResult stepResult = result.getStepResults().get(0);
        assertEquals(2, stepResult.getAttemptCount());
    }
    
    @Test
    void executeTestWithFailFast_FirstStepFails_StopsExecution() throws Exception {
        // Arrange
        TestExecutionPlan plan = createFailFastTestPlan();
        
        // First step fails
        ServiceInteraction failedInteraction = new ServiceInteraction(
            "critical-service", InteractionType.HTTP_REQUEST, InteractionStatus.FAILURE);
        
        when(restApiAdapter.get(eq("critical-service"), eq("/critical"), anyString(), isNull()))
            .thenReturn(CompletableFuture.completedFuture(failedInteraction));
        
        // Act
        CompletableFuture<TestResult> resultFuture = executionEngine.executeTest(plan);
        TestResult result = resultFuture.get(30, TimeUnit.SECONDS);
        
        // Assert
        assertEquals(TestStatus.FAILED, result.getStatus());
        assertEquals(2, result.getTotalSteps());
        assertEquals(0, result.getSuccessfulSteps());
        assertEquals(1, result.getFailedSteps());
        
        // Verify only first step was executed
        assertEquals(1, result.getStepResults().size());
        
        // Verify second step was never called
        verify(kafkaTestProducer, never()).sendTestEvent(anyString(), any(), any());
    }
    
    private TestExecutionPlan createOrderFulfillmentTestPlan() {
        TestConfiguration config = new TestConfiguration();
        config.setDefaultTimeoutMs(30000L);
        config.setFailFast(false);
        
        List<TestStep> steps = Arrays.asList(
            createRestStep("create-order", "POST", "order-service", "/orders", 
                Map.of("itemId", "item-123", "quantity", 2)),
            createRestStep("check-inventory", "GET", "inventory-service", "/inventory/item-123", null),
            createKafkaStep("publish-order-event", "order-events", 
                Map.of("orderId", "12345", "event", "order-created")),
            createDatabaseStep("verify-order-stored", "orders", "orders", "12345", "GET"),
            createAssertionStep("verify-order-status", "EQUALS", "created", "created")
        );
        
        return new TestExecutionPlan("order-fulfillment-test", 
            "Complete order fulfillment test scenario", steps, config);
    }
    
    private TestExecutionPlan createRetryTestPlan() {
        TestConfiguration config = new TestConfiguration();
        config.setDefaultTimeoutMs(10000L);
        
        RetryPolicy retryPolicy = new RetryPolicy(3, 100L);
        retryPolicy.setRetryOnExceptions(new String[]{"RuntimeException", "TimeoutException"});
        
        TestStep step = createRestStep("flaky-call", "GET", "flaky-service", "/health", null);
        step.setRetryPolicy(retryPolicy);
        
        return new TestExecutionPlan("retry-test", "Test retry functionality", 
            Arrays.asList(step), config);
    }
    
    private TestExecutionPlan createFailFastTestPlan() {
        TestConfiguration config = new TestConfiguration();
        config.setFailFast(true);
        
        List<TestStep> steps = Arrays.asList(
            createRestStep("critical-step", "GET", "critical-service", "/critical", null),
            createKafkaStep("should-not-execute", "test-topic", Map.of("data", "test"))
        );
        
        return new TestExecutionPlan("fail-fast-test", "Test fail-fast behavior", steps, config);
    }
    
    private TestStep createRestStep(String stepId, String method, String service, String endpoint, Object body) {
        TestStep step = new TestStep(stepId, StepType.REST_CALL, service, 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("method", method);
        inputData.put("endpoint", endpoint);
        if (body != null) {
            inputData.put("body", body);
        }
        
        step.setInputData(inputData);
        return step;
    }
    
    private TestStep createKafkaStep(String stepId, String topic, Object eventData) {
        TestStep step = new TestStep(stepId, StepType.KAFKA_EVENT, "kafka-service", 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("topic", topic);
        inputData.put("eventData", eventData);
        
        step.setInputData(inputData);
        return step;
    }
    
    private TestStep createDatabaseStep(String stepId, String bucket, String collection, String docId, String operation) {
        TestStep step = new TestStep(stepId, StepType.DATABASE_CHECK, "couchbase-service", 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("bucket", bucket);
        inputData.put("collection", collection);
        inputData.put("documentId", docId);
        inputData.put("operation", operation);
        
        step.setInputData(inputData);
        return step;
    }
    
    private TestStep createAssertionStep(String stepId, String assertionType, Object expected, Object actual) {
        TestStep step = new TestStep(stepId, StepType.ASSERTION, "assertion-service", 30000L);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("assertionType", assertionType);
        inputData.put("expectedValue", expected);
        inputData.put("actualValue", actual);
        
        step.setInputData(inputData);
        return step;
    }
}