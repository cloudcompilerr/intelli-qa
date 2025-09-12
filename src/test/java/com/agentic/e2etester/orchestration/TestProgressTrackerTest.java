package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TestProgressTracker
 */
class TestProgressTrackerTest {
    
    private TestProgressTracker progressTracker;
    private OrchestrationContext context;
    
    @BeforeEach
    void setUp() {
        progressTracker = new TestProgressTracker();
        context = createOrchestrationContext();
    }
    
    @Test
    void shouldInitializeProgressState() {
        // When
        progressTracker.updateProgress(context);
        OrchestrationProgress progress = progressTracker.getProgress(context);
        
        // Then
        assertThat(progress).isNotNull();
        assertThat(progress.getOrchestrationId()).isEqualTo(context.getOrchestrationId());
        assertThat(progress.getTotalSteps()).isEqualTo(3);
        assertThat(progress.getCompletedSteps()).isGreaterThanOrEqualTo(0);
        assertThat(progress.getProgressPercentage()).isGreaterThanOrEqualTo(0.0);
        assertThat(progress.isCompleted()).isFalse();
    }
    
    @Test
    void shouldUpdateProgressOverTime() throws InterruptedException {
        // Given
        progressTracker.updateProgress(context);
        OrchestrationProgress initialProgress = progressTracker.getProgress(context);
        
        // When - wait and update again
        Thread.sleep(1000);
        progressTracker.updateProgress(context);
        OrchestrationProgress updatedProgress = progressTracker.getProgress(context);
        
        // Then
        assertThat(updatedProgress.getElapsedTime()).isGreaterThan(initialProgress.getElapsedTime());
        assertThat(updatedProgress.getCompletedSteps()).isGreaterThanOrEqualTo(initialProgress.getCompletedSteps());
    }
    
    @Test
    void shouldMarkOrchestrationAsCompleted() {
        // Given
        progressTracker.updateProgress(context);
        TestResult result = createSuccessfulResult();
        
        // When
        progressTracker.markCompleted(context, result);
        OrchestrationProgress progress = progressTracker.getProgress(context);
        
        // Then
        assertThat(progress.isCompleted()).isTrue();
        assertThat(progress.getCompletedSteps()).isEqualTo(progress.getTotalSteps());
        assertThat(progress.getProgressPercentage()).isEqualTo(100.0);
        assertThat(progress.getFinalResult()).isEqualTo(result);
    }
    
    @Test
    void shouldReturnNotFoundForInvalidOrchestrationId() {
        // When
        OrchestrationProgress progress = progressTracker.getProgress(createInvalidContext());
        
        // Then
        assertThat(progress.getOrchestrationId()).isEqualTo("invalid-id");
        assertThat(progress.getCurrentStep()).isEqualTo("Not Found");
        assertThat(progress.getTotalSteps()).isEqualTo(0);
    }
    
    @Test
    void shouldTrackCompletedSteps() {
        // Given
        progressTracker.updateProgress(context);
        
        // When
        int completedSteps = progressTracker.getCompletedSteps(context);
        
        // Then
        assertThat(completedSteps).isGreaterThanOrEqualTo(0);
        assertThat(completedSteps).isLessThanOrEqualTo(context.getPlan().getSteps().size());
    }
    
    @Test
    void shouldTrackErrorCount() {
        // Given
        progressTracker.updateProgress(context);
        
        // When
        int errorCount = progressTracker.getErrorCount(context);
        
        // Then
        assertThat(errorCount).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void shouldCleanupProgressTracking() {
        // Given
        progressTracker.updateProgress(context);
        OrchestrationProgress initialProgress = progressTracker.getProgress(context);
        assertThat(initialProgress.getOrchestrationId()).isEqualTo(context.getOrchestrationId());
        
        // When
        progressTracker.cleanup(context.getOrchestrationId());
        OrchestrationProgress afterCleanup = progressTracker.getProgress(context);
        
        // Then
        assertThat(afterCleanup.getCurrentStep()).isEqualTo("Not Found");
    }
    
    @Test
    void shouldCalculateProgressPercentage() {
        // Given
        progressTracker.updateProgress(context);
        
        // When
        OrchestrationProgress progress = progressTracker.getProgress(context);
        
        // Then
        assertThat(progress.getProgressPercentage()).isBetween(0.0, 100.0);
    }
    
    @Test
    void shouldEstimateRemainingTime() throws InterruptedException {
        // Given
        progressTracker.updateProgress(context);
        Thread.sleep(1000); // Let some time pass
        progressTracker.updateProgress(context);
        
        // When
        OrchestrationProgress progress = progressTracker.getProgress(context);
        
        // Then
        assertThat(progress.getEstimatedRemainingTime()).isNotNull();
        assertThat(progress.getEstimatedRemainingTime()).isGreaterThanOrEqualTo(Duration.ZERO);
    }
    
    @Test
    void shouldProvideProgressSummary() {
        // Given
        progressTracker.updateProgress(context);
        
        // When
        OrchestrationProgress progress = progressTracker.getProgress(context);
        String summary = progress.getProgressSummary();
        
        // Then
        assertThat(summary).isNotNull();
        assertThat(summary).contains("In Progress");
        assertThat(summary).contains(context.getOrchestrationId());
    }
    
    @Test
    void shouldProvideCompletedSummary() {
        // Given
        progressTracker.updateProgress(context);
        TestResult result = createSuccessfulResult();
        progressTracker.markCompleted(context, result);
        
        // When
        OrchestrationProgress progress = progressTracker.getProgress(context);
        String summary = progress.getProgressSummary();
        
        // Then
        assertThat(summary).isNotNull();
        assertThat(summary).contains("Completed");
        assertThat(summary).contains("100.0%");
    }
    
    private OrchestrationContext createOrchestrationContext() {
        OrchestrationContext context = new OrchestrationContext();
        context.setOrchestrationId("orch-123");
        context.setStatus(OrchestrationStatus.RUNNING);
        context.setStartTime(Instant.now().minusSeconds(30));
        
        TestExecutionPlan plan = new TestExecutionPlan();
        plan.setTestId("test-123");
        
        // Create 3 test steps
        TestStep step1 = createTestStep("step-1");
        TestStep step2 = createTestStep("step-2");
        TestStep step3 = createTestStep("step-3");
        plan.setSteps(Arrays.asList(step1, step2, step3));
        
        context.setPlan(plan);
        return context;
    }
    
    private OrchestrationContext createInvalidContext() {
        OrchestrationContext context = new OrchestrationContext();
        context.setOrchestrationId("invalid-id");
        return context;
    }
    
    private TestStep createTestStep(String stepId) {
        TestStep step = new TestStep();
        step.setStepId(stepId);
        step.setType(StepType.REST_CALL);
        step.setTargetService("test-service");
        step.setTimeout(Duration.ofSeconds(30));
        step.setInputData(new HashMap<>());
        return step;
    }
    
    private TestResult createSuccessfulResult() {
        TestResult result = new TestResult();
        result.setTestId("test-123");
        result.setStatus(TestStatus.PASSED);
        result.setStartTime(Instant.now().minusSeconds(60));
        result.setEndTime(Instant.now());
        return result;
    }
}