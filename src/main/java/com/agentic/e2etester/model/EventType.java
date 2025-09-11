package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of different types of test events.
 */
public enum EventType {
    TEST_STARTED("test_started"),
    TEST_COMPLETED("test_completed"),
    STEP_STARTED("step_started"),
    STEP_COMPLETED("step_completed"),
    STEP_FAILED("step_failed"),
    ASSERTION_PASSED("assertion_passed"),
    ASSERTION_FAILED("assertion_failed"),
    SERVICE_CALL("service_call"),
    ERROR_OCCURRED("error_occurred"),
    WARNING("warning"),
    INFO("info");
    
    private final String value;
    
    EventType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static EventType fromValue(String value) {
        for (EventType type : EventType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}