package com.agentic.e2etester.integration.kafka;

import com.agentic.e2etester.model.TestContext;
import com.agentic.e2etester.model.ServiceInteraction;
import com.agentic.e2etester.model.TestEvent;
import com.agentic.e2etester.model.InteractionType;
import com.agentic.e2etester.model.InteractionStatus;
import com.agentic.e2etester.model.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for KafkaTestProducer using embedded Kafka.
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"test-topic", "order-events", "payment-events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext
class KafkaTestProducerIntegrationTest {
    
    private static final String TEST_TOPIC = "test-topic";
    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    
    private KafkaTestProducer kafkaTestProducer;
    private KafkaConsumer<String, Object> testConsumer;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Create producer
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        DefaultKafkaProducerFactory<String, Object> producerFactory = 
            new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        
        kafkaTestProducer = new KafkaTestProducer(kafkaTemplate, objectMapper);
        
        // Create consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        
        testConsumer = new KafkaConsumer<>(consumerProps);
    }
    
    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }
    
    @Test
    void shouldSendTestEventSuccessfully() throws Exception {
        // Given
        TestContext testContext = new TestContext("test-correlation-123");
        testContext.setCurrentStepId("step-1");
        
        Map<String, Object> testEvent = Map.of(
            "orderId", "order-123",
            "customerId", "customer-456",
            "amount", 99.99
        );
        
        // Subscribe consumer to topic
        testConsumer.subscribe(Collections.singletonList(TEST_TOPIC));
        
        // When
        CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> future = 
            kafkaTestProducer.sendTestEvent(TEST_TOPIC, testEvent, testContext);
        
        // Then
        var sendResult = future.get(10, TimeUnit.SECONDS);
        assertThat(sendResult).isNotNull();
        assertThat(sendResult.getRecordMetadata().topic()).isEqualTo(TEST_TOPIC);
        
        // Verify the message was received
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertThat(records).isNotEmpty();
        
        ConsumerRecord<String, Object> record = records.iterator().next();
        assertThat(record.topic()).isEqualTo(TEST_TOPIC);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> receivedEvent = (Map<String, Object>) record.value();
        assertThat(receivedEvent).containsEntry("orderId", "order-123");
        assertThat(receivedEvent).containsEntry("customerId", "customer-456");
        assertThat(receivedEvent).containsEntry("correlationId", "test-correlation-123");
        assertThat(receivedEvent).containsKey("testTimestamp");
        
        // Verify interaction was recorded
        List<ServiceInteraction> interactions = testContext.getInteractions();
        assertThat(interactions).hasSize(1);
        
        ServiceInteraction interaction = interactions.get(0);
        assertThat(interaction.getServiceId()).isEqualTo("kafka-topic-" + TEST_TOPIC);
        assertThat(interaction.getType()).isEqualTo(InteractionType.KAFKA_PRODUCER);
        assertThat(interaction.getStatus()).isEqualTo(InteractionStatus.SUCCESS);
        assertThat(interaction.getCorrelationId()).isEqualTo("test-correlation-123");
        assertThat(interaction.getStepId()).isEqualTo("step-1");
        assertThat(interaction.getResponseTimeMs()).isGreaterThan(0L);
        
        // Verify event was recorded
        List<TestEvent> events = testContext.getEvents();
        assertThat(events).hasSize(1);
        
        TestEvent event = events.get(0);
        assertThat(event.getType()).isEqualTo(EventType.KAFKA_MESSAGE_SENT);
        assertThat(event.getCorrelationId()).isEqualTo("test-correlation-123");
        assertThat(event.getStepId()).isEqualTo("step-1");
    }
    
    @Test
    void shouldSendTestEventWithHeaders() throws Exception {
        // Given
        TestContext testContext = new TestContext("test-correlation-456");
        testContext.setCurrentStepId("step-2");
        
        Map<String, Object> testEvent = Map.of(
            "eventType", "ORDER_CREATED",
            "data", Map.of("orderId", "order-789")
        );
        
        Map<String, String> headers = Map.of(
            "source", "order-service",
            "version", "1.0"
        );
        
        // Subscribe consumer to topic
        testConsumer.subscribe(Collections.singletonList(ORDER_EVENTS_TOPIC));
        
        // When
        CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> future = 
            kafkaTestProducer.sendTestEventWithHeaders(ORDER_EVENTS_TOPIC, testEvent, headers, testContext);
        
        // Then
        var sendResult = future.get(10, TimeUnit.SECONDS);
        assertThat(sendResult).isNotNull();
        
        // Verify the message was received
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertThat(records).isNotEmpty();
        
        ConsumerRecord<String, Object> record = records.iterator().next();
        @SuppressWarnings("unchecked")
        Map<String, Object> receivedEvent = (Map<String, Object>) record.value();
        assertThat(receivedEvent).containsEntry("eventType", "ORDER_CREATED");
        assertThat(receivedEvent).containsEntry("correlationId", "test-correlation-456");
        
        // Verify interaction metadata includes headers
        ServiceInteraction interaction = testContext.getInteractions().get(0);
        assertThat(interaction.getMetadata()).isEqualTo(headers);
    }
    
    @Test
    void shouldSendBatchTestEvents() throws Exception {
        // Given
        TestContext testContext = new TestContext("batch-correlation-789");
        testContext.setCurrentStepId("batch-step");
        
        Map<String, Object> events = Map.of(
            ORDER_EVENTS_TOPIC, Map.of("orderId", "order-100", "status", "created"),
            PAYMENT_EVENTS_TOPIC, Map.of("paymentId", "payment-200", "amount", 150.00)
        );
        
        // Subscribe consumer to both topics
        testConsumer.subscribe(Arrays.asList(ORDER_EVENTS_TOPIC, PAYMENT_EVENTS_TOPIC));
        
        // When
        CompletableFuture<Void> future = kafkaTestProducer.sendBatchTestEvents(events, testContext);
        
        // Then
        future.get(10, TimeUnit.SECONDS);
        
        // Verify messages were received
        Set<String> receivedTopics = new HashSet<>();
        long endTime = System.currentTimeMillis() + 10000; // 10 seconds timeout
        
        while (receivedTopics.size() < 2 && System.currentTimeMillis() < endTime) {
            ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, Object> record : records) {
                receivedTopics.add(record.topic());
                
                @SuppressWarnings("unchecked")
                Map<String, Object> receivedEvent = (Map<String, Object>) record.value();
                assertThat(receivedEvent).containsEntry("correlationId", "batch-correlation-789");
            }
        }
        
        assertThat(receivedTopics).containsExactlyInAnyOrder(ORDER_EVENTS_TOPIC, PAYMENT_EVENTS_TOPIC);
        
        // Verify interactions were recorded for both events
        List<ServiceInteraction> interactions = testContext.getInteractions();
        assertThat(interactions).hasSize(2);
        
        Set<String> interactionServices = interactions.stream()
            .map(ServiceInteraction::getServiceId)
            .collect(java.util.stream.Collectors.toSet());
        
        assertThat(interactionServices).containsExactlyInAnyOrder(
            "kafka-topic-" + ORDER_EVENTS_TOPIC,
            "kafka-topic-" + PAYMENT_EVENTS_TOPIC
        );
    }
    
    @Test
    void shouldHandleProducerFailureGracefully() throws Exception {
        // Given
        TestContext testContext = new TestContext("failure-correlation-999");
        testContext.setCurrentStepId("failure-step");
        
        // Create a producer with invalid configuration to force failure
        Map<String, Object> invalidProps = new HashMap<>();
        invalidProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "invalid:9999");
        invalidProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        invalidProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        invalidProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
        invalidProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2000);
        
        DefaultKafkaProducerFactory<String, Object> invalidProducerFactory = 
            new DefaultKafkaProducerFactory<>(invalidProps);
        KafkaTemplate<String, Object> invalidKafkaTemplate = new KafkaTemplate<>(invalidProducerFactory);
        
        KafkaTestProducer failingProducer = new KafkaTestProducer(invalidKafkaTemplate, objectMapper);
        
        Map<String, Object> testEvent = Map.of("test", "data");
        
        // When
        CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> future = 
            failingProducer.sendTestEvent(TEST_TOPIC, testEvent, testContext);
        
        // Then
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(Exception.class);
        
        // Verify failure was recorded
        List<ServiceInteraction> interactions = testContext.getInteractions();
        assertThat(interactions).hasSize(1);
        
        ServiceInteraction interaction = interactions.get(0);
        assertThat(interaction.getStatus()).isEqualTo(InteractionStatus.FAILURE);
        assertThat(interaction.getErrorMessage()).isNotNull();
        
        // Verify failure event was recorded
        List<TestEvent> events = testContext.getEvents();
        assertThat(events).hasSize(1);
        
        TestEvent event = events.get(0);
        assertThat(event.getType()).isEqualTo(EventType.KAFKA_MESSAGE_FAILED);
    }
}