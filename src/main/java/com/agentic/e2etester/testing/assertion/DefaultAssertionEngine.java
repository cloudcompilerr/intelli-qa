package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of the AssertionEngine that provides comprehensive assertion evaluation
 * for business, technical, and data validations.
 */
@Component
public class DefaultAssertionEngine implements AssertionEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultAssertionEngine.class);
    
    private final ObjectMapper objectMapper;
    private final Map<String, CustomAssertionEvaluator> customEvaluators;
    
    public DefaultAssertionEngine() {
        this.objectMapper = new ObjectMapper();
        this.customEvaluators = new ConcurrentHashMap<>();
    }
    
    @Override
    public AssertionResult evaluateAssertion(AssertionRule rule, TestContext context) {
        if (rule == null) {
            return createFailedResult("INVALID_RULE", "Assertion rule cannot be null", null, null);
        }
        
        if (!rule.getEnabled()) {
            return createSkippedResult(rule.getRuleId(), "Assertion rule is disabled");
        }
        
        try {
            // Check for custom evaluators first
            for (CustomAssertionEvaluator evaluator : getSortedCustomEvaluators()) {
                if (evaluator.canEvaluate(rule)) {
                    logger.debug("Using custom evaluator {} for rule {}", evaluator.getEvaluatorName(), rule.getRuleId());
                    return evaluator.evaluate(rule, context);
                }
            }
            
            // Use built-in evaluation logic
            return evaluateBuiltInAssertion(rule, context);
            
        } catch (Exception e) {
            logger.error("Error evaluating assertion rule {}: {}", rule.getRuleId(), e.getMessage(), e);
            return createFailedResult(rule.getRuleId(), "Assertion evaluation failed: " + e.getMessage(), null, null);
        }
    }
    
    @Override
    public List<AssertionResult> evaluateAssertions(List<AssertionRule> rules, TestContext context) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        
        return rules.stream()
                .map(rule -> evaluateAssertion(rule, context))
                .collect(Collectors.toList());
    }
    
    @Override
    public AssertionResult validateBusinessOutcome(BusinessAssertion assertion, TestContext context) {
        try {
            logger.debug("Validating business assertion: {}", assertion.getRuleId());
            
            // Validate business rule against context
            String businessRule = assertion.getBusinessRule();
            Map<String, Object> businessContext = assertion.getBusinessContext();
            
            // Extract business metrics from test context
            Object businessOutcome = extractBusinessOutcome(context, assertion);
            
            // Apply business rule validation
            boolean passed = evaluateBusinessRule(businessRule, businessOutcome, businessContext);
            
            AssertionResult result = new AssertionResult(assertion.getRuleId(), passed);
            result.setActualValue(businessOutcome);
            result.setExpectedValue(assertion.getExpectedValue());
            result.setSeverity(assertion.getSeverity());
            result.setStepId(context.getCurrentStepId());
            
            if (passed) {
                result.setMessage("Business outcome validation passed: " + assertion.getDescription());
            } else {
                result.setErrorMessage("Business outcome validation failed: " + assertion.getDescription());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating business assertion {}: {}", assertion.getRuleId(), e.getMessage(), e);
            return createFailedResult(assertion.getRuleId(), "Business validation failed: " + e.getMessage(), null, null);
        }
    }
    
    @Override
    public AssertionResult validateTechnicalMetrics(TechnicalAssertion assertion, TestContext context) {
        try {
            logger.debug("Validating technical assertion: {}", assertion.getRuleId());
            
            String metricName = assertion.getMetricName();
            Object actualValue = extractTechnicalMetric(context, assertion);
            
            boolean passed = evaluateTechnicalMetric(assertion, actualValue);
            
            AssertionResult result = new AssertionResult(assertion.getRuleId(), passed);
            result.setActualValue(actualValue);
            result.setExpectedValue(assertion.getExpectedValue());
            result.setSeverity(assertion.getSeverity());
            result.setStepId(context.getCurrentStepId());
            
            if (passed) {
                result.setMessage("Technical metric validation passed: " + assertion.getDescription());
            } else {
                result.setErrorMessage("Technical metric validation failed: " + assertion.getDescription());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating technical assertion {}: {}", assertion.getRuleId(), e.getMessage(), e);
            return createFailedResult(assertion.getRuleId(), "Technical validation failed: " + e.getMessage(), null, null);
        }
    }
    
    @Override
    public AssertionResult validateDataConsistency(DataAssertion assertion, TestContext context) {
        try {
            logger.debug("Validating data assertion: {}", assertion.getRuleId());
            
            Object actualData = extractDataValue(context, assertion);
            boolean passed = evaluateDataConsistency(assertion, actualData);
            
            AssertionResult result = new AssertionResult(assertion.getRuleId(), passed);
            result.setActualValue(actualData);
            result.setExpectedValue(assertion.getExpectedValue());
            result.setSeverity(assertion.getSeverity());
            result.setStepId(context.getCurrentStepId());
            
            if (passed) {
                result.setMessage("Data consistency validation passed: " + assertion.getDescription());
            } else {
                result.setErrorMessage("Data consistency validation failed: " + assertion.getDescription());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating data assertion {}: {}", assertion.getRuleId(), e.getMessage(), e);
            return createFailedResult(assertion.getRuleId(), "Data validation failed: " + e.getMessage(), null, null);
        }
    }
    
    @Override
    public void registerCustomEvaluator(CustomAssertionEvaluator evaluator) {
        if (evaluator != null && evaluator.getEvaluatorName() != null) {
            customEvaluators.put(evaluator.getEvaluatorName(), evaluator);
            logger.info("Registered custom assertion evaluator: {}", evaluator.getEvaluatorName());
        }
    }
    
    @Override
    public boolean supportsAssertion(AssertionRule rule) {
        if (rule == null) {
            return false;
        }
        
        // Check custom evaluators
        for (CustomAssertionEvaluator evaluator : customEvaluators.values()) {
            if (evaluator.canEvaluate(rule)) {
                return true;
            }
        }
        
        // Check built-in assertion types
        return rule.getType() != null;
    }
    
    // Private helper methods
    
    private AssertionResult evaluateBuiltInAssertion(AssertionRule rule, TestContext context) {
        AssertionType type = rule.getType();
        if (type == null) {
            return createFailedResult(rule.getRuleId(), "Assertion type is null", null, null);
        }
        
        Object actualValue = extractActualValue(rule, context);
        Object expectedValue = rule.getExpectedValue();
        
        boolean passed = evaluateCondition(type, actualValue, expectedValue, rule);
        
        AssertionResult result = new AssertionResult(rule.getRuleId(), passed);
        result.setActualValue(actualValue);
        result.setExpectedValue(expectedValue);
        result.setSeverity(rule.getSeverity());
        result.setStepId(context.getCurrentStepId());
        
        if (passed) {
            result.setMessage("Assertion passed: " + rule.getDescription());
        } else {
            result.setErrorMessage("Assertion failed: " + rule.getDescription());
        }
        
        return result;
    }
    
    private Object extractActualValue(AssertionRule rule, TestContext context) {
        String actualValuePath = rule.getActualValuePath();
        if (actualValuePath == null || actualValuePath.isEmpty()) {
            return null;
        }
        
        try {
            // Try JSON path extraction first
            if (actualValuePath.startsWith("$.")) {
                return extractJsonPathValue(context, actualValuePath);
            }
            
            // Try context variable extraction
            if (actualValuePath.startsWith("context.")) {
                return extractContextValue(context, actualValuePath.substring(8));
            }
            
            // Try interaction extraction
            if (actualValuePath.startsWith("interaction.")) {
                return extractInteractionValue(context, actualValuePath.substring(12));
            }
            
            // Try metrics extraction
            if (actualValuePath.startsWith("metrics.")) {
                return extractMetricsValue(context, actualValuePath.substring(8));
            }
            
            // Default to variable lookup
            return context.getVariable(actualValuePath);
            
        } catch (Exception e) {
            logger.warn("Failed to extract actual value from path {}: {}", actualValuePath, e.getMessage());
            return null;
        }
    }
    
    private Object extractJsonPathValue(TestContext context, String jsonPath) {
        try {
            // Convert context to JSON and apply JSON path
            String contextJson = objectMapper.writeValueAsString(context);
            return JsonPath.read(contextJson, jsonPath);
        } catch (PathNotFoundException e) {
            logger.debug("JSON path not found: {}", jsonPath);
            return null;
        } catch (Exception e) {
            logger.warn("Error extracting JSON path {}: {}", jsonPath, e.getMessage());
            return null;
        }
    }
    
    private Object extractContextValue(TestContext context, String path) {
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return null;
        }
        
        switch (parts[0]) {
            case "correlationId":
                return context.getCorrelationId();
            case "currentStepId":
                return context.getCurrentStepId();
            case "startTime":
                return context.getStartTime();
            case "endTime":
                return context.getEndTime();
            case "variables":
                return parts.length > 1 ? context.getVariable(parts[1]) : context.getVariables();
            case "executionState":
                return parts.length > 1 ? context.getExecutionState().get(parts[1]) : context.getExecutionState();
            default:
                return null;
        }
    }
    
    private Object extractInteractionValue(TestContext context, String path) {
        List<ServiceInteraction> interactions = context.getInteractions();
        if (interactions == null || interactions.isEmpty()) {
            return null;
        }
        
        // Get the latest interaction by default
        ServiceInteraction interaction = interactions.get(interactions.size() - 1);
        
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return interaction;
        }
        
        switch (parts[0]) {
            case "serviceId":
                return interaction.getServiceId();
            case "type":
                return interaction.getType();
            case "status":
                return interaction.getStatus();
            case "responseTime":
                return interaction.getResponseTime();
            case "request":
                return interaction.getRequest();
            case "response":
                return interaction.getResponse();
            default:
                return null;
        }
    }
    
    private Object extractMetricsValue(TestContext context, String path) {
        TestMetrics metrics = context.getMetrics();
        if (metrics == null) {
            return null;
        }
        
        // This would need to be implemented based on the TestMetrics structure
        // For now, return null as TestMetrics implementation is not provided
        return null;
    }
    
    private boolean evaluateCondition(AssertionType type, Object actualValue, Object expectedValue, AssertionRule rule) {
        switch (type) {
            case EQUALS:
                return Objects.equals(actualValue, expectedValue);
            case NOT_EQUALS:
                return !Objects.equals(actualValue, expectedValue);
            case CONTAINS:
                return actualValue != null && actualValue.toString().contains(expectedValue.toString());
            case NOT_CONTAINS:
                return actualValue == null || !actualValue.toString().contains(expectedValue.toString());
            case GREATER_THAN:
                return compareNumbers(actualValue, expectedValue) > 0;
            case LESS_THAN:
                return compareNumbers(actualValue, expectedValue) < 0;
            case GREATER_THAN_OR_EQUAL:
                return compareNumbers(actualValue, expectedValue) >= 0;
            case LESS_THAN_OR_EQUAL:
                return compareNumbers(actualValue, expectedValue) <= 0;
            case REGEX_MATCH:
                return actualValue != null && Pattern.matches(expectedValue.toString(), actualValue.toString());
            case JSON_PATH:
                return evaluateJsonPath(actualValue, expectedValue, rule);
            case RESPONSE_TIME:
                return evaluateResponseTime(actualValue, expectedValue);
            case STATUS_CODE:
                return evaluateStatusCode(actualValue, expectedValue);
            case CUSTOM:
                return evaluateCustomCondition(actualValue, expectedValue, rule);
            default:
                logger.warn("Unsupported assertion type: {}", type);
                return false;
        }
    }
    
    private int compareNumbers(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return actual == expected ? 0 : (actual == null ? -1 : 1);
        }
        
        try {
            double actualNum = Double.parseDouble(actual.toString());
            double expectedNum = Double.parseDouble(expected.toString());
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            logger.warn("Cannot compare non-numeric values: {} and {}", actual, expected);
            return 0;
        }
    }
    
    private boolean evaluateJsonPath(Object actualValue, Object expectedValue, AssertionRule rule) {
        try {
            String jsonPath = rule.getCondition();
            if (jsonPath == null) {
                return false;
            }
            
            String json = objectMapper.writeValueAsString(actualValue);
            Object pathValue = JsonPath.read(json, jsonPath);
            return Objects.equals(pathValue, expectedValue);
        } catch (Exception e) {
            logger.warn("Error evaluating JSON path condition: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean evaluateResponseTime(Object actualValue, Object expectedValue) {
        try {
            if (actualValue instanceof Duration && expectedValue instanceof Duration) {
                return ((Duration) actualValue).compareTo((Duration) expectedValue) <= 0;
            }
            
            long actualMs = Long.parseLong(actualValue.toString());
            long expectedMs = Long.parseLong(expectedValue.toString());
            return actualMs <= expectedMs;
        } catch (Exception e) {
            logger.warn("Error evaluating response time: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean evaluateStatusCode(Object actualValue, Object expectedValue) {
        try {
            int actualCode = Integer.parseInt(actualValue.toString());
            int expectedCode = Integer.parseInt(expectedValue.toString());
            return actualCode == expectedCode;
        } catch (Exception e) {
            logger.warn("Error evaluating status code: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean evaluateCustomCondition(Object actualValue, Object expectedValue, AssertionRule rule) {
        // Custom condition evaluation based on rule parameters
        String condition = rule.getCondition();
        if (condition == null) {
            return false;
        }
        
        // This is a simplified implementation - in practice, you might want to use
        // a more sophisticated expression evaluator like SpEL
        return condition.equals("true") || Objects.equals(actualValue, expectedValue);
    }
    
    private Object extractBusinessOutcome(TestContext context, BusinessAssertion assertion) {
        // Extract business-specific data from context
        Map<String, Object> businessContext = assertion.getBusinessContext();
        String orderType = assertion.getOrderType();
        String customerSegment = assertion.getCustomerSegment();
        
        // This would typically involve complex business logic
        // For now, return a simplified extraction
        return context.getVariable("businessOutcome");
    }
    
    private boolean evaluateBusinessRule(String businessRule, Object businessOutcome, Map<String, Object> businessContext) {
        // Simplified business rule evaluation
        // In practice, this would involve a business rules engine
        if (businessRule == null || businessOutcome == null) {
            return false;
        }
        
        // Example business rule evaluations
        switch (businessRule.toLowerCase()) {
            case "order_completed":
                return "COMPLETED".equals(businessOutcome.toString());
            case "payment_processed":
                return "PAID".equals(businessOutcome.toString());
            case "inventory_updated":
                return businessContext != null && businessContext.containsKey("inventoryUpdated");
            default:
                return false;
        }
    }
    
    private Object extractTechnicalMetric(TestContext context, TechnicalAssertion assertion) {
        String metricName = assertion.getMetricName();
        String serviceId = assertion.getServiceId();
        
        // Extract technical metrics from interactions
        List<ServiceInteraction> interactions = context.getInteractions();
        if (interactions != null && serviceId != null) {
            return interactions.stream()
                    .filter(i -> serviceId.equals(i.getServiceId()))
                    .findFirst()
                    .map(i -> extractMetricFromInteraction(i, metricName))
                    .orElse(null);
        }
        
        return context.getVariable(metricName);
    }
    
    private Object extractMetricFromInteraction(ServiceInteraction interaction, String metricName) {
        switch (metricName.toLowerCase()) {
            case "response_time":
                return interaction.getResponseTime();
            case "status":
                return interaction.getStatus();
            case "service_id":
                return interaction.getServiceId();
            default:
                return null;
        }
    }
    
    private boolean evaluateTechnicalMetric(TechnicalAssertion assertion, Object actualValue) {
        String metricName = assertion.getMetricName();
        
        switch (metricName.toLowerCase()) {
            case "response_time":
                Duration threshold = assertion.getResponseTimeThreshold();
                if (threshold != null && actualValue instanceof Duration) {
                    return ((Duration) actualValue).compareTo(threshold) <= 0;
                }
                break;
            case "status_code":
                Integer expectedCode = assertion.getExpectedStatusCode();
                if (expectedCode != null && actualValue != null) {
                    return expectedCode.equals(Integer.parseInt(actualValue.toString()));
                }
                break;
            case "performance":
                Double perfThreshold = assertion.getPerformanceThreshold();
                if (perfThreshold != null && actualValue != null) {
                    return Double.parseDouble(actualValue.toString()) <= perfThreshold;
                }
                break;
        }
        
        return Objects.equals(actualValue, assertion.getExpectedValue());
    }
    
    private Object extractDataValue(TestContext context, DataAssertion assertion) {
        String dataSource = assertion.getDataSource();
        String jsonPath = assertion.getJsonPath();
        
        if (jsonPath != null) {
            return extractJsonPathValue(context, jsonPath);
        }
        
        // Extract from variables or execution state
        return context.getVariable(dataSource);
    }
    
    private boolean evaluateDataConsistency(DataAssertion assertion, Object actualData) {
        // Validate required fields
        List<String> requiredFields = assertion.getRequiredFields();
        if (requiredFields != null && !requiredFields.isEmpty()) {
            if (actualData instanceof Map) {
                Map<?, ?> dataMap = (Map<?, ?>) actualData;
                for (String field : requiredFields) {
                    if (!dataMap.containsKey(field)) {
                        return false;
                    }
                }
                // All required fields are present
                return true;
            } else {
                // Required fields specified but data is not a Map
                return false;
            }
        }
        
        // Schema validation
        String schemaValidation = assertion.getSchemaValidation();
        if (schemaValidation != null) {
            // Simplified schema validation - in practice, use JSON Schema validator
            return actualData != null;
        }
        
        return Objects.equals(actualData, assertion.getExpectedValue());
    }
    
    private List<CustomAssertionEvaluator> getSortedCustomEvaluators() {
        return customEvaluators.values().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getPriority(), e1.getPriority()))
                .collect(Collectors.toList());
    }
    
    private AssertionResult createFailedResult(String ruleId, String errorMessage, Object actualValue, Object expectedValue) {
        AssertionResult result = new AssertionResult(ruleId, false);
        result.setErrorMessage(errorMessage);
        result.setActualValue(actualValue);
        result.setExpectedValue(expectedValue);
        result.setSeverity(AssertionSeverity.ERROR);
        return result;
    }
    
    private AssertionResult createSkippedResult(String ruleId, String message) {
        AssertionResult result = new AssertionResult(ruleId, true);
        result.setMessage(message);
        result.setSeverity(AssertionSeverity.INFO);
        return result;
    }
}