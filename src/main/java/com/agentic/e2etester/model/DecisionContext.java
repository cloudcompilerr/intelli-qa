package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Context information used by the AI agent for making execution decisions.
 * Contains current test context, historical patterns, and execution history.
 */
public class DecisionContext {
    
    private TestContext testContext;
    private Instant timestamp;
    private List<TestPattern> similarPatterns;
    private List<TestExecutionHistory> executionHistory;
    private Map<String, Object> environmentData;
    private String currentStepId;
    private StepResult lastStepResult;
    private List<TestFailure> recentFailures;
    private double systemLoad;
    private Map<String, ServiceStatus> serviceStatuses;
    
    public DecisionContext() {
        this.environmentData = new HashMap<>();
        this.serviceStatuses = new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    /**
     * Adds environment data for decision making.
     */
    public void addEnvironmentData(String key, Object value) {
        this.environmentData.put(key, value);
    }
    
    /**
     * Gets environment data value.
     */
    public Object getEnvironmentData(String key) {
        return this.environmentData.get(key);
    }
    
    /**
     * Adds service status information.
     */
    public void addServiceStatus(String serviceName, ServiceStatus status) {
        this.serviceStatuses.put(serviceName, status);
    }
    
    /**
     * Gets service status.
     */
    public ServiceStatus getServiceStatus(String serviceName) {
        return this.serviceStatuses.get(serviceName);
    }
    
    /**
     * Checks if any services are unhealthy.
     */
    public boolean hasUnhealthyServices() {
        return serviceStatuses.values().stream()
                .anyMatch(status -> status != ServiceStatus.HEALTHY);
    }
    
    /**
     * Gets count of similar patterns found.
     */
    public int getSimilarPatternCount() {
        return similarPatterns != null ? similarPatterns.size() : 0;
    }
    
    /**
     * Gets count of execution history records.
     */
    public int getExecutionHistoryCount() {
        return executionHistory != null ? executionHistory.size() : 0;
    }
    
    /**
     * Checks if system is under high load.
     */
    public boolean isHighLoad() {
        return systemLoad > 0.8;
    }
    
    /**
     * Gets the most recent failure if any.
     */
    public TestFailure getMostRecentFailure() {
        if (recentFailures == null || recentFailures.isEmpty()) {
            return null;
        }
        return recentFailures.get(recentFailures.size() - 1);
    }
    
    // Getters and Setters
    
    public TestContext getTestContext() {
        return testContext;
    }
    
    public void setTestContext(TestContext testContext) {
        this.testContext = testContext;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<TestPattern> getSimilarPatterns() {
        return similarPatterns;
    }
    
    public void setSimilarPatterns(List<TestPattern> similarPatterns) {
        this.similarPatterns = similarPatterns;
    }
    
    public List<TestExecutionHistory> getExecutionHistory() {
        return executionHistory;
    }
    
    public void setExecutionHistory(List<TestExecutionHistory> executionHistory) {
        this.executionHistory = executionHistory;
    }
    
    public Map<String, Object> getEnvironmentData() {
        return environmentData;
    }
    
    public void setEnvironmentData(Map<String, Object> environmentData) {
        this.environmentData = environmentData;
    }
    
    public String getCurrentStepId() {
        return currentStepId;
    }
    
    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }
    
    public StepResult getLastStepResult() {
        return lastStepResult;
    }
    
    public void setLastStepResult(StepResult lastStepResult) {
        this.lastStepResult = lastStepResult;
    }
    
    public List<TestFailure> getRecentFailures() {
        return recentFailures;
    }
    
    public void setRecentFailures(List<TestFailure> recentFailures) {
        this.recentFailures = recentFailures;
    }
    
    public double getSystemLoad() {
        return systemLoad;
    }
    
    public void setSystemLoad(double systemLoad) {
        this.systemLoad = systemLoad;
    }
    
    public Map<String, ServiceStatus> getServiceStatuses() {
        return serviceStatuses;
    }
    
    public void setServiceStatuses(Map<String, ServiceStatus> serviceStatuses) {
        this.serviceStatuses = serviceStatuses;
    }
    
    @Override
    public String toString() {
        return String.format("DecisionContext{step='%s', patterns=%d, history=%d, load=%.2f}", 
                           currentStepId, getSimilarPatternCount(), getExecutionHistoryCount(), systemLoad);
    }
}