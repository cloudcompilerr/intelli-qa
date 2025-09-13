package com.agentic.e2etester.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for audit logging and compliance tracking.
 */
public interface AuditService {
    
    /**
     * Records an audit event.
     *
     * @param userId the user ID
     * @param sessionId the session ID
     * @param eventType the event type
     * @param resource the resource accessed
     * @param action the action performed
     * @param result the result of the action
     * @param clientIpAddress the client IP address
     * @param userAgent the user agent
     * @param additionalData additional event data
     */
    void recordEvent(String userId, String sessionId, AuditEventType eventType,
                    String resource, String action, AuditResult result,
                    String clientIpAddress, String userAgent, Map<String, Object> additionalData);
    
    /**
     * Records an authentication event.
     *
     * @param userId the user ID
     * @param result the authentication result
     * @param clientIpAddress the client IP address
     * @param userAgent the user agent
     * @param authenticationType the authentication type used
     */
    void recordAuthenticationEvent(String userId, AuditResult result,
                                  String clientIpAddress, String userAgent, String authenticationType);
    
    /**
     * Records a test execution event.
     *
     * @param securityContext the security context
     * @param testId the test ID
     * @param action the test action
     * @param result the result
     * @param additionalData additional test data
     */
    void recordTestExecutionEvent(SecurityContext securityContext, String testId,
                                 String action, AuditResult result, Map<String, Object> additionalData);
    
    /**
     * Records a credential management event.
     *
     * @param securityContext the security context
     * @param credentialId the credential ID
     * @param action the credential action
     * @param result the result
     */
    void recordCredentialEvent(SecurityContext securityContext, String credentialId,
                              String action, AuditResult result);
    
    /**
     * Records a security violation event.
     *
     * @param userId the user ID
     * @param violationType the violation type
     * @param description the violation description
     * @param clientIpAddress the client IP address
     * @param riskLevel the risk level
     */
    void recordSecurityViolation(String userId, String violationType, String description,
                                String clientIpAddress, String riskLevel);
    
    /**
     * Retrieves audit events for a specific user.
     *
     * @param userId the user ID
     * @param fromTime the start time
     * @param toTime the end time
     * @return list of audit events
     */
    List<AuditEvent> getAuditEvents(String userId, Instant fromTime, Instant toTime);
    
    /**
     * Retrieves audit events by type.
     *
     * @param eventType the event type
     * @param fromTime the start time
     * @param toTime the end time
     * @return list of audit events
     */
    List<AuditEvent> getAuditEventsByType(AuditEventType eventType, Instant fromTime, Instant toTime);
    
    /**
     * Retrieves high-risk audit events.
     *
     * @param fromTime the start time
     * @param toTime the end time
     * @return list of high-risk audit events
     */
    List<AuditEvent> getHighRiskEvents(Instant fromTime, Instant toTime);
    
    /**
     * Generates a compliance report.
     *
     * @param fromTime the start time
     * @param toTime the end time
     * @return compliance report
     */
    ComplianceReport generateComplianceReport(Instant fromTime, Instant toTime);
}