package com.agentic.e2etester.recovery;

/**
 * Result of a single rollback action
 */
public class RollbackActionResult {
    
    private final String actionId;
    private final String description;
    private final boolean successful;
    private final String errorMessage;
    
    public RollbackActionResult(String actionId, String description, boolean successful, String errorMessage) {
        this.actionId = actionId;
        this.description = description;
        this.successful = successful;
        this.errorMessage = errorMessage;
    }
    
    public String getActionId() { return actionId; }
    public String getDescription() { return description; }
    public boolean isSuccessful() { return successful; }
    public String getErrorMessage() { return errorMessage; }
}