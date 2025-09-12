package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of possible test execution statuses.
 */
public enum TestStatus {
    PENDING("pending"),
    RUNNING("running"),
    PASSED("passed"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    TIMEOUT("timeout"),
    SKIPPED("skipped"),
    PAUSED("paused"),
    PARTIAL_SUCCESS("partial_success"),
    NOT_FOUND("not_found");
    
    private final String value;
    
    TestStatus(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static TestStatus fromValue(String value) {
        for (TestStatus status : TestStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown test status: " + value);
    }
    
    public boolean isTerminal() {
        return this == PASSED || this == FAILED || this == CANCELLED || this == TIMEOUT || this == SKIPPED || this == PARTIAL_SUCCESS;
    }
    
    public boolean isSuccessful() {
        return this == PASSED || this == PARTIAL_SUCCESS;
    }
    
    @Override
    public String toString() {
        return value;
    }
}