package com.agentic.e2etester.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationTraceTest {
    
    private CorrelationTrace correlationTrace;
    
    @BeforeEach
    void setUp() {
        correlationTrace = new CorrelationTrace("corr-123", "order-service");
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        assertEquals("corr-123", correlationTrace.getCorrelationId());
        assertEquals("order-service", correlationTrace.getRootService());
        assertEquals(TraceStatus.ACTIVE, correlationTrace.getStatus());
        assertNotNull(correlationTrace.getStartTime());
        assertNull(correlationTrace.getEndTime());
        assertEquals(0L, correlationTrace.getTotalDurationMs());
    }
    
    @Test
    void testDefaultConstructor() {
        CorrelationTrace trace = new CorrelationTrace();
        assertEquals(TraceStatus.ACTIVE, trace.getStatus());
        assertNotNull(trace.getStartTime());
    }
    
    @Test
    void testSettersAndGetters() {
        correlationTrace.setTraceSpans(new ArrayList<>());
        correlationTrace.setErrorDetails("Test error");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("testId", "test-123");
        correlationTrace.setMetadata(metadata);
        
        assertNotNull(correlationTrace.getTraceSpans());
        assertEquals("Test error", correlationTrace.getErrorDetails());
        assertEquals(metadata, correlationTrace.getMetadata());
    }
    
    @Test
    void testAddSpan() {
        correlationTrace.setTraceSpans(new ArrayList<>());
        
        TraceSpan span = new TraceSpan("span-1", "payment-service", "process-payment");
        correlationTrace.addSpan(span);
        
        assertEquals(1, correlationTrace.getTraceSpans().size());
        assertEquals(span, correlationTrace.getTraceSpans().get(0));
    }
    
    @Test
    void testCompleteTrace() {
        Instant beforeComplete = Instant.now();
        correlationTrace.completeTrace();
        Instant afterComplete = Instant.now();
        
        assertEquals(TraceStatus.COMPLETED, correlationTrace.getStatus());
        assertNotNull(correlationTrace.getEndTime());
        assertTrue(correlationTrace.getEndTime().isAfter(beforeComplete) || 
                  correlationTrace.getEndTime().equals(beforeComplete));
        assertTrue(correlationTrace.getEndTime().isBefore(afterComplete) || 
                  correlationTrace.getEndTime().equals(afterComplete));
        assertTrue(correlationTrace.getTotalDurationMs() >= 0);
    }
    
    @Test
    void testFailTrace() {
        String errorDetails = "Service timeout occurred";
        
        Instant beforeFail = Instant.now();
        correlationTrace.failTrace(errorDetails);
        Instant afterFail = Instant.now();
        
        assertEquals(TraceStatus.FAILED, correlationTrace.getStatus());
        assertEquals(errorDetails, correlationTrace.getErrorDetails());
        assertNotNull(correlationTrace.getEndTime());
        assertTrue(correlationTrace.getEndTime().isAfter(beforeFail) || 
                  correlationTrace.getEndTime().equals(beforeFail));
        assertTrue(correlationTrace.getEndTime().isBefore(afterFail) || 
                  correlationTrace.getEndTime().equals(afterFail));
        assertTrue(correlationTrace.getTotalDurationMs() >= 0);
    }
    
    @Test
    void testSetEndTimeCalculatesDuration() {
        Instant startTime = Instant.now().minusSeconds(5);
        correlationTrace.setStartTime(startTime);
        
        Instant endTime = Instant.now();
        correlationTrace.setEndTime(endTime);
        
        long expectedDuration = endTime.toEpochMilli() - startTime.toEpochMilli();
        assertEquals(expectedDuration, correlationTrace.getTotalDurationMs());
    }
    
    @Test
    void testEqualsAndHashCode() {
        CorrelationTrace trace1 = new CorrelationTrace("corr-123", "service1");
        CorrelationTrace trace2 = new CorrelationTrace("corr-123", "service2");
        CorrelationTrace trace3 = new CorrelationTrace("corr-456", "service1");
        
        // Same correlation ID should be equal
        assertEquals(trace1, trace2);
        assertEquals(trace1.hashCode(), trace2.hashCode());
        
        // Different correlation ID should not be equal
        assertNotEquals(trace1, trace3);
        assertNotEquals(trace1.hashCode(), trace3.hashCode());
        
        // Null and different class should not be equal
        assertNotEquals(trace1, null);
        assertNotEquals(trace1, "string");
    }
    
    @Test
    void testToString() {
        String toString = correlationTrace.toString();
        assertTrue(toString.contains("corr-123"));
        assertTrue(toString.contains("order-service"));
        assertTrue(toString.contains("ACTIVE"));
        assertTrue(toString.contains("totalDurationMs=0"));
        assertTrue(toString.contains("spansCount=0"));
    }
    
    @Test
    void testAllTraceStatuses() {
        // Test all trace statuses can be set
        for (TraceStatus status : TraceStatus.values()) {
            correlationTrace.setStatus(status);
            assertEquals(status, correlationTrace.getStatus());
        }
    }
}