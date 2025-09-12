package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CouchbaseAdapter.
 */
@ExtendWith(MockitoExtension.class)
class CouchbaseAdapterTest {
    
    @Mock
    private Cluster cluster;
    
    @Mock
    private DocumentValidator documentValidator;
    
    @Mock
    private QueryExecutor queryExecutor;
    
    private CouchbaseAdapter couchbaseAdapter;
    
    @BeforeEach
    void setUp() {
        couchbaseAdapter = new CouchbaseAdapter(cluster, documentValidator, queryExecutor);
    }
    
    @Test
    void testValidateDocument_Success() {
        // Given
        String bucketName = "test-bucket";
        String collectionName = "_default";
        String documentId = "test-doc";
        Map<String, Object> expectedValues = Map.of("field1", "value1");
        
        JsonObject mockDocument = JsonObject.create().put("field1", "value1");
        DocumentValidationResult mockResult = DocumentValidationResult.success(documentId);
        
        // Mock the getDocument method by creating a spy
        CouchbaseAdapter spyAdapter = spy(couchbaseAdapter);
        doReturn(Optional.of(mockDocument)).when(spyAdapter).getDocument(bucketName, collectionName, documentId);
        
        when(documentValidator.validate(mockDocument, expectedValues)).thenReturn(mockResult);
        
        // When
        DocumentValidationResult result = spyAdapter.validateDocument(bucketName, collectionName, documentId, expectedValues);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(documentId, result.getDocumentId());
        verify(documentValidator).validate(mockDocument, expectedValues);
    }
    
    @Test
    void testValidateDocument_DocumentNotFound() {
        // Given
        String bucketName = "test-bucket";
        String collectionName = "_default";
        String documentId = "missing-doc";
        Map<String, Object> expectedValues = Map.of("field1", "value1");
        
        // Mock the getDocument method by creating a spy
        CouchbaseAdapter spyAdapter = spy(couchbaseAdapter);
        doReturn(Optional.empty()).when(spyAdapter).getDocument(bucketName, collectionName, documentId);
        
        // When
        DocumentValidationResult result = spyAdapter.validateDocument(bucketName, collectionName, documentId, expectedValues);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("Document not found", result.getErrorMessage());
        verifyNoInteractions(documentValidator);
    }
    
    @Test
    void testCompareDocuments_Success() {
        // Given
        String bucketName = "test-bucket";
        String collectionName = "_default";
        String documentId1 = "doc1";
        String documentId2 = "doc2";
        
        JsonObject doc1 = JsonObject.create().put("field1", "value1");
        JsonObject doc2 = JsonObject.create().put("field1", "value2");
        DocumentComparisonResult mockResult = DocumentComparisonResult.different(List.of());
        
        // Mock the getDocument method by creating a spy
        CouchbaseAdapter spyAdapter = spy(couchbaseAdapter);
        doReturn(Optional.of(doc1)).when(spyAdapter).getDocument(bucketName, collectionName, documentId1);
        doReturn(Optional.of(doc2)).when(spyAdapter).getDocument(bucketName, collectionName, documentId2);
        
        when(documentValidator.compare(doc1, doc2)).thenReturn(mockResult);
        
        // When
        DocumentComparisonResult result = spyAdapter.compareDocuments(bucketName, collectionName, documentId1, documentId2);
        
        // Then
        assertTrue(result.isSuccess());
        verify(documentValidator).compare(doc1, doc2);
    }
    
    @Test
    void testCompareDocuments_DocumentNotFound() {
        // Given
        String bucketName = "test-bucket";
        String collectionName = "_default";
        String documentId1 = "doc1";
        String documentId2 = "missing-doc";
        
        JsonObject doc1 = JsonObject.create().put("field1", "value1");
        
        // Mock the getDocument method by creating a spy
        CouchbaseAdapter spyAdapter = spy(couchbaseAdapter);
        doReturn(Optional.of(doc1)).when(spyAdapter).getDocument(bucketName, collectionName, documentId1);
        doReturn(Optional.empty()).when(spyAdapter).getDocument(bucketName, collectionName, documentId2);
        
        // When
        DocumentComparisonResult result = spyAdapter.compareDocuments(bucketName, collectionName, documentId1, documentId2);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("One or both documents not found", result.getErrorMessage());
        verifyNoInteractions(documentValidator);
    }
}