package com.agentic.e2etester.security;

/**
 * Types of credentials supported by the system.
 */
public enum CredentialType {
    /**
     * API key for service authentication
     */
    API_KEY,
    
    /**
     * Username and password combination
     */
    USERNAME_PASSWORD,
    
    /**
     * OAuth2 access token
     */
    OAUTH2_TOKEN,
    
    /**
     * JWT token
     */
    JWT_TOKEN,
    
    /**
     * Database connection credentials
     */
    DATABASE_CREDENTIALS,
    
    /**
     * Kafka authentication credentials
     */
    KAFKA_CREDENTIALS,
    
    /**
     * TLS/SSL certificates
     */
    TLS_CERTIFICATE,
    
    /**
     * SSH private key
     */
    SSH_PRIVATE_KEY,
    
    /**
     * Generic secret value
     */
    SECRET
}