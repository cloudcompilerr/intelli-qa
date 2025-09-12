package com.agentic.e2etester.service;

import com.agentic.e2etester.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the complete test memory and context management system.
 * Tests the interaction between TestMemoryService and CorrelationTrackingService.
 */
@ExtendWith(MockitoExtension.class)
class TestMemoryIntegrationTest {
    
    @Mock
    private VectorStore vectorStore;
    
    private ObjectMapper objectMapper;
    private DefaultTestMemoryService testMemoryService;
    private DefaultCorrelationTrackingService correlationTrackingService;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        testMemoryService = new DefaultTestMemoryService(vectorStore, objectMapper);
        correlationTrackingService = new DefaultCorrelationTrackingService(testMemoryService);
    }
    
    @Test
    void testCompleteOrderFulfillmentScenario() {
        // Simulate a complete order fulfillment test scenario
        String correlationId = correlationTrackingService.generateCorrelationId();
        
        // 1. Start the trace
        CorrelationTrace trace = correlationTrackingService.startTrace(correlationId, "order-service");
        assertNotNull(trace);
        assertEquals(correlationId, trace.getCorrelationId());
        
        // 2. Create test context
        TestContext context = new TestContext(correlationId);
        context.setTestExecutionPlanId("plan-order-fulfillment");
        context.setInteractions(new ArrayList<>());
        context.setEvents(new ArrayList<>());
        
        // 3. Simulate service interactions with spans
        List<String> services = Arrays.asList("order-service", "payment-service", "inventory-service", "shipping-service");
        List<TraceSpan> spans = new ArrayList<>();
        
        for (String service : services) {
            TraceSpan span = correlationTrackingService.startSpan(correlationId, service, "process-order");
            spans.add(span);
            
            // Add some tags and logs
            correlationTrackingService.addSpanTag(correlationId, span.getSpanId(), "service.version", "1.0");
            correlationTrackingService.addSpanLog(correlationId, span.getSpanId(), "processing.start", Instant.now());
            
            // Create service interaction
            ServiceInteraction interaction = new ServiceInteraction();
            interaction.setServiceId(service);
            interaction.setType(InteractionType.HTTP_REQUEST);
            interaction.setStatus(InteractionStatus.SUCCESS);
            interaction.setTimestamp(Instant.now());
            interaction.setResponseTime(java.time.Duration.ofMillis(100 + (int)(Math.random() * 400)));
            context.addInteraction(interaction);
            
            // Finish span successfully
            correlationTrackingService.finishSpan(correlationId, span.getSpanId());
        }
        
        // 4. Complete the trace
        Optional<CorrelationTrace> completedTrace = correlationTrackingService.completeTrace(correlationId);
        assertTrue(completedTrace.isPresent());
        assertEquals(TraceStatus.COMPLETED, completedTrace.get().getStatus());
        
        // 5. Store execution history
        TestExecutionHistory history = new TestExecutionHistory(
                "exec-" + UUID.randomUUID(), 
                context.getTestExecutionPlanId(), 
                correlationId
        );
        history.setStatus(TestStatus.PASSED);
        history.setServicesInvolved(services);
        history.setInteractions(context.getInteractions());
        history.setEndTime(Instant.now());
        
        testMemoryService.storeExecutionHistory(history);
        
        // 6. Create and store a test pattern based on this successful execution
        TestPattern pattern = new TestPattern(
                "pattern-order-fulfillment", 
                "Standard Order Fulfillment Pattern", 
                PatternType.SUCCESS_FLOW
        );
        pattern.setDescription("Standard order fulfillment flow through all services");
        pattern.setServiceFlow(services);
        pattern.setTags(Arrays.asList("order", "fulfillment", "e2e"));
        pattern.incrementUsage();
        pattern.updateSuccessRate(true);
        pattern.setAverageExecutionTimeMs(history.getExecutionTimeMs());
        
        testMemoryService.storeTestPattern(pattern);
        
        // 7. Verify pattern can be found for similar contexts
        Document mockDoc = new Document(pattern.getPatternId(), "test content", Map.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(Arrays.asList(mockDoc));
        
        List<TestPattern> similarPatterns = testMemoryService.findSimilarPatterns(context, 5);
        assertEquals(1, similarPatterns.size());
        assertEquals(pattern, similarPatterns.get(0));
        
        // 8. Verify execution history can be retrieved
        List<TestExecutionHistory> historyByCorrelation = 
                testMemoryService.findExecutionHistoryByCorrelationId(correlationId);
        assertEquals(1, historyByCorrelation.size());
        assertEquals(history, historyByCorrelation.get(0));
        
        // 9. Verify trace can be retrieved
        Optional<CorrelationTrace> retrievedTrace = testMemoryService.findCorrelationTrace(correlationId);
        assertTrue(retrievedTrace.isPresent());
        assertEquals(correlationId, retrievedTrace.get().getCorrelationId());
    }
    
    @Test
    void testFailureScenarioWithPatternLearning() {
        String correlationId = correlationTrackingService.generateCorrelationId();
        
        // Start trace
        CorrelationTrace trace = correlationTrackingService.startTrace(correlationId, "order-service");
        
        // Simulate partial success then failure
        TraceSpan orderSpan = correlationTrackingService.startSpan(correlationId, "order-service", "create-order");
        correlationTrackingService.finishSpan(correlationId, orderSpan.getSpanId());
        
        TraceSpan paymentSpan = correlationTrackingService.startSpan(correlationId, "payment-service", "process-payment");
        correlationTrackingService.finishSpanWithError(correlationId, paymentSpan.getSpanId(), "Payment gateway timeout");
        
        // Fail the entire trace
        String errorDetails = "Payment processing failed";
        Optional<CorrelationTrace> failedTrace = correlationTrackingService.failTrace(correlationId, errorDetails);
        assertTrue(failedTrace.isPresent());
        assertEquals(TraceStatus.FAILED, failedTrace.get().getStatus());
        
        // Store failure execution history
        TestExecutionHistory failureHistory = new TestExecutionHistory(
                "exec-failure-" + UUID.randomUUID(), 
                "plan-order-fulfillment", 
                correlationId
        );
        failureHistory.setStatus(TestStatus.FAILED);
        failureHistory.setServicesInvolved(Arrays.asList("order-service", "payment-service"));
        failureHistory.setEndTime(Instant.now());
        
        TestFailure testFailure = new TestFailure();
        testFailure.setFailureType(FailureType.SERVICE_FAILURE);
        testFailure.setSeverity(FailureSeverity.HIGH);
        testFailure.setErrorMessage("Payment gateway timeout");
        testFailure.setServiceId("payment-service");
        failureHistory.setFailures(Arrays.asList(testFailure));
        
        testMemoryService.storeExecutionHistory(failureHistory);
        
        // Create failure pattern
        TestPattern failurePattern = new TestPattern(
                "pattern-payment-timeout", 
                "Payment Gateway Timeout Pattern", 
                PatternType.FAILURE_PATTERN
        );
        failurePattern.setDescription("Common failure pattern when payment gateway times out");
        failurePattern.setServiceFlow(Arrays.asList("order-service", "payment-service"));
        failurePattern.setFailurePatterns(Arrays.asList("timeout", "payment_gateway_error"));
        failurePattern.setTags(Arrays.asList("payment", "timeout", "failure"));
        failurePattern.incrementUsage();
        failurePattern.updateSuccessRate(false);
        
        testMemoryService.storeTestPattern(failurePattern);
        
        // Verify failure patterns can be found by type
        List<TestPattern> failurePatterns = testMemoryService.findPatternsByType(PatternType.FAILURE_PATTERN, 10);
        assertEquals(1, failurePatterns.size());
        assertEquals(failurePattern, failurePatterns.get(0));
    }
    
    @Test
    void testPatternExtractionFromMultipleExecutions() {
        // Create multiple similar executions to trigger pattern extraction
        List<String> commonServiceFlow = Arrays.asList("user-service", "catalog-service", "recommendation-service");
        
        for (int i = 0; i < 5; i++) {
            String correlationId = "corr-pattern-test-" + i;
            
            TestExecutionHistory history = new TestExecutionHistory(
                    "exec-pattern-" + i, 
                    "plan-recommendation", 
                    correlationId
            );
            history.setStatus(i < 4 ? TestStatus.PASSED : TestStatus.FAILED); // 4 successes, 1 failure
            history.setServicesInvolved(commonServiceFlow);
            history.setExecutionTimeMs(800L + (i * 50)); // Varying execution times
            history.setEnvironment("test");
            
            testMemoryService.storeExecutionHistory(history);
        }
        
        // Analyze and extract patterns
        List<TestPattern> extractedPatterns = testMemoryService.analyzeAndExtractPatterns(10);
        
        assertEquals(1, extractedPatterns.size());
        TestPattern extractedPattern = extractedPatterns.get(0);
        
        assertEquals(PatternType.SUCCESS_FLOW, extractedPattern.getType());
        assertEquals(commonServiceFlow, extractedPattern.getServiceFlow());
        assertEquals(0.8, extractedPattern.getSuccessRate(), 0.001); // 4/5 success rate
        assertEquals(5, extractedPattern.getUsageCount());
        assertTrue(extractedPattern.getName().contains("user-service->catalog-service->recommendation-service"));
        
        // Verify the pattern was stored
        List<TestPattern> successPatterns = testMemoryService.findPatternsByType(PatternType.SUCCESS_FLOW, 10);
        assertTrue(successPatterns.contains(extractedPattern));
    }
    
    @Test
    void testCorrelationTrackingWithNestedSpans() {
        String correlationId = correlationTrackingService.generateCorrelationId();
        
        // Start trace
        correlationTrackingService.startTrace(correlationId, "api-gateway");
        
        // Create parent span
        TraceSpan parentSpan = correlationTrackingService.startSpan(correlationId, "api-gateway", "handle-request");
        correlationTrackingService.addSpanTag(correlationId, parentSpan.getSpanId(), "http.method", "POST");
        correlationTrackingService.addSpanTag(correlationId, parentSpan.getSpanId(), "http.url", "/api/orders");
        
        // Create child spans
        TraceSpan authSpan = correlationTrackingService.startChildSpan(
                correlationId, parentSpan.getSpanId(), "auth-service", "validate-token");
        correlationTrackingService.addSpanLog(correlationId, authSpan.getSpanId(), "token.type", "JWT");
        correlationTrackingService.finishSpan(correlationId, authSpan.getSpanId());
        
        TraceSpan businessSpan = correlationTrackingService.startChildSpan(
                correlationId, parentSpan.getSpanId(), "order-service", "create-order");
        correlationTrackingService.addSpanLog(correlationId, businessSpan.getSpanId(), "order.id", "ORD-12345");
        correlationTrackingService.finishSpan(correlationId, businessSpan.getSpanId());
        
        // Finish parent span
        correlationTrackingService.finishSpan(correlationId, parentSpan.getSpanId());
        
        // Complete trace
        Optional<CorrelationTrace> completedTrace = correlationTrackingService.completeTrace(correlationId);
        assertTrue(completedTrace.isPresent());
        
        // Verify span hierarchy
        assertEquals(parentSpan.getSpanId(), authSpan.getParentSpanId());
        assertEquals(parentSpan.getSpanId(), businessSpan.getParentSpanId());
        assertNull(parentSpan.getParentSpanId());
        
        // Verify all spans are finished
        assertEquals(SpanStatus.FINISHED, parentSpan.getStatus());
        assertEquals(SpanStatus.FINISHED, authSpan.getStatus());
        assertEquals(SpanStatus.FINISHED, businessSpan.getStatus());
    }
    
    @Test
    void testDataRetentionAndCleanup() {
        // Create old data
        String oldCorrelationId = "old-corr-123";
        TestExecutionHistory oldHistory = new TestExecutionHistory("old-exec", "old-plan", oldCorrelationId);
        oldHistory.setStartTime(Instant.now().minusSeconds(86400 * 10)); // 10 days ago
        testMemoryService.storeExecutionHistory(oldHistory);
        
        CorrelationTrace oldTrace = new CorrelationTrace(oldCorrelationId, "old-service");
        oldTrace.setStartTime(Instant.now().minusSeconds(86400 * 10)); // 10 days ago
        testMemoryService.storeCorrelationTrace(oldTrace);
        
        // Create recent data
        String recentCorrelationId = "recent-corr-123";
        TestExecutionHistory recentHistory = new TestExecutionHistory("recent-exec", "recent-plan", recentCorrelationId);
        testMemoryService.storeExecutionHistory(recentHistory);
        
        CorrelationTrace recentTrace = new CorrelationTrace(recentCorrelationId, "recent-service");
        testMemoryService.storeCorrelationTrace(recentTrace);
        
        // Verify data exists before cleanup
        assertEquals(2, testMemoryService.findRecentExecutionHistory(10).size());
        assertTrue(testMemoryService.findCorrelationTrace(oldCorrelationId).isPresent());
        assertTrue(testMemoryService.findCorrelationTrace(recentCorrelationId).isPresent());
        
        // Cleanup old data (7 days retention)
        int cleanedCount = testMemoryService.cleanupOldData(7);
        assertEquals(2, cleanedCount); // Should clean up old history and old trace
        
        // Verify only recent data remains
        List<TestExecutionHistory> remainingHistory = testMemoryService.findRecentExecutionHistory(10);
        assertEquals(1, remainingHistory.size());
        assertEquals(recentHistory, remainingHistory.get(0));
        
        assertFalse(testMemoryService.findCorrelationTrace(oldCorrelationId).isPresent());
        assertTrue(testMemoryService.findCorrelationTrace(recentCorrelationId).isPresent());
    }
}