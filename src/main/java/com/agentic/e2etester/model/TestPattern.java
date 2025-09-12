package com.agentic.e2etester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a learned test pattern that can be reused for similar test scenarios.
 * Contains the pattern signature, execution characteristics, and success metrics.
 */
public class TestPattern {
    
    @NotBlank(message = "Pattern ID cannot be blank")
    @Size(max = 100, message = "Pattern ID cannot exceed 100 characters")
    @JsonProperty("patternId")
    private String patternId;
    
    @NotBlank(message = "Pattern name cannot be blank")
    @Size(max = 200, message = "Pattern name cannot exceed 200 characters")
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @NotNull(message = "Pattern type cannot be null")
    @JsonProperty("type")
    private PatternType type;
    
    @JsonProperty("serviceFlow")
    private List<String> serviceFlow;
    
    @JsonProperty("characteristics")
    private Map<String, Object> characteristics;
    
    @JsonProperty("successMetrics")
    private TestMetrics successMetrics;
    
    @JsonProperty("failurePatterns")
    private List<String> failurePatterns;
    
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    
    @JsonProperty("lastUsed")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant lastUsed;
    
    @JsonProperty("usageCount")
    private int usageCount;
    
    @JsonProperty("successRate")
    private double successRate;
    
    @JsonProperty("averageExecutionTime")
    private long averageExecutionTimeMs;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    // Default constructor for Jackson
    public TestPattern() {
        this.createdAt = Instant.now();
        this.usageCount = 0;
        this.successRate = 0.0;
    }
    
    // Constructor with required fields
    public TestPattern(String patternId, String name, PatternType type) {
        this();
        this.patternId = patternId;
        this.name = name;
        this.type = type;
    }
    
    // Getters and setters
    public String getPatternId() {
        return patternId;
    }
    
    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public PatternType getType() {
        return type;
    }
    
    public void setType(PatternType type) {
        this.type = type;
    }
    
    public List<String> getServiceFlow() {
        return serviceFlow;
    }
    
    public void setServiceFlow(List<String> serviceFlow) {
        this.serviceFlow = serviceFlow;
    }
    
    public Map<String, Object> getCharacteristics() {
        return characteristics;
    }
    
    public void setCharacteristics(Map<String, Object> characteristics) {
        this.characteristics = characteristics;
    }
    
    public TestMetrics getSuccessMetrics() {
        return successMetrics;
    }
    
    public void setSuccessMetrics(TestMetrics successMetrics) {
        this.successMetrics = successMetrics;
    }
    
    public List<String> getFailurePatterns() {
        return failurePatterns;
    }
    
    public void setFailurePatterns(List<String> failurePatterns) {
        this.failurePatterns = failurePatterns;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
    
    public long getAverageExecutionTimeMs() {
        return averageExecutionTimeMs;
    }
    
    public void setAverageExecutionTimeMs(long averageExecutionTimeMs) {
        this.averageExecutionTimeMs = averageExecutionTimeMs;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    // Utility methods
    public void incrementUsage() {
        this.usageCount++;
        this.lastUsed = Instant.now();
    }
    
    public void updateSuccessRate(boolean success) {
        double totalTests = this.usageCount;
        double currentSuccesses = this.successRate * (totalTests - 1);
        if (success) {
            currentSuccesses++;
        }
        this.successRate = currentSuccesses / totalTests;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestPattern that = (TestPattern) o;
        return Objects.equals(patternId, that.patternId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(patternId);
    }
    
    @Override
    public String toString() {
        return "TestPattern{" +
               "patternId='" + patternId + '\'' +
               ", name='" + name + '\'' +
               ", type=" + type +
               ", usageCount=" + usageCount +
               ", successRate=" + successRate +
               '}';
    }
}