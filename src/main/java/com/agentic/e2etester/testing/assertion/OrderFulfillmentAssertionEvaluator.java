package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionResult;
import com.agentic.e2etester.model.AssertionRule;
import com.agentic.e2etester.model.AssertionSeverity;
import com.agentic.e2etester.model.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Custom assertion evaluator for order fulfillment business logic.
 * Demonstrates how to create domain-specific assertion evaluators.
 */
@Component
public class OrderFulfillmentAssertionEvaluator implements CustomAssertionEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderFulfillmentAssertionEvaluator.class);
    
    @Override
    public String getEvaluatorName() {
        return "OrderFulfillmentEvaluator";
    }
    
    @Override
    public boolean canEvaluate(AssertionRule rule) {
        if (rule == null || rule.getCondition() == null) {
            return false;
        }
        
        String condition = rule.getCondition().toLowerCase();
        return condition.contains("order_fulfillment") || 
               condition.contains("inventory_check") ||
               condition.contains("payment_processing") ||
               condition.contains("shipping_validation");
    }
    
    @Override
    public AssertionResult evaluate(AssertionRule rule, TestContext context) {
        logger.debug("Evaluating order fulfillment assertion: {}", rule.getRuleId());
        
        try {
            String condition = rule.getCondition().toLowerCase();
            boolean passed = false;
            String message = "";
            Object actualValue = null;
            
            if (condition.contains("order_fulfillment")) {
                passed = evaluateOrderFulfillment(context);
                actualValue = context.getVariable("orderStatus");
                message = passed ? "Order fulfillment completed successfully" : "Order fulfillment failed";
                
            } else if (condition.contains("inventory_check")) {
                passed = evaluateInventoryCheck(context);
                actualValue = context.getVariable("inventoryStatus");
                message = passed ? "Inventory check passed" : "Inventory check failed";
                
            } else if (condition.contains("payment_processing")) {
                passed = evaluatePaymentProcessing(context);
                actualValue = context.getVariable("paymentStatus");
                message = passed ? "Payment processing completed" : "Payment processing failed";
                
            } else if (condition.contains("shipping_validation")) {
                passed = evaluateShippingValidation(context);
                actualValue = context.getVariable("shippingStatus");
                message = passed ? "Shipping validation passed" : "Shipping validation failed";
            }
            
            AssertionResult result = new AssertionResult(rule.getRuleId(), passed);
            result.setActualValue(actualValue);
            result.setExpectedValue(rule.getExpectedValue());
            result.setSeverity(rule.getSeverity() != null ? rule.getSeverity() : AssertionSeverity.ERROR);
            result.setStepId(context.getCurrentStepId());
            
            if (passed) {
                result.setMessage(message);
            } else {
                result.setErrorMessage(message);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error evaluating order fulfillment assertion {}: {}", rule.getRuleId(), e.getMessage(), e);
            
            AssertionResult result = new AssertionResult(rule.getRuleId(), false);
            result.setErrorMessage("Order fulfillment assertion evaluation failed: " + e.getMessage());
            result.setSeverity(AssertionSeverity.ERROR);
            result.setStepId(context.getCurrentStepId());
            
            return result;
        }
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for domain-specific evaluations
    }
    
    private boolean evaluateOrderFulfillment(TestContext context) {
        Object orderStatus = context.getVariable("orderStatus");
        Object inventoryStatus = context.getVariable("inventoryStatus");
        Object paymentStatus = context.getVariable("paymentStatus");
        Object shippingStatus = context.getVariable("shippingStatus");
        
        return "COMPLETED".equals(orderStatus) &&
               "AVAILABLE".equals(inventoryStatus) &&
               "PAID".equals(paymentStatus) &&
               "SHIPPED".equals(shippingStatus);
    }
    
    private boolean evaluateInventoryCheck(TestContext context) {
        Object inventoryStatus = context.getVariable("inventoryStatus");
        Object availableQuantity = context.getVariable("availableQuantity");
        Object requestedQuantity = context.getVariable("requestedQuantity");
        
        if (!"AVAILABLE".equals(inventoryStatus)) {
            return false;
        }
        
        if (availableQuantity != null && requestedQuantity != null) {
            try {
                int available = Integer.parseInt(availableQuantity.toString());
                int requested = Integer.parseInt(requestedQuantity.toString());
                return available >= requested;
            } catch (NumberFormatException e) {
                logger.warn("Invalid quantity values: available={}, requested={}", availableQuantity, requestedQuantity);
                return false;
            }
        }
        
        return true;
    }
    
    private boolean evaluatePaymentProcessing(TestContext context) {
        Object paymentStatus = context.getVariable("paymentStatus");
        Object transactionId = context.getVariable("transactionId");
        Object paymentAmount = context.getVariable("paymentAmount");
        
        return "PAID".equals(paymentStatus) &&
               transactionId != null &&
               paymentAmount != null;
    }
    
    private boolean evaluateShippingValidation(TestContext context) {
        Object shippingStatus = context.getVariable("shippingStatus");
        Object shippingAddress = context.getVariable("shippingAddress");
        Object trackingNumber = context.getVariable("trackingNumber");
        
        return ("SHIPPED".equals(shippingStatus) || "DELIVERED".equals(shippingStatus)) &&
               shippingAddress != null &&
               trackingNumber != null;
    }
}