package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.model.TestStatus;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for the test execution engine that orchestrates the execution
 * of test steps across microservices with async capabilities.
 */
public interface TestExecutionEngine {
    
    /**
     * Executes a complete test execution plan asynchronously.
     *
     * @param plan the test execution plan to execute
     * @return CompletableFuture containing the test result
     */
    CompletableFuture<TestResult> executeTest(TestExecutionPlan plan);
    
    /**
     * Gets the current execution status of a running test.
     *
     * @param testId the ID of the test to check
     * @return the current test status
     */
    TestStatus getExecutionStatus(String testId);
    
    /**
     * Pauses the execution of a running test.
     *
     * @param testId the ID of the test to pause
     */
    void pauseExecution(String testId);
    
    /**
     * Resumes the execution of a paused test.
     *
     * @param testId the ID of the test to resume
     */
    void resumeExecution(String testId);
    
    /**
     * Cancels the execution of a running test.
     *
     * @param testId the ID of the test to cancel
     */
    void cancelExecution(String testId);
}