package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.model.TestExecutionPlan;

import java.time.Instant;

/**
 * Context object that maintains state during test orchestration
 */
public class OrchestrationContext {
    
    private String orchestrationId;
    private TestExecutionPlan plan;
    private OrchestrationStatus status;
    private String correlationId;
    private Instant createdTime;
    private Instant startTime;
    private Instant endTime;
    private Exception error;
    
    public OrchestrationContext() {
    }
    
    public String getOrchestrationId() {
        return orchestrationId;
    }
    
    public void setOrchestrationId(String orchestrationId) {
        this.orchestrationId = orchestrationId;
    }
    
    public TestExecutionPlan getPlan() {
        return plan;
    }
    
    public void setPlan(TestExecutionPlan plan) {
        this.plan = plan;
    }
    
    public OrchestrationStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrchestrationStatus status) {
        this.status = status;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Instant getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
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
    
    public Exception getError() {
        return error;
    }
    
    public void setError(Exception error) {
        this.error = error;
    }
    
    @Override
    public String toString() {
        return "OrchestrationContext{" +
                "orchestrationId='" + orchestrationId + '\'' +
                ", status=" + status +
                ", correlationId='" + correlationId + '\'' +
                ", createdTime=" + createdTime +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}