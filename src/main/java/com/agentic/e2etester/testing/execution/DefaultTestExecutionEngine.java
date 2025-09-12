package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Default implementation of the test execution engine with async orchestration capabilities.
 * Handles test step execution with timeout, retry logic, and correlation tracking.
 */
@Component
public class DefaultTestExecutionEngine implements TestExecutionEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultTestExecutionEngine.class);
    
    private final List<StepExecutor> stepExecutors;
    private final ExecutorService executorService;
    private final Map<String, TestExecutionContext> activeExecutions;
    
    public DefaultTestExecutionEngine(List<StepExecutor> stepExecutors) {
        this.stepExecutors = stepExecutors;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "test-execution-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        this.activeExecutions = new ConcurrentHashMap<>();
    }
    
    @Override
    public CompletableFuture<TestResult> executeTest(TestExecutionPlan plan) {
        String testId = plan.getTestId();
        logger.info("Starting test execution for test ID: {}", testId);
        
        // Create test context with correlation ID
        TestContext testContext = new TestContext(UUID.randomUUID().toString());
        testContext.setTestExecutionPlanId(testId);
        testContext.setStartTime(Instant.now());
        
        // Create execution context
        TestExecutionContext executionContext = new TestExecutionContext(testId, testContext);
        activeExecutions.put(testId, executionContext);
        
        // Create test result
        TestResult testResult = new TestResult(testId, TestStatus.RUNNING);
        testResult.setStartTime(testContext.getStartTime());
        testResult.setCorrelationId(testContext.getCorrelationId());
        testResult.setTotalSteps(plan.getSteps().size());
        
        // Execute test asynchronously
        CompletableFuture<TestResult> resultFuture = CompletableFuture
            .supplyAsync(() -> executeTestSteps(plan, executionContext, testResult), executorService)
            .handle((result, throwable) -> {
                // Clean up execution context
                activeExecutions.remove(testId);
                
                if (throwable != null) {
                    logger.error("Test execution failed for test ID: {}", testId, throwable);
                    result.setStatus(TestStatus.FAILED);
                    result.setErrorMessage(throwable.getMessage());
                    result.setFailureReason("Execution engine error: " + throwable.getClass().getSimpleName());
                }
                
                result.setEndTime(Instant.now());
                testContext.setEndTime(result.getEndTime());
                
                logger.info("Test execution completed for test ID: {} with status: {}", 
                    testId, result.getStatus());
                
                return result;
            });
        
        return resultFuture;
    }
    
    private TestResult executeTestSteps(TestExecutionPlan plan, TestExecutionContext executionContext, TestResult testResult) {
        List<TestStep> steps = plan.getSteps();
        List<StepResult> stepResults = new ArrayList<>();
        TestContext testContext = executionContext.getTestContext();
        
        int successfulSteps = 0;
        int failedSteps = 0;
        
        for (TestStep step : steps) {
            // Check for cancellation or pause requests
            if (executionContext.shouldStop()) {
                logger.info("Test execution cancelled for test ID: {}", plan.getTestId());
                testResult.setStatus(TestStatus.CANCELLED);
                break;
            }
            
            if (executionContext.shouldPause()) {
                handlePauseRequest(executionContext);
            }
            
            // Update current step
            testContext.setCurrentStepId(step.getStepId());
            executionContext.setStatus(TestStatus.RUNNING);
            
            logger.debug("Executing step: {} for test ID: {}", step.getStepId(), plan.getTestId());
            
            try {
                StepResult stepResult = executeStepWithRetry(step, testContext).get();
                stepResults.add(stepResult);
                
                if (stepResult.isSuccessful()) {
                    successfulSteps++;
                } else {
                    failedSteps++;
                    
                    // Check if this is a critical failure that should stop execution
                    if (shouldStopOnFailure(step, plan.getConfiguration())) {
                        logger.warn("Critical step failed, stopping test execution: {}", step.getStepId());
                        testResult.setStatus(TestStatus.FAILED);
                        testResult.setFailureReason("Critical step failure: " + step.getStepId());
                        break;
                    }
                }
                
            } catch (Exception e) {
                logger.error("Failed to execute step: {} for test ID: {}", step.getStepId(), plan.getTestId(), e);
                
                StepResult failedStepResult = new StepResult(step.getStepId(), TestStatus.FAILED);
                failedStepResult.setErrorMessage(e.getMessage());
                failedStepResult.setEndTime(Instant.now());
                stepResults.add(failedStepResult);
                
                failedSteps++;
                
                if (shouldStopOnFailure(step, plan.getConfiguration())) {
                    testResult.setStatus(TestStatus.FAILED);
                    testResult.setFailureReason("Step execution error: " + e.getMessage());
                    break;
                }
            }
        }
        
        // Update final test result
        testResult.setStepResults(stepResults);
        testResult.setSuccessfulSteps(successfulSteps);
        testResult.setFailedSteps(failedSteps);
        
        // Determine final status if not already set
        if (testResult.getStatus() == TestStatus.RUNNING) {
            if (failedSteps == 0) {
                testResult.setStatus(TestStatus.PASSED);
            } else if (successfulSteps > 0) {
                testResult.setStatus(TestStatus.PARTIAL_SUCCESS);
            } else {
                testResult.setStatus(TestStatus.FAILED);
            }
        }
        
        return testResult;
    }
    
    private CompletableFuture<StepResult> executeStepWithRetry(TestStep step, TestContext testContext) {
        RetryPolicy retryPolicy = step.getRetryPolicy();
        int maxAttempts = retryPolicy != null ? retryPolicy.getMaxAttempts() : 1;
        long delayMs = retryPolicy != null ? retryPolicy.getDelayMs() : 0;
        
        return executeStepWithRetryInternal(step, testContext, 1, maxAttempts, delayMs);
    }
    
    private CompletableFuture<StepResult> executeStepWithRetryInternal(TestStep step, TestContext testContext, 
                                                                      int attempt, int maxAttempts, long delayMs) {
        
        StepExecutor executor = findStepExecutor(step);
        if (executor == null) {
            StepResult errorResult = new StepResult(step.getStepId(), TestStatus.FAILED);
            errorResult.setErrorMessage("No executor found for step type: " + step.getType());
            errorResult.setEndTime(Instant.now());
            return CompletableFuture.completedFuture(errorResult);
        }
        
        // Apply timeout to step execution
        CompletableFuture<StepResult> stepFuture = executor.executeStep(step, testContext);
        
        if (step.getTimeoutMs() != null && step.getTimeoutMs() > 0) {
            stepFuture = stepFuture.orTimeout(step.getTimeoutMs(), TimeUnit.MILLISECONDS);
        }
        
        return stepFuture.handle((result, throwable) -> {
            if (throwable != null) {
                logger.warn("Step execution attempt {} failed for step: {}", attempt, step.getStepId(), throwable);
                
                // Check if we should retry
                if (attempt < maxAttempts && shouldRetry(throwable, step.getRetryPolicy())) {
                    logger.info("Retrying step: {} (attempt {} of {})", step.getStepId(), attempt + 1, maxAttempts);
                    
                    // Schedule retry with delay
                    if (delayMs > 0) {
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            StepResult errorResult = new StepResult(step.getStepId(), TestStatus.FAILED);
                            errorResult.setErrorMessage("Retry interrupted: " + e.getMessage());
                            errorResult.setAttemptCount(attempt);
                            errorResult.setEndTime(Instant.now());
                            return errorResult;
                        }
                    }
                    
                    // Recursive retry
                    try {
                        return executeStepWithRetryInternal(step, testContext, attempt + 1, maxAttempts, delayMs).get();
                    } catch (Exception e) {
                        StepResult errorResult = new StepResult(step.getStepId(), TestStatus.FAILED);
                        errorResult.setErrorMessage("Retry execution failed: " + e.getMessage());
                        errorResult.setAttemptCount(attempt);
                        errorResult.setEndTime(Instant.now());
                        return errorResult;
                    }
                } else {
                    // No more retries, return failure
                    StepResult errorResult = new StepResult(step.getStepId(), TestStatus.FAILED);
                    errorResult.setErrorMessage(throwable.getMessage());
                    errorResult.setAttemptCount(attempt);
                    errorResult.setEndTime(Instant.now());
                    return errorResult;
                }
            } else {
                // Success
                result.setAttemptCount(attempt);
                result.setCorrelationId(testContext.getCorrelationId());
                return result;
            }
        });
    }
    
    private StepExecutor findStepExecutor(TestStep step) {
        return stepExecutors.stream()
            .filter(executor -> executor.canExecute(step))
            .findFirst()
            .orElse(null);
    }
    
    private boolean shouldRetry(Throwable throwable, RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            return false;
        }
        
        // Check if the exception type is retryable
        List<String> retryableExceptions = retryPolicy.getRetryableExceptions();
        if (retryableExceptions != null && !retryableExceptions.isEmpty()) {
            String exceptionName = throwable.getClass().getSimpleName();
            return retryableExceptions.contains(exceptionName);
        }
        
        // Default: retry on timeout and connection exceptions
        return throwable instanceof TimeoutException ||
               throwable.getCause() instanceof java.net.ConnectException ||
               throwable.getMessage().toLowerCase().contains("timeout") ||
               throwable.getMessage().toLowerCase().contains("connection");
    }
    
    private boolean shouldStopOnFailure(TestStep step, TestConfiguration configuration) {
        // Check step-level configuration first
        if (step.getRetryPolicy() != null && step.getRetryPolicy().isStopOnFailure()) {
            return true;
        }
        
        // Check global configuration
        return configuration != null && configuration.isStopOnFirstFailure();
    }
    
    private void handlePauseRequest(TestExecutionContext executionContext) {
        logger.info("Pausing test execution for test ID: {}", executionContext.getTestId());
        executionContext.setStatus(TestStatus.PAUSED);
        
        // Wait until pause is cleared or cancellation is requested
        while (executionContext.shouldPause() && !executionContext.shouldStop()) {
            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executionContext.requestCancel();
                break;
            }
        }
        
        if (!executionContext.shouldStop()) {
            logger.info("Resuming test execution for test ID: {}", executionContext.getTestId());
            executionContext.setStatus(TestStatus.RUNNING);
        }
    }
    
    @Override
    public TestStatus getExecutionStatus(String testId) {
        TestExecutionContext context = activeExecutions.get(testId);
        return context != null ? context.getStatus() : TestStatus.NOT_FOUND;
    }
    
    @Override
    public void pauseExecution(String testId) {
        TestExecutionContext context = activeExecutions.get(testId);
        if (context != null) {
            logger.info("Pause requested for test ID: {}", testId);
            context.requestPause();
        } else {
            logger.warn("Cannot pause test ID: {} - execution not found", testId);
        }
    }
    
    @Override
    public void resumeExecution(String testId) {
        TestExecutionContext context = activeExecutions.get(testId);
        if (context != null) {
            logger.info("Resume requested for test ID: {}", testId);
            context.clearPause();
        } else {
            logger.warn("Cannot resume test ID: {} - execution not found", testId);
        }
    }
    
    @Override
    public void cancelExecution(String testId) {
        TestExecutionContext context = activeExecutions.get(testId);
        if (context != null) {
            logger.info("Cancel requested for test ID: {}", testId);
            context.requestCancel();
            context.setStatus(TestStatus.CANCELLED);
            
            // Cancel the execution future if it exists
            CompletableFuture<Void> future = context.getExecutionFuture();
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        } else {
            logger.warn("Cannot cancel test ID: {} - execution not found", testId);
        }
    }
}