package com.agentic.e2etester.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthCheckResultTest {
    
    @Test
    void testDefaultConstructor() {
        HealthCheckResult result = new HealthCheckResult();
        assertNull(result.getServiceId());
        assertNull(result.getStatus());
        assertNull(result.getTimestamp());
        assertNull(result.getResponseTime());
        assertNull(result.getStatusMessage());
        assertNull(result.getDetails());
        assertNull(result.getError());
        assertFalse(result.isHealthy());
    }
    
    @Test
    void testParameterizedConstructor() {
        String serviceId = "test-service";
        ServiceStatus status = ServiceStatus.HEALTHY;
        
        HealthCheckResult result = new HealthCheckResult(serviceId, status);
        
        assertEquals(serviceId, result.getServiceId());
        assertEquals(status, result.getStatus());
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(result.isHealthy());
    }
    
    @Test
    void testHealthyFactoryMethod() {
        String serviceId = "healthy-service";
        
        HealthCheckResult result = HealthCheckResult.healthy(serviceId);
        
        assertEquals(serviceId, result.getServiceId());
        assertEquals(ServiceStatus.HEALTHY, result.getStatus());
        assertNotNull(result.getTimestamp());
        assertTrue(result.isHealthy());
    }
    
    @Test
    void testUnhealthyFactoryMethodWithMessage() {
        String serviceId = "unhealthy-service";
        String message = "Service is down";
        
        HealthCheckResult result = HealthCheckResult.unhealthy(serviceId, message);
        
        assertEquals(serviceId, result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertEquals(message, result.getStatusMessage());
        assertNotNull(result.getTimestamp());
        assertFalse(result.isHealthy());
    }
    
    @Test
    void testUnhealthyFactoryMethodWithThrowable() {
        String serviceId = "error-service";
        RuntimeException error = new RuntimeException("Connection failed");
        
        HealthCheckResult result = HealthCheckResult.unhealthy(serviceId, error);
        
        assertEquals(serviceId, result.getServiceId());
        assertEquals(ServiceStatus.UNHEALTHY, result.getStatus());
        assertEquals(error.getMessage(), result.getStatusMessage());
        assertEquals(error, result.getError());
        assertNotNull(result.getTimestamp());
        assertFalse(result.isHealthy());
    }
    
    @Test
    void testSettersAndGetters() {
        HealthCheckResult result = new HealthCheckResult();
        
        result.setServiceId("test-service");
        assertEquals("test-service", result.getServiceId());
        
        result.setStatus(ServiceStatus.DEGRADED);
        assertEquals(ServiceStatus.DEGRADED, result.getStatus());
        
        Instant timestamp = Instant.now();
        result.setTimestamp(timestamp);
        assertEquals(timestamp, result.getTimestamp());
        
        Duration responseTime = Duration.ofMillis(150);
        result.setResponseTime(responseTime);
        assertEquals(responseTime, result.getResponseTime());
        
        result.setStatusMessage("Service responding slowly");
        assertEquals("Service responding slowly", result.getStatusMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("cpu", "85%");
        result.setDetails(details);
        assertEquals(details, result.getDetails());
        
        Exception error = new Exception("Test error");
        result.setError(error);
        assertEquals(error, result.getError());
    }
    
    @Test
    void testIsHealthy() {
        HealthCheckResult result = new HealthCheckResult();
        
        result.setStatus(ServiceStatus.HEALTHY);
        assertTrue(result.isHealthy());
        
        result.setStatus(ServiceStatus.UNHEALTHY);
        assertFalse(result.isHealthy());
        
        result.setStatus(ServiceStatus.DEGRADED);
        assertFalse(result.isHealthy());
        
        result.setStatus(ServiceStatus.UNKNOWN);
        assertFalse(result.isHealthy());
        
        result.setStatus(ServiceStatus.MAINTENANCE);
        assertFalse(result.isHealthy());
        
        result.setStatus(null);
        assertFalse(result.isHealthy());
    }
    
    @Test
    void testToString() {
        HealthCheckResult result = new HealthCheckResult("svc-1", ServiceStatus.HEALTHY);
        result.setResponseTime(Duration.ofMillis(100));
        
        String toString = result.toString();
        
        assertTrue(toString.contains("svc-1"));
        assertTrue(toString.contains("HEALTHY"));
        assertTrue(toString.contains("PT0.1S")); // Duration format
    }
    
    @Test
    void testNullValues() {
        HealthCheckResult result = new HealthCheckResult();
        
        // Test that null values are handled gracefully
        result.setServiceId(null);
        assertNull(result.getServiceId());
        
        result.setStatus(null);
        assertNull(result.getStatus());
        assertFalse(result.isHealthy()); // Should handle null status
        
        result.setTimestamp(null);
        assertNull(result.getTimestamp());
        
        result.setResponseTime(null);
        assertNull(result.getResponseTime());
        
        result.setStatusMessage(null);
        assertNull(result.getStatusMessage());
        
        result.setDetails(null);
        assertNull(result.getDetails());
        
        result.setError(null);
        assertNull(result.getError());
    }
}