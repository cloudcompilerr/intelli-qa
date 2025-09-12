package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionSeverity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataAssertionTest {
    
    @Test
    void testDefaultConstructor() {
        DataAssertion assertion = new DataAssertion();
        
        assertNotNull(assertion);
        assertTrue(assertion.getEnabled());
        assertEquals(AssertionSeverity.ERROR, assertion.getSeverity());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        DataAssertion assertion = new DataAssertion(
            "data-rule-1", 
            "Database validation", 
            "user_database"
        );
        
        assertNotNull(assertion);
        assertEquals("data-rule-1", assertion.getRuleId());
        assertEquals("Database validation", assertion.getDescription());
        assertEquals("user_database", assertion.getDataSource());
        assertEquals(AssertionSeverity.ERROR, assertion.getSeverity());
    }
    
    @Test
    void testSettersAndGetters() {
        DataAssertion assertion = new DataAssertion();
        
        assertion.setDataSource("order_database");
        assertion.setDatabaseName("ecommerce");
        assertion.setCollectionName("orders");
        assertion.setDocumentId("order-123");
        assertion.setQueryExpression("SELECT * FROM orders WHERE id = ?");
        assertion.setJsonPath("$.order.status");
        assertion.setSchemaValidation("order-schema.json");
        assertion.setDataTransformationRule("uppercase(status)");
        
        List<String> requiredFields = Arrays.asList("id", "customerId", "status", "total");
        assertion.setRequiredFields(requiredFields);
        
        assertEquals("order_database", assertion.getDataSource());
        assertEquals("ecommerce", assertion.getDatabaseName());
        assertEquals("orders", assertion.getCollectionName());
        assertEquals("order-123", assertion.getDocumentId());
        assertEquals("SELECT * FROM orders WHERE id = ?", assertion.getQueryExpression());
        assertEquals("$.order.status", assertion.getJsonPath());
        assertEquals("order-schema.json", assertion.getSchemaValidation());
        assertEquals("uppercase(status)", assertion.getDataTransformationRule());
        assertEquals(requiredFields, assertion.getRequiredFields());
    }
    
    @Test
    void testRequiredFieldsList() {
        DataAssertion assertion = new DataAssertion();
        
        List<String> requiredFields = Arrays.asList("id", "name", "email", "createdAt");
        assertion.setRequiredFields(requiredFields);
        
        assertEquals(4, assertion.getRequiredFields().size());
        assertTrue(assertion.getRequiredFields().contains("id"));
        assertTrue(assertion.getRequiredFields().contains("name"));
        assertTrue(assertion.getRequiredFields().contains("email"));
        assertTrue(assertion.getRequiredFields().contains("createdAt"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        DataAssertion assertion1 = new DataAssertion("rule-1", "Test", "database1");
        assertion1.setDatabaseName("test_db");
        assertion1.setCollectionName("users");
        assertion1.setDocumentId("user-123");
        
        DataAssertion assertion2 = new DataAssertion("rule-1", "Test", "database1");
        assertion2.setDatabaseName("test_db");
        assertion2.setCollectionName("users");
        assertion2.setDocumentId("user-123");
        
        DataAssertion assertion3 = new DataAssertion("rule-1", "Test", "database2");
        assertion3.setDatabaseName("different_db");
        assertion3.setCollectionName("orders");
        assertion3.setDocumentId("order-456");
        
        // Same data source, database name, collection name, and document ID
        assertEquals(assertion1, assertion2);
        assertEquals(assertion1.hashCode(), assertion2.hashCode());
        
        // Different data source, database name, collection name, or document ID
        assertNotEquals(assertion1, assertion3);
        assertNotEquals(assertion1.hashCode(), assertion3.hashCode());
    }
    
    @Test
    void testToString() {
        DataAssertion assertion = new DataAssertion("data-rule-1", "Test", "user_database");
        assertion.setDatabaseName("ecommerce");
        assertion.setCollectionName("users");
        assertion.setDocumentId("user-123");
        assertion.setQueryExpression("SELECT * FROM users WHERE id = ?");
        
        String toString = assertion.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("data-rule-1"));
        assertTrue(toString.contains("user_database"));
        assertTrue(toString.contains("ecommerce"));
        assertTrue(toString.contains("users"));
        assertTrue(toString.contains("user-123"));
        assertTrue(toString.contains("SELECT * FROM users WHERE id = ?"));
    }
    
    @Test
    void testInheritanceFromAssertionRule() {
        DataAssertion assertion = new DataAssertion();
        
        // Test inherited methods
        assertion.setRuleId("test-rule");
        assertion.setDescription("Test description");
        assertion.setExpectedValue("ACTIVE");
        assertion.setActualValuePath("$.user.status");
        
        assertEquals("test-rule", assertion.getRuleId());
        assertEquals("Test description", assertion.getDescription());
        assertEquals("ACTIVE", assertion.getExpectedValue());
        assertEquals("$.user.status", assertion.getActualValuePath());
    }
    
    @Test
    void testJsonPathAndSchemaValidation() {
        DataAssertion assertion = new DataAssertion();
        
        assertion.setJsonPath("$.order.items[*].price");
        assertion.setSchemaValidation("order-schema-v2.json");
        
        assertEquals("$.order.items[*].price", assertion.getJsonPath());
        assertEquals("order-schema-v2.json", assertion.getSchemaValidation());
    }
    
    @Test
    void testDataTransformationRule() {
        DataAssertion assertion = new DataAssertion();
        
        assertion.setDataTransformationRule("trim(name) AND lowercase(email)");
        
        assertEquals("trim(name) AND lowercase(email)", assertion.getDataTransformationRule());
    }
}