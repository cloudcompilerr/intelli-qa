package com.agentic.e2etester.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the complete result of a test execution including status,
 * metrics, failures, and detailed analysis.
 */
public class TestResult {
    
    @NotBlank(message = "Test ID cannot be blank")
    @Size(max = 100, message = "Test ID cannot exceed 100 characters")
    @JsonProperty("testId")
    private String testId;
    
    @NotNull(message = "Test status cannot be null")
    @JsonProperty("status")
    private TestStatus status;
    
    @JsonProperty("startTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startTime;
    
    @JsonProperty("endTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant endTime;
    
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;
    
    @Valid
    @JsonProperty("stepResults")
    private List<StepResult> stepResults;
    
    @Valid
    @JsonProperty("assertionResults")
    private List<AssertionResult> assertionResults;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("failureReason")
    private String failureReason;
    
    @Valid
    @JsonProperty("metrics")
    private TestMetrics metrics;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("totalSteps")
    private Integer totalSteps;
    
    @JsonProperty("successfulSteps")
    private Integer successfulSteps;
    
    @JsonProperty("failedSteps")
    private Integer failedSteps;
    
    // Default constructor for Jackson
    public TestResult() {}
    
    // Constructor with required fields
    public TestResult(String testId, TestStatus status) {
        this.testId = testId;
        this.status = status;
        this.startTime = Instant.now();
    }
    
    // Getters and setters
    public String getTestId() {
        return testId;
    }
    
    public void setTestId(String testId) {
        this.testId = testId;
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
    
    public List<StepResult> getStepResults() {
        return stepResults;
    }
    
    public void setStepResults(List<StepResult> stepResults) {
        this.stepResults = stepResults;
    }
    
    public List<AssertionResult> getAssertionResults() {
        return assertionResults;
    }
    
    public void setAssertionResults(List<AssertionResult> assertionResults) {
        this.assertionResults = assertionResults;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public TestMetrics getMetrics() {
        return metrics;
    }
    
    public void setMetrics(TestMetrics metrics) {
        this.metrics = metrics;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Integer getTotalSteps() {
        return totalSteps;
    }
    
    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }
    
    public Integer getSuccessfulSteps() {
        return successfulSteps;
    }
    
    public void setSuccessfulSteps(Integer successfulSteps) {
        this.successfulSteps = successfulSteps;
    }
    
    public Integer getFailedSteps() {
        return failedSteps;
    }
    
    public void setFailedSteps(Integer failedSteps) {
        this.failedSteps = failedSteps;
    }
    
    // Utility methods
    public boolean isSuccessful() {
        return status == TestStatus.PASSED;
    }
    
    public boolean isFailed() {
        return status == TestStatus.FAILED;
    }
    
    public double getSuccessRate() {
        if (totalSteps == null || totalSteps == 0) {
            return 0.0;
        }
        return (double) (successfulSteps != null ? successfulSteps : 0) / totalSteps;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestResult that = (TestResult) o;
        return Objects.equals(testId, that.testId) &&
               Objects.equals(correlationId, that.correlationId) &&
               Objects.equals(startTime, that.startTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(testId, correlationId, startTime);
    }
    
    @Override
    public String toString() {
        return "TestResult{" +
               "testId='" + testId + '\'' +
               ", status=" + status +
               ", executionTimeMs=" + executionTimeMs +
               ", totalSteps=" + totalSteps +
               ", successfulSteps=" + successfulSteps +
               ", failedSteps=" + failedSteps +
               ", correlationId='" + correlationId + '\'' +
               '}';
    }
}