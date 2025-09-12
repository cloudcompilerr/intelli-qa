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
 * Represents a correlation trace that tracks the flow of a request across multiple services.
 * Used for distributed tracing and correlation analysis.
 */
public class CorrelationTrace {
    
    @NotBlank(message = "Correlation ID cannot be blank")
    @Size(max = 100, message = "Correlation ID cannot exceed 100 characters")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @NotBlank(message = "Root service cannot be blank")
    @JsonProperty("rootService")
    private String rootService;
    
    @JsonProperty("traceSpans")
    private List<TraceSpan> traceSpans;
    
    @JsonProperty("startTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startTime;
    
    @JsonProperty("endTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant endTime;
    
    @JsonProperty("totalDurationMs")
    private long totalDurationMs;
    
    @NotNull(message = "Trace status cannot be null")
    @JsonProperty("status")
    private TraceStatus status;
    
    @JsonProperty("errorDetails")
    private String errorDetails;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    // Default constructor for Jackson
    public CorrelationTrace() {
        this.startTime = Instant.now();
        this.status = TraceStatus.ACTIVE;
    }
    
    // Constructor with required fields
    public CorrelationTrace(String correlationId, String rootService) {
        this();
        this.correlationId = correlationId;
        this.rootService = rootService;
    }
    
    // Getters and setters
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getRootService() {
        return rootService;
    }
    
    public void setRootService(String rootService) {
        this.rootService = rootService;
    }
    
    public List<TraceSpan> getTraceSpans() {
        return traceSpans;
    }
    
    public void setTraceSpans(List<TraceSpan> traceSpans) {
        this.traceSpans = traceSpans;
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
            this.totalDurationMs = endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }
    
    public long getTotalDurationMs() {
        return totalDurationMs;
    }
    
    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }
    
    public TraceStatus getStatus() {
        return status;
    }
    
    public void setStatus(TraceStatus status) {
        this.status = status;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    // Utility methods
    public void addSpan(TraceSpan span) {
        if (this.traceSpans != null) {
            this.traceSpans.add(span);
        }
    }
    
    public void completeTrace() {
        this.endTime = Instant.now();
        this.status = TraceStatus.COMPLETED;
        if (this.startTime != null) {
            this.totalDurationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }
    
    public void failTrace(String errorDetails) {
        this.endTime = Instant.now();
        this.status = TraceStatus.FAILED;
        this.errorDetails = errorDetails;
        if (this.startTime != null) {
            this.totalDurationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CorrelationTrace that = (CorrelationTrace) o;
        return Objects.equals(correlationId, that.correlationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }
    
    @Override
    public String toString() {
        return "CorrelationTrace{" +
               "correlationId='" + correlationId + '\'' +
               ", rootService='" + rootService + '\'' +
               ", status=" + status +
               ", totalDurationMs=" + totalDurationMs +
               ", spansCount=" + (traceSpans != null ? traceSpans.size() : 0) +
               '}';
    }
}