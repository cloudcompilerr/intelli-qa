package com.agentic.e2etester.testdata;

import com.agentic.e2etester.config.TestEnvironmentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of TestDataManager
 */
@Service
public class DefaultTestDataManager implements TestDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultTestDataManager.class);
    
    private final TestEnvironmentConfiguration environmentConfig;
    private final Map<String, Map<String, Object>> testDataStore = new ConcurrentHashMap<>();
    private final Map<String, String> testSnapshots = new ConcurrentHashMap<>();
    private final TestDataGenerator dataGenerator;
    
    public DefaultTestDataManager(TestDataGenerator dataGenerator, TestEnvironmentConfiguration environmentConfig) {
        this.environmentConfig = environmentConfig;
        this.dataGenerator = dataGenerator;
    }
    
    @Override
    public CompletableFuture<Map<String, Object>> generateTestData(String testId, TestDataRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Generating test data for test: {}", testId);
            
            try {
                Map<String, Object> generatedData = dataGenerator.generateData(request);
                
                // Store generated data if isolation is enabled
                if (environmentConfig.getCurrentEnvironment().isDataIsolationEnabled()) {
                    testDataStore.put(testId, new ConcurrentHashMap<>(generatedData));
                }
                
                logger.info("Successfully generated {} data items for test: {}", 
                           generatedData.size(), testId);
                return generatedData;
                
            } catch (Exception e) {
                logger.error("Failed to generate test data for test: {}", testId, e);
                throw new RuntimeException("Test data generation failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> storeTestData(String testId, String dataKey, Object data) {
        return CompletableFuture.runAsync(() -> {
            logger.debug("Storing test data for test: {}, key: {}", testId, dataKey);
            
            testDataStore.computeIfAbsent(testId, k -> new ConcurrentHashMap<>())
                         .put(dataKey, data);
        });
    }
    
    @Override
    public CompletableFuture<Object> getTestData(String testId, String dataKey) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> testData = testDataStore.get(testId);
            if (testData == null) {
                logger.warn("No test data found for test: {}", testId);
                return null;
            }
            
            return testData.get(dataKey);
        });
    }
    
    @Override
    public CompletableFuture<Void> cleanupTestData(String testId) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Cleaning up test data for test: {}", testId);
            
            if (environmentConfig.getCurrentEnvironment().isCleanupAfterTest()) {
                testDataStore.remove(testId);
                testSnapshots.remove(testId);
                logger.info("Test data cleaned up for test: {}", testId);
            } else {
                logger.info("Cleanup disabled for environment, retaining test data for test: {}", testId);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> isolateTestData(String testId) {
        return CompletableFuture.runAsync(() -> {
            if (!environmentConfig.getCurrentEnvironment().isDataIsolationEnabled()) {
                logger.warn("Data isolation is disabled for current environment");
                return;
            }
            
            logger.info("Isolating test data for test: {}", testId);
            
            // Create isolated namespace for test data
            testDataStore.computeIfAbsent(testId, k -> new ConcurrentHashMap<>());
            
            logger.info("Test data isolated for test: {}", testId);
        });
    }
    
    @Override
    public CompletableFuture<TestDataValidationResult> validateTestData(String testId) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Validating test data for test: {}", testId);
            
            TestDataValidationResult result = new TestDataValidationResult(testId, true);
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            Map<String, Object> testData = testDataStore.get(testId);
            if (testData == null) {
                errors.add("No test data found for test: " + testId);
                result.setValid(false);
            } else {
                // Validate data integrity
                for (Map.Entry<String, Object> entry : testData.entrySet()) {
                    if (entry.getValue() == null) {
                        warnings.add("Null value found for key: " + entry.getKey());
                    }
                }
                
                if (testData.isEmpty()) {
                    warnings.add("Test data is empty for test: " + testId);
                }
            }
            
            result.setErrors(errors);
            result.setWarnings(warnings);
            
            if (!errors.isEmpty()) {
                result.setValid(false);
            }
            
            logger.info("Test data validation completed for test: {}, valid: {}", 
                       testId, result.isValid());
            
            return result;
        });
    }
    
    @Override
    public CompletableFuture<String> createDataSnapshot(String testId) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Creating data snapshot for test: {}", testId);
            
            String snapshotId = testId + "_snapshot_" + Instant.now().toEpochMilli();
            Map<String, Object> testData = testDataStore.get(testId);
            
            if (testData != null) {
                // Create a deep copy of the test data
                Map<String, Object> snapshot = new HashMap<>(testData);
                testDataStore.put(snapshotId, snapshot);
                testSnapshots.put(testId, snapshotId);
                
                logger.info("Data snapshot created with ID: {} for test: {}", snapshotId, testId);
            } else {
                logger.warn("No test data found to snapshot for test: {}", testId);
            }
            
            return snapshotId;
        });
    }
    
    @Override
    public CompletableFuture<Void> restoreFromSnapshot(String testId, String snapshotId) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Restoring test data from snapshot: {} for test: {}", snapshotId, testId);
            
            Map<String, Object> snapshotData = testDataStore.get(snapshotId);
            if (snapshotData != null) {
                testDataStore.put(testId, new ConcurrentHashMap<>(snapshotData));
                logger.info("Test data restored from snapshot for test: {}", testId);
            } else {
                logger.error("Snapshot not found: {} for test: {}", snapshotId, testId);
                throw new RuntimeException("Snapshot not found: " + snapshotId);
            }
        });
    }
}