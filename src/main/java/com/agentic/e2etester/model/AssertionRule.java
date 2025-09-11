package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Represents an assertion rule to be evaluated during test execution.
 */
public class AssertionRule {
    
    @NotBlank(message = "Rule ID cannot be blank")
    @Size(max = 100, message = "Rule ID cannot exceed 100 characters")
    @JsonProperty("ruleId")
    private String ruleId;
    
    @NotNull(message = "Assertion type cannot be null")
    @JsonProperty("type")
    private AssertionType type;
    
    @NotBlank(message = "Description cannot be blank")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("condition")
    private String condition;
    
    @JsonProperty("expectedValue")
    private Object expectedValue;
    
    @JsonProperty("actualValuePath")
    private String actualValuePath;
    
    @JsonProperty("parameters")
    private Map<String, Object> parameters;
    
    @JsonProperty("severity")
    private AssertionSeverity severity;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    // Default constructor
    public AssertionRule() {
        this.enabled = true;
        this.severity = AssertionSeverity.ERROR;
    }
    
    // Constructor with required fields
    public AssertionRule(String ruleId, AssertionType type, String description) {
        this();
        this.ruleId = ruleId;
        this.type = type;
        this.description = description;
    }
    
    // Getters and setters
    public String getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
    
    public AssertionType getType() {
        return type;
    }
    
    public void setType(AssertionType type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public Object getExpectedValue() {
        return expectedValue;
    }
    
    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }
    
    public String getActualValuePath() {
        return actualValuePath;
    }
    
    public void setActualValuePath(String actualValuePath) {
        this.actualValuePath = actualValuePath;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public AssertionSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(AssertionSeverity severity) {
        this.severity = severity;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssertionRule that = (AssertionRule) o;
        return Objects.equals(ruleId, that.ruleId) &&
               type == that.type &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(ruleId, type, description);
    }
    
    @Override
    public String toString() {
        return "AssertionRule{" +
               "ruleId='" + ruleId + '\'' +
               ", type=" + type +
               ", description='" + description + '\'' +
               ", severity=" + severity +
               ", enabled=" + enabled +
               '}';
    }
}