package com.agentic.e2etester.integration;

import com.agentic.e2etester.controller.AgenticTestController;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.orchestration.TestOrchestrator;
import com.agentic.e2etester.service.ServiceDiscoveryManager;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import com.agentic.e2etester.integration.kafka.KafkaTestProducer;
import com.agentic.e2etester.integration.rest.RestApiAdapter;
import com.agentic.e2etester.integration.database.CouchbaseAdapter;
import com.agentic.e2etester.monitoring.TestObservabilityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive end-to-end integration tests that validate the complete
 * order fulfillment journey across all microservices with full system integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public class EndToEndIntegrationTest {

    @Autowired
    private AgenticTestController agenticTestController;
    
    @Autowired
    private TestOrchestrator testOrchestrator;
    
    @Autowired
    private ServiceDiscoveryManager serviceDiscoveryManager;
    
    @Autowired
    private TestExecutionEngine testExecutionEngine;
    
    @Autowired
    private KafkaTestProducer kafkaTestProducer;
    
    @Autowired
    private RestApiAdapter restApiAdapter;
    
    @Autowired
    private CouchbaseAdapter couchbaseAdapter;
    
    @Autowired
    private TestObservabilityManager observabilityManager;

    private TestContext testContext;
    private String correlationId;

    @BeforeEach
    void setUp() {
        correlationId = UUID.randomUUID().toString();
        testContext = createTestContext(correlationId);
    }

    @Test
    void testCompleteOrderFulfillmentFlow() throws Exception {
        // Test complete order fulfillment across all 16-20 microservices
        String scenario = """
            Complete order fulfillment test:
            1. Customer places order for product SKU-12345
            2. Inventory service validates availability
            3. Payment service processes payment
            4. Order service creates order record
            5. Fulfillment service initiates picking
            6. Shipping service schedules delivery
            7. Notification service sends confirmations
            8. All services update order status appropriately
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        assertNotNull(plan);
        assertEquals(8, plan.getSteps().size());

        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(5, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        assertTrue(testResult.getStepResults().stream()
            .allMatch(step -> step.getStatus() == TestStatus.PASSED));
        
        // Verify all services were involved
        assertTrue(testResult.getServiceInteractions().size() >= 16);
        
        // Verify end-to-end correlation tracking
        assertTrue(testResult.getServiceInteractions().stream()
            .allMatch(interaction -> interaction.getCorrelationId().equals(correlationId)));
    }

    @Test
    void testMultiServiceFailureRecovery() throws Exception {
        // Test system behavior when multiple services fail
        String scenario = """
            Order fulfillment with service failures:
            1. Customer places order
            2. Simulate inventory service timeout
            3. Simulate payment service circuit breaker
            4. Verify graceful degradation
            5. Verify error handling and rollback
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        // Inject failure conditions
        plan.getSteps().get(1).getConfiguration().put("simulateTimeout", true);
        plan.getSteps().get(2).getConfiguration().put("simulateCircuitBreaker", true);

        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(3, TimeUnit.MINUTES);

        // Should handle failures gracefully
        assertEquals(TestStatus.FAILED, testResult.getStatus());
        assertNotNull(testResult.getFailureAnalysis());
        assertTrue(testResult.getFailureAnalysis().getRemediationSuggestions().size() > 0);
        
        // Verify rollback occurred
        assertTrue(testResult.getEvents().stream()
            .anyMatch(event -> event.getType() == EventType.ROLLBACK_INITIATED));
    }

    @Test
    void testCrossServiceDataConsistency() throws Exception {
        // Test data consistency across all microservices and databases
        String scenario = """
            Data consistency validation:
            1. Create order with specific customer and product data
            2. Verify data propagation to all relevant services
            3. Validate data consistency across all databases
            4. Check eventual consistency after async operations
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(4, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify data consistency assertions passed
        assertTrue(testResult.getAssertionResults().stream()
            .filter(assertion -> assertion.getType() == AssertionType.DATA_CONSISTENCY)
            .allMatch(assertion -> assertion.isPassed()));
    }

    @Test
    void testEventDrivenArchitectureFlow() throws Exception {
        // Test complete Kafka event flow across all services
        String scenario = """
            Event-driven architecture validation:
            1. Publish order created event
            2. Verify all downstream services consume events
            3. Validate event ordering and partitioning
            4. Check dead letter queue handling
            5. Verify event replay capabilities
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(3, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify Kafka events were properly tracked
        assertTrue(testResult.getServiceInteractions().stream()
            .anyMatch(interaction -> interaction.getType() == InteractionType.KAFKA_PRODUCE));
        assertTrue(testResult.getServiceInteractions().stream()
            .anyMatch(interaction -> interaction.getType() == InteractionType.KAFKA_CONSUME));
    }

    @Test
    void testBusinessScenarioValidation() throws Exception {
        // Test business-critical scenarios end-to-end
        String scenario = """
            Critical business scenario - Premium customer order:
            1. Premium customer places high-value order
            2. Apply premium customer discounts
            3. Use expedited fulfillment process
            4. Verify premium shipping options
            5. Send premium customer notifications
            6. Validate loyalty points calculation
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(4, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify business assertions passed
        assertTrue(testResult.getAssertionResults().stream()
            .filter(assertion -> assertion.getCategory() == AssertionCategory.BUSINESS)
            .allMatch(assertion -> assertion.isPassed()));
    }

    @Test
    void testSystemResilienceUnderLoad() throws Exception {
        // Test system behavior under concurrent load
        List<CompletableFuture<TestResult>> concurrentTests = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            String scenario = String.format("""
                Concurrent order test %d:
                1. Customer places order
                2. Process payment
                3. Fulfill order
                4. Complete delivery
                """, i);
            
            TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
            concurrentTests.add(testExecutionEngine.executeTest(plan));
        }

        // Wait for all tests to complete
        CompletableFuture<Void> allTests = CompletableFuture.allOf(
            concurrentTests.toArray(new CompletableFuture[0]));
        
        allTests.get(10, TimeUnit.MINUTES);

        // Verify all tests passed
        for (CompletableFuture<TestResult> test : concurrentTests) {
            TestResult result = test.get();
            assertEquals(TestStatus.PASSED, result.getStatus());
        }
    }

    @Test
    void testServiceDiscoveryIntegration() throws Exception {
        // Test dynamic service discovery and health monitoring
        List<ServiceInfo> discoveredServices = serviceDiscoveryManager.discoverServices();
        
        assertTrue(discoveredServices.size() >= 16, 
            "Should discover at least 16 microservices");
        
        // Verify all services are healthy
        for (ServiceInfo service : discoveredServices) {
            HealthCheckResult health = serviceDiscoveryManager.checkHealth(service.getServiceId());
            assertEquals(ServiceStatus.HEALTHY, health.getStatus());
        }
    }

    @Test
    void testMonitoringAndObservability() throws Exception {
        // Test comprehensive monitoring during test execution
        String scenario = """
            Monitoring validation test:
            1. Execute order fulfillment
            2. Collect metrics from all services
            3. Validate distributed tracing
            4. Check alerting thresholds
            """;

        TestExecutionPlan plan = agenticTestController.parseTestScenario(scenario);
        
        CompletableFuture<TestResult> result = testExecutionEngine.executeTest(plan);
        TestResult testResult = result.get(3, TimeUnit.MINUTES);

        assertEquals(TestStatus.PASSED, testResult.getStatus());
        
        // Verify monitoring data was collected
        assertNotNull(testResult.getMetrics());
        assertTrue(testResult.getMetrics().getResponseTimes().size() > 0);
        assertTrue(testResult.getMetrics().getErrorRates().size() > 0);
    }

    private TestContext createTestContext(String correlationId) {
        TestContext context = new TestContext();
        context.setCorrelationId(correlationId);
        context.setExecutionState(new HashMap<>());
        context.setInteractions(new ArrayList<>());
        context.setMetrics(new TestMetrics());
        context.setEvents(new ArrayList<>());
        return context;
    }
}