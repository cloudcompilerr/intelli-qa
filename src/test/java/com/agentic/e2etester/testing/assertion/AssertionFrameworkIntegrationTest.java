package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AssertionFrameworkIntegrationTest {
    
    private DefaultAssertionEngine assertionEngine;
    private OrderFulfillmentAssertionEvaluator customEvaluator;
    private TestContext testContext;
    
    @BeforeEach
    void setUp() {
        assertionEngine = new DefaultAssertionEngine();
        customEvaluator = new OrderFulfillmentAssertionEvaluator();
        assertionEngine.registerCustomEvaluator(customEvaluator);
        
        testContext = createCompleteTestContext();
    }
    
    @Test
    void testCompleteOrderFulfillmentScenario() {
        // Create a comprehensive set of assertions for order fulfillment
        List<AssertionRule> assertions = Arrays.asList(
            // Business assertions
            createBusinessAssertion(),
            
            // Technical assertions
            createTechnicalAssertion(),
            
            // Data assertions
            createDataAssertion(),
            
            // Custom order fulfillment assertion
            createCustomOrderFulfillmentAssertion()
        );
        
        List<AssertionResult> results = assertionEngine.evaluateAssertions(assertions, testContext);
        
        assertNotNull(results);
        assertEquals(4, results.size());
        
        // All assertions should pass in this successful scenario
        assertTrue(results.stream().allMatch(AssertionResult::getPassed), 
                  "All assertions should pass in successful order fulfillment scenario");
        
        // Verify each assertion type
        AssertionResult businessResult = results.get(0);
        assertEquals("business-order-completion", businessResult.getRuleId());
        assertEquals(AssertionSeverity.CRITICAL, businessResult.getSeverity());
        
        AssertionResult technicalResult = results.get(1);
        assertEquals("technical-response-time", technicalResult.getRuleId());
        assertEquals(AssertionSeverity.ERROR, technicalResult.getSeverity());
        
        AssertionResult dataResult = results.get(2);
        assertEquals("data-order-persistence", dataResult.getRuleId());
        assertEquals(AssertionSeverity.ERROR, dataResult.getSeverity());
        
        AssertionResult customResult = results.get(3);
        assertEquals("custom-order-fulfillment", customResult.getRuleId());
        assertTrue(customResult.getMessage().contains("Order fulfillment completed successfully"));
    }
    
    @Test
    void testFailedOrderFulfillmentScenario() {
        // Create a failed order scenario
        TestContext failedContext = createFailedTestContext();
        
        List<AssertionRule> assertions = Arrays.asList(
            createCustomOrderFulfillmentAssertion(),
            createBusinessAssertion()
        );
        
        List<AssertionResult> results = assertionEngine.evaluateAssertions(assertions, failedContext);
        
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // Custom assertion should fail due to incomplete order
        AssertionResult customResult = results.get(0);
        assertFalse(customResult.getPassed());
        assertTrue(customResult.getErrorMessage().contains("Order fulfillment failed"));
        
        // Business assertion should also fail
        AssertionResult businessResult = results.get(1);
        assertFalse(businessResult.getPassed());
    }
    
    @Test
    void testMixedAssertionResults() {
        // Create a scenario with mixed success/failure
        TestContext mixedContext = createMixedTestContext();
        
        List<AssertionRule> assertions = Arrays.asList(
            createTechnicalAssertion(), // Should pass - good response time
            createDataAssertion(),      // Should fail - missing required field
            createCustomOrderFulfillmentAssertion() // Should fail - incomplete order
        );
        
        List<AssertionResult> results = assertionEngine.evaluateAssertions(assertions, mixedContext);
        
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // Technical assertion should pass
        assertTrue(results.get(0).getPassed());
        
        // Data assertion should fail
        assertFalse(results.get(1).getPassed());
        
        // Custom assertion should fail
        assertFalse(results.get(2).getPassed());
    }
    
    @Test
    void testBusinessAssertionValidation() {
        BusinessAssertion businessAssertion = new BusinessAssertion(
            "business-premium-order", 
            "Premium order validation", 
            "order_completed", 
            Map.of("orderType", "premium", "customerSegment", "vip")
        );
        businessAssertion.setCustomerSegment("vip");
        businessAssertion.setOrderType("premium");
        businessAssertion.setExpectedValue("COMPLETED");
        
        AssertionResult result = assertionEngine.validateBusinessOutcome(businessAssertion, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("business-premium-order", result.getRuleId());
        assertEquals(AssertionSeverity.CRITICAL, result.getSeverity());
    }
    
    @Test
    void testTechnicalAssertionValidation() {
        TechnicalAssertion technicalAssertion = new TechnicalAssertion(
            "technical-performance", 
            "Performance validation", 
            "response_time"
        );
        technicalAssertion.setServiceId("order-service");
        technicalAssertion.setResponseTimeThreshold(Duration.ofMillis(2000));
        
        AssertionResult result = assertionEngine.validateTechnicalMetrics(technicalAssertion, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("technical-performance", result.getRuleId());
    }
    
    @Test
    void testDataAssertionValidation() {
        DataAssertion dataAssertion = new DataAssertion(
            "data-validation", 
            "Order data validation", 
            "orderData"
        );
        dataAssertion.setRequiredFields(Arrays.asList("orderId", "customerId", "status", "total"));
        
        AssertionResult result = assertionEngine.validateDataConsistency(dataAssertion, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("data-validation", result.getRuleId());
    }
    
    @Test
    void testCustomEvaluatorPriority() {
        // Create a rule that both built-in and custom evaluators can handle
        AssertionRule rule = new AssertionRule("priority-test", AssertionType.CUSTOM, "Priority test");
        rule.setCondition("order_fulfillment_complete");
        
        // The custom evaluator should be used due to higher priority
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        // The result should come from the custom evaluator
        assertTrue(result.getMessage().contains("Order fulfillment completed successfully"));
    }
    
    private TestContext createCompleteTestContext() {
        TestContext context = new TestContext("integration-test-123");
        context.setCurrentStepId("integration-step-1");
        context.setTestExecutionPlanId("integration-plan-123");
        context.setStartTime(Instant.now().minusSeconds(30));
        
        // Set up successful order fulfillment variables
        context.setVariable("orderStatus", "COMPLETED");
        context.setVariable("inventoryStatus", "AVAILABLE");
        context.setVariable("paymentStatus", "PAID");
        context.setVariable("shippingStatus", "SHIPPED");
        context.setVariable("businessOutcome", "COMPLETED");
        
        // Set up order data
        Map<String, Object> orderData = Map.of(
            "orderId", "order-123",
            "customerId", "customer-456",
            "status", "COMPLETED",
            "total", 99.99
        );
        context.setVariable("orderData", orderData);
        
        // Add service interaction
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("order-service");
        interaction.setType(InteractionType.HTTP_REQUEST);
        interaction.setStatus(InteractionStatus.SUCCESS);
        interaction.setResponseTime(Duration.ofMillis(1200));
        interaction.setTimestamp(Instant.now());
        context.addInteraction(interaction);
        
        return context;
    }
    
    private TestContext createFailedTestContext() {
        TestContext context = new TestContext("failed-test-123");
        context.setCurrentStepId("failed-step-1");
        
        // Set up failed order fulfillment variables
        context.setVariable("orderStatus", "FAILED");
        context.setVariable("inventoryStatus", "OUT_OF_STOCK");
        context.setVariable("paymentStatus", "DECLINED");
        context.setVariable("shippingStatus", "NOT_SHIPPED");
        context.setVariable("businessOutcome", "FAILED");
        
        return context;
    }
    
    private TestContext createMixedTestContext() {
        TestContext context = new TestContext("mixed-test-123");
        context.setCurrentStepId("mixed-step-1");
        
        // Set up mixed scenario
        context.setVariable("orderStatus", "PENDING");
        context.setVariable("inventoryStatus", "AVAILABLE");
        context.setVariable("paymentStatus", "PENDING");
        context.setVariable("shippingStatus", "NOT_SHIPPED");
        
        // Incomplete order data (missing required field)
        Map<String, Object> orderData = Map.of(
            "orderId", "order-123",
            "customerId", "customer-456",
            "status", "PENDING"
            // Missing "total" field
        );
        context.setVariable("orderData", orderData);
        
        // Add service interaction with good response time
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("order-service");
        interaction.setResponseTime(Duration.ofMillis(800));
        context.addInteraction(interaction);
        
        return context;
    }
    
    private AssertionRule createBusinessAssertion() {
        BusinessAssertion assertion = new BusinessAssertion(
            "business-order-completion", 
            "Business order completion validation", 
            "order_completed", 
            Map.of("orderType", "standard")
        );
        assertion.setExpectedValue("COMPLETED");
        return assertion;
    }
    
    private AssertionRule createTechnicalAssertion() {
        TechnicalAssertion assertion = new TechnicalAssertion(
            "technical-response-time", 
            "Response time validation", 
            "response_time"
        );
        assertion.setServiceId("order-service");
        assertion.setResponseTimeThreshold(Duration.ofMillis(2000));
        return assertion;
    }
    
    private AssertionRule createDataAssertion() {
        DataAssertion assertion = new DataAssertion(
            "data-order-persistence", 
            "Order data persistence validation", 
            "orderData"
        );
        assertion.setRequiredFields(Arrays.asList("orderId", "customerId", "status", "total"));
        return assertion;
    }
    
    private AssertionRule createCustomOrderFulfillmentAssertion() {
        AssertionRule rule = new AssertionRule("custom-order-fulfillment", AssertionType.CUSTOM, 
                                             "Custom order fulfillment validation");
        rule.setCondition("order_fulfillment_complete");
        return rule;
    }
}