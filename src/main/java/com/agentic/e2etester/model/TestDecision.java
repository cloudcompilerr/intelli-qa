package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents an AI-driven decision made during test execution.
 * Contains the decision type, confidence level, and reasoning.
 */
public class TestDecision {
    
    private DecisionType decisionType;
    private double confidence;
    private String reasoning;
    private Map<String, Object> parameters;
    private Instant timestamp;
    private String sessionId;
    
    public TestDecision() {
        this.parameters = new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    public TestDecision(DecisionType decisionType, double confidence, String reasoning) {
        this();
        this.decisionType = decisionType;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }
    
    /**
     * Creates a failsafe decision for error scenarios.
     */
    public static TestDecision createFailsafeDecision(String reason) {
        return new TestDecision(DecisionType.ABORT, 1.0, "Failsafe decision: " + reason);
    }
    
    /**
     * Creates a continue decision with specified confidence.
     */
    public static TestDecision createContinueDecision(double confidence, String reasoning) {
        return new TestDecision(DecisionType.CONTINUE, confidence, reasoning);
    }
    
    /**
     * Creates a retry decision with parameters.
     */
    public static TestDecision createRetryDecision(int maxRetries, long delayMs, String reasoning) {
        TestDecision decision = new TestDecision(DecisionType.RETRY, 0.8, reasoning);
        decision.addParameter("maxRetries", maxRetries);
        decision.addParameter("delayMs", delayMs);
        return decision;
    }
    
    /**
     * Creates an adapt decision to modify test flow.
     */
    public static TestDecision createAdaptDecision(String adaptationType, String reasoning) {
        TestDecision decision = new TestDecision(DecisionType.ADAPT, 0.7, reasoning);
        decision.addParameter("adaptationType", adaptationType);
        return decision;
    }
    
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
    
    public boolean hasParameter(String key) {
        return this.parameters.containsKey(key);
    }
    
    // Getters and Setters
    
    public DecisionType getDecisionType() {
        return decisionType;
    }
    
    public void setDecisionType(DecisionType decisionType) {
        this.decisionType = decisionType;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String toString() {
        return String.format("TestDecision{type=%s, confidence=%.2f, reasoning='%s'}", 
                           decisionType, confidence, reasoning);
    }
}