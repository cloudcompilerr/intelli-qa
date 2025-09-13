package com.agentic.e2etester.security;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a secure credential used for test execution.
 */
public class Credential {
    private final String credentialId;
    private final String name;
    private final CredentialType type;
    private final String encryptedValue;
    private final String keyId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant lastRotatedAt;
    private final String createdBy;
    private final Map<String, String> metadata;
    private final boolean isActive;
    
    public Credential(String credentialId, String name, CredentialType type,
                     String encryptedValue, String keyId, Instant createdAt,
                     Instant expiresAt, Instant lastRotatedAt, String createdBy,
                     Map<String, String> metadata, boolean isActive) {
        this.credentialId = credentialId;
        this.name = name;
        this.type = type;
        this.encryptedValue = encryptedValue;
        this.keyId = keyId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastRotatedAt = lastRotatedAt;
        this.createdBy = createdBy;
        this.metadata = metadata;
        this.isActive = isActive;
    }
    
    public String getCredentialId() {
        return credentialId;
    }
    
    public String getName() {
        return name;
    }
    
    public CredentialType getType() {
        return type;
    }
    
    public String getEncryptedValue() {
        return encryptedValue;
    }
    
    public String getKeyId() {
        return keyId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public Instant getLastRotatedAt() {
        return lastRotatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    public boolean needsRotation(int rotationDays) {
        if (lastRotatedAt == null) {
            return true;
        }
        return Instant.now().isAfter(lastRotatedAt.plusSeconds(rotationDays * 24 * 60 * 60));
    }
}