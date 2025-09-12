package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of assertion categories for test scenario parsing.
 */
public enum AssertionCategory {
    BUSINESS("business"),
    TECHNICAL("technical"),
    DATA("data");
    
    private final String value;
    
    AssertionCategory(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static AssertionCategory fromValue(String value) {
        for (AssertionCategory category : AssertionCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown assertion category: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}