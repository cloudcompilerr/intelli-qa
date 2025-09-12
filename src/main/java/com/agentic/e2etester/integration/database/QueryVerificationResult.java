package com.agentic.e2etester.integration.database;

/**
 * Result of verifying query results against expectations.
 */
public class QueryVerificationResult {
    
    private final boolean success;
    private final String errorMessage;
    private final String details;
    
    private QueryVerificationResult(boolean success, String errorMessage, String details) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.details = details;
    }
    
    public static QueryVerificationResult success() {
        return new QueryVerificationResult(true, null, null);
    }
    
    public static QueryVerificationResult success(String details) {
        return new QueryVerificationResult(true, null, details);
    }
    
    public static QueryVerificationResult failure(String errorMessage) {
        return new QueryVerificationResult(false, errorMessage, null);
    }
    
    public static QueryVerificationResult failure(String errorMessage, String details) {
        return new QueryVerificationResult(false, errorMessage, details);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getDetails() {
        return details;
    }
    
    @Override
    public String toString() {
        return "QueryVerificationResult{" +
                "success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}