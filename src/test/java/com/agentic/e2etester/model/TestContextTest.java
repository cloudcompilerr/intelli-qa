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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestContextTest {
    
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
    void testValidTestContext() {
        TestContext context = createValidTestContext();
        
        Set<ConstraintViolation<TestContext>> violations = validator.validate(context);
        assertTrue(violations.isEmpty(), "Valid test context should not have violations");
    }
    
    @Test
    void testInvalidTestContext_BlankCorrelationId() {
        TestContext context = createValidTestContext();
        context.setCorrelationId("");
        
        Set<ConstraintViolation<TestContext>> violations = validator.validate(context);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Correlation ID cannot be blank")));
    }
    
    @Test
    void testDefaultConstructor() {
        TestContext context = new TestContext();
        
        assertNotNull(context.getExecutionState());
        assertNotNull(context.getInteractions());
        assertNotNull(context.getEvents());
        assertNotNull(context.getVariables());
        assertNotNull(context.getStartTime());
        assertTrue(context.getExecutionState().isEmpty());
        assertTrue(context.getInteractions().isEmpty());
        assertTrue(context.getEvents().isEmpty());
        assertTrue(context.getVariables().isEmpty());
    }
    
    @Test
    void testConstructorWithCorrelationId() {
        String correlationId = "test-correlation-123";
        TestContext context = new TestContext(correlationId);
        
        assertEquals(correlationId, context.getCorrelationId());
        assertNotNull(context.getExecutionState());
        assertNotNull(context.getInteractions());
        assertNotNull(context.getEvents());
        assertNotNull(context.getVariables());
    }
    
    @Test
    void testAddInteraction() {
        TestContext context = createValidTestContext();
        ServiceInteraction interaction = createValidServiceInteraction();
        
        context.addInteraction(interaction);
        
        assertEquals(1, context.getInteractions().size());
        assertEquals(interaction, context.getInteractions().get(0));
    }
    
    @Test
    void testAddEvent() {
        TestContext context = createValidTestContext();
        TestEvent event = createValidTestEvent();
        
        context.addEvent(event);
        
        assertEquals(1, context.getEvents().size());
        assertEquals(event, context.getEvents().get(0));
    }
    
    @Test
    void testVariableManagement() {
        TestContext context = createValidTestContext();
        
        context.setVariable("key1", "value1");
        context.setVariable("key2", 123);
        context.setVariable("key3", true);
        
        assertEquals("value1", context.getVariable("key1"));
        assertEquals(123, context.getVariable("key2"));
        assertEquals(true, context.getVariable("key3"));
        assertNull(context.getVariable("nonexistent"));
    }
    
    @Test
    void testExecutionStateManagement() {
        TestContext context = createValidTestContext();
        
        context.updateExecutionState("status", "running");
        context.updateExecutionState("progress", 0.5);
        
        assertEquals("running", context.getExecutionState().get("status"));
        assertEquals(0.5, context.getExecutionState().get("progress"));
    }
    
    @Test
    void testSerialization() throws Exception {
        TestContext context = createValidTestContext();
        context.setVariable("testVar", "testValue");
        context.updateExecutionState("status", "running");
        
        String json = objectMapper.writeValueAsString(context);
        assertNotNull(json);
        assertTrue(json.contains("test-correlation-123"));
        
        TestContext deserialized = objectMapper.readValue(json, TestContext.class);
        assertEquals(context.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(context.getTestExecutionPlanId(), deserialized.getTestExecutionPlanId());
        assertEquals(context.getCurrentStepId(), deserialized.getCurrentStepId());
    }
    
    @Test
    void testEqualsAndHashCode() {
        TestContext context1 = createValidTestContext();
        TestContext context2 = createValidTestContext();
        
        assertEquals(context1, context2);
        assertEquals(context1.hashCode(), context2.hashCode());
        
        context2.setCorrelationId("different-correlation");
        assertNotEquals(context1, context2);
        assertNotEquals(context1.hashCode(), context2.hashCode());
    }
    
    @Test
    void testToString() {
        TestContext context = createValidTestContext();
        String toString = context.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("TestContext"));
        assertTrue(toString.contains("test-correlation-123"));
        assertTrue(toString.contains("test-plan-123"));
        assertTrue(toString.contains("step-1"));
    }
    
    @Test
    void testConcurrentModification() {
        TestContext context = createValidTestContext();
        
        // Test that concurrent collections are used
        assertDoesNotThrow(() -> {
            context.addInteraction(createValidServiceInteraction());
            context.addEvent(createValidTestEvent());
            context.setVariable("key", "value");
            context.updateExecutionState("status", "running");
        });
    }
    
    private TestContext createValidTestContext() {
        TestContext context = new TestContext("test-correlation-123");
        context.setTestExecutionPlanId("test-plan-123");
        context.setCurrentStepId("step-1");
        context.setStartTime(Instant.now());
        
        TestMetrics metrics = new TestMetrics();
        metrics.setTotalRequests(10);
        metrics.setSuccessfulRequests(8);
        metrics.setFailedRequests(2);
        context.setMetrics(metrics);
        
        return context;
    }
    
    private ServiceInteraction createValidServiceInteraction() {
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("test-service");
        interaction.setType(InteractionType.HTTP_REQUEST);
        interaction.setStatus(InteractionStatus.SUCCESS);
        interaction.setTimestamp(Instant.now());
        interaction.setResponseTimeMs(100L);
        return interaction;
    }
    
    private TestEvent createValidTestEvent() {
        TestEvent event = new TestEvent();
        event.setEventId("event-1");
        event.setType(EventType.STEP_STARTED);
        event.setMessage("Step started");
        event.setTimestamp(Instant.now());
        event.setSeverity(EventSeverity.INFO);
        return event;
    }
}