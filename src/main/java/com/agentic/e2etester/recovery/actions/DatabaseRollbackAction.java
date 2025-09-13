package com.agentic.e2etester.recovery.actions;

import com.agentic.e2etester.recovery.RollbackAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Rollback action for database operations
 */
public class DatabaseRollbackAction implements RollbackAction {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRollbackAction.class);
    
    private final String id;
    private final String serviceId;
    private final String operation;
    private final Object originalData;
    private final int priority;
    
    public DatabaseRollbackAction(String id, String serviceId, String operation, Object originalData, int priority) {
        this.id = id;
        this.serviceId = serviceId;
        this.operation = operation;
        this.originalData = originalData;
        this.priority = priority;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getDescription() {
        return String.format("Rollback database operation '%s' on service '%s'", operation, serviceId);
    }
    
    @Override
    public CompletableFuture<Void> execute() {
        logger.info("Executing database rollback: {}", getDescription());
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Simulate database rollback operation
                switch (operation) {
                    case "INSERT" -> rollbackInsert();
                    case "UPDATE" -> rollbackUpdate();
                    case "DELETE" -> rollbackDelete();
                    default -> logger.warn("Unknown database operation for rollback: {}", operation);
                }
                
                logger.info("Database rollback completed successfully: {}", id);
            } catch (Exception e) {
                logger.error("Database rollback failed: {}", id, e);
                throw new RuntimeException("Database rollback failed", e);
            }
        });
    }
    
    @Override
    public boolean canExecute() {
        // Check if rollback is safe to execute
        return originalData != null && serviceId != null;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String getServiceId() {
        return serviceId;
    }
    
    private void rollbackInsert() {
        // Simulate deleting inserted record
        logger.debug("Rolling back INSERT operation by deleting record");
        simulateDelay(100);
    }
    
    private void rollbackUpdate() {
        // Simulate restoring original data
        logger.debug("Rolling back UPDATE operation by restoring original data: {}", originalData);
        simulateDelay(150);
    }
    
    private void rollbackDelete() {
        // Simulate restoring deleted record
        logger.debug("Rolling back DELETE operation by restoring record: {}", originalData);
        simulateDelay(200);
    }
    
    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rollback interrupted", e);
        }
    }
}