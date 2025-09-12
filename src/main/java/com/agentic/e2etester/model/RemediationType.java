package com.agentic.e2etester.model;

/**
 * Enumeration of different types of remediation actions
 */
public enum RemediationType {
    CONFIGURATION_CHANGE("Change configuration settings"),
    SERVICE_RESTART("Restart affected service"),
    NETWORK_FIX("Fix network connectivity issue"),
    DATA_REPAIR("Repair or restore data"),
    CODE_FIX("Fix application code"),
    INFRASTRUCTURE_SCALING("Scale infrastructure resources"),
    DEPENDENCY_UPDATE("Update dependencies or versions"),
    MONITORING_ALERT("Set up monitoring or alerting"),
    DOCUMENTATION_UPDATE("Update documentation"),
    MANUAL_INTERVENTION("Requires manual intervention");

    private final String description;

    RemediationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}