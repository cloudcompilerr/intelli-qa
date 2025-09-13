package com.agentic.e2etester.recovery;

import java.time.Instant;
import java.util.List;

/**
 * Result of a rollback operation
 */
public class RollbackResult {
    
    private final String testId;
    private final boolean successful;
    private final List<RollbackActionResult> actionResults;
    private final Instant timestamp;
    
    public RollbackResult(String testId, boolean successful, List<RollbackActionResult> actionResults) {
        this.testId = testId;
        this.successful = successful;
        this.actionResults = actionResults;
        this.timestamp = Instant.now();
    }
    
    public String getTestId() { return testId; }
    public boolean isSuccessful() { return successful; }
    public List<RollbackActionResult> getActionResults() { return actionResults; }
    public Instant getTimestamp() { return timestamp; }
    
    public int getSuccessfulActionCount() {
        return (int) actionResults.stream().filter(RollbackActionResult::isSuccessful).count();
    }
    
    public int getFailedActionCount() {
        return actionResults.size() - getSuccessfulActionCount();
    }
    
    public List<RollbackActionResult> getFailedActions() {
        return actionResults.stream().filter(r -> !r.isSuccessful()).toList();
    }
}