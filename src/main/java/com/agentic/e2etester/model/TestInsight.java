package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents an insight learned by the AI agent from test patterns and execution history.
 * Used to improve future test execution and decision making.
 */
public class TestInsight {
    
    private String insightId;
    private InsightType type;
    private String title;
    private String description;
    private double confidence;
    private String category;
    private Map<String, Object> metadata;
    private Instant discoveredAt;
    private int usageCount;
    private double effectiveness;
    private String source;
    
    public TestInsight() {
        this.metadata = new HashMap<>();
        this.discoveredAt = Instant.now();
        this.usageCount = 0;
        this.effectiveness = 0.0;
    }
    
    public TestInsight(InsightType type, String title, String description, double confidence) {
        this();
        this.type = type;
        this.title = title;
        this.description = description;
        this.confidence = confidence;
    }
    
    /**
     * Creates a performance insight.
     */
    public static TestInsight createPerformanceInsight(String title, String description, double confidence) {
        return new TestInsight(InsightType.PERFORMANCE, title, description, confidence);
    }
    
    /**
     * Creates a reliability insight.
     */
    public static TestInsight createReliabilityInsight(String title, String description, double confidence) {
        return new TestInsight(InsightType.RELIABILITY, title, description, confidence);
    }
    
    /**
     * Creates a pattern insight.
     */
    public static TestInsight createPatternInsight(String title, String description, double confidence) {
        return new TestInsight(InsightType.PATTERN, title, description, confidence);
    }
    
    /**
     * Creates a failure insight.
     */
    public static TestInsight createFailureInsight(String title, String description, double confidence) {
        return new TestInsight(InsightType.FAILURE_PATTERN, title, description, confidence);
    }
    
    /**
     * Adds metadata to the insight.
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    /**
     * Gets metadata value.
     */
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
    
    /**
     * Increments usage count and updates effectiveness.
     */
    public void recordUsage(boolean successful) {
        this.usageCount++;
        if (successful) {
            // Update effectiveness using weighted average
            this.effectiveness = (this.effectiveness * (usageCount - 1) + 1.0) / usageCount;
        } else {
            this.effectiveness = (this.effectiveness * (usageCount - 1)) / usageCount;
        }
    }
    
    /**
     * Checks if insight is highly effective.
     */
    public boolean isHighlyEffective() {
        return effectiveness > 0.8 && usageCount >= 3;
    }
    
    /**
     * Checks if insight is reliable based on usage.
     */
    public boolean isReliable() {
        return usageCount >= 5 && effectiveness > 0.6;
    }
    
    // Getters and Setters
    
    public String getInsightId() {
        return insightId;
    }
    
    public void setInsightId(String insightId) {
        this.insightId = insightId;
    }
    
    public InsightType getType() {
        return type;
    }
    
    public void setType(InsightType type) {
        this.type = type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Instant getDiscoveredAt() {
        return discoveredAt;
    }
    
    public void setDiscoveredAt(Instant discoveredAt) {
        this.discoveredAt = discoveredAt;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public double getEffectiveness() {
        return effectiveness;
    }
    
    public void setEffectiveness(double effectiveness) {
        this.effectiveness = effectiveness;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    @Override
    public String toString() {
        return String.format("TestInsight{type=%s, title='%s', confidence=%.2f, effectiveness=%.2f, usage=%d}", 
                           type, title, confidence, effectiveness, usageCount);
    }
}