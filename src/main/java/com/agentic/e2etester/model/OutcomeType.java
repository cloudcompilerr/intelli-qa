package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of different types of expected outcomes.
 */
public enum OutcomeType {
    SUCCESS_RESPONSE("success_response"),
    ERROR_RESPONSE("error_response"),
    STATUS_CODE("status_code"),
    RESPONSE_TIME("response_time"),
    DATA_VALIDATION("data_validation"),
    EVENT_PUBLISHED("event_published"),
    DATABASE_CHANGE("database_change"),
    SERVICE_CALL("service_call");
    
    private final String value;
    
    OutcomeType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static OutcomeType fromValue(String value) {
        for (OutcomeType type : OutcomeType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown outcome type: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}