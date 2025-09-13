package com.agentic.e2etester.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of credential manager with AES encryption.
 */
@Service
public class DefaultCredentialManager implements CredentialManager {
    
    private final Map<String, Credential> credentials = new ConcurrentHashMap<>();
    private final Map<String, SecretKey> encryptionKeys = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final AuditService auditService;
    
    @Autowired
    public DefaultCredentialManager(AuditService auditService) {
        this.auditService = auditService;
        initializeDefaultEncryptionKey();
    }
    
    private void initializeDefaultEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey defaultKey = keyGen.generateKey();
            encryptionKeys.put("default", defaultKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }
    
    @Override
    public Credential storeCredential(String name, CredentialType type, String value, 
                                     Map<String, String> metadata) {
        try {
            String credentialId = UUID.randomUUID().toString();
            String keyId = "default";
            String encryptedValue = encrypt(value, keyId);
            
            Credential credential = new Credential(
                credentialId,
                name,
                type,
                encryptedValue,
                keyId,
                Instant.now(),
                null, // No expiration by default
                Instant.now(),
                "system", // TODO: Get from security context
                metadata != null ? metadata : new HashMap<>(),
                true
            );
            
            credentials.put(credentialId, credential);
            
            // Audit the credential creation
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential:" + credentialId,
                "CREATE",
                AuditResult.SUCCESS,
                null,
                null,
                Map.of("credentialName", name, "credentialType", type.toString())
            );
            
            return credential;
        } catch (Exception e) {
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential",
                "CREATE",
                AuditResult.SYSTEM_FAILURE,
                null,
                null,
                Map.of("error", e.getMessage())
            );
            throw new RuntimeException("Failed to store credential", e);
        }
    }
    
    @Override
    public Optional<Credential> getCredential(String name) {
        return credentials.values().stream()
            .filter(c -> c.getName().equals(name) && c.isActive())
            .findFirst();
    }
    
    @Override
    public Optional<String> getCredentialValue(String credentialId) {
        try {
            Credential credential = credentials.get(credentialId);
            if (credential == null || !credential.isActive() || credential.isExpired()) {
                return Optional.empty();
            }
            
            String decryptedValue = decrypt(credential.getEncryptedValue(), credential.getKeyId());
            
            // Audit credential access
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential:" + credentialId,
                "READ",
                AuditResult.SUCCESS,
                null,
                null,
                Map.of("credentialName", credential.getName())
            );
            
            return Optional.of(decryptedValue);
        } catch (Exception e) {
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential:" + credentialId,
                "READ",
                AuditResult.SYSTEM_FAILURE,
                null,
                null,
                Map.of("error", e.getMessage())
            );
            return Optional.empty();
        }
    }
    
    @Override
    public List<Credential> listCredentials() {
        return new ArrayList<>(credentials.values());
    }
    
    @Override
    public List<Credential> listCredentialsByType(CredentialType type) {
        return credentials.values().stream()
            .filter(c -> c.getType() == type && c.isActive())
            .toList();
    }
    
    @Override
    public Credential rotateCredential(String credentialId, String newValue) {
        try {
            Credential existingCredential = credentials.get(credentialId);
            if (existingCredential == null) {
                throw new IllegalArgumentException("Credential not found: " + credentialId);
            }
            
            String encryptedValue = encrypt(newValue, existingCredential.getKeyId());
            
            Credential rotatedCredential = new Credential(
                existingCredential.getCredentialId(),
                existingCredential.getName(),
                existingCredential.getType(),
                encryptedValue,
                existingCredential.getKeyId(),
                existingCredential.getCreatedAt(),
                existingCredential.getExpiresAt(),
                Instant.now(),
                existingCredential.getCreatedBy(),
                existingCredential.getMetadata(),
                existingCredential.isActive()
            );
            
            credentials.put(credentialId, rotatedCredential);
            
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential:" + credentialId,
                "ROTATE",
                AuditResult.SUCCESS,
                null,
                null,
                Map.of("credentialName", existingCredential.getName())
            );
            
            return rotatedCredential;
        } catch (Exception e) {
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential:" + credentialId,
                "ROTATE",
                AuditResult.SYSTEM_FAILURE,
                null,
                null,
                Map.of("error", e.getMessage())
            );
            throw new RuntimeException("Failed to rotate credential", e);
        }
    }
    
    @Override
    public void deactivateCredential(String credentialId) {
        Credential existingCredential = credentials.get(credentialId);
        if (existingCredential != null) {
            Credential deactivatedCredential = new Credential(
                existingCredential.getCredentialId(),
                existingCredential.getName(),
                existingCredential.getType(),
                existingCredential.getEncryptedValue(),
                existingCredential.getKeyId(),
                existingCredential.getCreatedAt(),
                existingCredential.getExpiresAt(),
                existingCredential.getLastRotatedAt(),
                existingCredential.getCreatedBy(),
                existingCredential.getMetadata(),
                false
            );
            
            credentials.put(credentialId, deactivatedCredential);
            
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential:" + credentialId,
                "DEACTIVATE",
                AuditResult.SUCCESS,
                null,
                null,
                Map.of("credentialName", existingCredential.getName())
            );
        }
    }
    
    @Override
    public void deleteCredential(String credentialId) {
        Credential credential = credentials.remove(credentialId);
        if (credential != null) {
            auditService.recordEvent(
                "system",
                null,
                AuditEventType.CREDENTIAL_MANAGEMENT,
                "credential:" + credentialId,
                "DELETE",
                AuditResult.SUCCESS,
                null,
                null,
                Map.of("credentialName", credential.getName())
            );
        }
    }
    
    @Override
    public List<Credential> findCredentialsNeedingRotation(int rotationDays) {
        return credentials.values().stream()
            .filter(c -> c.isActive() && c.needsRotation(rotationDays))
            .toList();
    }
    
    @Override
    public boolean validateCredentialIntegrity(String credentialId) {
        try {
            Credential credential = credentials.get(credentialId);
            if (credential == null) {
                return false;
            }
            
            // Try to decrypt the credential to validate integrity
            decrypt(credential.getEncryptedValue(), credential.getKeyId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String encrypt(String plaintext, String keyId) throws Exception {
        SecretKey key = encryptionKeys.get(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Encryption key not found: " + keyId);
        }
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    private String decrypt(String encryptedText, String keyId) throws Exception {
        SecretKey key = encryptionKeys.get(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Encryption key not found: " + keyId);
        }
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}