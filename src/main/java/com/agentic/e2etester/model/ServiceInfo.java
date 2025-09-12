package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Represents information about a discovered microservice
 */
public class ServiceInfo {
    private String serviceId;
    private String serviceName;
    private String baseUrl;
    private String version;
    private ServiceStatus status;
    private Instant lastHealthCheck;
    private Map<String, String> metadata;
    private Set<String> endpoints;
    private Map<String, Object> healthDetails;
    
    public ServiceInfo() {}
    
    public ServiceInfo(String serviceId, String serviceName, String baseUrl) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.baseUrl = baseUrl;
        this.status = ServiceStatus.UNKNOWN;
        this.lastHealthCheck = Instant.now();
    }
    
    // Getters and setters
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public ServiceStatus getStatus() {
        return status;
    }
    
    public void setStatus(ServiceStatus status) {
        this.status = status;
    }
    
    public Instant getLastHealthCheck() {
        return lastHealthCheck;
    }
    
    public void setLastHealthCheck(Instant lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public Set<String> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(Set<String> endpoints) {
        this.endpoints = endpoints;
    }
    
    public Map<String, Object> getHealthDetails() {
        return healthDetails;
    }
    
    public void setHealthDetails(Map<String, Object> healthDetails) {
        this.healthDetails = healthDetails;
    }
    
    @Override
    public String toString() {
        return "ServiceInfo{" +
                "serviceId='" + serviceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", status=" + status +
                '}';
    }
}