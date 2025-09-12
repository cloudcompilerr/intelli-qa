package com.agentic.e2etester.integration.kafka;

import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.TestContext;
import com.agentic.e2etester.model.TestEvent;
import com.agentic.e2etester.model.InteractionType;
import com.agentic.e2etester.model.InteractionStatus;
import com.agentic.e2etester.model.EventType;
import com.agentic.e2etester.model.EventSeverity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for injecting test events into the system under test.
 * Handles correlation ID tracking and interaction logging.
 */
@Component
public class KafkaTestProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaTestProducer.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public KafkaTestProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Sends a test event to the specified Kafka topic with correlation tracking.
     *
     * @param topic the Kafka topic to send to
     * @param event the event payload to send
     * @param testContext the test context for correlation tracking
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, Object>> sendTestEvent(String topic, Object event, TestContext testContext) {
        String correlationId = testContext.getCorrelationId();
        String messageKey = generateMessageKey(correlationId);
        
        logger.debug("Sending test event to topic '{}' with correlation ID: {}", topic, correlationId);
        
        // Record the interaction start
        ServiceInteraction interaction = createInteraction(topic, event, correlationId, testContext.getCurrentStepId());
        testContext.addInteraction(interaction);
        
        // Add correlation ID to event if it's a Map
        Object enrichedEvent = enrichEventWithCorrelationId(event, correlationId);
        
        Instant startTime = Instant.now();
        
        return kafkaTemplate.send(topic, messageKey, enrichedEvent)
            .whenComplete((result, throwable) -> {
                long responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis();
                
                if (throwable != null) {
                    handleSendFailure(interaction, throwable, responseTime, testContext);
                } else {
                    handleSendSuccess(interaction, result, responseTime, testContext);
                }
            });
    }
    
    /**
     * Sends a test event with custom headers for advanced correlation tracking.
     *
     * @param topic the Kafka topic to send to
     * @param event the event payload to send
     * @param headers additional headers to include
     * @param testContext the test context for correlation tracking
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, Object>> sendTestEventWithHeaders(
            String topic, Object event, Map<String, String> headers, TestContext testContext) {
        
        String correlationId = testContext.getCorrelationId();
        String messageKey = generateMessageKey(correlationId);
        
        logger.debug("Sending test event with headers to topic '{}' with correlation ID: {}", topic, correlationId);
        
        // Record the interaction start
        ServiceInteraction interaction = createInteraction(topic, event, correlationId, testContext.getCurrentStepId());
        interaction.setMetadata(headers);
        testContext.addInteraction(interaction);
        
        // Add correlation ID to event and headers
        Object enrichedEvent = enrichEventWithCorrelationId(event, correlationId);
        
        Instant startTime = Instant.now();
        
        return kafkaTemplate.send(topic, messageKey, enrichedEvent)
            .whenComplete((result, throwable) -> {
                long responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis();
                
                if (throwable != null) {
                    handleSendFailure(interaction, throwable, responseTime, testContext);
                } else {
                    handleSendSuccess(interaction, result, responseTime, testContext);
                }
            });
    }
    
    /**
     * Sends a batch of test events to multiple topics.
     *
     * @param events map of topic to event payload
     * @param testContext the test context for correlation tracking
     * @return CompletableFuture that completes when all events are sent
     */
    public CompletableFuture<Void> sendBatchTestEvents(Map<String, Object> events, TestContext testContext) {
        logger.debug("Sending batch of {} test events with correlation ID: {}", 
                    events.size(), testContext.getCorrelationId());
        
        CompletableFuture<?>[] futures = events.entrySet().stream()
            .map(entry -> sendTestEvent(entry.getKey(), entry.getValue(), testContext))
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures);
    }
    
    private String generateMessageKey(String correlationId) {
        return correlationId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private ServiceInteraction createInteraction(String topic, Object event, String correlationId, String stepId) {
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("kafka-topic-" + topic);
        interaction.setType(InteractionType.KAFKA_PRODUCER);
        interaction.setRequest(event);
        interaction.setCorrelationId(correlationId);
        interaction.setStepId(stepId);
        interaction.setStatus(InteractionStatus.IN_PROGRESS);
        interaction.setTimestamp(Instant.now());
        return interaction;
    }
    
    private Object enrichEventWithCorrelationId(Object event, String correlationId) {
        if (event instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventMap = (Map<String, Object>) event;
            eventMap.put("correlationId", correlationId);
            eventMap.put("testTimestamp", Instant.now().toString());
            return eventMap;
        }
        return event;
    }
    
    private void handleSendSuccess(ServiceInteraction interaction, SendResult<String, Object> result, 
                                 long responseTime, TestContext testContext) {
        interaction.setStatus(InteractionStatus.SUCCESS);
        interaction.setResponseTimeMs(responseTime);
        interaction.setResponse(Map.of(
            "partition", result.getRecordMetadata().partition(),
            "offset", result.getRecordMetadata().offset(),
            "topic", result.getRecordMetadata().topic()
        ));
        
        // Log success event
        TestEvent event = new TestEvent(
            UUID.randomUUID().toString(),
            EventType.KAFKA_MESSAGE_SENT,
            "Successfully sent message to topic: " + result.getRecordMetadata().topic()
        );
        event.setCorrelationId(testContext.getCorrelationId());
        event.setStepId(testContext.getCurrentStepId());
        event.setSeverity(EventSeverity.INFO);
        event.setData(Map.of(
            "topic", result.getRecordMetadata().topic(),
            "partition", result.getRecordMetadata().partition(),
            "offset", result.getRecordMetadata().offset(),
            "responseTimeMs", responseTime
        ));
        testContext.addEvent(event);
        
        logger.info("Successfully sent message to topic '{}' [partition={}, offset={}] in {}ms", 
                   result.getRecordMetadata().topic(), 
                   result.getRecordMetadata().partition(),
                   result.getRecordMetadata().offset(),
                   responseTime);
    }
    
    private void handleSendFailure(ServiceInteraction interaction, Throwable throwable, 
                                 long responseTime, TestContext testContext) {
        interaction.setStatus(InteractionStatus.FAILURE);
        interaction.setResponseTimeMs(responseTime);
        interaction.setErrorMessage(throwable.getMessage());
        
        // Log failure event
        TestEvent event = new TestEvent(
            UUID.randomUUID().toString(),
            EventType.KAFKA_MESSAGE_FAILED,
            "Failed to send message: " + throwable.getMessage()
        );
        event.setCorrelationId(testContext.getCorrelationId());
        event.setStepId(testContext.getCurrentStepId());
        event.setSeverity(EventSeverity.ERROR);
        event.setData(Map.of(
            "error", throwable.getMessage(),
            "responseTimeMs", responseTime
        ));
        testContext.addEvent(event);
        
        logger.error("Failed to send message to Kafka in {}ms: {}", responseTime, throwable.getMessage(), throwable);
    }
}