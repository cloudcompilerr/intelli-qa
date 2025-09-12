package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.service.CorrelationTrackingService;
import com.agentic.e2etester.service.TestMemoryService;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central orchestrator that coordinates AI agent and execution engine for intelligent test execution.
 * Provides dynamic test flow adaptation and real-time progress tracking.
 */
@Component
public class TestOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(TestOrchestrator.class);
    
    private final TestExecutionEngine executionEngine;
    private final LLMService llmService;
    private final CorrelationTrackingService correlationService;
    private final TestMemoryService testMemoryService;
    private final TestProgressTracker progressTracker;
    private final TestFlowAdapter flowAdapter;
    
    private final Map<String, OrchestrationContext> activeOrchestrations = new ConcurrentHashMap<>();
    
    public TestOrchestrator(TestExecutionEngine executionEngine,
                           LLMService llmService,
                           CorrelationTrackingService correlationService,
                           TestMemoryService testMemoryService,
                           TestProgressTracker progressTracker,
                           TestFlowAdapter flowAdapter) {
        this.executionEngine = executionEngine;
        this.llmService = llmService;
        this.correlationService = correlationService;
        this.testMemoryService = testMemoryService;
        this.progressTracker = progressTracker;
        this.flowAdapter = flowAdapter;
    }
    
    /**
     * Orchestrates the complete test execution with AI-driven coordination
     */
    public CompletableFuture<TestResult> orchestrateTest(TestExecutionPlan plan) {
        String orchestrationId = generateOrchestrationId();
        logger.info("Starting test orchestration {} for plan {}", orchestrationId, plan.getTestId());
        
        OrchestrationContext context = createOrchestrationContext(orchestrationId, plan);
        activeOrchestrations.put(orchestrationId, context);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeOrchestration(context);
            } catch (Exception e) {
                logger.error("Orchestration {} failed", orchestrationId, e);
                context.setStatus(OrchestrationStatus.FAILED);
                context.setError(e);
                return createFailureResult(context, e);
            } finally {
                activeOrchestrations.remove(orchestrationId);
            }
        });
    }
    
    /**
     * Gets real-time orchestration status
     */
    public OrchestrationStatus getOrchestrationStatus(String orchestrationId) {
        OrchestrationContext context = activeOrchestrations.get(orchestrationId);
        return context != null ? context.getStatus() : OrchestrationStatus.NOT_FOUND;
    }
    
    /**
     * Gets detailed orchestration progress
     */
    public OrchestrationProgress getOrchestrationProgress(String orchestrationId) {
        OrchestrationContext context = activeOrchestrations.get(orchestrationId);
        if (context == null) {
            return OrchestrationProgress.notFound(orchestrationId);
        }
        
        return progressTracker.getProgress(context);
    }
    
    /**
     * Pauses an active orchestration
     */
    public boolean pauseOrchestration(String orchestrationId) {
        OrchestrationContext context = activeOrchestrations.get(orchestrationId);
        if (context != null && context.getStatus() == OrchestrationStatus.RUNNING) {
            context.setStatus(OrchestrationStatus.PAUSED);
            logger.info("Orchestration {} paused", orchestrationId);
            return true;
        }
        return false;
    }
    
    /**
     * Resumes a paused orchestration
     */
    public boolean resumeOrchestration(String orchestrationId) {
        OrchestrationContext context = activeOrchestrations.get(orchestrationId);
        if (context != null && context.getStatus() == OrchestrationStatus.PAUSED) {
            context.setStatus(OrchestrationStatus.RUNNING);
            logger.info("Orchestration {} resumed", orchestrationId);
            return true;
        }
        return false;
    }
    
    private TestResult executeOrchestration(OrchestrationContext context) {
        context.setStatus(OrchestrationStatus.RUNNING);
        context.setStartTime(Instant.now());
        
        logger.info("Executing orchestration {} with {} steps", 
                   context.getOrchestrationId(), context.getPlan().getSteps().size());
        
        // Initialize correlation tracking
        String correlationId = correlationService.generateCorrelationId();
        correlationService.startTrace(correlationId, "test-orchestrator");
        context.setCorrelationId(correlationId);
        
        // Store test pattern for learning
        testMemoryService.storeTestPattern(createTestPattern(context.getPlan()));
        
        TestResult result = null;
        
        try {
            // Execute test with dynamic flow adaptation
            result = executeWithAdaptation(context);
            
            // Update progress to completion
            progressTracker.markCompleted(context, result);
            
            // Learn from execution results
            learnFromExecution(context, result);
            
            context.setStatus(result.getStatus() == TestStatus.PASSED ? 
                             OrchestrationStatus.COMPLETED : OrchestrationStatus.FAILED);
            
        } catch (Exception e) {
            logger.error("Orchestration execution failed", e);
            context.setStatus(OrchestrationStatus.FAILED);
            context.setError(e);
            result = createFailureResult(context, e);
        } finally {
            context.setEndTime(Instant.now());
            correlationService.completeTrace(correlationId);
        }
        
        return result;
    }
    
    private TestResult executeWithAdaptation(OrchestrationContext context) {
        TestExecutionPlan currentPlan = context.getPlan();
        
        // Execute initial plan
        CompletableFuture<TestResult> executionFuture = executionEngine.executeTest(currentPlan);
        
        // Monitor execution and adapt as needed
        while (!executionFuture.isDone()) {
            try {
                Thread.sleep(1000); // Check every second
                
                // Check if adaptation is needed
                if (shouldAdaptFlow(context)) {
                    TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
                    if (adaptedPlan != null) {
                        logger.info("Adapting test flow for orchestration {}", context.getOrchestrationId());
                        context.setPlan(adaptedPlan);
                        // Note: In a real implementation, we'd need to coordinate with the execution engine
                        // to apply the adapted plan mid-execution
                    }
                }
                
                // Update progress
                progressTracker.updateProgress(context);
                
                // Check for pause/resume
                if (context.getStatus() == OrchestrationStatus.PAUSED) {
                    waitForResume(context);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        try {
            return executionFuture.get();
        } catch (Exception e) {
            throw new RuntimeException("Test execution failed", e);
        }
    }
    
    private boolean shouldAdaptFlow(OrchestrationContext context) {
        // AI-driven decision making for flow adaptation
        String prompt = String.format(
            "Based on the current test execution context, should we adapt the test flow? " +
            "Current status: %s, Steps completed: %d/%d, Errors: %d",
            context.getStatus(),
            progressTracker.getCompletedSteps(context),
            context.getPlan().getSteps().size(),
            progressTracker.getErrorCount(context)
        );
        
        try {
            LLMService.LLMResponse response = llmService.sendPrompt(prompt);
            return response.isSuccess() && 
                   (response.getContent().toLowerCase().contains("yes") || 
                    response.getContent().toLowerCase().contains("adapt"));
        } catch (Exception e) {
            logger.warn("Failed to get AI decision for flow adaptation", e);
            return false;
        }
    }
    
    private void waitForResume(OrchestrationContext context) throws InterruptedException {
        while (context.getStatus() == OrchestrationStatus.PAUSED) {
            Thread.sleep(500);
        }
    }
    
    private void learnFromExecution(OrchestrationContext context, TestResult result) {
        try {
            // Store execution history for learning
            TestExecutionHistory history = new TestExecutionHistory();
            history.setTestPlanId(context.getPlan().getTestId());
            history.setExecutionId(context.getOrchestrationId());
            history.setExecutionTimeMs(java.time.Duration.between(context.getStartTime(), context.getEndTime()).toMillis());
            history.setStatus(result.getStatus());
            history.setCorrelationId(context.getCorrelationId());
            
            testMemoryService.storeExecutionHistory(history);
            
            // If test failed, analyze patterns for future improvement
            if (result.getStatus() == TestStatus.FAILED) {
                List<TestPattern> similarPatterns = testMemoryService.findSimilarPatterns(
                    createTestContext(context), 10);
                logger.info("Found {} similar failure patterns for learning", similarPatterns.size());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to learn from execution", e);
        }
    }
    
    private OrchestrationContext createOrchestrationContext(String orchestrationId, TestExecutionPlan plan) {
        OrchestrationContext context = new OrchestrationContext();
        context.setOrchestrationId(orchestrationId);
        context.setPlan(plan);
        context.setStatus(OrchestrationStatus.INITIALIZED);
        context.setCreatedTime(Instant.now());
        return context;
    }
    
    private TestPattern createTestPattern(TestExecutionPlan plan) {
        TestPattern pattern = new TestPattern();
        pattern.setPatternId(generatePatternId());
        pattern.setName("Test Pattern for " + plan.getTestId());
        pattern.setType(PatternType.SUCCESS_FLOW);
        pattern.setDescription("Execution pattern with " + plan.getSteps().size() + " steps");
        pattern.setCreatedAt(Instant.now());
        return pattern;
    }
    
    private TestContext createTestContext(OrchestrationContext context) {
        TestContext testContext = new TestContext();
        testContext.setCorrelationId(context.getCorrelationId());
        // TestContext doesn't have setTestId method, so we'll set it in the execution state
        testContext.getExecutionState().put("testId", context.getPlan().getTestId());
        return testContext;
    }
    
    private TestResult createFailureResult(OrchestrationContext context, Exception error) {
        TestResult result = new TestResult();
        result.setTestId(context.getPlan().getTestId());
        result.setStatus(TestStatus.FAILED);
        result.setStartTime(context.getStartTime());
        result.setEndTime(Instant.now());
        result.setErrorMessage(error.getMessage());
        return result;
    }
    
    private String generateOrchestrationId() {
        return "orch-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    private String generatePatternId() {
        return "pattern-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}