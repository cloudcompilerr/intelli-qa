package com.agentic.e2etester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuration for local LLM integration using Ollama.
 * Provides chat client with retry capabilities for robust AI interactions.
 */
@Configuration
@EnableRetry
public class LLMConfiguration {

    /**
     * Creates a ChatClient with retry capabilities for robust LLM interactions.
     * 
     * @param chatModel the Ollama chat model
     * @return configured ChatClient with retry template
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are an expert software testing assistant specializing in microservices and event-driven architectures. " +
                              "You understand Spring Boot, Kafka, and distributed systems testing patterns. " +
                              "Provide precise, actionable responses focused on test automation and system validation.")
                .build();
    }

    /**
     * Retry template specifically configured for LLM operations.
     * 
     * @return RetryTemplate with exponential backoff for LLM calls
     */
    @Bean("llmRetryTemplate")
    public RetryTemplate llmRetryTemplate() {
        return RetryUtils.DEFAULT_RETRY_TEMPLATE;
    }

    /**
     * ObjectMapper configured for JSON processing in AI responses.
     * 
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        return mapper;
    }

    /**
     * Configuration properties for LLM-specific settings.
     */
    @ConfigurationProperties(prefix = "agentic.llm")
    public static class LLMProperties {
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        private double retryMultiplier = 2.0;
        private long maxRetryDelayMs = 10000;
        private int timeoutSeconds = 30;

        // Getters and setters
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }

        public double getRetryMultiplier() { return retryMultiplier; }
        public void setRetryMultiplier(double retryMultiplier) { this.retryMultiplier = retryMultiplier; }

        public long getMaxRetryDelayMs() { return maxRetryDelayMs; }
        public void setMaxRetryDelayMs(long maxRetryDelayMs) { this.maxRetryDelayMs = maxRetryDelayMs; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}