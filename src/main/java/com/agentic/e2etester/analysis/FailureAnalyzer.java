package com.agentic.e2etester.analysis;

import com.agentic.e2etester.model.FailureAnalysis;
import com.agentic.e2etester.model.TestContext;
import com.agentic.e2etester.model.TestFailure;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI-powered failure analysis system
 */
public interface FailureAnalyzer {
    
    /**
     * Analyzes a test failure using AI to determine root cause and provide remediation suggestions
     * 
     * @param failure The test failure to analyze
     * @param context The test execution context
     * @return CompletableFuture containing the failure analysis result
     */
    CompletableFuture<FailureAnalysis> analyzeFailure(TestFailure failure, TestContext context);
    
    /**
     * Correlates multiple failures to identify patterns and common root causes
     * 
     * @param failures List of failures to correlate
     * @return CompletableFuture containing correlated failure analysis
     */
    CompletableFuture<List<FailureAnalysis>> correlateFailures(List<TestFailure> failures);
    
    /**
     * Analyzes historical failure patterns to improve future analysis accuracy
     * 
     * @param historicalFailures List of historical failures with known outcomes
     * @return CompletableFuture indicating completion of pattern learning
     */
    CompletableFuture<Void> learnFromPatterns(List<TestFailure> historicalFailures);
    
    /**
     * Gets similar failures from historical data based on failure characteristics
     * 
     * @param failure The failure to find similar cases for
     * @return CompletableFuture containing list of similar failures
     */
    CompletableFuture<List<TestFailure>> findSimilarFailures(TestFailure failure);
}