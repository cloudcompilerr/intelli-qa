package com.agentic.e2etester.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestPatternTest {
    
    private TestPattern testPattern;
    
    @BeforeEach
    void setUp() {
        testPattern = new TestPattern("pattern-123", "Order Processing Pattern", PatternType.SUCCESS_FLOW);
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        assertEquals("pattern-123", testPattern.getPatternId());
        assertEquals("Order Processing Pattern", testPattern.getName());
        assertEquals(PatternType.SUCCESS_FLOW, testPattern.getType());
        assertEquals(0, testPattern.getUsageCount());
        assertEquals(0.0, testPattern.getSuccessRate());
        assertNotNull(testPattern.getCreatedAt());
    }
    
    @Test
    void testDefaultConstructor() {
        TestPattern pattern = new TestPattern();
        assertNotNull(pattern.getCreatedAt());
        assertEquals(0, pattern.getUsageCount());
        assertEquals(0.0, pattern.getSuccessRate());
    }
    
    @Test
    void testSettersAndGetters() {
        testPattern.setDescription("Test description");
        testPattern.setServiceFlow(Arrays.asList("service1", "service2", "service3"));
        
        Map<String, Object> characteristics = new HashMap<>();
        characteristics.put("avgResponseTime", 500L);
        characteristics.put("complexity", "medium");
        testPattern.setCharacteristics(characteristics);
        
        TestMetrics metrics = new TestMetrics();
        testPattern.setSuccessMetrics(metrics);
        
        testPattern.setFailurePatterns(Arrays.asList("timeout", "connection_error"));
        testPattern.setTags(Arrays.asList("order", "payment", "fulfillment"));
        
        assertEquals("Test description", testPattern.getDescription());
        assertEquals(Arrays.asList("service1", "service2", "service3"), testPattern.getServiceFlow());
        assertEquals(characteristics, testPattern.getCharacteristics());
        assertEquals(metrics, testPattern.getSuccessMetrics());
        assertEquals(Arrays.asList("timeout", "connection_error"), testPattern.getFailurePatterns());
        assertEquals(Arrays.asList("order", "payment", "fulfillment"), testPattern.getTags());
    }
    
    @Test
    void testIncrementUsage() {
        assertEquals(0, testPattern.getUsageCount());
        assertNull(testPattern.getLastUsed());
        
        Instant beforeIncrement = Instant.now();
        testPattern.incrementUsage();
        Instant afterIncrement = Instant.now();
        
        assertEquals(1, testPattern.getUsageCount());
        assertNotNull(testPattern.getLastUsed());
        assertTrue(testPattern.getLastUsed().isAfter(beforeIncrement) || 
                  testPattern.getLastUsed().equals(beforeIncrement));
        assertTrue(testPattern.getLastUsed().isBefore(afterIncrement) || 
                  testPattern.getLastUsed().equals(afterIncrement));
    }
    
    @Test
    void testUpdateSuccessRate() {
        // First usage - success
        testPattern.incrementUsage();
        testPattern.updateSuccessRate(true);
        assertEquals(1.0, testPattern.getSuccessRate(), 0.001);
        
        // Second usage - failure
        testPattern.incrementUsage();
        testPattern.updateSuccessRate(false);
        assertEquals(0.5, testPattern.getSuccessRate(), 0.001);
        
        // Third usage - success
        testPattern.incrementUsage();
        testPattern.updateSuccessRate(true);
        assertEquals(0.667, testPattern.getSuccessRate(), 0.001);
    }
    
    @Test
    void testUpdateSuccessRateWithMultipleOperations() {
        // Simulate 10 operations: 8 successes, 2 failures
        for (int i = 0; i < 8; i++) {
            testPattern.incrementUsage();
            testPattern.updateSuccessRate(true);
        }
        for (int i = 0; i < 2; i++) {
            testPattern.incrementUsage();
            testPattern.updateSuccessRate(false);
        }
        
        assertEquals(10, testPattern.getUsageCount());
        assertEquals(0.8, testPattern.getSuccessRate(), 0.001);
    }
    
    @Test
    void testAverageExecutionTime() {
        testPattern.setAverageExecutionTimeMs(1000L);
        assertEquals(1000L, testPattern.getAverageExecutionTimeMs());
    }
    
    @Test
    void testEqualsAndHashCode() {
        TestPattern pattern1 = new TestPattern("pattern-123", "Test Pattern", PatternType.SUCCESS_FLOW);
        TestPattern pattern2 = new TestPattern("pattern-123", "Different Name", PatternType.FAILURE_PATTERN);
        TestPattern pattern3 = new TestPattern("pattern-456", "Test Pattern", PatternType.SUCCESS_FLOW);
        
        // Same pattern ID should be equal
        assertEquals(pattern1, pattern2);
        assertEquals(pattern1.hashCode(), pattern2.hashCode());
        
        // Different pattern ID should not be equal
        assertNotEquals(pattern1, pattern3);
        assertNotEquals(pattern1.hashCode(), pattern3.hashCode());
        
        // Null and different class should not be equal
        assertNotEquals(pattern1, null);
        assertNotEquals(pattern1, "string");
    }
    
    @Test
    void testToString() {
        String toString = testPattern.toString();
        assertTrue(toString.contains("pattern-123"));
        assertTrue(toString.contains("Order Processing Pattern"));
        assertTrue(toString.contains("SUCCESS_FLOW"));
        assertTrue(toString.contains("usageCount=0"));
        assertTrue(toString.contains("successRate=0.0"));
    }
    
    @Test
    void testAllPatternTypes() {
        // Test all pattern types can be set
        for (PatternType type : PatternType.values()) {
            TestPattern pattern = new TestPattern("test-" + type, "Test", type);
            assertEquals(type, pattern.getType());
        }
    }
}