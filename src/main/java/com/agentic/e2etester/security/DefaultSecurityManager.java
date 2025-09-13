package com.agentic.e2etester.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of security manager.
 */
@Service
public class DefaultSecurityManager implements SecurityManager {
    
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    
    @Autowired
    public DefaultSecurityManager(AuthenticationService authenticationService,
                                 AuthorizationService authorizationService,
                                 AuditService auditService) {
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }
    
    @Override
    public AuthenticationResult authenticate(String username, String password,
                                           String clientIpAddress, String userAgent) {
        return authenticationService.authenticate(username, password, clientIpAddress, userAgent);
    }
    
    @Override
    public AuthenticationResult authenticateWithApiKey(String apiKey,
                                                      String clientIpAddress, String userAgent) {
        return authenticationService.authenticateWithApiKey(apiKey, clientIpAddress, userAgent);
    }
    
    @Override
    public AuthorizationResult authorizeTestExecution(String sessionId, String testId, String testEnvironment) {
        Optional<SecurityContext> contextOpt = authenticationService.validateSession(sessionId);
        if (contextOpt.isEmpty()) {
            auditService.recordEvent(
                "unknown",
                sessionId,
                AuditEventType.AUTHORIZATION,
                "test:" + testId,
                "EXECUTE",
                AuditResult.AUTHORIZATION_FAILURE,
                null,
                null,
                Map.of("reason", "Invalid session")
            );
            return AuthorizationResult.denied("Invalid session");
        }
        
        SecurityContext context = contextOpt.get();
        AuthorizationResult result = authorizationService.authorizeTestExecution(context, testId, testEnvironment);
        
        auditService.recordEvent(
            context.getUserId(),
            sessionId,
            AuditEventType.AUTHORIZATION,
            "test:" + testId,
            "EXECUTE",
            result.isAuthorized() ? AuditResult.SUCCESS : AuditResult.AUTHORIZATION_FAILURE,
            context.getClientIpAddress(),
            null,
            Map.of("testEnvironment", testEnvironment, "authorized", result.isAuthorized())
        );
        
        return result;
    }
    
    @Override
    public AuthorizationResult authorizeCredentialAccess(String sessionId, String credentialId, String action) {
        Optional<SecurityContext> contextOpt = authenticationService.validateSession(sessionId);
        if (contextOpt.isEmpty()) {
            auditService.recordEvent(
                "unknown",
                sessionId,
                AuditEventType.AUTHORIZATION,
                "credential:" + credentialId,
                action,
                AuditResult.AUTHORIZATION_FAILURE,
                null,
                null,
                Map.of("reason", "Invalid session")
            );
            return AuthorizationResult.denied("Invalid session");
        }
        
        SecurityContext context = contextOpt.get();
        AuthorizationResult result = authorizationService.authorizeCredentialAccess(context, credentialId, action);
        
        auditService.recordEvent(
            context.getUserId(),
            sessionId,
            AuditEventType.AUTHORIZATION,
            "credential:" + credentialId,
            action,
            result.isAuthorized() ? AuditResult.SUCCESS : AuditResult.AUTHORIZATION_FAILURE,
            context.getClientIpAddress(),
            null,
            Map.of("authorized", result.isAuthorized())
        );
        
        return result;
    }
    
    @Override
    public Optional<SecurityContext> getSecurityContext(String sessionId) {
        return authenticationService.validateSession(sessionId);
    }
    
    @Override
    public void invalidateSession(String sessionId) {
        authenticationService.invalidateSession(sessionId);
    }
    
    @Override
    public void recordSecurityEvent(SecurityContext securityContext, AuditEventType eventType,
                                   String resource, String action, AuditResult result,
                                   Map<String, Object> additionalData) {
        auditService.recordEvent(
            securityContext.getUserId(),
            securityContext.getSessionId(),
            eventType,
            resource,
            action,
            result,
            securityContext.getClientIpAddress(),
            null,
            additionalData
        );
    }
}