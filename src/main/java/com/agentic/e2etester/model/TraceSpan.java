package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single span in a distributed trace, capturing the execution of an operation
 * within a specific service.
 */
public class TraceSpan {
    
    @NotBlank(message = "Span ID cannot be blank")
    @Size(max = 100, message = "Span ID cannot exceed 100 characters")
    @JsonProperty("spanId")
    private String spanId;
    
    @JsonProperty("parentSpanId")
    private String parentSpanId;
    
    @NotBlank(message = "Service name cannot be blank")
    @JsonProperty("serviceName")
    private String serviceName;
    
    @NotBlank(message = "Operation name cannot be blank")
    @JsonProperty("operationName")
    private String operationName;
    
    @JsonProperty("startTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startTime;
    
    @JsonProperty("endTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant endTime;
    
    @JsonProperty("durationMs")
    private long durationMs;
    
    @NotNull(message = "Span status cannot be null")
    @JsonProperty("status")
    private SpanStatus status;
    
    @JsonProperty("tags")
    private Map<String, String> tags;
    
    @JsonProperty("logs")
    private Map<String, Object> logs;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    // Default constructor for Jackson
    public TraceSpan() {
        this.startTime = Instant.now();
        this.status = SpanStatus.ACTIVE;
    }
    
    // Constructor with required fields
    public TraceSpan(String spanId, String serviceName, String operationName) {
        this();
        this.spanId = spanId;
        this.serviceName = serviceName;
        this.operationName = operationName;
    }
    
    // Getters and setters
    public String getSpanId() {
        return spanId;
    }
    
    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
    
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getOperationName() {
        return operationName;
    }
    
    public void setOperationName(String operationName) {
        this.operationName = operationName;
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
            this.durationMs = endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public SpanStatus getStatus() {
        return status;
    }
    
    public void setStatus(SpanStatus status) {
        this.status = status;
    }
    
    public Map<String, String> getTags() {
        return tags;
    }
    
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getLogs() {
        return logs;
    }
    
    public void setLogs(Map<String, Object> logs) {
        this.logs = logs;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    // Utility methods
    public void finish() {
        this.endTime = Instant.now();
        this.status = SpanStatus.FINISHED;
        if (this.startTime != null) {
            this.durationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }
    
    public void finishWithError(String errorMessage) {
        this.endTime = Instant.now();
        this.status = SpanStatus.ERROR;
        this.errorMessage = errorMessage;
        if (this.startTime != null) {
            this.durationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }
    
    public void addTag(String key, String value) {
        if (this.tags != null) {
            this.tags.put(key, value);
        }
    }
    
    public void addLog(String key, Object value) {
        if (this.logs != null) {
            this.logs.put(key, value);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceSpan traceSpan = (TraceSpan) o;
        return Objects.equals(spanId, traceSpan.spanId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(spanId);
    }
    
    @Override
    public String toString() {
        return "TraceSpan{" +
               "spanId='" + spanId + '\'' +
               ", serviceName='" + serviceName + '\'' +
               ", operationName='" + operationName + '\'' +
               ", status=" + status +
               ", durationMs=" + durationMs +
               '}';
    }
}