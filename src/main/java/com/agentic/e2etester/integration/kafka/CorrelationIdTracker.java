package com.agentic.e2etester.integration.kafka;

import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.TestEvent;
import com.agentic.e2etester.model.EventType;
import com.agentic.e2etester.model.EventSeverity;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Tracks correlation IDs across Kafka messages to maintain test flow visibility.
 * Provides utilities for correlating messages across different topics and services.
 */
@Component
public class CorrelationIdTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdTracker.class);
    
    private final Map<String, CorrelationTrace> activeTraces;
    private final Map<String, List<MessageTrace>> messageHistory;
    
    public CorrelationIdTracker() {
        this.activeTraces = new ConcurrentHashMap<>();
        this.messageHistory = new ConcurrentHashMap<>();
    }
    
    /**
     * Starts tracking a new correlation ID.
     *
     * @param correlationId the correlation ID to track
     * @param testId the associated test ID
     * @param description optional description of the trace
     */
    public void startTrace(String correlationId, String testId, String description) {
        CorrelationTrace trace = new CorrelationTrace(correlationId, testId, description);
        activeTraces.put(correlationId, trace);
        messageHistory.put(correlationId, new CopyOnWriteArrayList<>());
        
        logger.debug("Started correlation trace for ID: {} (test: {})", correlationId, testId);
    }
    
    /**
     * Records a message sent with the correlation ID.
     *
     * @param correlationId the correlation ID
     * @param topic the topic the message was sent to
     * @param messageKey the message key
     * @param payload the message payload
     * @param interaction the service interaction record
     */
    public void recordMessageSent(String correlationId, String topic, String messageKey, 
                                Object payload, ServiceInteraction interaction) {
        CorrelationTrace trace = activeTraces.get(correlationId);
        if (trace != null) {
            MessageTrace messageTrace = new MessageTrace(
                MessageTrace.Direction.SENT,
                topic,
                messageKey,
                payload,
                interaction,
                Instant.now()
            );
            
            trace.addMessage(messageTrace);
            messageHistory.get(correlationId).add(messageTrace);
            
            logger.debug("Recorded sent message for correlation ID {} to topic: {}", correlationId, topic);
        }
    }
    
    /**
     * Records a message received with the correlation ID.
     *
     * @param correlationId the correlation ID
     * @param record the consumer record
     * @param interaction the service interaction record
     */
    public void recordMessageReceived(String correlationId, ConsumerRecord<String, Object> record, 
                                    ServiceInteraction interaction) {
        CorrelationTrace trace = activeTraces.get(correlationId);
        if (trace != null) {
            MessageTrace messageTrace = new MessageTrace(
                MessageTrace.Direction.RECEIVED,
                record.topic(),
                record.key(),
                record.value(),
                interaction,
                Instant.ofEpochMilli(record.timestamp())
            );
            
            trace.addMessage(messageTrace);
            messageHistory.get(correlationId).add(messageTrace);
            
            logger.debug("Recorded received message for correlation ID {} from topic: {}", 
                        correlationId, record.topic());
        }
    }
    
    /**
     * Gets the current trace for a correlation ID.
     *
     * @param correlationId the correlation ID
     * @return the correlation trace, or null if not found
     */
    public CorrelationTrace getTrace(String correlationId) {
        return activeTraces.get(correlationId);
    }
    
    /**
     * Gets the message history for a correlation ID.
     *
     * @param correlationId the correlation ID
     * @return list of message traces
     */
    public List<MessageTrace> getMessageHistory(String correlationId) {
        return messageHistory.getOrDefault(correlationId, Collections.emptyList());
    }
    
    /**
     * Completes a correlation trace.
     *
     * @param correlationId the correlation ID to complete
     * @return the completed trace summary
     */
    public TraceSummary completeTrace(String correlationId) {
        CorrelationTrace trace = activeTraces.remove(correlationId);
        List<MessageTrace> messages = messageHistory.get(correlationId);
        
        if (trace != null && messages != null) {
            TraceSummary summary = new TraceSummary(trace, messages);
            logger.debug("Completed correlation trace for ID: {} with {} messages", 
                        correlationId, messages.size());
            return summary;
        }
        
        return null;
    }
    
    /**
     * Analyzes message flow for potential issues.
     *
     * @param correlationId the correlation ID to analyze
     * @return list of potential issues found
     */
    public List<FlowIssue> analyzeMessageFlow(String correlationId) {
        List<MessageTrace> messages = messageHistory.get(correlationId);
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<FlowIssue> issues = new ArrayList<>();
        
        // Check for message ordering issues
        checkMessageOrdering(messages, issues);
        
        // Check for missing responses
        checkMissingResponses(messages, issues);
        
        // Check for timing issues
        checkTimingIssues(messages, issues);
        
        return issues;
    }
    
    /**
     * Gets all active correlation IDs being tracked.
     *
     * @return set of active correlation IDs
     */
    public Set<String> getActiveCorrelationIds() {
        return new HashSet<>(activeTraces.keySet());
    }
    
    /**
     * Cleans up old traces that are no longer needed.
     *
     * @param olderThan duration to consider traces as old
     * @return number of traces cleaned up
     */
    public int cleanupOldTraces(Duration olderThan) {
        Instant cutoff = Instant.now().minus(olderThan);
        
        List<String> toRemove = activeTraces.entrySet().stream()
            .filter(entry -> entry.getValue().getStartTime().isBefore(cutoff))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        toRemove.forEach(correlationId -> {
            activeTraces.remove(correlationId);
            messageHistory.remove(correlationId);
        });
        
        logger.debug("Cleaned up {} old correlation traces", toRemove.size());
        return toRemove.size();
    }
    
    private void checkMessageOrdering(List<MessageTrace> messages, List<FlowIssue> issues) {
        // Check if messages are in chronological order
        for (int i = 1; i < messages.size(); i++) {
            MessageTrace current = messages.get(i);
            MessageTrace previous = messages.get(i - 1);
            
            if (current.getTimestamp().isBefore(previous.getTimestamp())) {
                issues.add(new FlowIssue(
                    FlowIssue.Type.ORDERING_ISSUE,
                    "Message received out of order",
                    Map.of(
                        "currentMessage", current.getTopic() + ":" + current.getMessageKey(),
                        "previousMessage", previous.getTopic() + ":" + previous.getMessageKey()
                    )
                ));
            }
        }
    }
    
    private void checkMissingResponses(List<MessageTrace> messages, List<FlowIssue> issues) {
        // Simple heuristic: check if we have more sent messages than received
        long sentCount = messages.stream()
            .filter(m -> m.getDirection() == MessageTrace.Direction.SENT)
            .count();
        long receivedCount = messages.stream()
            .filter(m -> m.getDirection() == MessageTrace.Direction.RECEIVED)
            .count();
        
        if (sentCount > receivedCount + 1) { // Allow for some async processing
            issues.add(new FlowIssue(
                FlowIssue.Type.MISSING_RESPONSE,
                "Potential missing responses detected",
                Map.of("sentCount", sentCount, "receivedCount", receivedCount)
            ));
        }
    }
    
    private void checkTimingIssues(List<MessageTrace> messages, List<FlowIssue> issues) {
        // Check for unusually long gaps between messages
        Duration maxGap = Duration.ofMinutes(5); // Configurable threshold
        
        for (int i = 1; i < messages.size(); i++) {
            MessageTrace current = messages.get(i);
            MessageTrace previous = messages.get(i - 1);
            
            Duration gap = Duration.between(previous.getTimestamp(), current.getTimestamp());
            if (gap.compareTo(maxGap) > 0) {
                issues.add(new FlowIssue(
                    FlowIssue.Type.TIMING_ISSUE,
                    "Long gap between messages detected",
                    Map.of(
                        "gapDuration", gap.toString(),
                        "betweenMessages", previous.getTopic() + " -> " + current.getTopic()
                    )
                ));
            }
        }
    }
    
    // Inner classes for data structures
    public static class CorrelationTrace {
        private final String correlationId;
        private final String testId;
        private final String description;
        private final Instant startTime;
        private final List<MessageTrace> messages;
        
        public CorrelationTrace(String correlationId, String testId, String description) {
            this.correlationId = correlationId;
            this.testId = testId;
            this.description = description;
            this.startTime = Instant.now();
            this.messages = new CopyOnWriteArrayList<>();
        }
        
        public void addMessage(MessageTrace message) {
            messages.add(message);
        }
        
        // Getters
        public String getCorrelationId() { return correlationId; }
        public String getTestId() { return testId; }
        public String getDescription() { return description; }
        public Instant getStartTime() { return startTime; }
        public List<MessageTrace> getMessages() { return new ArrayList<>(messages); }
    }
    
    public static class MessageTrace {
        public enum Direction { SENT, RECEIVED }
        
        private final Direction direction;
        private final String topic;
        private final String messageKey;
        private final Object payload;
        private final ServiceInteraction interaction;
        private final Instant timestamp;
        
        public MessageTrace(Direction direction, String topic, String messageKey, Object payload,
                          ServiceInteraction interaction, Instant timestamp) {
            this.direction = direction;
            this.topic = topic;
            this.messageKey = messageKey;
            this.payload = payload;
            this.interaction = interaction;
            this.timestamp = timestamp;
        }
        
        // Getters
        public Direction getDirection() { return direction; }
        public String getTopic() { return topic; }
        public String getMessageKey() { return messageKey; }
        public Object getPayload() { return payload; }
        public ServiceInteraction getInteraction() { return interaction; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    public static class TraceSummary {
        private final String correlationId;
        private final String testId;
        private final Instant startTime;
        private final Instant endTime;
        private final int messageCount;
        private final List<String> topicsInvolved;
        private final Duration totalDuration;
        
        public TraceSummary(CorrelationTrace trace, List<MessageTrace> messages) {
            this.correlationId = trace.getCorrelationId();
            this.testId = trace.getTestId();
            this.startTime = trace.getStartTime();
            this.endTime = Instant.now();
            this.messageCount = messages.size();
            this.topicsInvolved = messages.stream()
                .map(MessageTrace::getTopic)
                .distinct()
                .collect(Collectors.toList());
            this.totalDuration = Duration.between(startTime, endTime);
        }
        
        // Getters
        public String getCorrelationId() { return correlationId; }
        public String getTestId() { return testId; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public int getMessageCount() { return messageCount; }
        public List<String> getTopicsInvolved() { return topicsInvolved; }
        public Duration getTotalDuration() { return totalDuration; }
    }
    
    public static class FlowIssue {
        public enum Type { ORDERING_ISSUE, MISSING_RESPONSE, TIMING_ISSUE }
        
        private final Type type;
        private final String description;
        private final Map<String, Object> details;
        
        public FlowIssue(Type type, String description, Map<String, Object> details) {
            this.type = type;
            this.description = description;
            this.details = details;
        }
        
        // Getters
        public Type getType() { return type; }
        public String getDescription() { return description; }
        public Map<String, Object> getDetails() { return details; }
    }
}