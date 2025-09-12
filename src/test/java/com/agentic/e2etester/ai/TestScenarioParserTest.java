package com.agentic.e2etester.ai;

import com.agentic.e2etester.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestScenarioParser.
 */
@ExtendWith(MockitoExtension.class)
class TestScenarioParserTest {

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
    void parseScenario_ValidScenario_ReturnsTestExecutionPlan() throws Exception {
        // Arrange
        String scenario = "When a customer places an order, then the order should be processed and inventory should be updated";
        String template = "Test template with {scenario}";
        String llmResponse = """
            {
              "testId": "test-001",
              "scenario": "When a customer places an order, then the order should be processed and inventory should be updated",
              "steps": [
                {
                  "stepId": "step-1",
                  "type": "rest_call",
                  "description": "Create order",
                  "targetService": "order-service",
                  "inputData": {"customerId": "123", "productId": "456"},
                  "expectedOutcomes": ["Order created successfully"],
                  "timeout": "30s",
                  "dependencies": []
                }
              ],
              "assertions": [
                {
                  "type": "BUSINESS",
                  "description": "Order should be created",
                  "condition": "order.status == 'CREATED'",
                  "expectedValue": "CREATED"
                }
              ],
              "testData": {
                "customerId": "test-customer-123",
                "orderId": "test-order-456"
              }
            }
            """;

        when(promptTemplates.getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class))).thenReturn(template);
        when(llmService.sendTemplatedPrompt(eq(template), any(Map.class)))
            .thenReturn(new LLMService.LLMResponse(llmResponse, 1000L, true));

        // Act
        TestExecutionPlan plan = parser.parseScenario(scenario);

        // Assert
        assertNotNull(plan);
        assertEquals("test-001", plan.getTestId());
        assertEquals(scenario, plan.getScenario());
        assertEquals(1, plan.getSteps().size());
        assertEquals(1, plan.getAssertions().size());
        assertNotNull(plan.getTestData());

        TestStep step = plan.getSteps().get(0);
        assertEquals("step-1", step.getStepId());
        assertEquals(StepType.REST_CALL, step.getType());
        assertEquals("Create order", step.getDescription());
        assertEquals("order-service", step.getTargetService());

        AssertionRule assertion = plan.getAssertions().get(0);
        assertEquals(AssertionType.EQUALS, assertion.getType()); // Mapped from BUSINESS category
        assertEquals("Order should be created", assertion.getDescription());
        assertEquals("order.status == 'CREATED'", assertion.getCondition());
        assertEquals("CREATED", assertion.getExpectedValue());

        verify(promptTemplates).getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class));
        verify(llmService).sendTemplatedPrompt(eq(template), any(Map.class));
    }

    @Test
    void parseScenario_LLMFails_ThrowsTestScenarioParsingException() {
        // Arrange
        String scenario = "Given a customer wants to place an order, when they submit the order, then the system should process it successfully";
        String template = "Test template";
        String errorMessage = "LLM connection failed";

        when(promptTemplates.getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class))).thenReturn(template);
        when(llmService.sendTemplatedPrompt(eq(template), any(Map.class)))
            .thenReturn(new LLMService.LLMResponse("", 0L, false, errorMessage));

        // Act & Assert
        TestScenarioParser.TestScenarioParsingException exception = assertThrows(
            TestScenarioParser.TestScenarioParsingException.class,
            () -> parser.parseScenario(scenario)
        );

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Failed to parse test scenario"));
    }

    @Test
    void parseScenario_InvalidJSON_ThrowsTestScenarioParsingException() {
        // Arrange
        String scenario = "Given a customer wants to place an order, when they submit the order, then the system should process it successfully";
        String template = "Test template";
        String invalidJson = "{ invalid json }";

        when(promptTemplates.getTemplate(eq("TEST_SCENARIO_PARSING"), any(Map.class))).thenReturn(template);
        when(llmService.sendTemplatedPrompt(eq(template), any(Map.class)))
            .thenReturn(new LLMService.LLMResponse(invalidJson, 1000L, true));

        // Act & Assert
        TestScenarioParser.TestScenarioParsingException exception = assertThrows(
            TestScenarioParser.TestScenarioParsingException.class,
            () -> parser.parseScenario(scenario)
        );

        assertEquals("Failed to parse test scenario", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void validateScenario_ValidScenario_ReturnsValidResult() {
        // Arrange
        String scenario = "Given a customer, when they place an order, then the order should be processed successfully";

        // Act
        TestScenarioParser.ScenarioValidationResult result = parser.validateScenario(scenario);

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("Scenario appears valid", result.getMessage());
    }

    @Test
    void validateScenario_EmptyScenario_ReturnsInvalidResult() {
        // Arrange
        String scenario = "";

        // Act
        TestScenarioParser.ScenarioValidationResult result = parser.validateScenario(scenario);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Scenario cannot be empty", result.getMessage());
    }

    @Test
    void validateScenario_NullScenario_ReturnsInvalidResult() {
        // Arrange
        String scenario = null;

        // Act
        TestScenarioParser.ScenarioValidationResult result = parser.validateScenario(scenario);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Scenario cannot be empty", result.getMessage());
    }

    @Test
    void validateScenario_TooShortScenario_ReturnsInvalidResult() {
        // Arrange
        String scenario = "Short";

        // Act
        TestScenarioParser.ScenarioValidationResult result = parser.validateScenario(scenario);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Scenario too short, provide more details", result.getMessage());
    }

    @Test
    void validateScenario_TooLongScenario_ReturnsInvalidResult() {
        // Arrange
        String scenario = "A".repeat(5001);

        // Act
        TestScenarioParser.ScenarioValidationResult result = parser.validateScenario(scenario);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Scenario too long, please break it down", result.getMessage());
    }

    @Test
    void validateScenario_NoAction_ReturnsInvalidResult() {
        // Arrange
        String scenario = "This scenario has no action words and no expectations either";

        // Act
        TestScenarioParser.ScenarioValidationResult result = parser.validateScenario(scenario);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Scenario should describe what action to perform", result.getMessage());
    }

    @Test
    void validateScenario_NoExpectation_ReturnsInvalidResult() {
        // Arrange
        String scenario = "When a user performs an action but nothing is mentioned about what should happen";

        // Act
        TestScenarioParser.ScenarioValidationResult result = parser.validateScenario(scenario);

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals("Scenario should describe expected outcomes", result.getMessage());
    }

    @Test
    void testExecutionPlan_Constructor_SetsPropertiesCorrectly() {
        // Arrange
        String testId = "test-123";
        String scenario = "Test scenario";
        List<TestStep> steps = List.of();
        TestConfiguration configuration = new TestConfiguration();
        Map<String, Object> testData = Map.of("key", "value");

        // Act
        TestExecutionPlan plan = new TestExecutionPlan(testId, scenario, steps, configuration);
        plan.setTestData(testData);

        // Assert
        assertEquals(testId, plan.getTestId());
        assertEquals(scenario, plan.getScenario());
        assertEquals(steps, plan.getSteps());
        assertEquals(configuration, plan.getConfiguration());
        assertEquals(testData, plan.getTestData());
    }

    @Test
    void scenarioValidationResult_Constructor_SetsPropertiesCorrectly() {
        // Arrange
        boolean valid = true;
        String message = "Test message";

        // Act
        TestScenarioParser.ScenarioValidationResult result = 
            new TestScenarioParser.ScenarioValidationResult(valid, message);

        // Assert
        assertEquals(valid, result.isValid());
        assertEquals(message, result.getMessage());
    }

    @Test
    void testScenarioParsingException_Constructor_SetsMessageAndCause() {
        // Arrange
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Root cause");

        // Act
        TestScenarioParser.TestScenarioParsingException exception = 
            new TestScenarioParser.TestScenarioParsingException(message, cause);

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void stepType_EnumValues_AreCorrect() {
        // Act & Assert
        StepType[] types = StepType.values();
        assertEquals(7, types.length);
        assertTrue(List.of(types).contains(StepType.KAFKA_EVENT));
        assertTrue(List.of(types).contains(StepType.REST_CALL));
        assertTrue(List.of(types).contains(StepType.DATABASE_CHECK));
        assertTrue(List.of(types).contains(StepType.ASSERTION));
        assertTrue(List.of(types).contains(StepType.WAIT));
        assertTrue(List.of(types).contains(StepType.SETUP));
        assertTrue(List.of(types).contains(StepType.CLEANUP));
    }

    @Test
    void assertionType_EnumValues_AreCorrect() {
        // Act & Assert
        AssertionType[] types = AssertionType.values();
        assertEquals(13, types.length);
        assertTrue(List.of(types).contains(AssertionType.EQUALS));
        assertTrue(List.of(types).contains(AssertionType.NOT_EQUALS));
        assertTrue(List.of(types).contains(AssertionType.CONTAINS));
        assertTrue(List.of(types).contains(AssertionType.CUSTOM));
    }
}