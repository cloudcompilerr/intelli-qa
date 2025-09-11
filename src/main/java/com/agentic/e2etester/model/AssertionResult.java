package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Represents the result of evaluating an assertion rule.
 */
public class AssertionResult {
    
    @NotBlank(message = "Rule ID cannot be blank")
    @Size(max = 100, message = "Rule ID cannot exceed 100 characters")
    @JsonProperty("ruleId")
    private String ruleId;
    
    @NotNull(message = "Passed status cannot be null")
    @JsonProperty("passed")
    private Boolean passed;
    
    @JsonProperty("actualValue")
    private Object actualValue;
    
    @JsonProperty("expectedValue")
    private Object expectedValue;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("severity")
    private AssertionSeverity severity;
    
    @JsonProperty("stepId")
    private String stepId;
    
    // Default constructor
    public AssertionResult() {}
    
    // Constructor with required fields
    public AssertionResult(String ruleId, Boolean passed) {
        this.ruleId = ruleId;
        this.passed = passed;
    }
    
    // Constructor with all common fields
    public AssertionResult(String ruleId, Boolean passed, Object actualValue, Object expectedValue, String message) {
        this.ruleId = ruleId;
        this.passed = passed;
        this.actualValue = actualValue;
        this.expectedValue = expectedValue;
        this.message = message;
    }
    
    // Getters and setters
    public String getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
    
    public Boolean getPassed() {
        return passed;
    }
    
    public void setPassed(Boolean passed) {
        this.passed = passed;
    }
    
    public Object getActualValue() {
        return actualValue;
    }
    
    public void setActualValue(Object actualValue) {
        this.actualValue = actualValue;
    }
    
    public Object getExpectedValue() {
        return expectedValue;
    }
    
    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public AssertionSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(AssertionSeverity severity) {
        this.severity = severity;
    }
    
    public String getStepId() {
        return stepId;
    }
    
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }
    
    // Utility methods
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(passed);
    }
    
    public boolean isFailed() {
        return Boolean.FALSE.equals(passed);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssertionResult that = (AssertionResult) o;
        return Objects.equals(ruleId, that.ruleId) &&
               Objects.equals(passed, that.passed) &&
               Objects.equals(stepId, that.stepId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(ruleId, passed, stepId);
    }
    
    @Override
    public String toString() {
        return "AssertionResult{" +
               "ruleId='" + ruleId + '\'' +
               ", passed=" + passed +
               ", actualValue=" + actualValue +
               ", expectedValue=" + expectedValue +
               ", message='" + message + '\'' +
               ", stepId='" + stepId + '\'' +
               '}';
    }
}