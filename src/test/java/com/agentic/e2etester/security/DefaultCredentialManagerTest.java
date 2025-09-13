package com.agentic.e2etester.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class DefaultCredentialManagerTest {
    
    @Mock
    private AuditService auditService;
    
    private DefaultCredentialManager credentialManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        credentialManager = new DefaultCredentialManager(auditService);
    }
    
    @Test
    void testStoreCredential() {
        // Given
        String name = "test-credential";
        CredentialType type = CredentialType.API_KEY;
        String value = "secret-api-key";
        Map<String, String> metadata = Map.of("environment", "test");
        
        // When
        Credential credential = credentialManager.storeCredential(name, type, value, metadata);
        
        // Then
        assertNotNull(credential);
        assertEquals(name, credential.getName());
        assertEquals(type, credential.getType());
        assertTrue(credential.isActive());
        assertNotNull(credential.getCredentialId());
        assertNotNull(credential.getEncryptedValue());
        assertNotEquals(value, credential.getEncryptedValue()); // Should be encrypted
        
        verify(auditService).recordEvent(anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any());
    }
    
    @Test
    void testGetCredential() {
        // Given
        String name = "test-credential";
        Credential stored = credentialManager.storeCredential(name, CredentialType.API_KEY, "secret", null);
        
        // When
        Optional<Credential> retrieved = credentialManager.getCredential(name);
        
        // Then
        assertTrue(retrieved.isPresent());
        assertEquals(stored.getCredentialId(), retrieved.get().getCredentialId());
        assertEquals(name, retrieved.get().getName());
    }
    
    @Test
    void testGetCredentialValue() {
        // Given
        String value = "secret-value";
        Credential credential = credentialManager.storeCredential("test", CredentialType.SECRET, value, null);
        
        // When
        Optional<String> retrievedValue = credentialManager.getCredentialValue(credential.getCredentialId());
        
        // Then
        assertTrue(retrievedValue.isPresent());
        assertEquals(value, retrievedValue.get());
        
        verify(auditService).recordEvent(anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any());
    }
    
    @Test
    void testListCredentialsByType() {
        // Given
        credentialManager.storeCredential("api-key-1", CredentialType.API_KEY, "key1", null);
        credentialManager.storeCredential("api-key-2", CredentialType.API_KEY, "key2", null);
        credentialManager.storeCredential("password", CredentialType.USERNAME_PASSWORD, "pass", null);
        
        // When
        List<Credential> apiKeys = credentialManager.listCredentialsByType(CredentialType.API_KEY);
        
        // Then
        assertEquals(2, apiKeys.size());
        assertTrue(apiKeys.stream().allMatch(c -> c.getType() == CredentialType.API_KEY));
    }
    
    @Test
    void testRotateCredential() {
        // Given
        String originalValue = "original-secret";
        String newValue = "new-secret";
        Credential original = credentialManager.storeCredential("test", CredentialType.SECRET, originalValue, null);
        
        // When
        Credential rotated = credentialManager.rotateCredential(original.getCredentialId(), newValue);
        
        // Then
        assertEquals(original.getCredentialId(), rotated.getCredentialId());
        assertEquals(original.getName(), rotated.getName());
        assertNotEquals(original.getEncryptedValue(), rotated.getEncryptedValue());
        assertTrue(rotated.getLastRotatedAt().isAfter(original.getLastRotatedAt()));
        
        // Verify new value can be retrieved
        Optional<String> retrievedValue = credentialManager.getCredentialValue(rotated.getCredentialId());
        assertTrue(retrievedValue.isPresent());
        assertEquals(newValue, retrievedValue.get());
    }
    
    @Test
    void testDeactivateCredential() {
        // Given
        Credential credential = credentialManager.storeCredential("test", CredentialType.SECRET, "secret", null);
        assertTrue(credential.isActive());
        
        // When
        credentialManager.deactivateCredential(credential.getCredentialId());
        
        // Then
        Optional<Credential> retrieved = credentialManager.getCredential("test");
        assertTrue(retrieved.isEmpty()); // Should not find inactive credentials
    }
    
    @Test
    void testDeleteCredential() {
        // Given
        Credential credential = credentialManager.storeCredential("test", CredentialType.SECRET, "secret", null);
        
        // When
        credentialManager.deleteCredential(credential.getCredentialId());
        
        // Then
        Optional<Credential> retrieved = credentialManager.getCredential("test");
        assertTrue(retrieved.isEmpty());
        
        Optional<String> value = credentialManager.getCredentialValue(credential.getCredentialId());
        assertTrue(value.isEmpty());
    }
    
    @Test
    void testFindCredentialsNeedingRotation() {
        // Given
        credentialManager.storeCredential("old-credential", CredentialType.API_KEY, "old-key", null);
        
        // When
        List<Credential> needingRotation = credentialManager.findCredentialsNeedingRotation(0); // 0 days = all need rotation
        
        // Then
        assertEquals(1, needingRotation.size());
        assertEquals("old-credential", needingRotation.get(0).getName());
    }
    
    @Test
    void testValidateCredentialIntegrity() {
        // Given
        Credential credential = credentialManager.storeCredential("test", CredentialType.SECRET, "secret", null);
        
        // When
        boolean isValid = credentialManager.validateCredentialIntegrity(credential.getCredentialId());
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateCredentialIntegrityInvalidId() {
        // When
        boolean isValid = credentialManager.validateCredentialIntegrity("non-existent-id");
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testStoreCredentialWithNullMetadata() {
        // When
        Credential credential = credentialManager.storeCredential("test", CredentialType.SECRET, "secret", null);
        
        // Then
        assertNotNull(credential.getMetadata());
        assertTrue(credential.getMetadata().isEmpty());
    }
}