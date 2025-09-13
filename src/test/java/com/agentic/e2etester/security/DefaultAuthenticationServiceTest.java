package com.agentic.e2etester.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class DefaultAuthenticationServiceTest {
    
    @Mock
    private AuditService auditService;
    
    private DefaultAuthenticationService authenticationService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticationService = new DefaultAuthenticationService(auditService);
    }
    
    @Test
    void testSuccessfulAuthentication() {
        // When
        AuthenticationResult result = authenticationService.authenticate(
            "admin", "password", "127.0.0.1", "test-agent"
        );
        
        // Then
        assertTrue(result.isSuccessful());
        assertTrue(result.getSecurityContext().isPresent());
        assertTrue(result.getSessionId().isPresent());
        
        SecurityContext context = result.getSecurityContext().get();
        assertEquals("admin", context.getUserId());
        assertTrue(context.hasRole(SecurityRole.ADMIN));
        assertEquals("127.0.0.1", context.getClientIpAddress());
        
        verify(auditService).recordAuthenticationEvent(anyString(), any(), anyString(), anyString(), anyString());
    }
    
    @Test
    void testFailedAuthentication() {
        // When
        AuthenticationResult result = authenticationService.authenticate(
            "admin", "wrong-password", "127.0.0.1", "test-agent"
        );
        
        // Then
        assertFalse(result.isSuccessful());
        assertTrue(result.getFailureReason().isPresent());
        assertTrue(result.getSecurityContext().isEmpty());
        assertTrue(result.getSessionId().isEmpty());
        
        verify(auditService).recordAuthenticationEvent(anyString(), any(), anyString(), anyString(), anyString());
    }
    
    @Test
    void testAuthenticationWithNonExistentUser() {
        // When
        AuthenticationResult result = authenticationService.authenticate(
            "non-existent", "password", "127.0.0.1", "test-agent"
        );
        
        // Then
        assertFalse(result.isSuccessful());
        assertEquals("Invalid credentials", result.getFailureReason().orElse(""));
    }
    
    @Test
    void testApiKeyAuthentication() {
        // When
        AuthenticationResult result = authenticationService.authenticateWithApiKey(
            "test-api-key-123", "127.0.0.1", "test-agent"
        );
        
        // Then
        assertTrue(result.isSuccessful());
        assertTrue(result.getSecurityContext().isPresent());
        
        SecurityContext context = result.getSecurityContext().get();
        assertEquals("api-user", context.getUserId());
        assertTrue(context.hasRole(SecurityRole.CICD_SYSTEM));
        assertEquals("API_KEY", context.getAuthenticationType());
    }
    
    @Test
    void testInvalidApiKeyAuthentication() {
        // When
        AuthenticationResult result = authenticationService.authenticateWithApiKey(
            "invalid-api-key", "127.0.0.1", "test-agent"
        );
        
        // Then
        assertFalse(result.isSuccessful());
        assertEquals("Invalid API key", result.getFailureReason().orElse(""));
    }
    
    @Test
    void testTokenAuthentication() {
        // When
        AuthenticationResult result = authenticationService.authenticateWithToken(
            "jwt-token", "127.0.0.1", "test-agent"
        );
        
        // Then
        assertFalse(result.isSuccessful());
        assertEquals("Token authentication not implemented", result.getFailureReason().orElse(""));
    }
    
    @Test
    void testValidateSession() {
        // Given
        AuthenticationResult authResult = authenticationService.authenticate(
            "admin", "password", "127.0.0.1", "test-agent"
        );
        String sessionId = authResult.getSessionId().orElseThrow();
        
        // When
        Optional<SecurityContext> context = authenticationService.validateSession(sessionId);
        
        // Then
        assertTrue(context.isPresent());
        assertEquals("admin", context.get().getUserId());
    }
    
    @Test
    void testValidateInvalidSession() {
        // When
        Optional<SecurityContext> context = authenticationService.validateSession("invalid-session");
        
        // Then
        assertTrue(context.isEmpty());
    }
    
    @Test
    void testInvalidateSession() {
        // Given
        AuthenticationResult authResult = authenticationService.authenticate(
            "admin", "password", "127.0.0.1", "test-agent"
        );
        String sessionId = authResult.getSessionId().orElseThrow();
        
        // When
        authenticationService.invalidateSession(sessionId);
        
        // Then
        Optional<SecurityContext> context = authenticationService.validateSession(sessionId);
        assertTrue(context.isEmpty());
    }
    
    @Test
    void testRefreshSession() {
        // Given
        AuthenticationResult authResult = authenticationService.authenticate(
            "admin", "password", "127.0.0.1", "test-agent"
        );
        String sessionId = authResult.getSessionId().orElseThrow();
        
        // When
        Optional<SecurityContext> refreshedContext = authenticationService.refreshSession(sessionId);
        
        // Then
        assertTrue(refreshedContext.isPresent());
        assertEquals("admin", refreshedContext.get().getUserId());
    }
    
    @Test
    void testAccountLockingAfterFailedAttempts() {
        // Given - Make multiple failed attempts
        for (int i = 0; i < 5; i++) {
            authenticationService.authenticate("admin", "wrong-password", "127.0.0.1", "test-agent");
        }
        
        // When
        boolean isLocked = authenticationService.isAccountLocked("admin");
        
        // Then
        assertTrue(isLocked);
        
        // Verify that even correct password fails when locked
        AuthenticationResult result = authenticationService.authenticate(
            "admin", "password", "127.0.0.1", "test-agent"
        );
        assertFalse(result.isSuccessful());
        assertEquals("Account is locked", result.getFailureReason().orElse(""));
    }
    
    @Test
    void testRecordFailedAttempt() {
        // When
        authenticationService.recordFailedAttempt("test-user", "127.0.0.1");
        
        // Then - Should not be locked after just one attempt
        assertFalse(authenticationService.isAccountLocked("test-user"));
    }
    
    @Test
    void testIsAccountLockedForNonExistentUser() {
        // When
        boolean isLocked = authenticationService.isAccountLocked("non-existent-user");
        
        // Then
        assertFalse(isLocked);
    }
}