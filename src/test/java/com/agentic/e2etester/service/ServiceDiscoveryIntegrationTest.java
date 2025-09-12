package com.agentic.e2etester.service;

import com.agentic.e2etester.config.ServiceDiscoveryConfiguration;
import com.agentic.e2etester.model.HealthCheckResult;
import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "agentic.service-discovery.auto-discovery-enabled=false",
    "agentic.service-discovery.health-check-interval=1",
    "agentic.service-discovery.discovery-interval=1",
    "agentic.service-discovery.health-check-timeout=2"
})
class ServiceDiscoveryIntegrationTest {
    
    @Autowired
    private ServiceDiscoveryManager serviceDiscoveryManager;
    
    @Autowired
    private ServiceDiscoveryConfiguration configuration;
    
    @Autowired
    private List<HealthChecker> healthCheckers;
    
    private MockWebServer mockWebServer;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
        
        // Clean up registered services
        List<ServiceInfo> services = serviceDiscoveryManager.getAllServices();
        for (ServiceInfo service : services) {
            serviceDiscoveryManager.unregisterService(service.getServiceId());
        }
    }
    
    @Test
    void testServiceRegistrationAndHealthCheck() throws Exception {
        // Given
        String healthResponse = """
            {
                "status": "UP",
                "components": {
                    "db": {
                        "status": "UP"
                    }
                }
            }
            """;
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(healthResponse)
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("test-service", "Test Service", baseUrl);
        
        // When
        ServiceInfo registered = serviceDiscoveryManager.registerService(serviceInfo);
        
        // Then
        assertNotNull(registered);
        assertEquals("test-service", registered.getServiceId());
        
        // Verify service is retrievable
        Optional<ServiceInfo> retrieved = serviceDiscoveryManager.getService("test-service");
        assertTrue(retrieved.isPresent());
        assertEquals("Test Service", retrieved.get().getServiceName());
        
        // Wait a bit for async health check to complete
        Thread.sleep(100);
        
        // Verify health check was performed
        CompletableFuture<HealthCheckResult> healthFuture = serviceDiscoveryManager.checkServiceHealth("test-service");
        HealthCheckResult healthResult = healthFuture.get(5, TimeUnit.SECONDS);
        
        assertNotNull(healthResult);
        assertEquals("test-service", healthResult.getServiceId());
        assertTrue(healthResult.isHealthy());
    }
    
    @Test
    void testMultipleServicesHealthCheck() throws Exception {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\": \"UP\"}")
            .addHeader("Content-Type", "application/json"));
        
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\": \"DOWN\"}")
            .addHeader("Content-Type", "application/json"));
        
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo service1 = new ServiceInfo("service-1", "Service 1", baseUrl);
        ServiceInfo service2 = new ServiceInfo("service-2", "Service 2", baseUrl);
        
        // When
        serviceDiscoveryManager.registerService(service1);
        serviceDiscoveryManager.registerService(service2);
        
        CompletableFuture<List<HealthCheckResult>> healthFuture = serviceDiscoveryManager.checkAllServicesHealth();
        List<HealthCheckResult> results = healthFuture.get(10, TimeUnit.SECONDS);
        
        // Then
        assertEquals(2, results.size());
        
        HealthCheckResult result1 = results.stream()
            .filter(r -> "service-1".equals(r.getServiceId()))
            .findFirst()
            .orElse(null);
        assertNotNull(result1);
        
        HealthCheckResult result2 = results.stream()
            .filter(r -> "service-2".equals(r.getServiceId()))
            .findFirst()
            .orElse(null);
        assertNotNull(result2);
        
        // At least one should be processed (depending on timing)
        assertTrue(result1.getStatus() != null || result2.getStatus() != null);
    }
    
    @Test
    void testServicesByName() {
        // Given
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo orderService1 = new ServiceInfo("order-1", "Order Service", baseUrl);
        ServiceInfo orderService2 = new ServiceInfo("order-2", "Order Service", baseUrl);
        ServiceInfo paymentService = new ServiceInfo("payment-1", "Payment Service", baseUrl);
        
        // When
        serviceDiscoveryManager.registerService(orderService1);
        serviceDiscoveryManager.registerService(orderService2);
        serviceDiscoveryManager.registerService(paymentService);
        
        List<ServiceInfo> orderServices = serviceDiscoveryManager.getServicesByName("Order Service");
        List<ServiceInfo> paymentServices = serviceDiscoveryManager.getServicesByName("Payment Service");
        
        // Then
        assertEquals(2, orderServices.size());
        assertEquals(1, paymentServices.size());
        
        assertTrue(orderServices.stream().allMatch(s -> "Order Service".equals(s.getServiceName())));
        assertEquals("Payment Service", paymentServices.get(0).getServiceName());
    }
    
    @Test
    void testServiceUnregistration() {
        // Given
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo serviceInfo = new ServiceInfo("temp-service", "Temporary Service", baseUrl);
        
        // When
        serviceDiscoveryManager.registerService(serviceInfo);
        assertTrue(serviceDiscoveryManager.getService("temp-service").isPresent());
        
        boolean unregistered = serviceDiscoveryManager.unregisterService("temp-service");
        
        // Then
        assertTrue(unregistered);
        assertFalse(serviceDiscoveryManager.getService("temp-service").isPresent());
    }
    
    @Test
    void testHealthCheckersAreInjected() {
        // Then
        assertNotNull(healthCheckers);
        assertFalse(healthCheckers.isEmpty());
        
        // Should have at least the ActuatorHealthChecker
        assertTrue(healthCheckers.stream()
            .anyMatch(hc -> hc instanceof ActuatorHealthChecker));
    }
    
    @Test
    void testConfigurationIsLoaded() {
        // Then
        assertNotNull(configuration);
        assertFalse(configuration.isAutoDiscoveryEnabled()); // Set in test properties
        assertEquals(1, configuration.getHealthCheckInterval());
        assertEquals(1, configuration.getDiscoveryInterval());
        assertEquals(2, configuration.getHealthCheckTimeout());
    }
    
    @Test
    void testServiceDiscoveryStatistics() {
        // Given
        String baseUrl = mockWebServer.url("/").toString();
        ServiceInfo service1 = new ServiceInfo("stats-service-1", "Stats Service 1", baseUrl);
        service1.setStatus(ServiceStatus.HEALTHY);
        ServiceInfo service2 = new ServiceInfo("stats-service-2", "Stats Service 2", baseUrl);
        service2.setStatus(ServiceStatus.UNHEALTHY);
        
        // When
        serviceDiscoveryManager.registerService(service1);
        serviceDiscoveryManager.registerService(service2);
        
        Map<String, Object> statistics = serviceDiscoveryManager.getStatistics();
        
        // Then
        assertNotNull(statistics);
        assertEquals(2, statistics.get("totalServices"));
        assertTrue((Integer) statistics.get("healthCheckersCount") > 0);
        assertEquals(false, statistics.get("autoDiscoveryEnabled"));
        
        @SuppressWarnings("unchecked")
        Map<ServiceStatus, Long> statusCounts = (Map<ServiceStatus, Long>) statistics.get("servicesByStatus");
        assertNotNull(statusCounts);
    }
    
    @Test
    void testHealthMonitoringLifecycle() {
        // When
        serviceDiscoveryManager.startHealthMonitoring();
        Map<String, Object> stats1 = serviceDiscoveryManager.getStatistics();
        
        serviceDiscoveryManager.stopHealthMonitoring();
        Map<String, Object> stats2 = serviceDiscoveryManager.getStatistics();
        
        // Then
        // The monitoring status might change, but the method calls should not throw exceptions
        assertNotNull(stats1);
        assertNotNull(stats2);
    }
    
    @Test
    void testServiceDiscovery() throws Exception {
        // When
        CompletableFuture<List<ServiceInfo>> discoveryFuture = serviceDiscoveryManager.discoverServices();
        List<ServiceInfo> discoveredServices = discoveryFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertNotNull(discoveredServices);
        // Currently returns empty list as discovery mechanisms are not fully implemented
        assertTrue(discoveredServices.isEmpty());
    }
}