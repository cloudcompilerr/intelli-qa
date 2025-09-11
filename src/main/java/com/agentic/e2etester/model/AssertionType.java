package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of different types of assertions that can be performed.
 */
public enum AssertionType {
    EQUALS("equals"),
    NOT_EQUALS("not_equals"),
    CONTAINS("contains"),
    NOT_CONTAINS("not_contains"),
    GREATER_THAN("greater_than"),
    LESS_THAN("less_than"),
    GREATER_THAN_OR_EQUAL("greater_than_or_equal"),
    LESS_THAN_OR_EQUAL("less_than_or_equal"),
    REGEX_MATCH("regex_match"),
    JSON_PATH("json_path"),
    RESPONSE_TIME("response_time"),
    STATUS_CODE("status_code"),
    CUSTOM("custom");
    
    private final String value;
    
    AssertionType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static AssertionType fromValue(String value) {
        for (AssertionType type : AssertionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown assertion type: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}