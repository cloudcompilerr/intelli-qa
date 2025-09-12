package com.agentic.e2etester.monitoring;

import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.TestMetrics;
import com.agentic.e2etester.model.TestResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for detailed test metrics data collected during test execution.
 */
public class TestMetricsData {
    
    private final String testId;
    private final Instant startTime;
    private Instant endTime;
    private String testType;
    private Duration totalExecutionTime;
    private TestResult finalResult;
    private TestMetrics detailedMetrics;
    private Map<String, Object> initialState = new HashMap<>();
    private List<ServiceInteraction> serviceInteractions = new ArrayList<>();
    
    public TestMetricsData(String testId, Instant startTime) {
        this.testId = testId;
        this.startTime = startTime;
    }
    
    public void addServiceInteraction(ServiceInteraction interaction) {
        serviceInteractions.add(interaction);
    }
    
    // Getters and setters
    public String getTestId() {
        return testId;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public String getTestType() {
        return testType;
    }
    
    public void setTestType(String testType) {
        this.testType = testType;
    }
    
    public Duration getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    public void setTotalExecutionTime(Duration totalExecutionTime) {
        this.totalExecutionTime = totalExecutionTime;
    }
    
    public TestResult getFinalResult() {
        return finalResult;
    }
    
    public void setFinalResult(TestResult finalResult) {
        this.finalResult = finalResult;
    }
    
    public TestMetrics getDetailedMetrics() {
        return detailedMetrics;
    }
    
    public void setDetailedMetrics(TestMetrics detailedMetrics) {
        this.detailedMetrics = detailedMetrics;
    }
    
    public Map<String, Object> getInitialState() {
        return initialState;
    }
    
    public void setInitialState(Map<String, Object> initialState) {
        this.initialState = initialState;
    }
    
    public List<ServiceInteraction> getServiceInteractions() {
        return serviceInteractions;
    }
    
    public void setServiceInteractions(List<ServiceInteraction> serviceInteractions) {
        this.serviceInteractions = serviceInteractions;
    }
}