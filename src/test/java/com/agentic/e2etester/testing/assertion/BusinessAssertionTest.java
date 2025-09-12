package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionSeverity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BusinessAssertionTest {
    
    @Test
    void testDefaultConstructor() {
        BusinessAssertion assertion = new BusinessAssertion();
        
        assertNotNull(assertion);
        assertTrue(assertion.getEnabled());
        assertEquals(AssertionSeverity.CRITICAL, assertion.getSeverity());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        Map<String, Object> businessContext = Map.of("orderType", "premium", "customerSegment", "vip");
        
        BusinessAssertion assertion = new BusinessAssertion(
            "business-rule-1", 
            "Order completion validation", 
            "order_completed", 
            businessContext
        );
        
        assertNotNull(assertion);
        assertEquals("business-rule-1", assertion.getRuleId());
        assertEquals("Order completion validation", assertion.getDescription());
        assertEquals("order_completed", assertion.getBusinessRule());
        assertEquals(businessContext, assertion.getBusinessContext());
        assertEquals(AssertionSeverity.CRITICAL, assertion.getSeverity());
    }
    
    @Test
    void testSettersAndGetters() {
        BusinessAssertion assertion = new BusinessAssertion();
        
        assertion.setBusinessRule("payment_processed");
        assertion.setCustomerSegment("premium");
        assertion.setOrderType("express");
        assertion.setFulfillmentPath("same_day_delivery");
        
        Map<String, Object> context = Map.of("region", "US", "priority", "high");
        assertion.setBusinessContext(context);
        
        assertEquals("payment_processed", assertion.getBusinessRule());
        assertEquals("premium", assertion.getCustomerSegment());
        assertEquals("express", assertion.getOrderType());
        assertEquals("same_day_delivery", assertion.getFulfillmentPath());
        assertEquals(context, assertion.getBusinessContext());
    }
    
    @Test
    void testEqualsAndHashCode() {
        Map<String, Object> context1 = Map.of("orderType", "standard");
        Map<String, Object> context2 = Map.of("orderType", "premium");
        
        BusinessAssertion assertion1 = new BusinessAssertion("rule-1", "Test", "order_completed", context1);
        assertion1.setCustomerSegment("regular");
        assertion1.setOrderType("standard");
        
        BusinessAssertion assertion2 = new BusinessAssertion("rule-1", "Test", "order_completed", context2);
        assertion2.setCustomerSegment("regular");
        assertion2.setOrderType("standard");
        
        BusinessAssertion assertion3 = new BusinessAssertion("rule-2", "Test", "order_completed", context1);
        assertion3.setCustomerSegment("premium");
        assertion3.setOrderType("express");
        
        // Same rule ID, business rule, customer segment, and order type
        assertEquals(assertion1, assertion2);
        assertEquals(assertion1.hashCode(), assertion2.hashCode());
        
        // Different rule ID or other fields
        assertNotEquals(assertion1, assertion3);
        assertNotEquals(assertion1.hashCode(), assertion3.hashCode());
    }
    
    @Test
    void testToString() {
        Map<String, Object> context = Map.of("orderType", "premium");
        
        BusinessAssertion assertion = new BusinessAssertion("business-rule-1", "Test", "order_completed", context);
        assertion.setCustomerSegment("vip");
        assertion.setOrderType("express");
        assertion.setFulfillmentPath("same_day");
        
        String toString = assertion.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("business-rule-1"));
        assertTrue(toString.contains("order_completed"));
        assertTrue(toString.contains("vip"));
        assertTrue(toString.contains("express"));
        assertTrue(toString.contains("same_day"));
    }
    
    @Test
    void testInheritanceFromAssertionRule() {
        BusinessAssertion assertion = new BusinessAssertion();
        
        // Test inherited methods
        assertion.setRuleId("test-rule");
        assertion.setDescription("Test description");
        assertion.setExpectedValue("COMPLETED");
        assertion.setActualValuePath("$.order.status");
        
        assertEquals("test-rule", assertion.getRuleId());
        assertEquals("Test description", assertion.getDescription());
        assertEquals("COMPLETED", assertion.getExpectedValue());
        assertEquals("$.order.status", assertion.getActualValuePath());
    }
}