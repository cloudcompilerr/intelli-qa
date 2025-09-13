package com.agentic.e2etester.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Compliance report containing audit statistics and findings.
 */
public class ComplianceReport {
    private final Instant reportGeneratedAt;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final long totalEvents;
    private final long successfulEvents;
    private final long failedEvents;
    private final long securityViolations;
    private final Map<AuditEventType, Long> eventsByType;
    private final Map<String, Long> eventsByUser;
    private final List<AuditEvent> highRiskEvents;
    private final List<String> complianceFindings;
    private final double complianceScore;
    
    public ComplianceReport(Instant reportGeneratedAt, Instant periodStart, Instant periodEnd,
                           long totalEvents, long successfulEvents, long failedEvents,
                           long securityViolations, Map<AuditEventType, Long> eventsByType,
                           Map<String, Long> eventsByUser, List<AuditEvent> highRiskEvents,
                           List<String> complianceFindings, double complianceScore) {
        this.reportGeneratedAt = reportGeneratedAt;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalEvents = totalEvents;
        this.successfulEvents = successfulEvents;
        this.failedEvents = failedEvents;
        this.securityViolations = securityViolations;
        this.eventsByType = eventsByType;
        this.eventsByUser = eventsByUser;
        this.highRiskEvents = highRiskEvents;
        this.complianceFindings = complianceFindings;
        this.complianceScore = complianceScore;
    }
    
    public Instant getReportGeneratedAt() {
        return reportGeneratedAt;
    }
    
    public Instant getPeriodStart() {
        return periodStart;
    }
    
    public Instant getPeriodEnd() {
        return periodEnd;
    }
    
    public long getTotalEvents() {
        return totalEvents;
    }
    
    public long getSuccessfulEvents() {
        return successfulEvents;
    }
    
    public long getFailedEvents() {
        return failedEvents;
    }
    
    public long getSecurityViolations() {
        return securityViolations;
    }
    
    public Map<AuditEventType, Long> getEventsByType() {
        return eventsByType;
    }
    
    public Map<String, Long> getEventsByUser() {
        return eventsByUser;
    }
    
    public List<AuditEvent> getHighRiskEvents() {
        return highRiskEvents;
    }
    
    public List<String> getComplianceFindings() {
        return complianceFindings;
    }
    
    public double getComplianceScore() {
        return complianceScore;
    }
    
    public double getSuccessRate() {
        return totalEvents > 0 ? (double) successfulEvents / totalEvents : 0.0;
    }
    
    public double getFailureRate() {
        return totalEvents > 0 ? (double) failedEvents / totalEvents : 0.0;
    }
}