package com.agentic.e2etester.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for security components without Spring context.
 */
class SecurityBasicTest {
    
    @Test
    void testSecurityContextCreation() {
        // Given
        String userId = "test-user";
        String sessionId = "session-123";
        Set<SecurityRole> roles = Set.of(SecurityRole.TEST_EXECUTOR);
        Set<String> permissions = Set.of("test:execute", "test:view");
        Instant authenticatedAt = Instant.now();
        String authenticationType = "PASSWORD";
        String clientIpAddress = "127.0.0.1";
        
        // When
        SecurityContext context = new SecurityContext(
            userId, sessionId, roles, permissions, 
            authenticatedAt, authenticationType, clientIpAddress
        );
        
        // Then
        assertEquals(userId, context.getUserId());
        assertEquals(sessionId, context.getSessionId());
        assertEquals(roles, context.getRoles());
        assertEquals(permissions, context.getPermissions());
        assertEquals(authenticatedAt, context.getAuthenticatedAt());
        assertEquals(authenticationType, context.getAuthenticationType());
        assertEquals(clientIpAddress, context.getClientIpAddress());
        
        assertTrue(context.hasRole(SecurityRole.TEST_EXECUTOR));
        assertFalse(context.hasRole(SecurityRole.ADMIN));
        assertTrue(context.hasPermission("test:execute"));
        assertFalse(context.hasPermission("admin:delete"));
        assertTrue(context.hasAnyRole(SecurityRole.TEST_EXECUTOR, SecurityRole.ADMIN));
    }
    
    @Test
    void testCredentialCreation() {
        // Given
        String credentialId = "cred-123";
        String name = "test-credential";
        CredentialType type = CredentialType.API_KEY;
        String encryptedValue = "encrypted-value";
        String keyId = "key-1";
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plusSeconds(3600);
        Instant lastRotatedAt = createdAt;
        String createdBy = "admin";
        Map<String, String> metadata = Map.of("environment", "test");
        boolean isActive = true;
        
        // When
        Credential credential = new Credential(
            credentialId, name, type, encryptedValue, keyId,
            createdAt, expiresAt, lastRotatedAt, createdBy,
            metadata, isActive
        );
        
        // Then
        assertEquals(credentialId, credential.getCredentialId());
        assertEquals(name, credential.getName());
        assertEquals(type, credential.getType());
        assertEquals(encryptedValue, credential.getEncryptedValue());
        assertEquals(keyId, credential.getKeyId());
        assertEquals(createdAt, credential.getCreatedAt());
        assertEquals(expiresAt, credential.getExpiresAt());
        assertEquals(lastRotatedAt, credential.getLastRotatedAt());
        assertEquals(createdBy, credential.getCreatedBy());
        assertEquals(metadata, credential.getMetadata());
        assertTrue(credential.isActive());
        assertFalse(credential.isExpired());
        assertTrue(credential.needsRotation(0)); // 0 days = immediate rotation needed
    }
    
    @Test
    void testAuditEventCreation() {
        // Given
        String eventId = "event-123";
        String userId = "user-1";
        String sessionId = "session-1";
        AuditEventType eventType = AuditEventType.AUTHENTICATION;
        String resource = "login";
        String action = "LOGIN";
        AuditResult result = AuditResult.SUCCESS;
        Instant timestamp = Instant.now();
        String clientIpAddress = "127.0.0.1";
        String userAgent = "test-agent";
        Map<String, Object> additionalData = Map.of("method", "password");
        String riskLevel = "LOW";
        
        // When
        AuditEvent event = new AuditEvent(
            eventId, userId, sessionId, eventType, resource, action,
            result, timestamp, clientIpAddress, userAgent, additionalData, riskLevel
        );
        
        // Then
        assertEquals(eventId, event.getEventId());
        assertEquals(userId, event.getUserId());
        assertEquals(sessionId, event.getSessionId());
        assertEquals(eventType, event.getEventType());
        assertEquals(resource, event.getResource());
        assertEquals(action, event.getAction());
        assertEquals(result, event.getResult());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals(clientIpAddress, event.getClientIpAddress());
        assertEquals(userAgent, event.getUserAgent());
        assertEquals(additionalData, event.getAdditionalData());
        assertEquals(riskLevel, event.getRiskLevel());
    }
    
    @Test
    void testAuthenticationResultSuccess() {
        // Given
        SecurityContext context = new SecurityContext(
            "user-1", "session-1", Set.of(SecurityRole.ADMIN), Set.of("*"),
            Instant.now(), "PASSWORD", "127.0.0.1"
        );
        String sessionId = "session-1";
        
        // When
        AuthenticationResult result = AuthenticationResult.success(context, sessionId);
        
        // Then
        assertTrue(result.isSuccessful());
        assertTrue(result.getSecurityContext().isPresent());
        assertTrue(result.getSessionId().isPresent());
        assertTrue(result.getFailureReason().isEmpty());
        assertEquals(context, result.getSecurityContext().get());
        assertEquals(sessionId, result.getSessionId().get());
    }
    
    @Test
    void testAuthenticationResultFailure() {
        // Given
        String reason = "Invalid credentials";
        
        // When
        AuthenticationResult result = AuthenticationResult.failure(reason);
        
        // Then
        assertFalse(result.isSuccessful());
        assertTrue(result.getSecurityContext().isEmpty());
        assertTrue(result.getSessionId().isEmpty());
        assertTrue(result.getFailureReason().isPresent());
        assertEquals(reason, result.getFailureReason().get());
    }
    
    @Test
    void testAuthorizationResultAuthorized() {
        // When
        AuthorizationResult result = AuthorizationResult.authorized();
        
        // Then
        assertTrue(result.isAuthorized());
        assertTrue(result.getReason().isEmpty());
        assertTrue(result.getRequiredRole().isEmpty());
        assertTrue(result.getRequiredPermission().isEmpty());
    }
    
    @Test
    void testAuthorizationResultDenied() {
        // Given
        String reason = "Access denied";
        
        // When
        AuthorizationResult result = AuthorizationResult.denied(reason);
        
        // Then
        assertFalse(result.isAuthorized());
        assertTrue(result.getReason().isPresent());
        assertEquals(reason, result.getReason().get());
    }
    
    @Test
    void testAuthorizationResultDeniedMissingRole() {
        // Given
        String requiredRole = "ADMIN";
        
        // When
        AuthorizationResult result = AuthorizationResult.deniedMissingRole(requiredRole);
        
        // Then
        assertFalse(result.isAuthorized());
        assertTrue(result.getRequiredRole().isPresent());
        assertEquals(requiredRole, result.getRequiredRole().get());
        assertEquals("Missing required role", result.getReason().get());
    }
    
    @Test
    void testAuthorizationResultDeniedMissingPermission() {
        // Given
        String requiredPermission = "test:execute";
        
        // When
        AuthorizationResult result = AuthorizationResult.deniedMissingPermission(requiredPermission);
        
        // Then
        assertFalse(result.isAuthorized());
        assertTrue(result.getRequiredPermission().isPresent());
        assertEquals(requiredPermission, result.getRequiredPermission().get());
        assertEquals("Missing required permission", result.getReason().get());
    }
    
    @Test
    void testSecurityContextHolder() {
        // Given
        SecurityContext context = new SecurityContext(
            "user-1", "session-1", Set.of(SecurityRole.ADMIN), Set.of("*"),
            Instant.now(), "PASSWORD", "127.0.0.1"
        );
        
        // When
        assertFalse(SecurityContextHolder.hasContext());
        assertNull(SecurityContextHolder.getContext());
        
        SecurityContextHolder.setContext(context);
        
        // Then
        assertTrue(SecurityContextHolder.hasContext());
        assertEquals(context, SecurityContextHolder.getContext());
        
        // When
        SecurityContextHolder.clearContext();
        
        // Then
        assertFalse(SecurityContextHolder.hasContext());
        assertNull(SecurityContextHolder.getContext());
    }
}