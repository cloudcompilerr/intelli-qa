package com.agentic.e2etester.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LLMService.
 */
@ExtendWith(MockitoExtension.class)
class LLMServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    private LLMService llmService;

    @BeforeEach
    void setUp() {
        llmService = new LLMService(chatClient);
    }

    @Test
    void sendPrompt_Success_ReturnsValidResponse() {
        // Arrange
        String prompt = "Test prompt";
        String expectedResponse = "Test response from LLM";
        
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(expectedResponse);

        // Act
        LLMService.LLMResponse response = llmService.sendPrompt(prompt);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(expectedResponse, response.getContent());
        assertTrue(response.getResponseTimeMs() >= 0);
        assertNull(response.getErrorMessage());
        
        verify(chatClient).prompt();
        verify(requestSpec).user(prompt);
        verify(requestSpec).call();
        verify(callResponseSpec).content();
    }

    @Test
    void sendPrompt_LLMThrowsException_ThrowsLLMException() {
        // Arrange
        String prompt = "Test prompt";
        RuntimeException llmError = new RuntimeException("LLM connection failed");
        
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(llmError);

        // Act & Assert
        LLMService.LLMException exception = assertThrows(
            LLMService.LLMException.class,
            () -> llmService.sendPrompt(prompt)
        );
        
        assertEquals("Failed to get response from LLM", exception.getMessage());
        assertEquals(llmError, exception.getCause());
    }

    @Test
    void sendTemplatedPrompt_LLMThrowsException_ThrowsLLMException() {
        // Arrange
        String template = "Hello {name}";
        Map<String, Object> parameters = Map.of("name", "World");
        RuntimeException llmError = new RuntimeException("Template processing failed");
        
        when(chatClient.prompt(any(Prompt.class))).thenThrow(llmError);

        // Act & Assert
        LLMService.LLMException exception = assertThrows(
            LLMService.LLMException.class,
            () -> llmService.sendTemplatedPrompt(template, parameters)
        );
        
        assertEquals("Failed to get response from LLM with template", exception.getMessage());
        assertEquals(llmError, exception.getCause());
    }

    @Test
    void sendPromptAsync_Success_ReturnsCompletableFuture() throws Exception {
        // Arrange
        String prompt = "Async test prompt";
        String expectedResponse = "Async response";
        
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(expectedResponse);

        // Act
        CompletableFuture<LLMService.LLMResponse> future = llmService.sendPromptAsync(prompt);
        LLMService.LLMResponse response = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(expectedResponse, response.getContent());
    }

    @Test
    void sendPromptAsync_LLMFails_ReturnsFailedResponse() throws Exception {
        // Arrange
        String prompt = "Failing async prompt";
        
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Async LLM failure"));

        // Act
        CompletableFuture<LLMService.LLMResponse> future = llmService.sendPromptAsync(prompt);
        LLMService.LLMResponse response = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("", response.getContent());
        assertNotNull(response.getErrorMessage());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("LLMException") || 
                  response.getErrorMessage().contains("Async LLM failure"));
    }

    @Test
    void isLLMAvailable_LLMResponds_ReturnsTrue() {
        // Arrange
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Yes, I am available");

        // Act
        boolean available = llmService.isLLMAvailable();

        // Assert
        assertTrue(available);
    }

    @Test
    void isLLMAvailable_LLMThrowsException_ReturnsFalse() {
        // Arrange
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("LLM not available"));

        // Act
        boolean available = llmService.isLLMAvailable();

        // Assert
        assertFalse(available);
    }

    @Test
    void isLLMAvailable_LLMReturnsEmptyResponse_ReturnsFalse() {
        // Arrange
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("");

        // Act
        boolean available = llmService.isLLMAvailable();

        // Assert
        assertFalse(available);
    }

    @Test
    void getHealthStatus_LLMHealthy_ReturnsHealthyStatus() {
        // Arrange
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("I am healthy");

        // Act
        LLMService.LLMHealthStatus status = llmService.getHealthStatus();

        // Assert
        assertNotNull(status);
        assertTrue(status.isAvailable());
        assertTrue(status.getResponseTimeMs() >= 0);
        assertEquals("LLM is responsive", status.getMessage());
    }

    @Test
    void getHealthStatus_LLMUnhealthy_ReturnsUnhealthyStatus() {
        // Arrange
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Health check failed"));

        // Act
        LLMService.LLMHealthStatus status = llmService.getHealthStatus();

        // Assert
        assertNotNull(status);
        assertFalse(status.isAvailable());
        assertTrue(status.getResponseTimeMs() >= -1); // Allow for timing variations
        assertNotNull(status.getMessage());
        assertTrue(status.getMessage().length() > 0);
    }

    @Test
    void llmResponse_Constructor_SetsPropertiesCorrectly() {
        // Arrange
        String content = "Test content";
        long responseTime = 1500L;
        boolean success = true;
        String errorMessage = "No error";

        // Act
        LLMService.LLMResponse response = new LLMService.LLMResponse(content, responseTime, success, errorMessage);

        // Assert
        assertEquals(content, response.getContent());
        assertEquals(responseTime, response.getResponseTimeMs());
        assertEquals(success, response.isSuccess());
        assertEquals(errorMessage, response.getErrorMessage());
    }

    @Test
    void llmHealthStatus_Constructor_SetsPropertiesCorrectly() {
        // Arrange
        boolean available = true;
        long responseTime = 500L;
        String message = "All good";

        // Act
        LLMService.LLMHealthStatus status = new LLMService.LLMHealthStatus(available, responseTime, message);

        // Assert
        assertEquals(available, status.isAvailable());
        assertEquals(responseTime, status.getResponseTimeMs());
        assertEquals(message, status.getMessage());
    }

    @Test
    void llmException_Constructor_SetsMessageAndCause() {
        // Arrange
        String message = "Test exception message";
        Throwable cause = new RuntimeException("Root cause");

        // Act
        LLMService.LLMException exception = new LLMService.LLMException(message, cause);

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}