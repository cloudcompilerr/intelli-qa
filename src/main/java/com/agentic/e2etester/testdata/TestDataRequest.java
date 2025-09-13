package com.agentic.e2etester.testdata;

import java.util.Map;
import java.util.List;

/**
 * Request object for test data generation
 */
public class TestDataRequest {
    private String testScenario;
    private Map<String, Object> parameters;
    private List<String> requiredDataTypes;
    private TestDataIsolationLevel isolationLevel;
    private boolean generateRealisticData;
    private Map<String, String> constraints;
    
    public TestDataRequest() {}
    
    public TestDataRequest(String testScenario, Map<String, Object> parameters) {
        this.testScenario = testScenario;
        this.parameters = parameters;
        this.isolationLevel = TestDataIsolationLevel.TEST_LEVEL;
        this.generateRealisticData = true;
    }
    
    // Getters and setters
    public String getTestScenario() {
        return testScenario;
    }
    
    public void setTestScenario(String testScenario) {
        this.testScenario = testScenario;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public List<String> getRequiredDataTypes() {
        return requiredDataTypes;
    }
    
    public void setRequiredDataTypes(List<String> requiredDataTypes) {
        this.requiredDataTypes = requiredDataTypes;
    }
    
    public TestDataIsolationLevel getIsolationLevel() {
        return isolationLevel;
    }
    
    public void setIsolationLevel(TestDataIsolationLevel isolationLevel) {
        this.isolationLevel = isolationLevel;
    }
    
    public boolean isGenerateRealisticData() {
        return generateRealisticData;
    }
    
    public void setGenerateRealisticData(boolean generateRealisticData) {
        this.generateRealisticData = generateRealisticData;
    }
    
    public Map<String, String> getConstraints() {
        return constraints;
    }
    
    public void setConstraints(Map<String, String> constraints) {
        this.constraints = constraints;
    }
}