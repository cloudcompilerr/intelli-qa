package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionRule;
import com.agentic.e2etester.model.AssertionSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents a technical assertion that validates system behavior, performance, and technical metrics.
 * Used for validating response times, status codes, system health, and technical requirements.
 */
public class TechnicalAssertion extends AssertionRule {
    
    @NotBlank(message = "Metric name cannot be blank")
    private String metricName;
    
    private String serviceId;
    private String endpointPath;
    
    @Positive(message = "Response time threshold must be positive")
    private Duration responseTimeThreshold;
    
    private Integer expectedStatusCode;
    private String healthCheckEndpoint;
    private Double performanceThreshold;
    private String performanceMetric;
    
    // Default constructor
    public TechnicalAssertion() {
        super();
        this.setSeverity(AssertionSeverity.ERROR);
    }
    
    // Constructor with required fields
    public TechnicalAssertion(String ruleId, String description, String metricName) {
        super(ruleId, null, description);
        this.metricName = metricName;
        this.setSeverity(AssertionSeverity.ERROR);
    }
    
    // Getters and setters
    public String getMetricName() {
        return metricName;
    }
    
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getEndpointPath() {
        return endpointPath;
    }
    
    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }
    
    public Duration getResponseTimeThreshold() {
        return responseTimeThreshold;
    }
    
    public void setResponseTimeThreshold(Duration responseTimeThreshold) {
        this.responseTimeThreshold = responseTimeThreshold;
    }
    
    public Integer getExpectedStatusCode() {
        return expectedStatusCode;
    }
    
    public void setExpectedStatusCode(Integer expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }
    
    public String getHealthCheckEndpoint() {
        return healthCheckEndpoint;
    }
    
    public void setHealthCheckEndpoint(String healthCheckEndpoint) {
        this.healthCheckEndpoint = healthCheckEndpoint;
    }
    
    public Double getPerformanceThreshold() {
        return performanceThreshold;
    }
    
    public void setPerformanceThreshold(Double performanceThreshold) {
        this.performanceThreshold = performanceThreshold;
    }
    
    public String getPerformanceMetric() {
        return performanceMetric;
    }
    
    public void setPerformanceMetric(String performanceMetric) {
        this.performanceMetric = performanceMetric;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TechnicalAssertion that = (TechnicalAssertion) o;
        return Objects.equals(metricName, that.metricName) &&
               Objects.equals(serviceId, that.serviceId) &&
               Objects.equals(endpointPath, that.endpointPath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), metricName, serviceId, endpointPath);
    }
    
    @Override
    public String toString() {
        return "TechnicalAssertion{" +
               "ruleId='" + getRuleId() + '\'' +
               ", metricName='" + metricName + '\'' +
               ", serviceId='" + serviceId + '\'' +
               ", endpointPath='" + endpointPath + '\'' +
               ", responseTimeThreshold=" + responseTimeThreshold +
               ", expectedStatusCode=" + expectedStatusCode +
               '}';
    }
}