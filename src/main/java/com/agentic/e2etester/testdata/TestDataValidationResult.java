package com.agentic.e2etester.testdata;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of test data validation
 */
public class TestDataValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private Map<String, Object> validationDetails;
    private Instant validationTimestamp;
    private String testId;
    
    public TestDataValidationResult() {
        this.validationTimestamp = Instant.now();
    }
    
    public TestDataValidationResult(String testId, boolean valid) {
        this();
        this.testId = testId;
        this.valid = valid;
    }
    
    // Getters and setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public Map<String, Object> getValidationDetails() {
        return validationDetails;
    }
    
    public void setValidationDetails(Map<String, Object> validationDetails) {
        this.validationDetails = validationDetails;
    }
    
    public Instant getValidationTimestamp() {
        return validationTimestamp;
    }
    
    public void setValidationTimestamp(Instant validationTimestamp) {
        this.validationTimestamp = validationTimestamp;
    }
    
    public String getTestId() {
        return testId;
    }
    
    public void setTestId(String testId) {
        this.testId = testId;
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}