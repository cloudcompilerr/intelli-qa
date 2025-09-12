package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.model.TestStep;
import com.agentic.e2etester.model.StepResult;
import com.agentic.e2etester.model.TestContext;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for executing individual test steps.
 * Implementations handle specific step types (REST, Kafka, Database, etc.).
 */
public interface StepExecutor {
    
    /**
     * Executes a single test step asynchronously.
     *
     * @param step the test step to execute
     * @param context the test context for correlation and state tracking
     * @return CompletableFuture containing the step result
     */
    CompletableFuture<StepResult> executeStep(TestStep step, TestContext context);
    
    /**
     * Checks if this executor can handle the given step type.
     *
     * @param step the test step to check
     * @return true if this executor can handle the step
     */
    boolean canExecute(TestStep step);
}