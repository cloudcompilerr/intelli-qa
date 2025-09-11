package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of assertion severity levels.
 */
public enum AssertionSeverity {
    INFO("info"),
    WARNING("warning"),
    ERROR("error"),
    CRITICAL("critical");
    
    private final String value;
    
    AssertionSeverity(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static AssertionSeverity fromValue(String value) {
        for (AssertionSeverity severity : AssertionSeverity.values()) {
            if (severity.value.equals(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown assertion severity: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}