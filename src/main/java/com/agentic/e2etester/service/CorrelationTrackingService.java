package com.agentic.e2etester.service;

import com.agentic.e2etester.model.CorrelationTrace;
import com.agentic.e2etester.model.TraceSpan;

import java.util.Optional;

/**
 * Service interface for tracking correlation IDs across distributed service calls.
 * Provides capabilities for creating, updating, and managing correlation traces.
 */
public interface CorrelationTrackingService {
    
    /**
     * Starts a new correlation trace for a test execution.
     *
     * @param correlationId the correlation ID for the trace
     * @param rootService the root service that initiated the trace
     * @return the created correlation trace
     */
    CorrelationTrace startTrace(String correlationId, String rootService);
    
    /**
     * Starts a new span within an existing trace.
     *
     * @param correlationId the correlation ID of the parent trace
     * @param serviceName the name of the service executing the span
     * @param operationName the name of the operation being executed
     * @return the created trace span
     */
    TraceSpan startSpan(String correlationId, String serviceName, String operationName);
    
    /**
     * Starts a child span within an existing span.
     *
     * @param correlationId the correlation ID of the parent trace
     * @param parentSpanId the ID of the parent span
     * @param serviceName the name of the service executing the span
     * @param operationName the name of the operation being executed
     * @return the created trace span
     */
    TraceSpan startChildSpan(String correlationId, String parentSpanId, String serviceName, String operationName);
    
    /**
     * Finishes a span successfully.
     *
     * @param correlationId the correlation ID of the trace
     * @param spanId the ID of the span to finish
     * @return true if the span was found and finished
     */
    boolean finishSpan(String correlationId, String spanId);
    
    /**
     * Finishes a span with an error.
     *
     * @param correlationId the correlation ID of the trace
     * @param spanId the ID of the span to finish
     * @param errorMessage the error message
     * @return true if the span was found and finished
     */
    boolean finishSpanWithError(String correlationId, String spanId, String errorMessage);
    
    /**
     * Adds a tag to a span.
     *
     * @param correlationId the correlation ID of the trace
     * @param spanId the ID of the span
     * @param key the tag key
     * @param value the tag value
     * @return true if the span was found and tag was added
     */
    boolean addSpanTag(String correlationId, String spanId, String key, String value);
    
    /**
     * Adds a log entry to a span.
     *
     * @param correlationId the correlation ID of the trace
     * @param spanId the ID of the span
     * @param key the log key
     * @param value the log value
     * @return true if the span was found and log was added
     */
    boolean addSpanLog(String correlationId, String spanId, String key, Object value);
    
    /**
     * Completes a correlation trace successfully.
     *
     * @param correlationId the correlation ID of the trace to complete
     * @return the completed trace if found
     */
    Optional<CorrelationTrace> completeTrace(String correlationId);
    
    /**
     * Fails a correlation trace with an error.
     *
     * @param correlationId the correlation ID of the trace to fail
     * @param errorDetails the error details
     * @return the failed trace if found
     */
    Optional<CorrelationTrace> failTrace(String correlationId, String errorDetails);
    
    /**
     * Retrieves the current trace for a correlation ID.
     *
     * @param correlationId the correlation ID to search for
     * @return the correlation trace if found
     */
    Optional<CorrelationTrace> getTrace(String correlationId);
    
    /**
     * Generates a new unique correlation ID.
     *
     * @return a new correlation ID
     */
    String generateCorrelationId();
    
    /**
     * Generates a new unique span ID.
     *
     * @return a new span ID
     */
    String generateSpanId();
}