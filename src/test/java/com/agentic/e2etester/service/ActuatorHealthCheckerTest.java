package com.agentic.e2etester.service;

import com.agentic.e2etester.model.HealthCheckResult;
import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ActuatorHealthCheckerTest {
    
    private ActuatorHealthChecker healthChecker;
    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws IOException {
        healthChecker = new ActuatorHealthChecker();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();
        
        // Set test configuration
        ReflectionTestUtils.setField(healthChecker, "timeoutSeconds", 5);
        ReflectionTestUtils.setField(healthChecker, "actuatorHealthPath", "/actuator/health");
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testSupports() {
        // Test with valid service info
        ServiceInfo validService = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        assertTrue(healthChecker.supports(validService));
        
        // Test with null service info
        assertFalse(healthChecker.supports(null));
        
        // Test with null base URL
        ServiceInfo nullUrlService = new ServiceInfo("svc-2", "Service 2", null);
        assertFalse(healthChecker.supports(nullUrlService));
        
        // Test with empty base URL
        ServiceInfo emptyUrlService = new ServiceInfo("svc-3", "Service 3", "");
        assertFalse(healthChecker.supports(emptyUrlService));
        
        // Test with whitespace-only base URL
        ServiceInfo whitespaceUrlService = new ServiceInfo("svc-4", "Service 4", "   ");
        assertFalse(healthChecker.supports(whitespaceUrlService));
    }
    
    @Test
    void testCheckHealthWithHealthyResponse() throws Exception {
        // Given
        String healthResponse = """
            {
                "status": "UP",
                "components": {
                    "db": {
                        "status": "UP",
                        "details": {
                            "database": "PostgreSQL",
                            "validationQuery": "isValid()"
                        }
                    },
                    "diskSpace": {
                        "status": "UP",
                        "details": {
                            "total": 499963174912,
                            "free": 91943821312,
                            "threshold": 10485760,
                            "exists": true
                        }
                    }
                }
            }
            """;
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(healthResponse)
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.HEALTHY, result.getStatus());
        assertTrue(result.isHealthy());
        assertEquals("Actuator health status: UP", result.getStatusMessage());
        assertNotNull(result.getResponseTime());
        assertNotNull(result.getDetails());
        
        Map<String, Object> details = result.getDetails();
        assertEquals("UP", details.get("status"));
        assertTrue(details.containsKey("components"));
    }
    
    @Test
    void testCheckHealthWithUnhealthyResponse() throws Exception {
        // Given
        String healthResponse = """
            {
                "status": "DOWN",
                "components": {
                    "db": {
                        "status": "DOWN",
                        "details": {
                            "error": "Connection refused"
                        }
                    }
                }
            }
            """;
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(healthResponse)
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertFalse(result.isHealthy());
        assertEquals("Actuator health status: DOWN", result.getStatusMessage());
        assertNotNull(result.getResponseTime());
    }
    
    @Test
    void testCheckHealthWithOutOfServiceResponse() throws Exception {
        // Given
        String healthResponse = """
            {
                "status": "OUT_OF_SERVICE"
            }
            """;
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(healthResponse)
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.MAINTENANCE, result.getStatus());
        assertFalse(result.isHealthy());
    }
    
    @Test
    void testCheckHealthWithUnknownResponse() throws Exception {
        // Given
        String healthResponse = """
            {
                "status": "UNKNOWN"
            }
            """;
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(healthResponse)
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.UNKNOWN, result.getStatus());
        assertFalse(result.isHealthy());
    }
    
    @Test
    void testCheckHealthWithCustomStatus() throws Exception {
        // Given
        String healthResponse = """
            {
                "status": "DEGRADED"
            }
            """;
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(healthResponse)
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.DEGRADED, result.getStatus());
        assertFalse(result.isHealthy());
    }
    
    @Test
    void testCheckHealthWithHttpError() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertFalse(result.isHealthy());
        assertTrue(result.getStatusMessage().contains("HTTP 500"));
        assertNotNull(result.getResponseTime());
    }
    
    @Test
    void testCheckHealthWithInvalidJson() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("invalid json")
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertFalse(result.isHealthy());
        assertEquals("Failed to parse health response", result.getStatusMessage());
        assertNotNull(result.getError());
        assertNotNull(result.getResponseTime());
    }
    
    @Test
    void testCheckHealthWithConnectionError() {
        // Given - no mock response, server will refuse connection
        String baseUrl = "http://localhost:99999"; // Invalid port
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertFalse(result.isHealthy());
        assertNotNull(result.getError());
        assertNotNull(result.getResponseTime());
    }
    
    @Test
    void testBuildHealthUrlWithTrailingSlash() {
        // Given
        String baseUrl = mockWebServer.url("/").toString(); // Already has trailing slash
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        
        // Then - should not throw exception and should build correct URL
        assertDoesNotThrow(() -> resultFuture.join());
    }
    
    @Test
    void testBuildHealthUrlWithoutTrailingSlash() {
        // Given
        String baseUrl = mockWebServer.url("").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", baseUrl);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = healthChecker.checkHealth(serviceInfo);
        
        // Then - should not throw exception and should build correct URL
        assertDoesNotThrow(() -> resultFuture.join());
    }
}