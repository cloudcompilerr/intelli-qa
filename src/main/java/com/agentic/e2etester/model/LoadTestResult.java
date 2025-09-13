package com.agentic.e2etester.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a load test execution containing performance metrics and test outcomes.
 */
public class LoadTestResult {
    private int userId;
    private Instant startTime;
    private Instant endTime;
    private List<TestResult> orderResults = new ArrayList<>();
    private List<Exception> errors = new ArrayList<>();
    private Duration totalExecutionTime;
    private double averageResponseTime;
    private double throughput;
    private double errorRate;
    private int successfulOrders;
    private int failedOrders;

    // Constructors
    public LoadTestResult() {}

    public LoadTestResult(int userId) {
        this.userId = userId;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        if (startTime != null) {
            this.totalExecutionTime = Duration.between(startTime, endTime);
        }
    }

    public List<TestResult> getOrderResults() {
        return orderResults;
    }

    public void setOrderResults(List<TestResult> orderResults) {
        this.orderResults = orderResults;
        calculateMetrics();
    }

    public List<Exception> getErrors() {
        return errors;
    }

    public void setErrors(List<Exception> errors) {
        this.errors = errors;
    }

    public void addError(Exception error) {
        this.errors.add(error);
    }

    public Duration getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public int getSuccessfulOrders() {
        return successfulOrders;
    }

    public int getFailedOrders() {
        return failedOrders;
    }

    private void calculateMetrics() {
        if (orderResults.isEmpty()) {
            return;
        }

        // Calculate success/failure counts
        successfulOrders = (int) orderResults.stream()
            .mapToLong(result -> result.getStatus() == TestStatus.PASSED ? 1 : 0)
            .sum();
        failedOrders = orderResults.size() - successfulOrders;

        // Calculate error rate
        errorRate = (double) failedOrders / orderResults.size();

        // Calculate average response time
        averageResponseTime = orderResults.stream()
            .mapToDouble(result -> result.getExecutionTime().toMillis())
            .average()
            .orElse(0.0);

        // Calculate throughput (orders per second)
        if (totalExecutionTime != null && totalExecutionTime.toSeconds() > 0) {
            throughput = (double) orderResults.size() / totalExecutionTime.toSeconds();
        }
    }
}