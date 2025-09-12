package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionRule;
import com.agentic.e2etester.model.AssertionSeverity;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Objects;

/**
 * Represents a data assertion that validates data consistency, integrity, and persistence.
 * Used for validating database operations, data transformations, and data flow across services.
 */
public class DataAssertion extends AssertionRule {
    
    @NotBlank(message = "Data source cannot be blank")
    private String dataSource;
    
    private String databaseName;
    private String collectionName;
    private String documentId;
    private String queryExpression;
    private String jsonPath;
    private String schemaValidation;
    private List<String> requiredFields;
    private String dataTransformationRule;
    
    // Default constructor
    public DataAssertion() {
        super();
        this.setSeverity(AssertionSeverity.ERROR);
    }
    
    // Constructor with required fields
    public DataAssertion(String ruleId, String description, String dataSource) {
        super(ruleId, null, description);
        this.dataSource = dataSource;
        this.setSeverity(AssertionSeverity.ERROR);
    }
    
    // Getters and setters
    public String getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    public String getDatabaseName() {
        return databaseName;
    }
    
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
    
    public String getCollectionName() {
        return collectionName;
    }
    
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getQueryExpression() {
        return queryExpression;
    }
    
    public void setQueryExpression(String queryExpression) {
        this.queryExpression = queryExpression;
    }
    
    public String getJsonPath() {
        return jsonPath;
    }
    
    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }
    
    public String getSchemaValidation() {
        return schemaValidation;
    }
    
    public void setSchemaValidation(String schemaValidation) {
        this.schemaValidation = schemaValidation;
    }
    
    public List<String> getRequiredFields() {
        return requiredFields;
    }
    
    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields;
    }
    
    public String getDataTransformationRule() {
        return dataTransformationRule;
    }
    
    public void setDataTransformationRule(String dataTransformationRule) {
        this.dataTransformationRule = dataTransformationRule;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataAssertion that = (DataAssertion) o;
        return Objects.equals(dataSource, that.dataSource) &&
               Objects.equals(databaseName, that.databaseName) &&
               Objects.equals(collectionName, that.collectionName) &&
               Objects.equals(documentId, that.documentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataSource, databaseName, collectionName, documentId);
    }
    
    @Override
    public String toString() {
        return "DataAssertion{" +
               "ruleId='" + getRuleId() + '\'' +
               ", dataSource='" + dataSource + '\'' +
               ", databaseName='" + databaseName + '\'' +
               ", collectionName='" + collectionName + '\'' +
               ", documentId='" + documentId + '\'' +
               ", queryExpression='" + queryExpression + '\'' +
               '}';
    }
}