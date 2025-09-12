package com.agentic.e2etester.orchestration;

import com.agentic.e2etester.model.TestResult;

import java.time.Duration;

/**
 * Represents the current progress of a test orchestration
 */
public class OrchestrationProgress {
    
    private final String orchestrationId;
    private final int totalSteps;
    private final int completedSteps;
    private final int errorCount;
    private final String currentStep;
    private final double progressPercentage;
    private final Duration elapsedTime;
    private final Duration estimatedRemainingTime;
    private final boolean completed;
    private final TestResult finalResult;
    
    public OrchestrationProgress(String orchestrationId,
                               int totalSteps,
                               int completedSteps,
                               int errorCount,
                               String currentStep,
                               double progressPercentage,
                               Duration elapsedTime,
                               Duration estimatedRemainingTime,
                               boolean completed,
                               TestResult finalResult) {
        this.orchestrationId = orchestrationId;
        this.totalSteps = totalSteps;
        this.completedSteps = completedSteps;
        this.errorCount = errorCount;
        this.currentStep = currentStep;
        this.progressPercentage = progressPercentage;
        this.elapsedTime = elapsedTime;
        this.estimatedRemainingTime = estimatedRemainingTime;
        this.completed = completed;
        this.finalResult = finalResult;
    }
    
    /**
     * Creates a progress object for a not found orchestration
     */
    public static OrchestrationProgress notFound(String orchestrationId) {
        return new OrchestrationProgress(
            orchestrationId, 0, 0, 0, "Not Found", 0.0, 
            Duration.ZERO, Duration.ZERO, false, null
        );
    }
    
    public String getOrchestrationId() {
        return orchestrationId;
    }
    
    public int getTotalSteps() {
        return totalSteps;
    }
    
    public int getCompletedSteps() {
        return completedSteps;
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public String getCurrentStep() {
        return currentStep;
    }
    
    public double getProgressPercentage() {
        return progressPercentage;
    }
    
    public Duration getElapsedTime() {
        return elapsedTime;
    }
    
    public Duration getEstimatedRemainingTime() {
        return estimatedRemainingTime;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public TestResult getFinalResult() {
        return finalResult;
    }
    
    /**
     * Gets a human-readable progress summary
     */
    public String getProgressSummary() {
        if (orchestrationId.equals("Not Found")) {
            return "Orchestration not found";
        }
        
        if (completed) {
            return String.format("Completed: %d/%d steps (%.1f%%) in %s", 
                               completedSteps, totalSteps, progressPercentage, 
                               formatDuration(elapsedTime));
        }
        
        return String.format("In Progress: %d/%d steps (%.1f%%) - %s - ETA: %s", 
                           completedSteps, totalSteps, progressPercentage, 
                           currentStep, formatDuration(estimatedRemainingTime));
    }
    
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "0s";
        }
        
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
    
    @Override
    public String toString() {
        return "OrchestrationProgress{" +
                "orchestrationId='" + orchestrationId + '\'' +
                ", totalSteps=" + totalSteps +
                ", completedSteps=" + completedSteps +
                ", errorCount=" + errorCount +
                ", progressPercentage=" + progressPercentage +
                ", completed=" + completed +
                '}';
    }
}