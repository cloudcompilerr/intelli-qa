package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents an interaction with a microservice during test execution.
 * Captures request, response, timing, and status information.
 */
public class ServiceInteraction {
    
    @NotBlank(message = "Service ID cannot be blank")
    @Size(max = 200, message = "Service ID cannot exceed 200 characters")
    @JsonProperty("serviceId")
    private String serviceId;
    
    @NotNull(message = "Interaction type cannot be null")
    @JsonProperty("type")
    private InteractionType type;
    
    @NotNull(message = "Timestamp cannot be null")
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;
    
    @JsonProperty("request")
    private Object request;
    
    @JsonProperty("response")
    private Object response;
    
    @PositiveOrZero(message = "Response time must be positive or zero")
    @JsonProperty("responseTimeMs")
    private Long responseTimeMs;
    
    @NotNull(message = "Interaction status cannot be null")
    @JsonProperty("status")
    private InteractionStatus status;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("stepId")
    private String stepId;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("metadata")
    private Object metadata;
    
    // Default constructor for Jackson
    public ServiceInteraction() {
        this.timestamp = Instant.now();
    }
    
    // Constructor with required fields
    public ServiceInteraction(String serviceId, InteractionType type, InteractionStatus status) {
        this();
        this.serviceId = serviceId;
        this.type = type;
        this.status = status;
    }
    
    // Getters and setters
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public InteractionType getType() {
        return type;
    }
    
    public void setType(InteractionType type) {
        this.type = type;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Object getRequest() {
        return request;
    }
    
    public void setRequest(Object request) {
        this.request = request;
    }
    
    public Object getResponse() {
        return response;
    }
    
    public void setResponse(Object response) {
        this.response = response;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public Duration getResponseTime() {
        return responseTimeMs != null ? Duration.ofMillis(responseTimeMs) : null;
    }
    
    public void setResponseTime(Duration responseTime) {
        this.responseTimeMs = responseTime != null ? responseTime.toMillis() : null;
    }
    
    public InteractionStatus getStatus() {
        return status;
    }
    
    public void setStatus(InteractionStatus status) {
        this.status = status;
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Object getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInteraction that = (ServiceInteraction) o;
        return Objects.equals(serviceId, that.serviceId) &&
               type == that.type &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(correlationId, that.correlationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serviceId, type, timestamp, correlationId);
    }
    
    @Override
    public String toString() {
        return "ServiceInteraction{" +
               "serviceId='" + serviceId + '\'' +
               ", type=" + type +
               ", timestamp=" + timestamp +
               ", status=" + status +
               ", responseTimeMs=" + responseTimeMs +
               ", correlationId='" + correlationId + '\'' +
               ", stepId='" + stepId + '\'' +
               '}';
    }
}