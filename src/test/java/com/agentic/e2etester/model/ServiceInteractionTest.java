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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ServiceInteractionTest {
    
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
    void testValidServiceInteraction() {
        ServiceInteraction interaction = createValidServiceInteraction();
        
        Set<ConstraintViolation<ServiceInteraction>> violations = validator.validate(interaction);
        assertTrue(violations.isEmpty(), "Valid service interaction should not have violations");
    }
    
    @Test
    void testInvalidServiceInteraction_BlankServiceId() {
        ServiceInteraction interaction = createValidServiceInteraction();
        interaction.setServiceId("");
        
        Set<ConstraintViolation<ServiceInteraction>> violations = validator.validate(interaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Service ID cannot be blank")));
    }
    
    @Test
    void testInvalidServiceInteraction_NullInteractionType() {
        ServiceInteraction interaction = createValidServiceInteraction();
        interaction.setType(null);
        
        Set<ConstraintViolation<ServiceInteraction>> violations = validator.validate(interaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Interaction type cannot be null")));
    }
    
    @Test
    void testInvalidServiceInteraction_NullTimestamp() {
        ServiceInteraction interaction = createValidServiceInteraction();
        interaction.setTimestamp(null);
        
        Set<ConstraintViolation<ServiceInteraction>> violations = validator.validate(interaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Timestamp cannot be null")));
    }
    
    @Test
    void testInvalidServiceInteraction_NullStatus() {
        ServiceInteraction interaction = createValidServiceInteraction();
        interaction.setStatus(null);
        
        Set<ConstraintViolation<ServiceInteraction>> violations = validator.validate(interaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Interaction status cannot be null")));
    }
    
    @Test
    void testInvalidServiceInteraction_NegativeResponseTime() {
        ServiceInteraction interaction = createValidServiceInteraction();
        interaction.setResponseTimeMs(-100L);
        
        Set<ConstraintViolation<ServiceInteraction>> violations = validator.validate(interaction);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Response time must be positive or zero")));
    }
    
    @Test
    void testDefaultConstructor() {
        ServiceInteraction interaction = new ServiceInteraction();
        
        assertNotNull(interaction.getTimestamp());
        // Timestamp should be set to current time (within a reasonable range)
        assertTrue(interaction.getTimestamp().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(interaction.getTimestamp().isAfter(Instant.now().minusSeconds(1)));
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        String serviceId = "test-service";
        InteractionType type = InteractionType.HTTP_REQUEST;
        InteractionStatus status = InteractionStatus.SUCCESS;
        
        ServiceInteraction interaction = new ServiceInteraction(serviceId, type, status);
        
        assertEquals(serviceId, interaction.getServiceId());
        assertEquals(type, interaction.getType());
        assertEquals(status, interaction.getStatus());
        assertNotNull(interaction.getTimestamp());
    }
    
    @Test
    void testResponseTimeDurationConversion() {
        ServiceInteraction interaction = createValidServiceInteraction();
        interaction.setResponseTimeMs(1500L);
        
        Duration responseTime = interaction.getResponseTime();
        assertNotNull(responseTime);
        assertEquals(1500, responseTime.toMillis());
        
        interaction.setResponseTime(Duration.ofSeconds(2));
        assertEquals(2000L, interaction.getResponseTimeMs());
    }
    
    @Test
    void testSerialization() throws Exception {
        ServiceInteraction interaction = createValidServiceInteraction();
        
        String json = objectMapper.writeValueAsString(interaction);
        assertNotNull(json);
        assertTrue(json.contains("test-service"));
        assertTrue(json.contains("http_request"));
        assertTrue(json.contains("success"));
        
        ServiceInteraction deserialized = objectMapper.readValue(json, ServiceInteraction.class);
        assertEquals(interaction.getServiceId(), deserialized.getServiceId());
        assertEquals(interaction.getType(), deserialized.getType());
        assertEquals(interaction.getStatus(), deserialized.getStatus());
        assertEquals(interaction.getResponseTimeMs(), deserialized.getResponseTimeMs());
    }
    
    @Test
    void testEqualsAndHashCode() {
        Instant fixedTime = Instant.parse("2023-01-01T00:00:00Z");
        ServiceInteraction interaction1 = createValidServiceInteraction();
        interaction1.setTimestamp(fixedTime);
        ServiceInteraction interaction2 = createValidServiceInteraction();
        interaction2.setTimestamp(fixedTime);
        
        assertEquals(interaction1, interaction2);
        assertEquals(interaction1.hashCode(), interaction2.hashCode());
        
        interaction2.setServiceId("different-service");
        assertNotEquals(interaction1, interaction2);
        assertNotEquals(interaction1.hashCode(), interaction2.hashCode());
    }
    
    @Test
    void testToString() {
        ServiceInteraction interaction = createValidServiceInteraction();
        String toString = interaction.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("ServiceInteraction"));
        assertTrue(toString.contains("test-service"));
        // Check for the actual enum value representation
        assertTrue(toString.contains("HTTP_REQUEST") || toString.contains("http_request"));
        assertTrue(toString.contains("SUCCESS") || toString.contains("success"));
    }
    
    @Test
    void testInteractionTypeEnumSerialization() throws Exception {
        // Test all interaction types
        for (InteractionType type : InteractionType.values()) {
            ServiceInteraction interaction = createValidServiceInteraction();
            interaction.setType(type);
            
            String json = objectMapper.writeValueAsString(interaction);
            ServiceInteraction deserialized = objectMapper.readValue(json, ServiceInteraction.class);
            
            assertEquals(type, deserialized.getType());
        }
    }
    
    @Test
    void testInteractionStatusEnumSerialization() throws Exception {
        // Test all interaction statuses
        for (InteractionStatus status : InteractionStatus.values()) {
            ServiceInteraction interaction = createValidServiceInteraction();
            interaction.setStatus(status);
            
            String json = objectMapper.writeValueAsString(interaction);
            ServiceInteraction deserialized = objectMapper.readValue(json, ServiceInteraction.class);
            
            assertEquals(status, deserialized.getStatus());
        }
    }
    
    @Test
    void testRequestResponseHandling() {
        ServiceInteraction interaction = createValidServiceInteraction();
        
        // Test with different types of request/response objects
        interaction.setRequest("{\"key\": \"value\"}");
        interaction.setResponse("{\"result\": \"success\"}");
        
        assertEquals("{\"key\": \"value\"}", interaction.getRequest());
        assertEquals("{\"result\": \"success\"}", interaction.getResponse());
        
        // Test with complex objects
        interaction.setRequest(new TestRequestObject("test", 123));
        interaction.setResponse(new TestResponseObject("success", 200));
        
        assertNotNull(interaction.getRequest());
        assertNotNull(interaction.getResponse());
    }
    
    private ServiceInteraction createValidServiceInteraction() {
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("test-service");
        interaction.setType(InteractionType.HTTP_REQUEST);
        interaction.setTimestamp(Instant.now());
        interaction.setStatus(InteractionStatus.SUCCESS);
        interaction.setResponseTimeMs(100L);
        interaction.setCorrelationId("test-correlation-123");
        interaction.setStepId("step-1");
        interaction.setRequest("{\"test\": \"request\"}");
        interaction.setResponse("{\"test\": \"response\"}");
        return interaction;
    }
    
    // Helper classes for testing complex objects
    private static class TestRequestObject {
        public String name;
        public int value;
        
        public TestRequestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
    
    private static class TestResponseObject {
        public String status;
        public int code;
        
        public TestResponseObject(String status, int code) {
            this.status = status;
            this.code = code;
        }
    }
}