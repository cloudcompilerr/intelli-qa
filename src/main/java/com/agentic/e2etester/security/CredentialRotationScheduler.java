package com.agentic.e2etester.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Scheduler for automatic credential rotation.
 */
@Component
public class CredentialRotationScheduler {
    
    private final CredentialManager credentialManager;
    private final AuditService auditService;
    
    private static final int DEFAULT_ROTATION_DAYS = 90;
    
    @Autowired
    public CredentialRotationScheduler(CredentialManager credentialManager, AuditService auditService) {
        this.credentialManager = credentialManager;
        this.auditService = auditService;
    }
    
    /**
     * Checks for credentials that need rotation every day at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkCredentialRotation() {
        try {
            List<Credential> credentialsNeedingRotation = credentialManager.findCredentialsNeedingRotation(DEFAULT_ROTATION_DAYS);
            
            if (!credentialsNeedingRotation.isEmpty()) {
                auditService.recordEvent(
                    "system",
                    null,
                    AuditEventType.CREDENTIAL_MANAGEMENT,
                    "credential-rotation",
                    "CHECK",
                    AuditResult.SUCCESS,
                    null,
                    null,
                    Map.of("credentialsNeedingRotation", credentialsNeedingRotation.size())
                );
                
                // Log warning for credentials needing rotation
                for (Credential credential : credentialsNeedingRotation) {
                    auditService.recordEvent(
                        "system",
                        null,
                        AuditEventType.CREDENTIAL_MANAGEMENT,
                        "credential:" + credential.getCredentialId(),
                        "ROTATION_NEEDED",
                        AuditResult.SUCCESS,
                        null,
                        null,
                        Map.of(
                            "credentialName", credential.getName(),
                            "credentialType", credential.getType().toString(),
                            "lastRotated", credential.getLastRotatedAt().toString()
                        )
                    );
                }
            }
        } catch (Exception e) {
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential-rotation",
                "CHECK",
                AuditResult.SYSTEM_FAILURE,
                null,
                null,
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Validates credential integrity every week on Sunday at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void validateCredentialIntegrity() {
        try {
            List<Credential> allCredentials = credentialManager.listCredentials();
            int validCredentials = 0;
            int invalidCredentials = 0;
            
            for (Credential credential : allCredentials) {
                if (credential.isActive()) {
                    boolean isValid = credentialManager.validateCredentialIntegrity(credential.getCredentialId());
                    if (isValid) {
                        validCredentials++;
                    } else {
                        invalidCredentials++;
                        auditService.recordEvent(
                            "system",
                            null,
                            AuditEventType.SECURITY_VIOLATION,
                            "credential:" + credential.getCredentialId(),
                            "INTEGRITY_FAILURE",
                            AuditResult.SECURITY_BLOCKED,
                            null,
                            null,
                            Map.of(
                                "credentialName", credential.getName(),
                                "credentialType", credential.getType().toString()
                            )
                        );
                    }
                }
            }
            
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential-integrity",
                "VALIDATE",
                AuditResult.SUCCESS,
                null,
                null,
                Map.of(
                    "totalCredentials", allCredentials.size(),
                    "validCredentials", validCredentials,
                    "invalidCredentials", invalidCredentials
                )
            );
            
        } catch (Exception e) {
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential-integrity",
                "VALIDATE",
                AuditResult.SYSTEM_FAILURE,
                null,
                null,
                Map.of("error", e.getMessage())
            );
        }
    }
}