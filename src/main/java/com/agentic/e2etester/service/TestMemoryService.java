package com.agentic.e2etester.service;

import com.agentic.e2etester.model.*;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing test memory, patterns, and execution history.
 * Provides capabilities for storing, retrieving, and learning from test patterns.
 */
public interface TestMemoryService {
    
    /**
     * Stores a test pattern in the vector store for future retrieval.
     *
     * @param pattern the test pattern to store
     * @return the stored pattern with updated metadata
     */
    TestPattern storeTestPattern(TestPattern pattern);
    
    /**
     * Retrieves similar test patterns based on the current test context.
     *
     * @param context the current test context
     * @param limit maximum number of patterns to return
     * @return list of similar test patterns
     */
    List<TestPattern> findSimilarPatterns(TestContext context, int limit);
    
    /**
     * Retrieves test patterns by type.
     *
     * @param type the pattern type to search for
     * @param limit maximum number of patterns to return
     * @return list of patterns of the specified type
     */
    List<TestPattern> findPatternsByType(PatternType type, int limit);
    
    /**
     * Updates an existing test pattern with new usage statistics.
     *
     * @param patternId the ID of the pattern to update
     * @param success whether the pattern usage was successful
     * @param executionTimeMs the execution time for this usage
     * @return the updated pattern
     */
    Optional<TestPattern> updatePatternUsage(String patternId, boolean success, long executionTimeMs);
    
    /**
     * Stores test execution history for pattern learning.
     *
     * @param history the test execution history to store
     * @return the stored history record
     */
    TestExecutionHistory storeExecutionHistory(TestExecutionHistory history);
    
    /**
     * Retrieves test execution history by correlation ID.
     *
     * @param correlationId the correlation ID to search for
     * @return list of execution history records
     */
    List<TestExecutionHistory> findExecutionHistoryByCorrelationId(String correlationId);
    
    /**
     * Retrieves recent test execution history.
     *
     * @param limit maximum number of records to return
     * @return list of recent execution history records
     */
    List<TestExecutionHistory> findRecentExecutionHistory(int limit);
    
    /**
     * Analyzes execution history to identify new patterns.
     *
     * @param limit maximum number of recent executions to analyze
     * @return list of newly identified patterns
     */
    List<TestPattern> analyzeAndExtractPatterns(int limit);
    
    /**
     * Stores a correlation trace for distributed tracing.
     *
     * @param trace the correlation trace to store
     * @return the stored trace
     */
    CorrelationTrace storeCorrelationTrace(CorrelationTrace trace);
    
    /**
     * Retrieves a correlation trace by correlation ID.
     *
     * @param correlationId the correlation ID to search for
     * @return the correlation trace if found
     */
    Optional<CorrelationTrace> findCorrelationTrace(String correlationId);
    
    /**
     * Updates an existing correlation trace with new spans.
     *
     * @param correlationId the correlation ID of the trace to update
     * @param span the new span to add
     * @return the updated trace if found
     */
    Optional<CorrelationTrace> addSpanToTrace(String correlationId, TraceSpan span);
    
    /**
     * Completes a correlation trace.
     *
     * @param correlationId the correlation ID of the trace to complete
     * @return the completed trace if found
     */
    Optional<CorrelationTrace> completeTrace(String correlationId);
    
    /**
     * Marks a correlation trace as failed.
     *
     * @param correlationId the correlation ID of the trace to fail
     * @param errorDetails the error details
     * @return the failed trace if found
     */
    Optional<CorrelationTrace> failTrace(String correlationId, String errorDetails);
    
    /**
     * Cleans up old test data based on retention policies.
     *
     * @param retentionDays number of days to retain data
     * @return number of records cleaned up
     */
    int cleanupOldData(int retentionDays);
}