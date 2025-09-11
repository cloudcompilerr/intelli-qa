package com.agentic.e2etester.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PromptTemplates.
 */
class PromptTemplatesTest {

    private PromptTemplates promptTemplates;

    @BeforeEach
    void setUp() {
        promptTemplates = new PromptTemplates();
    }

    @Test
    void getTemplate_TestScenarioParsing_ReturnsCorrectTemplate() {
        // Arrange
        Map<String, Object> parameters = Map.of("scenario", "test scenario");

        // Act
        String template = promptTemplates.getTemplate("TEST_SCENARIO_PARSING", parameters);

        // Assert
        assertNotNull(template);
        assertTrue(template.contains("Parse the following natural language test scenario"));
        assertTrue(template.contains("{scenario}"));
        assertTrue(template.contains("JSON format"));
        assertTrue(template.contains("testId"));
        assertTrue(template.contains("steps"));
        assertTrue(template.contains("assertions"));
    }

    @Test
    void getTemplate_FailureAnalysis_ReturnsCorrectTemplate() {
        // Arrange
        Map<String, Object> parameters = Map.of(
            "testId", "test-123",
            "failedStep", "step-1",
            "errorMessage", "Connection failed",
            "serviceName", "order-service",
            "timestamp", "2024-01-01T10:00:00Z",
            "systemContext", "Production environment",
            "serviceInteractions", "REST call to order service",
            "relevantLogs", "Error logs here"
        );

        // Act
        String template = promptTemplates.getTemplate("FAILURE_ANALYSIS", parameters);

        // Assert
        assertNotNull(template);
        assertTrue(template.contains("Analyze the following test failure"));
        assertTrue(template.contains("{testId}"));
        assertTrue(template.contains("{failedStep}"));
        assertTrue(template.contains("{errorMessage}"));
        assertTrue(template.contains("rootCause"));
        assertTrue(template.contains("failureCategory"));
        assertTrue(template.contains("remediationSuggestions"));
    }

    @Test
    void getTemplate_TestDataGeneration_ReturnsCorrectTemplate() {
        // Arrange
        Map<String, Object> parameters = Map.of(
            "scenario", "Order placement test",
            "dataRequirements", "Customer and order data",
            "businessRules", "Valid customer with active account"
        );

        // Act
        String template = promptTemplates.getTemplate("TEST_DATA_GENERATION", parameters);

        // Assert
        assertNotNull(template);
        assertTrue(template.contains("Generate realistic test data"));
        assertTrue(template.contains("{scenario}"));
        assertTrue(template.contains("{dataRequirements}"));
        assertTrue(template.contains("{businessRules}"));
        assertTrue(template.contains("customer"));
        assertTrue(template.contains("order"));
        assertTrue(template.contains("fulfillment"));
    }

    @Test
    void getTemplate_ResultValidation_ReturnsCorrectTemplate() {
        // Arrange
        Map<String, Object> parameters = Map.of(
            "scenario", "Order processing test",
            "expectedOutcomes", "Order should be created",
            "actualResults", "Order created successfully",
            "executionContext", "Test environment"
        );

        // Act
        String template = promptTemplates.getTemplate("RESULT_VALIDATION", parameters);

        // Assert
        assertNotNull(template);
        assertTrue(template.contains("Validate the following test execution results"));
        assertTrue(template.contains("{scenario}"));
        assertTrue(template.contains("{expectedOutcomes}"));
        assertTrue(template.contains("{actualResults}"));
        assertTrue(template.contains("overallResult"));
        assertTrue(template.contains("validationDetails"));
        assertTrue(template.contains("businessImpact"));
    }

    @Test
    void getTemplate_ServiceInteractionAnalysis_ReturnsCorrectTemplate() {
        // Arrange
        Map<String, Object> parameters = Map.of(
            "serviceChain", "order-service -> payment-service -> inventory-service",
            "interactions", "REST calls and Kafka events",
            "performanceMetrics", "Latency and throughput data",
            "errorPatterns", "Timeout errors in payment service"
        );

        // Act
        String template = promptTemplates.getTemplate("SERVICE_INTERACTION_ANALYSIS", parameters);

        // Assert
        assertNotNull(template);
        assertTrue(template.contains("Analyze the following service interaction pattern"));
        assertTrue(template.contains("{serviceChain}"));
        assertTrue(template.contains("{interactions}"));
        assertTrue(template.contains("{performanceMetrics}"));
        assertTrue(template.contains("interactionFlow"));
        assertTrue(template.contains("performanceAnalysis"));
        assertTrue(template.contains("reliabilityAssessment"));
    }

    @Test
    void getTemplate_CaseInsensitive_ReturnsCorrectTemplate() {
        // Arrange
        Map<String, Object> parameters = Map.of("scenario", "test");

        // Act
        String template1 = promptTemplates.getTemplate("test_scenario_parsing", parameters);
        String template2 = promptTemplates.getTemplate("TEST_SCENARIO_PARSING", parameters);
        String template3 = promptTemplates.getTemplate("Test_Scenario_Parsing", parameters);

        // Assert
        assertEquals(template1, template2);
        assertEquals(template2, template3);
    }

    @Test
    void getTemplate_UnknownTemplate_ThrowsIllegalArgumentException() {
        // Arrange
        Map<String, Object> parameters = Map.of();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> promptTemplates.getTemplate("UNKNOWN_TEMPLATE", parameters)
        );

        assertTrue(exception.getMessage().contains("Unknown template: UNKNOWN_TEMPLATE"));
    }

    @Test
    void getTemplate_MissingRequiredParameter_ThrowsIllegalArgumentException() {
        // Arrange
        Map<String, Object> parameters = Map.of(); // Missing required 'scenario' parameter

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> promptTemplates.getTemplate("TEST_SCENARIO_PARSING", parameters)
        );

        assertTrue(exception.getMessage().contains("Missing required parameter 'scenario'"));
        assertTrue(exception.getMessage().contains("TEST_SCENARIO_PARSING"));
    }

    @Test
    void getTemplate_AllRequiredParametersPresent_DoesNotThrowException() {
        // Arrange
        Map<String, Object> parameters = Map.of("scenario", "test scenario");

        // Act & Assert
        assertDoesNotThrow(() -> promptTemplates.getTemplate("TEST_SCENARIO_PARSING", parameters));
    }

    @Test
    void getTemplate_ExtraParameters_DoesNotThrowException() {
        // Arrange
        Map<String, Object> parameters = Map.of(
            "scenario", "test scenario",
            "extraParam", "extra value"
        );

        // Act & Assert
        assertDoesNotThrow(() -> promptTemplates.getTemplate("TEST_SCENARIO_PARSING", parameters));
    }

    @Test
    void validateTemplateParameters_FailureAnalysisTemplate_ValidatesAllParameters() {
        // Arrange
        Map<String, Object> completeParameters = Map.of(
            "testId", "test-123",
            "failedStep", "step-1",
            "errorMessage", "Error occurred",
            "serviceName", "order-service",
            "timestamp", "2024-01-01T10:00:00Z",
            "systemContext", "Production",
            "serviceInteractions", "REST calls",
            "relevantLogs", "Log entries"
        );

        Map<String, Object> incompleteParameters = Map.of(
            "testId", "test-123",
            "failedStep", "step-1"
            // Missing other required parameters
        );

        // Act & Assert
        assertDoesNotThrow(() -> promptTemplates.getTemplate("FAILURE_ANALYSIS", completeParameters));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> promptTemplates.getTemplate("FAILURE_ANALYSIS", incompleteParameters)
        );
        
        assertTrue(exception.getMessage().contains("Missing required parameter"));
    }

    @Test
    void constantTemplates_ContainExpectedContent() {
        // Test that all template constants contain expected key elements
        
        // TEST_SCENARIO_PARSING_TEMPLATE
        assertTrue(PromptTemplates.TEST_SCENARIO_PARSING_TEMPLATE.contains("Parse the following natural language test scenario"));
        assertTrue(PromptTemplates.TEST_SCENARIO_PARSING_TEMPLATE.contains("JSON format"));
        assertTrue(PromptTemplates.TEST_SCENARIO_PARSING_TEMPLATE.contains("{scenario}"));
        
        // FAILURE_ANALYSIS_TEMPLATE
        assertTrue(PromptTemplates.FAILURE_ANALYSIS_TEMPLATE.contains("Analyze the following test failure"));
        assertTrue(PromptTemplates.FAILURE_ANALYSIS_TEMPLATE.contains("{testId}"));
        assertTrue(PromptTemplates.FAILURE_ANALYSIS_TEMPLATE.contains("rootCause"));
        
        // TEST_DATA_GENERATION_TEMPLATE
        assertTrue(PromptTemplates.TEST_DATA_GENERATION_TEMPLATE.contains("Generate realistic test data"));
        assertTrue(PromptTemplates.TEST_DATA_GENERATION_TEMPLATE.contains("{scenario}"));
        assertTrue(PromptTemplates.TEST_DATA_GENERATION_TEMPLATE.contains("customer"));
        
        // RESULT_VALIDATION_TEMPLATE
        assertTrue(PromptTemplates.RESULT_VALIDATION_TEMPLATE.contains("Validate the following test execution results"));
        assertTrue(PromptTemplates.RESULT_VALIDATION_TEMPLATE.contains("{expectedOutcomes}"));
        assertTrue(PromptTemplates.RESULT_VALIDATION_TEMPLATE.contains("overallResult"));
        
        // SERVICE_INTERACTION_ANALYSIS_TEMPLATE
        assertTrue(PromptTemplates.SERVICE_INTERACTION_ANALYSIS_TEMPLATE.contains("Analyze the following service interaction pattern"));
        assertTrue(PromptTemplates.SERVICE_INTERACTION_ANALYSIS_TEMPLATE.contains("{serviceChain}"));
        assertTrue(PromptTemplates.SERVICE_INTERACTION_ANALYSIS_TEMPLATE.contains("performanceAnalysis"));
    }
}