package com.agentic.e2etester.integration.database;

/**
 * Result of database health check operations.
 */
public class DatabaseHealthResult {
    
    private final boolean healthy;
    private final String errorMessage;
    private final long responseTimeMs;
    
    private DatabaseHealthResult(boolean healthy, String errorMessage, long responseTimeMs) {
        this.healthy = healthy;
        this.errorMessage = errorMessage;
        this.responseTimeMs = responseTimeMs;
    }
    
    public static DatabaseHealthResult healthy() {
        return new DatabaseHealthResult(true, null, 0);
    }
    
    public static DatabaseHealthResult healthy(long responseTimeMs) {
        return new DatabaseHealthResult(true, null, responseTimeMs);
    }
    
    public static DatabaseHealthResult unhealthy(String errorMessage) {
        return new DatabaseHealthResult(false, errorMessage, 0);
    }
    
    public static DatabaseHealthResult unhealthy(String errorMessage, long responseTimeMs) {
        return new DatabaseHealthResult(false, errorMessage, responseTimeMs);
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    @Override
    public String toString() {
        return "DatabaseHealthResult{" +
                "healthy=" + healthy +
                ", errorMessage='" + errorMessage + '\'' +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}