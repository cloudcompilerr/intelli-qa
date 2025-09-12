package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionResult;
import com.agentic.e2etester.model.AssertionRule;
import com.agentic.e2etester.model.TestContext;

import java.util.List;

/**
 * Core interface for the assertion engine that evaluates business, technical, and data validations.
 * Provides comprehensive assertion capabilities for end-to-end testing scenarios.
 */
public interface AssertionEngine {
    
    /**
     * Evaluates a single assertion rule against the test context.
     *
     * @param rule the assertion rule to evaluate
     * @param context the test execution context containing data and state
     * @return the result of the assertion evaluation
     */
    AssertionResult evaluateAssertion(AssertionRule rule, TestContext context);
    
    /**
     * Evaluates multiple assertion rules against the test context.
     *
     * @param rules the list of assertion rules to evaluate
     * @param context the test execution context containing data and state
     * @return the list of assertion results
     */
    List<AssertionResult> evaluateAssertions(List<AssertionRule> rules, TestContext context);
    
    /**
     * Validates business outcome assertions based on business logic and rules.
     *
     * @param assertion the business assertion to validate
     * @param context the test execution context
     * @return the result of the business assertion validation
     */
    AssertionResult validateBusinessOutcome(BusinessAssertion assertion, TestContext context);
    
    /**
     * Validates technical metrics and system behavior assertions.
     *
     * @param assertion the technical assertion to validate
     * @param context the test execution context
     * @return the result of the technical assertion validation
     */
    AssertionResult validateTechnicalMetrics(TechnicalAssertion assertion, TestContext context);
    
    /**
     * Validates data consistency and integrity assertions.
     *
     * @param assertion the data assertion to validate
     * @param context the test execution context
     * @return the result of the data assertion validation
     */
    AssertionResult validateDataConsistency(DataAssertion assertion, TestContext context);
    
    /**
     * Registers a custom assertion evaluator for specific assertion types.
     *
     * @param evaluator the custom assertion evaluator to register
     */
    void registerCustomEvaluator(CustomAssertionEvaluator evaluator);
    
    /**
     * Checks if the assertion engine supports a specific assertion type.
     *
     * @param rule the assertion rule to check
     * @return true if the assertion type is supported, false otherwise
     */
    boolean supportsAssertion(AssertionRule rule);
}