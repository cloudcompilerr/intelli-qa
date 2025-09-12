package com.agentic.e2etester.integration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for test integration components.
 * Provides producers and consumers optimized for testing scenarios.
 */
@Configuration
public class KafkaTestConfiguration {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${agentic.kafka.test.consumer.group-id:agentic-e2e-test-consumer}")
    private String testConsumerGroupId;
    
    @Value("${agentic.kafka.test.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;
    
    @Value("${agentic.kafka.test.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;
    
    @Value("${agentic.kafka.test.producer.acks:all}")
    private String producerAcks;
    
    @Value("${agentic.kafka.test.producer.retries:3}")
    private int producerRetries;
    
    @Value("${agentic.kafka.test.producer.batch-size:16384}")
    private int producerBatchSize;
    
    @Value("${agentic.kafka.test.producer.linger-ms:1}")
    private int producerLingerMs;
    
    /**
     * Producer factory for test event injection.
     */
    @Bean
    public ProducerFactory<String, Object> kafkaTestProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, producerAcks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, producerLingerMs);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Add correlation ID to headers automatically
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * Kafka template for test event production.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTestTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(kafkaTestProducerFactory());
        
        // Set default topic if needed
        // template.setDefaultTopic("test-events");
        
        return template;
    }
    
    /**
     * Consumer factory for test event monitoring.
     */
    @Bean
    public ConsumerFactory<String, Object> kafkaTestConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, testConsumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        
        // Configure JSON deserializer to trust all packages for testing
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    /**
     * Listener container factory for test event monitoring.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaTestListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaTestConsumerFactory());
        
        // Configure for manual acknowledgment to have better control
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Set concurrency for parallel processing
        factory.setConcurrency(3);
        
        // Configure error handling
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());
        
        return factory;
    }
    
    /**
     * Dedicated consumer factory for correlation tracking.
     * Uses a separate consumer group to avoid interference with main consumers.
     */
    @Bean
    public ConsumerFactory<String, Object> correlationTrackingConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, testConsumerGroupId + "-correlation");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // Only new messages for tracking
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        
        // Configure JSON deserializer
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    /**
     * ObjectMapper bean for JSON serialization/deserialization.
     */
    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}