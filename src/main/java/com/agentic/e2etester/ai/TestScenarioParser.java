package com.agentic.e2etester.ai;

import com.agentic.e2etester.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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
    public TestExecutionPlan parse(String scenario) {
        return parseScenario(scenario);
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
            
            // Validate scenario first
            ScenarioValidationResult validation = validateScenario(scenario);
            if (!validation.isValid()) {
                throw new TestScenarioParsingException("Invalid scenario: " + validation.getMessage());
            }
            
            // Prepare template parameters
            Map<String, Object> parameters = Map.of("scenario", scenario);
            
            // Get the template and send to LLM
            String template = promptTemplates.getTemplate("TEST_SCENARIO_PARSING", parameters);
            LLMService.LLMResponse response = llmService.sendTemplatedPrompt(template, parameters);
            
            if (!response.isSuccess()) {
                throw new TestScenarioParsingException("LLM failed to parse scenario: " + response.getErrorMessage());
            }
            
            // Parse JSON response into TestExecutionPlan
            TestExecutionPlan plan = parseJsonToTestExecutionPlan(response.getContent());
            
            // Validate the parsed plan
            ValidationResult planValidation = validateTestExecutionPlan(plan);
            if (!planValidation.isValid()) {
                throw new TestScenarioParsingException("Generated test plan is invalid: " + planValidation.getMessage());
            }
            
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
     * Parses JSON response from LLM into a TestExecutionPlan using proper model classes.
     * 
     * @param jsonContent the JSON content from LLM
     * @return parsed TestExecutionPlan
     * @throws Exception if parsing fails
     */
    private TestExecutionPlan parseJsonToTestExecutionPlan(String jsonContent) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        
        // Extract basic fields
        String testId = rootNode.path("testId").asText();
        String scenario = rootNode.path("scenario").asText();
        
        // Parse test data
        Map<String, Object> testData = new HashMap<>();
        if (rootNode.has("testData")) {
            testData = objectMapper.convertValue(rootNode.get("testData"), new TypeReference<Map<String, Object>>() {});
        }
        
        // Parse steps
        List<TestStep> steps = new ArrayList<>();
        if (rootNode.has("steps")) {
            JsonNode stepsNode = rootNode.get("steps");
            for (int i = 0; i < stepsNode.size(); i++) {
                JsonNode stepNode = stepsNode.get(i);
                TestStep step = parseTestStep(stepNode, i + 1);
                steps.add(step);
            }
        }
        
        // Create test configuration
        TestConfiguration configuration = createDefaultTestConfiguration();
        
        // Parse assertions
        List<AssertionRule> assertions = new ArrayList<>();
        if (rootNode.has("assertions")) {
            JsonNode assertionsNode = rootNode.get("assertions");
            for (JsonNode assertionNode : assertionsNode) {
                AssertionRule assertion = parseAssertionRule(assertionNode);
                assertions.add(assertion);
            }
        }
        
        // Create and populate the test execution plan
        TestExecutionPlan plan = new TestExecutionPlan(testId, scenario, steps, configuration);
        plan.setTestData(testData);
        plan.setAssertions(assertions);
        
        return plan;
    }

    /**
     * Parses a JSON node into a TestStep.
     */
    private TestStep parseTestStep(JsonNode stepNode, int order) {
        String stepId = stepNode.path("stepId").asText("step-" + order);
        String typeStr = stepNode.path("type").asText();
        StepType type = StepType.fromValue(typeStr.toLowerCase());
        String targetService = stepNode.path("targetService").asText();
        String description = stepNode.path("description").asText();
        
        // Parse timeout
        String timeoutStr = stepNode.path("timeout").asText("30s");
        Long timeoutMs = parseTimeoutToMillis(timeoutStr);
        
        TestStep step = new TestStep(stepId, type, targetService, timeoutMs);
        step.setDescription(description);
        step.setOrder(order);
        
        // Parse input data
        if (stepNode.has("inputData")) {
            Map<String, Object> inputData = objectMapper.convertValue(
                stepNode.get("inputData"), new TypeReference<Map<String, Object>>() {});
            step.setInputData(inputData);
        }
        
        // Parse expected outcomes
        if (stepNode.has("expectedOutcomes")) {
            List<ExpectedOutcome> expectedOutcomes = new ArrayList<>();
            JsonNode outcomesNode = stepNode.get("expectedOutcomes");
            for (JsonNode outcomeNode : outcomesNode) {
                ExpectedOutcome outcome = new ExpectedOutcome();
                outcome.setType(OutcomeType.SUCCESS_RESPONSE); // Default type
                outcome.setDescription(outcomeNode.asText());
                expectedOutcomes.add(outcome);
            }
            step.setExpectedOutcomes(expectedOutcomes);
        }
        
        // Parse dependencies
        if (stepNode.has("dependencies")) {
            List<String> dependencies = new ArrayList<>();
            JsonNode depsNode = stepNode.get("dependencies");
            for (JsonNode depNode : depsNode) {
                dependencies.add(depNode.asText());
            }
            step.setDependsOn(dependencies);
        }
        
        return step;
    }

    /**
     * Parses a JSON node into an AssertionRule.
     */
    private AssertionRule parseAssertionRule(JsonNode assertionNode) {
        String categoryStr = assertionNode.path("type").asText();
        String description = assertionNode.path("description").asText();
        String condition = assertionNode.path("condition").asText();
        String expectedValue = assertionNode.path("expectedValue").asText();
        
        // Map assertion category to appropriate AssertionType
        AssertionType type = mapCategoryToAssertionType(categoryStr, condition);
        
        AssertionRule rule = new AssertionRule();
        rule.setType(type);
        rule.setDescription(description);
        rule.setCondition(condition);
        rule.setExpectedValue(expectedValue);
        rule.setSeverity(AssertionSeverity.CRITICAL); // Default severity
        
        return rule;
    }

    /**
     * Maps assertion category to appropriate AssertionType based on condition.
     */
    private AssertionType mapCategoryToAssertionType(String category, String condition) {
        // Analyze the condition to determine the best AssertionType
        String lowerCondition = condition.toLowerCase();
        
        if (lowerCondition.contains("==") || lowerCondition.contains("equals")) {
            return AssertionType.EQUALS;
        } else if (lowerCondition.contains("!=") || lowerCondition.contains("not equals")) {
            return AssertionType.NOT_EQUALS;
        } else if (lowerCondition.contains("contains")) {
            return AssertionType.CONTAINS;
        } else if (lowerCondition.contains(">") && lowerCondition.contains("=")) {
            return AssertionType.GREATER_THAN_OR_EQUAL;
        } else if (lowerCondition.contains("<") && lowerCondition.contains("=")) {
            return AssertionType.LESS_THAN_OR_EQUAL;
        } else if (lowerCondition.contains(">")) {
            return AssertionType.GREATER_THAN;
        } else if (lowerCondition.contains("<")) {
            return AssertionType.LESS_THAN;
        } else if (lowerCondition.contains("response_time") || lowerCondition.contains("time")) {
            return AssertionType.RESPONSE_TIME;
        } else if (lowerCondition.contains("status") || lowerCondition.contains("code")) {
            return AssertionType.STATUS_CODE;
        } else if (lowerCondition.contains("regex") || lowerCondition.contains("match")) {
            return AssertionType.REGEX_MATCH;
        } else {
            return AssertionType.CUSTOM; // Default for complex conditions
        }
    }

    /**
     * Creates a default test configuration.
     */
    private TestConfiguration createDefaultTestConfiguration() {
        TestConfiguration config = new TestConfiguration();
        config.setMaxRetries(3);
        config.setDefaultTimeoutMs(30000L);
        config.setParallelExecution(false);
        config.setFailFast(true);
        return config;
    }

    /**
     * Parses timeout string (e.g., "30s", "2m") to milliseconds.
     */
    private Long parseTimeoutToMillis(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.trim().isEmpty()) {
            return 30000L; // Default 30 seconds
        }
        
        timeoutStr = timeoutStr.trim().toLowerCase();
        
        try {
            if (timeoutStr.endsWith("ms")) {
                return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 2));
            } else if (timeoutStr.endsWith("s")) {
                return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)) * 1000L;
            } else if (timeoutStr.endsWith("m")) {
                return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)) * 60000L;
            } else {
                // Assume seconds if no unit
                return Long.parseLong(timeoutStr) * 1000L;
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid timeout format: {}, using default 30s", timeoutStr);
            return 30000L;
        }
    }

    /**
     * Validates a parsed test execution plan.
     */
    private ValidationResult validateTestExecutionPlan(TestExecutionPlan plan) {
        List<String> errors = new ArrayList<>();
        
        if (plan.getTestId() == null || plan.getTestId().trim().isEmpty()) {
            errors.add("Test ID cannot be empty");
        }
        
        if (plan.getScenario() == null || plan.getScenario().trim().isEmpty()) {
            errors.add("Scenario cannot be empty");
        }
        
        if (plan.getSteps() == null || plan.getSteps().isEmpty()) {
            errors.add("Test plan must have at least one step");
        } else {
            // Validate each step
            for (int i = 0; i < plan.getSteps().size(); i++) {
                TestStep step = plan.getSteps().get(i);
                List<String> stepErrors = validateTestStep(step, i + 1);
                errors.addAll(stepErrors);
            }
        }
        
        if (plan.getConfiguration() == null) {
            errors.add("Test configuration cannot be null");
        }
        
        boolean isValid = errors.isEmpty();
        String message = isValid ? "Test execution plan is valid" : String.join("; ", errors);
        
        return new ValidationResult(isValid, message);
    }

    /**
     * Validates a test step.
     */
    private List<String> validateTestStep(TestStep step, int stepNumber) {
        List<String> errors = new ArrayList<>();
        
        if (step.getStepId() == null || step.getStepId().trim().isEmpty()) {
            errors.add("Step " + stepNumber + ": Step ID cannot be empty");
        }
        
        if (step.getType() == null) {
            errors.add("Step " + stepNumber + ": Step type cannot be null");
        }
        
        if (step.getTargetService() == null || step.getTargetService().trim().isEmpty()) {
            errors.add("Step " + stepNumber + ": Target service cannot be empty");
        }
        
        if (step.getTimeoutMs() == null || step.getTimeoutMs() <= 0) {
            errors.add("Step " + stepNumber + ": Timeout must be positive");
        }
        
        return errors;
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
     * Represents the result of validation operations.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
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