package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a test failure with detailed context for analysis
 */
public class TestFailure {
    private String failureId;
    private String testId;
    private String stepId;
    private FailureType failureType;
    private FailureSeverity severity;
    private String errorMessage;
    private String stackTrace;
    private Instant timestamp;
    private String serviceId;
    private Map<String, Object> context;
    private List<String> logEntries;
    private Map<String, Object> systemState;
    private String correlationId;

    public TestFailure() {}

    public TestFailure(String failureId, String testId, String stepId, FailureType failureType, 
                      FailureSeverity severity, String errorMessage) {
        this.failureId = failureId;
        this.testId = testId;
        this.stepId = stepId;
        this.failureType = failureType;
        this.severity = severity;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
    }

    // Getters and setters
    public String getFailureId() { return failureId; }
    public void setFailureId(String failureId) { this.failureId = failureId; }

    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }

    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public FailureType getFailureType() { return failureType; }
    public void setFailureType(FailureType failureType) { this.failureType = failureType; }

    public FailureSeverity getSeverity() { return severity; }
    public void setSeverity(FailureSeverity severity) { this.severity = severity; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public List<String> getLogEntries() { return logEntries; }
    public void setLogEntries(List<String> logEntries) { this.logEntries = logEntries; }

    public Map<String, Object> getSystemState() { return systemState; }
    public void setSystemState(Map<String, Object> systemState) { this.systemState = systemState; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}