package com.agentic.e2etester.model;

/**
 * Enumeration of failure severity levels
 */
public enum FailureSeverity {
    CRITICAL("Critical failure that blocks entire test execution"),
    HIGH("High severity failure affecting major functionality"),
    MEDIUM("Medium severity failure with workaround available"),
    LOW("Low severity failure with minimal impact"),
    INFO("Informational issue that doesn't affect functionality");

    private final String description;

    FailureSeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}