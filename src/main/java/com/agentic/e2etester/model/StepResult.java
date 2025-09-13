package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents the result of executing a single test step.
 */
public class StepResult {
    
    @NotBlank(message = "Step ID cannot be blank")
    @Size(max = 100, message = "Step ID cannot exceed 100 characters")
    @JsonProperty("stepId")
    private String stepId;
    
    @NotNull(message = "Status cannot be null")
    @JsonProperty("status")
    private TestStatus status;
    
    @JsonProperty("startTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startTime;
    
    @JsonProperty("endTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant endTime;
    
    @PositiveOrZero(message = "Execution time must be positive or zero")
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("output")
    private Object output;
    
    @JsonProperty("attemptCount")
    private Integer attemptCount;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("details")
    private String details;
    
    // Default constructor
    public StepResult() {}
    
    // Constructor with required fields
    public StepResult(String stepId, TestStatus status) {
        this.stepId = stepId;
        this.status = status;
        this.startTime = Instant.now();
        this.attemptCount = 1;
    }
    
    // Getters and setters
    public String getStepId() {
        return stepId;
    }
    
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }
    
    public TestStatus getStatus() {
        return status;
    }
    
    public void setStatus(TestStatus status) {
        this.status = status;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        if (this.startTime != null && endTime != null) {
            this.executionTimeMs = Duration.between(this.startTime, endTime).toMillis();
        }
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public Duration getExecutionTime() {
        return executionTimeMs != null ? Duration.ofMillis(executionTimeMs) : null;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Object getOutput() {
        return output;
    }
    
    public void setOutput(Object output) {
        this.output = output;
    }
    
    public Integer getAttemptCount() {
        return attemptCount;
    }
    
    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    // Utility methods
    public boolean isSuccessful() {
        return status == TestStatus.PASSED;
    }
    
    public boolean isFailed() {
        return status == TestStatus.FAILED;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StepResult that = (StepResult) o;
        return Objects.equals(stepId, that.stepId) &&
               status == that.status &&
               Objects.equals(startTime, that.startTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(stepId, status, startTime);
    }
    
    @Override
    public String toString() {
        return "StepResult{" +
               "stepId='" + stepId + '\'' +
               ", status=" + status +
               ", executionTimeMs=" + executionTimeMs +
               ", attemptCount=" + attemptCount +
               ", correlationId='" + correlationId + '\'' +
               '}';
    }
}