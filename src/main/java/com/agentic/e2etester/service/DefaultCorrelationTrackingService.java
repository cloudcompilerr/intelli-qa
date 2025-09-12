package com.agentic.e2etester.service;

import com.agentic.e2etester.model.CorrelationTrace;
import com.agentic.e2etester.model.SpanStatus;
import com.agentic.e2etester.model.TraceSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of CorrelationTrackingService that manages correlation traces
 * and spans for distributed tracing across microservices.
 */
@Service
public class DefaultCorrelationTrackingService implements CorrelationTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCorrelationTrackingService.class);
    
    private final TestMemoryService testMemoryService;
    
    // In-memory cache for active traces and spans
    private final Map<String, Map<String, TraceSpan>> activeSpans = new ConcurrentHashMap<>();
    
    public DefaultCorrelationTrackingService(TestMemoryService testMemoryService) {
        this.testMemoryService = testMemoryService;
    }
    
    @Override
    public CorrelationTrace startTrace(String correlationId, String rootService) {
        logger.debug("Starting trace: {} for root service: {}", correlationId, rootService);
        
        CorrelationTrace trace = new CorrelationTrace(correlationId, rootService);
        trace.setTraceSpans(new ArrayList<>());
        
        // Initialize spans map for this trace
        activeSpans.put(correlationId, new ConcurrentHashMap<>());
        
        // Store the trace
        testMemoryService.storeCorrelationTrace(trace);
        
        logger.info("Started trace: {} for root service: {}", correlationId, rootService);
        return trace;
    }
    
    @Override
    public TraceSpan startSpan(String correlationId, String serviceName, String operationName) {
        return startChildSpan(correlationId, null, serviceName, operationName);
    }
    
    @Override
    public TraceSpan startChildSpan(String correlationId, String parentSpanId, String serviceName, String operationName) {
        logger.debug("Starting span for trace: {} - service: {} - operation: {}", 
                correlationId, serviceName, operationName);
        
        String spanId = generateSpanId();
        TraceSpan span = new TraceSpan(spanId, serviceName, operationName);
        span.setParentSpanId(parentSpanId);
        span.setTags(new HashMap<>());
        span.setLogs(new HashMap<>());
        
        // Add to active spans
        Map<String, TraceSpan> traceSpans = activeSpans.get(correlationId);
        if (traceSpans != null) {
            traceSpans.put(spanId, span);
        }
        
        // Add to the trace
        testMemoryService.addSpanToTrace(correlationId, span);
        
        logger.info("Started span: {} for trace: {} - service: {}", spanId, correlationId, serviceName);
        return span;
    }
    
    @Override
    public boolean finishSpan(String correlationId, String spanId) {
        logger.debug("Finishing span: {} for trace: {}", spanId, correlationId);
        
        Map<String, TraceSpan> traceSpans = activeSpans.get(correlationId);
        if (traceSpans != null) {
            TraceSpan span = traceSpans.get(spanId);
            if (span != null) {
                span.finish();
                traceSpans.remove(spanId);
                
                logger.info("Finished span: {} for trace: {}", spanId, correlationId);
                return true;
            }
        }
        
        logger.warn("Span not found: {} for trace: {}", spanId, correlationId);
        return false;
    }
    
    @Override
    public boolean finishSpanWithError(String correlationId, String spanId, String errorMessage) {
        logger.debug("Finishing span with error: {} for trace: {} - error: {}", 
                spanId, correlationId, errorMessage);
        
        Map<String, TraceSpan> traceSpans = activeSpans.get(correlationId);
        if (traceSpans != null) {
            TraceSpan span = traceSpans.get(spanId);
            if (span != null) {
                span.finishWithError(errorMessage);
                traceSpans.remove(spanId);
                
                logger.info("Finished span with error: {} for trace: {}", spanId, correlationId);
                return true;
            }
        }
        
        logger.warn("Span not found: {} for trace: {}", spanId, correlationId);
        return false;
    }
    
    @Override
    public boolean addSpanTag(String correlationId, String spanId, String key, String value) {
        logger.debug("Adding tag to span: {} for trace: {} - {}={}", 
                spanId, correlationId, key, value);
        
        Map<String, TraceSpan> traceSpans = activeSpans.get(correlationId);
        if (traceSpans != null) {
            TraceSpan span = traceSpans.get(spanId);
            if (span != null) {
                span.addTag(key, value);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean addSpanLog(String correlationId, String spanId, String key, Object value) {
        logger.debug("Adding log to span: {} for trace: {} - {}={}", 
                spanId, correlationId, key, value);
        
        Map<String, TraceSpan> traceSpans = activeSpans.get(correlationId);
        if (traceSpans != null) {
            TraceSpan span = traceSpans.get(spanId);
            if (span != null) {
                span.addLog(key, value);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public Optional<CorrelationTrace> completeTrace(String correlationId) {
        logger.debug("Completing trace: {}", correlationId);
        
        // Finish any remaining active spans
        Map<String, TraceSpan> traceSpans = activeSpans.get(correlationId);
        if (traceSpans != null) {
            for (TraceSpan span : traceSpans.values()) {
                if (span.getStatus() == SpanStatus.ACTIVE) {
                    span.finish();
                }
            }
            activeSpans.remove(correlationId);
        }
        
        Optional<CorrelationTrace> trace = testMemoryService.completeTrace(correlationId);
        if (trace.isPresent()) {
            logger.info("Completed trace: {}", correlationId);
        } else {
            logger.warn("Trace not found for completion: {}", correlationId);
        }
        
        return trace;
    }
    
    @Override
    public Optional<CorrelationTrace> failTrace(String correlationId, String errorDetails) {
        logger.debug("Failing trace: {} - error: {}", correlationId, errorDetails);
        
        // Finish any remaining active spans with error
        Map<String, TraceSpan> traceSpans = activeSpans.get(correlationId);
        if (traceSpans != null) {
            for (TraceSpan span : traceSpans.values()) {
                if (span.getStatus() == SpanStatus.ACTIVE) {
                    span.finishWithError("Trace failed: " + errorDetails);
                }
            }
            activeSpans.remove(correlationId);
        }
        
        Optional<CorrelationTrace> trace = testMemoryService.failTrace(correlationId, errorDetails);
        if (trace.isPresent()) {
            logger.info("Failed trace: {}", correlationId);
        } else {
            logger.warn("Trace not found for failure: {}", correlationId);
        }
        
        return trace;
    }
    
    @Override
    public Optional<CorrelationTrace> getTrace(String correlationId) {
        logger.debug("Getting trace: {}", correlationId);
        
        return testMemoryService.findCorrelationTrace(correlationId);
    }
    
    @Override
    public String generateCorrelationId() {
        return "corr-" + UUID.randomUUID().toString();
    }
    
    @Override
    public String generateSpanId() {
        return "span-" + UUID.randomUUID().toString();
    }
}