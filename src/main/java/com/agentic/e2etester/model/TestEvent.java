package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents an event that occurred during test execution.
 */
public class TestEvent {
    
    @NotBlank(message = "Event ID cannot be blank")
    @Size(max = 100, message = "Event ID cannot exceed 100 characters")
    @JsonProperty("eventId")
    private String eventId;
    
    @NotNull(message = "Event type cannot be null")
    @JsonProperty("type")
    private EventType type;
    
    @NotNull(message = "Timestamp cannot be null")
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("stepId")
    private String stepId;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("severity")
    private EventSeverity severity;
    
    // Default constructor
    public TestEvent() {
        this.timestamp = Instant.now();
        this.severity = EventSeverity.INFO;
    }
    
    // Constructor with required fields
    public TestEvent(String eventId, EventType type, String message) {
        this();
        this.eventId = eventId;
        this.type = type;
        this.message = message;
    }
    
    // Getters and setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public EventType getType() {
        return type;
    }
    
    public void setType(EventType type) {
        this.type = type;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getStepId() {
        return stepId;
    }
    
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public EventSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(EventSeverity severity) {
        this.severity = severity;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestEvent testEvent = (TestEvent) o;
        return Objects.equals(eventId, testEvent.eventId) &&
               type == testEvent.type &&
               Objects.equals(timestamp, testEvent.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId, type, timestamp);
    }
    
    @Override
    public String toString() {
        return "TestEvent{" +
               "eventId='" + eventId + '\'' +
               ", type=" + type +
               ", timestamp=" + timestamp +
               ", message='" + message + '\'' +
               ", source='" + source + '\'' +
               ", severity=" + severity +
               '}';
    }
}