package com.agentic.e2etester.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Maintains the execution context and state for a running test.
 * Tracks all interactions, events, and metrics during test execution.
 */
public class TestContext {
    
    @NotBlank(message = "Correlation ID cannot be blank")
    @Size(max = 100, message = "Correlation ID cannot exceed 100 characters")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("executionState")
    private Map<String, Object> executionState;
    
    @Valid
    @JsonProperty("interactions")
    private List<ServiceInteraction> interactions;
    
    @Valid
    @JsonProperty("metrics")
    private TestMetrics metrics;
    
    @Valid
    @JsonProperty("events")
    private List<TestEvent> events;
    
    @JsonProperty("startTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startTime;
    
    @JsonProperty("endTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant endTime;
    
    @JsonProperty("currentStepId")
    private String currentStepId;
    
    @JsonProperty("testExecutionPlanId")
    private String testExecutionPlanId;
    
    @JsonProperty("variables")
    private Map<String, Object> variables;
    
    // Default constructor for Jackson
    public TestContext() {
        this.executionState = new ConcurrentHashMap<>();
        this.interactions = new CopyOnWriteArrayList<>();
        this.events = new CopyOnWriteArrayList<>();
        this.variables = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
    }
    
    // Constructor with required fields
    public TestContext(String correlationId) {
        this();
        this.correlationId = correlationId;
    }
    
    // Getters and setters
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Map<String, Object> getExecutionState() {
        return executionState;
    }
    
    public void setExecutionState(Map<String, Object> executionState) {
        this.executionState = executionState;
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
    
    public List<TestEvent> getEvents() {
        return events;
    }
    
    public void setEvents(List<TestEvent> events) {
        this.events = events;
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
    }
    
    public String getCurrentStepId() {
        return currentStepId;
    }
    
    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }
    
    public String getTestExecutionPlanId() {
        return testExecutionPlanId;
    }
    
    public void setTestExecutionPlanId(String testExecutionPlanId) {
        this.testExecutionPlanId = testExecutionPlanId;
    }
    
    public Map<String, Object> getVariables() {
        return variables;
    }
    
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
    
    // Utility methods
    public void addInteraction(ServiceInteraction interaction) {
        if (this.interactions != null) {
            this.interactions.add(interaction);
        }
    }
    
    public void addEvent(TestEvent event) {
        if (this.events != null) {
            this.events.add(event);
        }
    }
    
    public void setVariable(String key, Object value) {
        if (this.variables != null) {
            this.variables.put(key, value);
        }
    }
    
    public Object getVariable(String key) {
        return this.variables != null ? this.variables.get(key) : null;
    }
    
    public void updateExecutionState(String key, Object value) {
        if (this.executionState != null) {
            this.executionState.put(key, value);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestContext that = (TestContext) o;
        return Objects.equals(correlationId, that.correlationId) &&
               Objects.equals(testExecutionPlanId, that.testExecutionPlanId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(correlationId, testExecutionPlanId);
    }
    
    @Override
    public String toString() {
        return "TestContext{" +
               "correlationId='" + correlationId + '\'' +
               ", testExecutionPlanId='" + testExecutionPlanId + '\'' +
               ", currentStepId='" + currentStepId + '\'' +
               ", startTime=" + startTime +
               ", endTime=" + endTime +
               ", interactionsCount=" + (interactions != null ? interactions.size() : 0) +
               ", eventsCount=" + (events != null ? events.size() : 0) +
               '}';
    }
}