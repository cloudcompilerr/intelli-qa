package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryMetaData;

import java.util.Collections;
import java.util.List;

/**
 * Result of executing a N1QL query.
 */
public class QueryExecutionResult {
    
    private final boolean success;
    private final List<JsonObject> rows;
    private final QueryMetaData metaData;
    private final String errorMessage;
    
    private QueryExecutionResult(boolean success, List<JsonObject> rows, QueryMetaData metaData, String errorMessage) {
        this.success = success;
        this.rows = rows != null ? rows : Collections.emptyList();
        this.metaData = metaData;
        this.errorMessage = errorMessage;
    }
    
    public static QueryExecutionResult success(List<JsonObject> rows, QueryMetaData metaData) {
        return new QueryExecutionResult(true, rows, metaData, null);
    }
    
    public static QueryExecutionResult failure(String errorMessage) {
        return new QueryExecutionResult(false, null, null, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public List<JsonObject> getRows() {
        return rows;
    }
    
    public QueryMetaData getMetaData() {
        return metaData;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public int getRowCount() {
        return rows.size();
    }
    
    public boolean isEmpty() {
        return rows.isEmpty();
    }
    
    @Override
    public String toString() {
        return "QueryExecutionResult{" +
                "success=" + success +
                ", rowCount=" + rows.size() +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}