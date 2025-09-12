package com.agentic.e2etester.analysis;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPatternRecognitionServiceTest {

    private DefaultPatternRecognitionService patternRecognitionService;

    @BeforeEach
    void setUp() {
        patternRecognitionService = new DefaultPatternRecognitionService();
    }

    @Test
    void testRecognizeFailurePatterns_MultipleFailures_IdentifiesPatterns() {
        // Given
        List<TestFailure> failures = Arrays.asList(
            createTestFailure(FailureType.NETWORK_FAILURE, "service-a", "Network timeout"),
            createTestFailure(FailureType.NETWORK_FAILURE, "service-b", "Connection refused"),
            createTestFailure(FailureType.SERVICE_FAILURE, "service-a", "Service unavailable"),
            createTestFailure(FailureType.DATA_FAILURE, "service-c", "Database error")
        );

        // When
        Map<String, Integer> patterns = patternRecognitionService.recognizeFailurePatterns(failures).join();

        // Then
        assertNotNull(patterns);
        assertFalse(patterns.isEmpty());
        
        // Should identify failure type patterns
        assertEquals(2, patterns.get("FAILURE_TYPE_NETWORK_FAILURE").intValue());
        assertEquals(1, patterns.get("FAILURE_TYPE_SERVICE_FAILURE").intValue());
        assertEquals(1, patterns.get("FAILURE_TYPE_DATA_FAILURE").intValue());
        
        // Should identify service-specific patterns
        assertEquals(2, patterns.get("SERVICE_service-a_FAILURES").intValue());
        assertEquals(1, patterns.get("SERVICE_service-b_FAILURES").intValue());
        assertEquals(1, patterns.get("SERVICE_service-c_FAILURES").intValue());
    }

    @Test
    void testRecognizeFailurePatterns_TemporalPatterns_IdentifiesHourlyAndDailyPatterns() {
        // Given
        Instant baseTime = Instant.parse("2024-01-15T14:30:00Z"); // Monday 2:30 PM
        List<TestFailure> failures = Arrays.asList(
            createTestFailureAtTime(FailureType.NETWORK_FAILURE, baseTime),
            createTestFailureAtTime(FailureType.SERVICE_FAILURE, baseTime.plus(1, ChronoUnit.HOURS)), // 3:30 PM
            createTestFailureAtTime(FailureType.DATA_FAILURE, baseTime.plus(24, ChronoUnit.HOURS)) // Tuesday 2:30 PM
        );

        // When
        Map<String, Integer> patterns = patternRecognitionService.recognizeFailurePatterns(failures).join();

        // Then
        assertNotNull(patterns);
        
        // Should identify hourly patterns (check that some hourly patterns exist)
        boolean hasHourlyPatterns = patterns.keySet().stream()
            .anyMatch(key -> key.startsWith("HOURLY_PATTERN_"));
        assertTrue(hasHourlyPatterns, "Should have hourly patterns");
        
        // Should identify daily patterns (check that some daily patterns exist)
        boolean hasDailyPatterns = patterns.keySet().stream()
            .anyMatch(key -> key.startsWith("DAILY_PATTERN_"));
        assertTrue(hasDailyPatterns, "Should have daily patterns");
    }

    @Test
    void testRecognizeFailurePatterns_ErrorMessageKeywords_IdentifiesKeywordPatterns() {
        // Given
        List<TestFailure> failures = Arrays.asList(
            createTestFailure(FailureType.NETWORK_FAILURE, "service-a", "timeout connection error"),
            createTestFailure(FailureType.SERVICE_FAILURE, "service-b", "timeout service unavailable"),
            createTestFailure(FailureType.DATA_FAILURE, "service-c", "database connection failed")
        );

        // When
        Map<String, Integer> patterns = patternRecognitionService.recognizeFailurePatterns(failures).join();

        // Then
        assertNotNull(patterns);
        
        // Should identify keyword patterns
        assertTrue(patterns.containsKey("ERROR_KEYWORD_TIMEOUT"));
        assertEquals(2, patterns.get("ERROR_KEYWORD_TIMEOUT").intValue());
        
        assertTrue(patterns.containsKey("ERROR_KEYWORD_CONNECTION"));
        assertEquals(2, patterns.get("ERROR_KEYWORD_CONNECTION").intValue());
    }

    @Test
    void testIdentifyFailureSequences_CorrelatedFailures_IdentifiesSequences() {
        // Given
        String correlationId = "test-correlation-123";
        Instant baseTime = Instant.now();
        
        List<TestFailure> failures = Arrays.asList(
            createCorrelatedFailureAtTime(FailureType.NETWORK_FAILURE, correlationId, baseTime),
            createCorrelatedFailureAtTime(FailureType.SERVICE_FAILURE, correlationId, baseTime.plus(1, ChronoUnit.MINUTES)),
            createCorrelatedFailureAtTime(FailureType.DATA_FAILURE, correlationId, baseTime.plus(2, ChronoUnit.MINUTES))
        );

        // When
        List<List<FailureType>> sequences = patternRecognitionService.identifyFailureSequences(failures).join();

        // Then
        assertNotNull(sequences);
        assertEquals(1, sequences.size());
        
        List<FailureType> sequence = sequences.get(0);
        assertEquals(3, sequence.size());
        assertEquals(FailureType.NETWORK_FAILURE, sequence.get(0));
        assertEquals(FailureType.SERVICE_FAILURE, sequence.get(1));
        assertEquals(FailureType.DATA_FAILURE, sequence.get(2));
    }

    @Test
    void testIdentifyFailureSequences_MultipleCorrelationIds_IdentifiesMultipleSequences() {
        // Given
        Instant baseTime = Instant.now();
        
        List<TestFailure> failures = Arrays.asList(
            // First sequence
            createCorrelatedFailureAtTime(FailureType.NETWORK_FAILURE, "correlation-1", baseTime),
            createCorrelatedFailureAtTime(FailureType.SERVICE_FAILURE, "correlation-1", baseTime.plus(1, ChronoUnit.MINUTES)),
            
            // Second sequence
            createCorrelatedFailureAtTime(FailureType.DATA_FAILURE, "correlation-2", baseTime.plus(5, ChronoUnit.MINUTES)),
            createCorrelatedFailureAtTime(FailureType.INFRASTRUCTURE_FAILURE, "correlation-2", baseTime.plus(6, ChronoUnit.MINUTES))
        );

        // When
        List<List<FailureType>> sequences = patternRecognitionService.identifyFailureSequences(failures).join();

        // Then
        assertNotNull(sequences);
        assertEquals(2, sequences.size());
        
        // Verify both sequences are identified
        assertTrue(sequences.stream().anyMatch(seq -> 
            seq.size() == 2 && seq.get(0) == FailureType.NETWORK_FAILURE && seq.get(1) == FailureType.SERVICE_FAILURE));
        assertTrue(sequences.stream().anyMatch(seq -> 
            seq.size() == 2 && seq.get(0) == FailureType.DATA_FAILURE && seq.get(1) == FailureType.INFRASTRUCTURE_FAILURE));
    }

    @Test
    void testAnalyzePreFailureBehavior_SystemMetrics_IdentifiesBehaviorPatterns() {
        // Given
        Map<String, Object> systemMetrics = new HashMap<>();
        systemMetrics.put("cpu_usage", 85.0);
        systemMetrics.put("memory_usage", 92.0);
        systemMetrics.put("network_latency", 1200.0);
        
        List<TestFailure> failures = Arrays.asList(
            createTestFailure(FailureType.INFRASTRUCTURE_FAILURE, "service-a", "High CPU usage")
        );

        // When
        Map<String, Object> behaviorPatterns = patternRecognitionService.analyzePreFailureBehavior(systemMetrics, failures).join();

        // Then
        assertNotNull(behaviorPatterns);
        
        // Should identify behavior patterns based on system metrics
        assertTrue(behaviorPatterns.containsKey("HIGH_CPU_BEFORE_FAILURE"));
        assertTrue(behaviorPatterns.containsKey("MEMORY_PRESSURE_BEFORE_FAILURE"));
        assertTrue(behaviorPatterns.containsKey("HIGH_LATENCY_BEFORE_FAILURE"));
    }

    @Test
    void testUpdatePatternDatabase_RecentFailures_UpdatesDatabase() {
        // Given
        List<TestFailure> recentFailures = Arrays.asList(
            createTestFailure(FailureType.NETWORK_FAILURE, "service-a", "New network error"),
            createTestFailure(FailureType.SERVICE_FAILURE, "service-b", "New service error")
        );

        // When
        assertDoesNotThrow(() -> {
            patternRecognitionService.updatePatternDatabase(recentFailures).join();
        });

        // Then - verify that patterns are updated by checking if new patterns are recognized
        Map<String, Integer> patterns = patternRecognitionService.recognizeFailurePatterns(recentFailures).join();
        assertNotNull(patterns);
        assertFalse(patterns.isEmpty());
    }

    @Test
    void testPredictPotentialFailures_HighCpuUsage_PredictsInfrastructureFailure() {
        // Given
        Map<String, Object> currentSystemState = new HashMap<>();
        currentSystemState.put("cpu_usage", 85.0);
        currentSystemState.put("memory_usage", 70.0);
        currentSystemState.put("network_latency", 500.0);

        // When
        Map<FailureType, Double> predictions = patternRecognitionService.predictPotentialFailures(currentSystemState).join();

        // Then
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
        
        // Should predict higher probability for infrastructure failure due to high CPU
        assertTrue(predictions.get(FailureType.INFRASTRUCTURE_FAILURE) > 0.5);
        assertTrue(predictions.get(FailureType.TIMEOUT_FAILURE) > 0.5);
        
        // All probabilities should be between 0 and 1
        predictions.values().forEach(prob -> {
            assertTrue(prob >= 0.0 && prob <= 1.0);
        });
    }

    @Test
    void testPredictPotentialFailures_HighMemoryUsage_PredictsMemoryRelatedFailures() {
        // Given
        Map<String, Object> currentSystemState = new HashMap<>();
        currentSystemState.put("cpu_usage", 50.0);
        currentSystemState.put("memory_usage", 95.0);
        currentSystemState.put("network_latency", 200.0);

        // When
        Map<FailureType, Double> predictions = patternRecognitionService.predictPotentialFailures(currentSystemState).join();

        // Then
        assertNotNull(predictions);
        
        // Should predict higher probability for infrastructure and service failures due to high memory usage
        assertTrue(predictions.get(FailureType.INFRASTRUCTURE_FAILURE) > 0.7);
        assertTrue(predictions.get(FailureType.SERVICE_FAILURE) > 0.4);
    }

    @Test
    void testPredictPotentialFailures_HighNetworkLatency_PredictsNetworkFailures() {
        // Given
        Map<String, Object> currentSystemState = new HashMap<>();
        currentSystemState.put("cpu_usage", 40.0);
        currentSystemState.put("memory_usage", 60.0);
        currentSystemState.put("network_latency", 1500.0);

        // When
        Map<FailureType, Double> predictions = patternRecognitionService.predictPotentialFailures(currentSystemState).join();

        // Then
        assertNotNull(predictions);
        
        // Should predict higher probability for network and timeout failures due to high latency
        assertTrue(predictions.get(FailureType.NETWORK_FAILURE) > 0.6);
        assertTrue(predictions.get(FailureType.TIMEOUT_FAILURE) > 0.7);
    }

    @Test
    void testPredictPotentialFailures_NormalSystemState_LowProbabilities() {
        // Given
        Map<String, Object> currentSystemState = new HashMap<>();
        currentSystemState.put("cpu_usage", 30.0);
        currentSystemState.put("memory_usage", 50.0);
        currentSystemState.put("network_latency", 100.0);

        // When
        Map<FailureType, Double> predictions = patternRecognitionService.predictPotentialFailures(currentSystemState).join();

        // Then
        assertNotNull(predictions);
        
        // Should have low probabilities for all failure types with normal system state
        predictions.values().forEach(prob -> {
            assertTrue(prob <= 0.3); // Should be low probability
        });
    }

    @Test
    void testRecognizeFailurePatterns_EmptyFailures_ReturnsEmptyPatterns() {
        // Given
        List<TestFailure> emptyFailures = Collections.emptyList();

        // When
        Map<String, Integer> patterns = patternRecognitionService.recognizeFailurePatterns(emptyFailures).join();

        // Then
        assertNotNull(patterns);
        assertTrue(patterns.isEmpty());
    }

    @Test
    void testIdentifyFailureSequences_SingleFailures_ReturnsEmptySequences() {
        // Given
        List<TestFailure> singleFailures = Arrays.asList(
            createTestFailure(FailureType.NETWORK_FAILURE, "service-a", "Single failure")
        );

        // When
        List<List<FailureType>> sequences = patternRecognitionService.identifyFailureSequences(singleFailures).join();

        // Then
        assertNotNull(sequences);
        assertTrue(sequences.isEmpty()); // No sequences with single failures
    }

    private TestFailure createTestFailure(FailureType type, String serviceId, String errorMessage) {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("test-123");
        failure.setFailureType(type);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage(errorMessage);
        failure.setTimestamp(Instant.now());
        failure.setServiceId(serviceId);
        return failure;
    }

    private TestFailure createTestFailureAtTime(FailureType type, Instant timestamp) {
        TestFailure failure = createTestFailure(type, "service-test", "Test error");
        failure.setTimestamp(timestamp);
        return failure;
    }

    private TestFailure createCorrelatedFailureAtTime(FailureType type, String correlationId, Instant timestamp) {
        TestFailure failure = createTestFailureAtTime(type, timestamp);
        failure.setCorrelationId(correlationId);
        return failure;
    }
}