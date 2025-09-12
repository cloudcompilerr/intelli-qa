package com.agentic.e2etester.integration.database;

import java.util.Map;

/**
 * Represents a query request with parameters.
 */
public class QueryRequest {
    
    private final String query;
    private final Map<String, Object> parameters;
    
    public QueryRequest(String query, Map<String, Object> parameters) {
        this.query = query;
        this.parameters = parameters;
    }
    
    public static QueryRequest of(String query) {
        return new QueryRequest(query, null);
    }
    
    public static QueryRequest of(String query, Map<String, Object> parameters) {
        return new QueryRequest(query, parameters);
    }
    
    public String getQuery() {
        return query;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    @Override
    public String toString() {
        return "QueryRequest{" +
                "query='" + query + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}