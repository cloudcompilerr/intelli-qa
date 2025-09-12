package com.agentic.e2etester.integration.rest;

import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.InteractionStatus;
import com.agentic.e2etester.model.ServiceStatus;
import com.agentic.e2etester.service.ServiceDiscovery;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    RestApiAdapter.class,
    EndpointDiscoveryService.class,
    AuthenticationHandler.class,
    RestIntegrationConfiguration.class
})
@TestPropertySource(properties = {
    "agentic.e2etester.auth.default-username=testuser",
    "agentic.e2etester.auth.default-password=testpass",
    "agentic.e2etester.rest.connection-timeout=5000",
    "agentic.e2etester.rest.read-timeout=10000"
})
class RestIntegrationTest {
    
    @Autowired
    private RestApiAdapter restApiAdapter;
    
    @Autowired
    private EndpointDiscoveryService endpointDiscoveryService;
    
    @MockBean
    private ServiceDiscovery serviceDiscovery;
    
    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void shouldPerformCompleteOrderFulfillmentFlow() throws Exception {
        // Given - Setup mock services for order fulfillment flow
        String baseUrl = mockWebServer.url("/").toString();
        
        // Mock order service
        ServiceInfo orderService = new ServiceInfo("order-service", "Order Service", baseUrl);
        orderService.setStatus(ServiceStatus.HEALTHY);
        when(serviceDiscovery.getService("order-service"))
                .thenReturn(Optional.of(orderService));
        
        // Mock payment service  
        ServiceInfo paymentService = new ServiceInfo("payment-service", "Payment Service", baseUrl);
        paymentService.setStatus(ServiceStatus.HEALTHY);
        when(serviceDiscovery.getService("payment-service"))
                .thenReturn(Optional.of(paymentService));
        
        // Mock inventory service
        ServiceInfo inventoryService = new ServiceInfo("inventory-service", "Inventory Service", baseUrl);
        inventoryService.setStatus(ServiceStatus.HEALTHY);
        when(serviceDiscovery.getService("inventory-service"))
                .thenReturn(Optional.of(inventoryService));
        
        // Setup mock responses for the order fulfillment flow
        
        // 1. Create order
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"orderId\":\"order-123\",\"status\":\"created\"}")
                .setResponseCode(201)
                .addHeader("Content-Type", "application/json"));
        
        // 2. Process payment
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"paymentId\":\"payment-456\",\"status\":\"processed\"}")
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        
        // 3. Reserve inventory
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"reservationId\":\"reservation-789\",\"status\":\"reserved\"}")
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        
        String correlationId = "test-correlation-123";
        
        // When - Execute order fulfillment flow
        
        // Step 1: Create order
        Map<String, Object> orderRequest = Map.of(
                "customerId", "customer-123",
                "items", Map.of("productId", "product-456", "quantity", 2),
                "totalAmount", 99.99
        );
        
        CompletableFuture<ServiceInteraction> orderResult = restApiAdapter.post(
                "order-service", "/api/orders", orderRequest, correlationId, null);
        
        // Step 2: Process payment
        Map<String, Object> paymentRequest = Map.of(
                "orderId", "order-123",
                "amount", 99.99,
                "paymentMethod", "credit_card"
        );
        
        CompletableFuture<ServiceInteraction> paymentResult = restApiAdapter.post(
                "payment-service", "/api/payments", paymentRequest, correlationId, null);
        
        // Step 3: Reserve inventory
        Map<String, Object> inventoryRequest = Map.of(
                "orderId", "order-123",
                "items", Map.of("productId", "product-456", "quantity", 2)
        );
        
        CompletableFuture<ServiceInteraction> inventoryResult = restApiAdapter.post(
                "inventory-service", "/api/reservations", inventoryRequest, correlationId, null);
        
        // Then - Verify all steps completed successfully
        ServiceInteraction orderInteraction = orderResult.get(10, TimeUnit.SECONDS);
        ServiceInteraction paymentInteraction = paymentResult.get(10, TimeUnit.SECONDS);
        ServiceInteraction inventoryInteraction = inventoryResult.get(10, TimeUnit.SECONDS);
        
        // Verify order creation
        assertThat(orderInteraction.getStatus()).isEqualTo(InteractionStatus.SUCCESS);
        assertThat(orderInteraction.getCorrelationId()).isEqualTo(correlationId);
        assertThat(orderInteraction.getResponse().toString()).contains("order-123");
        
        // Verify payment processing
        assertThat(paymentInteraction.getStatus()).isEqualTo(InteractionStatus.SUCCESS);
        assertThat(paymentInteraction.getCorrelationId()).isEqualTo(correlationId);
        assertThat(paymentInteraction.getResponse().toString()).contains("payment-456");
        
        // Verify inventory reservation
        assertThat(inventoryInteraction.getStatus()).isEqualTo(InteractionStatus.SUCCESS);
        assertThat(inventoryInteraction.getCorrelationId()).isEqualTo(correlationId);
        assertThat(inventoryInteraction.getResponse().toString()).contains("reservation-789");
        
        // Verify requests were made with correct correlation IDs
        RecordedRequest orderRequest1 = mockWebServer.takeRequest();
        RecordedRequest paymentRequest1 = mockWebServer.takeRequest();
        RecordedRequest inventoryRequest1 = mockWebServer.takeRequest();
        
        assertThat(orderRequest1.getHeader("X-Correlation-ID")).isEqualTo(correlationId);
        assertThat(paymentRequest1.getHeader("X-Correlation-ID")).isEqualTo(correlationId);
        assertThat(inventoryRequest1.getHeader("X-Correlation-ID")).isEqualTo(correlationId);
        
        // Verify authentication headers were applied (Basic Auth for inventory service)
        assertThat(inventoryRequest1.getHeader("Authorization")).startsWith("Basic");
    }
    
    @Test
    void shouldDiscoverServiceEndpointsFromOpenAPI() throws Exception {
        // Given
        String serviceId = "api-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "API Service", baseUrl);
        serviceInfo.setStatus(ServiceStatus.HEALTHY);
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // Mock OpenAPI documentation
        String openApiDoc = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "Test API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/api/users": {
                      "get": {
                        "summary": "List users",
                        "operationId": "listUsers"
                      },
                      "post": {
                        "summary": "Create user",
                        "operationId": "createUser"
                      }
                    },
                    "/api/users/{id}": {
                      "get": {
                        "summary": "Get user by ID",
                        "operationId": "getUserById"
                      },
                      "put": {
                        "summary": "Update user",
                        "operationId": "updateUser"
                      },
                      "delete": {
                        "summary": "Delete user",
                        "operationId": "deleteUser"
                      }
                    },
                    "/api/health": {
                      "get": {
                        "summary": "Health check",
                        "operationId": "healthCheck"
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
        ServiceInfo result = future.get(10, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEndpoints()).isNotNull();
        
        Set<String> endpoints = result.getEndpoints();
        
        // Verify paths are discovered
        assertThat(endpoints).contains("/api/users", "/api/users/{id}", "/api/health");
        
        // Verify HTTP methods are discovered
        assertThat(endpoints).contains("GET /api/users", "POST /api/users");
        assertThat(endpoints).contains("GET /api/users/{id}", "PUT /api/users/{id}", "DELETE /api/users/{id}");
        assertThat(endpoints).contains("GET /api/health");
        
        // Verify the request was made to the correct OpenAPI endpoint
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v3/api-docs");
        assertThat(request.getMethod()).isEqualTo("GET");
    }
    
    @Test
    void shouldHandleServiceFailureGracefully() throws Exception {
        // Given
        String serviceId = "failing-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Failing Service", baseUrl);
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // Mock service failure
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"error\":\"Internal Server Error\"}")
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));
        
        String correlationId = "test-correlation-failure";
        
        // When
        CompletableFuture<ServiceInteraction> future = restApiAdapter.get(
                serviceId, "/api/failing-endpoint", correlationId, null);
        ServiceInteraction result = future.get(10, TimeUnit.SECONDS);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(InteractionStatus.FAILURE);
        assertThat(result.getCorrelationId()).isEqualTo(correlationId);
        assertThat(result.getErrorMessage()).contains("500");
        assertThat(result.getResponse().toString()).contains("Internal Server Error");
        assertThat(result.getResponseTime()).isNotNull();
        assertThat(result.getResponseTime().toMillis()).isGreaterThan(0);
    }
    
    @Test
    void shouldValidateEndpointAvailability() throws Exception {
        // Given
        String serviceId = "health-service";
        String baseUrl = mockWebServer.url("/").toString();
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, "Health Service", baseUrl);
        when(serviceDiscovery.getService(serviceId))
                .thenReturn(Optional.of(serviceInfo));
        
        // Mock healthy endpoint
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        // Mock unhealthy endpoint
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        
        // When
        CompletableFuture<Boolean> healthyResult = restApiAdapter.isEndpointAvailable(serviceId, "/api/health");
        CompletableFuture<Boolean> unhealthyResult = restApiAdapter.isEndpointAvailable(serviceId, "/api/unhealthy");
        
        // Then
        assertThat(healthyResult.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(unhealthyResult.get(5, TimeUnit.SECONDS)).isFalse();
        
        // Verify HEAD requests were made
        RecordedRequest healthRequest = mockWebServer.takeRequest();
        RecordedRequest unhealthyRequest = mockWebServer.takeRequest();
        
        assertThat(healthRequest.getMethod()).isEqualTo("HEAD");
        assertThat(healthRequest.getPath()).isEqualTo("/api/health");
        assertThat(unhealthyRequest.getMethod()).isEqualTo("HEAD");
        assertThat(unhealthyRequest.getPath()).isEqualTo("/api/unhealthy");
    }
}