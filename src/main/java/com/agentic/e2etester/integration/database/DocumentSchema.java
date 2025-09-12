package com.agentic.e2etester.integration.database;

import java.util.List;
import java.util.Map;

/**
 * Defines the expected schema for document validation.
 */
public class DocumentSchema {
    
    private final List<String> requiredFields;
    private final Map<String, Class<?>> fieldTypes;
    
    public DocumentSchema(List<String> requiredFields, Map<String, Class<?>> fieldTypes) {
        this.requiredFields = requiredFields;
        this.fieldTypes = fieldTypes;
    }
    
    public static DocumentSchemaBuilder builder() {
        return new DocumentSchemaBuilder();
    }
    
    public List<String> getRequiredFields() {
        return requiredFields;
    }
    
    public Map<String, Class<?>> getFieldTypes() {
        return fieldTypes;
    }
    
    @Override
    public String toString() {
        return "DocumentSchema{" +
                "requiredFields=" + requiredFields +
                ", fieldTypes=" + fieldTypes +
                '}';
    }
    
    public static class DocumentSchemaBuilder {
        private List<String> requiredFields;
        private Map<String, Class<?>> fieldTypes;
        
        public DocumentSchemaBuilder requiredFields(List<String> requiredFields) {
            this.requiredFields = requiredFields;
            return this;
        }
        
        public DocumentSchemaBuilder fieldTypes(Map<String, Class<?>> fieldTypes) {
            this.fieldTypes = fieldTypes;
            return this;
        }
        
        public DocumentSchema build() {
            return new DocumentSchema(requiredFields, fieldTypes);
        }
    }
}