package com.agentic.e2etester.config;

import com.agentic.e2etester.testdata.TestDataManager;
import com.agentic.e2etester.testdata.TestDataRequest;
import com.agentic.e2etester.testdata.TestDataValidationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConfigurationManagementIntegrationTest {
    
    @Autowired
    private TestEnvironmentConfiguration environmentConfig;
    
    @Autowired
    private TestDataConfiguration testDataConfig;
    
    @Autowired
    private TestDataManager testDataManager;
    
    @Test
    void shouldLoadEnvironmentConfiguration() {
        assertThat(environmentConfig).isNotNull();
        assertThat(environmentConfig.getActiveEnvironment()).isNotNull();
        assertThat(environmentConfig.getCurrentEnvironment()).isNotNull();
        assertThat(environmentConfig.getEnvironments()).isNotEmpty();
    }
    
    @Test
    void shouldLoadTestDataConfiguration() {
        assertThat(testDataConfig).isNotNull();
        assertThat(testDataConfig.isEnabled()).isTrue();
        assertThat(testDataConfig.getCleanupDelay()).isNotNull();
        assertThat(testDataConfig.getValidationTimeout()).isNotNull();
    }
    
    @Test
    void shouldIntegrateEnvironmentConfigWithTestDataManager() throws Exception {
        // Given
        String testId = "integration-test-001";
        TestDataRequest request = new TestDataRequest("order_fulfillment", 
            Map.of("customerId", "CUST-INTEGRATION"));
        
        // When - Generate test data
        CompletableFuture<Map<String, Object>> dataResult = testDataManager.generateTestData(testId, request);
        Map<String, Object> testData = dataResult.get();
        
        // Then - Verify data generation respects environment settings
        assertThat(testData).isNotEmpty();
        assertThat(testData).containsEntry("customerId", "CUST-INTEGRATION");
        
        // When - Validate test data
        CompletableFuture<TestDataValidationResult> validationResult = testDataManager.validateTestData(testId);
        TestDataValidationResult validation = validationResult.get();
        
        // Then - Validation should pass
        assertThat(validation.isValid()).isTrue();
        assertThat(validation.getTestId()).isEqualTo(testId);
        
        // When - Cleanup (if enabled in environment)
        testDataManager.cleanupTestData(testId).get();
        
        // Then - Data should be cleaned up based on environment configuration
        if (environmentConfig.getCurrentEnvironment().isCleanupAfterTest()) {
            CompletableFuture<Object> cleanupCheck = testDataManager.getTestData(testId, "order");
            assertThat(cleanupCheck.get()).isNull();
        }
    }
    
    @Test
    void shouldHandleDataIsolationBasedOnEnvironment() throws Exception {
        // Given
        String testId1 = "isolation-test-001";
        String testId2 = "isolation-test-002";
        
        // When - Isolate test data for both tests
        testDataManager.isolateTestData(testId1).get();
        testDataManager.isolateTestData(testId2).get();
        
        // Store different data for each test
        testDataManager.storeTestData(testId1, "shared-key", "value-for-test1").get();
        testDataManager.storeTestData(testId2, "shared-key", "value-for-test2").get();
        
        // Then - Each test should have its own isolated data
        CompletableFuture<Object> data1 = testDataManager.getTestData(testId1, "shared-key");
        CompletableFuture<Object> data2 = testDataManager.getTestData(testId2, "shared-key");
        
        assertThat(data1.get()).isEqualTo("value-for-test1");
        assertThat(data2.get()).isEqualTo("value-for-test2");
        
        // Cleanup
        testDataManager.cleanupTestData(testId1).get();
        testDataManager.cleanupTestData(testId2).get();
    }
    
    @Test
    void shouldCreateAndRestoreSnapshotsWhenEnabled() throws Exception {
        // Given
        if (!testDataConfig.isEnableSnapshots()) {
            return; // Skip if snapshots are disabled
        }
        
        String testId = "snapshot-test-001";
        TestDataRequest request = new TestDataRequest("customer_registration", Map.of());
        
        // When - Generate and store initial data
        Map<String, Object> initialData = testDataManager.generateTestData(testId, request).get();
        
        // Create snapshot
        String snapshotId = testDataManager.createDataSnapshot(testId).get();
        
        // Modify data
        testDataManager.storeTestData(testId, "modified-key", "modified-value").get();
        
        // Restore from snapshot
        testDataManager.restoreFromSnapshot(testId, snapshotId).get();
        
        // Then - Data should be restored to original state
        CompletableFuture<Object> restoredData = testDataManager.getTestData(testId, "customer");
        assertThat(restoredData.get()).isEqualTo(initialData.get("customer"));
        
        // Modified key should not exist after restore
        CompletableFuture<Object> modifiedData = testDataManager.getTestData(testId, "modified-key");
        assertThat(modifiedData.get()).isNull();
        
        // Cleanup
        testDataManager.cleanupTestData(testId).get();
    }
    
    @Test
    void shouldGenerateDataForMultipleScenarios() throws Exception {
        // Given
        List<String> scenarios = List.of("order_fulfillment", "payment_processing", "inventory_management");
        
        for (String scenario : scenarios) {
            String testId = "scenario-test-" + scenario;
            TestDataRequest request = new TestDataRequest(scenario, Map.of());
            
            // When
            CompletableFuture<Map<String, Object>> dataResult = testDataManager.generateTestData(testId, request);
            Map<String, Object> testData = dataResult.get();
            
            // Then
            assertThat(testData).isNotEmpty();
            
            // Validate scenario-specific data
            switch (scenario) {
                case "order_fulfillment":
                    assertThat(testData).containsKey("order");
                    break;
                case "payment_processing":
                    assertThat(testData).containsKey("payment");
                    break;
                case "inventory_management":
                    assertThat(testData).containsKey("inventory");
                    break;
            }
            
            // Cleanup
            testDataManager.cleanupTestData(testId).get();
        }
    }
    
    @Test
    void shouldRespectEnvironmentLimitsAndTimeouts() {
        // Given
        TestEnvironmentConfiguration.EnvironmentConfig currentEnv = environmentConfig.getCurrentEnvironment();
        
        // Then - Verify environment limits are properly configured
        assertThat(currentEnv.getMaxConcurrentTests()).isGreaterThan(0);
        assertThat(currentEnv.getTestTimeout()).isNotNull();
        assertThat(currentEnv.getTestTimeout().toSeconds()).isGreaterThan(0);
        
        // Verify test data configuration respects limits
        assertThat(testDataConfig.getMaxDataSizePerTest()).isGreaterThan(0);
        assertThat(testDataConfig.getMaxSnapshots()).isGreaterThan(0);
        assertThat(testDataConfig.getValidationTimeout().toSeconds()).isGreaterThan(0);
    }
}