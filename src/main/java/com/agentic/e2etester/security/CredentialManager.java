package com.agentic.e2etester.security;

import java.util.List;
import java.util.Optional;

/**
 * Interface for managing secure credentials used in test execution.
 */
public interface CredentialManager {
    
    /**
     * Stores a new credential securely.
     *
     * @param name the credential name
     * @param type the credential type
     * @param value the credential value to encrypt and store
     * @param metadata additional metadata for the credential
     * @return the stored credential
     */
    Credential storeCredential(String name, CredentialType type, String value, 
                              java.util.Map<String, String> metadata);
    
    /**
     * Retrieves a credential by name.
     *
     * @param name the credential name
     * @return the credential if found
     */
    Optional<Credential> getCredential(String name);
    
    /**
     * Retrieves the decrypted value of a credential.
     *
     * @param credentialId the credential ID
     * @return the decrypted credential value
     */
    Optional<String> getCredentialValue(String credentialId);
    
    /**
     * Lists all credentials (without values).
     *
     * @return list of credentials
     */
    List<Credential> listCredentials();
    
    /**
     * Lists credentials by type.
     *
     * @param type the credential type
     * @return list of credentials of the specified type
     */
    List<Credential> listCredentialsByType(CredentialType type);
    
    /**
     * Rotates a credential with a new value.
     *
     * @param credentialId the credential ID
     * @param newValue the new credential value
     * @return the updated credential
     */
    Credential rotateCredential(String credentialId, String newValue);
    
    /**
     * Deactivates a credential.
     *
     * @param credentialId the credential ID
     */
    void deactivateCredential(String credentialId);
    
    /**
     * Deletes a credential permanently.
     *
     * @param credentialId the credential ID
     */
    void deleteCredential(String credentialId);
    
    /**
     * Finds credentials that need rotation.
     *
     * @param rotationDays the number of days after which rotation is needed
     * @return list of credentials needing rotation
     */
    List<Credential> findCredentialsNeedingRotation(int rotationDays);
    
    /**
     * Validates credential integrity.
     *
     * @param credentialId the credential ID
     * @return true if credential is valid and not corrupted
     */
    boolean validateCredentialIntegrity(String credentialId);
}