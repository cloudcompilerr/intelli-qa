package com.agentic.e2etester.cicd;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a deployment event that can trigger automated testing.
 */
public class DeploymentEvent {
    
    private String eventId;
    private String platform;
    private String projectId;
    private String environment;
    private String version;
    private String branch;
    private String commitHash;
    private Instant timestamp;
    private DeploymentEventType eventType;
    private Map<String, Object> metadata;
    private Map<String, String> services; // service name -> version mapping
    
    public DeploymentEvent() {}
    
    public DeploymentEvent(String eventId, String platform, String projectId, 
                          String environment, String version, DeploymentEventType eventType) {
        this.eventId = eventId;
        this.platform = platform;
        this.projectId = projectId;
        this.environment = environment;
        this.version = version;
        this.eventType = eventType;
        this.timestamp = Instant.now();
    }
    
    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public DeploymentEventType getEventType() { return eventType; }
    public void setEventType(DeploymentEventType eventType) { this.eventType = eventType; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Map<String, String> getServices() { return services; }
    public void setServices(Map<String, String> services) { this.services = services; }
}