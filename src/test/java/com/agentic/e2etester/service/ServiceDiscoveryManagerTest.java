package com.agentic.e2etester.service;

import com.agentic.e2etester.model.HealthCheckResult;
import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceDiscoveryManagerTest {
    
    @Mock
    private HealthChecker mockHealthChecker;
    
    private ServiceDiscoveryManager serviceDiscoveryManager;
    
    @BeforeEach
    void setUp() {
        List<HealthChecker> healthCheckers = Arrays.asList(mockHealthChecker);
        serviceDiscoveryManager = new ServiceDiscoveryManager(healthCheckers);
        
        // Set test configuration values
        ReflectionTestUtils.setField(serviceDiscoveryManager, "healthCheckIntervalSeconds", 1);
        ReflectionTestUtils.setField(serviceDiscoveryManager, "discoveryIntervalSeconds", 1);
        ReflectionTestUtils.setField(serviceDiscoveryManager, "autoDiscoveryEnabled", false);
    }
    
    @Test
    void testRegisterService() {
        // Given
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(true);
        when(mockHealthChecker.checkHealth(any(ServiceInfo.class)))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-1")));
        
        // When
        ServiceInfo registered = serviceDiscoveryManager.registerService(serviceInfo);
        
        // Then
        assertNotNull(registered);
        assertEquals("svc-1", registered.getServiceId());
        assertEquals("Service 1", registered.getServiceName());
        assertEquals("http://localhost:8080", registered.getBaseUrl());
        
        // Verify service is in registry
        Optional<ServiceInfo> retrieved = serviceDiscoveryManager.getService("svc-1");
        assertTrue(retrieved.isPresent());
        assertEquals("svc-1", retrieved.get().getServiceId());
    }
    
    @Test
    void testRegisterServiceWithNullServiceInfo() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            serviceDiscoveryManager.registerService(null);
        });
    }
    
    @Test
    void testRegisterServiceWithNullServiceId() {
        // Given
        ServiceInfo serviceInfo = new ServiceInfo(null, "Service 1", "http://localhost:8080");
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            serviceDiscoveryManager.registerService(serviceInfo);
        });
    }
    
    @Test
    void testUnregisterService() {
        // Given
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(true);
        when(mockHealthChecker.checkHealth(any(ServiceInfo.class)))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-1")));
        
        serviceDiscoveryManager.registerService(serviceInfo);
        
        // When
        boolean unregistered = serviceDiscoveryManager.unregisterService("svc-1");
        
        // Then
        assertTrue(unregistered);
        assertFalse(serviceDiscoveryManager.getService("svc-1").isPresent());
    }
    
    @Test
    void testUnregisterNonExistentService() {
        // When
        boolean unregistered = serviceDiscoveryManager.unregisterService("non-existent");
        
        // Then
        assertFalse(unregistered);
    }
    
    @Test
    void testUnregisterServiceWithNullId() {
        // When
        boolean unregistered = serviceDiscoveryManager.unregisterService(null);
        
        // Then
        assertFalse(unregistered);
    }
    
    @Test
    void testGetAllServices() {
        // Given
        ServiceInfo service1 = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        ServiceInfo service2 = new ServiceInfo("svc-2", "Service 2", "http://localhost:8081");
        
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(true);
        when(mockHealthChecker.checkHealth(any(ServiceInfo.class)))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-1")))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-2")));
        
        serviceDiscoveryManager.registerService(service1);
        serviceDiscoveryManager.registerService(service2);
        
        // When
        List<ServiceInfo> allServices = serviceDiscoveryManager.getAllServices();
        
        // Then
        assertEquals(2, allServices.size());
        assertTrue(allServices.stream().anyMatch(s -> "svc-1".equals(s.getServiceId())));
        assertTrue(allServices.stream().anyMatch(s -> "svc-2".equals(s.getServiceId())));
    }
    
    @Test
    void testGetServicesByName() {
        // Given
        ServiceInfo service1 = new ServiceInfo("svc-1", "Order Service", "http://localhost:8080");
        ServiceInfo service2 = new ServiceInfo("svc-2", "Order Service", "http://localhost:8081");
        ServiceInfo service3 = new ServiceInfo("svc-3", "Payment Service", "http://localhost:8082");
        
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(true);
        when(mockHealthChecker.checkHealth(any(ServiceInfo.class)))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-1")))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-2")))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-3")));
        
        serviceDiscoveryManager.registerService(service1);
        serviceDiscoveryManager.registerService(service2);
        serviceDiscoveryManager.registerService(service3);
        
        // When
        List<ServiceInfo> orderServices = serviceDiscoveryManager.getServicesByName("Order Service");
        List<ServiceInfo> paymentServices = serviceDiscoveryManager.getServicesByName("Payment Service");
        List<ServiceInfo> nonExistentServices = serviceDiscoveryManager.getServicesByName("Non-existent Service");
        
        // Then
        assertEquals(2, orderServices.size());
        assertEquals(1, paymentServices.size());
        assertEquals(0, nonExistentServices.size());
        
        assertTrue(orderServices.stream().allMatch(s -> "Order Service".equals(s.getServiceName())));
        assertEquals("Payment Service", paymentServices.get(0).getServiceName());
    }
    
    @Test
    void testGetServicesByNameWithNull() {
        // When
        List<ServiceInfo> services = serviceDiscoveryManager.getServicesByName(null);
        
        // Then
        assertTrue(services.isEmpty());
    }
    
    @Test
    void testCheckServiceHealth() {
        // Given
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        HealthCheckResult expectedResult = HealthCheckResult.healthy("svc-1");
        
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(true);
        when(mockHealthChecker.checkHealth(any(ServiceInfo.class)))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));
        
        serviceDiscoveryManager.registerService(serviceInfo);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = serviceDiscoveryManager.checkServiceHealth("svc-1");
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.HEALTHY, result.getStatus());
        assertTrue(result.isHealthy());
    }
    
    @Test
    void testCheckServiceHealthForNonExistentService() {
        // When
        CompletableFuture<HealthCheckResult> resultFuture = serviceDiscoveryManager.checkServiceHealth("non-existent");
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("non-existent", result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertFalse(result.isHealthy());
        assertEquals("Service not found in registry", result.getStatusMessage());
    }
    
    @Test
    void testCheckServiceHealthWithNoSupportingHealthChecker() {
        // Given
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(false);
        
        serviceDiscoveryManager.registerService(serviceInfo);
        
        // When
        CompletableFuture<HealthCheckResult> resultFuture = serviceDiscoveryManager.checkServiceHealth("svc-1");
        HealthCheckResult result = resultFuture.join();
        
        // Then
        assertNotNull(result);
        assertEquals("svc-1", result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertFalse(result.isHealthy());
        assertEquals("No health checker available", result.getStatusMessage());
    }
    
    @Test
    void testCheckAllServicesHealth() {
        // Given
        ServiceInfo service1 = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        ServiceInfo service2 = new ServiceInfo("svc-2", "Service 2", "http://localhost:8081");
        
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(true);
        when(mockHealthChecker.checkHealth(service1))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-1")));
        when(mockHealthChecker.checkHealth(service2))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.unhealthy("svc-2", "Service down")));
        
        serviceDiscoveryManager.registerService(service1);
        serviceDiscoveryManager.registerService(service2);
        
        // When
        CompletableFuture<List<HealthCheckResult>> resultsFuture = serviceDiscoveryManager.checkAllServicesHealth();
        List<HealthCheckResult> results = resultsFuture.join();
        
        // Then
        assertEquals(2, results.size());
        
        HealthCheckResult result1 = results.stream()
            .filter(r -> "svc-1".equals(r.getServiceId()))
            .findFirst()
            .orElse(null);
        assertNotNull(result1);
        assertTrue(result1.isHealthy());
        
        HealthCheckResult result2 = results.stream()
            .filter(r -> "svc-2".equals(r.getServiceId()))
            .findFirst()
            .orElse(null);
        assertNotNull(result2);
        assertFalse(result2.isHealthy());
    }
    
    @Test
    void testGetStatistics() {
        // Given
        ServiceInfo service1 = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        service1.setStatus(ServiceStatus.HEALTHY);
        ServiceInfo service2 = new ServiceInfo("svc-2", "Service 2", "http://localhost:8081");
        service2.setStatus(ServiceStatus.UNHEALTHY);
        
        when(mockHealthChecker.supports(any(ServiceInfo.class))).thenReturn(true);
        when(mockHealthChecker.checkHealth(any(ServiceInfo.class)))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.healthy("svc-1")))
            .thenReturn(CompletableFuture.completedFuture(HealthCheckResult.unhealthy("svc-2", "Down")));
        
        serviceDiscoveryManager.registerService(service1);
        serviceDiscoveryManager.registerService(service2);
        
        // When
        Map<String, Object> statistics = serviceDiscoveryManager.getStatistics();
        
        // Then
        assertNotNull(statistics);
        assertEquals(2, statistics.get("totalServices"));
        assertEquals(1, statistics.get("healthCheckersCount"));
        assertEquals(false, statistics.get("autoDiscoveryEnabled"));
        
        @SuppressWarnings("unchecked")
        Map<ServiceStatus, Long> statusCounts = (Map<ServiceStatus, Long>) statistics.get("servicesByStatus");
        assertNotNull(statusCounts);
    }
    
    @Test
    void testDiscoverServices() {
        // When
        CompletableFuture<List<ServiceInfo>> discoveryFuture = serviceDiscoveryManager.discoverServices();
        List<ServiceInfo> discoveredServices = discoveryFuture.join();
        
        // Then
        assertNotNull(discoveredServices);
        // Currently returns empty list as discovery mechanisms are not implemented
        assertTrue(discoveredServices.isEmpty());
    }
}