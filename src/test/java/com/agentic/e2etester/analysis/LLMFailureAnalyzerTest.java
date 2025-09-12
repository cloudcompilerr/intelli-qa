package com.agentic.e2etester.analysis;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LLMFailureAnalyzerTest {

    @Mock
    private LLMService llmService;
    
    @Mock
    private LogCorrelationService logCorrelationService;
    
    @Mock
    private PatternRecognitionService patternRecognitionService;
    
    private LLMFailureAnalyzer failureAnalyzer;
    
    @BeforeEach
    void setUp() {
        failureAnalyzer = new LLMFailureAnalyzer(llmService, logCorrelationService, patternRecognitionService);
    }
    
    @Test
    void testAnalyzeFailure_NetworkFailure_Success() {
        // Given
        TestFailure failure = createTestFailure(FailureType.NETWORK_FAILURE, "Connection timeout");
        TestContext context = createTestContext();
        
        when(logCorrelationService.extractRelevantLogs(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList("Network error in service A")));
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Root cause: Network connectivity issue\nConfidence: 0.8\nCategory: NETWORK\nRemediation suggestions:\n- Check network configuration\n- Restart network services", 100, true));
        
        when(patternRecognitionService.predictPotentialFailures(any()))
            .thenReturn(CompletableFuture.completedFuture(Map.of(FailureType.NETWORK_FAILURE, 0.7)));
        
        // When
        CompletableFuture<FailureAnalysis> result = failureAnalyzer.analyzeFailure(failure, context);
        FailureAnalysis analysis = result.join();
        
        // Then
        assertNotNull(analysis);
        assertEquals(failure.getFailureId(), analysis.getFailureId());
        assertEquals("Network connectivity issue", analysis.getRootCause());
        assertEquals(0.8, analysis.getConfidenceScore(), 0.01);
        assertEquals("NETWORK", analysis.getRootCauseCategory());
        assertFalse(analysis.getRemediationSuggestions().isEmpty());
        
        verify(logCorrelationService).extractRelevantLogs(failure, context);
        verify(llmService).sendPrompt(anyString());
        verify(patternRecognitionService).predictPotentialFailures(any());
    }
    
    @Test
    void testAnalyzeFailure_ServiceFailure_Success() {
        // Given
        TestFailure failure = createTestFailure(FailureType.SERVICE_FAILURE, "Service unavailable");
        TestContext context = createTestContext();
        
        when(logCorrelationService.extractRelevantLogs(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList("Service health check failed")));
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Root cause: Service overload due to high traffic\nConfidence: 0.9\nCategory: SERVICE\nRemediation suggestions:\n- Scale up service instances\n- Implement circuit breaker", 100, true));
        
        when(patternRecognitionService.predictPotentialFailures(any()))
            .thenReturn(CompletableFuture.completedFuture(Map.of(FailureType.SERVICE_FAILURE, 0.8)));
        
        // When
        FailureAnalysis analysis = failureAnalyzer.analyzeFailure(failure, context).join();
        
        // Then
        assertNotNull(analysis);
        assertEquals("Service overload due to high traffic", analysis.getRootCause());
        assertEquals(0.9, analysis.getConfidenceScore(), 0.01);
        assertEquals("SERVICE", analysis.getRootCauseCategory());
        assertEquals(2, analysis.getRemediationSuggestions().size());
        
        // Verify remediation suggestions
        List<RemediationSuggestion> suggestions = analysis.getRemediationSuggestions();
        assertTrue(suggestions.stream().anyMatch(s -> s.getDescription().contains("Scale up service instances")));
        assertTrue(suggestions.stream().anyMatch(s -> s.getDescription().contains("Implement circuit breaker")));
    }
    
    @Test
    void testAnalyzeFailure_DatabaseFailure_Success() {
        // Given
        TestFailure failure = createTestFailure(FailureType.DATA_FAILURE, "Database connection failed");
        TestContext context = createTestContext();
        
        when(logCorrelationService.extractRelevantLogs(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Arrays.asList("Connection pool exhausted")));
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Root cause: Database connection pool exhaustion\nConfidence: 0.85\nCategory: DATABASE\nRemediation suggestions:\n- Increase connection pool size\n- Optimize database queries", 100, true));
        
        when(patternRecognitionService.predictPotentialFailures(any()))
            .thenReturn(CompletableFuture.completedFuture(Map.of(FailureType.DATA_FAILURE, 0.75)));
        
        // When
        FailureAnalysis analysis = failureAnalyzer.analyzeFailure(failure, context).join();
        
        // Then
        assertNotNull(analysis);
        assertEquals("Database connection pool exhaustion", analysis.getRootCause());
        assertEquals(0.85, analysis.getConfidenceScore(), 0.01);
        assertEquals("DATABASE", analysis.getRootCauseCategory());
        
        // Verify remediation suggestion types
        List<RemediationSuggestion> suggestions = analysis.getRemediationSuggestions();
        assertFalse(suggestions.isEmpty(), "Should have remediation suggestions");
        // Check that at least one suggestion has a valid type
        assertTrue(suggestions.stream().anyMatch(s -> s.getType() != null), "Should have valid remediation types");
    }
    
    @Test
    void testAnalyzeFailure_LLMServiceFailure_FallbackAnalysis() {
        // Given
        TestFailure failure = createTestFailure(FailureType.UNKNOWN_FAILURE, "Unknown error");
        TestContext context = createTestContext();
        
        when(logCorrelationService.extractRelevantLogs(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        
        when(llmService.sendPrompt(anyString()))
            .thenThrow(new RuntimeException("LLM service unavailable"));
        
        // When
        FailureAnalysis analysis = failureAnalyzer.analyzeFailure(failure, context).join();
        
        // Then
        assertNotNull(analysis);
        assertEquals(failure.getFailureId(), analysis.getFailureId());
        assertTrue(analysis.getRootCause().contains("Analysis failed"));
        assertEquals(0.1, analysis.getConfidenceScore(), 0.01);
        assertEquals("UNKNOWN", analysis.getRootCauseCategory());
        assertEquals("Fallback-Analysis", analysis.getAnalysisModel());
        
        // Should have fallback remediation suggestion
        assertEquals(1, analysis.getRemediationSuggestions().size());
        RemediationSuggestion suggestion = analysis.getRemediationSuggestions().get(0);
        assertEquals(RemediationType.MANUAL_INTERVENTION, suggestion.getType());
    }
    
    @Test
    void testCorrelateFailures_MultipleFailures_Success() {
        // Given
        List<TestFailure> failures = Arrays.asList(
            createTestFailure(FailureType.NETWORK_FAILURE, "Connection timeout"),
            createTestFailure(FailureType.SERVICE_FAILURE, "Service unavailable"),
            createTestFailure(FailureType.DATA_FAILURE, "Database error")
        );
        
        // Set same correlation ID for all failures
        failures.forEach(f -> f.setCorrelationId("test-correlation-123"));
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Common root cause: Network infrastructure failure causing cascade\nPrimary failure point: Network layer\nRecommended remediation: Fix network connectivity", 100, true));
        
        // When
        List<FailureAnalysis> analyses = failureAnalyzer.correlateFailures(failures).join();
        
        // Then
        assertNotNull(analyses);
        assertEquals(1, analyses.size()); // All failures grouped by correlation ID
        
        FailureAnalysis analysis = analyses.get(0);
        assertTrue(analysis.getFailureId().startsWith("CORRELATED-"));
        assertEquals("LLM-Correlation-Analysis", analysis.getAnalysisModel());
        assertEquals("CORRELATED_FAILURE", analysis.getRootCauseCategory());
        assertEquals(3, analysis.getRelatedFailures().size());
        
        verify(llmService).sendPrompt(contains("correlated failures"));
    }
    
    @Test
    void testLearnFromPatterns_HistoricalFailures_Success() {
        // Given
        List<TestFailure> historicalFailures = Arrays.asList(
            createTestFailure(FailureType.NETWORK_FAILURE, "Timeout"),
            createTestFailure(FailureType.NETWORK_FAILURE, "Connection refused"),
            createTestFailure(FailureType.SERVICE_FAILURE, "Service down")
        );
        
        when(patternRecognitionService.updatePatternDatabase(any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Pattern analysis: Network failures are most common, occurring 67% of the time", 100, true));
        
        // When
        CompletableFuture<Void> result = failureAnalyzer.learnFromPatterns(historicalFailures);
        
        // Then
        assertDoesNotThrow(() -> result.join());
        
        verify(patternRecognitionService).updatePatternDatabase(historicalFailures);
        verify(llmService).sendPrompt(contains("historical failures"));
    }
    
    @Test
    void testFindSimilarFailures_BasicImplementation() {
        // Given
        TestFailure failure = createTestFailure(FailureType.NETWORK_FAILURE, "Connection timeout");
        
        // When
        List<TestFailure> similarFailures = failureAnalyzer.findSimilarFailures(failure).join();
        
        // Then
        assertNotNull(similarFailures);
        // Basic implementation returns empty list - this would be enhanced with vector search
        assertTrue(similarFailures.isEmpty());
    }
    
    @Test
    void testRemediationSuggestionTypeDetermination() {
        // Given
        TestFailure failure = createTestFailure(FailureType.SERVICE_FAILURE, "Service error");
        TestContext context = createTestContext();
        
        when(logCorrelationService.extractRelevantLogs(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        
        when(llmService.sendPrompt(anyString()))
            .thenReturn(new LLMService.LLMResponse("Root cause: Service issue\nConfidence: 0.7\nRemediation suggestions:\n- Restart the service\n- Update configuration settings\n- Fix network connectivity\n- Repair database corruption\n- Fix application bug\n- Scale infrastructure resources", 100, true));
        
        when(patternRecognitionService.predictPotentialFailures(any()))
            .thenReturn(CompletableFuture.completedFuture(Collections.emptyMap()));
        
        // When
        FailureAnalysis analysis = failureAnalyzer.analyzeFailure(failure, context).join();
        
        // Then
        List<RemediationSuggestion> suggestions = analysis.getRemediationSuggestions();
        assertEquals(6, suggestions.size());
        
        // Verify different remediation types are correctly identified
        Map<RemediationType, Long> typeCount = suggestions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                RemediationSuggestion::getType, 
                java.util.stream.Collectors.counting()));
        
        assertTrue(typeCount.containsKey(RemediationType.SERVICE_RESTART));
        assertTrue(typeCount.containsKey(RemediationType.CONFIGURATION_CHANGE));
        assertTrue(typeCount.containsKey(RemediationType.NETWORK_FIX));
        assertTrue(typeCount.containsKey(RemediationType.DATA_REPAIR));
        assertTrue(typeCount.containsKey(RemediationType.CODE_FIX));
        assertTrue(typeCount.containsKey(RemediationType.INFRASTRUCTURE_SCALING));
    }
    
    private TestFailure createTestFailure(FailureType type, String errorMessage) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("test-123");
        failure.setStepId("step-456");
        failure.setFailureType(type);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage(errorMessage);
        failure.setTimestamp(Instant.now());
        failure.setServiceId("test-service");
        failure.setCorrelationId("correlation-123");
        return failure;
    }
    
    private TestContext createTestContext() {
        TestContext context = new TestContext();
        context.setTestExecutionPlanId("test-123");
        context.setCorrelationId("correlation-123");
        context.setExecutionState(new HashMap<>());
        context.setInteractions(new ArrayList<>());
        context.setEvents(new ArrayList<>());
        return context;
    }
}