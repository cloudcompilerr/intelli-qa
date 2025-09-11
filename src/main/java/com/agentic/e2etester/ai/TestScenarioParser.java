package com.agentic.e2etester.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for parsing natural language test scenarios into structured test execution plans
 * using AI-powered natural language processing.
 */
@Service
public class TestScenarioParser {

    private static final Logger logger = LoggerFactory.getLogger(TestScenarioParser.class);
    
    private final LLMService llmService;
    private final PromptTemplates promptTemplates;
    private final ObjectMapper objectMapper;

    public TestScenarioParser(LLMService llmService, PromptTemplates promptTemplates, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.promptTemplates = promptTemplates;
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a natural language test scenario into a structured test execution plan.
     * 
     * @param scenario the natural language test scenario
     * @return parsed test execution plan
     * @throws TestScenarioParsingException if parsing fails
     */
    public TestExecutionPlan parseScenario(String scenario) {
        try {
            logger.info("Parsing test scenario: {}", scenario.substring(0, Math.min(100, scenario.length())));
            
            // Prepare template parameters
            Map<String, Object> parameters = Map.of("scenario", scenario);
            
            // Get the template and send to LLM
            String template = promptTemplates.getTemplate("TEST_SCENARIO_PARSING", parameters);
            LLMService.LLMResponse response = llmService.sendTemplatedPrompt(template, parameters);
            
            if (!response.isSuccess()) {
                throw new TestScenarioParsingException("LLM failed to parse scenario: " + response.getErrorMessage());
            }
            
            // Parse JSON response into TestExecutionPlan
            TestExecutionPlan plan = objectMapper.readValue(response.getContent(), TestExecutionPlan.class);
            
            logger.info("Successfully parsed scenario into {} steps", plan.getSteps().size());
            return plan;
            
        } catch (Exception e) {
            logger.error("Failed to parse test scenario: {}", e.getMessage(), e);
            throw new TestScenarioParsingException("Failed to parse test scenario", e);
        }
    }

    /**
     * Validates a test scenario for completeness and clarity.
     * 
     * @param scenario the test scenario to validate
     * @return validation result
     */
    public ScenarioValidationResult validateScenario(String scenario) {
        try {
            if (scenario == null || scenario.trim().isEmpty()) {
                return new ScenarioValidationResult(false, "Scenario cannot be empty");
            }
            
            if (scenario.length() < 20) {
                return new ScenarioValidationResult(false, "Scenario too short, provide more details");
            }
            
            if (scenario.length() > 5000) {
                return new ScenarioValidationResult(false, "Scenario too long, please break it down");
            }
            
            // Check for key elements that should be present in a good test scenario
            String lowerScenario = scenario.toLowerCase();
            boolean hasAction = lowerScenario.contains("when") || lowerScenario.contains("given") || 
                               lowerScenario.contains("should") || lowerScenario.contains("test");
            boolean hasExpectation = lowerScenario.contains("then") || lowerScenario.contains("expect") || 
                                   lowerScenario.contains("verify") || lowerScenario.contains("validate");
            
            if (!hasAction) {
                return new ScenarioValidationResult(false, "Scenario should describe what action to perform");
            }
            
            if (!hasExpectation) {
                return new ScenarioValidationResult(false, "Scenario should describe expected outcomes");
            }
            
            return new ScenarioValidationResult(true, "Scenario appears valid");
            
        } catch (Exception e) {
            logger.error("Error validating scenario: {}", e.getMessage(), e);
            return new ScenarioValidationResult(false, "Validation failed: " + e.getMessage());
        }
    }

    /**
     * Represents a parsed test execution plan.
     */
    public static class TestExecutionPlan {
        private String testId;
        private String scenario;
        private String businessFlow;
        private java.util.List<TestStep> steps;
        private java.util.List<Assertion> assertions;
        private Map<String, Object> testData;

        // Constructors
        public TestExecutionPlan() {}

        public TestExecutionPlan(String testId, String scenario, String businessFlow, 
                               java.util.List<TestStep> steps, java.util.List<Assertion> assertions, 
                               Map<String, Object> testData) {
            this.testId = testId;
            this.scenario = scenario;
            this.businessFlow = businessFlow;
            this.steps = steps;
            this.assertions = assertions;
            this.testData = testData;
        }

        // Getters and setters
        public String getTestId() { return testId; }
        public void setTestId(String testId) { this.testId = testId; }

        public String getScenario() { return scenario; }
        public void setScenario(String scenario) { this.scenario = scenario; }

        public String getBusinessFlow() { return businessFlow; }
        public void setBusinessFlow(String businessFlow) { this.businessFlow = businessFlow; }

        public java.util.List<TestStep> getSteps() { return steps; }
        public void setSteps(java.util.List<TestStep> steps) { this.steps = steps; }

        public java.util.List<Assertion> getAssertions() { return assertions; }
        public void setAssertions(java.util.List<Assertion> assertions) { this.assertions = assertions; }

        public Map<String, Object> getTestData() { return testData; }
        public void setTestData(Map<String, Object> testData) { this.testData = testData; }
    }

    /**
     * Represents a test step in the execution plan.
     */
    public static class TestStep {
        private String stepId;
        private StepType type;
        private String description;
        private String targetService;
        private Map<String, Object> inputData;
        private java.util.List<String> expectedOutcomes;
        private String timeout;
        private java.util.List<String> dependencies;

        // Constructors
        public TestStep() {}

        // Getters and setters
        public String getStepId() { return stepId; }
        public void setStepId(String stepId) { this.stepId = stepId; }

        public StepType getType() { return type; }
        public void setType(StepType type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTargetService() { return targetService; }
        public void setTargetService(String targetService) { this.targetService = targetService; }

        public Map<String, Object> getInputData() { return inputData; }
        public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }

        public java.util.List<String> getExpectedOutcomes() { return expectedOutcomes; }
        public void setExpectedOutcomes(java.util.List<String> expectedOutcomes) { this.expectedOutcomes = expectedOutcomes; }

        public String getTimeout() { return timeout; }
        public void setTimeout(String timeout) { this.timeout = timeout; }

        public java.util.List<String> getDependencies() { return dependencies; }
        public void setDependencies(java.util.List<String> dependencies) { this.dependencies = dependencies; }
    }

    /**
     * Represents an assertion in the test plan.
     */
    public static class Assertion {
        private AssertionType type;
        private String description;
        private String condition;
        private String expectedValue;

        // Constructors
        public Assertion() {}

        // Getters and setters
        public AssertionType getType() { return type; }
        public void setType(AssertionType type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public String getExpectedValue() { return expectedValue; }
        public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    }

    /**
     * Represents the result of scenario validation.
     */
    public static class ScenarioValidationResult {
        private final boolean valid;
        private final String message;

        public ScenarioValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    /**
     * Enumeration of test step types.
     */
    public enum StepType {
        KAFKA_EVENT, REST_CALL, DATABASE_CHECK, ASSERTION
    }

    /**
     * Enumeration of assertion types.
     */
    public enum AssertionType {
        BUSINESS, TECHNICAL, DATA
    }

    /**
     * Exception thrown when test scenario parsing fails.
     */
    public static class TestScenarioParsingException extends RuntimeException {
        public TestScenarioParsingException(String message) {
            super(message);
        }

        public TestScenarioParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}