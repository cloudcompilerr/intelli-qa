package com.agentic.e2etester.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single test step within a test execution plan.
 * Each step defines an action to be performed against a specific service.
 */
public class TestStep {
    
    @NotBlank(message = "Step ID cannot be blank")
    @Size(max = 100, message = "Step ID cannot exceed 100 characters")
    @JsonProperty("stepId")
    private String stepId;
    
    @NotNull(message = "Step type cannot be null")
    @JsonProperty("type")
    private StepType type;
    
    @NotBlank(message = "Target service cannot be blank")
    @Size(max = 200, message = "Target service cannot exceed 200 characters")
    @JsonProperty("targetService")
    private String targetService;
    
    @JsonProperty("inputData")
    private Map<String, Object> inputData;
    
    @Valid
    @JsonProperty("expectedOutcomes")
    private List<ExpectedOutcome> expectedOutcomes;
    
    @NotNull(message = "Timeout cannot be null")
    @JsonProperty("timeoutMs")
    private Long timeoutMs;
    
    @Valid
    @JsonProperty("retryPolicy")
    private RetryPolicy retryPolicy;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("order")
    @Positive(message = "Order must be positive")
    private Integer order;
    
    @JsonProperty("dependsOn")
    private List<String> dependsOn;
    
    // Default constructor for Jackson
    public TestStep() {}
    
    // Constructor with required fields
    public TestStep(String stepId, StepType type, String targetService, Long timeoutMs) {
        this.stepId = stepId;
        this.type = type;
        this.targetService = targetService;
        this.timeoutMs = timeoutMs;
    }
    
    // Getters and setters
    public String getStepId() {
        return stepId;
    }
    
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }
    
    public StepType getType() {
        return type;
    }
    
    public void setType(StepType type) {
        this.type = type;
    }
    
    public String getTargetService() {
        return targetService;
    }
    
    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }
    
    public Map<String, Object> getInputData() {
        return inputData;
    }
    
    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }
    
    public List<ExpectedOutcome> getExpectedOutcomes() {
        return expectedOutcomes;
    }
    
    public void setExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
    }
    
    public Long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public Duration getTimeout() {
        return timeoutMs != null ? Duration.ofMillis(timeoutMs) : null;
    }
    
    public void setTimeout(Duration timeout) {
        this.timeoutMs = timeout != null ? timeout.toMillis() : null;
    }
    
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }
    
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public List<String> getDependsOn() {
        return dependsOn;
    }
    
    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestStep testStep = (TestStep) o;
        return Objects.equals(stepId, testStep.stepId) &&
               type == testStep.type &&
               Objects.equals(targetService, testStep.targetService);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(stepId, type, targetService);
    }
    
    @Override
    public String toString() {
        return "TestStep{" +
               "stepId='" + stepId + '\'' +
               ", type=" + type +
               ", targetService='" + targetService + '\'' +
               ", timeoutMs=" + timeoutMs +
               ", order=" + order +
               '}';
    }
}