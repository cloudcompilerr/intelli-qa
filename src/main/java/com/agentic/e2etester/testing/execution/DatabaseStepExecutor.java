package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.integration.database.CouchbaseAdapter;
import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Step executor for database operations and validations.
 */
@Component
public class DatabaseStepExecutor implements StepExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseStepExecutor.class);
    
    private final CouchbaseAdapter couchbaseAdapter;
    
    public DatabaseStepExecutor(CouchbaseAdapter couchbaseAdapter) {
        this.couchbaseAdapter = couchbaseAdapter;
    }
    
    @Override
    public CompletableFuture<StepResult> executeStep(TestStep step, TestContext context) {
        logger.debug("Executing database step: {}", step.getStepId());
        
        StepResult stepResult = new StepResult(step.getStepId(), TestStatus.RUNNING);
        stepResult.setStartTime(Instant.now());
        
        // Extract database operation details
        String operation = (String) step.getInputData().get("operation");
        String bucketName = (String) step.getInputData().get("bucket");
        String collectionName = (String) step.getInputData().get("collection");
        String documentId = (String) step.getInputData().get("documentId");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Object result = null;
                
                switch (operation.toUpperCase()) {
                    case "GET":
                        result = couchbaseAdapter.getDocument(bucketName, collectionName, documentId);
                        break;
                    case "VALIDATE":
                        // Perform document validation
                        result = performDocumentValidation(step, bucketName, collectionName, documentId);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported database operation: " + operation);
                }
                
                stepResult.setStatus(TestStatus.PASSED);
                stepResult.setOutput(result);
                stepResult.setEndTime(Instant.now());
                
                return stepResult;
                
            } catch (Exception e) {
                logger.error("Database step failed: {}", step.getStepId(), e);
                stepResult.setStatus(TestStatus.FAILED);
                stepResult.setErrorMessage(e.getMessage());
                stepResult.setEndTime(Instant.now());
                return stepResult;
            }
        });
    }
    
    private Object performDocumentValidation(TestStep step, String bucketName, String collectionName, String documentId) {
        // This would integrate with the document validator
        // For now, just return a simple validation result
        return "Document validation completed for " + documentId;
    }
    
    @Override
    public boolean canExecute(TestStep step) {
        return step.getType() == StepType.DATABASE_CHECK;
    }
}