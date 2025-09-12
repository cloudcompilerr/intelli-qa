package com.agentic.e2etester.integration.database;

import java.util.Collections;
import java.util.List;

/**
 * Result of document validation operations.
 */
public class DocumentValidationResult {
    
    private final boolean success;
    private final String documentId;
    private final List<ValidationError> errors;
    private final String errorMessage;
    
    private DocumentValidationResult(boolean success, String documentId, List<ValidationError> errors, String errorMessage) {
        this.success = success;
        this.documentId = documentId;
        this.errors = errors != null ? errors : Collections.emptyList();
        this.errorMessage = errorMessage;
    }
    
    public static DocumentValidationResult success() {
        return new DocumentValidationResult(true, null, null, null);
    }
    
    public static DocumentValidationResult success(String documentId) {
        return new DocumentValidationResult(true, documentId, null, null);
    }
    
    public static DocumentValidationResult failure(String documentId, String errorMessage) {
        return new DocumentValidationResult(false, documentId, null, errorMessage);
    }
    
    public static DocumentValidationResult failure(List<ValidationError> errors) {
        return new DocumentValidationResult(false, null, errors, null);
    }
    
    public static DocumentValidationResult failure(String documentId, List<ValidationError> errors) {
        return new DocumentValidationResult(false, documentId, errors, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public String getErrorMessage() {
        if (errorMessage != null) {
            return errorMessage;
        }
        
        if (!errors.isEmpty()) {
            return errors.size() + " validation errors: " + 
                   errors.stream()
                       .map(ValidationError::getMessage)
                       .reduce((a, b) -> a + "; " + b)
                       .orElse("");
        }
        
        return null;
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    @Override
    public String toString() {
        return "DocumentValidationResult{" +
                "success=" + success +
                ", documentId='" + documentId + '\'' +
                ", errorCount=" + errors.size() +
                ", errorMessage='" + getErrorMessage() + '\'' +
                '}';
    }
}