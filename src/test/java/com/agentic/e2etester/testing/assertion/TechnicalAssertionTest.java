package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionSeverity;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TechnicalAssertionTest {
    
    @Test
    void testDefaultConstructor() {
        TechnicalAssertion assertion = new TechnicalAssertion();
        
        assertNotNull(assertion);
        assertTrue(assertion.getEnabled());
        assertEquals(AssertionSeverity.ERROR, assertion.getSeverity());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        TechnicalAssertion assertion = new TechnicalAssertion(
            "tech-rule-1", 
            "Response time validation", 
            "response_time"
        );
        
        assertNotNull(assertion);
        assertEquals("tech-rule-1", assertion.getRuleId());
        assertEquals("Response time validation", assertion.getDescription());
        assertEquals("response_time", assertion.getMetricName());
        assertEquals(AssertionSeverity.ERROR, assertion.getSeverity());
    }
    
    @Test
    void testSettersAndGetters() {
        TechnicalAssertion assertion = new TechnicalAssertion();
        
        assertion.setMetricName("cpu_usage");
        assertion.setServiceId("user-service");
        assertion.setEndpointPath("/api/users");
        assertion.setResponseTimeThreshold(Duration.ofMillis(1500));
        assertion.setExpectedStatusCode(200);
        assertion.setHealthCheckEndpoint("/health");
        assertion.setPerformanceThreshold(95.0);
        assertion.setPerformanceMetric("availability");
        
        assertEquals("cpu_usage", assertion.getMetricName());
        assertEquals("user-service", assertion.getServiceId());
        assertEquals("/api/users", assertion.getEndpointPath());
        assertEquals(Duration.ofMillis(1500), assertion.getResponseTimeThreshold());
        assertEquals(Integer.valueOf(200), assertion.getExpectedStatusCode());
        assertEquals("/health", assertion.getHealthCheckEndpoint());
        assertEquals(95.0, assertion.getPerformanceThreshold());
        assertEquals("availability", assertion.getPerformanceMetric());
    }
    
    @Test
    void testResponseTimeThresholdValidation() {
        TechnicalAssertion assertion = new TechnicalAssertion();
        
        // Valid positive duration
        Duration validDuration = Duration.ofMillis(1000);
        assertion.setResponseTimeThreshold(validDuration);
        assertEquals(validDuration, assertion.getResponseTimeThreshold());
        
        // Zero duration should be allowed
        Duration zeroDuration = Duration.ZERO;
        assertion.setResponseTimeThreshold(zeroDuration);
        assertEquals(zeroDuration, assertion.getResponseTimeThreshold());
    }
    
    @Test
    void testEqualsAndHashCode() {
        TechnicalAssertion assertion1 = new TechnicalAssertion("rule-1", "Test", "response_time");
        assertion1.setServiceId("service-1");
        assertion1.setEndpointPath("/api/test");
        
        TechnicalAssertion assertion2 = new TechnicalAssertion("rule-1", "Test", "response_time");
        assertion2.setServiceId("service-1");
        assertion2.setEndpointPath("/api/test");
        
        TechnicalAssertion assertion3 = new TechnicalAssertion("rule-1", "Test", "cpu_usage");
        assertion3.setServiceId("service-2");
        assertion3.setEndpointPath("/api/different");
        
        // Same rule ID, metric name, service ID, and endpoint path
        assertEquals(assertion1, assertion2);
        assertEquals(assertion1.hashCode(), assertion2.hashCode());
        
        // Different metric name, service ID, or endpoint path
        assertNotEquals(assertion1, assertion3);
        assertNotEquals(assertion1.hashCode(), assertion3.hashCode());
    }
    
    @Test
    void testToString() {
        TechnicalAssertion assertion = new TechnicalAssertion("tech-rule-1", "Test", "response_time");
        assertion.setServiceId("user-service");
        assertion.setEndpointPath("/api/users");
        assertion.setResponseTimeThreshold(Duration.ofMillis(2000));
        assertion.setExpectedStatusCode(200);
        
        String toString = assertion.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("tech-rule-1"));
        assertTrue(toString.contains("response_time"));
        assertTrue(toString.contains("user-service"));
        assertTrue(toString.contains("/api/users"));
        assertTrue(toString.contains("PT2S"));
        assertTrue(toString.contains("200"));
    }
    
    @Test
    void testInheritanceFromAssertionRule() {
        TechnicalAssertion assertion = new TechnicalAssertion();
        
        // Test inherited methods
        assertion.setRuleId("test-rule");
        assertion.setDescription("Test description");
        assertion.setExpectedValue(Duration.ofMillis(1000));
        assertion.setActualValuePath("$.metrics.responseTime");
        
        assertEquals("test-rule", assertion.getRuleId());
        assertEquals("Test description", assertion.getDescription());
        assertEquals(Duration.ofMillis(1000), assertion.getExpectedValue());
        assertEquals("$.metrics.responseTime", assertion.getActualValuePath());
    }
    
    @Test
    void testPerformanceMetrics() {
        TechnicalAssertion assertion = new TechnicalAssertion();
        
        assertion.setPerformanceMetric("throughput");
        assertion.setPerformanceThreshold(1000.0);
        
        assertEquals("throughput", assertion.getPerformanceMetric());
        assertEquals(1000.0, assertion.getPerformanceThreshold());
    }
}