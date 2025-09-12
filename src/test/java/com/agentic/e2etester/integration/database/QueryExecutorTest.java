package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryMetaData;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QueryExecutor.
 */
@ExtendWith(MockitoExtension.class)
class QueryExecutorTest {
    
    @Mock
    private Cluster cluster;
    
    @Mock
    private QueryResult queryResult;
    
    @Mock
    private QueryMetaData queryMetaData;
    
    private QueryExecutor queryExecutor;
    
    @BeforeEach
    void setUp() {
        queryExecutor = new QueryExecutor(cluster);
    }
    
    @Test
    void testExecuteQuery_Success() throws Exception {
        // Given
        String query = "SELECT * FROM test WHERE id = $1";
        Map<String, Object> parameters = Map.of("1", "test-id");
        
        List<JsonObject> mockRows = List.of(
            JsonObject.create().put("id", "test-id").put("name", "Test Document")
        );
        
        when(cluster.query(anyString(), any(QueryOptions.class))).thenReturn(queryResult);
        when(queryResult.rowsAsObject()).thenReturn(mockRows);
        when(queryResult.metaData()).thenReturn(queryMetaData);
        
        // When
        CompletableFuture<QueryExecutionResult> future = queryExecutor.executeQuery(query, parameters);
        QueryExecutionResult result = future.get();
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRowCount());
        assertFalse(result.isEmpty());
        assertEquals(mockRows, result.getRows());
        assertEquals(queryMetaData, result.getMetaData());
    }
    
    @Test
    void testExecuteQuery_Failure() throws Exception {
        // Given
        String query = "INVALID QUERY";
        Map<String, Object> parameters = Map.of();
        
        when(cluster.query(anyString(), any(QueryOptions.class)))
            .thenThrow(new RuntimeException("Query syntax error"));
        
        // When
        CompletableFuture<QueryExecutionResult> future = queryExecutor.executeQuery(query, parameters);
        QueryExecutionResult result = future.get();
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals(0, result.getRowCount());
        assertTrue(result.isEmpty());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Query syntax error"));
    }
    
    @Test
    void testVerifyResults_RowCountMatch() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1"),
            JsonObject.create().put("id", "2")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .expectedRowCount(2);
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testVerifyResults_RowCountMismatch() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .expectedRowCount(2);
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Expected 2 rows but got 1"));
    }
    
    @Test
    void testVerifyResults_MinimumRowCount() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1"),
            JsonObject.create().put("id", "2"),
            JsonObject.create().put("id", "3")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .minimumRowCount(2);
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testVerifyResults_MinimumRowCountFail() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .minimumRowCount(3);
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Expected at least 3 rows but got 1"));
    }
    
    @Test
    void testVerifyResults_MaximumRowCount() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1"),
            JsonObject.create().put("id", "2")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .maximumRowCount(5);
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testVerifyResults_MaximumRowCountFail() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1"),
            JsonObject.create().put("id", "2"),
            JsonObject.create().put("id", "3")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .maximumRowCount(2);
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Expected at most 2 rows but got 3"));
    }
    
    @Test
    void testVerifyResults_ExpectedFieldValues() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("status", "COMPLETED").put("amount", 100.0),
            JsonObject.create().put("status", "PENDING").put("amount", 200.0),
            JsonObject.create().put("status", "COMPLETED").put("amount", 150.0)
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .expectedFieldValues(Map.of("status", "COMPLETED"));
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testVerifyResults_ExpectedFieldValuesFail() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("status", "PENDING").put("amount", 100.0),
            JsonObject.create().put("status", "PENDING").put("amount", 200.0)
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .expectedFieldValues(Map.of("status", "COMPLETED"));
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("No row found with field 'status' having value 'COMPLETED'"));
    }
    
    @Test
    void testVerifyResults_RequiredFields() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1").put("name", "Test 1").put("status", "ACTIVE"),
            JsonObject.create().put("id", "2").put("name", "Test 2").put("status", "INACTIVE")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .requiredFields(List.of("id", "name", "status"));
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testVerifyResults_RequiredFieldsFail() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("id", "1").put("name", "Test 1"),
            JsonObject.create().put("id", "2").put("status", "INACTIVE")
        );
        
        QueryExpectation expectation = QueryExpectation.builder()
            .requiredFields(List.of("id", "name", "status"));
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Not all rows contain required field"));
    }
    
    @Test
    void testVerifyResults_CustomValidation() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("amount", 100.0),
            JsonObject.create().put("amount", 200.0),
            JsonObject.create().put("amount", 150.0)
        );
        
        QueryValidationRule customRule = (rowList) -> {
            double totalAmount = rowList.stream()
                .mapToDouble(row -> row.getDouble("amount"))
                .sum();
            
            if (totalAmount > 400.0) {
                return QueryVerificationResult.success("Total amount validation passed");
            } else {
                return QueryVerificationResult.failure("Total amount too low: " + totalAmount);
            }
        };
        
        QueryExpectation expectation = QueryExpectation.builder()
            .customValidations(List.of(customRule));
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertTrue(result.isSuccess());
        // Note: Custom validation details are not propagated in the current implementation
    }
    
    @Test
    void testVerifyResults_CustomValidationFail() {
        // Given
        List<JsonObject> rows = List.of(
            JsonObject.create().put("amount", 50.0),
            JsonObject.create().put("amount", 75.0)
        );
        
        QueryValidationRule customRule = (rowList) -> {
            double totalAmount = rowList.stream()
                .mapToDouble(row -> row.getDouble("amount"))
                .sum();
            
            if (totalAmount > 400.0) {
                return QueryVerificationResult.success("Total amount validation passed");
            } else {
                return QueryVerificationResult.failure("Total amount too low: " + totalAmount);
            }
        };
        
        QueryExpectation expectation = QueryExpectation.builder()
            .customValidations(List.of(customRule));
        
        // When
        QueryVerificationResult result = queryExecutor.verifyResults(rows, expectation);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Total amount too low: 125.0"));
    }
}