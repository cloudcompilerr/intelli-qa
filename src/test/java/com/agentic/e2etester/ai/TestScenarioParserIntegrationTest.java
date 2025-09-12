package com.agentic.e2etester.ai;

import com.agentic.e2etester.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for TestScenarioParser with sample natural language test cases.
 */
@ExtendWith(MockitoExtension.class)
class TestScenarioParserIntegrationTest {

    @Mock
    private LLMService llmService;

    @Mock
    private PromptTemplates promptTemplates;

    private ObjectMapper objectMapper;
    private TestScenarioParser parser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new TestScenarioParser(llmService, promptTemplates, objectMapper);
    }

    @Test
    void parseScenario_CompleteOrderFulfillmentFlow_ReturnsComprehensiveTestPlan() throws Exception {
        // Arrange
        String scenario = """
            Given a customer with ID 'CUST-001' wants to place an order for product 'PROD-123' with quantity 2,
            When the customer submits the order through the order-service,
            Then the system should create the order, process payment, reserve inventory, and initiate fulfillment,
            And the customer should receive an order confirmation with tracking information,
            And the order status should be 'CONFIRMED' in the database.
            """;

        String template = "Test template";
        String llmResponse = """
            {
              "testId": "order-fulfillment-e2e-001",
              "scenario": "Complete order fulfillment flow test",
              "steps": [
                {
                  "stepId": "setup-test-data",
                  "type": "setup",
                  "description": "Initialize test customer and product data",
                  "targetService": "test-data-service",
                  "inputData": {
                    "customerId": "CUST-001",
                    "productId": "PROD-123",
                    "quantity": 2
                  },
                  "expectedOutcomes": ["Test data created successfully"],
                  "timeout": "10s",
                  "dependencies": []
                },
                {
                  "stepId": "create-order",
                  "type": "rest_call",
                  "description": "Submit order through order service",
                  "targetService": "order-service",
                  "inputData": {
                    "customerId": "CUST-001",
                    "items": [
                      {
                        "productId": "PROD-123",
                        "quantity": 2,
                        "price": 29.99
                      }
                    ]
                  },
                  "expectedOutcomes": ["Order created with status PENDING"],
                  "timeout": "30s",
                  "dependencies": ["setup-test-data"]
                },
                {
                  "stepId": "verify-payment-event",
                  "type": "kafka_event",
                  "description": "Verify payment processing event is published",
                  "targetService": "payment-events-topic",
                  "inputData": {},
                  "expectedOutcomes": ["Payment event received with correct order ID"],
                  "timeout": "15s",
                  "dependencies": ["create-order"]
                },
                {
                  "stepId": "check-inventory-reservation",
                  "type": "database_check",
                  "description": "Verify inventory is reserved for the order",
                  "targetService": "inventory-database",
                  "inputData": {
                    "productId": "PROD-123",
                    "expectedReservedQuantity": 2
                  },
                  "expectedOutcomes": ["Inventory reserved successfully"],
                  "timeout": "20s",
                  "dependencies": ["verify-payment-event"]
                },
                {
                  "stepId": "verify-fulfillment-initiation",
                  "type": "rest_call",
                  "description": "Check fulfillment service received order",
                  "targetService": "fulfillment-service",
                  "inputData": {
                    "orderId": "${create-order.response.orderId}"
                  },
                  "expectedOutcomes": ["Fulfillment process initiated"],
                  "timeout": "25s",
                  "dependencies": ["check-inventory-reservation"]
                },
                {
                  "stepId": "validate-order-confirmation",
                  "type": "assertion",
                  "description": "Validate final order status and confirmation",
                  "targetService": "order-service",
                  "inputData": {
                    "orderId": "${create-order.response.orderId}"
                  },
                  "expectedOutcomes": ["Order status is CONFIRMED", "Tracking information provided"],
                  "timeout": "10s",
                  "dependencies": ["verify-fulfillment-initiation"]
                },
                {
                  "stepId": "cleanup-test-data",
                  "type": "cleanup",
                  "description": "Clean up test artifacts",
                  "targetService": "test-data-service",
                  "inputData": {
                    "orderId": "${create-order.response.orderId}",
                    "customerId": "CUST-001"
                  },
                  "expectedOutcomes": ["Test data cleaned successfully"],
                  "timeout": "15s",
                  "dependencies": ["validate-order-confirmation"]
                }
              ],
              "assertions": [
                {
                  "type": "BUSINESS",
                  "description": "Order should be successfully created and confirmed",
                  "condition": "order.status == 'CONFIRMED'",
                  "expectedValue": "CONFIRMED"
                },
                {
                  "type": "TECHNICAL",
                  "description": "All service interactions should complete within SLA",
                  "condition": "totalExecutionTime < 120000",
                  "expectedValue": "< 2 minutes"
                },
                {
                  "type": "DATA",
                  "description": "Inventory should be properly reserved",
                  "condition": "inventory.reserved >= order.quantity",
                  "expectedValue": "2"
                }
              ],
              "testData": {
                "customerId": "CUST-001",
                "productId": "PROD-123",
                "quantity": 2,
                "expectedPrice": 29.99
              }
            }
            """;

        when(promptTemplates.getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class))).thenReturn(template);
        when(llmService.sendTemplatedPrompt(eq(template), any(Map.class)))
            .thenReturn(new LLMService.LLMResponse(llmResponse, 2000L, true));

        // Act
        TestExecutionPlan plan = parser.parseScenario(scenario);

        // Assert
        assertNotNull(plan);
        assertEquals("order-fulfillment-e2e-001", plan.getTestId());
        assertEquals(7, plan.getSteps().size());
        assertEquals(3, plan.getAssertions().size());
        assertNotNull(plan.getTestData());
        assertNotNull(plan.getConfiguration());

        // Verify step sequence and types
        assertEquals("setup-test-data", plan.getSteps().get(0).getStepId());
        assertEquals(StepType.SETUP, plan.getSteps().get(0).getType());
        
        assertEquals("create-order", plan.getSteps().get(1).getStepId());
        assertEquals(StepType.REST_CALL, plan.getSteps().get(1).getType());
        
        assertEquals("verify-payment-event", plan.getSteps().get(2).getStepId());
        assertEquals(StepType.KAFKA_EVENT, plan.getSteps().get(2).getType());
        
        assertEquals("check-inventory-reservation", plan.getSteps().get(3).getStepId());
        assertEquals(StepType.DATABASE_CHECK, plan.getSteps().get(3).getType());
        
        assertEquals("cleanup-test-data", plan.getSteps().get(6).getStepId());
        assertEquals(StepType.CLEANUP, plan.getSteps().get(6).getType());

        // Verify assertions are mapped correctly
        assertEquals(AssertionType.EQUALS, plan.getAssertions().get(0).getType()); // BUSINESS -> EQUALS
        assertEquals(AssertionType.LESS_THAN, plan.getAssertions().get(1).getType()); // TECHNICAL -> LESS_THAN
        assertEquals(AssertionType.GREATER_THAN_OR_EQUAL, plan.getAssertions().get(2).getType()); // DATA -> GREATER_THAN_OR_EQUAL

        verify(promptTemplates).getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class));
        verify(llmService).sendTemplatedPrompt(eq(template), any(Map.class));
    }

    @Test
    void parseScenario_PaymentFailureScenario_ReturnsErrorHandlingTestPlan() throws Exception {
        // Arrange
        String scenario = """
            Given a customer attempts to place an order with an invalid payment method,
            When the payment processing fails,
            Then the system should handle the error gracefully,
            And the order should be marked as 'PAYMENT_FAILED',
            And the customer should receive an appropriate error message,
            And no inventory should be reserved.
            """;

        String template = "Test template";
        String llmResponse = """
            {
              "testId": "payment-failure-handling-001",
              "scenario": "Payment failure error handling test",
              "steps": [
                {
                  "stepId": "setup-invalid-payment",
                  "type": "setup",
                  "description": "Setup customer with invalid payment method",
                  "targetService": "test-data-service",
                  "inputData": {
                    "customerId": "CUST-INVALID-PAY",
                    "paymentMethod": "INVALID_CARD"
                  },
                  "expectedOutcomes": ["Invalid payment setup complete"],
                  "timeout": "10s",
                  "dependencies": []
                },
                {
                  "stepId": "attempt-order-creation",
                  "type": "rest_call",
                  "description": "Attempt to create order with invalid payment",
                  "targetService": "order-service",
                  "inputData": {
                    "customerId": "CUST-INVALID-PAY",
                    "items": [{"productId": "PROD-123", "quantity": 1}],
                    "paymentMethod": "INVALID_CARD"
                  },
                  "expectedOutcomes": ["Order creation initiated"],
                  "timeout": "30s",
                  "dependencies": ["setup-invalid-payment"]
                },
                {
                  "stepId": "verify-payment-failure-event",
                  "type": "kafka_event",
                  "description": "Verify payment failure event is published",
                  "targetService": "payment-events-topic",
                  "inputData": {},
                  "expectedOutcomes": ["Payment failure event received"],
                  "timeout": "20s",
                  "dependencies": ["attempt-order-creation"]
                },
                {
                  "stepId": "check-order-status",
                  "type": "database_check",
                  "description": "Verify order status is PAYMENT_FAILED",
                  "targetService": "order-database",
                  "inputData": {
                    "orderId": "${attempt-order-creation.response.orderId}"
                  },
                  "expectedOutcomes": ["Order status is PAYMENT_FAILED"],
                  "timeout": "15s",
                  "dependencies": ["verify-payment-failure-event"]
                },
                {
                  "stepId": "verify-no-inventory-reservation",
                  "type": "database_check",
                  "description": "Verify no inventory was reserved",
                  "targetService": "inventory-database",
                  "inputData": {
                    "productId": "PROD-123",
                    "orderId": "${attempt-order-creation.response.orderId}"
                  },
                  "expectedOutcomes": ["No inventory reserved for failed order"],
                  "timeout": "10s",
                  "dependencies": ["check-order-status"]
                }
              ],
              "assertions": [
                {
                  "type": "BUSINESS",
                  "description": "Order should be marked as payment failed",
                  "condition": "order.status == 'PAYMENT_FAILED'",
                  "expectedValue": "PAYMENT_FAILED"
                },
                {
                  "type": "DATA",
                  "description": "No inventory should be reserved for failed payment",
                  "condition": "inventory.reserved == 0",
                  "expectedValue": "0"
                }
              ],
              "testData": {
                "customerId": "CUST-INVALID-PAY",
                "productId": "PROD-123",
                "invalidPaymentMethod": "INVALID_CARD"
              }
            }
            """;

        when(promptTemplates.getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class))).thenReturn(template);
        when(llmService.sendTemplatedPrompt(eq(template), any(Map.class)))
            .thenReturn(new LLMService.LLMResponse(llmResponse, 1500L, true));

        // Act
        TestExecutionPlan plan = parser.parseScenario(scenario);

        // Assert
        assertNotNull(plan);
        assertEquals("payment-failure-handling-001", plan.getTestId());
        assertEquals(5, plan.getSteps().size());
        assertEquals(2, plan.getAssertions().size());

        // Verify error handling steps
        assertTrue(plan.getSteps().stream().anyMatch(step -> 
            step.getDescription().contains("invalid payment")));
        assertTrue(plan.getSteps().stream().anyMatch(step -> 
            step.getDescription().contains("payment failure event")));
        assertTrue(plan.getSteps().stream().anyMatch(step -> 
            step.getDescription().contains("no inventory")));

        verify(promptTemplates).getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class));
        verify(llmService).sendTemplatedPrompt(eq(template), any(Map.class));
    }

    @Test
    void parseScenario_HighVolumeOrderScenario_ReturnsPerformanceTestPlan() throws Exception {
        // Arrange
        String scenario = """
            Given the system needs to handle 1000 concurrent orders during peak traffic,
            When multiple customers simultaneously place orders,
            Then all orders should be processed within acceptable time limits,
            And the system should maintain data consistency,
            And no orders should be lost or duplicated.
            """;

        String template = "Test template";
        String llmResponse = """
            {
              "testId": "high-volume-performance-001",
              "scenario": "High volume concurrent order processing test",
              "steps": [
                {
                  "stepId": "setup-performance-test-data",
                  "type": "setup",
                  "description": "Setup 1000 test customers and products",
                  "targetService": "test-data-service",
                  "inputData": {
                    "customerCount": 1000,
                    "productCount": 50
                  },
                  "expectedOutcomes": ["Performance test data ready"],
                  "timeout": "60s",
                  "dependencies": []
                },
                {
                  "stepId": "initiate-concurrent-orders",
                  "type": "rest_call",
                  "description": "Submit 1000 concurrent orders",
                  "targetService": "order-service",
                  "inputData": {
                    "concurrentRequests": 1000,
                    "requestPattern": "burst"
                  },
                  "expectedOutcomes": ["All orders submitted successfully"],
                  "timeout": "120s",
                  "dependencies": ["setup-performance-test-data"]
                },
                {
                  "stepId": "monitor-kafka-throughput",
                  "type": "kafka_event",
                  "description": "Monitor event processing throughput",
                  "targetService": "order-events-topic",
                  "inputData": {
                    "expectedEventCount": 1000,
                    "timeWindow": "120s"
                  },
                  "expectedOutcomes": ["All events processed within time window"],
                  "timeout": "150s",
                  "dependencies": ["initiate-concurrent-orders"]
                },
                {
                  "stepId": "validate-data-consistency",
                  "type": "database_check",
                  "description": "Verify all orders are persisted correctly",
                  "targetService": "order-database",
                  "inputData": {
                    "expectedOrderCount": 1000
                  },
                  "expectedOutcomes": ["All orders persisted", "No duplicates found"],
                  "timeout": "60s",
                  "dependencies": ["monitor-kafka-throughput"]
                },
                {
                  "stepId": "check-system-performance",
                  "type": "assertion",
                  "description": "Validate system performance metrics",
                  "targetService": "monitoring-service",
                  "inputData": {
                    "metricsToCheck": ["response_time", "error_rate", "throughput"]
                  },
                  "expectedOutcomes": ["Performance within SLA"],
                  "timeout": "30s",
                  "dependencies": ["validate-data-consistency"]
                }
              ],
              "assertions": [
                {
                  "type": "TECHNICAL",
                  "description": "Average response time should be under 2 seconds",
                  "condition": "avg_response_time < 2000",
                  "expectedValue": "< 2000ms"
                },
                {
                  "type": "DATA",
                  "description": "All 1000 orders should be processed",
                  "condition": "processed_orders == 1000",
                  "expectedValue": "1000"
                },
                {
                  "type": "TECHNICAL",
                  "description": "Error rate should be less than 1%",
                  "condition": "error_rate < 0.01",
                  "expectedValue": "< 1%"
                }
              ],
              "testData": {
                "concurrentUsers": 1000,
                "testDuration": "2m",
                "expectedThroughput": "500 orders/second"
              }
            }
            """;

        when(promptTemplates.getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class))).thenReturn(template);
        when(llmService.sendTemplatedPrompt(eq(template), any(Map.class)))
            .thenReturn(new LLMService.LLMResponse(llmResponse, 1800L, true));

        // Act
        TestExecutionPlan plan = parser.parseScenario(scenario);

        // Assert
        assertNotNull(plan);
        assertEquals("high-volume-performance-001", plan.getTestId());
        assertEquals(5, plan.getSteps().size());
        assertEquals(3, plan.getAssertions().size());

        // Verify performance testing aspects
        assertTrue(plan.getSteps().stream().anyMatch(step -> 
            step.getDescription().contains("1000 concurrent")));
        assertTrue(plan.getSteps().stream().anyMatch(step -> 
            step.getDescription().contains("throughput")));
        assertTrue(plan.getSteps().stream().anyMatch(step -> 
            step.getDescription().contains("performance metrics")));

        // Verify performance assertions
        assertTrue(plan.getAssertions().stream().anyMatch(assertion -> 
            assertion.getCondition().contains("response_time")));
        assertTrue(plan.getAssertions().stream().anyMatch(assertion -> 
            assertion.getCondition().contains("error_rate")));

        verify(promptTemplates).getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class));
        verify(llmService).sendTemplatedPrompt(eq(template), any(Map.class));
    }

    @Test
    void parseScenario_InvalidScenario_ThrowsValidationException() {
        // Arrange
        String invalidScenario = "Short";

        // Act & Assert
        TestScenarioParser.TestScenarioParsingException exception = assertThrows(
            TestScenarioParser.TestScenarioParsingException.class,
            () -> parser.parseScenario(invalidScenario)
        );

        assertTrue(exception.getMessage().contains("Invalid scenario: Scenario too short, provide more details"));
    }

    @Test
    void parseScenario_MalformedLLMResponse_ThrowsParsingException() {
        // Arrange
        String scenario = "Given a valid scenario, when processing occurs, then results should be generated";
        String template = "Test template";
        String malformedResponse = "{ invalid json response }";

        when(promptTemplates.getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class))).thenReturn(template);
        when(llmService.sendTemplatedPrompt(eq(template), any(Map.class)))
            .thenReturn(new LLMService.LLMResponse(malformedResponse, 1000L, true));

        // Act & Assert
        TestScenarioParser.TestScenarioParsingException exception = assertThrows(
            TestScenarioParser.TestScenarioParsingException.class,
            () -> parser.parseScenario(scenario)
        );

        assertEquals("Failed to parse test scenario", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}