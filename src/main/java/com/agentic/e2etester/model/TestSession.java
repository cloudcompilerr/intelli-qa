package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an active test session managed by the AI agent.
 * Contains the test plan, execution state, and session metadata.
 */
public class TestSession {
    
    private String sessionId;
    private TestExecutionPlan testPlan;
    private TestSessionStatus status;
    private Instant startTime;
    private Instant endTime;
    private String correlationId;
    private Map<String, Object> sessionData;
    private List<TestDecision> decisions;
    private List<String> executionLog;
    private TestResult currentResult;
    private String errorMessage;
    
    public TestSession() {
        this.sessionData = new HashMap<>();
        this.decisions = new ArrayList<>();
        this.executionLog = new ArrayList<>();
        this.status = TestSessionStatus.INITIALIZED;
    }
    
    public TestSession(String sessionId, TestExecutionPlan testPlan) {
        this();
        this.sessionId = sessionId;
        this.testPlan = testPlan;
    }
    
    /**
     * Adds a decision made during this session.
     */
    public void addDecision(TestDecision decision) {
        decision.setSessionId(this.sessionId);
        this.decisions.add(decision);
    }
    
    /**
     * Adds an entry to the execution log.
     */
    public void addLogEntry(String entry) {
        String timestampedEntry = String.format("[%s] %s", Instant.now(), entry);
        this.executionLog.add(timestampedEntry);
    }
    
    /**
     * Sets session data value.
     */
    public void setSessionData(String key, Object value) {
        this.sessionData.put(key, value);
    }
    
    /**
     * Gets session data value.
     */
    public Object getSessionData(String key) {
        return this.sessionData.get(key);
    }
    
    /**
     * Checks if session has specific data.
     */
    public boolean hasSessionData(String key) {
        return this.sessionData.containsKey(key);
    }
    
    /**
     * Calculates the duration of the session.
     */
    public long getDurationMs() {
        if (startTime == null) {
            return 0;
        }
        Instant end = endTime != null ? endTime : Instant.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }
    
    /**
     * Gets the number of decisions made in this session.
     */
    public int getDecisionCount() {
        return decisions.size();
    }
    
    /**
     * Gets the latest decision made in this session.
     */
    public TestDecision getLatestDecision() {
        return decisions.isEmpty() ? null : decisions.get(decisions.size() - 1);
    }
    
    /**
     * Checks if the session is active (running or paused).
     */
    public boolean isActive() {
        return status == TestSessionStatus.RUNNING || status == TestSessionStatus.PAUSED;
    }
    
    /**
     * Checks if the session is completed (success or failure).
     */
    public boolean isCompleted() {
        return status == TestSessionStatus.COMPLETED || status == TestSessionStatus.FAILED;
    }
    
    // Getters and Setters
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public TestExecutionPlan getTestPlan() {
        return testPlan;
    }
    
    public void setTestPlan(TestExecutionPlan testPlan) {
        this.testPlan = testPlan;
    }
    
    public TestSessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(TestSessionStatus status) {
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
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Map<String, Object> getSessionData() {
        return sessionData;
    }
    
    public void setSessionData(Map<String, Object> sessionData) {
        this.sessionData = sessionData;
    }
    
    public List<TestDecision> getDecisions() {
        return decisions;
    }
    
    public void setDecisions(List<TestDecision> decisions) {
        this.decisions = decisions;
    }
    
    public List<String> getExecutionLog() {
        return executionLog;
    }
    
    public void setExecutionLog(List<String> executionLog) {
        this.executionLog = executionLog;
    }
    
    public TestResult getCurrentResult() {
        return currentResult;
    }
    
    public void setCurrentResult(TestResult currentResult) {
        this.currentResult = currentResult;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return String.format("TestSession{id='%s', status=%s, decisions=%d, duration=%dms}", 
                           sessionId, status, getDecisionCount(), getDurationMs());
    }
}