package com.agentic.e2etester.integration.database;

import com.couchbase.client.java.json.JsonObject;

import java.util.List;

/**
 * Interface for custom query validation rules.
 */
@FunctionalInterface
public interface QueryValidationRule {
    
    /**
     * Validates the query results according to custom logic.
     * 
     * @param rows The query result rows to validate
     * @return The validation result
     */
    QueryVerificationResult validate(List<JsonObject> rows);
}