package com.agentic.e2etester.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAuditServiceTest {
    
    private DefaultAuditService auditService;
    private SecurityContext testContext;
    
    @BeforeEach
    void setUp() {
        auditService = new DefaultAuditService();
        testContext = new SecurityContext(
            "test-user",
            "session-1",
            Set.of(SecurityRole.TEST_EXECUTOR),
            Set.of("test:execute"),
            Instant.now(),
            "PASSWORD",
            "127.0.0.1"
        );
    }
    
    @Test
    void testRecordEvent() {
        // When
        auditService.recordEvent(
            "user-1",
            "session-1",
            AuditEventType.AUTHENTICATION,
            "login",
            "LOGIN",
            AuditResult.SUCCESS,
            "127.0.0.1",
            "test-agent",
            Map.of("method", "password")
        );
        
        // Then
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> events = auditService.getAuditEvents("user-1", fromTime, toTime);
        
        assertEquals(1, events.size());
        AuditEvent event = events.get(0);
        assertEquals("user-1", event.getUserId());
        assertEquals("session-1", event.getSessionId());
        assertEquals(AuditEventType.AUTHENTICATION, event.getEventType());
        assertEquals("login", event.getResource());
        assertEquals("LOGIN", event.getAction());
        assertEquals(AuditResult.SUCCESS, event.getResult());
        assertEquals("127.0.0.1", event.getClientIpAddress());
        assertEquals("test-agent", event.getUserAgent());
        assertEquals("password", event.getAdditionalData().get("method"));
    }
    
    @Test
    void testRecordAuthenticationEvent() {
        // When
        auditService.recordAuthenticationEvent(
            "user-1",
            AuditResult.SUCCESS,
            "127.0.0.1",
            "test-agent",
            "PASSWORD"
        );
        
        // Then
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> events = auditService.getAuditEventsByType(AuditEventType.AUTHENTICATION, fromTime, toTime);
        
        assertEquals(1, events.size());
        AuditEvent event = events.get(0);
        assertEquals(AuditEventType.AUTHENTICATION, event.getEventType());
        assertEquals("PASSWORD", event.getAdditionalData().get("authenticationType"));
    }
    
    @Test
    void testRecordTestExecutionEvent() {
        // When
        auditService.recordTestExecutionEvent(
            testContext,
            "test-123",
            "EXECUTE",
            AuditResult.SUCCESS,
            Map.of("duration", "30s")
        );
        
        // Then
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> events = auditService.getAuditEventsByType(AuditEventType.TEST_EXECUTION, fromTime, toTime);
        
        assertEquals(1, events.size());
        AuditEvent event = events.get(0);
        assertEquals(AuditEventType.TEST_EXECUTION, event.getEventType());
        assertEquals("test:test-123", event.getResource());
        assertEquals("test-123", event.getAdditionalData().get("testId"));
        assertEquals("30s", event.getAdditionalData().get("duration"));
    }
    
    @Test
    void testRecordCredentialEvent() {
        // When
        auditService.recordCredentialEvent(
            testContext,
            "cred-123",
            "READ",
            AuditResult.SUCCESS
        );
        
        // Then
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> events = auditService.getAuditEventsByType(AuditEventType.CREDENTIAL_MANAGEMENT, fromTime, toTime);
        
        assertEquals(1, events.size());
        AuditEvent event = events.get(0);
        assertEquals(AuditEventType.CREDENTIAL_MANAGEMENT, event.getEventType());
        assertEquals("credential:cred-123", event.getResource());
        assertEquals("cred-123", event.getAdditionalData().get("credentialId"));
    }
    
    @Test
    void testRecordSecurityViolation() {
        // When
        auditService.recordSecurityViolation(
            "user-1",
            "UNAUTHORIZED_ACCESS",
            "Attempted to access restricted resource",
            "127.0.0.1",
            "HIGH"
        );
        
        // Then
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> events = auditService.getAuditEventsByType(AuditEventType.SECURITY_VIOLATION, fromTime, toTime);
        
        assertEquals(1, events.size());
        AuditEvent event = events.get(0);
        assertEquals(AuditEventType.SECURITY_VIOLATION, event.getEventType());
        assertEquals(AuditResult.SECURITY_BLOCKED, event.getResult());
        assertEquals("HIGH", event.getRiskLevel());
        assertEquals("UNAUTHORIZED_ACCESS", event.getAdditionalData().get("violationType"));
    }
    
    @Test
    void testGetAuditEvents() {
        // Given
        auditService.recordAuthenticationEvent("user-1", AuditResult.SUCCESS, "127.0.0.1", "agent", "PASSWORD");
        auditService.recordAuthenticationEvent("user-2", AuditResult.SUCCESS, "127.0.0.1", "agent", "PASSWORD");
        auditService.recordAuthenticationEvent("user-1", AuditResult.AUTHENTICATION_FAILURE, "127.0.0.1", "agent", "PASSWORD");
        
        // When
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> user1Events = auditService.getAuditEvents("user-1", fromTime, toTime);
        
        // Then
        assertEquals(2, user1Events.size());
        assertTrue(user1Events.stream().allMatch(e -> e.getUserId().equals("user-1")));
    }
    
    @Test
    void testGetAuditEventsByType() {
        // Given
        auditService.recordAuthenticationEvent("user-1", AuditResult.SUCCESS, "127.0.0.1", "agent", "PASSWORD");
        auditService.recordTestExecutionEvent(testContext, "test-1", "EXECUTE", AuditResult.SUCCESS, Map.of());
        
        // When
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> authEvents = auditService.getAuditEventsByType(AuditEventType.AUTHENTICATION, fromTime, toTime);
        List<AuditEvent> testEvents = auditService.getAuditEventsByType(AuditEventType.TEST_EXECUTION, fromTime, toTime);
        
        // Then
        assertEquals(1, authEvents.size());
        assertEquals(1, testEvents.size());
        assertEquals(AuditEventType.AUTHENTICATION, authEvents.get(0).getEventType());
        assertEquals(AuditEventType.TEST_EXECUTION, testEvents.get(0).getEventType());
    }
    
    @Test
    void testGetHighRiskEvents() {
        // Given
        auditService.recordSecurityViolation("user-1", "BREACH", "Security breach", "127.0.0.1", "HIGH");
        auditService.recordAuthenticationEvent("user-2", AuditResult.SUCCESS, "127.0.0.1", "agent", "PASSWORD");
        
        // When
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        List<AuditEvent> highRiskEvents = auditService.getHighRiskEvents(fromTime, toTime);
        
        // Then
        assertEquals(1, highRiskEvents.size());
        assertEquals("HIGH", highRiskEvents.get(0).getRiskLevel());
        assertEquals(AuditEventType.SECURITY_VIOLATION, highRiskEvents.get(0).getEventType());
    }
    
    @Test
    void testGenerateComplianceReport() {
        // Given
        auditService.recordAuthenticationEvent("user-1", AuditResult.SUCCESS, "127.0.0.1", "agent", "PASSWORD");
        auditService.recordAuthenticationEvent("user-2", AuditResult.AUTHENTICATION_FAILURE, "127.0.0.1", "agent", "PASSWORD");
        auditService.recordSecurityViolation("user-3", "VIOLATION", "Test violation", "127.0.0.1", "HIGH");
        auditService.recordTestExecutionEvent(testContext, "test-1", "EXECUTE", AuditResult.SUCCESS, Map.of());
        
        // When
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        ComplianceReport report = auditService.generateComplianceReport(fromTime, toTime);
        
        // Then
        assertEquals(4, report.getTotalEvents());
        assertEquals(2, report.getSuccessfulEvents()); // 1 auth success + 1 test success
        assertEquals(2, report.getFailedEvents());
        assertEquals(1, report.getSecurityViolations());
        
        assertTrue(report.getEventsByType().containsKey(AuditEventType.AUTHENTICATION));
        assertTrue(report.getEventsByType().containsKey(AuditEventType.SECURITY_VIOLATION));
        assertTrue(report.getEventsByType().containsKey(AuditEventType.TEST_EXECUTION));
        
        assertTrue(report.getEventsByUser().containsKey("user-1"));
        assertTrue(report.getEventsByUser().containsKey("user-2"));
        assertTrue(report.getEventsByUser().containsKey("user-3"));
        assertTrue(report.getEventsByUser().containsKey("test-user"));
        
        assertEquals(1, report.getHighRiskEvents().size());
        assertTrue(report.getComplianceFindings().contains("Security violations detected: 1"));
        
        assertEquals(0.5, report.getSuccessRate(), 0.01); // 2 successful out of 4 total
        assertEquals(0.5, report.getFailureRate(), 0.01); // 2 failed out of 4 total
        
        // Compliance score should be reduced due to security violations
        assertTrue(report.getComplianceScore() < 50.0); // 50% success rate - 10 point penalty for violation
    }
    
    @Test
    void testGenerateComplianceReportWithNoEvents() {
        // When
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        ComplianceReport report = auditService.generateComplianceReport(fromTime, toTime);
        
        // Then
        assertEquals(0, report.getTotalEvents());
        assertEquals(0, report.getSuccessfulEvents());
        assertEquals(0, report.getFailedEvents());
        assertEquals(0, report.getSecurityViolations());
        assertEquals(100.0, report.getComplianceScore()); // Perfect score with no events
        assertTrue(report.getComplianceFindings().isEmpty());
    }
}