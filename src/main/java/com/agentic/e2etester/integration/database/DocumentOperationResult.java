package com.agentic.e2etester.integration.database;

/**
 * Result of a document operation (store, delete, etc.).
 */
public class DocumentOperationResult {
    
    private final boolean success;
    private final String documentId;
    private final Long cas;
    private final String errorMessage;
    
    private DocumentOperationResult(boolean success, String documentId, Long cas, String errorMessage) {
        this.success = success;
        this.documentId = documentId;
        this.cas = cas;
        this.errorMessage = errorMessage;
    }
    
    public static DocumentOperationResult success(String documentId, Long cas) {
        return new DocumentOperationResult(true, documentId, cas, null);
    }
    
    public static DocumentOperationResult failure(String documentId, String errorMessage) {
        return new DocumentOperationResult(false, documentId, null, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public Long getCas() {
        return cas;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        return "DocumentOperationResult{" +
                "success=" + success +
                ", documentId='" + documentId + '\'' +
                ", cas=" + cas +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}