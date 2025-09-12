package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import com.agentic.e2etester.service.TestMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TestFlowAdapter
 */
@ExtendWith(MockitoExtension.class)
class TestFlowAdapterTest {
    
    @Mock
    private LLMService llmService;
    
    @Mock
    private TestMemoryService testMemoryService;
    
    private TestFlowAdapter flowAdapter;
    private OrchestrationContext context;
    
    @BeforeEach
    void setUp() {
        flowAdapter = new TestFlowAdapter(llmService, testMemoryService);
        context = createOrchestrationContext();
    }
    
    @Test
    void shouldReturnNullWhenNoAdaptationNeeded() {
        // Given
        LLMService.LLMResponse mockResponse = new LLMService.LLMResponse("No adaptation needed", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(mockResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNull();
    }
    
    @Test
    void shouldAdaptFlowWhenAISuggestsAdaptation() {
        // Given
        LLMService.LLMResponse adaptResponse = new LLMService.LLMResponse("Yes, adapt the flow for better performance and retry with modifications", 100, true);
        when(llmService.sendPrompt(contains("should we adapt"))).thenReturn(adaptResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        assertThat(adaptedPlan.getTestId()).contains("adapted");
        assertThat(adaptedPlan.getScenario()).contains("Adapted");
        assertThat(adaptedPlan.getSteps()).isNotEmpty();
    }
    
    @Test
    void shouldAdaptStepsForRetryWithModification() {
        // Given
        LLMService.LLMResponse retryResponse = new LLMService.LLMResponse("Yes, retry with modifications due to failures", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(retryResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        assertThat(adaptedPlan.getSteps()).hasSize(context.getPlan().getSteps().size());
        
        // Check that timeouts were increased (doubled)
        TestStep originalStep = context.getPlan().getSteps().get(0);
        TestStep adaptedStep = adaptedPlan.getSteps().get(0);
        assertThat(adaptedStep.getTimeout()).isEqualTo(originalStep.getTimeout().multipliedBy(2));
        assertThat(adaptedStep.getRetryPolicy().getMaxAttempts())
            .isEqualTo(originalStep.getRetryPolicy().getMaxAttempts() + 2);
    }
    
    @Test
    void shouldAdaptStepsForOptimization() {
        // Given
        LLMService.LLMResponse optimizeResponse = new LLMService.LLMResponse("Yes, optimize the flow for better performance", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(optimizeResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        
        // Check that timeouts were reduced (halved)
        TestStep originalStep = context.getPlan().getSteps().get(0);
        TestStep adaptedStep = adaptedPlan.getSteps().get(0);
        assertThat(adaptedStep.getTimeout()).isEqualTo(originalStep.getTimeout().dividedBy(2));
    }
    
    @Test
    void shouldSkipUnavailableServices() {
        // Given
        LLMService.LLMResponse skipResponse = new LLMService.LLMResponse("Yes, skip unavailable services to continue testing", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(skipResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        // In this test, no services are actually unavailable, so all steps should remain
        assertThat(adaptedPlan.getSteps()).hasSize(context.getPlan().getSteps().size());
    }
    
    @Test
    void shouldAddValidationSteps() {
        // Given
        LLMService.LLMResponse validationResponse = new LLMService.LLMResponse("Yes, add validation steps to ensure system health", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(validationResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        // Should have original steps plus validation steps
        assertThat(adaptedPlan.getSteps().size()).isGreaterThan(context.getPlan().getSteps().size());
        
        // Check that health check validation step was added
        boolean hasHealthCheck = adaptedPlan.getSteps().stream()
            .anyMatch(step -> step.getStepId().contains("health-check"));
        assertThat(hasHealthCheck).isTrue();
    }
    
    @Test
    void shouldAdaptAssertions() {
        // Given
        LLMService.LLMResponse relaxResponse = new LLMService.LLMResponse("Yes, relax assertions due to system constraints", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(relaxResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        assertThat(adaptedPlan.getAssertions()).isNotEmpty();
        
        // Check that assertion severities were adapted
        AssertionRule originalAssertion = context.getPlan().getAssertions().get(0);
        AssertionRule adaptedAssertion = adaptedPlan.getAssertions().get(0);
        
        if (originalAssertion.getSeverity() == AssertionSeverity.CRITICAL) {
            assertThat(adaptedAssertion.getSeverity()).isEqualTo(AssertionSeverity.ERROR);
        }
    }
    
    @Test
    void shouldHandleLLMServiceFailure() {
        // Given
        when(llmService.sendPrompt(anyString()))
            .thenThrow(new RuntimeException("LLM service unavailable"));
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNull(); // Should handle failure gracefully
    }
    
    @Test
    void shouldPreserveOriginalPlanProperties() {
        // Given
        LLMService.LLMResponse adaptResponse = new LLMService.LLMResponse("Yes, adapt the flow", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(adaptResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        assertThat(adaptedPlan.getTestData()).isEqualTo(context.getPlan().getTestData());
        assertThat(adaptedPlan.getConfiguration()).isEqualTo(context.getPlan().getConfiguration());
    }
    
    @Test
    void shouldGenerateUniqueAdaptedIds() {
        // Given
        LLMService.LLMResponse adaptResponse = new LLMService.LLMResponse("Yes, adapt the flow", 100, true);
        when(llmService.sendPrompt(anyString())).thenReturn(adaptResponse);
        
        // When
        TestExecutionPlan adaptedPlan = flowAdapter.adaptFlow(context);
        
        // Then
        assertThat(adaptedPlan).isNotNull();
        assertThat(adaptedPlan.getTestId()).isNotEqualTo(context.getPlan().getTestId());
        assertThat(adaptedPlan.getTestId()).contains("adapted");
        
        // Check that step IDs are also adapted
        for (int i = 0; i < adaptedPlan.getSteps().size(); i++) {
            TestStep originalStep = context.getPlan().getSteps().get(i);
            TestStep adaptedStep = adaptedPlan.getSteps().get(i);
            assertThat(adaptedStep.getStepId()).isNotEqualTo(originalStep.getStepId());
            assertThat(adaptedStep.getStepId()).contains("adapted");
        }
    }
    
    private OrchestrationContext createOrchestrationContext() {
        OrchestrationContext context = new OrchestrationContext();
        context.setOrchestrationId("orch-123");
        context.setStatus(OrchestrationStatus.RUNNING);
        context.setStartTime(Instant.now().minusSeconds(30));
        
        TestExecutionPlan plan = createTestExecutionPlan();
        context.setPlan(plan);
        
        return context;
    }
    
    private TestExecutionPlan createTestExecutionPlan() {
        TestExecutionPlan plan = new TestExecutionPlan();
        plan.setTestId("test-123");
        plan.setScenario("Original test scenario");
        plan.setTestData(new HashMap<>());
        plan.setConfiguration(new TestConfiguration());
        
        // Create test steps
        TestStep step1 = createTestStep("step-1", "service-1");
        TestStep step2 = createTestStep("step-2", "service-2");
        plan.setSteps(Arrays.asList(step1, step2));
        
        // Create assertions
        AssertionRule assertion = createAssertionRule();
        plan.setAssertions(Arrays.asList(assertion));
        
        return plan;
    }
    
    private TestStep createTestStep(String stepId, String targetService) {
        TestStep step = new TestStep();
        step.setStepId(stepId);
        step.setType(StepType.REST_CALL);
        step.setTargetService(targetService);
        step.setTimeout(Duration.ofSeconds(30));
        step.setInputData(new HashMap<>());
        
        RetryPolicy retryPolicy = new RetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryPolicy.setBackoffMultiplier(1.0);
        step.setRetryPolicy(retryPolicy);
        
        return step;
    }
    
    private AssertionRule createAssertionRule() {
        AssertionRule rule = new AssertionRule();
        rule.setRuleId("rule-1");
        rule.setType(AssertionType.EQUALS);
        rule.setSeverity(AssertionSeverity.CRITICAL);
        rule.setDescription("Test assertion");
        rule.setExpectedValue("expected");
        return rule;
    }
}