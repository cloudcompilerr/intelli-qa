package com.agentic.e2etester.system;

import com.agentic.e2etester.controller.AgenticTestController;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import com.agentic.e2etester.testing.assertion.AssertionEngine;
import com.agentic.e2etester.service.ServiceDiscoveryManager;
import com.agentic.e2etester.monitoring.TestObservabilityManager;
import com.agentic.e2etester.service.TestMemoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests that validate complete order fulfillment scenarios end-to-end.
 * These tests verify business-critical paths and customer-facing functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("system-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderFulfillmentSystemTest {

    @Autowired
    private AgenticTestController agenticTestController;
    
    @Autowired
    private TestExecutionEngine testExecutionEngine;
    
    @Autowired
    private AssertionEngine assertionEngine;
    
    @Autowired
    private ServiceDiscoveryManager serviceDiscoveryManager;
    
    @Autowired
    private TestObservabilityManager observabilityManager;
    
    @Autowired
    private TestMemoryService testMemoryService;

    private SystemTestContext systemContext;

    @BeforeEach
    void setUp() {
        systemContext = createSystemTestContext();
        // Ensure all services are healthy before testing
        verifySystemHealth();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void testStandardOrderFulfillmentFlow() throws Exception {
        // Test the complete standard order fulfillment journey
        String scenario = """
            Standard Order Fulfillment System Test:
            
            Business Context: Customer places a standard order for in-stock items
            
            Expected Flow:
            1. Customer browses catalog and selects products
            2. Customer adds items to cart and proceeds to checkout
            3. Customer provides shipping and payment information
            4. System validates inventory availability
            5. Payment service processes payment authorization
            6. Order service creates order record with unique order ID
            7. Inventory service reserves items and updates stock levels
            8. Fulfillment service creates picking list and assigns warehouse
            9. Shipping service calculates rates and creates shipping label
            10. Notification service sends order confirmation to customer
            11. Warehouse management system processes picking
            12. Shipping service updates tracking information
            13. Notification service sends shipping confirmation
            14. Customer receives order and system marks as delivered
            15. Post-delivery follow-up and feedback collection
            
            Success Criteria:
            - Order processed within 30 seconds
            - All services respond successfully
            - Data consistency maintained across all systems
            - Customer receives all expected notifications
            - Inventory levels accurately updated
            - Payment processed correctly
            - Shipping information generated
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        assertNotNull(plan);
        assertEquals(15, plan.getSteps().size());

        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(2, TimeUnit.MINUTES);

        // System test assertions
        assertEquals(TestStatus.PASSED, testResult.getStatus(), 
            "Standard order fulfillment should complete successfully");
        
        // Verify all critical services were involved
        Set<String> expectedServices = Set.of(
            "catalog-service", "cart-service", "checkout-service", "inventory-service",
            "payment-service", "order-service", "fulfillment-service", "shipping-service",
            "notification-service", "warehouse-service", "tracking-service"
        );
        
        Set<String> actualServices = testResult.getServiceInteractions().stream()
            .map(ServiceInteraction::getServiceId)
            .collect(java.util.stream.Collectors.toSet());
            
        assertTrue(actualServices.containsAll(expectedServices), 
            "All expected services should be involved in order fulfillment");

        // Verify business assertions
        assertTrue(testResult.getAssertionResults().stream()
            .filter(assertion -> assertion.getCategory() == AssertionCategory.BUSINESS)
            .allMatch(AssertionResult::isPassed), 
            "All business assertions should pass");

        // Verify performance requirements
        assertTrue(testResult.getExecutionTime().toSeconds() < 30, 
            "Order processing should complete within 30 seconds");

        // Store successful pattern for future reference
        testMemoryService.storeSuccessfulPattern(createTestPattern(testResult));
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void testPremiumCustomerOrderFlow() throws Exception {
        // Test premium customer order with special handling
        String scenario = """
            Premium Customer Order System Test:
            
            Business Context: Premium customer places high-value order with expedited processing
            
            Expected Flow:
            1. Premium customer authentication and profile validation
            2. Premium catalog access with exclusive items
            3. Premium pricing and discount application
            4. Priority inventory allocation
            5. Premium payment processing with higher limits
            6. Expedited order processing workflow
            7. Priority fulfillment with premium packaging
            8. Expedited shipping options and premium carriers
            9. Premium customer notifications with personalization
            10. White-glove delivery coordination
            11. Premium customer service follow-up
            12. Loyalty points calculation and application
            
            Success Criteria:
            - Premium customer benefits applied correctly
            - Expedited processing times (under 15 seconds)
            - Premium notifications sent
            - Loyalty points calculated accurately
            - Premium shipping options available
            - White-glove delivery scheduled
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(90, TimeUnit.SECONDS);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify premium customer specific assertions
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("premium discount")),
            "Premium discount should be applied");
            
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("loyalty points")),
            "Loyalty points should be calculated");

        // Verify expedited processing
        assertTrue(testResult.getExecutionTime().toSeconds() < 15, 
            "Premium order processing should be expedited");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void testBackorderScenarioHandling() throws Exception {
        // Test order fulfillment when items are backordered
        String scenario = """
            Backorder Scenario System Test:
            
            Business Context: Customer orders items with mixed availability (some in stock, some backordered)
            
            Expected Flow:
            1. Customer places order with mixed inventory availability
            2. Inventory service identifies available and backordered items
            3. System offers partial fulfillment options to customer
            4. Customer chooses to proceed with partial shipment
            5. Available items processed through normal fulfillment
            6. Backordered items added to backorder queue
            7. Customer notified of partial shipment and backorder status
            8. Available items shipped with partial order notification
            9. Backorder tracking and estimated availability updates
            10. Backordered items fulfilled when inventory replenished
            11. Second shipment processed and customer notified
            12. Order marked complete when all items delivered
            
            Success Criteria:
            - Partial fulfillment handled correctly
            - Backorder queue managed properly
            - Customer notifications accurate and timely
            - Inventory tracking maintained
            - Order completion tracked across multiple shipments
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(3, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify backorder handling
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("backorder")),
            "Backorder handling should be verified");
            
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("partial shipment")),
            "Partial shipment should be processed");
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void testInternationalOrderFulfillment() throws Exception {
        // Test international order with customs and currency handling
        String scenario = """
            International Order System Test:
            
            Business Context: International customer places order requiring customs processing
            
            Expected Flow:
            1. International customer places order with foreign address
            2. Currency conversion and international pricing applied
            3. International shipping options and costs calculated
            4. Customs documentation requirements determined
            5. Export compliance and restrictions validated
            6. International payment processing with currency conversion
            7. Order processed with international fulfillment workflow
            8. Customs forms and documentation generated
            9. International carrier selection and booking
            10. Export documentation and customs clearance
            11. International tracking and delivery coordination
            12. Customs duties and taxes handling
            13. International delivery confirmation
            
            Success Criteria:
            - Currency conversion accurate
            - International shipping calculated correctly
            - Customs documentation complete
            - Export compliance verified
            - International tracking functional
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(4, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify international order handling
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("currency conversion")),
            "Currency conversion should be handled");
            
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("customs")),
            "Customs processing should be verified");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void testOrderCancellationAndRefundFlow() throws Exception {
        // Test order cancellation at various stages
        String scenario = """
            Order Cancellation System Test:
            
            Business Context: Customer cancels order at different stages of fulfillment
            
            Test Scenarios:
            A. Cancellation before fulfillment starts
            B. Cancellation during picking process
            C. Cancellation after shipping but before delivery
            
            Expected Flow for Each Scenario:
            1. Customer initiates cancellation request
            2. System determines cancellation feasibility based on order status
            3. Cancellation workflow triggered with appropriate handling
            4. Inventory adjustments made if items not yet shipped
            5. Payment refund processing initiated
            6. Shipping cancellation if order not yet delivered
            7. Customer notification of cancellation confirmation
            8. Refund processing and customer notification
            9. Order status updated to cancelled
            10. Analytics and reporting updated
            
            Success Criteria:
            - Cancellation handled appropriately for each stage
            - Inventory correctly adjusted
            - Refunds processed accurately
            - Customer notifications sent
            - Order tracking updated
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(3, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify cancellation handling
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("cancellation")),
            "Order cancellation should be handled");
            
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("refund")),
            "Refund processing should be verified");
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void testReturnAndExchangeProcess() throws Exception {
        // Test post-delivery return and exchange scenarios
        String scenario = """
            Return and Exchange System Test:
            
            Business Context: Customer initiates return/exchange after receiving order
            
            Expected Flow:
            1. Customer initiates return request through customer portal
            2. Return eligibility validation (time limits, condition, etc.)
            3. Return authorization (RMA) generated
            4. Return shipping label created and sent to customer
            5. Customer ships item back using provided label
            6. Warehouse receives returned item
            7. Quality inspection and condition assessment
            8. Return processing decision (refund, exchange, reject)
            9. Inventory adjustment for returned items
            10. Refund processing or exchange item shipment
            11. Customer notification of return completion
            12. Return analytics and reporting
            
            Success Criteria:
            - Return eligibility correctly validated
            - RMA process functional
            - Quality inspection workflow operational
            - Refund/exchange processing accurate
            - Inventory adjustments correct
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(4, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify return/exchange handling
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("return")),
            "Return processing should be verified");
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    void testBulkOrderProcessing() throws Exception {
        // Test bulk/wholesale order processing
        String scenario = """
            Bulk Order System Test:
            
            Business Context: B2B customer places large bulk order with special requirements
            
            Expected Flow:
            1. B2B customer authentication and bulk pricing access
            2. Bulk order creation with quantity discounts
            3. Bulk inventory allocation and availability check
            4. Special packaging and handling requirements
            5. Bulk payment processing with credit terms
            6. Bulk fulfillment workflow with consolidated shipping
            7. Freight shipping coordination for large orders
            8. B2B customer notifications and documentation
            9. Bulk delivery coordination and scheduling
            10. Invoice generation and B2B payment terms
            11. Bulk order completion and customer confirmation
            
            Success Criteria:
            - Bulk pricing applied correctly
            - Large quantity inventory handled
            - Freight shipping coordinated
            - B2B payment terms applied
            - Bulk fulfillment workflow operational
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(5, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify bulk order handling
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("bulk")),
            "Bulk order processing should be verified");
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    void testSubscriptionOrderFulfillment() throws Exception {
        // Test recurring subscription order processing
        String scenario = """
            Subscription Order System Test:
            
            Business Context: Customer has active subscription with recurring deliveries
            
            Expected Flow:
            1. Subscription service triggers recurring order
            2. Customer subscription status and preferences validated
            3. Subscription inventory allocation and availability
            4. Automatic payment processing for subscription
            5. Subscription order creation with recurring order ID
            6. Standard fulfillment process for subscription items
            7. Subscription-specific packaging and messaging
            8. Subscription delivery with recurring schedule
            9. Subscription analytics and usage tracking
            10. Next subscription order scheduling
            11. Subscription management and customer portal updates
            
            Success Criteria:
            - Subscription triggers working correctly
            - Recurring payment processing functional
            - Subscription inventory managed properly
            - Recurring delivery schedule maintained
            - Subscription analytics captured
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(3, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify subscription handling
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("subscription")),
            "Subscription order processing should be verified");
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    void testCrossChannelOrderIntegration() throws Exception {
        // Test order placed through multiple channels (web, mobile, phone, store)
        String scenario = """
            Cross-Channel Order Integration Test:
            
            Business Context: Customer interacts across multiple channels during order process
            
            Expected Flow:
            1. Customer starts order on mobile app
            2. Customer continues on web browser
            3. Customer calls customer service for assistance
            4. Customer service representative accesses order in progress
            5. Order modifications made through customer service
            6. Customer completes payment through web
            7. Order confirmation sent across all channels
            8. Customer tracks order through mobile app
            9. Customer receives notifications on preferred channels
            10. Cross-channel analytics and attribution tracking
            
            Success Criteria:
            - Order state synchronized across channels
            - Customer service integration functional
            - Cross-channel notifications working
            - Attribution tracking accurate
            - Consistent customer experience
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(4, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify cross-channel integration
        assertTrue(testResult.getAssertionResults().stream()
            .anyMatch(assertion -> assertion.getDescription().contains("cross-channel")),
            "Cross-channel integration should be verified");
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    void testSystemRecoveryAfterFailure() throws Exception {
        // Test system recovery and order completion after service failures
        String scenario = """
            System Recovery Test:
            
            Business Context: Order processing continues after temporary system failures
            
            Expected Flow:
            1. Customer places order during normal operation
            2. Simulate temporary service failures during processing
            3. System detects failures and activates recovery mechanisms
            4. Failed operations queued for retry
            5. Services recover and resume processing
            6. Queued operations processed successfully
            7. Order completion despite temporary failures
            8. Customer experience minimally impacted
            9. System monitoring and alerting functional
            10. Recovery metrics and reporting captured
            
            Success Criteria:
            - Order completes despite failures
            - Recovery mechanisms functional
            - Data consistency maintained
            - Customer experience preserved
            - Monitoring and alerting working
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        // Inject some controlled failures
        plan.getConfiguration().put("simulateFailures", true);
        plan.getConfiguration().put("failureRecoveryTest", true);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(5, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify recovery mechanisms
        assertTrue(testResult.getEvents().stream()
            .anyMatch(event -> event.getType() == EventType.RECOVERY_INITIATED),
            "Recovery mechanisms should be activated");
            
        assertTrue(testResult.getEvents().stream()
            .anyMatch(event -> event.getType() == EventType.RECOVERY_COMPLETED),
            "Recovery should complete successfully");
    }

    private void verifySystemHealth() {
        List<ServiceInfo> services = serviceDiscoveryManager.discoverServices();
        for (ServiceInfo service : services) {
            HealthCheckResult health = serviceDiscoveryManager.checkHealth(service.getServiceId());
            if (health.getStatus() != ServiceStatus.HEALTHY) {
                fail("Service " + service.getServiceId() + " is not healthy: " + health.getMessage());
            }
        }
    }

    private SystemTestContext createSystemTestContext() {
        SystemTestContext context = new SystemTestContext();
        context.setTestSuiteId(UUID.randomUUID().toString());
        context.setStartTime(Instant.now());
        context.setEnvironment("system-test");
        return context;
    }

    private TestPattern createTestPattern(TestResult testResult) {
        TestPattern pattern = new TestPattern();
        pattern.setPatternType(PatternType.SUCCESSFUL_FLOW);
        pattern.setServiceSequence(testResult.getServiceInteractions().stream()
            .map(ServiceInteraction::getServiceId)
            .toList());
        pattern.setExecutionTime(testResult.getExecutionTime());
        pattern.setSuccessRate(1.0);
        return pattern;
    }
}