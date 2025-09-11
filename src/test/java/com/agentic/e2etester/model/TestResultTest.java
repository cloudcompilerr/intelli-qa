package com.agentic.e2etester.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestResultTest {
    
    private Validator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Test
    void testValidTestResult() {
        TestResult result = createValidTestResult();
        
        Set<ConstraintViolation<TestResult>> violations = validator.validate(result);
        assertTrue(violations.isEmpty(), "Valid test result should not have violations");
    }
    
    @Test
    void testInvalidTestResult_BlankTestId() {
        TestResult result = createValidTestResult();
        result.setTestId("");
        
        Set<ConstraintViolation<TestResult>> violations = validator.validate(result);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Test ID cannot be blank")));
    }
    
    @Test
    void testInvalidTestResult_NullStatus() {
        TestResult result = createValidTestResult();
        result.setStatus(null);
        
        Set<ConstraintViolation<TestResult>> violations = validator.validate(result);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Test status cannot be null")));
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        String testId = "test-123";
        TestStatus status = TestStatus.PASSED;
        
        TestResult result = new TestResult(testId, status);
        
        assertEquals(testId, result.getTestId());
        assertEquals(status, result.getStatus());
        assertNotNull(result.getStartTime());
    }
    
    @Test
    void testExecutionTimeCalculation() {
        TestResult result = createValidTestResult();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(30);
        
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        
        assertEquals(30000L, result.getExecutionTimeMs());
        assertEquals(Duration.ofSeconds(30), result.getExecutionTime());
    }
    
    @Test
    void testSuccessRateCalculation() {
        TestResult result = createValidTestResult();
        
        result.setTotalSteps(10);
        result.setSuccessfulSteps(8);
        result.setFailedSteps(2);
        
        assertEquals(0.8, result.getSuccessRate(), 0.001);
        
        // Test with zero total steps
        result.setTotalSteps(0);
        assertEquals(0.0, result.getSuccessRate(), 0.001);
        
        // Test with null values
        result.setTotalSteps(null);
        result.setSuccessfulSteps(null);
        assertEquals(0.0, result.getSuccessRate(), 0.001);
    }
    
    @Test
    void testStatusUtilityMethods() {
        TestResult result = createValidTestResult();
        
        result.setStatus(TestStatus.PASSED);
        assertTrue(result.isSuccessful());
        assertFalse(result.isFailed());
        
        result.setStatus(TestStatus.FAILED);
        assertFalse(result.isSuccessful());
        assertTrue(result.isFailed());
        
        result.setStatus(TestStatus.RUNNING);
        assertFalse(result.isSuccessful());
        assertFalse(result.isFailed());
    }
    
    @Test
    void testSerialization() throws Exception {
        TestResult result = createValidTestResult();
        
        String json = objectMapper.writeValueAsString(result);
        assertNotNull(json);
        assertTrue(json.contains("test-result-1"));
        assertTrue(json.contains("passed"));
        
        TestResult deserialized = objectMapper.readValue(json, TestResult.class);
        assertEquals(result.getTestId(), deserialized.getTestId());
        assertEquals(result.getStatus(), deserialized.getStatus());
        assertEquals(result.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(result.getTotalSteps(), deserialized.getTotalSteps());
    }
    
    @Test
    void testEqualsAndHashCode() {
        Instant fixedTime = Instant.parse("2023-01-01T00:00:00Z");
        TestResult result1 = createValidTestResult();
        result1.setStartTime(fixedTime);
        TestResult result2 = createValidTestResult();
        result2.setStartTime(fixedTime);
        
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
        
        result2.setTestId("different-test");
        assertNotEquals(result1, result2);
        assertNotEquals(result1.hashCode(), result2.hashCode());
    }
    
    @Test
    void testToString() {
        TestResult result = createValidTestResult();
        String toString = result.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("TestResult"));
        assertTrue(toString.contains("test-result-1"));
        // Check for the actual enum value representation
        assertTrue(toString.contains("PASSED") || toString.contains("passed"));
        assertTrue(toString.contains("totalSteps=5"));
    }
    
    @Test
    void testTestStatusEnumSerialization() throws Exception {
        // Test all test statuses
        for (TestStatus status : TestStatus.values()) {
            TestResult result = createValidTestResult();
            result.setStatus(status);
            
            String json = objectMapper.writeValueAsString(result);
            TestResult deserialized = objectMapper.readValue(json, TestResult.class);
            
            assertEquals(status, deserialized.getStatus());
        }
    }
    
    @Test
    void testStepResultsHandling() {
        TestResult result = createValidTestResult();
        
        StepResult stepResult1 = new StepResult("step-1", TestStatus.PASSED);
        StepResult stepResult2 = new StepResult("step-2", TestStatus.FAILED);
        
        result.setStepResults(Arrays.asList(stepResult1, stepResult2));
        
        assertEquals(2, result.getStepResults().size());
        assertEquals("step-1", result.getStepResults().get(0).getStepId());
        assertEquals("step-2", result.getStepResults().get(1).getStepId());
    }
    
    @Test
    void testAssertionResultsHandling() {
        TestResult result = createValidTestResult();
        
        AssertionResult assertionResult1 = new AssertionResult("rule-1", true);
        AssertionResult assertionResult2 = new AssertionResult("rule-2", false);
        
        result.setAssertionResults(Arrays.asList(assertionResult1, assertionResult2));
        
        assertEquals(2, result.getAssertionResults().size());
        assertEquals("rule-1", result.getAssertionResults().get(0).getRuleId());
        assertEquals("rule-2", result.getAssertionResults().get(1).getRuleId());
    }
    
    @Test
    void testMetricsHandling() {
        TestResult result = createValidTestResult();
        
        TestMetrics metrics = new TestMetrics();
        metrics.setTotalRequests(100);
        metrics.setSuccessfulRequests(95);
        metrics.setFailedRequests(5);
        metrics.setAverageResponseTimeMs(150L);
        
        result.setMetrics(metrics);
        
        assertNotNull(result.getMetrics());
        assertEquals(100, result.getMetrics().getTotalRequests());
        assertEquals(95, result.getMetrics().getSuccessfulRequests());
        assertEquals(5, result.getMetrics().getFailedRequests());
        assertEquals(150L, result.getMetrics().getAverageResponseTimeMs());
    }
    
    private TestResult createValidTestResult() {
        TestResult result = new TestResult();
        result.setTestId("test-result-1");
        result.setStatus(TestStatus.PASSED);
        result.setStartTime(Instant.now().minusSeconds(30));
        result.setEndTime(Instant.now());
        result.setCorrelationId("test-correlation-123");
        result.setSummary("Test completed successfully");
        result.setTotalSteps(5);
        result.setSuccessfulSteps(5);
        result.setFailedSteps(0);
        
        return result;
    }
}