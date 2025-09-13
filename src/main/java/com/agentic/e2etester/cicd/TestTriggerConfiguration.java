package com.agentic.e2etester.cicd;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration for test triggering behavior.
 */
public class TestTriggerConfiguration {
    
    private boolean autoTriggerEnabled;
    private List<DeploymentEventType> triggerEvents;
    private List<String> triggerEnvironments;
    private Duration delayAfterDeployment;
    private Duration testTimeout;
    private int maxRetries;
    private Map<String, String> testScenarioMapping; // environment -> scenario mapping
    private List<String> requiredServices;
    private boolean waitForHealthChecks;
    
    public TestTriggerConfiguration() {
        this.autoTriggerEnabled = true;
        this.maxRetries = 3;
        this.testTimeout = Duration.ofMinutes(30);
        this.delayAfterDeployment = Duration.ofMinutes(2);
        this.waitForHealthChecks = true;
    }
    
    // Getters and setters
    public boolean isAutoTriggerEnabled() { return autoTriggerEnabled; }
    public void setAutoTriggerEnabled(boolean autoTriggerEnabled) { this.autoTriggerEnabled = autoTriggerEnabled; }
    
    public List<DeploymentEventType> getTriggerEvents() { return triggerEvents; }
    public void setTriggerEvents(List<DeploymentEventType> triggerEvents) { this.triggerEvents = triggerEvents; }
    
    public List<String> getTriggerEnvironments() { return triggerEnvironments; }
    public void setTriggerEnvironments(List<String> triggerEnvironments) { 
        this.triggerEnvironments = triggerEnvironments; 
    }
    
    public Duration getDelayAfterDeployment() { return delayAfterDeployment; }
    public void setDelayAfterDeployment(Duration delayAfterDeployment) { 
        this.delayAfterDeployment = delayAfterDeployment; 
    }
    
    public Duration getTestTimeout() { return testTimeout; }
    public void setTestTimeout(Duration testTimeout) { this.testTimeout = testTimeout; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public Map<String, String> getTestScenarioMapping() { return testScenarioMapping; }
    public void setTestScenarioMapping(Map<String, String> testScenarioMapping) { 
        this.testScenarioMapping = testScenarioMapping; 
    }
    
    public List<String> getRequiredServices() { return requiredServices; }
    public void setRequiredServices(List<String> requiredServices) { this.requiredServices = requiredServices; }
    
    public boolean isWaitForHealthChecks() { return waitForHealthChecks; }
    public void setWaitForHealthChecks(boolean waitForHealthChecks) { this.waitForHealthChecks = waitForHealthChecks; }
}