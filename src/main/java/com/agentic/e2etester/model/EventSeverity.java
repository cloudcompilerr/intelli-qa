package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of event severity levels.
 */
public enum EventSeverity {
    DEBUG("debug"),
    INFO("info"),
    WARNING("warning"),
    ERROR("error"),
    CRITICAL("critical");
    
    private final String value;
    
    EventSeverity(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static EventSeverity fromValue(String value) {
        for (EventSeverity severity : EventSeverity.values()) {
            if (severity.value.equals(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown event severity: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}