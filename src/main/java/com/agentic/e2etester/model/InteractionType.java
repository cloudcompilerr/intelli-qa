package com.agentic.e2etester.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of different types of service interactions.
 */
public enum InteractionType {
    HTTP_REQUEST("http_request"),
    KAFKA_PRODUCE("kafka_produce"),
    KAFKA_CONSUME("kafka_consume"),
    DATABASE_QUERY("database_query"),
    DATABASE_UPDATE("database_update"),
    HEALTH_CHECK("health_check"),
    DISCOVERY("discovery");
    
    private final String value;
    
    InteractionType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static InteractionType fromValue(String value) {
        for (InteractionType type : InteractionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown interaction type: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}