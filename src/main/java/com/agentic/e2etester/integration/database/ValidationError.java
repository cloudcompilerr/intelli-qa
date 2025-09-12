package com.agentic.e2etester.integration.database;

/**
 * Represents a validation error for a specific field in a document.
 */
public class ValidationError {
    
    private final String fieldPath;
    private final String message;
    
    public ValidationError(String fieldPath, String message) {
        this.fieldPath = fieldPath;
        this.message = message;
    }
    
    public String getFieldPath() {
        return fieldPath;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return "ValidationError{" +
                "fieldPath='" + fieldPath + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ValidationError that = (ValidationError) o;
        
        if (!fieldPath.equals(that.fieldPath)) return false;
        return message.equals(that.message);
    }
    
    @Override
    public int hashCode() {
        int result = fieldPath.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }
}