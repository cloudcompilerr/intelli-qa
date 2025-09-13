package com.agentic.e2etester.security;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a security audit event for compliance tracking.
 */
public class AuditEvent {
    private final String eventId;
    private final String userId;
    private final String sessionId;
    private final AuditEventType eventType;
    private final String resource;
    private final String action;
    private final AuditResult result;
    private final Instant timestamp;
    private final String clientIpAddress;
    private final String userAgent;
    private final Map<String, Object> additionalData;
    private final String riskLevel;
    
    public AuditEvent(String eventId, String userId, String sessionId, 
                     AuditEventType eventType, String resource, String action,
                     AuditResult result, Instant timestamp, String clientIpAddress,
                     String userAgent, Map<String, Object> additionalData, String riskLevel) {
        this.eventId = eventId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.resource = resource;
        this.action = action;
        this.result = result;
        this.timestamp = timestamp;
        this.clientIpAddress = clientIpAddress;
        this.userAgent = userAgent;
        this.additionalData = additionalData;
        this.riskLevel = riskLevel;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public AuditEventType getEventType() {
        return eventType;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getAction() {
        return action;
    }
    
    public AuditResult getResult() {
        return result;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getClientIpAddress() {
        return clientIpAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
}