package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Represents an expected outcome for a test step.
 */
public class ExpectedOutcome {
    
    @NotBlank(message = "Outcome ID cannot be blank")
    @Size(max = 100, message = "Outcome ID cannot exceed 100 characters")
    @JsonProperty("outcomeId")
    private String outcomeId;
    
    @NotNull(message = "Outcome type cannot be null")
    @JsonProperty("type")
    private OutcomeType type;
    
    @NotBlank(message = "Description cannot be blank")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("expectedValue")
    private Object expectedValue;
    
    @JsonProperty("condition")
    private String condition;
    
    // Default constructor
    public ExpectedOutcome() {}
    
    // Constructor with required fields
    public ExpectedOutcome(String outcomeId, OutcomeType type, String description) {
        this.outcomeId = outcomeId;
        this.type = type;
        this.description = description;
    }
    
    // Getters and setters
    public String getOutcomeId() {
        return outcomeId;
    }
    
    public void setOutcomeId(String outcomeId) {
        this.outcomeId = outcomeId;
    }
    
    public OutcomeType getType() {
        return type;
    }
    
    public void setType(OutcomeType type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Object getExpectedValue() {
        return expectedValue;
    }
    
    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedOutcome that = (ExpectedOutcome) o;
        return Objects.equals(outcomeId, that.outcomeId) &&
               type == that.type &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(outcomeId, type, description);
    }
    
    @Override
    public String toString() {
        return "ExpectedOutcome{" +
               "outcomeId='" + outcomeId + '\'' +
               ", type=" + type +
               ", description='" + description + '\'' +
               '}';
    }
}