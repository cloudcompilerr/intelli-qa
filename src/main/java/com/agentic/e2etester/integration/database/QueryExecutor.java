package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Query execution and result verification utilities for Couchbase N1QL queries.
 */
@Component
public class QueryExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    
    private final Cluster cluster;
    
    @Autowired
    public QueryExecutor(Cluster cluster) {
        this.cluster = cluster;
    }
    
    /**
     * Executes a N1QL query with parameters.
     */
    public CompletableFuture<QueryExecutionResult> executeQuery(String query, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Executing N1QL query: {}", query);
                logger.debug("Query parameters: {}", parameters);
                
                QueryOptions options = QueryOptions.queryOptions()
                    .timeout(DEFAULT_TIMEOUT);
                
                // Add parameters if provided
                if (parameters != null && !parameters.isEmpty()) {
                    options = options.parameters(JsonObject.from(parameters));
                }
                
                QueryResult result = cluster.query(query, options);
                
                List<JsonObject> rows = result.rowsAsObject();
                
                logger.debug("Query executed successfully, returned {} rows", rows.size());
                
                return QueryExecutionResult.success(rows, result.metaData());
                
            } catch (Exception e) {
                logger.error("Failed to execute query: {}", e.getMessage());
                return QueryExecutionResult.failure(e.getMessage());
            }
        });
    }
    
    /**
     * Executes a query with a custom timeout.
     */
    public CompletableFuture<QueryExecutionResult> executeQuery(String query, Map<String, Object> parameters, 
                                                               Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Executing N1QL query with timeout {}: {}", timeout, query);
                
                QueryOptions options = QueryOptions.queryOptions()
                    .timeout(timeout);
                
                if (parameters != null && !parameters.isEmpty()) {
                    options = options.parameters(JsonObject.from(parameters));
                }
                
                QueryResult result = cluster.query(query, options);
                List<JsonObject> rows = result.rowsAsObject();
                
                logger.debug("Query executed successfully with custom timeout, returned {} rows", rows.size());
                
                return QueryExecutionResult.success(rows, result.metaData());
                
            } catch (Exception e) {
                logger.error("Failed to execute query with custom timeout: {}", e.getMessage());
                return QueryExecutionResult.failure(e.getMessage());
            }
        });
    }
    
    /**
     * Verifies query results against expected outcomes.
     */
    public QueryVerificationResult verifyResults(List<JsonObject> actualRows, QueryExpectation expectation) {
        logger.debug("Verifying query results against expectation");
        
        try {
            // Check row count expectation
            if (expectation.getExpectedRowCount() != null) {
                int actualCount = actualRows.size();
                int expectedCount = expectation.getExpectedRowCount();
                
                if (actualCount != expectedCount) {
                    return QueryVerificationResult.failure(
                        String.format("Expected %d rows but got %d", expectedCount, actualCount));
                }
            }
            
            // Check minimum row count
            if (expectation.getMinimumRowCount() != null) {
                int actualCount = actualRows.size();
                int minimumCount = expectation.getMinimumRowCount();
                
                if (actualCount < minimumCount) {
                    return QueryVerificationResult.failure(
                        String.format("Expected at least %d rows but got %d", minimumCount, actualCount));
                }
            }
            
            // Check maximum row count
            if (expectation.getMaximumRowCount() != null) {
                int actualCount = actualRows.size();
                int maximumCount = expectation.getMaximumRowCount();
                
                if (actualCount > maximumCount) {
                    return QueryVerificationResult.failure(
                        String.format("Expected at most %d rows but got %d", maximumCount, actualCount));
                }
            }
            
            // Check specific field values in results
            if (expectation.getExpectedFieldValues() != null && !expectation.getExpectedFieldValues().isEmpty()) {
                for (Map.Entry<String, Object> expectedField : expectation.getExpectedFieldValues().entrySet()) {
                    String fieldName = expectedField.getKey();
                    Object expectedValue = expectedField.getValue();
                    
                    boolean foundMatch = actualRows.stream()
                        .anyMatch(row -> {
                            Object actualValue = row.get(fieldName);
                            return isValueMatch(actualValue, expectedValue);
                        });
                    
                    if (!foundMatch) {
                        return QueryVerificationResult.failure(
                            String.format("No row found with field '%s' having value '%s'", 
                                        fieldName, expectedValue));
                    }
                }
            }
            
            // Check that all rows contain required fields
            if (expectation.getRequiredFields() != null && !expectation.getRequiredFields().isEmpty()) {
                for (String requiredField : expectation.getRequiredFields()) {
                    boolean allRowsHaveField = actualRows.stream()
                        .allMatch(row -> row.containsKey(requiredField) && row.get(requiredField) != null);
                    
                    if (!allRowsHaveField) {
                        return QueryVerificationResult.failure(
                            String.format("Not all rows contain required field '%s'", requiredField));
                    }
                }
            }
            
            // Check custom validation rules
            if (expectation.getCustomValidations() != null) {
                for (QueryValidationRule rule : expectation.getCustomValidations()) {
                    QueryVerificationResult ruleResult = rule.validate(actualRows);
                    if (!ruleResult.isSuccess()) {
                        return ruleResult;
                    }
                }
            }
            
            logger.debug("Query result verification successful");
            return QueryVerificationResult.success("All verification checks passed");
            
        } catch (Exception e) {
            logger.error("Failed to verify query results: {}", e.getMessage());
            return QueryVerificationResult.failure("Verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Executes a query and verifies the results in one operation.
     */
    public CompletableFuture<QueryVerificationResult> executeAndVerify(String query, Map<String, Object> parameters,
                                                                      QueryExpectation expectation) {
        return executeQuery(query, parameters)
            .thenApply(executionResult -> {
                if (!executionResult.isSuccess()) {
                    return QueryVerificationResult.failure("Query execution failed: " + 
                                                          executionResult.getErrorMessage());
                }
                
                return verifyResults(executionResult.getRows(), expectation);
            });
    }
    
    /**
     * Counts the number of documents matching a query.
     */
    public CompletableFuture<Long> countDocuments(String bucketName, String whereClause, 
                                                 Map<String, Object> parameters) {
        String countQuery = String.format("SELECT COUNT(*) as count FROM `%s` WHERE %s", 
                                         bucketName, whereClause);
        
        return executeQuery(countQuery, parameters)
            .thenApply(result -> {
                if (!result.isSuccess() || result.getRows().isEmpty()) {
                    return 0L;
                }
                
                JsonObject firstRow = result.getRows().get(0);
                Object countValue = firstRow.get("count");
                
                if (countValue instanceof Number number) {
                    return number.longValue();
                }
                
                return 0L;
            });
    }
    
    /**
     * Checks if a value matches the expected value, handling different types.
     */
    private boolean isValueMatch(Object actualValue, Object expectedValue) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }
        
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        
        // Handle numeric comparisons
        if (actualValue instanceof Number actualNum && expectedValue instanceof Number expectedNum) {
            return actualNum.doubleValue() == expectedNum.doubleValue();
        }
        
        return actualValue.equals(expectedValue);
    }
    
    /**
     * Executes multiple queries in parallel.
     */
    public CompletableFuture<List<QueryExecutionResult>> executeQueriesInParallel(
            List<QueryRequest> queryRequests) {
        
        List<CompletableFuture<QueryExecutionResult>> futures = queryRequests.stream()
            .map(request -> executeQuery(request.getQuery(), request.getParameters()))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}