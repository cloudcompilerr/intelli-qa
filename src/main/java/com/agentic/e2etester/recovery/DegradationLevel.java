package com.agentic.e2etester.recovery;

/**
 * Levels of graceful degradation
 */
public enum DegradationLevel {
    NONE("No degradation - full functionality"),
    MINIMAL("Minimal degradation - non-critical features disabled"),
    MODERATE("Moderate degradation - reduced functionality"),
    SEVERE("Severe degradation - core functionality only"),
    CRITICAL("Critical degradation - emergency mode");

    private final String description;

    DegradationLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    
    public boolean isMoreSevereThan(DegradationLevel other) {
        return this.ordinal() > other.ordinal();
    }
    
    public boolean isLessSevereThan(DegradationLevel other) {
        return this.ordinal() < other.ordinal();
    }
}