package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CouchbaseAdapter using Testcontainers.
 * Requires Docker to be running.
 */
@SpringBootTest
@Testcontainers
class CouchbaseAdapterDockerIntegrationTest {
    
    private static final String BUCKET_NAME = "test-bucket";
    private static final String COLLECTION_NAME = "_default";
    
    @Container
    static CouchbaseContainer couchbaseContainer = new CouchbaseContainer("couchbase/server:7.2.0")
            .withBucket(new BucketDefinition(BUCKET_NAME))
            .withStartupTimeout(Duration.ofMinutes(2));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.couchbase.connection-string", couchbaseContainer::getConnectionString);
        registry.add("spring.couchbase.username", couchbaseContainer::getUsername);
        registry.add("spring.couchbase.password", couchbaseContainer::getPassword);
    }
    
    private CouchbaseAdapter couchbaseAdapter;
    private Cluster cluster;
    
    @BeforeEach
    void setUp() {
        cluster = Cluster.connect(
            couchbaseContainer.getConnectionString(),
            couchbaseContainer.getUsername(),
            couchbaseContainer.getPassword()
        );
        
        DocumentValidator documentValidator = new DocumentValidator();
        QueryExecutor queryExecutor = new QueryExecutor(cluster);
        couchbaseAdapter = new CouchbaseAdapter(cluster, documentValidator, queryExecutor);
    }
    
    @Test
    void testStoreAndRetrieveDocument() throws Exception {
        // Given
        String documentId = "test-doc-1";
        JsonObject document = JsonObject.create()
            .put("id", documentId)
            .put("name", "Test Document")
            .put("value", 42)
            .put("active", true);
        
        // When
        CompletableFuture<DocumentOperationResult> storeResult = 
            couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, documentId, document);
        
        DocumentOperationResult result = storeResult.get();
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(documentId, result.getDocumentId());
        assertNotNull(result.getCas());
        
        // Verify retrieval
        Optional<JsonObject> retrievedDoc = couchbaseAdapter.getDocument(BUCKET_NAME, COLLECTION_NAME, documentId);
        assertTrue(retrievedDoc.isPresent());
        assertEquals("Test Document", retrievedDoc.get().getString("name"));
        assertEquals(42, retrievedDoc.get().getInt("value"));
        assertTrue(retrievedDoc.get().getBoolean("active"));
    }
    
    @Test
    void testDocumentValidation() throws Exception {
        // Given
        String documentId = "validation-test-doc";
        JsonObject document = JsonObject.create()
            .put("orderId", "ORDER-123")
            .put("customerId", "CUST-456")
            .put("amount", 99.99)
            .put("status", "PENDING");
        
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, documentId, document).get();
        
        Map<String, Object> expectedValues = Map.of(
            "orderId", "ORDER-123",
            "customerId", "CUST-456",
            "amount", 99.99,
            "status", "PENDING"
        );
        
        // When
        DocumentValidationResult validationResult = 
            couchbaseAdapter.validateDocument(BUCKET_NAME, COLLECTION_NAME, documentId, expectedValues);
        
        // Then
        assertTrue(validationResult.isSuccess());
        assertEquals(0, validationResult.getErrorCount());
    }
    
    @Test
    void testDocumentValidationFailure() throws Exception {
        // Given
        String documentId = "validation-failure-doc";
        JsonObject document = JsonObject.create()
            .put("orderId", "ORDER-123")
            .put("amount", 99.99);
        
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, documentId, document).get();
        
        Map<String, Object> expectedValues = Map.of(
            "orderId", "ORDER-456", // Wrong value
            "customerId", "CUST-789", // Missing field
            "amount", 99.99
        );
        
        // When
        DocumentValidationResult validationResult = 
            couchbaseAdapter.validateDocument(BUCKET_NAME, COLLECTION_NAME, documentId, expectedValues);
        
        // Then
        assertFalse(validationResult.isSuccess());
        assertEquals(2, validationResult.getErrorCount());
        
        List<ValidationError> errors = validationResult.getErrors();
        assertTrue(errors.stream().anyMatch(e -> e.getFieldPath().equals("orderId")));
        assertTrue(errors.stream().anyMatch(e -> e.getFieldPath().equals("customerId")));
    }
    
    @Test
    void testDocumentComparison() throws Exception {
        // Given
        String doc1Id = "compare-doc-1";
        String doc2Id = "compare-doc-2";
        
        JsonObject document1 = JsonObject.create()
            .put("name", "Document 1")
            .put("value", 100);
        
        JsonObject document2 = JsonObject.create()
            .put("name", "Document 2")
            .put("value", 200);
        
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, doc1Id, document1).get();
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, doc2Id, document2).get();
        
        // When
        DocumentComparisonResult comparisonResult = 
            couchbaseAdapter.compareDocuments(BUCKET_NAME, COLLECTION_NAME, doc1Id, doc2Id);
        
        // Then
        assertTrue(comparisonResult.isSuccess());
        assertFalse(comparisonResult.isIdentical());
        assertEquals(2, comparisonResult.getDifferenceCount());
        
        List<FieldDifference> differences = comparisonResult.getDifferences();
        assertTrue(differences.stream().anyMatch(d -> d.getFieldName().equals("name")));
        assertTrue(differences.stream().anyMatch(d -> d.getFieldName().equals("value")));
    }
    
    @Test
    void testQueryExecution() throws Exception {
        // Given - Store test documents
        for (int i = 1; i <= 5; i++) {
            JsonObject doc = JsonObject.create()
                .put("type", "order")
                .put("orderId", "ORDER-" + i)
                .put("amount", i * 10.0)
                .put("status", i % 2 == 0 ? "COMPLETED" : "PENDING");
            
            couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, "order-" + i, doc).get();
        }
        
        // Wait for indexing
        Thread.sleep(1000);
        
        // When
        String query = "SELECT * FROM `" + BUCKET_NAME + "` WHERE type = 'order' AND status = 'COMPLETED'";
        CompletableFuture<QueryExecutionResult> queryResult = couchbaseAdapter.executeQuery(query, null);
        
        QueryExecutionResult result = queryResult.get();
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(2, result.getRowCount()); // Should find 2 completed orders
        assertFalse(result.isEmpty());
    }
    
    @Test
    void testQueryVerification() throws Exception {
        // Given - Store test documents
        JsonObject order1 = JsonObject.create()
            .put("type", "order")
            .put("orderId", "ORDER-VERIFY-1")
            .put("amount", 150.0)
            .put("status", "COMPLETED");
        
        JsonObject order2 = JsonObject.create()
            .put("type", "order")
            .put("orderId", "ORDER-VERIFY-2")
            .put("amount", 250.0)
            .put("status", "COMPLETED");
        
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, "verify-order-1", order1).get();
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, "verify-order-2", order2).get();
        
        // Wait for indexing
        Thread.sleep(1000);
        
        String query = "SELECT * FROM `" + BUCKET_NAME + "` WHERE type = 'order' AND orderId LIKE 'ORDER-VERIFY-%'";
        
        QueryExpectation expectation = QueryExpectation.builder()
            .expectedRowCount(2)
            .requiredFields(List.of("type", "orderId", "amount", "status"))
            .expectedFieldValues(Map.of("status", "COMPLETED"));
        
        // When
        QueryVerificationResult verificationResult = 
            couchbaseAdapter.verifyQueryResults(query, null, expectation);
        
        // Then
        assertTrue(verificationResult.isSuccess());
    }
    
    @Test
    void testDocumentExists() throws Exception {
        // Given
        String documentId = "exists-test-doc";
        JsonObject document = JsonObject.create().put("test", "value");
        
        // When - Document doesn't exist yet
        boolean existsBefore = couchbaseAdapter.documentExists(BUCKET_NAME, COLLECTION_NAME, documentId);
        
        // Store document
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, documentId, document).get();
        
        // When - Document exists now
        boolean existsAfter = couchbaseAdapter.documentExists(BUCKET_NAME, COLLECTION_NAME, documentId);
        
        // Then
        assertFalse(existsBefore);
        assertTrue(existsAfter);
    }
    
    @Test
    void testDeleteDocument() throws Exception {
        // Given
        String documentId = "delete-test-doc";
        JsonObject document = JsonObject.create().put("test", "value");
        
        couchbaseAdapter.storeDocument(BUCKET_NAME, COLLECTION_NAME, documentId, document).get();
        assertTrue(couchbaseAdapter.documentExists(BUCKET_NAME, COLLECTION_NAME, documentId));
        
        // When
        CompletableFuture<DocumentOperationResult> deleteResult = 
            couchbaseAdapter.deleteDocument(BUCKET_NAME, COLLECTION_NAME, documentId);
        
        DocumentOperationResult result = deleteResult.get();
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(documentId, result.getDocumentId());
        assertFalse(couchbaseAdapter.documentExists(BUCKET_NAME, COLLECTION_NAME, documentId));
    }
    
    @Test
    void testHealthCheck() {
        // When
        DatabaseHealthResult healthResult = couchbaseAdapter.checkHealth();
        
        // Then
        assertTrue(healthResult.isHealthy());
        assertNull(healthResult.getErrorMessage());
    }
}