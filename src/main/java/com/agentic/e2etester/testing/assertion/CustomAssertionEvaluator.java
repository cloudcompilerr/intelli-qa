package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionResult;
import com.agentic.e2etester.model.AssertionRule;
import com.agentic.e2etester.model.TestContext;

/**
 * Interface for custom assertion evaluators that can be registered with the assertion engine.
 * Allows for extensible assertion logic beyond the built-in assertion types.
 */
public interface CustomAssertionEvaluator {
    
    /**
     * Gets the name of this custom evaluator.
     *
     * @return the evaluator name
     */
    String getEvaluatorName();
    
    /**
     * Checks if this evaluator can handle the given assertion rule.
     *
     * @param rule the assertion rule to check
     * @return true if this evaluator can handle the rule, false otherwise
     */
    boolean canEvaluate(AssertionRule rule);
    
    /**
     * Evaluates the assertion rule against the test context.
     *
     * @param rule the assertion rule to evaluate
     * @param context the test execution context
     * @return the result of the assertion evaluation
     */
    AssertionResult evaluate(AssertionRule rule, TestContext context);
    
    /**
     * Gets the priority of this evaluator when multiple evaluators can handle the same rule.
     * Higher values indicate higher priority.
     *
     * @return the evaluator priority
     */
    default int getPriority() {
        return 0;
    }
}