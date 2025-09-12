package com.agentic.e2etester.model;

/**
 * Enumeration of different types of test failures
 */
public enum FailureType {
    NETWORK_FAILURE("Network connectivity or communication failure"),
    SERVICE_FAILURE("Service unavailable or returning errors"),
    DATA_FAILURE("Data validation or consistency failure"),
    BUSINESS_LOGIC_FAILURE("Business rule or assertion failure"),
    INFRASTRUCTURE_FAILURE("Infrastructure component failure"),
    TIMEOUT_FAILURE("Operation timeout or performance failure"),
    AUTHENTICATION_FAILURE("Authentication or authorization failure"),
    CONFIGURATION_FAILURE("Configuration or setup failure"),
    UNKNOWN_FAILURE("Unknown or unclassified failure");

    private final String description;

    FailureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}