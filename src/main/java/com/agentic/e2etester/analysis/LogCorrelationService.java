package com.agentic.e2etester.analysis;

import com.agentic.e2etester.model.TestFailure;
import com.agentic.e2etester.model.TestContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for correlating logs across distributed services to identify failure patterns
 */
public interface LogCorrelationService {
    
    /**
     * Correlates logs from multiple services based on correlation ID and timestamp
     * 
     * @param correlationId The correlation ID to trace across services
     * @param timeWindow Time window in milliseconds to search for related logs
     * @return CompletableFuture containing correlated log entries
     */
    CompletableFuture<List<String>> correlateLogs(String correlationId, long timeWindow);
    
    /**
     * Extracts relevant log entries for a specific failure
     * 
     * @param failure The failure to extract logs for
     * @param context The test execution context
     * @return CompletableFuture containing relevant log entries
     */
    CompletableFuture<List<String>> extractRelevantLogs(TestFailure failure, TestContext context);
    
    /**
     * Identifies patterns in log entries that may indicate specific failure types
     * 
     * @param logEntries List of log entries to analyze
     * @return CompletableFuture containing identified patterns and their significance
     */
    CompletableFuture<Map<String, Double>> identifyLogPatterns(List<String> logEntries);
    
    /**
     * Searches for error patterns across service logs
     * 
     * @param services List of service IDs to search
     * @param errorPatterns List of error patterns to search for
     * @param timeRange Time range to search within
     * @return CompletableFuture containing found error occurrences
     */
    CompletableFuture<Map<String, List<String>>> searchErrorPatterns(
        List<String> services, List<String> errorPatterns, long timeRange);
}