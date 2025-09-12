package com.agentic.e2etester.model;

/**
 * Represents a suggested remediation action for a failure
 */
public class RemediationSuggestion {
    private String suggestionId;
    private String title;
    private String description;
    private RemediationType type;
    private int priority;
    private double confidenceScore;
    private String actionCommand;
    private String documentation;

    public RemediationSuggestion() {}

    public RemediationSuggestion(String suggestionId, String title, String description, 
                               RemediationType type, int priority, double confidenceScore) {
        this.suggestionId = suggestionId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.confidenceScore = confidenceScore;
    }

    // Getters and setters
    public String getSuggestionId() { return suggestionId; }
    public void setSuggestionId(String suggestionId) { this.suggestionId = suggestionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RemediationType getType() { return type; }
    public void setType(RemediationType type) { this.type = type; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getActionCommand() { return actionCommand; }
    public void setActionCommand(String actionCommand) { this.actionCommand = actionCommand; }

    public String getDocumentation() { return documentation; }
    public void setDocumentation(String documentation) { this.documentation = documentation; }
}