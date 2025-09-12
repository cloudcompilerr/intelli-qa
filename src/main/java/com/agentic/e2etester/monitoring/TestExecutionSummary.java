package com.agentic.e2etester.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Summary information for active test executions.
 */
public class TestExecutionSummary {
    
    private final String testId;
    private final Instant startTime;
    private final Duration runningTime;
    private final String status;
    private final Map<String, Object> executionState;
    
    public TestExecutionSummary(String testId, Instant startTime, Duration runningTime, 
                              String status, Map<String, Object> executionState) {
        this.testId = testId;
        this.startTime = startTime;
        this.runningTime = runningTime;
        this.status = status;
        this.executionState = executionState;
    }
    
    // Getters
    public String getTestId() {
        return testId;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Duration getRunningTime() {
        return runningTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public Map<String, Object> getExecutionState() {
        return executionState;
    }
}