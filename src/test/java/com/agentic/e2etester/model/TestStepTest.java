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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestStepTest {
    
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
    void testValidTestStep() {
        TestStep step = createValidTestStep();
        
        Set<ConstraintViolation<TestStep>> violations = validator.validate(step);
        assertTrue(violations.isEmpty(), "Valid test step should not have violations");
    }
    
    @Test
    void testInvalidTestStep_BlankStepId() {
        TestStep step = createValidTestStep();
        step.setStepId("");
        
        Set<ConstraintViolation<TestStep>> violations = validator.validate(step);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Step ID cannot be blank")));
    }
    
    @Test
    void testInvalidTestStep_NullStepType() {
        TestStep step = createValidTestStep();
        step.setType(null);
        
        Set<ConstraintViolation<TestStep>> violations = validator.validate(step);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Step type cannot be null")));
    }
    
    @Test
    void testInvalidTestStep_BlankTargetService() {
        TestStep step = createValidTestStep();
        step.setTargetService("");
        
        Set<ConstraintViolation<TestStep>> violations = validator.validate(step);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Target service cannot be blank")));
    }
    
    @Test
    void testInvalidTestStep_NullTimeout() {
        TestStep step = createValidTestStep();
        step.setTimeoutMs(null);
        
        Set<ConstraintViolation<TestStep>> violations = validator.validate(step);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Timeout cannot be null")));
    }
    
    @Test
    void testInvalidTestStep_NegativeOrder() {
        TestStep step = createValidTestStep();
        step.setOrder(-1);
        
        Set<ConstraintViolation<TestStep>> violations = validator.validate(step);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Order must be positive")));
    }
    
    @Test
    void testTimeoutDurationConversion() {
        TestStep step = createValidTestStep();
        step.setTimeoutMs(30000L);
        
        Duration timeout = step.getTimeout();
        assertNotNull(timeout);
        assertEquals(30, timeout.getSeconds());
        
        step.setTimeout(Duration.ofMinutes(2));
        assertEquals(120000L, step.getTimeoutMs());
    }
    
    @Test
    void testSerialization() throws Exception {
        TestStep step = createValidTestStep();
        
        String json = objectMapper.writeValueAsString(step);
        assertNotNull(json);
        assertTrue(json.contains("test-step-1"));
        assertTrue(json.contains("rest_call"));
        assertTrue(json.contains("test-service"));
        
        TestStep deserialized = objectMapper.readValue(json, TestStep.class);
        assertEquals(step.getStepId(), deserialized.getStepId());
        assertEquals(step.getType(), deserialized.getType());
        assertEquals(step.getTargetService(), deserialized.getTargetService());
        assertEquals(step.getTimeoutMs(), deserialized.getTimeoutMs());
    }
    
    @Test
    void testEqualsAndHashCode() {
        TestStep step1 = createValidTestStep();
        TestStep step2 = createValidTestStep();
        
        assertEquals(step1, step2);
        assertEquals(step1.hashCode(), step2.hashCode());
        
        step2.setStepId("different-step");
        assertNotEquals(step1, step2);
        assertNotEquals(step1.hashCode(), step2.hashCode());
    }
    
    @Test
    void testToString() {
        TestStep step = createValidTestStep();
        String toString = step.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("TestStep"));
        assertTrue(toString.contains("test-step-1"));
        assertTrue(toString.contains("test-service"));
        // Check for the actual enum value representation
        assertTrue(toString.contains("REST_CALL") || toString.contains("rest_call"));
    }
    
    @Test
    void testStepTypeEnumSerialization() throws Exception {
        // Test all step types
        for (StepType type : StepType.values()) {
            TestStep step = createValidTestStep();
            step.setType(type);
            
            String json = objectMapper.writeValueAsString(step);
            TestStep deserialized = objectMapper.readValue(json, TestStep.class);
            
            assertEquals(type, deserialized.getType());
        }
    }
    
    @Test
    void testInputDataHandling() {
        TestStep step = createValidTestStep();
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("key1", "value1");
        inputData.put("key2", 123);
        inputData.put("key3", true);
        
        step.setInputData(inputData);
        
        assertEquals(inputData, step.getInputData());
        assertEquals("value1", step.getInputData().get("key1"));
        assertEquals(123, step.getInputData().get("key2"));
        assertEquals(true, step.getInputData().get("key3"));
    }
    
    private TestStep createValidTestStep() {
        TestStep step = new TestStep();
        step.setStepId("test-step-1");
        step.setType(StepType.REST_CALL);
        step.setTargetService("test-service");
        step.setTimeoutMs(30000L);
        step.setDescription("Test step description");
        step.setOrder(1);
        step.setDependsOn(Arrays.asList());
        
        // Add some expected outcomes
        ExpectedOutcome outcome = new ExpectedOutcome();
        outcome.setOutcomeId("outcome-1");
        outcome.setType(OutcomeType.SUCCESS_RESPONSE);
        outcome.setDescription("Expect successful response");
        step.setExpectedOutcomes(Arrays.asList(outcome));
        
        // Add retry policy
        RetryPolicy retryPolicy = new RetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryPolicy.setInitialDelayMs(1000L);
        step.setRetryPolicy(retryPolicy);
        
        return step;
    }
}