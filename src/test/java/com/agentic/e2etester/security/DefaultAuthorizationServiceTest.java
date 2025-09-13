package com.agentic.e2etester.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAuthorizationServiceTest {
    
    private DefaultAuthorizationService authorizationService;
    private SecurityContext adminContext;
    private SecurityContext testExecutorContext;
    private SecurityContext viewerContext;
    
    @BeforeEach
    void setUp() {
        authorizationService = new DefaultAuthorizationService();
        
        adminContext = new SecurityContext(
            "admin",
            "session-1",
            Set.of(SecurityRole.ADMIN),
            Set.of("*"),
            Instant.now(),
            "PASSWORD",
            "127.0.0.1"
        );
        
        testExecutorContext = new SecurityContext(
            "executor",
            "session-2",
            Set.of(SecurityRole.TEST_EXECUTOR),
            Set.of("test:execute", "test:view", "credential:read"),
            Instant.now(),
            "PASSWORD",
            "127.0.0.1"
        );
        
        viewerContext = new SecurityContext(
            "viewer",
            "session-3",
            Set.of(SecurityRole.TEST_VIEWER),
            Set.of("test:view"),
            Instant.now(),
            "PASSWORD",
            "127.0.0.1"
        );
    }
    
    @Test
    void testAdminHasAllAccess() {
        // When/Then
        assertTrue(authorizationService.isAuthorized(adminContext, "test", "execute"));
        assertTrue(authorizationService.isAuthorized(adminContext, "credential", "write"));
        assertTrue(authorizationService.isAuthorized(adminContext, "configuration", "delete"));
        assertTrue(authorizationService.isAuthorized(adminContext, "any-resource", "any-action"));
    }
    
    @Test
    void testTestExecutorPermissions() {
        // When/Then
        assertTrue(authorizationService.isAuthorized(testExecutorContext, "test", "execute"));
        assertTrue(authorizationService.isAuthorized(testExecutorContext, "test", "view"));
        assertTrue(authorizationService.isAuthorized(testExecutorContext, "credential", "read"));
        
        assertFalse(authorizationService.isAuthorized(testExecutorContext, "credential", "write"));
        assertFalse(authorizationService.isAuthorized(testExecutorContext, "configuration", "read"));
    }
    
    @Test
    void testViewerPermissions() {
        // When/Then
        assertTrue(authorizationService.isAuthorized(viewerContext, "test", "view"));
        
        assertFalse(authorizationService.isAuthorized(viewerContext, "test", "execute"));
        assertFalse(authorizationService.isAuthorized(viewerContext, "credential", "read"));
    }
    
    @Test
    void testNullSecurityContext() {
        // When/Then
        assertFalse(authorizationService.isAuthorized(null, "test", "execute"));
        assertFalse(authorizationService.hasRole(null, SecurityRole.ADMIN));
        assertFalse(authorizationService.hasPermission(null, "test:execute"));
    }
    
    @Test
    void testHasRole() {
        // When/Then
        assertTrue(authorizationService.hasRole(adminContext, SecurityRole.ADMIN));
        assertTrue(authorizationService.hasRole(testExecutorContext, SecurityRole.TEST_EXECUTOR));
        assertTrue(authorizationService.hasRole(viewerContext, SecurityRole.TEST_VIEWER));
        
        assertFalse(authorizationService.hasRole(testExecutorContext, SecurityRole.ADMIN));
        assertFalse(authorizationService.hasRole(viewerContext, SecurityRole.TEST_EXECUTOR));
    }
    
    @Test
    void testHasAnyRole() {
        // When/Then
        assertTrue(authorizationService.hasAnyRole(adminContext, SecurityRole.ADMIN, SecurityRole.TEST_EXECUTOR));
        assertTrue(authorizationService.hasAnyRole(testExecutorContext, SecurityRole.ADMIN, SecurityRole.TEST_EXECUTOR));
        assertTrue(authorizationService.hasAnyRole(viewerContext, SecurityRole.TEST_VIEWER, SecurityRole.ADMIN));
        
        assertFalse(authorizationService.hasAnyRole(viewerContext, SecurityRole.ADMIN, SecurityRole.TEST_EXECUTOR));
    }
    
    @Test
    void testHasPermission() {
        // When/Then
        assertTrue(authorizationService.hasPermission(adminContext, "*"));
        assertTrue(authorizationService.hasPermission(testExecutorContext, "test:execute"));
        assertTrue(authorizationService.hasPermission(viewerContext, "test:view"));
        
        assertFalse(authorizationService.hasPermission(testExecutorContext, "credential:write"));
        assertFalse(authorizationService.hasPermission(viewerContext, "test:execute"));
    }
    
    @Test
    void testAuthorizeTestExecution() {
        // When
        AuthorizationResult adminResult = authorizationService.authorizeTestExecution(adminContext, "test-1", "development");
        AuthorizationResult executorResult = authorizationService.authorizeTestExecution(testExecutorContext, "test-1", "development");
        AuthorizationResult viewerResult = authorizationService.authorizeTestExecution(viewerContext, "test-1", "development");
        
        // Then
        assertTrue(adminResult.isAuthorized());
        assertTrue(executorResult.isAuthorized());
        assertFalse(viewerResult.isAuthorized());
    }
    
    @Test
    void testAuthorizeTestExecutionInProduction() {
        // When
        AuthorizationResult adminResult = authorizationService.authorizeTestExecution(adminContext, "test-1", "production");
        AuthorizationResult executorResult = authorizationService.authorizeTestExecution(testExecutorContext, "test-1", "production");
        
        // Then
        assertTrue(adminResult.isAuthorized());
        assertFalse(executorResult.isAuthorized()); // Test executor cannot run tests in production
        assertEquals("ADMIN or CICD_SYSTEM", executorResult.getRequiredRole().orElse(""));
    }
    
    @Test
    void testAuthorizeCredentialAccess() {
        // When
        AuthorizationResult adminRead = authorizationService.authorizeCredentialAccess(adminContext, "cred-1", "READ");
        AuthorizationResult adminWrite = authorizationService.authorizeCredentialAccess(adminContext, "cred-1", "WRITE");
        AuthorizationResult adminDelete = authorizationService.authorizeCredentialAccess(adminContext, "cred-1", "DELETE");
        
        AuthorizationResult executorRead = authorizationService.authorizeCredentialAccess(testExecutorContext, "cred-1", "READ");
        AuthorizationResult executorWrite = authorizationService.authorizeCredentialAccess(testExecutorContext, "cred-1", "WRITE");
        AuthorizationResult executorDelete = authorizationService.authorizeCredentialAccess(testExecutorContext, "cred-1", "DELETE");
        
        // Then
        assertTrue(adminRead.isAuthorized());
        assertTrue(adminWrite.isAuthorized());
        assertTrue(adminDelete.isAuthorized());
        
        assertTrue(executorRead.isAuthorized());
        assertFalse(executorWrite.isAuthorized()); // Only admin can write/delete credentials
        assertFalse(executorDelete.isAuthorized());
    }
    
    @Test
    void testAuthorizeConfigurationAccess() {
        // When
        AuthorizationResult adminResult = authorizationService.authorizeConfigurationAccess(adminContext, "app.timeout", "READ");
        AuthorizationResult executorResult = authorizationService.authorizeConfigurationAccess(testExecutorContext, "app.timeout", "READ");
        
        // Then
        assertTrue(adminResult.isAuthorized());
        assertFalse(executorResult.isAuthorized()); // Test executor doesn't have configuration permissions
    }
    
    @Test
    void testAuthorizeSecurityConfigurationAccess() {
        // When
        AuthorizationResult adminResult = authorizationService.authorizeConfigurationAccess(adminContext, "security.encryption.key", "READ");
        AuthorizationResult executorResult = authorizationService.authorizeConfigurationAccess(testExecutorContext, "security.encryption.key", "READ");
        
        // Then
        assertTrue(adminResult.isAuthorized());
        assertFalse(executorResult.isAuthorized());
    }
    
    @Test
    void testAuthorizeWithNullContext() {
        // When
        AuthorizationResult testResult = authorizationService.authorizeTestExecution(null, "test-1", "development");
        AuthorizationResult credResult = authorizationService.authorizeCredentialAccess(null, "cred-1", "READ");
        AuthorizationResult configResult = authorizationService.authorizeConfigurationAccess(null, "config", "READ");
        
        // Then
        assertFalse(testResult.isAuthorized());
        assertFalse(credResult.isAuthorized());
        assertFalse(configResult.isAuthorized());
        
        assertEquals("No security context", testResult.getReason().orElse(""));
        assertEquals("No security context", credResult.getReason().orElse(""));
        assertEquals("No security context", configResult.getReason().orElse(""));
    }
}