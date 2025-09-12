package com.agentic.e2etester.integration.rest;

import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceStatus;
import com.agentic.e2etester.service.ServiceDiscovery;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointDiscoveryServiceTest {
    
    @Mock
    private ServiceDiscovery serviceDiscovery;
    
    private MockWebServer mockWebServer;
    private EndpointDiscoveryService endpointDiscoveryService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        objectMapper = new ObjectMapper();
        WebClient.Builder webClientBuilder = WebClient.builder();
        endpointDiscoveryService = new EndpointDiscoveryService(serviceDiscovery, webClientBuilder, objectMapper);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void shouldResolveEndpointSuccessfully() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/test";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // When
        CompletableFuture<String> future = endpointDiscoveryService.resolveEndpoint(serviceId, endpoint);
        String result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isEqualTo(baseUrl + "api/test");
    }
    
    @Test
    void shouldHandleEndpointWithLeadingSlash() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "/api/test";
        String baseUrl = mockWebServer.url("/").toString(); // ends with /
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // When
        CompletableFuture<String> future = endpointDiscoveryService.resolveEndpoint(serviceId, endpoint);
        String result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isEqualTo(baseUrl + "api/test");
    }
    
    @Test
    void shouldHandleBaseUrlWithoutTrailingSlash() throws Exception {
        // Given
        String serviceId = "test-service";
        String endpoint = "api/test";
        String baseUrl = mockWebServer.url("").toString().replaceAll("/$", ""); // remove trailing slash
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // When
        CompletableFuture<String> future = endpointDiscoveryService.resolveEndpoint(serviceId, endpoint);
        String result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isEqualTo(baseUrl + "/" + endpoint);
    }
    
    @Test
    void shouldDiscoverEndpointsFromOpenAPI() throws Exception {
        // Given
        String serviceId = "test-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        serviceInfo.setStatus(ServiceStatus.HEALTHY);
        
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // Mock OpenAPI documentation response
        String openApiDoc = """
                {
                  "openapi": "3.0.0",
                  "paths": {
                    "/api/users": {
                      "get": {
                        "summary": "Get users"
                      },
                      "post": {
                        "summary": "Create user"
                      }
                    },
                    "/api/orders": {
                      "get": {
                        "summary": "Get orders"
                      }
                    }
                  }
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(openApiDoc)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        
        // When
        CompletableFuture<ServiceInfo> future = endpointDiscoveryService.discoverServiceEndpoints(serviceId);
        ServiceInfo result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEndpoints()).isNotNull();
        assertThat(result.getEndpoints()).contains("/api/users", "/api/orders");
        assertThat(result.getEndpoints()).contains("GET /api/users", "POST /api/users", "GET /api/orders");
    }
    
    @Test
    void shouldFallbackToActuatorWhenOpenAPIFails() throws Exception {
        // Given
        String serviceId = "test-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        serviceInfo.setStatus(ServiceStatus.HEALTHY);
        
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // Mock failed OpenAPI responses (try multiple paths)
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // /v3/api-docs
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // /v2/api-docs
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // /swagger/v1/swagger.json
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // /api-docs
        mockWebServer.enqueue(new MockResponse().setResponseCode(404)); // /swagger.json
        
        // Mock successful actuator response
        String actuatorResponse = """
                {
                  "_links": {
                    "self": {
                      "href": "http://localhost:8080/actuator",
                      "templated": false
                    },
                    "health": {
                      "href": "http://localhost:8080/actuator/health",
                      "templated": false
                    },
                    "info": {
                      "href": "http://localhost:8080/actuator/info",
                      "templated": false
                    },
                    "metrics": {
                      "href": "http://localhost:8080/actuator/metrics",
                      "templated": false
                    }
                  }
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setBody(actuatorResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        
        // When
        CompletableFuture<ServiceInfo> future = endpointDiscoveryService.discoverServiceEndpoints(serviceId);
        ServiceInfo result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEndpoints()).isNotNull();
        assertThat(result.getEndpoints()).contains("/actuator/health", "/actuator/info", "/actuator/metrics");
    }
    
    @Test
    void shouldReturnEmptyEndpointsWhenBothOpenAPIAndActuatorFail() throws Exception {
        // Given
        String serviceId = "test-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        serviceInfo.setStatus(ServiceStatus.HEALTHY);
        
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // Mock all requests to fail
        for (int i = 0; i < 6; i++) { // 5 OpenAPI paths + 1 actuator
            mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        }
        
        // When
        CompletableFuture<ServiceInfo> future = endpointDiscoveryService.discoverServiceEndpoints(serviceId);
        ServiceInfo result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEndpoints()).isNotNull();
        assertThat(result.getEndpoints()).isEmpty();
    }
    
    @Test
    void shouldReturnNullForUnknownService() throws Exception {
        // Given
        String serviceId = "unknown-service";
        
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.empty());
        
        // When
        CompletableFuture<ServiceInfo> future = endpointDiscoveryService.discoverServiceEndpoints(serviceId);
        ServiceInfo result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void shouldCacheDiscoveredEndpoints() throws Exception {
        // Given
        String serviceId = "test-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        serviceInfo.setStatus(ServiceStatus.HEALTHY);
        serviceInfo.setEndpoints(Set.of("/api/test"));
        
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // Mock OpenAPI response for first call
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"paths\":{\"/api/cached\":{\"get\":{}}}}")
                .setResponseCode(200));
        
        // When - first call
        CompletableFuture<ServiceInfo> future1 = endpointDiscoveryService.discoverServiceEndpoints(serviceId);
        ServiceInfo result1 = future1.get(5, TimeUnit.SECONDS);
        
        // When - second call (should use cache)
        CompletableFuture<ServiceInfo> future2 = endpointDiscoveryService.discoverServiceEndpoints(serviceId);
        ServiceInfo result2 = future2.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1).isSameAs(result2); // Should be the same cached instance
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1); // Only one HTTP request made
    }
    
    @Test
    void shouldClearCache() throws Exception {
        // Given
        String serviceId = "test-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Test Service", baseUrl);
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"paths\":{\"/api/test\":{\"get\":{}}}}")
                .setResponseCode(200));
        
        // First call to populate cache
        endpointDiscoveryService.discoverServiceEndpoints(serviceId).get(5, TimeUnit.SECONDS);
        
        // Clear cache
        endpointDiscoveryService.clearCache(serviceId);
        
        // Mock another response for second call
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"paths\":{\"/api/test2\":{\"get\":{}}}}")
                .setResponseCode(200));
        
        // When - second call after cache clear
        CompletableFuture<ServiceInfo> future = endpointDiscoveryService.discoverServiceEndpoints(serviceId);
        ServiceInfo result = future.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2); // Two HTTP requests made
    }
}