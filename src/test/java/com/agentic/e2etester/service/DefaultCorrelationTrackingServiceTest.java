package com.agentic.e2etester.service;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultCorrelationTrackingServiceTest {
    
    @Mock
    private TestMemoryService testMemoryService;
    
    private DefaultCorrelationTrackingService correlationTrackingService;
    
    @BeforeEach
    void setUp() {
        correlationTrackingService = new DefaultCorrelationTrackingService(testMemoryService);
    }
    
    @Test
    void testStartTrace() {
        String correlationId = "corr-123";
        String rootService = "order-service";
        
        when(testMemoryService.storeCorrelationTrace(any(CorrelationTrace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        CorrelationTrace result = correlationTrackingService.startTrace(correlationId, rootService);
        
        assertNotNull(result);
        assertEquals(correlationId, result.getCorrelationId());
        assertEquals(rootService, result.getRootService());
        assertEquals(TraceStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getTraceSpans());
        
        verify(testMemoryService).storeCorrelationTrace(any(CorrelationTrace.class));
    }
    
    @Test
    void testStartSpan() {
        String correlationId = "corr-123";
        String serviceName = "payment-service";
        String operationName = "process-payment";
        
        // Setup trace first
        correlationTrackingService.startTrace(correlationId, "root-service");
        
        when(testMemoryService.addSpanToTrace(eq(correlationId), any(TraceSpan.class)))
                .thenReturn(Optional.of(new CorrelationTrace()));
        
        TraceSpan result = correlationTrackingService.startSpan(correlationId, serviceName, operationName);
        
        assertNotNull(result);
        assertNotNull(result.getSpanId());
        assertEquals(serviceName, result.getServiceName());
        assertEquals(operationName, result.getOperationName());
        assertEquals(SpanStatus.ACTIVE, result.getStatus());
        assertNull(result.getParentSpanId());
        
        verify(testMemoryService).addSpanToTrace(eq(correlationId), any(TraceSpan.class));
    }
    
    @Test
    void testStartChildSpan() {
        String correlationId = "corr-123";
        String parentSpanId = "parent-span-123";
        String serviceName = "inventory-service";
        String operationName = "check-availability";
        
        // Setup trace first
        correlationTrackingService.startTrace(correlationId, "root-service");
        
        when(testMemoryService.addSpanToTrace(eq(correlationId), any(TraceSpan.class)))
                .thenReturn(Optional.of(new CorrelationTrace()));
        
        TraceSpan result = correlationTrackingService.startChildSpan(
                correlationId, parentSpanId, serviceName, operationName);
        
        assertNotNull(result);
        assertNotNull(result.getSpanId());
        assertEquals(parentSpanId, result.getParentSpanId());
        assertEquals(serviceName, result.getServiceName());
        assertEquals(operationName, result.getOperationName());
        assertEquals(SpanStatus.ACTIVE, result.getStatus());
        
        verify(testMemoryService).addSpanToTrace(eq(correlationId), any(TraceSpan.class));
    }
    
    @Test
    void testFinishSpan() {
        String correlationId = "corr-123";
        
        // Setup trace and span
        correlationTrackingService.startTrace(correlationId, "root-service");
        TraceSpan span = correlationTrackingService.startSpan(correlationId, "service", "operation");
        
        boolean result = correlationTrackingService.finishSpan(correlationId, span.getSpanId());
        
        assertTrue(result);
        assertEquals(SpanStatus.FINISHED, span.getStatus());
        assertNotNull(span.getEndTime());
    }
    
    @Test
    void testFinishSpanNotFound() {
        String correlationId = "corr-123";
        String nonExistentSpanId = "nonexistent-span";
        
        boolean result = correlationTrackingService.finishSpan(correlationId, nonExistentSpanId);
        
        assertFalse(result);
    }
    
    @Test
    void testFinishSpanWithError() {
        String correlationId = "corr-123";
        String errorMessage = "Service timeout occurred";
        
        // Setup trace and span
        correlationTrackingService.startTrace(correlationId, "root-service");
        TraceSpan span = correlationTrackingService.startSpan(correlationId, "service", "operation");
        
        boolean result = correlationTrackingService.finishSpanWithError(
                correlationId, span.getSpanId(), errorMessage);
        
        assertTrue(result);
        assertEquals(SpanStatus.ERROR, span.getStatus());
        assertEquals(errorMessage, span.getErrorMessage());
        assertNotNull(span.getEndTime());
    }
    
    @Test
    void testAddSpanTag() {
        String correlationId = "corr-123";
        String key = "http.method";
        String value = "POST";
        
        // Setup trace and span
        correlationTrackingService.startTrace(correlationId, "root-service");
        TraceSpan span = correlationTrackingService.startSpan(correlationId, "service", "operation");
        
        boolean result = correlationTrackingService.addSpanTag(correlationId, span.getSpanId(), key, value);
        
        assertTrue(result);
        assertEquals(value, span.getTags().get(key));
    }
    
    @Test
    void testAddSpanLog() {
        String correlationId = "corr-123";
        String key = "request.body";
        Object value = "{\"orderId\": \"123\"}";
        
        // Setup trace and span
        correlationTrackingService.startTrace(correlationId, "root-service");
        TraceSpan span = correlationTrackingService.startSpan(correlationId, "service", "operation");
        
        boolean result = correlationTrackingService.addSpanLog(correlationId, span.getSpanId(), key, value);
        
        assertTrue(result);
        assertEquals(value, span.getLogs().get(key));
    }
    
    @Test
    void testCompleteTrace() {
        String correlationId = "corr-123";
        
        // Setup trace with active spans
        correlationTrackingService.startTrace(correlationId, "root-service");
        TraceSpan span1 = correlationTrackingService.startSpan(correlationId, "service1", "operation1");
        TraceSpan span2 = correlationTrackingService.startSpan(correlationId, "service2", "operation2");
        
        CorrelationTrace completedTrace = new CorrelationTrace(correlationId, "root-service");
        completedTrace.setStatus(TraceStatus.COMPLETED);
        
        when(testMemoryService.completeTrace(correlationId))
                .thenReturn(Optional.of(completedTrace));
        
        Optional<CorrelationTrace> result = correlationTrackingService.completeTrace(correlationId);
        
        assertTrue(result.isPresent());
        assertEquals(TraceStatus.COMPLETED, result.get().getStatus());
        
        // Verify all active spans were finished
        assertEquals(SpanStatus.FINISHED, span1.getStatus());
        assertEquals(SpanStatus.FINISHED, span2.getStatus());
        
        verify(testMemoryService).completeTrace(correlationId);
    }
    
    @Test
    void testFailTrace() {
        String correlationId = "corr-123";
        String errorDetails = "Database connection failed";
        
        // Setup trace with active spans
        correlationTrackingService.startTrace(correlationId, "root-service");
        TraceSpan span1 = correlationTrackingService.startSpan(correlationId, "service1", "operation1");
        TraceSpan span2 = correlationTrackingService.startSpan(correlationId, "service2", "operation2");
        
        CorrelationTrace failedTrace = new CorrelationTrace(correlationId, "root-service");
        failedTrace.setStatus(TraceStatus.FAILED);
        failedTrace.setErrorDetails(errorDetails);
        
        when(testMemoryService.failTrace(correlationId, errorDetails))
                .thenReturn(Optional.of(failedTrace));
        
        Optional<CorrelationTrace> result = correlationTrackingService.failTrace(correlationId, errorDetails);
        
        assertTrue(result.isPresent());
        assertEquals(TraceStatus.FAILED, result.get().getStatus());
        assertEquals(errorDetails, result.get().getErrorDetails());
        
        // Verify all active spans were finished with error
        assertEquals(SpanStatus.ERROR, span1.getStatus());
        assertEquals(SpanStatus.ERROR, span2.getStatus());
        assertTrue(span1.getErrorMessage().contains(errorDetails));
        assertTrue(span2.getErrorMessage().contains(errorDetails));
        
        verify(testMemoryService).failTrace(correlationId, errorDetails);
    }
    
    @Test
    void testGetTrace() {
        String correlationId = "corr-123";
        CorrelationTrace trace = new CorrelationTrace(correlationId, "root-service");
        
        when(testMemoryService.findCorrelationTrace(correlationId))
                .thenReturn(Optional.of(trace));
        
        Optional<CorrelationTrace> result = correlationTrackingService.getTrace(correlationId);
        
        assertTrue(result.isPresent());
        assertEquals(trace, result.get());
        
        verify(testMemoryService).findCorrelationTrace(correlationId);
    }
    
    @Test
    void testGenerateCorrelationId() {
        String correlationId = correlationTrackingService.generateCorrelationId();
        
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("corr-"));
        assertTrue(correlationId.length() > 5);
    }
    
    @Test
    void testGenerateSpanId() {
        String spanId = correlationTrackingService.generateSpanId();
        
        assertNotNull(spanId);
        assertTrue(spanId.startsWith("span-"));
        assertTrue(spanId.length() > 5);
    }
    
    @Test
    void testGenerateUniqueIds() {
        // Test that generated IDs are unique
        java.util.Set<String> correlationIds = new java.util.HashSet<>();
        java.util.Set<String> spanIds = new java.util.HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            correlationIds.add(correlationTrackingService.generateCorrelationId());
            spanIds.add(correlationTrackingService.generateSpanId());
        }
        
        assertEquals(100, correlationIds.size());
        assertEquals(100, spanIds.size());
    }
}