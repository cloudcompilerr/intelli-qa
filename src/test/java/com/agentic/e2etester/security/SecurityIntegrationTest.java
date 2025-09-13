package com.agentic.e2etester.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete security system.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityIntegrationTest {
    
    @Autowired
    private SecurityManager securityManager;
    
    @Autowired
    private CredentialManager credentialManager;
    
    @Autowired
    private AuditService auditService;
    
    @BeforeEach
    void setUp() {
        // Test setup if needed
    }
    
    @Test
    void testCompleteAuthenticationFlow() {
        // Given
        String username = "admin";
        String password = "password";
        String clientIp = "127.0.0.1";
        String userAgent = "test-agent";
        
        // When - Authenticate
        AuthenticationResult authResult = securityManager.authenticate(username, password, clientIp, userAgent);
        
        // Then
        assertTrue(authResult.isSuccessful());
        assertTrue(authResult.getSessionId().isPresent());
        assertTrue(authResult.getSecurityContext().isPresent());
        
        String sessionId = authResult.getSessionId().get();
        SecurityContext context = authResult.getSecurityContext().get();
        
        assertEquals(username, context.getUserId());
        assertTrue(context.hasRole(SecurityRole.ADMIN));
        assertEquals(clientIp, context.getClientIpAddress());
        
        // Verify session validation
        Optional<SecurityContext> validatedContext = securityManager.getSecurityContext(sessionId);
        assertTrue(validatedContext.isPresent());
        assertEquals(username, validatedContext.get().getUserId());
        
        // Verify audit event was recorded
        Instant fromTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant toTime = Instant.now().plus(1, ChronoUnit.MINUTES);
        List<AuditEvent> authEvents = auditService.getAuditEventsByType(AuditEventType.AUTHENTICATION, fromTime, toTime);
        
        assertEquals(1, authEvents.size());
        AuditEvent authEvent = authEvents.get(0);
        assertEquals(username, authEvent.getUserId());
        assertEquals(AuditResult.SUCCESS, authEvent.getResult());
    }
    
    @Test
    void testApiKeyAuthentication() {
        // When
        AuthenticationResult result = securityManager.authenticateWithApiKey("test-api-key-123", "127.0.0.1", "test-agent");
        
        // Then
        assertTrue(result.isSuccessful());
        assertTrue(result.getSecurityContext().isPresent());
        
        SecurityContext context = result.getSecurityContext().get();
        assertEquals("api-user", context.getUserId());
        assertTrue(context.hasRole(SecurityRole.CICD_SYSTEM));
        assertEquals("API_KEY", context.getAuthenticationType());
    }
    
    @Test
    void testTestExecutionAuthorization() {
        // Given - Authenticate as test executor
        AuthenticationResult authResult = securityManager.authenticate("test-executor", "password", "127.0.0.1", "test-agent");
        String sessionId = authResult.getSessionId().orElseThrow();
        
        // When - Authorize test execution in development
        AuthorizationResult devResult = securityManager.authorizeTestExecution(sessionId, "test-123", "development");
        
        // Then
        assertTrue(devResult.isAuthorized());
        
        // When - Try to authorize test execution in production (should fail)
        AuthorizationResult prodResult = securityManager.authorizeTestExecution(sessionId, "test-123", "production");
        
        // Then
        assertFalse(prodResult.isAuthorized());
        assertTrue(prodResult.getRequiredRole().isPresent());
    }
    
    @Test
    void testCredentialManagementFlow() {
        // Given - Authenticate as admin
        AuthenticationResult authResult = securityManager.authenticate("admin", "password", "127.0.0.1", "test-agent");
        String sessionId = authResult.getSessionId().orElseThrow();
        
        // When - Create credential
        Credential credential = credentialManager.storeCredential(
            "test-api-key",
            CredentialType.API_KEY,
            "secret-key-value",
            Map.of("environment", "test")
        );
        
        // Then
        assertNotNull(credential);
        assertEquals("test-api-key", credential.getName());
        assertEquals(CredentialType.API_KEY, credential.getType());
        assertTrue(credential.isActive());
        
        // When - Authorize credential access
        AuthorizationResult readResult = securityManager.authorizeCredentialAccess(sessionId, credential.getCredentialId(), "READ");
        AuthorizationResult writeResult = securityManager.authorizeCredentialAccess(sessionId, credential.getCredentialId(), "WRITE");
        
        // Then
        assertTrue(readResult.isAuthorized());
        assertTrue(writeResult.isAuthorized()); // Admin can write
        
        // When - Retrieve credential value
        Optional<String> value = credentialManager.getCredentialValue(credential.getCredentialId());
        
        // Then
        assertTrue(value.isPresent());
        assertEquals("secret-key-value", value.get());
        
        // When - Rotate credential
        Credential rotatedCredential = credentialManager.rotateCredential(credential.getCredentialId(), "new-secret-value");
        
        // Then
        assertEquals(credential.getCredentialId(), rotatedCredential.getCredentialId());
        assertTrue(rotatedCredential.getLastRotatedAt().isAfter(credential.getLastRotatedAt()));
        
        // Verify new value
        Optional<String> newValue = credentialManager.getCredentialValue(credential.getCredentialId());
        assertTrue(newValue.isPresent());
        assertEquals("new-secret-value", newValue.get());
    }
    
    @Test
    void testAuditingAndCompliance() {
        // Given - Perform various operations
        AuthenticationResult authResult = securityManager.authenticate("admin", "password", "127.0.0.1", "test-agent");
        String sessionId = authResult.getSessionId().orElseThrow();
        SecurityContext context = authResult.getSecurityContext().orElseThrow();
        
        // Create and access credential
        Credential credential = credentialManager.storeCredential("audit-test", CredentialType.SECRET, "secret", null);
        credentialManager.getCredentialValue(credential.getCredentialId());
        
        // Record test execution
        auditService.recordTestExecutionEvent(context, "test-audit", "EXECUTE", AuditResult.SUCCESS, Map.of("duration", "45s"));
        
        // Record security violation
        auditService.recordSecurityViolation("malicious-user", "UNAUTHORIZED_ACCESS", "Attempted breach", "192.168.1.100", "HIGH");
        
        // When - Generate compliance report
        Instant fromTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toTime = Instant.now().plus(1, ChronoUnit.HOURS);
        ComplianceReport report = auditService.generateComplianceReport(fromTime, toTime);
        
        // Then
        assertTrue(report.getTotalEvents() > 0);
        assertTrue(report.getSuccessfulEvents() > 0);
        assertEquals(1, report.getSecurityViolations());
        
        assertTrue(report.getEventsByType().containsKey(AuditEventType.AUTHENTICATION));
        assertTrue(report.getEventsByType().containsKey(AuditEventType.CREDENTIAL_MANAGEMENT));
        assertTrue(report.getEventsByType().containsKey(AuditEventType.TEST_EXECUTION));
        assertTrue(report.getEventsByType().containsKey(AuditEventType.SECURITY_VIOLATION));
        
        assertTrue(report.getEventsByUser().containsKey("admin"));
        assertTrue(report.getEventsByUser().containsKey("malicious-user"));
        
        assertEquals(1, report.getHighRiskEvents().size());
        assertTrue(report.getComplianceFindings().contains("Security violations detected: 1"));
        
        // Compliance score should be reduced due to security violation
        assertTrue(report.getComplianceScore() < 100.0);
    }
    
    @Test
    void testSessionManagement() {
        // Given
        AuthenticationResult authResult = securityManager.authenticate("admin", "password", "127.0.0.1", "test-agent");
        String sessionId = authResult.getSessionId().orElseThrow();
        
        // When - Validate session
        Optional<SecurityContext> context1 = securityManager.getSecurityContext(sessionId);
        
        // Then
        assertTrue(context1.isPresent());
        assertEquals("admin", context1.get().getUserId());
        
        // When - Invalidate session
        securityManager.invalidateSession(sessionId);
        
        // Then - Session should no longer be valid
        Optional<SecurityContext> context2 = securityManager.getSecurityContext(sessionId);
        assertTrue(context2.isEmpty());
    }
    
    @Test
    void testFailedAuthenticationAndLocking() {
        // Given - Make multiple failed attempts
        for (int i = 0; i < 5; i++) {
            AuthenticationResult result = securityManager.authenticate("admin", "wrong-password", "127.0.0.1", "test-agent");
            assertFalse(result.isSuccessful());
        }
        
        // When - Try to authenticate with correct password (should fail due to locking)
        AuthenticationResult lockedResult = securityManager.authenticate("admin", "password", "127.0.0.1", "test-agent");
        
        // Then
        assertFalse(lockedResult.isSuccessful());
        assertEquals("Account is locked", lockedResult.getFailureReason().orElse(""));
        
        // Verify audit events
        Instant fromTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant toTime = Instant.now().plus(1, ChronoUnit.MINUTES);
        List<AuditEvent> authEvents = auditService.getAuditEventsByType(AuditEventType.AUTHENTICATION, fromTime, toTime);
        
        // Should have 6 failed authentication events (5 wrong password + 1 locked account)
        long failedEvents = authEvents.stream()
            .mapToLong(event -> event.getResult() == AuditResult.AUTHENTICATION_FAILURE ? 1 : 0)
            .sum();
        assertEquals(6, failedEvents);
    }
    
    @Test
    void testCredentialRotationNeeded() {
        // Given
        Credential credential = credentialManager.storeCredential("old-cred", CredentialType.API_KEY, "old-value", null);
        
        // When
        List<Credential> needingRotation = credentialManager.findCredentialsNeedingRotation(0); // 0 days = immediate rotation needed
        
        // Then
        assertEquals(1, needingRotation.size());
        assertEquals(credential.getCredentialId(), needingRotation.get(0).getCredentialId());
    }
    
    @Test
    void testCredentialIntegrityValidation() {
        // Given
        Credential credential = credentialManager.storeCredential("integrity-test", CredentialType.SECRET, "test-value", null);
        
        // When
        boolean isValid = credentialManager.validateCredentialIntegrity(credential.getCredentialId());
        
        // Then
        assertTrue(isValid);
        
        // When - Test with invalid credential ID
        boolean isInvalid = credentialManager.validateCredentialIntegrity("non-existent-id");
        
        // Then
        assertFalse(isInvalid);
    }
}