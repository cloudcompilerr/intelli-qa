package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a historical record of test execution with all relevant context and outcomes.
 * Used for pattern learning and failure analysis.
 */
public class TestExecutionHistory {
    
    @NotBlank(message = "Execution ID cannot be blank")
    @Size(max = 100, message = "Execution ID cannot exceed 100 characters")
    @JsonProperty("executionId")
    private String executionId;
    
    @NotBlank(message = "Test plan ID cannot be blank")
    @JsonProperty("testPlanId")
    private String testPlanId;
    
    @NotBlank(message = "Correlation ID cannot be blank")
    @JsonProperty("correlationId")
    private String correlationId;
    
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
    private long executionTimeMs;
    
    @JsonProperty("servicesInvolved")
    private List<String> servicesInvolved;
    
    @JsonProperty("interactions")
    private List<ServiceInteraction> interactions;
    
    @JsonProperty("metrics")
    private TestMetrics metrics;
    
    @JsonProperty("failures")
    private List<TestFailure> failures;
    
    @JsonProperty("environment")
    private String environment;
    
    @JsonProperty("configuration")
    private Map<String, Object> configuration;
    
    @JsonProperty("contextVariables")
    private Map<String, Object> contextVariables;
    
    @JsonProperty("patternId")
    private String patternId;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    // Default constructor for Jackson
    public TestExecutionHistory() {
        this.startTime = Instant.now();
    }
    
    // Constructor with required fields
    public TestExecutionHistory(String executionId, String testPlanId, String correlationId) {
        this();
        this.executionId = executionId;
        this.testPlanId = testPlanId;
        this.correlationId = correlationId;
    }
    
    // Getters and setters
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public String getTestPlanId() {
        return testPlanId;
    }
    
    public void setTestPlanId(String testPlanId) {
        this.testPlanId = testPlanId;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
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
            this.executionTimeMs = endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public List<String> getServicesInvolved() {
        return servicesInvolved;
    }
    
    public void setServicesInvolved(List<String> servicesInvolved) {
        this.servicesInvolved = servicesInvolved;
    }
    
    public List<ServiceInteraction> getInteractions() {
        return interactions;
    }
    
    public void setInteractions(List<ServiceInteraction> interactions) {
        this.interactions = interactions;
    }
    
    public TestMetrics getMetrics() {
        return metrics;
    }
    
    public void setMetrics(TestMetrics metrics) {
        this.metrics = metrics;
    }
    
    public List<TestFailure> getFailures() {
        return failures;
    }
    
    public void setFailures(List<TestFailure> failures) {
        this.failures = failures;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
    
    public Map<String, Object> getContextVariables() {
        return contextVariables;
    }
    
    public void setContextVariables(Map<String, Object> contextVariables) {
        this.contextVariables = contextVariables;
    }
    
    public String getPatternId() {
        return patternId;
    }
    
    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    // Utility methods
    public boolean isSuccessful() {
        return status == TestStatus.PASSED;
    }
    
    public boolean hasFailed() {
        return status == TestStatus.FAILED;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestExecutionHistory that = (TestExecutionHistory) o;
        return Objects.equals(executionId, that.executionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(executionId);
    }
    
    @Override
    public String toString() {
        return "TestExecutionHistory{" +
               "executionId='" + executionId + '\'' +
               ", testPlanId='" + testPlanId + '\'' +
               ", correlationId='" + correlationId + '\'' +
               ", status=" + status +
               ", executionTimeMs=" + executionTimeMs +
               ", servicesCount=" + (servicesInvolved != null ? servicesInvolved.size() : 0) +
               '}';
    }
}