package com.agentic.e2etester.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of audit service for compliance tracking.
 */
@Service
public class DefaultAuditService implements AuditService {
    
    private final Map<String, AuditEvent> auditEvents = new ConcurrentHashMap<>();
    
    @Override
    public void recordEvent(String userId, String sessionId, AuditEventType eventType,
                           String resource, String action, AuditResult result,
                           String clientIpAddress, String userAgent, Map<String, Object> additionalData) {
        String eventId = UUID.randomUUID().toString();
        String riskLevel = calculateRiskLevel(eventType, result, additionalData);
        
        AuditEvent event = new AuditEvent(
            eventId,
            userId,
            sessionId,
            eventType,
            resource,
            action,
            result,
            Instant.now(),
            clientIpAddress,
            userAgent,
            additionalData != null ? additionalData : new HashMap<>(),
            riskLevel
        );
        
        auditEvents.put(eventId, event);
        
        // Log high-risk events immediately
        if ("HIGH".equals(riskLevel)) {
            System.err.println("HIGH RISK AUDIT EVENT: " + event.getEventType() + 
                             " - " + event.getAction() + " by " + event.getUserId());
        }
    }
    
    @Override
    public void recordAuthenticationEvent(String userId, AuditResult result,
                                         String clientIpAddress, String userAgent, String authenticationType) {
        recordEvent(
            userId,
            null,
            AuditEventType.AUTHENTICATION,
            "authentication",
            "LOGIN",
            result,
            clientIpAddress,
            userAgent,
            Map.of("authenticationType", authenticationType)
        );
    }
    
    @Override
    public void recordTestExecutionEvent(SecurityContext securityContext, String testId,
                                        String action, AuditResult result, Map<String, Object> additionalData) {
        Map<String, Object> eventData = new HashMap<>(additionalData != null ? additionalData : new HashMap<>());
        eventData.put("testId", testId);
        
        recordEvent(
            securityContext.getUserId(),
            securityContext.getSessionId(),
            AuditEventType.TEST_EXECUTION,
            "test:" + testId,
            action,
            result,
            securityContext.getClientIpAddress(),
            null,
            eventData
        );
    }
    
    @Override
    public void recordCredentialEvent(SecurityContext securityContext, String credentialId,
                                     String action, AuditResult result) {
        recordEvent(
            securityContext.getUserId(),
            securityContext.getSessionId(),
            AuditEventType.CREDENTIAL_MANAGEMENT,
            "credential:" + credentialId,
            action,
            result,
            securityContext.getClientIpAddress(),
            null,
            Map.of("credentialId", credentialId)
        );
    }
    
    @Override
    public void recordSecurityViolation(String userId, String violationType, String description,
                                       String clientIpAddress, String riskLevel) {
        recordEvent(
            userId,
            null,
            AuditEventType.SECURITY_VIOLATION,
            "security",
            violationType,
            AuditResult.SECURITY_BLOCKED,
            clientIpAddress,
            null,
            Map.of("violationType", violationType, "description", description, "riskLevel", riskLevel)
        );
    }
    
    @Override
    public List<AuditEvent> getAuditEvents(String userId, Instant fromTime, Instant toTime) {
        return auditEvents.values().stream()
            .filter(event -> event.getUserId().equals(userId))
            .filter(event -> event.getTimestamp().isAfter(fromTime) && event.getTimestamp().isBefore(toTime))
            .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditEvent> getAuditEventsByType(AuditEventType eventType, Instant fromTime, Instant toTime) {
        return auditEvents.values().stream()
            .filter(event -> event.getEventType() == eventType)
            .filter(event -> event.getTimestamp().isAfter(fromTime) && event.getTimestamp().isBefore(toTime))
            .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditEvent> getHighRiskEvents(Instant fromTime, Instant toTime) {
        return auditEvents.values().stream()
            .filter(event -> "HIGH".equals(event.getRiskLevel()))
            .filter(event -> event.getTimestamp().isAfter(fromTime) && event.getTimestamp().isBefore(toTime))
            .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public ComplianceReport generateComplianceReport(Instant fromTime, Instant toTime) {
        List<AuditEvent> periodEvents = auditEvents.values().stream()
            .filter(event -> event.getTimestamp().isAfter(fromTime) && event.getTimestamp().isBefore(toTime))
            .collect(Collectors.toList());
        
        long totalEvents = periodEvents.size();
        long successfulEvents = periodEvents.stream()
            .mapToLong(event -> event.getResult() == AuditResult.SUCCESS ? 1 : 0)
            .sum();
        long failedEvents = totalEvents - successfulEvents;
        long securityViolations = periodEvents.stream()
            .mapToLong(event -> event.getEventType() == AuditEventType.SECURITY_VIOLATION ? 1 : 0)
            .sum();
        
        Map<AuditEventType, Long> eventsByType = periodEvents.stream()
            .collect(Collectors.groupingBy(AuditEvent::getEventType, Collectors.counting()));
        
        Map<String, Long> eventsByUser = periodEvents.stream()
            .collect(Collectors.groupingBy(AuditEvent::getUserId, Collectors.counting()));
        
        List<AuditEvent> highRiskEvents = periodEvents.stream()
            .filter(event -> "HIGH".equals(event.getRiskLevel()))
            .collect(Collectors.toList());
        
        List<String> complianceFindings = generateComplianceFindings(periodEvents);
        double complianceScore = calculateComplianceScore(periodEvents);
        
        return new ComplianceReport(
            Instant.now(),
            fromTime,
            toTime,
            totalEvents,
            successfulEvents,
            failedEvents,
            securityViolations,
            eventsByType,
            eventsByUser,
            highRiskEvents,
            complianceFindings,
            complianceScore
        );
    }
    
    private String calculateRiskLevel(AuditEventType eventType, AuditResult result, Map<String, Object> additionalData) {
        // High risk conditions
        if (eventType == AuditEventType.SECURITY_VIOLATION) {
            return "HIGH";
        }
        
        if (result == AuditResult.AUTHENTICATION_FAILURE || result == AuditResult.AUTHORIZATION_FAILURE) {
            return "MEDIUM";
        }
        
        if (eventType == AuditEventType.CREDENTIAL_MANAGEMENT && 
            (result == AuditResult.SYSTEM_FAILURE || result == AuditResult.VALIDATION_FAILURE)) {
            return "HIGH";
        }
        
        if (eventType == AuditEventType.TEST_EXECUTION && result == AuditResult.SUCCESS) {
            return "LOW";
        }
        
        return "MEDIUM";
    }
    
    private List<String> generateComplianceFindings(List<AuditEvent> events) {
        List<String> findings = new ArrayList<>();
        
        long authFailures = events.stream()
            .mapToLong(event -> event.getResult() == AuditResult.AUTHENTICATION_FAILURE ? 1 : 0)
            .sum();
        
        if (authFailures > 10) {
            findings.add("High number of authentication failures detected: " + authFailures);
        }
        
        long securityViolations = events.stream()
            .mapToLong(event -> event.getEventType() == AuditEventType.SECURITY_VIOLATION ? 1 : 0)
            .sum();
        
        if (securityViolations > 0) {
            findings.add("Security violations detected: " + securityViolations);
        }
        
        // Check for unusual activity patterns
        Map<String, Long> userActivity = events.stream()
            .collect(Collectors.groupingBy(AuditEvent::getUserId, Collectors.counting()));
        
        userActivity.entrySet().stream()
            .filter(entry -> entry.getValue() > 100)
            .forEach(entry -> findings.add("High activity from user: " + entry.getKey() + " (" + entry.getValue() + " events)"));
        
        return findings;
    }
    
    private double calculateComplianceScore(List<AuditEvent> events) {
        if (events.isEmpty()) {
            return 100.0;
        }
        
        long successfulEvents = events.stream()
            .mapToLong(event -> event.getResult() == AuditResult.SUCCESS ? 1 : 0)
            .sum();
        
        long securityViolations = events.stream()
            .mapToLong(event -> event.getEventType() == AuditEventType.SECURITY_VIOLATION ? 1 : 0)
            .sum();
        
        double successRate = (double) successfulEvents / events.size();
        double violationPenalty = Math.min(securityViolations * 10.0, 50.0); // Max 50 point penalty
        
        return Math.max(0.0, (successRate * 100.0) - violationPenalty);
    }
}