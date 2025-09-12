package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Step executor for assertion validations.
 */
@Component
public class AssertionStepExecutor implements StepExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(AssertionStepExecutor.class);
    
    @Override
    public CompletableFuture<StepResult> executeStep(TestStep step, TestContext context) {
        logger.debug("Executing assertion step: {}", step.getStepId());
        
        StepResult stepResult = new StepResult(step.getStepId(), TestStatus.RUNNING);
        stepResult.setStartTime(Instant.now());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Extract assertion details
                String assertionType = (String) step.getInputData().get("assertionType");
                Object expectedValue = step.getInputData().get("expectedValue");
                Object actualValue = step.getInputData().get("actualValue");
                
                boolean assertionPassed = performAssertion(assertionType, expectedValue, actualValue, context);
                
                stepResult.setStatus(assertionPassed ? TestStatus.PASSED : TestStatus.FAILED);
                stepResult.setOutput(String.format("Assertion %s: expected=%s, actual=%s", 
                    assertionPassed ? "PASSED" : "FAILED", expectedValue, actualValue));
                stepResult.setEndTime(Instant.now());
                
                return stepResult;
                
            } catch (Exception e) {
                logger.error("Assertion step failed: {}", step.getStepId(), e);
                stepResult.setStatus(TestStatus.FAILED);
                stepResult.setErrorMessage(e.getMessage());
                stepResult.setEndTime(Instant.now());
                return stepResult;
            }
        });
    }
    
    private boolean performAssertion(String assertionType, Object expectedValue, Object actualValue, TestContext context) {
        switch (assertionType.toUpperCase()) {
            case "EQUALS":
                return Objects.equals(expectedValue, actualValue);
            case "NOT_NULL":
                return actualValue != null;
            case "CONTAINS":
                return actualValue != null && actualValue.toString().contains(expectedValue.toString());
            case "GREATER_THAN":
                return compareNumbers(actualValue, expectedValue) > 0;
            case "LESS_THAN":
                return compareNumbers(actualValue, expectedValue) < 0;
            default:
                throw new IllegalArgumentException("Unsupported assertion type: " + assertionType);
        }
    }
    
    private int compareNumbers(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            double actualNum = ((Number) actual).doubleValue();
            double expectedNum = ((Number) expected).doubleValue();
            return Double.compare(actualNum, expectedNum);
        }
        throw new IllegalArgumentException("Cannot compare non-numeric values");
    }
    
    @Override
    public boolean canExecute(TestStep step) {
        return step.getType() == StepType.ASSERTION;
    }
}