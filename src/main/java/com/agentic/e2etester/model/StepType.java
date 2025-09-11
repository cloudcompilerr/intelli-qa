package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of different types of test steps that can be executed.
 */
public enum StepType {
    KAFKA_EVENT("kafka_event"),
    REST_CALL("rest_call"),
    DATABASE_CHECK("database_check"),
    ASSERTION("assertion"),
    WAIT("wait"),
    SETUP("setup"),
    CLEANUP("cleanup");
    
    private final String value;
    
    StepType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static StepType fromValue(String value) {
        for (StepType stepType : StepType.values()) {
            if (stepType.value.equals(value)) {
                return stepType;
            }
        }
        throw new IllegalArgumentException("Unknown step type: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}