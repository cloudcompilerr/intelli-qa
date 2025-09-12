package com.agentic.e2etester.integration.kafka;

import com.agentic.e2etester.model.TestContext;
import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.TestEvent;
import com.agentic.e2etester.model.InteractionType;
import com.agentic.e2etester.model.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for KafkaTestConsumer using embedded Kafka.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"consumer-test-topic", "multi-topic-1", "multi-topic-2"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9093",
        "port=9093"
    }
)
@DirtiesContext
class KafkaTestConsumerIntegrationTest {
    
    private static final String CONSUMER_TEST_TOPIC = "consumer-test-topic";
    private static final String MULTI_TOPIC_1 = "multi-topic-1";
    private static final String MULTI_TOPIC_2 = "multi-topic-2";
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    
    private KafkaTestConsumer kafkaTestConsumer;
    private KafkaConsumer<String, Object> testConsumer;
    private KafkaProducer<String, Object> testProducer;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        
        objectMapper = new ObjectMapper();
        kafkaTestConsumer = new KafkaTestConsumer(objectMapper);
        
        // Create consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-consumer-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        
        testConsumer = new KafkaConsumer<>(consumerProps);
        
        // Create producer
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        testProducer = new KafkaProducer<>(producerProps);
    }
    
    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
        if (testProducer != null) {
            testProducer.close();
        }
        kafkaTestConsumer.stopAllMonitoring();
    }
    
    @Test
    void shouldMonitorTopicForCorrelatedMessages() throws Exception {
        // Given
        String correlationId = "monitor-correlation-123";
        TestContext testContext = new TestContext(correlationId);
        testContext.setCurrentStepId("monitor-step");
        
        // Start monitoring
        CompletableFuture<List<ConsumerRecord<String, Object>>> monitoringFuture = 
            kafkaTestConsumer.monitorTopic(testConsumer, CONSUMER_TEST_TOPIC, correlationId, 
                                         testContext, Duration.ofSeconds(10));
        
        // Give monitoring a moment to start
        Thread.sleep(1000);
        
        // Send test messages
        Map<String, Object> correlatedMessage = Map.of(
            "correlationId", correlationId,
            "data", "test-data-1"
        );
        
        Map<String, Object> nonCorrelatedMessage = Map.of(
            "correlationId", "different-correlation",
            "data", "test-data-2"
        );
        
        testProducer.send(new ProducerRecord<>(CONSUMER_TEST_TOPIC, "key1", correlatedMessage));
        testProducer.send(new ProducerRecord<>(CONSUMER_TEST_TOPIC, "key2", nonCorrelatedMessage));
        testProducer.send(new ProducerRecord<>(CONSUMER_TEST_TOPIC, "key3", correlatedMessage));
        testProducer.flush();
        
        // When
        List<ConsumerRecord<String, Object>> collectedMessages = monitoringFuture.get(15, TimeUnit.SECONDS);
        
        // Then
        assertThat(collectedMessages).hasSize(2); // Only correlated messages
        
        for (ConsumerRecord<String, Object> record : collectedMessages) {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) record.value();
            assertThat(message).containsEntry("correlationId", correlationId);
        }
        
        // Verify interactions were recorded
        List<ServiceInteraction> interactions = testContext.getInteractions();
        assertThat(interactions).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify events were recorded
        List<TestEvent> events = testContext.getEvents();
        assertThat(events).isNotEmpty();
        
        // Should have monitoring started and completed events
        boolean hasStartedEvent = events.stream()
            .anyMatch(e -> e.getType() == EventType.KAFKA_MONITORING_STARTED);
        boolean hasCompletedEvent = events.stream()
            .anyMatch(e -> e.getType() == EventType.KAFKA_MONITORING_COMPLETED);
        
        assertThat(hasStartedEvent).isTrue();
        assertThat(hasCompletedEvent).isTrue();
    }
    
    @Test
    void shouldMonitorTopicWithCustomFilter() throws Exception {
        // Given
        TestContext testContext = new TestContext("filter-correlation-456");
        testContext.setCurrentStepId("filter-step");
        
        // Custom filter for messages with specific event type
        var messageFilter = (java.util.function.Predicate<ConsumerRecord<String, Object>>) record -> {
            if (record.value() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) record.value();
                return "ORDER_CREATED".equals(message.get("eventType"));
            }
            return false;
        };
        
        // Start monitoring with custom filter
        CompletableFuture<List<ConsumerRecord<String, Object>>> monitoringFuture = 
            kafkaTestConsumer.monitorTopicWithFilter(testConsumer, CONSUMER_TEST_TOPIC, 
                                                   messageFilter, testContext, Duration.ofSeconds(10));
        
        // Give monitoring a moment to start
        Thread.sleep(1000);
        
        // Send test messages
        Map<String, Object> orderCreatedMessage = Map.of(
            "eventType", "ORDER_CREATED",
            "orderId", "order-123"
        );
        
        Map<String, Object> orderUpdatedMessage = Map.of(
            "eventType", "ORDER_UPDATED",
            "orderId", "order-456"
        );
        
        testProducer.send(new ProducerRecord<>(CONSUMER_TEST_TOPIC, "key1", orderCreatedMessage));
        testProducer.send(new ProducerRecord<>(CONSUMER_TEST_TOPIC, "key2", orderUpdatedMessage));
        testProducer.send(new ProducerRecord<>(CONSUMER_TEST_TOPIC, "key3", orderCreatedMessage));
        testProducer.flush();
        
        // When
        List<ConsumerRecord<String, Object>> collectedMessages = monitoringFuture.get(15, TimeUnit.SECONDS);
        
        // Then
        assertThat(collectedMessages).hasSize(2); // Only ORDER_CREATED messages
        
        for (ConsumerRecord<String, Object> record : collectedMessages) {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) record.value();
            assertThat(message).containsEntry("eventType", "ORDER_CREATED");
        }
    }
    
    @Test
    void shouldMonitorMultipleTopicsSimultaneously() throws Exception {
        // Given
        String correlationId = "multi-correlation-789";
        TestContext testContext = new TestContext(correlationId);
        testContext.setCurrentStepId("multi-step");
        
        List<String> topics = Arrays.asList(MULTI_TOPIC_1, MULTI_TOPIC_2);
        
        // Start monitoring multiple topics
        CompletableFuture<Map<String, List<ConsumerRecord<String, Object>>>> monitoringFuture = 
            kafkaTestConsumer.monitorMultipleTopics(testConsumer, topics, correlationId, 
                                                  testContext, Duration.ofSeconds(10));
        
        // Give monitoring a moment to start
        Thread.sleep(1000);
        
        // Send messages to both topics
        Map<String, Object> message1 = Map.of(
            "correlationId", correlationId,
            "topic", MULTI_TOPIC_1,
            "data", "data-1"
        );
        
        Map<String, Object> message2 = Map.of(
            "correlationId", correlationId,
            "topic", MULTI_TOPIC_2,
            "data", "data-2"
        );
        
        Map<String, Object> nonCorrelatedMessage = Map.of(
            "correlationId", "different-correlation",
            "topic", MULTI_TOPIC_1,
            "data", "data-3"
        );
        
        testProducer.send(new ProducerRecord<>(MULTI_TOPIC_1, "key1", message1));
        testProducer.send(new ProducerRecord<>(MULTI_TOPIC_2, "key2", message2));
        testProducer.send(new ProducerRecord<>(MULTI_TOPIC_1, "key3", nonCorrelatedMessage));
        testProducer.flush();
        
        // When
        Map<String, List<ConsumerRecord<String, Object>>> collectedMessages = 
            monitoringFuture.get(15, TimeUnit.SECONDS);
        
        // Then
        assertThat(collectedMessages).containsKeys(MULTI_TOPIC_1, MULTI_TOPIC_2);
        assertThat(collectedMessages.get(MULTI_TOPIC_1)).hasSize(1); // Only correlated message
        assertThat(collectedMessages.get(MULTI_TOPIC_2)).hasSize(1); // Only correlated message
        
        // Verify message content
        @SuppressWarnings("unchecked")
        Map<String, Object> receivedMessage1 = (Map<String, Object>) 
            collectedMessages.get(MULTI_TOPIC_1).get(0).value();
        assertThat(receivedMessage1).containsEntry("correlationId", correlationId);
        assertThat(receivedMessage1).containsEntry("topic", MULTI_TOPIC_1);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> receivedMessage2 = (Map<String, Object>) 
            collectedMessages.get(MULTI_TOPIC_2).get(0).value();
        assertThat(receivedMessage2).containsEntry("correlationId", correlationId);
        assertThat(receivedMessage2).containsEntry("topic", MULTI_TOPIC_2);
    }
    
    @Test
    void shouldTimeoutWhenNoMessagesReceived() throws Exception {
        // Given
        String correlationId = "timeout-correlation-999";
        TestContext testContext = new TestContext(correlationId);
        testContext.setCurrentStepId("timeout-step");
        
        // Start monitoring with short timeout
        CompletableFuture<List<ConsumerRecord<String, Object>>> monitoringFuture = 
            kafkaTestConsumer.monitorTopic(testConsumer, CONSUMER_TEST_TOPIC, correlationId, 
                                         testContext, Duration.ofSeconds(2));
        
        // Don't send any messages
        
        // When
        List<ConsumerRecord<String, Object>> collectedMessages = monitoringFuture.get(5, TimeUnit.SECONDS);
        
        // Then
        assertThat(collectedMessages).isEmpty();
        
        // Verify monitoring events were still recorded
        List<TestEvent> events = testContext.getEvents();
        boolean hasStartedEvent = events.stream()
            .anyMatch(e -> e.getType() == EventType.KAFKA_MONITORING_STARTED);
        boolean hasCompletedEvent = events.stream()
            .anyMatch(e -> e.getType() == EventType.KAFKA_MONITORING_COMPLETED);
        
        assertThat(hasStartedEvent).isTrue();
        assertThat(hasCompletedEvent).isTrue();
    }
    
    @Test
    void shouldStopMonitoringWhenRequested() throws Exception {
        // Given
        String correlationId = "stop-correlation-111";
        TestContext testContext = new TestContext(correlationId);
        
        // Start monitoring
        CompletableFuture<List<ConsumerRecord<String, Object>>> monitoringFuture = 
            kafkaTestConsumer.monitorTopic(testConsumer, CONSUMER_TEST_TOPIC, correlationId, 
                                         testContext, Duration.ofSeconds(30)); // Long timeout
        
        // Give monitoring a moment to start
        Thread.sleep(1000);
        
        // When - stop all monitoring
        kafkaTestConsumer.stopAllMonitoring();
        
        // Then - monitoring should complete quickly
        List<ConsumerRecord<String, Object>> collectedMessages = monitoringFuture.get(5, TimeUnit.SECONDS);
        assertThat(collectedMessages).isEmpty();
    }
}