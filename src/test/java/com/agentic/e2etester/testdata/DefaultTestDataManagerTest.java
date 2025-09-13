package com.agentic.e2etester.testdata;

import com.agentic.e2etester.config.TestEnvironmentConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefaultTestDataManagerTest {
    
    @Mock
    private TestEnvironmentConfiguration environmentConfig;
    
    @Mock
    private TestEnvironmentConfiguration.EnvironmentConfig currentEnvironment;
    
    @Mock
    private TestDataGenerator dataGenerator;
    
    private DefaultTestDataManager testDataManager;
    
    @BeforeEach
    void setUp() {
        lenient().when(environmentConfig.getCurrentEnvironment()).thenReturn(currentEnvironment);
        lenient().when(currentEnvironment.isDataIsolationEnabled()).thenReturn(true);
        lenient().when(currentEnvironment.isCleanupAfterTest()).thenReturn(true);
        
        testDataManager = new DefaultTestDataManager(dataGenerator, environmentConfig);
    }
    
    @Test
    void shouldGenerateTestData() throws Exception {
        // Given
        String testId = "test-123";
        TestDataRequest request = new TestDataRequest("order_fulfillment", Map.of("customerId", "CUST-001"));
        Map<String, Object> expectedData = Map.of("orderId", "ORD-123", "customerId", "CUST-001");
        
        when(dataGenerator.generateData(any(TestDataRequest.class))).thenReturn(expectedData);
        
        // When
        CompletableFuture<Map<String, Object>> result = testDataManager.generateTestData(testId, request);
        
        // Then
        Map<String, Object> actualData = result.get();
        assertThat(actualData).isEqualTo(expectedData);
    }
    
    @Test
    void shouldStoreAndRetrieveTestData() throws Exception {
        // Given
        String testId = "test-123";
        String dataKey = "order";
        Object testData = Map.of("orderId", "ORD-123");
        
        // When
        testDataManager.storeTestData(testId, dataKey, testData).get();
        CompletableFuture<Object> result = testDataManager.getTestData(testId, dataKey);
        
        // Then
        Object retrievedData = result.get();
        assertThat(retrievedData).isEqualTo(testData);
    }
    
    @Test
    void shouldReturnNullForNonExistentTestData() throws Exception {
        // Given
        String testId = "nonexistent-test";
        String dataKey = "order";
        
        // When
        CompletableFuture<Object> result = testDataManager.getTestData(testId, dataKey);
        
        // Then
        Object retrievedData = result.get();
        assertThat(retrievedData).isNull();
    }
    
    @Test
    void shouldValidateTestData() throws Exception {
        // Given
        String testId = "test-123";
        testDataManager.storeTestData(testId, "order", Map.of("orderId", "ORD-123")).get();
        
        // When
        CompletableFuture<TestDataValidationResult> result = testDataManager.validateTestData(testId);
        
        // Then
        TestDataValidationResult validation = result.get();
        assertThat(validation.isValid()).isTrue();
        assertThat(validation.getTestId()).isEqualTo(testId);
        assertThat(validation.hasErrors()).isFalse();
    }
    
    @Test
    void shouldFailValidationForMissingTestData() throws Exception {
        // Given
        String testId = "nonexistent-test";
        
        // When
        CompletableFuture<TestDataValidationResult> result = testDataManager.validateTestData(testId);
        
        // Then
        TestDataValidationResult validation = result.get();
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.hasErrors()).isTrue();
        assertThat(validation.getErrors()).contains("No test data found for test: " + testId);
    }
    
    @Test
    void shouldCreateAndRestoreSnapshot() throws Exception {
        // Given
        String testId = "test-123";
        String dataKey = "order";
        Object originalData = Map.of("orderId", "ORD-123", "status", "PENDING");
        
        testDataManager.storeTestData(testId, dataKey, originalData).get();
        
        // When - Create snapshot
        CompletableFuture<String> snapshotResult = testDataManager.createDataSnapshot(testId);
        String snapshotId = snapshotResult.get();
        
        // Modify original data
        testDataManager.storeTestData(testId, dataKey, Map.of("orderId", "ORD-123", "status", "COMPLETED")).get();
        
        // Restore from snapshot
        testDataManager.restoreFromSnapshot(testId, snapshotId).get();
        
        // Then
        CompletableFuture<Object> restoredResult = testDataManager.getTestData(testId, dataKey);
        Object restoredData = restoredResult.get();
        
        assertThat(snapshotId).startsWith(testId + "_snapshot_");
        assertThat(restoredData).isEqualTo(originalData);
    }
    
    @Test
    void shouldIsolateTestData() throws Exception {
        // Given
        String testId = "test-123";
        
        // When
        testDataManager.isolateTestData(testId).get();
        
        // Then - Should be able to store data in isolated namespace
        testDataManager.storeTestData(testId, "isolated-key", "isolated-value").get();
        CompletableFuture<Object> result = testDataManager.getTestData(testId, "isolated-key");
        
        assertThat(result.get()).isEqualTo("isolated-value");
    }
    
    @Test
    void shouldCleanupTestDataWhenEnabled() throws Exception {
        // Given
        String testId = "test-123";
        testDataManager.storeTestData(testId, "order", Map.of("orderId", "ORD-123")).get();
        
        // When
        testDataManager.cleanupTestData(testId).get();
        
        // Then
        CompletableFuture<Object> result = testDataManager.getTestData(testId, "order");
        assertThat(result.get()).isNull();
    }
    
    @Test
    void shouldNotCleanupTestDataWhenDisabled() throws Exception {
        // Given
        when(currentEnvironment.isCleanupAfterTest()).thenReturn(false);
        
        String testId = "test-123";
        Object testData = Map.of("orderId", "ORD-123");
        testDataManager.storeTestData(testId, "order", testData).get();
        
        // When
        testDataManager.cleanupTestData(testId).get();
        
        // Then
        CompletableFuture<Object> result = testDataManager.getTestData(testId, "order");
        assertThat(result.get()).isEqualTo(testData);
    }
}