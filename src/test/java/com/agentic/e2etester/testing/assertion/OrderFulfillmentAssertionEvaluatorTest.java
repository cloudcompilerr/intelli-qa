package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionResult;
import com.agentic.e2etester.model.AssertionRule;
import com.agentic.e2etester.model.AssertionSeverity;
import com.agentic.e2etester.model.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderFulfillmentAssertionEvaluatorTest {
    
    private OrderFulfillmentAssertionEvaluator evaluator;
    private TestContext testContext;
    
    @BeforeEach
    void setUp() {
        evaluator = new OrderFulfillmentAssertionEvaluator();
        testContext = new TestContext("test-correlation-123");
        testContext.setCurrentStepId("step-1");
    }
    
    @Test
    void testGetEvaluatorName() {
        assertEquals("OrderFulfillmentEvaluator", evaluator.getEvaluatorName());
    }
    
    @Test
    void testGetPriority() {
        assertEquals(100, evaluator.getPriority());
    }
    
    @Test
    void testCanEvaluate_OrderFulfillment_ReturnsTrue() {
        AssertionRule rule = new AssertionRule();
        rule.setCondition("order_fulfillment_complete");
        
        assertTrue(evaluator.canEvaluate(rule));
    }
    
    @Test
    void testCanEvaluate_InventoryCheck_ReturnsTrue() {
        AssertionRule rule = new AssertionRule();
        rule.setCondition("inventory_check_passed");
        
        assertTrue(evaluator.canEvaluate(rule));
    }
    
    @Test
    void testCanEvaluate_PaymentProcessing_ReturnsTrue() {
        AssertionRule rule = new AssertionRule();
        rule.setCondition("payment_processing_complete");
        
        assertTrue(evaluator.canEvaluate(rule));
    }
    
    @Test
    void testCanEvaluate_ShippingValidation_ReturnsTrue() {
        AssertionRule rule = new AssertionRule();
        rule.setCondition("shipping_validation_passed");
        
        assertTrue(evaluator.canEvaluate(rule));
    }
    
    @Test
    void testCanEvaluate_UnrelatedCondition_ReturnsFalse() {
        AssertionRule rule = new AssertionRule();
        rule.setCondition("unrelated_condition");
        
        assertFalse(evaluator.canEvaluate(rule));
    }
    
    @Test
    void testCanEvaluate_NullRule_ReturnsFalse() {
        assertFalse(evaluator.canEvaluate(null));
    }
    
    @Test
    void testCanEvaluate_NullCondition_ReturnsFalse() {
        AssertionRule rule = new AssertionRule();
        rule.setCondition(null);
        
        assertFalse(evaluator.canEvaluate(rule));
    }
    
    @Test
    void testEvaluate_OrderFulfillment_Success() {
        AssertionRule rule = new AssertionRule("order-fulfillment-test", null, "Order fulfillment test");
        rule.setCondition("order_fulfillment_complete");
        rule.setSeverity(AssertionSeverity.CRITICAL);
        
        // Set up successful order fulfillment context
        testContext.setVariable("orderStatus", "COMPLETED");
        testContext.setVariable("inventoryStatus", "AVAILABLE");
        testContext.setVariable("paymentStatus", "PAID");
        testContext.setVariable("shippingStatus", "SHIPPED");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("order-fulfillment-test", result.getRuleId());
        assertEquals("COMPLETED", result.getActualValue());
        assertEquals(AssertionSeverity.CRITICAL, result.getSeverity());
        assertEquals("step-1", result.getStepId());
        assertTrue(result.getMessage().contains("Order fulfillment completed successfully"));
    }
    
    @Test
    void testEvaluate_OrderFulfillment_Failure() {
        AssertionRule rule = new AssertionRule("order-fulfillment-test", null, "Order fulfillment test");
        rule.setCondition("order_fulfillment_complete");
        
        // Set up failed order fulfillment context (missing payment)
        testContext.setVariable("orderStatus", "PENDING");
        testContext.setVariable("inventoryStatus", "AVAILABLE");
        testContext.setVariable("paymentStatus", "PENDING");
        testContext.setVariable("shippingStatus", "NOT_SHIPPED");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("order-fulfillment-test", result.getRuleId());
        assertEquals("PENDING", result.getActualValue());
        assertTrue(result.getErrorMessage().contains("Order fulfillment failed"));
    }
    
    @Test
    void testEvaluate_InventoryCheck_Success() {
        AssertionRule rule = new AssertionRule("inventory-test", null, "Inventory check test");
        rule.setCondition("inventory_check_passed");
        
        testContext.setVariable("inventoryStatus", "AVAILABLE");
        testContext.setVariable("availableQuantity", "10");
        testContext.setVariable("requestedQuantity", "5");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("inventory-test", result.getRuleId());
        assertEquals("AVAILABLE", result.getActualValue());
        assertTrue(result.getMessage().contains("Inventory check passed"));
    }
    
    @Test
    void testEvaluate_InventoryCheck_InsufficientQuantity() {
        AssertionRule rule = new AssertionRule("inventory-test", null, "Inventory check test");
        rule.setCondition("inventory_check_passed");
        
        testContext.setVariable("inventoryStatus", "AVAILABLE");
        testContext.setVariable("availableQuantity", "3");
        testContext.setVariable("requestedQuantity", "5");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("inventory-test", result.getRuleId());
        assertTrue(result.getErrorMessage().contains("Inventory check failed"));
    }
    
    @Test
    void testEvaluate_PaymentProcessing_Success() {
        AssertionRule rule = new AssertionRule("payment-test", null, "Payment processing test");
        rule.setCondition("payment_processing_complete");
        
        testContext.setVariable("paymentStatus", "PAID");
        testContext.setVariable("transactionId", "txn-123456");
        testContext.setVariable("paymentAmount", "99.99");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("payment-test", result.getRuleId());
        assertEquals("PAID", result.getActualValue());
        assertTrue(result.getMessage().contains("Payment processing completed"));
    }
    
    @Test
    void testEvaluate_PaymentProcessing_MissingTransactionId() {
        AssertionRule rule = new AssertionRule("payment-test", null, "Payment processing test");
        rule.setCondition("payment_processing_complete");
        
        testContext.setVariable("paymentStatus", "PAID");
        // Missing transactionId
        testContext.setVariable("paymentAmount", "99.99");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("payment-test", result.getRuleId());
        assertTrue(result.getErrorMessage().contains("Payment processing failed"));
    }
    
    @Test
    void testEvaluate_ShippingValidation_Success() {
        AssertionRule rule = new AssertionRule("shipping-test", null, "Shipping validation test");
        rule.setCondition("shipping_validation_passed");
        
        testContext.setVariable("shippingStatus", "SHIPPED");
        testContext.setVariable("shippingAddress", "123 Main St, City, State 12345");
        testContext.setVariable("trackingNumber", "TRK123456789");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("shipping-test", result.getRuleId());
        assertEquals("SHIPPED", result.getActualValue());
        assertTrue(result.getMessage().contains("Shipping validation passed"));
    }
    
    @Test
    void testEvaluate_ShippingValidation_DeliveredStatus() {
        AssertionRule rule = new AssertionRule("shipping-test", null, "Shipping validation test");
        rule.setCondition("shipping_validation_passed");
        
        testContext.setVariable("shippingStatus", "DELIVERED");
        testContext.setVariable("shippingAddress", "123 Main St, City, State 12345");
        testContext.setVariable("trackingNumber", "TRK123456789");
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("shipping-test", result.getRuleId());
        assertTrue(result.getMessage().contains("Shipping validation passed"));
    }
    
    @Test
    void testEvaluate_ShippingValidation_MissingTrackingNumber() {
        AssertionRule rule = new AssertionRule("shipping-test", null, "Shipping validation test");
        rule.setCondition("shipping_validation_passed");
        
        testContext.setVariable("shippingStatus", "SHIPPED");
        testContext.setVariable("shippingAddress", "123 Main St, City, State 12345");
        // Missing trackingNumber
        
        AssertionResult result = evaluator.evaluate(rule, testContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("shipping-test", result.getRuleId());
        assertTrue(result.getErrorMessage().contains("Shipping validation failed"));
    }
    
    @Test
    void testEvaluate_ExceptionHandling() {
        AssertionRule rule = new AssertionRule("error-test", null, "Error test");
        rule.setCondition("order_fulfillment_complete");
        
        // Create a context that will cause order fulfillment to fail
        TestContext errorContext = new TestContext("test-correlation");
        errorContext.setCurrentStepId("error-step");
        // Don't set any order fulfillment variables, so the evaluation will fail
        
        AssertionResult result = evaluator.evaluate(rule, errorContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("error-test", result.getRuleId());
        assertEquals(AssertionSeverity.ERROR, result.getSeverity());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Order fulfillment failed"));
    }
}