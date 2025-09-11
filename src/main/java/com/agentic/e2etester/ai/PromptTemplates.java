package com.agentic.e2etester.ai;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Collection of prompt templates for test scenario parsing and AI interactions.
 * Provides structured prompts for converting natural language test scenarios
 * into executable test plans for microservices testing.
 */
@Component
public class PromptTemplates {

    /**
     * Template for parsing natural language test scenarios into structured test plans.
     */
    public static final String TEST_SCENARIO_PARSING_TEMPLATE = """
            You are an expert test automation engineer specializing in microservices and event-driven architectures.
            
            Parse the following natural language test scenario into a structured test execution plan.
            
            Test Scenario:
            {scenario}
            
            Context Information:
            - System Type: Event-driven microservices architecture
            - Technologies: Spring Boot, Kafka, Couchbase
            - Services Count: 16-20 microservices
            - Domain: Order fulfillment system
            
            Please provide a structured response in the following JSON format:
            {
              "testId": "unique-test-identifier",
              "scenario": "original scenario text",
              "businessFlow": "high-level business flow description",
              "steps": [
                {
                  "stepId": "step-1",
                  "type": "KAFKA_EVENT|REST_CALL|DATABASE_CHECK|ASSERTION",
                  "description": "what this step does",
                  "targetService": "service name or kafka topic",
                  "inputData": {},
                  "expectedOutcomes": ["list of expected results"],
                  "timeout": "30s",
                  "dependencies": ["previous step ids"]
                }
              ],
              "assertions": [
                {
                  "type": "BUSINESS|TECHNICAL|DATA",
                  "description": "what to validate",
                  "condition": "validation condition",
                  "expectedValue": "expected result"
                }
              ],
              "testData": {
                "customerId": "test-customer-123",
                "orderId": "test-order-456"
              }
            }
            
            Focus on:
            1. Breaking down the scenario into discrete, testable steps
            2. Identifying the correct sequence of microservice interactions
            3. Specifying appropriate Kafka events and REST API calls
            4. Including data validation checkpoints
            5. Defining clear success criteria
            
            Respond only with valid JSON, no additional text.
            """;

    /**
     * Template for analyzing test failures and providing diagnostics.
     */
    public static final String FAILURE_ANALYSIS_TEMPLATE = """
            You are an expert system diagnostician specializing in distributed microservices debugging.
            
            Analyze the following test failure and provide root cause analysis with remediation suggestions.
            
            Failure Information:
            Test ID: {testId}
            Failed Step: {failedStep}
            Error Message: {errorMessage}
            Service: {serviceName}
            Timestamp: {timestamp}
            
            System Context:
            {systemContext}
            
            Service Interactions:
            {serviceInteractions}
            
            Logs:
            {relevantLogs}
            
            Please provide analysis in the following JSON format:
            {
              "rootCause": "primary cause of the failure",
              "failureCategory": "NETWORK|SERVICE|DATA|BUSINESS_LOGIC|INFRASTRUCTURE",
              "affectedServices": ["list of impacted services"],
              "cascadeEffects": ["downstream impacts"],
              "diagnosticSteps": ["steps to verify the diagnosis"],
              "remediationSuggestions": [
                {
                  "action": "what to do",
                  "priority": "HIGH|MEDIUM|LOW",
                  "effort": "time estimate",
                  "description": "detailed explanation"
                }
              ],
              "preventionMeasures": ["how to prevent similar failures"],
              "monitoringRecommendations": ["what to monitor going forward"]
            }
            
            Focus on:
            1. Identifying the primary root cause vs symptoms
            2. Understanding the failure propagation path
            3. Providing actionable remediation steps
            4. Suggesting preventive measures
            
            Respond only with valid JSON, no additional text.
            """;

    /**
     * Template for generating test data based on business scenarios.
     */
    public static final String TEST_DATA_GENERATION_TEMPLATE = """
            You are a test data specialist for e-commerce order fulfillment systems.
            
            Generate realistic test data for the following test scenario:
            
            Scenario: {scenario}
            Data Requirements: {dataRequirements}
            Business Rules: {businessRules}
            
            Generate test data in the following JSON format:
            {
              "customer": {
                "customerId": "unique identifier",
                "email": "test email",
                "membershipLevel": "BRONZE|SILVER|GOLD|PLATINUM",
                "address": {
                  "street": "street address",
                  "city": "city",
                  "state": "state",
                  "zipCode": "zip code",
                  "country": "country"
                }
              },
              "order": {
                "orderId": "unique order identifier",
                "orderDate": "ISO date",
                "items": [
                  {
                    "productId": "product identifier",
                    "name": "product name",
                    "quantity": 1,
                    "price": 99.99,
                    "category": "product category"
                  }
                ],
                "totalAmount": 99.99,
                "currency": "USD",
                "paymentMethod": "CREDIT_CARD|DEBIT_CARD|PAYPAL|BANK_TRANSFER"
              },
              "fulfillment": {
                "warehouseId": "warehouse identifier",
                "shippingMethod": "STANDARD|EXPRESS|OVERNIGHT",
                "carrierCode": "carrier identifier",
                "estimatedDelivery": "ISO date"
              },
              "events": [
                {
                  "eventType": "ORDER_CREATED|PAYMENT_PROCESSED|INVENTORY_RESERVED|etc",
                  "timestamp": "ISO timestamp",
                  "payload": {}
                }
              ]
            }
            
            Ensure data is:
            1. Realistic and consistent
            2. Follows business rules
            3. Includes edge cases when specified
            4. Has proper relationships between entities
            
            Respond only with valid JSON, no additional text.
            """;

    /**
     * Template for validating test execution results.
     */
    public static final String RESULT_VALIDATION_TEMPLATE = """
            You are a test result validator for distributed systems testing.
            
            Validate the following test execution results against expected outcomes:
            
            Test Scenario: {scenario}
            Expected Outcomes: {expectedOutcomes}
            Actual Results: {actualResults}
            Execution Context: {executionContext}
            
            Provide validation in the following JSON format:
            {
              "overallResult": "PASS|FAIL|PARTIAL",
              "validationDetails": [
                {
                  "assertion": "what was being validated",
                  "expected": "expected value/behavior",
                  "actual": "actual value/behavior",
                  "result": "PASS|FAIL",
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "message": "detailed explanation"
                }
              ],
              "businessImpact": "impact on business functionality",
              "technicalIssues": ["list of technical problems found"],
              "recommendations": [
                {
                  "type": "FIX|INVESTIGATE|MONITOR",
                  "description": "what should be done",
                  "priority": "HIGH|MEDIUM|LOW"
                }
              ],
              "summary": "concise summary of validation results"
            }
            
            Focus on:
            1. Comparing expected vs actual outcomes
            2. Identifying critical vs non-critical failures
            3. Assessing business impact
            4. Providing clear next steps
            
            Respond only with valid JSON, no additional text.
            """;

    /**
     * Template for service interaction analysis.
     */
    public static final String SERVICE_INTERACTION_ANALYSIS_TEMPLATE = """
            You are a microservices architecture analyst.
            
            Analyze the following service interaction pattern and identify potential issues:
            
            Service Chain: {serviceChain}
            Interactions: {interactions}
            Performance Metrics: {performanceMetrics}
            Error Patterns: {errorPatterns}
            
            Provide analysis in JSON format:
            {
              "interactionFlow": "description of the service flow",
              "performanceAnalysis": {
                "totalLatency": "end-to-end latency",
                "bottlenecks": ["services causing delays"],
                "throughput": "requests per second capability"
              },
              "reliabilityAssessment": {
                "failurePoints": ["potential failure points"],
                "circuitBreakerStatus": ["services with circuit breakers"],
                "retryPatterns": ["retry mechanisms observed"]
              },
              "recommendations": [
                {
                  "area": "PERFORMANCE|RELIABILITY|SCALABILITY",
                  "suggestion": "specific recommendation",
                  "impact": "expected improvement"
                }
              ]
            }
            
            Respond only with valid JSON, no additional text.
            """;

    /**
     * Gets a prompt template by name with parameter validation.
     * 
     * @param templateName the name of the template
     * @param parameters the parameters to validate
     * @return the template string
     * @throws IllegalArgumentException if template not found or parameters invalid
     */
    public String getTemplate(String templateName, Map<String, Object> parameters) {
        String template = switch (templateName.toUpperCase()) {
            case "TEST_SCENARIO_PARSING" -> TEST_SCENARIO_PARSING_TEMPLATE;
            case "FAILURE_ANALYSIS" -> FAILURE_ANALYSIS_TEMPLATE;
            case "TEST_DATA_GENERATION" -> TEST_DATA_GENERATION_TEMPLATE;
            case "RESULT_VALIDATION" -> RESULT_VALIDATION_TEMPLATE;
            case "SERVICE_INTERACTION_ANALYSIS" -> SERVICE_INTERACTION_ANALYSIS_TEMPLATE;
            default -> throw new IllegalArgumentException("Unknown template: " + templateName);
        };

        validateTemplateParameters(templateName, template, parameters);
        return template;
    }

    /**
     * Validates that all required parameters are present for a template.
     * 
     * @param templateName the template name
     * @param template the template string
     * @param parameters the provided parameters
     * @throws IllegalArgumentException if required parameters are missing
     */
    private void validateTemplateParameters(String templateName, String template, Map<String, Object> parameters) {
        // Extract parameter placeholders from template (only top-level ones, not in JSON examples)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(template);
        
        java.util.Set<String> requiredParams = new java.util.HashSet<>();
        while (matcher.find()) {
            String param = matcher.group(1);
            // Only consider simple parameter names, not JSON content
            if (param.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
                requiredParams.add(param);
            }
        }
        
        // Check if all required parameters are provided
        for (String requiredParam : requiredParams) {
            if (!parameters.containsKey(requiredParam)) {
                throw new IllegalArgumentException(
                    String.format("Missing required parameter '%s' for template '%s'", requiredParam, templateName)
                );
            }
        }
    }
}