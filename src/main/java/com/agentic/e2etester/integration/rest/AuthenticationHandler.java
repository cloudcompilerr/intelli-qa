package com.agentic.e2etester.integration.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles authentication and authorization for REST API calls.
 * Supports multiple authentication schemes including Basic Auth, Bearer tokens, and API keys.
 */
@Component
public class AuthenticationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);
    
    // Cache for authentication tokens to avoid repeated authentication calls
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();
    
    @Value("${agentic.e2etester.auth.default-username:#{null}}")
    private String defaultUsername;
    
    @Value("${agentic.e2etester.auth.default-password:#{null}}")
    private String defaultPassword;
    
    @Value("${agentic.e2etester.auth.default-token:#{null}}")
    private String defaultBearerToken;
    
    @Value("${agentic.e2etester.auth.default-api-key:#{null}}")
    private String defaultApiKey;
    
    /**
     * Apply authentication to a WebClient request based on service configuration
     */
    public WebClient.RequestBodySpec applyAuthentication(WebClient.RequestBodySpec requestSpec, String serviceId) {
        
        // Try service-specific authentication first
        AuthenticationConfig authConfig = getAuthenticationConfig(serviceId);
        
        if (authConfig != null) {
            return applyAuthenticationConfig(requestSpec, authConfig);
        }
        
        // Fallback to default authentication
        return applyDefaultAuthentication(requestSpec);
    }
    
    /**
     * Apply authentication configuration to a request
     */
    private WebClient.RequestBodySpec applyAuthenticationConfig(WebClient.RequestBodySpec requestSpec, 
                                                              AuthenticationConfig authConfig) {
        
        switch (authConfig.getType()) {
            case BASIC:
                return applyBasicAuth(requestSpec, authConfig.getUsername(), authConfig.getPassword());
                
            case BEARER:
                return applyBearerToken(requestSpec, authConfig.getToken());
                
            case API_KEY:
                return applyApiKey(requestSpec, authConfig.getApiKey(), authConfig.getApiKeyHeader());
                
            case OAUTH2:
                return applyOAuth2Token(requestSpec, authConfig.getServiceId());
                
            case NONE:
            default:
                return requestSpec;
        }
    }
    
    /**
     * Apply default authentication if no service-specific configuration is found
     */
    private WebClient.RequestBodySpec applyDefaultAuthentication(WebClient.RequestBodySpec requestSpec) {
        
        // Try Bearer token first
        if (defaultBearerToken != null && !defaultBearerToken.trim().isEmpty()) {
            return applyBearerToken(requestSpec, defaultBearerToken);
        }
        
        // Try Basic Auth
        if (defaultUsername != null && defaultPassword != null && 
            !defaultUsername.trim().isEmpty() && !defaultPassword.trim().isEmpty()) {
            return applyBasicAuth(requestSpec, defaultUsername, defaultPassword);
        }
        
        // Try API Key
        if (defaultApiKey != null && !defaultApiKey.trim().isEmpty()) {
            return applyApiKey(requestSpec, defaultApiKey, "X-API-Key");
        }
        
        // No authentication
        logger.debug("No authentication configured, proceeding without authentication");
        return requestSpec;
    }
    
    /**
     * Apply Basic Authentication
     */
    private WebClient.RequestBodySpec applyBasicAuth(WebClient.RequestBodySpec requestSpec, 
                                                   String username, String password) {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        return requestSpec.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
    }
    
    /**
     * Apply Bearer Token Authentication
     */
    private WebClient.RequestBodySpec applyBearerToken(WebClient.RequestBodySpec requestSpec, String token) {
        return requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    
    /**
     * Apply API Key Authentication
     */
    private WebClient.RequestBodySpec applyApiKey(WebClient.RequestBodySpec requestSpec, 
                                                String apiKey, String headerName) {
        String header = headerName != null ? headerName : "X-API-Key";
        return requestSpec.header(header, apiKey);
    }
    
    /**
     * Apply OAuth2 Token Authentication (with token caching)
     */
    private WebClient.RequestBodySpec applyOAuth2Token(WebClient.RequestBodySpec requestSpec, String serviceId) {
        String cachedToken = tokenCache.get(serviceId);
        
        if (cachedToken != null) {
            return applyBearerToken(requestSpec, cachedToken);
        }
        
        // In a real implementation, this would fetch a token from an OAuth2 provider
        // For now, we'll use the default bearer token if available
        if (defaultBearerToken != null && !defaultBearerToken.trim().isEmpty()) {
            tokenCache.put(serviceId, defaultBearerToken);
            return applyBearerToken(requestSpec, defaultBearerToken);
        }
        
        logger.warn("OAuth2 token not available for service: {}", serviceId);
        return requestSpec;
    }
    
    /**
     * Get authentication configuration for a specific service
     * In a real implementation, this would load from configuration or a database
     */
    private AuthenticationConfig getAuthenticationConfig(String serviceId) {
        // This is a placeholder implementation
        // In practice, you would load this from configuration files, database, or service registry
        
        // Example service-specific configurations
        switch (serviceId) {
            case "order-service":
                return new AuthenticationConfig(AuthenticationType.BEARER, null, null, defaultBearerToken, null, null, serviceId);
                
            case "payment-service":
                return new AuthenticationConfig(AuthenticationType.API_KEY, null, null, null, defaultApiKey, "X-Payment-API-Key", serviceId);
                
            case "inventory-service":
                if (defaultUsername != null && defaultPassword != null) {
                    return new AuthenticationConfig(AuthenticationType.BASIC, defaultUsername, defaultPassword, null, null, null, serviceId);
                }
                break;
                
            default:
                // No service-specific configuration
                return null;
        }
        
        return null;
    }
    
    /**
     * Clear cached tokens for a service
     */
    public void clearTokenCache(String serviceId) {
        tokenCache.remove(serviceId);
        logger.debug("Cleared authentication token cache for service: {}", serviceId);
    }
    
    /**
     * Clear all cached tokens
     */
    public void clearAllTokenCaches() {
        tokenCache.clear();
        logger.debug("Cleared all authentication token caches");
    }
    
    /**
     * Authentication configuration data class
     */
    public static class AuthenticationConfig {
        private final AuthenticationType type;
        private final String username;
        private final String password;
        private final String token;
        private final String apiKey;
        private final String apiKeyHeader;
        private final String serviceId;
        
        public AuthenticationConfig(AuthenticationType type, String username, String password, 
                                  String token, String apiKey, String apiKeyHeader, String serviceId) {
            this.type = type;
            this.username = username;
            this.password = password;
            this.token = token;
            this.apiKey = apiKey;
            this.apiKeyHeader = apiKeyHeader;
            this.serviceId = serviceId;
        }
        
        // Getters
        public AuthenticationType getType() { return type; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getToken() { return token; }
        public String getApiKey() { return apiKey; }
        public String getApiKeyHeader() { return apiKeyHeader; }
        public String getServiceId() { return serviceId; }
    }
    
    /**
     * Supported authentication types
     */
    public enum AuthenticationType {
        NONE,
        BASIC,
        BEARER,
        API_KEY,
        OAUTH2
    }
}