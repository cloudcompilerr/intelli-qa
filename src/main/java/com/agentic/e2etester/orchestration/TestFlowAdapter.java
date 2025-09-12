package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.service.TestMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapts test flows dynamically based on system responses and AI analysis
 */
@Component
public class TestFlowAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(TestFlowAdapter.class);
    
    private final LLMService llmService;
    private final TestMemoryService testMemoryService;
    
    public TestFlowAdapter(LLMService llmService, TestMemoryService testMemoryService) {
        this.llmService = llmService;
        this.testMemoryService = testMemoryService;
    }
    
    /**
     * Adapts the test flow based on current orchestration context
     */
    public TestExecutionPlan adaptFlow(OrchestrationContext context) {
        logger.info("Analyzing flow adaptation for orchestration {}", context.getOrchestrationId());
        
        try {
            // Analyze current situation
            FlowAdaptationAnalysis analysis = analyzeCurrentFlow(context);
            
            if (!analysis.isAdaptationNeeded()) {
                logger.debug("No flow adaptation needed for orchestration {}", context.getOrchestrationId());
                return null;
            }
            
            // Generate adapted plan
            TestExecutionPlan adaptedPlan = generateAdaptedPlan(context, analysis);
            
            if (adaptedPlan != null) {
                logger.info("Generated adapted flow for orchestration {} with {} steps", 
                           context.getOrchestrationId(), adaptedPlan.getSteps().size());
            }
            
            return adaptedPlan;
            
        } catch (Exception e) {
            logger.error("Failed to adapt flow for orchestration {}", context.getOrchestrationId(), e);
            return null;
        }
    }
    
    /**
     * Analyzes the current flow to determine if adaptation is needed
     */
    private FlowAdaptationAnalysis analyzeCurrentFlow(OrchestrationContext context) {
        FlowAdaptationAnalysis analysis = new FlowAdaptationAnalysis();
        analysis.setOrchestrationId(context.getOrchestrationId());
        
        // Check for failure patterns
        if (hasRepeatedFailures(context)) {
            analysis.setAdaptationNeeded(true);
            analysis.setReason("Repeated failures detected");
            analysis.setAdaptationType(AdaptationType.RETRY_WITH_MODIFICATION);
        }
        
        // Check for performance issues
        if (hasPerformanceIssues(context)) {
            analysis.setAdaptationNeeded(true);
            analysis.setReason("Performance degradation detected");
            analysis.setAdaptationType(AdaptationType.OPTIMIZE_FLOW);
        }
        
        // Check for service unavailability
        if (hasServiceUnavailability(context)) {
            analysis.setAdaptationNeeded(true);
            analysis.setReason("Service unavailability detected");
            analysis.setAdaptationType(AdaptationType.SKIP_UNAVAILABLE_SERVICES);
        }
        
        // Use AI to analyze complex scenarios
        if (!analysis.isAdaptationNeeded()) {
            analysis = analyzeWithAI(context);
        }
        
        return analysis;
    }
    
    /**
     * Generates an adapted test execution plan
     */
    private TestExecutionPlan generateAdaptedPlan(OrchestrationContext context, FlowAdaptationAnalysis analysis) {
        TestExecutionPlan originalPlan = context.getPlan();
        TestExecutionPlan adaptedPlan = new TestExecutionPlan();
        
        // Copy basic properties
        adaptedPlan.setTestId(originalPlan.getTestId() + "-adapted");
        adaptedPlan.setScenario(originalPlan.getScenario() + " (Adapted)");
        adaptedPlan.setTestData(originalPlan.getTestData());
        adaptedPlan.setConfiguration(originalPlan.getConfiguration());
        
        // Adapt steps based on analysis
        List<TestStep> adaptedSteps = adaptSteps(originalPlan.getSteps(), analysis);
        adaptedPlan.setSteps(adaptedSteps);
        
        // Adapt assertions if needed
        List<AssertionRule> adaptedAssertions = adaptAssertions(originalPlan.getAssertions(), analysis);
        adaptedPlan.setAssertions(adaptedAssertions);
        
        return adaptedPlan;
    }
    
    /**
     * Adapts test steps based on the analysis
     */
    private List<TestStep> adaptSteps(List<TestStep> originalSteps, FlowAdaptationAnalysis analysis) {
        List<TestStep> adaptedSteps = new ArrayList<>();
        
        for (TestStep step : originalSteps) {
            TestStep adaptedStep = adaptStep(step, analysis);
            if (adaptedStep != null) {
                adaptedSteps.add(adaptedStep);
            }
        }
        
        // Add additional steps if needed
        if (analysis.getAdaptationType() == AdaptationType.ADD_VALIDATION_STEPS) {
            adaptedSteps.addAll(generateValidationSteps(analysis));
        }
        
        return adaptedSteps;
    }
    
    /**
     * Adapts a single test step
     */
    private TestStep adaptStep(TestStep originalStep, FlowAdaptationAnalysis analysis) {
        TestStep adaptedStep = new TestStep();
        adaptedStep.setStepId(originalStep.getStepId() + "-adapted");
        adaptedStep.setType(originalStep.getType());
        adaptedStep.setTargetService(originalStep.getTargetService());
        adaptedStep.setInputData(originalStep.getInputData());
        adaptedStep.setExpectedOutcomes(originalStep.getExpectedOutcomes());
        
        // Adapt based on analysis type
        switch (analysis.getAdaptationType()) {
            case RETRY_WITH_MODIFICATION:
                // Increase timeout and retry attempts
                adaptedStep.setTimeout(originalStep.getTimeout().multipliedBy(2));
                RetryPolicy newRetryPolicy = new RetryPolicy();
                newRetryPolicy.setMaxAttempts(originalStep.getRetryPolicy().getMaxAttempts() + 2);
                newRetryPolicy.setBackoffMultiplier(1.5);
                adaptedStep.setRetryPolicy(newRetryPolicy);
                break;
                
            case OPTIMIZE_FLOW:
                // Reduce timeout for faster execution
                adaptedStep.setTimeout(originalStep.getTimeout().dividedBy(2));
                break;
                
            case SKIP_UNAVAILABLE_SERVICES:
                // Skip steps for unavailable services
                if (isServiceUnavailable(originalStep.getTargetService())) {
                    return null; // Skip this step
                }
                break;
                
            default:
                // Keep original step configuration
                adaptedStep.setTimeout(originalStep.getTimeout());
                adaptedStep.setRetryPolicy(originalStep.getRetryPolicy());
        }
        
        return adaptedStep;
    }
    
    /**
     * Adapts assertion rules based on analysis
     */
    private List<AssertionRule> adaptAssertions(List<AssertionRule> originalAssertions, FlowAdaptationAnalysis analysis) {
        List<AssertionRule> adaptedAssertions = new ArrayList<>();
        
        for (AssertionRule assertion : originalAssertions) {
            AssertionRule adaptedAssertion = new AssertionRule();
            adaptedAssertion.setRuleId(assertion.getRuleId() + "-adapted");
            adaptedAssertion.setType(assertion.getType());
            // AssertionRule doesn't have category field, so we skip this
            adaptedAssertion.setDescription(assertion.getDescription());
            adaptedAssertion.setExpectedValue(assertion.getExpectedValue());
            
            // Adapt severity based on analysis
            if (analysis.getAdaptationType() == AdaptationType.RELAX_ASSERTIONS) {
                // Reduce severity for non-critical assertions
                AssertionSeverity currentSeverity = assertion.getSeverity();
                if (currentSeverity == AssertionSeverity.CRITICAL) {
                    adaptedAssertion.setSeverity(AssertionSeverity.ERROR);
                } else if (currentSeverity == AssertionSeverity.ERROR) {
                    adaptedAssertion.setSeverity(AssertionSeverity.WARNING);
                } else {
                    adaptedAssertion.setSeverity(currentSeverity);
                }
            } else {
                adaptedAssertion.setSeverity(assertion.getSeverity());
            }
            
            adaptedAssertions.add(adaptedAssertion);
        }
        
        return adaptedAssertions;
    }
    
    /**
     * Generates additional validation steps
     */
    private List<TestStep> generateValidationSteps(FlowAdaptationAnalysis analysis) {
        List<TestStep> validationSteps = new ArrayList<>();
        
        // Add health check step
        TestStep healthCheckStep = new TestStep();
        healthCheckStep.setStepId("health-check-validation");
        healthCheckStep.setType(StepType.REST_CALL);
        healthCheckStep.setTargetService("health-check");
        healthCheckStep.setTimeout(Duration.ofSeconds(30));
        
        validationSteps.add(healthCheckStep);
        
        return validationSteps;
    }
    
    /**
     * Uses AI to analyze complex adaptation scenarios
     */
    private FlowAdaptationAnalysis analyzeWithAI(OrchestrationContext context) {
        try {
            String prompt = String.format(
                "Analyze the test orchestration context and determine if flow adaptation is needed. " +
                "Orchestration ID: %s, Status: %s, Plan: %s steps. " +
                "Should we adapt the flow? If yes, what type of adaptation?",
                context.getOrchestrationId(),
                context.getStatus(),
                context.getPlan().getSteps().size()
            );
            
            LLMService.LLMResponse llmResponse = llmService.sendPrompt(prompt);
            if (!llmResponse.isSuccess()) {
                throw new RuntimeException("LLM service failed: " + llmResponse.getErrorMessage());
            }
            String response = llmResponse.getContent();
            
            FlowAdaptationAnalysis analysis = new FlowAdaptationAnalysis();
            analysis.setOrchestrationId(context.getOrchestrationId());
            
            if (response.toLowerCase().contains("adapt") || response.toLowerCase().contains("yes")) {
                analysis.setAdaptationNeeded(true);
                analysis.setReason("AI analysis suggests adaptation");
                analysis.setAdaptationType(determineAdaptationTypeFromResponse(response));
            } else {
                analysis.setAdaptationNeeded(false);
                analysis.setReason("AI analysis suggests no adaptation needed");
            }
            
            return analysis;
            
        } catch (Exception e) {
            logger.warn("Failed to analyze with AI", e);
            FlowAdaptationAnalysis analysis = new FlowAdaptationAnalysis();
            analysis.setAdaptationNeeded(false);
            analysis.setReason("AI analysis failed");
            return analysis;
        }
    }
    
    private AdaptationType determineAdaptationTypeFromResponse(String response) {
        String lowerResponse = response.toLowerCase();
        
        if (lowerResponse.contains("retry")) {
            return AdaptationType.RETRY_WITH_MODIFICATION;
        } else if (lowerResponse.contains("optimize") || lowerResponse.contains("performance")) {
            return AdaptationType.OPTIMIZE_FLOW;
        } else if (lowerResponse.contains("skip") || lowerResponse.contains("unavailable")) {
            return AdaptationType.SKIP_UNAVAILABLE_SERVICES;
        } else if (lowerResponse.contains("validation")) {
            return AdaptationType.ADD_VALIDATION_STEPS;
        } else {
            return AdaptationType.RETRY_WITH_MODIFICATION; // Default
        }
    }
    
    private boolean hasRepeatedFailures(OrchestrationContext context) {
        // In a real implementation, this would check execution history
        return false;
    }
    
    private boolean hasPerformanceIssues(OrchestrationContext context) {
        // In a real implementation, this would check performance metrics
        return false;
    }
    
    private boolean hasServiceUnavailability(OrchestrationContext context) {
        // In a real implementation, this would check service health
        return false;
    }
    
    private boolean isServiceUnavailable(String serviceName) {
        // In a real implementation, this would check service health
        return false;
    }
    
    /**
     * Internal class for flow adaptation analysis
     */
    private static class FlowAdaptationAnalysis {
        private String orchestrationId;
        private boolean adaptationNeeded;
        private String reason;
        private AdaptationType adaptationType;
        
        public String getOrchestrationId() {
            return orchestrationId;
        }
        
        public void setOrchestrationId(String orchestrationId) {
            this.orchestrationId = orchestrationId;
        }
        
        public boolean isAdaptationNeeded() {
            return adaptationNeeded;
        }
        
        public void setAdaptationNeeded(boolean adaptationNeeded) {
            this.adaptationNeeded = adaptationNeeded;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public AdaptationType getAdaptationType() {
            return adaptationType;
        }
        
        public void setAdaptationType(AdaptationType adaptationType) {
            this.adaptationType = adaptationType;
        }
    }
    
    /**
     * Types of flow adaptations
     */
    private enum AdaptationType {
        RETRY_WITH_MODIFICATION,
        OPTIMIZE_FLOW,
        SKIP_UNAVAILABLE_SERVICES,
        ADD_VALIDATION_STEPS,
        RELAX_ASSERTIONS
    }
}