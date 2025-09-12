package com.agentic.e2etester.integration.database;

import java.util.List;
import java.util.Map;

/**
 * Defines expectations for query result verification.
 */
public class QueryExpectation {
    
    private Integer expectedRowCount;
    private Integer minimumRowCount;
    private Integer maximumRowCount;
    private Map<String, Object> expectedFieldValues;
    private List<String> requiredFields;
    private List<QueryValidationRule> customValidations;
    
    public QueryExpectation() {
    }
    
    public static QueryExpectation builder() {
        return new QueryExpectation();
    }
    
    public QueryExpectation expectedRowCount(int count) {
        this.expectedRowCount = count;
        return this;
    }
    
    public QueryExpectation minimumRowCount(int count) {
        this.minimumRowCount = count;
        return this;
    }
    
    public QueryExpectation maximumRowCount(int count) {
        this.maximumRowCount = count;
        return this;
    }
    
    public QueryExpectation expectedFieldValues(Map<String, Object> fieldValues) {
        this.expectedFieldValues = fieldValues;
        return this;
    }
    
    public QueryExpectation requiredFields(List<String> fields) {
        this.requiredFields = fields;
        return this;
    }
    
    public QueryExpectation customValidations(List<QueryValidationRule> validations) {
        this.customValidations = validations;
        return this;
    }
    
    public Integer getExpectedRowCount() {
        return expectedRowCount;
    }
    
    public void setExpectedRowCount(Integer expectedRowCount) {
        this.expectedRowCount = expectedRowCount;
    }
    
    public Integer getMinimumRowCount() {
        return minimumRowCount;
    }
    
    public void setMinimumRowCount(Integer minimumRowCount) {
        this.minimumRowCount = minimumRowCount;
    }
    
    public Integer getMaximumRowCount() {
        return maximumRowCount;
    }
    
    public void setMaximumRowCount(Integer maximumRowCount) {
        this.maximumRowCount = maximumRowCount;
    }
    
    public Map<String, Object> getExpectedFieldValues() {
        return expectedFieldValues;
    }
    
    public void setExpectedFieldValues(Map<String, Object> expectedFieldValues) {
        this.expectedFieldValues = expectedFieldValues;
    }
    
    public List<String> getRequiredFields() {
        return requiredFields;
    }
    
    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields;
    }
    
    public List<QueryValidationRule> getCustomValidations() {
        return customValidations;
    }
    
    public void setCustomValidations(List<QueryValidationRule> customValidations) {
        this.customValidations = customValidations;
    }
    
    @Override
    public String toString() {
        return "QueryExpectation{" +
                "expectedRowCount=" + expectedRowCount +
                ", minimumRowCount=" + minimumRowCount +
                ", maximumRowCount=" + maximumRowCount +
                ", expectedFieldValues=" + expectedFieldValues +
                ", requiredFields=" + requiredFields +
                ", customValidations=" + (customValidations != null ? customValidations.size() : 0) +
                '}';
    }
}