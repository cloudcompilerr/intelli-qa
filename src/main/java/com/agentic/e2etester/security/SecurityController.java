package com.agentic.e2etester.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for security operations.
 */
@RestController
@RequestMapping("/api/security")
public class SecurityController {
    
    private final SecurityManager securityManager;
    private final CredentialManager credentialManager;
    private final AuditService auditService;
    
    @Autowired
    public SecurityController(SecurityManager securityManager, 
                             CredentialManager credentialManager,
                             AuditService auditService) {
        this.securityManager = securityManager;
        this.credentialManager = credentialManager;
        this.auditService = auditService;
    }
    
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthenticationResult result = securityManager.authenticate(
            request.getUsername(), 
            request.getPassword(), 
            clientIp, 
            userAgent
        );
        
        if (result.isSuccessful()) {
            return ResponseEntity.ok(Map.of(
                "sessionId", result.getSessionId().orElse(""),
                "userId", result.getSecurityContext().map(SecurityContext::getUserId).orElse(""),
                "roles", result.getSecurityContext().map(SecurityContext::getRoles).orElse(java.util.Set.of())
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", result.getFailureReason().orElse("Authentication failed")));
        }
    }
    
    @PostMapping("/auth/api-key")
    public ResponseEntity<?> authenticateWithApiKey(@RequestBody ApiKeyRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthenticationResult result = securityManager.authenticateWithApiKey(
            request.getApiKey(), 
            clientIp, 
            userAgent
        );
        
        if (result.isSuccessful()) {
            return ResponseEntity.ok(Map.of(
                "sessionId", result.getSessionId().orElse(""),
                "userId", result.getSecurityContext().map(SecurityContext::getUserId).orElse(""),
                "roles", result.getSecurityContext().map(SecurityContext::getRoles).orElse(java.util.Set.of())
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", result.getFailureReason().orElse("Authentication failed")));
        }
    }
    
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@RequestHeader("X-Session-ID") String sessionId) {
        securityManager.invalidateSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @GetMapping("/credentials")
    public ResponseEntity<?> listCredentials(@RequestHeader("X-Session-ID") String sessionId) {
        AuthorizationResult authResult = securityManager.authorizeCredentialAccess(sessionId, "*", "READ");
        if (!authResult.isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", authResult.getReason().orElse("Access denied")));
        }
        
        List<Credential> credentials = credentialManager.listCredentials();
        return ResponseEntity.ok(credentials.stream()
            .map(c -> Map.of(
                "id", c.getCredentialId(),
                "name", c.getName(),
                "type", c.getType(),
                "active", c.isActive(),
                "createdAt", c.getCreatedAt(),
                "lastRotated", c.getLastRotatedAt()
            ))
            .toList());
    }
    
    @PostMapping("/credentials")
    public ResponseEntity<?> createCredential(@RequestHeader("X-Session-ID") String sessionId,
                                             @RequestBody CreateCredentialRequest request) {
        AuthorizationResult authResult = securityManager.authorizeCredentialAccess(sessionId, "*", "WRITE");
        if (!authResult.isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", authResult.getReason().orElse("Access denied")));
        }
        
        try {
            Credential credential = credentialManager.storeCredential(
                request.getName(),
                request.getType(),
                request.getValue(),
                request.getMetadata()
            );
            
            return ResponseEntity.ok(Map.of(
                "id", credential.getCredentialId(),
                "name", credential.getName(),
                "type", credential.getType(),
                "created", true
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create credential: " + e.getMessage()));
        }
    }
    
    @PostMapping("/credentials/{credentialId}/rotate")
    public ResponseEntity<?> rotateCredential(@RequestHeader("X-Session-ID") String sessionId,
                                             @PathVariable String credentialId,
                                             @RequestBody RotateCredentialRequest request) {
        AuthorizationResult authResult = securityManager.authorizeCredentialAccess(sessionId, credentialId, "WRITE");
        if (!authResult.isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", authResult.getReason().orElse("Access denied")));
        }
        
        try {
            Credential rotatedCredential = credentialManager.rotateCredential(credentialId, request.getNewValue());
            return ResponseEntity.ok(Map.of(
                "id", rotatedCredential.getCredentialId(),
                "rotated", true,
                "lastRotated", rotatedCredential.getLastRotatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to rotate credential: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/credentials/{credentialId}")
    public ResponseEntity<?> deleteCredential(@RequestHeader("X-Session-ID") String sessionId,
                                             @PathVariable String credentialId) {
        AuthorizationResult authResult = securityManager.authorizeCredentialAccess(sessionId, credentialId, "DELETE");
        if (!authResult.isAuthorized()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", authResult.getReason().orElse("Access denied")));
        }
        
        try {
            credentialManager.deleteCredential(credentialId);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete credential: " + e.getMessage()));
        }
    }
    
    @GetMapping("/audit/events")
    public ResponseEntity<?> getAuditEvents(@RequestHeader("X-Session-ID") String sessionId,
                                           @RequestParam(required = false) String userId,
                                           @RequestParam(defaultValue = "24") int hours) {
        Optional<SecurityContext> contextOpt = securityManager.getSecurityContext(sessionId);
        if (contextOpt.isEmpty() || !contextOpt.get().hasRole(SecurityRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Admin access required"));
        }
        
        Instant fromTime = Instant.now().minus(hours, ChronoUnit.HOURS);
        Instant toTime = Instant.now();
        
        List<AuditEvent> events;
        if (userId != null) {
            events = auditService.getAuditEvents(userId, fromTime, toTime);
        } else {
            events = auditService.getAuditEventsByType(null, fromTime, toTime);
        }
        
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/audit/compliance-report")
    public ResponseEntity<?> getComplianceReport(@RequestHeader("X-Session-ID") String sessionId,
                                                @RequestParam(defaultValue = "168") int hours) {
        Optional<SecurityContext> contextOpt = securityManager.getSecurityContext(sessionId);
        if (contextOpt.isEmpty() || !contextOpt.get().hasRole(SecurityRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Admin access required"));
        }
        
        Instant fromTime = Instant.now().minus(hours, ChronoUnit.HOURS);
        Instant toTime = Instant.now();
        
        ComplianceReport report = auditService.generateComplianceReport(fromTime, toTime);
        return ResponseEntity.ok(report);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    // Request DTOs
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class ApiKeyRequest {
        private String apiKey;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }
    
    public static class CreateCredentialRequest {
        private String name;
        private CredentialType type;
        private String value;
        private Map<String, String> metadata;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public CredentialType getType() { return type; }
        public void setType(CredentialType type) { this.type = type; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }
    
    public static class RotateCredentialRequest {
        private String newValue;
        
        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }
    }
}