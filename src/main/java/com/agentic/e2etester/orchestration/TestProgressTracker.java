package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.model.TestStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks real-time progress of test orchestration
 */
@Component
public class TestProgressTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(TestProgressTracker.class);
    
    private final Map<String, ProgressState> progressStates = new ConcurrentHashMap<>();
    
    /**
     * Updates progress for an orchestration context
     */
    public void updateProgress(OrchestrationContext context) {
        String orchestrationId = context.getOrchestrationId();
        ProgressState state = progressStates.computeIfAbsent(orchestrationId, 
                                                            k -> new ProgressState(context));
        
        // Update current step and timing
        state.updateProgress();
        
        logger.debug("Progress updated for orchestration {}: {}/{} steps completed", 
                    orchestrationId, state.getCompletedSteps(), state.getTotalSteps());
    }
    
    /**
     * Marks orchestration as completed
     */
    public void markCompleted(OrchestrationContext context, TestResult result) {
        String orchestrationId = context.getOrchestrationId();
        ProgressState state = progressStates.get(orchestrationId);
        
        if (state != null) {
            state.markCompleted(result);
            logger.info("Orchestration {} completed with status {}", 
                       orchestrationId, result.getStatus());
        }
    }
    
    /**
     * Gets current progress for an orchestration
     */
    public OrchestrationProgress getProgress(OrchestrationContext context) {
        String orchestrationId = context.getOrchestrationId();
        ProgressState state = progressStates.get(orchestrationId);
        
        if (state == null) {
            return OrchestrationProgress.notFound(orchestrationId);
        }
        
        return state.toProgress();
    }
    
    /**
     * Gets number of completed steps
     */
    public int getCompletedSteps(OrchestrationContext context) {
        ProgressState state = progressStates.get(context.getOrchestrationId());
        return state != null ? state.getCompletedSteps() : 0;
    }
    
    /**
     * Gets number of errors encountered
     */
    public int getErrorCount(OrchestrationContext context) {
        ProgressState state = progressStates.get(context.getOrchestrationId());
        return state != null ? state.getErrorCount() : 0;
    }
    
    /**
     * Cleans up progress tracking for completed orchestrations
     */
    public void cleanup(String orchestrationId) {
        progressStates.remove(orchestrationId);
        logger.debug("Cleaned up progress tracking for orchestration {}", orchestrationId);
    }
    
    /**
     * Internal class to maintain progress state
     */
    private static class ProgressState {
        private final String orchestrationId;
        private final int totalSteps;
        private final Instant startTime;
        private int completedSteps;
        private int errorCount;
        private String currentStep;
        private Instant lastUpdateTime;
        private boolean completed;
        private TestResult finalResult;
        
        public ProgressState(OrchestrationContext context) {
            this.orchestrationId = context.getOrchestrationId();
            this.totalSteps = context.getPlan().getSteps().size();
            this.startTime = context.getStartTime() != null ? context.getStartTime() : Instant.now();
            this.completedSteps = 0;
            this.errorCount = 0;
            this.lastUpdateTime = Instant.now();
            this.completed = false;
        }
        
        public void updateProgress() {
            // In a real implementation, this would track actual step execution
            // For now, we simulate progress based on time elapsed
            Duration elapsed = Duration.between(startTime, Instant.now());
            long secondsElapsed = elapsed.getSeconds();
            
            // Simulate progress: assume each step takes ~10 seconds on average
            int estimatedCompleted = Math.min((int)(secondsElapsed / 10), totalSteps);
            
            if (estimatedCompleted > completedSteps) {
                completedSteps = estimatedCompleted;
                currentStep = "Step " + (completedSteps + 1) + " of " + totalSteps;
            }
            
            lastUpdateTime = Instant.now();
        }
        
        public void markCompleted(TestResult result) {
            this.completed = true;
            this.completedSteps = totalSteps;
            this.finalResult = result;
            this.lastUpdateTime = Instant.now();
        }
        
        public OrchestrationProgress toProgress() {
            return new OrchestrationProgress(
                orchestrationId,
                totalSteps,
                completedSteps,
                errorCount,
                currentStep,
                calculateProgressPercentage(),
                Duration.between(startTime, lastUpdateTime),
                estimateRemainingTime(),
                completed,
                finalResult
            );
        }
        
        private double calculateProgressPercentage() {
            if (totalSteps == 0) return 100.0;
            return (double) completedSteps / totalSteps * 100.0;
        }
        
        private Duration estimateRemainingTime() {
            if (completed || completedSteps == 0) {
                return Duration.ZERO;
            }
            
            Duration elapsed = Duration.between(startTime, lastUpdateTime);
            long avgTimePerStep = elapsed.getSeconds() / completedSteps;
            int remainingSteps = totalSteps - completedSteps;
            
            return Duration.ofSeconds(avgTimePerStep * remainingSteps);
        }
        
        public int getCompletedSteps() {
            return completedSteps;
        }
        
        public int getTotalSteps() {
            return totalSteps;
        }
        
        public int getErrorCount() {
            return errorCount;
        }
        
        public void incrementErrorCount() {
            this.errorCount++;
        }
    }
}