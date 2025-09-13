package com.agentic.e2etester.recovery.actions;

import com.agentic.e2etester.recovery.RollbackAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Rollback action for Kafka operations
 */
public class KafkaRollbackAction implements RollbackAction {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaRollbackAction.class);
    
    private final String id;
    private final String serviceId;
    private final String topic;
    private final String messageId;
    private final int priority;
    
    public KafkaRollbackAction(String id, String serviceId, String topic, String messageId, int priority) {
        this.id = id;
        this.serviceId = serviceId;
        this.topic = topic;
        this.messageId = messageId;
        this.priority = priority;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getDescription() {
        return String.format("Rollback Kafka message '%s' on topic '%s' for service '%s'", 
            messageId, topic, serviceId);
    }
    
    @Override
    public CompletableFuture<Void> execute() {
        logger.info("Executing Kafka rollback: {}", getDescription());
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Simulate sending compensating message
                sendCompensatingMessage();
                
                logger.info("Kafka rollback completed successfully: {}", id);
            } catch (Exception e) {
                logger.error("Kafka rollback failed: {}", id, e);
                throw new RuntimeException("Kafka rollback failed", e);
            }
        });
    }
    
    @Override
    public boolean canExecute() {
        // Check if rollback is safe to execute
        return topic != null && messageId != null && serviceId != null;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String getServiceId() {
        return serviceId;
    }
    
    private void sendCompensatingMessage() {
        // Simulate sending a compensating message to undo the original message effect
        logger.debug("Sending compensating message for {} on topic {}", messageId, topic);
        
        try {
            Thread.sleep(50); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka rollback interrupted", e);
        }
    }
}