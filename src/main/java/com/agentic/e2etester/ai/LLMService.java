package com.agentic.e2etester.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service wrapper for LLM interactions with comprehensive error handling and retry logic.
 * Provides robust communication with local Ollama LLM for test scenario processing.
 */
@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    
    private final ChatClient chatClient;
    
    public LLMService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Sends a prompt to the LLM with retry logic and error handling.
     * 
     * @param prompt the prompt to send
     * @return LLM response
     * @throws LLMException if all retry attempts fail
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public LLMResponse sendPrompt(String prompt) {
        try {
            logger.debug("Sending prompt to LLM: {}", prompt.substring(0, Math.min(100, prompt.length())));
            
            long startTime = System.currentTimeMillis();
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            long duration = System.currentTimeMillis() - startTime;
            
            logger.debug("LLM response received in {}ms", duration);
            
            return new LLMResponse(response, duration, true);
            
        } catch (Exception e) {
            logger.error("Error communicating with LLM: {}", e.getMessage(), e);
            throw new LLMException("Failed to get response from LLM", e);
        }
    }

    /**
     * Sends a templated prompt with parameters to the LLM.
     * 
     * @param template the prompt template
     * @param parameters template parameters
     * @return LLM response
     * @throws LLMException if all retry attempts fail
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public LLMResponse sendTemplatedPrompt(String template, Map<String, Object> parameters) {
        try {
            logger.debug("Sending templated prompt to LLM with {} parameters", parameters.size());
            
            PromptTemplate promptTemplate = new PromptTemplate(template);
            Prompt prompt = promptTemplate.create(parameters);
            
            long startTime = System.currentTimeMillis();
            
            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            String response = chatResponse.getResult().getOutput().getContent();
            
            long duration = System.currentTimeMillis() - startTime;
            
            logger.debug("LLM templated response received in {}ms", duration);
            
            return new LLMResponse(response, duration, true);
            
        } catch (Exception e) {
            logger.error("Error sending templated prompt to LLM: {}", e.getMessage(), e);
            throw new LLMException("Failed to get response from LLM with template", e);
        }
    }

    /**
     * Sends a prompt asynchronously to the LLM.
     * 
     * @param prompt the prompt to send
     * @return CompletableFuture with LLM response
     */
    public CompletableFuture<LLMResponse> sendPromptAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> sendPrompt(prompt))
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    logger.error("Async LLM call failed: {}", throwable.getMessage(), throwable);
                    return new LLMResponse("", 0, false, throwable.getMessage());
                });
    }

    /**
     * Validates if the LLM service is available and responsive.
     * 
     * @return true if LLM is available, false otherwise
     */
    public boolean isLLMAvailable() {
        try {
            LLMResponse response = sendPrompt("Hello, are you available?");
            return response.isSuccess() && response.getContent() != null && !response.getContent().trim().isEmpty();
        } catch (Exception e) {
            logger.warn("LLM availability check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the health status of the LLM service.
     * 
     * @return health status information
     */
    public LLMHealthStatus getHealthStatus() {
        try {
            long startTime = System.currentTimeMillis();
            boolean available = isLLMAvailable();
            long responseTime = System.currentTimeMillis() - startTime;
            
            return new LLMHealthStatus(available, responseTime, available ? "LLM is responsive" : "LLM is not responding");
        } catch (Exception e) {
            return new LLMHealthStatus(false, -1, "Health check failed: " + e.getMessage());
        }
    }

    /**
     * Represents an LLM response with metadata.
     */
    public static class LLMResponse {
        private final String content;
        private final long responseTimeMs;
        private final boolean success;
        private final String errorMessage;

        public LLMResponse(String content, long responseTimeMs, boolean success) {
            this(content, responseTimeMs, success, null);
        }

        public LLMResponse(String content, long responseTimeMs, boolean success, String errorMessage) {
            this.content = content;
            this.responseTimeMs = responseTimeMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getContent() { return content; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Represents LLM health status.
     */
    public static class LLMHealthStatus {
        private final boolean available;
        private final long responseTimeMs;
        private final String message;

        public LLMHealthStatus(boolean available, long responseTimeMs, String message) {
            this.available = available;
            this.responseTimeMs = responseTimeMs;
            this.message = message;
        }

        public boolean isAvailable() { return available; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public String getMessage() { return message; }
    }

    /**
     * Custom exception for LLM-related errors.
     */
    public static class LLMException extends RuntimeException {
        public LLMException(String message) {
            super(message);
        }

        public LLMException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}