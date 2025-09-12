package com.agentic.e2etester.integration.kafka;

import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.TestContext;
import com.agentic.e2etester.model.TestEvent;
import com.agentic.e2etester.model.InteractionType;
import com.agentic.e2etester.model.InteractionStatus;
import com.agentic.e2etester.model.EventType;
import com.agentic.e2etester.model.EventSeverity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Kafka consumer for monitoring event flows during test execution.
 * Provides correlation ID tracking and message filtering capabilities.
 */
@Component
public class KafkaTestConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaTestConsumer.class);
    
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final Map<String, ConsumerSession> activeSessions;
    
    public KafkaTestConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "kafka-test-consumer");
            t.setDaemon(true);
            return t;
        });
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    /**
     * Starts monitoring a topic for messages with the specified correlation ID.
     *
     * @param consumer the Kafka consumer to use
     * @param topic the topic to monitor
     * @param correlationId the correlation ID to filter by
     * @param testContext the test context for tracking
     * @param timeout maximum time to wait for messages
     * @return CompletableFuture with collected messages
     */
    public CompletableFuture<List<ConsumerRecord<String, Object>>> monitorTopic(
            KafkaConsumer<String, Object> consumer, 
            String topic, 
            String correlationId, 
            TestContext testContext,
            Duration timeout) {
        
        return monitorTopicWithFilter(consumer, topic, 
            record -> matchesCorrelationId(record, correlationId), 
            testContext, timeout);
    }
    
    /**
     * Starts monitoring a topic with a custom message filter.
     *
     * @param consumer the Kafka consumer to use
     * @param topic the topic to monitor
     * @param messageFilter predicate to filter messages
     * @param testContext the test context for tracking
     * @param timeout maximum time to wait for messages
     * @return CompletableFuture with collected messages
     */
    public CompletableFuture<List<ConsumerRecord<String, Object>>> monitorTopicWithFilter(
            KafkaConsumer<String, Object> consumer,
            String topic,
            Predicate<ConsumerRecord<String, Object>> messageFilter,
            TestContext testContext,
            Duration timeout) {
        
        String sessionId = UUID.randomUUID().toString();
        logger.debug("Starting topic monitoring session {} for topic '{}' with correlation ID: {}", 
                    sessionId, topic, testContext.getCorrelationId());
        
        CompletableFuture<List<ConsumerRecord<String, Object>>> future = new CompletableFuture<>();
        
        ConsumerSession session = new ConsumerSession(sessionId, consumer, topic, messageFilter, 
                                                    testContext, future, timeout);
        activeSessions.put(sessionId, session);
        
        // Start monitoring in background thread
        executorService.submit(() -> {
            try {
                runConsumerSession(session);
            } catch (Exception e) {
                logger.error("Error in consumer session {}: {}", sessionId, e.getMessage(), e);
                future.completeExceptionally(e);
            } finally {
                activeSessions.remove(sessionId);
            }
        });
        
        return future;
    }
    
    /**
     * Monitors multiple topics simultaneously for correlated messages.
     *
     * @param consumer the Kafka consumer to use
     * @param topics the topics to monitor
     * @param correlationId the correlation ID to filter by
     * @param testContext the test context for tracking
     * @param timeout maximum time to wait for messages
     * @return CompletableFuture with map of topic to collected messages
     */
    public CompletableFuture<Map<String, List<ConsumerRecord<String, Object>>>> monitorMultipleTopics(
            KafkaConsumer<String, Object> consumer,
            List<String> topics,
            String correlationId,
            TestContext testContext,
            Duration timeout) {
        
        String sessionId = UUID.randomUUID().toString();
        logger.debug("Starting multi-topic monitoring session {} for topics {} with correlation ID: {}", 
                    sessionId, topics, correlationId);
        
        CompletableFuture<Map<String, List<ConsumerRecord<String, Object>>>> future = new CompletableFuture<>();
        
        MultiTopicConsumerSession session = new MultiTopicConsumerSession(sessionId, consumer, topics, 
                                                                         correlationId, testContext, future, timeout);
        
        // Start monitoring in background thread
        executorService.submit(() -> {
            try {
                runMultiTopicConsumerSession(session);
            } catch (Exception e) {
                logger.error("Error in multi-topic consumer session {}: {}", sessionId, e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Stops monitoring for the specified session.
     *
     * @param sessionId the session ID to stop
     */
    public void stopMonitoring(String sessionId) {
        ConsumerSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.stop();
            logger.debug("Stopped monitoring session: {}", sessionId);
        }
    }
    
    /**
     * Stops all active monitoring sessions.
     */
    public void stopAllMonitoring() {
        logger.debug("Stopping all {} active monitoring sessions", activeSessions.size());
        activeSessions.values().forEach(ConsumerSession::stop);
        activeSessions.clear();
    }
    
    private void runConsumerSession(ConsumerSession session) {
        List<ConsumerRecord<String, Object>> collectedMessages = new ArrayList<>();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(session.timeout);
        
        // Subscribe to topic
        session.consumer.subscribe(Collections.singletonList(session.topic));
        
        // Record monitoring start
        recordMonitoringStart(session);
        
        try {
            while (!session.stopped.get() && Instant.now().isBefore(endTime)) {
                ConsumerRecords<String, Object> records = session.consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, Object> record : records) {
                    if (session.messageFilter.test(record)) {
                        collectedMessages.add(record);
                        recordMessageReceived(record, session);
                        
                        logger.debug("Collected message from topic '{}' [partition={}, offset={}] for session {}", 
                                   record.topic(), record.partition(), record.offset(), session.sessionId);
                    }
                }
                
                // Check if we should continue polling
                if (collectedMessages.isEmpty() && Duration.between(startTime, Instant.now()).compareTo(session.timeout) >= 0) {
                    break;
                }
            }
            
            recordMonitoringComplete(session, collectedMessages.size());
            session.future.complete(collectedMessages);
            
        } catch (Exception e) {
            recordMonitoringError(session, e);
            session.future.completeExceptionally(e);
        } finally {
            try {
                session.consumer.unsubscribe();
            } catch (Exception e) {
                logger.warn("Error unsubscribing consumer in session {}: {}", session.sessionId, e.getMessage());
            }
        }
    }
    
    private void runMultiTopicConsumerSession(MultiTopicConsumerSession session) {
        Map<String, List<ConsumerRecord<String, Object>>> collectedMessages = new HashMap<>();
        session.topics.forEach(topic -> collectedMessages.put(topic, new ArrayList<>()));
        
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(session.timeout);
        
        // Subscribe to all topics
        session.consumer.subscribe(session.topics);
        
        try {
            while (!session.stopped.get() && Instant.now().isBefore(endTime)) {
                ConsumerRecords<String, Object> records = session.consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, Object> record : records) {
                    if (matchesCorrelationId(record, session.correlationId)) {
                        collectedMessages.get(record.topic()).add(record);
                        
                        // Record interaction
                        ServiceInteraction interaction = createConsumerInteraction(record, session.testContext);
                        session.testContext.addInteraction(interaction);
                        
                        logger.debug("Collected message from topic '{}' for multi-topic session {}", 
                                   record.topic(), session.sessionId);
                    }
                }
            }
            
            session.future.complete(collectedMessages);
            
        } catch (Exception e) {
            session.future.completeExceptionally(e);
        } finally {
            try {
                session.consumer.unsubscribe();
            } catch (Exception e) {
                logger.warn("Error unsubscribing consumer in multi-topic session {}: {}", 
                           session.sessionId, e.getMessage());
            }
        }
    }
    
    private boolean matchesCorrelationId(ConsumerRecord<String, Object> record, String correlationId) {
        try {
            // Check if the message value contains the correlation ID
            if (record.value() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageMap = (Map<String, Object>) record.value();
                return correlationId.equals(messageMap.get("correlationId"));
            }
            
            // Check headers for correlation ID
            if (record.headers() != null) {
                var correlationHeader = record.headers().lastHeader("correlationId");
                if (correlationHeader != null) {
                    return correlationId.equals(new String(correlationHeader.value()));
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("Error checking correlation ID for record: {}", e.getMessage());
            return false;
        }
    }
    
    private void recordMonitoringStart(ConsumerSession session) {
        TestEvent event = new TestEvent(
            UUID.randomUUID().toString(),
            EventType.KAFKA_MONITORING_STARTED,
            "Started monitoring topic: " + session.topic
        );
        event.setCorrelationId(session.testContext.getCorrelationId());
        event.setStepId(session.testContext.getCurrentStepId());
        event.setSeverity(EventSeverity.INFO);
        event.setData(Map.of(
            "topic", session.topic,
            "sessionId", session.sessionId,
            "timeout", session.timeout.toString()
        ));
        session.testContext.addEvent(event);
    }
    
    private void recordMessageReceived(ConsumerRecord<String, Object> record, ConsumerSession session) {
        ServiceInteraction interaction = createConsumerInteraction(record, session.testContext);
        session.testContext.addInteraction(interaction);
        
        TestEvent event = new TestEvent(
            UUID.randomUUID().toString(),
            EventType.KAFKA_MESSAGE_RECEIVED,
            "Received message from topic: " + record.topic()
        );
        event.setCorrelationId(session.testContext.getCorrelationId());
        event.setStepId(session.testContext.getCurrentStepId());
        event.setSeverity(EventSeverity.INFO);
        event.setData(Map.of(
            "topic", record.topic(),
            "partition", record.partition(),
            "offset", record.offset(),
            "key", record.key(),
            "timestamp", record.timestamp()
        ));
        session.testContext.addEvent(event);
    }
    
    private void recordMonitoringComplete(ConsumerSession session, int messageCount) {
        TestEvent event = new TestEvent(
            UUID.randomUUID().toString(),
            EventType.KAFKA_MONITORING_COMPLETED,
            "Completed monitoring topic: " + session.topic
        );
        event.setCorrelationId(session.testContext.getCorrelationId());
        event.setStepId(session.testContext.getCurrentStepId());
        event.setSeverity(EventSeverity.INFO);
        event.setData(Map.of(
            "topic", session.topic,
            "sessionId", session.sessionId,
            "messagesCollected", messageCount
        ));
        session.testContext.addEvent(event);
    }
    
    private void recordMonitoringError(ConsumerSession session, Exception error) {
        TestEvent event = new TestEvent(
            UUID.randomUUID().toString(),
            EventType.KAFKA_MONITORING_FAILED,
            "Error monitoring topic: " + error.getMessage()
        );
        event.setCorrelationId(session.testContext.getCorrelationId());
        event.setStepId(session.testContext.getCurrentStepId());
        event.setSeverity(EventSeverity.ERROR);
        event.setData(Map.of(
            "topic", session.topic,
            "sessionId", session.sessionId,
            "error", error.getMessage()
        ));
        session.testContext.addEvent(event);
    }
    
    private ServiceInteraction createConsumerInteraction(ConsumerRecord<String, Object> record, TestContext testContext) {
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("kafka-topic-" + record.topic());
        interaction.setType(InteractionType.KAFKA_CONSUMER);
        interaction.setCorrelationId(testContext.getCorrelationId());
        interaction.setStepId(testContext.getCurrentStepId());
        interaction.setStatus(InteractionStatus.SUCCESS);
        interaction.setTimestamp(Instant.now());
        interaction.setResponse(record.value());
        interaction.setMetadata(Map.of(
            "partition", record.partition(),
            "offset", record.offset(),
            "key", record.key(),
            "timestamp", record.timestamp()
        ));
        return interaction;
    }
    
    // Inner classes for session management
    private static class ConsumerSession {
        final String sessionId;
        final KafkaConsumer<String, Object> consumer;
        final String topic;
        final Predicate<ConsumerRecord<String, Object>> messageFilter;
        final TestContext testContext;
        final CompletableFuture<List<ConsumerRecord<String, Object>>> future;
        final Duration timeout;
        final AtomicBoolean stopped = new AtomicBoolean(false);
        
        ConsumerSession(String sessionId, KafkaConsumer<String, Object> consumer, String topic,
                       Predicate<ConsumerRecord<String, Object>> messageFilter, TestContext testContext,
                       CompletableFuture<List<ConsumerRecord<String, Object>>> future, Duration timeout) {
            this.sessionId = sessionId;
            this.consumer = consumer;
            this.topic = topic;
            this.messageFilter = messageFilter;
            this.testContext = testContext;
            this.future = future;
            this.timeout = timeout;
        }
        
        void stop() {
            stopped.set(true);
        }
    }
    
    private static class MultiTopicConsumerSession {
        final String sessionId;
        final KafkaConsumer<String, Object> consumer;
        final List<String> topics;
        final String correlationId;
        final TestContext testContext;
        final CompletableFuture<Map<String, List<ConsumerRecord<String, Object>>>> future;
        final Duration timeout;
        final AtomicBoolean stopped = new AtomicBoolean(false);
        
        MultiTopicConsumerSession(String sessionId, KafkaConsumer<String, Object> consumer, List<String> topics,
                                String correlationId, TestContext testContext,
                                CompletableFuture<Map<String, List<ConsumerRecord<String, Object>>>> future,
                                Duration timeout) {
            this.sessionId = sessionId;
            this.consumer = consumer;
            this.topics = topics;
            this.correlationId = correlationId;
            this.testContext = testContext;
            this.future = future;
            this.timeout = timeout;
        }
        
        void stop() {
            stopped.set(true);
        }
    }
}