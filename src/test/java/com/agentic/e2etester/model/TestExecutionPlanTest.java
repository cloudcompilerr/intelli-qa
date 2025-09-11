package com.agentic.e2etester.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestExecutionPlanTest {
    
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
    void testValidTestExecutionPlan() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        
        Set<ConstraintViolation<TestExecutionPlan>> violations = validator.validate(plan);
        assertTrue(violations.isEmpty(), "Valid test execution plan should not have violations");
    }
    
    @Test
    void testInvalidTestExecutionPlan_BlankTestId() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        plan.setTestId("");
        
        Set<ConstraintViolation<TestExecutionPlan>> violations = validator.validate(plan);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Test ID cannot be blank")));
    }
    
    @Test
    void testInvalidTestExecutionPlan_BlankScenario() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        plan.setScenario("");
        
        Set<ConstraintViolation<TestExecutionPlan>> violations = validator.validate(plan);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Scenario description cannot be blank")));
    }
    
    @Test
    void testInvalidTestExecutionPlan_EmptySteps() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        plan.setSteps(List.of());
        
        Set<ConstraintViolation<TestExecutionPlan>> violations = validator.validate(plan);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Test steps cannot be empty")));
    }
    
    @Test
    void testInvalidTestExecutionPlan_NullConfiguration() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        plan.setConfiguration(null);
        
        Set<ConstraintViolation<TestExecutionPlan>> violations = validator.validate(plan);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Test configuration cannot be null")));
    }
    
    @Test
    void testInvalidTestExecutionPlan_DuplicateStepIds() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        TestStep step1 = createValidTestStep("step1");
        TestStep step2 = createValidTestStep("step1"); // Duplicate ID
        plan.setSteps(Arrays.asList(step1, step2));
        
        Set<ConstraintViolation<TestExecutionPlan>> violations = validator.validate(plan);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Duplicate step ID found: step1")));
    }
    
    @Test
    void testInvalidTestExecutionPlan_InvalidStepDependency() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        TestStep step1 = createValidTestStep("step1");
        step1.setDependsOn(Arrays.asList("nonexistent-step"));
        plan.setSteps(Arrays.asList(step1));
        
        Set<ConstraintViolation<TestExecutionPlan>> violations = validator.validate(plan);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Step dependency not found: nonexistent-step")));
    }
    
    @Test
    void testSerialization() throws Exception {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        
        String json = objectMapper.writeValueAsString(plan);
        assertNotNull(json);
        assertTrue(json.contains("test-plan-1"));
        assertTrue(json.contains("Test scenario description"));
        
        TestExecutionPlan deserialized = objectMapper.readValue(json, TestExecutionPlan.class);
        assertEquals(plan.getTestId(), deserialized.getTestId());
        assertEquals(plan.getScenario(), deserialized.getScenario());
        assertEquals(plan.getSteps().size(), deserialized.getSteps().size());
    }
    
    @Test
    void testEqualsAndHashCode() {
        TestExecutionPlan plan1 = createValidTestExecutionPlan();
        TestExecutionPlan plan2 = createValidTestExecutionPlan();
        
        assertEquals(plan1, plan2);
        assertEquals(plan1.hashCode(), plan2.hashCode());
        
        plan2.setTestId("different-id");
        assertNotEquals(plan1, plan2);
        assertNotEquals(plan1.hashCode(), plan2.hashCode());
    }
    
    @Test
    void testToString() {
        TestExecutionPlan plan = createValidTestExecutionPlan();
        String toString = plan.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("TestExecutionPlan"));
        assertTrue(toString.contains("test-plan-1"));
        assertTrue(toString.contains("stepsCount=1"));
    }
    
    private TestExecutionPlan createValidTestExecutionPlan() {
        TestExecutionPlan plan = new TestExecutionPlan();
        plan.setTestId("test-plan-1");
        plan.setScenario("Test scenario description");
        plan.setSteps(Arrays.asList(createValidTestStep("step1")));
        plan.setConfiguration(createValidTestConfiguration());
        plan.setCreatedAt(Instant.now());
        return plan;
    }
    
    private TestStep createValidTestStep(String stepId) {
        TestStep step = new TestStep();
        step.setStepId(stepId);
        step.setType(StepType.REST_CALL);
        step.setTargetService("test-service");
        step.setTimeoutMs(30000L);
        return step;
    }
    
    private TestConfiguration createValidTestConfiguration() {
        TestConfiguration config = new TestConfiguration();
        config.setDefaultTimeoutMs(30000L);
        config.setMaxRetries(3);
        config.setParallelExecution(false);
        config.setFailFast(true);
        return config;
    }
}