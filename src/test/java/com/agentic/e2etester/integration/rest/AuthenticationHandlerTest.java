package com.agentic.e2etester.integration.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationHandlerTest {
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    private AuthenticationHandler authenticationHandler;
    
    @BeforeEach
    void setUp() {
        authenticationHandler = new AuthenticationHandler();
    }
    
    @Test
    void shouldApplyBasicAuthenticationWhenConfigured() {
        // Given
        ReflectionTestUtils.setField(authenticationHandler, "defaultUsername", "testuser");
        ReflectionTestUtils.setField(authenticationHandler, "defaultPassword", "testpass");
        
        when(requestBodySpec.header(eq("Authorization"), eq("Basic " + 
                Base64.getEncoder().encodeToString("testuser:testpass".getBytes()))))
                .thenReturn(requestBodySpec);
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "unknown-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        verify(requestBodySpec).header(eq("Authorization"), eq("Basic " + 
                Base64.getEncoder().encodeToString("testuser:testpass".getBytes())));
    }
    
    @Test
    void shouldApplyBearerTokenWhenConfigured() {
        // Given
        String bearerToken = "test-bearer-token";
        ReflectionTestUtils.setField(authenticationHandler, "defaultBearerToken", bearerToken);
        
        when(requestBodySpec.header(eq("Authorization"), eq("Bearer " + bearerToken)))
                .thenReturn(requestBodySpec);
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "unknown-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        verify(requestBodySpec).header(eq("Authorization"), eq("Bearer " + bearerToken));
    }
    
    @Test
    void shouldApplyApiKeyWhenConfigured() {
        // Given
        String apiKey = "test-api-key";
        ReflectionTestUtils.setField(authenticationHandler, "defaultApiKey", apiKey);
        
        when(requestBodySpec.header(eq("X-API-Key"), eq(apiKey)))
                .thenReturn(requestBodySpec);
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "unknown-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        verify(requestBodySpec).header(eq("X-API-Key"), eq(apiKey));
    }
    
    @Test
    void shouldPrioritizeBearerTokenOverBasicAuth() {
        // Given
        ReflectionTestUtils.setField(authenticationHandler, "defaultUsername", "testuser");
        ReflectionTestUtils.setField(authenticationHandler, "defaultPassword", "testpass");
        ReflectionTestUtils.setField(authenticationHandler, "defaultBearerToken", "bearer-token");
        
        when(requestBodySpec.header(eq("Authorization"), eq("Bearer bearer-token")))
                .thenReturn(requestBodySpec);
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "unknown-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        verify(requestBodySpec).header(eq("Authorization"), eq("Bearer bearer-token"));
    }
    
    @Test
    void shouldApplyServiceSpecificBearerTokenForOrderService() {
        // Given
        String defaultToken = "default-token";
        ReflectionTestUtils.setField(authenticationHandler, "defaultBearerToken", defaultToken);
        
        when(requestBodySpec.header(eq("Authorization"), eq("Bearer " + defaultToken)))
                .thenReturn(requestBodySpec);
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "order-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        verify(requestBodySpec).header(eq("Authorization"), eq("Bearer " + defaultToken));
    }
    
    @Test
    void shouldApplyServiceSpecificApiKeyForPaymentService() {
        // Given
        String defaultApiKey = "default-api-key";
        ReflectionTestUtils.setField(authenticationHandler, "defaultApiKey", defaultApiKey);
        
        when(requestBodySpec.header(eq("X-Payment-API-Key"), eq(defaultApiKey)))
                .thenReturn(requestBodySpec);
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "payment-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        verify(requestBodySpec).header(eq("X-Payment-API-Key"), eq(defaultApiKey));
    }
    
    @Test
    void shouldApplyServiceSpecificBasicAuthForInventoryService() {
        // Given
        String username = "inventory-user";
        String password = "inventory-pass";
        ReflectionTestUtils.setField(authenticationHandler, "defaultUsername", username);
        ReflectionTestUtils.setField(authenticationHandler, "defaultPassword", password);
        
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        when(requestBodySpec.header(eq("Authorization"), eq(expectedAuth)))
                .thenReturn(requestBodySpec);
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "inventory-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        verify(requestBodySpec).header(eq("Authorization"), eq(expectedAuth));
    }
    
    @Test
    void shouldReturnRequestSpecUnchangedWhenNoAuthenticationConfigured() {
        // Given - no authentication configured
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "unknown-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        // No header calls should be made
    }
    
    @Test
    void shouldIgnoreEmptyOrBlankAuthenticationValues() {
        // Given
        ReflectionTestUtils.setField(authenticationHandler, "defaultUsername", "");
        ReflectionTestUtils.setField(authenticationHandler, "defaultPassword", " ");
        ReflectionTestUtils.setField(authenticationHandler, "defaultBearerToken", null);
        ReflectionTestUtils.setField(authenticationHandler, "defaultApiKey", "   ");
        
        // When
        WebClient.RequestBodySpec result = authenticationHandler.applyAuthentication(requestBodySpec, "unknown-service");
        
        // Then
        assertThat(result).isEqualTo(requestBodySpec);
        // No header calls should be made
    }
    
    @Test
    void shouldClearTokenCache() {
        // Given
        String serviceId = "test-service";
        
        // When
        authenticationHandler.clearTokenCache(serviceId);
        
        // Then - no exception should be thrown
        // This is mainly testing that the method exists and doesn't fail
    }
    
    @Test
    void shouldClearAllTokenCaches() {
        // When
        authenticationHandler.clearAllTokenCaches();
        
        // Then - no exception should be thrown
        // This is mainly testing that the method exists and doesn't fail
    }
}