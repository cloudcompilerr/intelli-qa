package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Document validation and comparison utilities for Couchbase documents.
 */
@Component
public class DocumentValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentValidator.class);
    
    /**
     * Validates a document against expected values and structure.
     */
    public DocumentValidationResult validate(JsonObject document, Map<String, Object> expectedValues) {
        logger.debug("Validating document against {} expected values", expectedValues.size());
        
        List<ValidationError> errors = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : expectedValues.entrySet()) {
            String fieldPath = entry.getKey();
            Object expectedValue = entry.getValue();
            
            ValidationError error = validateField(document, fieldPath, expectedValue);
            if (error != null) {
                errors.add(error);
            }
        }
        
        if (errors.isEmpty()) {
            logger.debug("Document validation successful");
            return DocumentValidationResult.success();
        } else {
            logger.debug("Document validation failed with {} errors", errors.size());
            return DocumentValidationResult.failure(errors);
        }
    }
    
    /**
     * Compares two documents and identifies differences.
     */
    public DocumentComparisonResult compare(JsonObject document1, JsonObject document2) {
        logger.debug("Comparing two documents");
        
        List<FieldDifference> differences = new ArrayList<>();
        Set<String> allKeys = new HashSet<>();
        
        // Collect all keys from both documents
        allKeys.addAll(document1.getNames());
        allKeys.addAll(document2.getNames());
        
        for (String key : allKeys) {
            Object value1 = document1.get(key);
            Object value2 = document2.get(key);
            
            if (!Objects.equals(value1, value2)) {
                differences.add(new FieldDifference(key, value1, value2));
            }
        }
        
        if (differences.isEmpty()) {
            logger.debug("Documents are identical");
            return DocumentComparisonResult.identical();
        } else {
            logger.debug("Documents differ in {} fields", differences.size());
            return DocumentComparisonResult.different(differences);
        }
    }
    
    /**
     * Validates a specific field in the document.
     */
    private ValidationError validateField(JsonObject document, String fieldPath, Object expectedValue) {
        try {
            Object actualValue = getFieldValue(document, fieldPath);
            
            if (expectedValue == null) {
                if (actualValue != null) {
                    return new ValidationError(fieldPath, "Expected null but got: " + actualValue);
                }
            } else if (actualValue == null) {
                return new ValidationError(fieldPath, "Expected " + expectedValue + " but field is missing or null");
            } else if (!isValueMatch(actualValue, expectedValue)) {
                return new ValidationError(fieldPath, 
                    String.format("Expected %s but got %s", expectedValue, actualValue));
            }
            
            return null; // No error
            
        } catch (Exception e) {
            return new ValidationError(fieldPath, "Failed to validate field: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves a field value from a document using dot notation path.
     */
    private Object getFieldValue(JsonObject document, String fieldPath) {
        String[] pathParts = fieldPath.split("\\.");
        Object current = document;
        
        for (String part : pathParts) {
            if (current instanceof JsonObject jsonObj) {
                current = jsonObj.get(part);
            } else if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null; // Path doesn't exist
            }
            
            if (current == null) {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Checks if two values match, handling different types appropriately.
     */
    private boolean isValueMatch(Object actualValue, Object expectedValue) {
        // Handle exact equality
        if (Objects.equals(actualValue, expectedValue)) {
            return true;
        }
        
        // Handle null cases
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        
        // Handle numeric comparisons (int vs long, etc.)
        if (actualValue instanceof Number actualNum && expectedValue instanceof Number expectedNum) {
            return Math.abs(actualNum.doubleValue() - expectedNum.doubleValue()) < 0.0001;
        }
        
        // Handle string comparisons
        if (actualValue instanceof String && expectedValue instanceof String) {
            return actualValue.equals(expectedValue);
        }
        
        // Handle collection comparisons
        if (actualValue instanceof Collection<?> actualColl && expectedValue instanceof Collection<?> expectedColl) {
            return actualColl.size() == expectedColl.size() && 
                   actualColl.containsAll(expectedColl) && 
                   expectedColl.containsAll(actualColl);
        }
        
        return false;
    }
    
    /**
     * Validates document structure against a schema.
     */
    public DocumentValidationResult validateSchema(JsonObject document, DocumentSchema schema) {
        logger.debug("Validating document against schema");
        
        List<ValidationError> errors = new ArrayList<>();
        
        // Check required fields
        for (String requiredField : schema.getRequiredFields()) {
            if (!document.containsKey(requiredField) || document.get(requiredField) == null) {
                errors.add(new ValidationError(requiredField, "Required field is missing"));
            }
        }
        
        // Check field types
        if (schema.getFieldTypes() != null) {
            for (Map.Entry<String, Class<?>> fieldType : schema.getFieldTypes().entrySet()) {
                String fieldName = fieldType.getKey();
                Class<?> expectedType = fieldType.getValue();
                
                if (document.containsKey(fieldName)) {
                    Object value = document.get(fieldName);
                    if (value != null && !expectedType.isAssignableFrom(value.getClass())) {
                        errors.add(new ValidationError(fieldName, 
                            String.format("Expected type %s but got %s", 
                                        expectedType.getSimpleName(), 
                                        value.getClass().getSimpleName())));
                    }
                }
            }
        }
        
        if (errors.isEmpty()) {
            return DocumentValidationResult.success();
        } else {
            return DocumentValidationResult.failure(errors);
        }
    }
    
    /**
     * Validates that a document contains all expected fields.
     */
    public DocumentValidationResult validateFieldsExist(JsonObject document, List<String> requiredFields) {
        List<ValidationError> errors = requiredFields.stream()
            .filter(field -> !document.containsKey(field))
            .map(field -> new ValidationError(field, "Required field is missing"))
            .collect(Collectors.toList());
        
        if (errors.isEmpty()) {
            return DocumentValidationResult.success();
        } else {
            return DocumentValidationResult.failure(errors);
        }
    }
}