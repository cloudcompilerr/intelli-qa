package com.agentic.e2etester.cicd;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of pipeline configuration validation.
 */
public class PipelineValidationResult {
    
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    private String summary;
    
    public PipelineValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.valid = true;
    }
    
    public PipelineValidationResult(boolean valid) {
        this();
        this.valid = valid;
    }
    
    public void addError(String field, String message) {
        this.errors.add(new ValidationError(field, message));
        this.valid = false;
    }
    
    public void addWarning(String field, String message) {
        this.warnings.add(new ValidationWarning(field, message));
    }
    
    // Getters and setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { this.errors = errors; }
    
    public List<ValidationWarning> getWarnings() { return warnings; }
    public void setWarnings(List<ValidationWarning> warnings) { this.warnings = warnings; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public static class ValidationError {
        private String field;
        private String message;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() { return field; }
        public String getMessage() { return message; }
    }
    
    public static class ValidationWarning {
        private String field;
        private String message;
        
        public ValidationWarning(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() { return field; }
        public String getMessage() { return message; }
    }
}