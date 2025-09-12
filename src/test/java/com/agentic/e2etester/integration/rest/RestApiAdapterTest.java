package com.agentic.e2etester.integration.rest;

import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.InteractionStatus;
import com.agentic.e2etester.model.InteractionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiAdapterTest {
    
    @Mock
    private EndpointDiscoveryService endpointDiscoveryService;
    
    @Mock
    private AuthenticationHandler authenticationHandler;
    
    private MockWebServer mockWebServer;
    private RestApiAdapter restApiAdapter;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        objectMapper = new ObjectMapper();
        
        WebClient.Builder webClientBuilder = WebClient.builder();
        restApiAdapter = new RestApiAdapter(webClientBuilder, endpointDiscoveryService, authenticationHandler);
        
        // Mock authentication handler to return the request spec unchanged
        lenient().when(authenticationHandler.applyAuthentication(org.mockito.ArgumentMatchers.any(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void shouldExecuteGetRequestSuccessfully() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/test";
        String correlationId = "test-correlation-id";
        String responseBody = "{\"message\":\"success\"}";
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        
        String fullUrl = mockWebServer.url(endpoint).toString();
        when(endpointDiscoveryService.resolveEndpoint(serviceId, endpoint))
                .thenReturn(CompletableFuture.completedFuture(fullUrl));
        
        // When
        CompletableFuture<ServiceInteraction> future = restApiAdapter.get(serviceId, endpoint, correlationId, null);
        ServiceInteraction result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getServiceId()).isEqualTo(serviceId);
        assertThat(result.getType()).isEqualTo(InteractionType.HTTP_REQUEST);
        assertThat(result.getStatus()).isEqualTo(InteractionStatus.SUCCESS);
        assertThat(result.getCorrelationId()).isEqualTo(correlationId);
        assertThat(result.getResponse()).isEqualTo(responseBody);
        assertThat(result.getResponseTime()).isNotNull();
        assertThat(result.getResponseTime().toMillis()).isGreaterThan(0);
        
        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo(endpoint);
        assertThat(recordedRequest.getHeader("X-Correlation-ID")).isEqualTo(correlationId);
    }
    
    @Test
    void shouldExecutePostRequestWithBodySuccessfully() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/create";
        String correlationId = "test-correlation-id";
        Map<String, Object> requestBody = Map.of("name", "test", "value", 123);
        String responseBody = "{\"id\":\"created-123\"}";
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .setResponseCode(201)
                .addHeader("Content-Type", "application/json"));
        
        String fullUrl = mockWebServer.url(endpoint).toString();
        when(endpointDiscoveryService.resolveEndpoint(serviceId, endpoint))
                .thenReturn(CompletableFuture.completedFuture(fullUrl));
        
        // When
        CompletableFuture<ServiceInteraction> future = restApiAdapter.post(serviceId, endpoint, requestBody, correlationId, null);
        ServiceInteraction result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getServiceId()).isEqualTo(serviceId);
        assertThat(result.getType()).isEqualTo(InteractionType.HTTP_REQUEST);
        assertThat(result.getStatus()).isEqualTo(InteractionStatus.SUCCESS);
        assertThat(result.getCorrelationId()).isEqualTo(correlationId);
        assertThat(result.getRequest()).isEqualTo(requestBody);
        assertThat(result.getResponse()).isEqualTo(responseBody);
        
        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo(endpoint);
        assertThat(recordedRequest.getHeader("X-Correlation-ID")).isEqualTo(correlationId);
        assertThat(recordedRequest.getHeader("Content-Type")).contains("application/json");
        
        // Verify request body
        String actualRequestBody = recordedRequest.getBody().readUtf8();
        Map<String, Object> actualBodyMap = objectMapper.readValue(actualRequestBody, Map.class);
        assertThat(actualBodyMap).containsEntry("name", "test");
        assertThat(actualBodyMap).containsEntry("value", 123);
    }
    
    @Test
    void shouldHandleHttpErrorResponse() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/error";
        String correlationId = "test-correlation-id";
        String errorResponse = "{\"error\":\"Not Found\"}";
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(errorResponse)
                .setResponseCode(404)
                .addHeader("Content-Type", "application/json"));
        
        String fullUrl = mockWebServer.url(endpoint).toString();
        when(endpointDiscoveryService.resolveEndpoint(serviceId, endpoint))
                .thenReturn(CompletableFuture.completedFuture(fullUrl));
        
        // When
        CompletableFuture<ServiceInteraction> future = restApiAdapter.get(serviceId, endpoint, correlationId, null);
        ServiceInteraction result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getServiceId()).isEqualTo(serviceId);
        assertThat(result.getStatus()).isEqualTo(InteractionStatus.FAILURE);
        assertThat(result.getCorrelationId()).isEqualTo(correlationId);
        assertThat(result.getResponse()).isEqualTo(errorResponse);
        assertThat(result.getErrorMessage()).contains("404");
        assertThat(result.getResponseTime()).isNotNull();
    }
    
    @Test
    void shouldHandleCustomHeaders() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/test";
        String correlationId = "test-correlation-id";
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Custom-Header", "custom-value");
        customHeaders.put("X-Request-ID", "request-123");
        
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"success\":true}")
                .setResponseCode(200));
        
        String fullUrl = mockWebServer.url(endpoint).toString();
        when(endpointDiscoveryService.resolveEndpoint(serviceId, endpoint))
                .thenReturn(CompletableFuture.completedFuture(fullUrl));
        
        // When
        CompletableFuture<ServiceInteraction> future = restApiAdapter.get(serviceId, endpoint, correlationId, customHeaders);
        ServiceInteraction result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(InteractionStatus.SUCCESS);
        
        // Verify custom headers were sent
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("X-Custom-Header")).isEqualTo("custom-value");
        assertThat(recordedRequest.getHeader("X-Request-ID")).isEqualTo("request-123");
        assertThat(recordedRequest.getHeader("X-Correlation-ID")).isEqualTo(correlationId);
    }
    
    @Test
    void shouldHandleEndpointResolutionFailure() throws Exception {
        // Given
        String serviceId = "unknown-service";
        String endpoint = "/api/test";
        String correlationId = "test-correlation-id";
        
        when(endpointDiscoveryService.resolveEndpoint(serviceId, endpoint))
                .thenReturn(CompletableFuture.failedFuture(new IllegalArgumentException("Service not found")));
        
        // When
        CompletableFuture<ServiceInteraction> future = restApiAdapter.get(serviceId, endpoint, correlationId, null);
        ServiceInteraction result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getServiceId()).isEqualTo(serviceId);
        assertThat(result.getStatus()).isEqualTo(InteractionStatus.FAILURE);
        assertThat(result.getCorrelationId()).isEqualTo(correlationId);
        assertThat(result.getErrorMessage()).contains("Failed to resolve endpoint");
        assertThat(result.getResponseTime()).isNotNull();
    }
    
    @Test
    void shouldCheckEndpointAvailability() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/health";
        
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String fullUrl = mockWebServer.url(endpoint).toString();
        when(endpointDiscoveryService.resolveEndpoint(serviceId, endpoint))
                .thenReturn(CompletableFuture.completedFuture(fullUrl));
        
        // When
        CompletableFuture<Boolean> future = restApiAdapter.isEndpointAvailable(serviceId, endpoint);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isTrue();
        
        // Verify HEAD request was made
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("HEAD");
        assertThat(recordedRequest.getPath()).isEqualTo(endpoint);
    }
    
    @Test
    void shouldReturnFalseForUnavailableEndpoint() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/unavailable";
        
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        
        String fullUrl = mockWebServer.url(endpoint).toString();
        when(endpointDiscoveryService.resolveEndpoint(serviceId, endpoint))
                .thenReturn(CompletableFuture.completedFuture(fullUrl));
        
        // When
        CompletableFuture<Boolean> future = restApiAdapter.isEndpointAvailable(serviceId, endpoint);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isFalse();
    }
}