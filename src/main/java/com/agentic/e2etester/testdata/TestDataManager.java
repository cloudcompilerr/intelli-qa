package com.agentic.e2etester.testdata;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing test data lifecycle
 */
public interface TestDataManager {
    
    /**
     * Generate dynamic test data for a specific test scenario
     */
    CompletableFuture<Map<String, Object>> generateTestData(String testId, TestDataRequest request);
    
    /**
     * Store test data with isolation
     */
    CompletableFuture<Void> storeTestData(String testId, String dataKey, Object data);
    
    /**
     * Retrieve test data by key
     */
    CompletableFuture<Object> getTestData(String testId, String dataKey);
    
    /**
     * Clean up test data after test completion
     */
    CompletableFuture<Void> cleanupTestData(String testId);
    
    /**
     * Isolate test data to prevent interference between tests
     */
    CompletableFuture<Void> isolateTestData(String testId);
    
    /**
     * Validate test data integrity
     */
    CompletableFuture<TestDataValidationResult> validateTestData(String testId);
    
    /**
     * Create a snapshot of test data for rollback purposes
     */
    CompletableFuture<String> createDataSnapshot(String testId);
    
    /**
     * Restore test data from a snapshot
     */
    CompletableFuture<Void> restoreFromSnapshot(String testId, String snapshotId);
}