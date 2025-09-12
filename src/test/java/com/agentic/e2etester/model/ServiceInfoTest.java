package com.agentic.e2etester.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ServiceInfoTest {
    
    @Test
    void testDefaultConstructor() {
        ServiceInfo serviceInfo = new ServiceInfo();
        assertNull(serviceInfo.getServiceId());
        assertNull(serviceInfo.getServiceName());
        assertNull(serviceInfo.getBaseUrl());
        assertNull(serviceInfo.getVersion());
        assertNull(serviceInfo.getStatus());
        assertNull(serviceInfo.getLastHealthCheck());
        assertNull(serviceInfo.getMetadata());
        assertNull(serviceInfo.getEndpoints());
        assertNull(serviceInfo.getHealthDetails());
    }
    
    @Test
    void testParameterizedConstructor() {
        String serviceId = "test-service-1";
        String serviceName = "Test Service";
        String baseUrl = "http://localhost:8080";
        
        ServiceInfo serviceInfo = new ServiceInfo(serviceId, serviceName, baseUrl);
        
        assertEquals(serviceId, serviceInfo.getServiceId());
        assertEquals(serviceName, serviceInfo.getServiceName());
        assertEquals(baseUrl, serviceInfo.getBaseUrl());
        assertEquals(ServiceStatus.UNKNOWN, serviceInfo.getStatus());
        assertNotNull(serviceInfo.getLastHealthCheck());
        assertTrue(serviceInfo.getLastHealthCheck().isBefore(Instant.now().plusSeconds(1)));
    }
    
    @Test
    void testSettersAndGetters() {
        ServiceInfo serviceInfo = new ServiceInfo();
        
        // Test all setters and getters
        serviceInfo.setServiceId("service-1");
        assertEquals("service-1", serviceInfo.getServiceId());
        
        serviceInfo.setServiceName("My Service");
        assertEquals("My Service", serviceInfo.getServiceName());
        
        serviceInfo.setBaseUrl("http://example.com");
        assertEquals("http://example.com", serviceInfo.getBaseUrl());
        
        serviceInfo.setVersion("2.1.0");
        assertEquals("2.1.0", serviceInfo.getVersion());
        
        serviceInfo.setStatus(ServiceStatus.HEALTHY);
        assertEquals(ServiceStatus.HEALTHY, serviceInfo.getStatus());
        
        Instant now = Instant.now();
        serviceInfo.setLastHealthCheck(now);
        assertEquals(now, serviceInfo.getLastHealthCheck());
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "microservice");
        serviceInfo.setMetadata(metadata);
        assertEquals(metadata, serviceInfo.getMetadata());
        
        Set<String> endpoints = new HashSet<>();
        endpoints.add("/api/v1/orders");
        serviceInfo.setEndpoints(endpoints);
        assertEquals(endpoints, serviceInfo.getEndpoints());
        
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("status", "UP");
        serviceInfo.setHealthDetails(healthDetails);
        assertEquals(healthDetails, serviceInfo.getHealthDetails());
    }
    
    @Test
    void testToString() {
        ServiceInfo serviceInfo = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        serviceInfo.setStatus(ServiceStatus.HEALTHY);
        
        String toString = serviceInfo.toString();
        
        assertTrue(toString.contains("svc-1"));
        assertTrue(toString.contains("Service 1"));
        assertTrue(toString.contains("http://localhost:8080"));
        assertTrue(toString.contains("HEALTHY"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        ServiceInfo service1 = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        ServiceInfo service2 = new ServiceInfo("svc-1", "Service 1", "http://localhost:8080");
        ServiceInfo service3 = new ServiceInfo("svc-2", "Service 2", "http://localhost:8081");
        
        // Note: ServiceInfo doesn't override equals/hashCode, so this tests object identity
        assertNotEquals(service1, service2); // Different objects
        assertEquals(service1, service1); // Same object
        assertNotEquals(service1, service3); // Different objects
    }
    
    @Test
    void testNullValues() {
        ServiceInfo serviceInfo = new ServiceInfo();
        
        // Test that null values are handled gracefully
        serviceInfo.setServiceId(null);
        assertNull(serviceInfo.getServiceId());
        
        serviceInfo.setServiceName(null);
        assertNull(serviceInfo.getServiceName());
        
        serviceInfo.setBaseUrl(null);
        assertNull(serviceInfo.getBaseUrl());
        
        serviceInfo.setVersion(null);
        assertNull(serviceInfo.getVersion());
        
        serviceInfo.setStatus(null);
        assertNull(serviceInfo.getStatus());
        
        serviceInfo.setLastHealthCheck(null);
        assertNull(serviceInfo.getLastHealthCheck());
        
        serviceInfo.setMetadata(null);
        assertNull(serviceInfo.getMetadata());
        
        serviceInfo.setEndpoints(null);
        assertNull(serviceInfo.getEndpoints());
        
        serviceInfo.setHealthDetails(null);
        assertNull(serviceInfo.getHealthDetails());
    }
}