package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Context for system test execution containing test suite information and environment details.
 */
public class SystemTestContext {
    private String testSuiteId;
    private Instant startTime;
    private String environment;
    private Map<String, Object> configuration = new HashMap<>();
    private Map<String, String> environmentVariables = new HashMap<>();
    private String testDataSet;
    private String correlationId;

    // Constructors
    public SystemTestContext() {}

    public SystemTestContext(String testSuiteId, String environment) {
        this.testSuiteId = testSuiteId;
        this.environment = environment;
        this.startTime = Instant.now();
    }

    // Getters and Setters
    public String getTestSuiteId() {
        return testSuiteId;
    }

    public void setTestSuiteId(String testSuiteId) {
        this.testSuiteId = testSuiteId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public void addConfiguration(String key, Object value) {
        this.configuration.put(key, value);
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public void addEnvironmentVariable(String key, String value) {
        this.environmentVariables.put(key, value);
    }

    public String getTestDataSet() {
        return testDataSet;
    }

    public void setTestDataSet(String testDataSet) {
        this.testDataSet = testDataSet;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}