package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of possible statuses for service interactions.
 */
public enum InteractionStatus {
    SUCCESS("success"),
    FAILURE("failure"),
    TIMEOUT("timeout"),
    RETRY("retry"),
    SKIPPED("skipped"),
    IN_PROGRESS("in_progress");
    
    private final String value;
    
    InteractionStatus(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static InteractionStatus fromValue(String value) {
        for (InteractionStatus status : InteractionStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown interaction status: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}