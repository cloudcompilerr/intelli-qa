package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentValidator.
 */
class DocumentValidatorTest {
    
    private DocumentValidator documentValidator;
    
    @BeforeEach
    void setUp() {
        documentValidator = new DocumentValidator();
    }
    
    @Test
    void testValidateDocument_Success() {
        // Given
        JsonObject document = JsonObject.create()
            .put("orderId", "ORDER-123")
            .put("customerId", "CUST-456")
            .put("amount", 99.99)
            .put("status", "PENDING");
        
        Map<String, Object> expectedValues = Map.of(
            "orderId", "ORDER-123",
            "customerId", "CUST-456",
            "amount", 99.99,
            "status", "PENDING"
        );
        
        // When
        DocumentValidationResult result = documentValidator.validate(document, expectedValues);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testValidateDocument_FieldMismatch() {
        // Given
        JsonObject document = JsonObject.create()
            .put("orderId", "ORDER-123")
            .put("amount", 99.99);
        
        Map<String, Object> expectedValues = Map.of(
            "orderId", "ORDER-456", // Wrong value
            "customerId", "CUST-789", // Missing field
            "amount", 99.99
        );
        
        // When
        DocumentValidationResult result = documentValidator.validate(document, expectedValues);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrorCount());
        
        List<ValidationError> errors = result.getErrors();
        assertTrue(errors.stream().anyMatch(e -> e.getFieldPath().equals("orderId")));
        assertTrue(errors.stream().anyMatch(e -> e.getFieldPath().equals("customerId")));
    }
    
    @Test
    void testValidateDocument_NestedFields() {
        // Given
        JsonObject nestedObject = JsonObject.create()
            .put("street", "123 Main St")
            .put("city", "Springfield");
        
        JsonObject document = JsonObject.create()
            .put("name", "John Doe")
            .put("address", nestedObject);
        
        Map<String, Object> expectedValues = Map.of(
            "name", "John Doe",
            "address.street", "123 Main St",
            "address.city", "Springfield"
        );
        
        // When
        DocumentValidationResult result = documentValidator.validate(document, expectedValues);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testValidateDocument_NumericComparison() {
        // Given
        JsonObject document = JsonObject.create()
            .put("intValue", 42)
            .put("doubleValue", 3.14)
            .put("longValue", 1000L);
        
        Map<String, Object> expectedValues = Map.of(
            "intValue", 42.0, // Different numeric type but same value
            "doubleValue", 3.14f, // Different precision
            "longValue", 1000 // Different numeric type
        );
        
        // When
        DocumentValidationResult result = documentValidator.validate(document, expectedValues);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testCompareDocuments_Identical() {
        // Given
        JsonObject document1 = JsonObject.create()
            .put("name", "Test Document")
            .put("value", 42)
            .put("active", true);
        
        JsonObject document2 = JsonObject.create()
            .put("name", "Test Document")
            .put("value", 42)
            .put("active", true);
        
        // When
        DocumentComparisonResult result = documentValidator.compare(document1, document2);
        
        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.isIdentical());
        assertEquals(0, result.getDifferenceCount());
    }
    
    @Test
    void testCompareDocuments_Different() {
        // Given
        JsonObject document1 = JsonObject.create()
            .put("name", "Document 1")
            .put("value", 100)
            .put("active", true);
        
        JsonObject document2 = JsonObject.create()
            .put("name", "Document 2")
            .put("value", 200)
            .put("active", true);
        
        // When
        DocumentComparisonResult result = documentValidator.compare(document1, document2);
        
        // Then
        assertTrue(result.isSuccess());
        assertFalse(result.isIdentical());
        assertEquals(2, result.getDifferenceCount());
        
        List<FieldDifference> differences = result.getDifferences();
        assertTrue(differences.stream().anyMatch(d -> 
            d.getFieldName().equals("name") && 
            "Document 1".equals(d.getValue1()) && 
            "Document 2".equals(d.getValue2())));
        assertTrue(differences.stream().anyMatch(d -> 
            d.getFieldName().equals("value") && 
            Integer.valueOf(100).equals(d.getValue1()) && 
            Integer.valueOf(200).equals(d.getValue2())));
    }
    
    @Test
    void testCompareDocuments_MissingFields() {
        // Given
        JsonObject document1 = JsonObject.create()
            .put("name", "Document 1")
            .put("value", 100)
            .put("extra", "field");
        
        JsonObject document2 = JsonObject.create()
            .put("name", "Document 1")
            .put("value", 100);
        
        // When
        DocumentComparisonResult result = documentValidator.compare(document1, document2);
        
        // Then
        assertTrue(result.isSuccess());
        assertFalse(result.isIdentical());
        assertEquals(1, result.getDifferenceCount());
        
        FieldDifference difference = result.getDifferences().get(0);
        assertEquals("extra", difference.getFieldName());
        assertEquals("field", difference.getValue1());
        assertNull(difference.getValue2());
    }
    
    @Test
    void testValidateSchema_Success() {
        // Given
        JsonObject document = JsonObject.create()
            .put("id", "DOC-123")
            .put("name", "Test Document")
            .put("value", 42)
            .put("active", true);
        
        DocumentSchema schema = DocumentSchema.builder()
            .requiredFields(List.of("id", "name", "value"))
            .fieldTypes(Map.of(
                "id", String.class,
                "name", String.class,
                "value", Integer.class,
                "active", Boolean.class
            ))
            .build();
        
        // When
        DocumentValidationResult result = documentValidator.validateSchema(document, schema);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testValidateSchema_MissingRequiredField() {
        // Given
        JsonObject document = JsonObject.create()
            .put("name", "Test Document")
            .put("value", 42);
        
        DocumentSchema schema = DocumentSchema.builder()
            .requiredFields(List.of("id", "name", "value"))
            .build();
        
        // When
        DocumentValidationResult result = documentValidator.validateSchema(document, schema);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorCount());
        
        ValidationError error = result.getErrors().get(0);
        assertEquals("id", error.getFieldPath());
        assertTrue(error.getMessage().contains("Required field is missing"));
    }
    
    @Test
    void testValidateFieldsExist_Success() {
        // Given
        JsonObject document = JsonObject.create()
            .put("orderId", "ORDER-123")
            .put("customerId", "CUST-456")
            .put("amount", 99.99);
        
        List<String> requiredFields = List.of("orderId", "customerId", "amount");
        
        // When
        DocumentValidationResult result = documentValidator.validateFieldsExist(document, requiredFields);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testValidateFieldsExist_MissingFields() {
        // Given
        JsonObject document = JsonObject.create()
            .put("orderId", "ORDER-123")
            .put("amount", 99.99);
        
        List<String> requiredFields = List.of("orderId", "customerId", "amount", "status");
        
        // When
        DocumentValidationResult result = documentValidator.validateFieldsExist(document, requiredFields);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrorCount());
        
        List<ValidationError> errors = result.getErrors();
        assertTrue(errors.stream().anyMatch(e -> e.getFieldPath().equals("customerId")));
        assertTrue(errors.stream().anyMatch(e -> e.getFieldPath().equals("status")));
    }
}