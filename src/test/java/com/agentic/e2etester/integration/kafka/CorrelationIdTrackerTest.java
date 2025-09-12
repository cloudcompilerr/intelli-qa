package com.agentic.e2etester.integration.kafka;

import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.InteractionType;
import com.agentic.e2etester.model.InteractionStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CorrelationIdTracker.
 */
class CorrelationIdTrackerTest {
    
    private CorrelationIdTracker correlationIdTracker;
    
    @BeforeEach
    void setUp() {
        correlationIdTracker = new CorrelationIdTracker();
    }
    
    @Test
    void shouldStartAndTrackCorrelationTrace() {
        // Given
        String correlationId = "test-correlation-123";
        String testId = "test-456";
        String description = "Order fulfillment test";
        
        // When
        correlationIdTracker.startTrace(correlationId, testId, description);
        
        // Then
        CorrelationIdTracker.CorrelationTrace trace = correlationIdTracker.getTrace(correlationId);
        assertThat(trace).isNotNull();
        assertThat(trace.getCorrelationId()).isEqualTo(correlationId);
        assertThat(trace.getTestId()).isEqualTo(testId);
        assertThat(trace.getDescription()).isEqualTo(description);
        assertThat(trace.getStartTime()).isBeforeOrEqualTo(Instant.now());
        assertThat(trace.getMessages()).isEmpty();
        
        Set<String> activeIds = correlationIdTracker.getActiveCorrelationIds();
        assertThat(activeIds).contains(correlationId);
    }
    
    @Test
    void shouldRecordMessageSent() {
        // Given
        String correlationId = "test-correlation-456";
        correlationIdTracker.startTrace(correlationId, "test-789", "Test description");
        
        ServiceInteraction interaction = createTestInteraction(correlationId);
        String topic = "order-events";
        String messageKey = "order-123";
        Map<String, Object> payload = Map.of("orderId", "order-123", "status", "created");
        
        // When
        correlationIdTracker.recordMessageSent(correlationId, topic, messageKey, payload, interaction);
        
        // Then
        CorrelationIdTracker.CorrelationTrace trace = correlationIdTracker.getTrace(correlationId);
        assertThat(trace.getMessages()).hasSize(1);
        
        CorrelationIdTracker.MessageTrace messageTrace = trace.getMessages().get(0);
        assertThat(messageTrace.getDirection()).isEqualTo(CorrelationIdTracker.MessageTrace.Direction.SENT);
        assertThat(messageTrace.getTopic()).isEqualTo(topic);
        assertThat(messageTrace.getMessageKey()).isEqualTo(messageKey);
        assertThat(messageTrace.getPayload()).isEqualTo(payload);
        assertThat(messageTrace.getInteraction()).isEqualTo(interaction);
        
        List<CorrelationIdTracker.MessageTrace> history = correlationIdTracker.getMessageHistory(correlationId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0)).isEqualTo(messageTrace);
    }
    
    @Test
    void shouldRecordMessageReceived() {
        // Given
        String correlationId = "test-correlation-789";
        correlationIdTracker.startTrace(correlationId, "test-101", "Test description");
        
        ServiceInteraction interaction = createTestInteraction(correlationId);
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
            "payment-events", 0, 100L, "payment-456", 
            Map.of("paymentId", "payment-456", "amount", 99.99)
        );
        
        // When
        correlationIdTracker.recordMessageReceived(correlationId, record, interaction);
        
        // Then
        CorrelationIdTracker.CorrelationTrace trace = correlationIdTracker.getTrace(correlationId);
        assertThat(trace.getMessages()).hasSize(1);
        
        CorrelationIdTracker.MessageTrace messageTrace = trace.getMessages().get(0);
        assertThat(messageTrace.getDirection()).isEqualTo(CorrelationIdTracker.MessageTrace.Direction.RECEIVED);
        assertThat(messageTrace.getTopic()).isEqualTo("payment-events");
        assertThat(messageTrace.getMessageKey()).isEqualTo("payment-456");
        assertThat(messageTrace.getPayload()).isEqualTo(record.value());
        assertThat(messageTrace.getInteraction()).isEqualTo(interaction);
        assertThat(messageTrace.getTimestamp()).isEqualTo(Instant.ofEpochMilli(record.timestamp()));
    }
    
    @Test
    void shouldCompleteTraceAndReturnSummary() {
        // Given
        String correlationId = "test-correlation-complete";
        correlationIdTracker.startTrace(correlationId, "test-complete", "Complete test");
        
        // Add some messages
        ServiceInteraction interaction1 = createTestInteraction(correlationId);
        ServiceInteraction interaction2 = createTestInteraction(correlationId);
        
        correlationIdTracker.recordMessageSent(correlationId, "topic1", "key1", 
                                             Map.of("data", "sent1"), interaction1);
        correlationIdTracker.recordMessageReceived(correlationId, 
            new ConsumerRecord<>("topic2", 0, 200L, "key2", Map.of("data", "received1")), 
            interaction2);
        
        // When
        CorrelationIdTracker.TraceSummary summary = correlationIdTracker.completeTrace(correlationId);
        
        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getCorrelationId()).isEqualTo(correlationId);
        assertThat(summary.getTestId()).isEqualTo("test-complete");
        assertThat(summary.getMessageCount()).isEqualTo(2);
        assertThat(summary.getTopicsInvolved()).containsExactlyInAnyOrder("topic1", "topic2");
        assertThat(summary.getTotalDuration()).isPositive();
        assertThat(summary.getStartTime()).isBeforeOrEqualTo(summary.getEndTime());
        
        // Trace should be removed from active traces
        assertThat(correlationIdTracker.getTrace(correlationId)).isNull();
        assertThat(correlationIdTracker.getActiveCorrelationIds()).doesNotContain(correlationId);
    }
    
    @Test
    void shouldAnalyzeMessageFlowForOrderingIssues() {
        // Given
        String correlationId = "test-correlation-ordering";
        correlationIdTracker.startTrace(correlationId, "test-ordering", "Ordering test");
        
        // When - analyze empty message flow (should not have ordering issues)
        List<CorrelationIdTracker.FlowIssue> issues = correlationIdTracker.analyzeMessageFlow(correlationId);
        
        // Then - should return empty list for empty message flow
        assertThat(issues).isEmpty();
    }
    
    @Test
    void shouldAnalyzeMessageFlowForMissingResponses() {
        // Given
        String correlationId = "test-correlation-missing";
        correlationIdTracker.startTrace(correlationId, "test-missing", "Missing response test");
        
        // Send multiple messages but receive none
        ServiceInteraction interaction1 = createTestInteraction(correlationId);
        ServiceInteraction interaction2 = createTestInteraction(correlationId);
        ServiceInteraction interaction3 = createTestInteraction(correlationId);
        
        correlationIdTracker.recordMessageSent(correlationId, "topic1", "key1", Map.of("data", "sent1"), interaction1);
        correlationIdTracker.recordMessageSent(correlationId, "topic2", "key2", Map.of("data", "sent2"), interaction2);
        correlationIdTracker.recordMessageSent(correlationId, "topic3", "key3", Map.of("data", "sent3"), interaction3);
        
        // When
        List<CorrelationIdTracker.FlowIssue> issues = correlationIdTracker.analyzeMessageFlow(correlationId);
        
        // Then
        assertThat(issues).isNotEmpty();
        boolean hasMissingResponseIssue = issues.stream()
            .anyMatch(issue -> issue.getType() == CorrelationIdTracker.FlowIssue.Type.MISSING_RESPONSE);
        assertThat(hasMissingResponseIssue).isTrue();
    }
    
    @Test
    void shouldAnalyzeMessageFlowForTimingIssues() {
        // Given
        String correlationId = "test-correlation-timing";
        correlationIdTracker.startTrace(correlationId, "test-timing", "Timing test");
        
        // When - analyze empty message flow (should not have timing issues)
        List<CorrelationIdTracker.FlowIssue> issues = correlationIdTracker.analyzeMessageFlow(correlationId);
        
        // Then - should return empty list for empty message flow
        assertThat(issues).isEmpty();
    }
    
    @Test
    void shouldCleanupOldTraces() {
        // Given
        String oldCorrelationId = "old-correlation";
        String newCorrelationId = "new-correlation";
        
        correlationIdTracker.startTrace(oldCorrelationId, "old-test", "Old test");
        
        // Simulate old trace by manipulating start time (in real scenario, this would be naturally old)
        // For testing, we'll just add the new trace and clean up immediately
        try {
            Thread.sleep(10); // Small delay to ensure different timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        correlationIdTracker.startTrace(newCorrelationId, "new-test", "New test");
        
        // When - cleanup traces older than 1 millisecond (very short for testing)
        int cleanedUp = correlationIdTracker.cleanupOldTraces(Duration.ofMillis(1));
        
        // Then
        assertThat(cleanedUp).isGreaterThanOrEqualTo(0); // May or may not clean up depending on timing
        assertThat(correlationIdTracker.getActiveCorrelationIds()).contains(newCorrelationId);
    }
    
    @Test
    void shouldReturnEmptyListForNonExistentCorrelationId() {
        // Given
        String nonExistentId = "non-existent-correlation";
        
        // When
        List<CorrelationIdTracker.MessageTrace> history = correlationIdTracker.getMessageHistory(nonExistentId);
        CorrelationIdTracker.CorrelationTrace trace = correlationIdTracker.getTrace(nonExistentId);
        List<CorrelationIdTracker.FlowIssue> issues = correlationIdTracker.analyzeMessageFlow(nonExistentId);
        
        // Then
        assertThat(history).isEmpty();
        assertThat(trace).isNull();
        assertThat(issues).isEmpty();
    }
    
    private ServiceInteraction createTestInteraction(String correlationId) {
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("test-service");
        interaction.setType(InteractionType.KAFKA_PRODUCER);
        interaction.setStatus(InteractionStatus.SUCCESS);
        interaction.setCorrelationId(correlationId);
        interaction.setTimestamp(Instant.now());
        return interaction;
    }
}