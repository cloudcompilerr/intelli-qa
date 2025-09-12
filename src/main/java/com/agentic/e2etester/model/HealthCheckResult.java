package com.agentic.e2etester.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Represents the result of a health check operation
 */
public class HealthCheckResult {
    private String serviceId;
    private ServiceStatus status;
    private Instant timestamp;
    private Duration responseTime;
    private String statusMessage;
    private Map<String, Object> details;
    private Throwable error;
    
    public HealthCheckResult() {}
    
    public HealthCheckResult(String serviceId, ServiceStatus status) {
        this.serviceId = serviceId;
        this.status = status;
        this.timestamp = Instant.now();
    }
    
    public static HealthCheckResult healthy(String serviceId) {
        return new HealthCheckResult(serviceId, ServiceStatus.HEALTHY);
    }
    
    public static HealthCheckResult unhealthy(String serviceId, String message) {
        HealthCheckResult result = new HealthCheckResult(serviceId, ServiceStatus.UNHEALTHY);
        result.setStatusMessage(message);
        return result;
    }
    
    public static HealthCheckResult unhealthy(String serviceId, Throwable error) {
        HealthCheckResult result = new HealthCheckResult(serviceId, ServiceStatus.UNHEALTHY);
        result.setError(error);
        result.setStatusMessage(error.getMessage());
        return result;
    }
    
    // Getters and setters
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public ServiceStatus getStatus() {
        return status;
    }
    
    public void setStatus(ServiceStatus status) {
        this.status = status;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Duration getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(Duration responseTime) {
        this.responseTime = responseTime;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
    
    public Throwable getError() {
        return error;
    }
    
    public void setError(Throwable error) {
        this.error = error;
    }
    
    public boolean isHealthy() {
        return status == ServiceStatus.HEALTHY;
    }
    
    @Override
    public String toString() {
        return "HealthCheckResult{" +
                "serviceId='" + serviceId + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", responseTime=" + responseTime +
                '}';
    }
}