package com.agentic.e2etester.recovery;

/**
 * Enumeration of circuit breaker states
 */
public enum CircuitBreakerState {
    CLOSED("Circuit is closed, allowing requests"),
    OPEN("Circuit is open, blocking requests"),
    HALF_OPEN("Circuit is half-open, testing recovery");

    private final String description;

    CircuitBreakerState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}