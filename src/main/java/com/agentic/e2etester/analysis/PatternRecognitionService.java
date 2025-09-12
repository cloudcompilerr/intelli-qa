package com.agentic.e2etester.analysis;

import com.agentic.e2etester.model.TestFailure;
import com.agentic.e2etester.model.FailureType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for recognizing patterns in test failures and system behavior
 */
public interface PatternRecognitionService {
    
    /**
     * Recognizes failure patterns from historical data
     * 
     * @param failures List of failures to analyze for patterns
     * @return CompletableFuture containing recognized patterns and their frequency
     */
    CompletableFuture<Map<String, Integer>> recognizeFailurePatterns(List<TestFailure> failures);
    
    /**
     * Identifies recurring failure sequences across test executions
     * 
     * @param failures List of failures ordered by timestamp
     * @return CompletableFuture containing identified failure sequences
     */
    CompletableFuture<List<List<FailureType>>> identifyFailureSequences(List<TestFailure> failures);
    
    /**
     * Analyzes system behavior patterns that precede failures
     * 
     * @param systemMetrics Historical system metrics data
     * @param failures Associated failures
     * @return CompletableFuture containing behavioral patterns that correlate with failures
     */
    CompletableFuture<Map<String, Object>> analyzePreFailureBehavior(
        Map<String, Object> systemMetrics, List<TestFailure> failures);
    
    /**
     * Learns new patterns from recent failures and updates pattern database
     * 
     * @param recentFailures List of recent failures to learn from
     * @return CompletableFuture indicating completion of pattern learning
     */
    CompletableFuture<Void> updatePatternDatabase(List<TestFailure> recentFailures);
    
    /**
     * Predicts potential failure types based on current system state
     * 
     * @param currentSystemState Current system metrics and state
     * @return CompletableFuture containing predicted failure types with probability scores
     */
    CompletableFuture<Map<FailureType, Double>> predictPotentialFailures(Map<String, Object> currentSystemState);
}