package com.agentic.e2etester.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.agentic.e2etester.model.validation.ValidTestExecutionPlan;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a complete test execution plan containing all steps and configuration
 * needed to execute an end-to-end test scenario across microservices.
 */
@ValidTestExecutionPlan
public class TestExecutionPlan {
    
    @NotBlank(message = "Test ID cannot be blank")
    @Size(max = 100, message = "Test ID cannot exceed 100 characters")
    @JsonProperty("testId")
    private String testId;
    
    @NotBlank(message = "Scenario description cannot be blank")
    @Size(max = 1000, message = "Scenario description cannot exceed 1000 characters")
    @JsonProperty("scenario")
    private String scenario;
    
    @NotEmpty(message = "Test steps cannot be empty")
    @Valid
    @JsonProperty("steps")
    private List<TestStep> steps;
    
    @JsonProperty("testData")
    private Map<String, Object> testData;
    
    @NotNull(message = "Test configuration cannot be null")
    @Valid
    @JsonProperty("configuration")
    private TestConfiguration configuration;
    
    @Valid
    @JsonProperty("assertions")
    private List<AssertionRule> assertions;
    
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    
    @JsonProperty("estimatedDuration")
    private Long estimatedDurationMs;
    
    // Default constructor for Jackson
    public TestExecutionPlan() {
        this.createdAt = Instant.now();
    }
    
    // Constructor with required fields
    public TestExecutionPlan(String testId, String scenario, List<TestStep> steps, TestConfiguration configuration) {
        this.testId = testId;
        this.scenario = scenario;
        this.steps = steps;
        this.configuration = configuration;
        this.createdAt = Instant.now();
    }
    
    // Getters and setters
    public String getTestId() {
        return testId;
    }
    
    public void setTestId(String testId) {
        this.testId = testId;
    }
    
    public String getScenario() {
        return scenario;
    }
    
    public void setScenario(String scenario) {
        this.scenario = scenario;
    }
    
    public List<TestStep> getSteps() {
        return steps;
    }
    
    public void setSteps(List<TestStep> steps) {
        this.steps = steps;
    }
    
    public Map<String, Object> getTestData() {
        return testData;
    }
    
    public void setTestData(Map<String, Object> testData) {
        this.testData = testData;
    }
    
    public TestConfiguration getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(TestConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public List<AssertionRule> getAssertions() {
        return assertions;
    }
    
    public void setAssertions(List<AssertionRule> assertions) {
        this.assertions = assertions;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }
    
    public void setEstimatedDurationMs(Long estimatedDurationMs) {
        this.estimatedDurationMs = estimatedDurationMs;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestExecutionPlan that = (TestExecutionPlan) o;
        return Objects.equals(testId, that.testId) &&
               Objects.equals(scenario, that.scenario) &&
               Objects.equals(steps, that.steps) &&
               Objects.equals(configuration, that.configuration);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(testId, scenario, steps, configuration);
    }
    
    @Override
    public String toString() {
        return "TestExecutionPlan{" +
               "testId='" + testId + '\'' +
               ", scenario='" + scenario + '\'' +
               ", stepsCount=" + (steps != null ? steps.size() : 0) +
               ", configuration=" + configuration +
               ", createdAt=" + createdAt +
               '}';
    }
}