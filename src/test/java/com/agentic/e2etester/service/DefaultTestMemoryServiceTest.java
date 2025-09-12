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
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTestMemoryServiceTest {
    
    @Mock
    private VectorStore vectorStore;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private DefaultTestMemoryService testMemoryService;
    
    @BeforeEach
    void setUp() {
        testMemoryService = new DefaultTestMemoryService(vectorStore, objectMapper);
    }
    
    @Test
    void testStoreTestPattern() {
        TestPattern pattern = new TestPattern("pattern-123", "Test Pattern", PatternType.SUCCESS_FLOW);
        pattern.setDescription("Test description");
        pattern.setServiceFlow(Arrays.asList("service1", "service2"));
        
        TestPattern result = testMemoryService.storeTestPattern(pattern);
        
        assertEquals(pattern, result);
        verify(vectorStore).add(anyList());
    }
    
    @Test
    void testStoreTestPatternWithException() {
        TestPattern pattern = new TestPattern("pattern-123", "Test Pattern", PatternType.SUCCESS_FLOW);
        
        doThrow(new RuntimeException("Vector store error")).when(vectorStore).add(anyList());
        
        assertThrows(RuntimeException.class, () -> testMemoryService.storeTestPattern(pattern));
    }
    
    @Test
    void testFindSimilarPatterns() {
        TestContext context = new TestContext("corr-123");
        context.setInteractions(Arrays.asList(
                createServiceInteraction("service1"),
                createServiceInteraction("service2")
        ));
        
        TestPattern pattern = new TestPattern("pattern-123", "Test Pattern", PatternType.SUCCESS_FLOW);
        testMemoryService.storeTestPattern(pattern);
        
        Document doc = new Document("pattern-123", "test content", Map.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(Arrays.asList(doc));
        
        List<TestPattern> results = testMemoryService.findSimilarPatterns(context, 5);
        
        assertEquals(1, results.size());
        assertEquals(pattern, results.get(0));
    }
    
    @Test
    void testFindSimilarPatternsWithException() {
        TestContext context = new TestContext("corr-123");
        
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Search error"));
        
        List<TestPattern> results = testMemoryService.findSimilarPatterns(context, 5);
        
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testFindPatternsByType() {
        TestPattern pattern1 = new TestPattern("pattern-1", "Pattern 1", PatternType.SUCCESS_FLOW);
        pattern1.setSuccessRate(0.9);
        TestPattern pattern2 = new TestPattern("pattern-2", "Pattern 2", PatternType.SUCCESS_FLOW);
        pattern2.setSuccessRate(0.8);
        TestPattern pattern3 = new TestPattern("pattern-3", "Pattern 3", PatternType.FAILURE_PATTERN);
        pattern3.setSuccessRate(0.7);
        
        testMemoryService.storeTestPattern(pattern1);
        testMemoryService.storeTestPattern(pattern2);
        testMemoryService.storeTestPattern(pattern3);
        
        List<TestPattern> results = testMemoryService.findPatternsByType(PatternType.SUCCESS_FLOW, 10);
        
        assertEquals(2, results.size());
        assertEquals(pattern1, results.get(0)); // Higher success rate first
        assertEquals(pattern2, results.get(1));
    }
    
    @Test
    void testUpdatePatternUsage() {
        TestPattern pattern = new TestPattern("pattern-123", "Test Pattern", PatternType.SUCCESS_FLOW);
        testMemoryService.storeTestPattern(pattern);
        
        Optional<TestPattern> result = testMemoryService.updatePatternUsage("pattern-123", true, 1000L);
        
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getUsageCount());
        assertEquals(1.0, result.get().getSuccessRate());
        assertEquals(1000L, result.get().getAverageExecutionTimeMs());
        assertNotNull(result.get().getLastUsed());
    }
    
    @Test
    void testUpdatePatternUsageNotFound() {
        Optional<TestPattern> result = testMemoryService.updatePatternUsage("nonexistent", true, 1000L);
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void testUpdatePatternUsageMultipleTimes() {
        TestPattern pattern = new TestPattern("pattern-123", "Test Pattern", PatternType.SUCCESS_FLOW);
        testMemoryService.storeTestPattern(pattern);
        
        // First usage: success, 1000ms
        testMemoryService.updatePatternUsage("pattern-123", true, 1000L);
        
        // Second usage: failure, 2000ms
        Optional<TestPattern> result = testMemoryService.updatePatternUsage("pattern-123", false, 2000L);
        
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getUsageCount());
        assertEquals(0.5, result.get().getSuccessRate(), 0.001);
        assertEquals(1500L, result.get().getAverageExecutionTimeMs()); // (1000 + 2000) / 2
    }
    
    @Test
    void testStoreExecutionHistory() {
        TestExecutionHistory history = new TestExecutionHistory("exec-123", "plan-123", "corr-123");
        history.setStatus(TestStatus.PASSED);
        
        TestExecutionHistory result = testMemoryService.storeExecutionHistory(history);
        
        assertEquals(history, result);
    }
    
    @Test
    void testFindExecutionHistoryByCorrelationId() {
        TestExecutionHistory history1 = new TestExecutionHistory("exec-1", "plan-1", "corr-123");
        TestExecutionHistory history2 = new TestExecutionHistory("exec-2", "plan-2", "corr-123");
        TestExecutionHistory history3 = new TestExecutionHistory("exec-3", "plan-3", "corr-456");
        
        testMemoryService.storeExecutionHistory(history1);
        testMemoryService.storeExecutionHistory(history2);
        testMemoryService.storeExecutionHistory(history3);
        
        List<TestExecutionHistory> results = testMemoryService.findExecutionHistoryByCorrelationId("corr-123");
        
        assertEquals(2, results.size());
        assertTrue(results.contains(history1));
        assertTrue(results.contains(history2));
    }
    
    @Test
    void testFindRecentExecutionHistory() {
        TestExecutionHistory history1 = new TestExecutionHistory("exec-1", "plan-1", "corr-1");
        history1.setStartTime(Instant.now().minusSeconds(300));
        
        TestExecutionHistory history2 = new TestExecutionHistory("exec-2", "plan-2", "corr-2");
        history2.setStartTime(Instant.now().minusSeconds(200));
        
        TestExecutionHistory history3 = new TestExecutionHistory("exec-3", "plan-3", "corr-3");
        history3.setStartTime(Instant.now().minusSeconds(100));
        
        testMemoryService.storeExecutionHistory(history1);
        testMemoryService.storeExecutionHistory(history2);
        testMemoryService.storeExecutionHistory(history3);
        
        List<TestExecutionHistory> results = testMemoryService.findRecentExecutionHistory(2);
        
        assertEquals(2, results.size());
        assertEquals(history3, results.get(0)); // Most recent first
        assertEquals(history2, results.get(1));
    }
    
    @Test
    void testAnalyzeAndExtractPatterns() {
        // Create multiple executions with same service flow
        List<String> serviceFlow = Arrays.asList("service1", "service2", "service3");
        
        for (int i = 0; i < 5; i++) {
            TestExecutionHistory history = new TestExecutionHistory("exec-" + i, "plan-" + i, "corr-" + i);
            history.setServicesInvolved(serviceFlow);
            history.setStatus(i < 4 ? TestStatus.PASSED : TestStatus.FAILED); // 4 successes, 1 failure
            history.setExecutionTimeMs(1000L + (i * 100)); // Varying execution times
            testMemoryService.storeExecutionHistory(history);
        }
        
        List<TestPattern> patterns = testMemoryService.analyzeAndExtractPatterns(10);
        
        assertEquals(1, patterns.size());
        TestPattern pattern = patterns.get(0);
        assertEquals(PatternType.SUCCESS_FLOW, pattern.getType());
        assertEquals(serviceFlow, pattern.getServiceFlow());
        assertEquals(0.8, pattern.getSuccessRate(), 0.001); // 4/5 success rate
        assertEquals(5, pattern.getUsageCount());
    }
    
    @Test
    void testStoreCorrelationTrace() {
        CorrelationTrace trace = new CorrelationTrace("corr-123", "root-service");
        
        CorrelationTrace result = testMemoryService.storeCorrelationTrace(trace);
        
        assertEquals(trace, result);
    }
    
    @Test
    void testFindCorrelationTrace() {
        CorrelationTrace trace = new CorrelationTrace("corr-123", "root-service");
        testMemoryService.storeCorrelationTrace(trace);
        
        Optional<CorrelationTrace> result = testMemoryService.findCorrelationTrace("corr-123");
        
        assertTrue(result.isPresent());
        assertEquals(trace, result.get());
    }
    
    @Test
    void testAddSpanToTrace() {
        CorrelationTrace trace = new CorrelationTrace("corr-123", "root-service");
        trace.setTraceSpans(new ArrayList<>());
        testMemoryService.storeCorrelationTrace(trace);
        
        TraceSpan span = new TraceSpan("span-1", "service1", "operation1");
        
        Optional<CorrelationTrace> result = testMemoryService.addSpanToTrace("corr-123", span);
        
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getTraceSpans().size());
        assertEquals(span, result.get().getTraceSpans().get(0));
    }
    
    @Test
    void testCompleteTrace() {
        CorrelationTrace trace = new CorrelationTrace("corr-123", "root-service");
        testMemoryService.storeCorrelationTrace(trace);
        
        Optional<CorrelationTrace> result = testMemoryService.completeTrace("corr-123");
        
        assertTrue(result.isPresent());
        assertEquals(TraceStatus.COMPLETED, result.get().getStatus());
        assertNotNull(result.get().getEndTime());
    }
    
    @Test
    void testFailTrace() {
        CorrelationTrace trace = new CorrelationTrace("corr-123", "root-service");
        testMemoryService.storeCorrelationTrace(trace);
        
        String errorDetails = "Service timeout";
        Optional<CorrelationTrace> result = testMemoryService.failTrace("corr-123", errorDetails);
        
        assertTrue(result.isPresent());
        assertEquals(TraceStatus.FAILED, result.get().getStatus());
        assertEquals(errorDetails, result.get().getErrorDetails());
        assertNotNull(result.get().getEndTime());
    }
    
    @Test
    void testCleanupOldData() {
        // Add old execution history
        TestExecutionHistory oldHistory = new TestExecutionHistory("old-exec", "old-plan", "old-corr");
        oldHistory.setStartTime(Instant.now().minus(10, ChronoUnit.DAYS));
        testMemoryService.storeExecutionHistory(oldHistory);
        
        // Add recent execution history
        TestExecutionHistory recentHistory = new TestExecutionHistory("recent-exec", "recent-plan", "recent-corr");
        testMemoryService.storeExecutionHistory(recentHistory);
        
        // Add old correlation trace
        CorrelationTrace oldTrace = new CorrelationTrace("old-corr", "old-service");
        oldTrace.setStartTime(Instant.now().minus(10, ChronoUnit.DAYS));
        testMemoryService.storeCorrelationTrace(oldTrace);
        
        // Add recent correlation trace
        CorrelationTrace recentTrace = new CorrelationTrace("recent-corr", "recent-service");
        testMemoryService.storeCorrelationTrace(recentTrace);
        
        int cleanedCount = testMemoryService.cleanupOldData(7); // 7 days retention
        
        assertEquals(2, cleanedCount); // Should clean up old history and old trace
        
        // Verify recent data is still there
        List<TestExecutionHistory> remainingHistory = testMemoryService.findRecentExecutionHistory(10);
        assertEquals(1, remainingHistory.size());
        assertEquals(recentHistory, remainingHistory.get(0));
        
        Optional<CorrelationTrace> remainingTrace = testMemoryService.findCorrelationTrace("recent-corr");
        assertTrue(remainingTrace.isPresent());
        assertEquals(recentTrace, remainingTrace.get());
    }
    
    private ServiceInteraction createServiceInteraction(String serviceId) {
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId(serviceId);
        interaction.setType(InteractionType.HTTP_REQUEST);
        interaction.setStatus(InteractionStatus.SUCCESS);
        interaction.setTimestamp(Instant.now());
        return interaction;
    }
}