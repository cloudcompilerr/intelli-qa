package com.agentic.e2etester.monitoring;

/**
 * Summary of test execution statuses.
 */
public class TestStatusSummary {
    
    private final int totalTests;
    private final long runningTests;
    private final long passedTests;
    private final long failedTests;
    private final long cancelledTests;
    
    public TestStatusSummary(int totalTests, long runningTests, long passedTests, 
                           long failedTests, long cancelledTests) {
        this.totalTests = totalTests;
        this.runningTests = runningTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.cancelledTests = cancelledTests;
    }
    
    // Getters
    public int getTotalTests() {
        return totalTests;
    }
    
    public long getRunningTests() {
        return runningTests;
    }
    
    public long getPassedTests() {
        return passedTests;
    }
    
    public long getFailedTests() {
        return failedTests;
    }
    
    public long getCancelledTests() {
        return cancelledTests;
    }
}