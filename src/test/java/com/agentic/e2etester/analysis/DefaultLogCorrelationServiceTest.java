package com.agentic.e2etester.analysis;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLogCorrelationServiceTest {

    private DefaultLogCorrelationService logCorrelationService;

    @BeforeEach
    void setUp() {
        logCorrelationService = new DefaultLogCorrelationService();
    }

    @Test
    void testCorrelateLogs_ValidCorrelationId_ReturnsLogs() {
        // Given
        String correlationId = "test-correlation-123";
        long timeWindow = 30000L;

        // When
        List<String> correlatedLogs = logCorrelationService.correlateLogs(correlationId, timeWindow).join();

        // Then
        assertNotNull(correlatedLogs);
        assertFalse(correlatedLogs.isEmpty());
        assertTrue(correlatedLogs.stream().anyMatch(log -> log.contains(correlationId)));
    }

    @Test
    void testExtractRelevantLogs_FailureWithLogs_ReturnsRelevantLogs() {
        // Given
        TestFailure failure = createTestFailure();
        failure.setLogEntries(Arrays.asList(
            "INFO: Processing request",
            "ERROR: Database connection failed",
            "WARN: Retrying operation"
        ));
        
        TestContext context = createTestContext();

        // When
        List<String> relevantLogs = logCorrelationService.extractRelevantLogs(failure, context).join();

        // Then
        assertNotNull(relevantLogs);
        assertFalse(relevantLogs.isEmpty());
        // Should prioritize error logs
        assertTrue(relevantLogs.stream().anyMatch(log -> log.contains("ERROR")));
    }

    @Test
    void testExtractRelevantLogs_NoErrorLogs_ReturnsAllLogs() {
        // Given
        TestFailure failure = createTestFailure();
        failure.setLogEntries(Arrays.asList(
            "INFO: Processing request",
            "INFO: Operation completed",
            "DEBUG: Debug information"
        ));
        
        TestContext context = createTestContext();

        // When
        List<String> relevantLogs = logCorrelationService.extractRelevantLogs(failure, context).join();

        // Then
        assertNotNull(relevantLogs);
        // Should return all logs when no error patterns found
        assertTrue(relevantLogs.size() >= 3);
    }

    @Test
    void testIdentifyLogPatterns_ErrorLogs_IdentifiesPatterns() {
        // Given
        List<String> logEntries = Arrays.asList(
            "ERROR: Connection timeout occurred",
            "EXCEPTION: NullPointerException in service",
            "FAILED: Database query execution failed",
            "ERROR: Network connection refused",
            "INFO: Normal operation log"
        );

        // When
        Map<String, Double> patterns = logCorrelationService.identifyLogPatterns(logEntries).join();

        // Then
        assertNotNull(patterns);
        assertFalse(patterns.isEmpty());
        
        // Should identify general error pattern
        assertTrue(patterns.containsKey("GENERAL_ERROR"));
        assertTrue(patterns.get("GENERAL_ERROR") > 0);
        
        // Should identify network error pattern
        assertTrue(patterns.containsKey("NETWORK_ERROR"));
        
        // Should identify runtime error pattern
        assertTrue(patterns.containsKey("RUNTIME_ERROR"));
        
        // Pattern scores should be normalized (between 0 and 1)
        patterns.values().forEach(score -> {
            assertTrue(score >= 0.0 && score <= 1.0);
        });
    }

    @Test
    void testIdentifyLogPatterns_CustomPatterns_IdentifiesCustomPatterns() {
        // Given
        List<String> logEntries = Arrays.asList(
            "Circuit breaker opened for service A",
            "Rate limit exceeded for user",
            "Out of memory error occurred",
            "Deadlock detected in database"
        );

        // When
        Map<String, Double> patterns = logCorrelationService.identifyLogPatterns(logEntries).join();

        // Then
        assertNotNull(patterns);
        
        // Should identify custom patterns
        assertTrue(patterns.containsKey("CIRCUIT_BREAKER"));
        assertTrue(patterns.containsKey("RATE_LIMITING"));
        assertTrue(patterns.containsKey("MEMORY_EXHAUSTION"));
        assertTrue(patterns.containsKey("DEADLOCK"));
    }

    @Test
    void testSearchErrorPatterns_MultipleServices_FindsPatterns() {
        // Given
        List<String> services = Arrays.asList("service-a", "service-b", "service-c");
        List<String> errorPatterns = Arrays.asList("timeout", "connection failed", "out of memory");
        long timeRange = 3600000L; // 1 hour

        // When
        Map<String, List<String>> results = logCorrelationService.searchErrorPatterns(services, errorPatterns, timeRange).join();

        // Then
        assertNotNull(results);
        
        // Results should contain services that have errors (probabilistic)
        // Since this is a simulation, we can't guarantee specific results
        // but we can verify the structure
        results.forEach((service, errors) -> {
            assertTrue(services.contains(service));
            assertNotNull(errors);
            assertFalse(errors.isEmpty());
            
            // Each error should mention the service and contain a pattern
            errors.forEach(error -> {
                assertTrue(error.contains(service));
                assertTrue(errorPatterns.stream().anyMatch(pattern -> error.contains(pattern)));
            });
        });
    }

    @Test
    void testIdentifyLogPatterns_EmptyLogs_ReturnsEmptyPatterns() {
        // Given
        List<String> emptyLogEntries = Collections.emptyList();

        // When
        Map<String, Double> patterns = logCorrelationService.identifyLogPatterns(emptyLogEntries).join();

        // Then
        assertNotNull(patterns);
        assertTrue(patterns.isEmpty());
    }

    @Test
    void testExtractRelevantLogs_FailureWithoutLogs_ReturnsCorrelatedLogs() {
        // Given
        TestFailure failure = createTestFailure();
        failure.setLogEntries(null); // No logs in failure
        
        TestContext context = createTestContext();
        context.setCorrelationId("test-correlation-456");

        // When
        List<String> relevantLogs = logCorrelationService.extractRelevantLogs(failure, context).join();

        // Then
        assertNotNull(relevantLogs);
        // Should still return correlated logs even if failure has no logs
        assertFalse(relevantLogs.isEmpty());
    }

    @Test
    void testCorrelateLogs_NullCorrelationId_HandlesGracefully() {
        // Given
        String correlationId = null;
        long timeWindow = 30000L;

        // When & Then
        assertDoesNotThrow(() -> {
            List<String> logs = logCorrelationService.correlateLogs(correlationId, timeWindow).join();
            assertNotNull(logs);
        });
    }

    @Test
    void testIdentifyLogPatterns_DatabaseErrorPatterns() {
        // Given
        List<String> logEntries = Arrays.asList(
            "SQLException: Connection pool exhausted",
            "Database error: Query timeout",
            "Connection pool error occurred",
            "SQL exception in transaction"
        );

        // When
        Map<String, Double> patterns = logCorrelationService.identifyLogPatterns(logEntries).join();

        // Then
        assertNotNull(patterns);
        assertTrue(patterns.containsKey("DATABASE_ERROR"));
        assertEquals(1.0, patterns.get("DATABASE_ERROR"), 0.01); // All entries match database pattern
    }

    @Test
    void testIdentifyLogPatterns_KafkaErrorPatterns() {
        // Given
        List<String> logEntries = Arrays.asList(
            "Kafka consumer error: Failed to consume message",
            "Message failed to send to topic",
            "Kafka error: Broker not available"
        );

        // When
        Map<String, Double> patterns = logCorrelationService.identifyLogPatterns(logEntries).join();

        // Then
        assertNotNull(patterns);
        assertTrue(patterns.containsKey("KAFKA_ERROR"));
        assertEquals(1.0, patterns.get("KAFKA_ERROR"), 0.01); // All entries match Kafka pattern
    }

    @Test
    void testIdentifyLogPatterns_AuthenticationErrorPatterns() {
        // Given
        List<String> logEntries = Arrays.asList(
            "Authentication failed for user",
            "Unauthorized access attempt",
            "Forbidden: Access denied"
        );

        // When
        Map<String, Double> patterns = logCorrelationService.identifyLogPatterns(logEntries).join();

        // Then
        assertNotNull(patterns);
        assertTrue(patterns.containsKey("AUTHENTICATION_ERROR"));
        assertEquals(1.0, patterns.get("AUTHENTICATION_ERROR"), 0.01); // All entries match auth pattern
    }

    private TestFailure createTestFailure() {
        TestFailure failure = new TestFailure();
        failure.setFailureId(UUID.randomUUID().toString());
        failure.setTestId("test-123");
        failure.setFailureType(FailureType.SERVICE_FAILURE);
        failure.setSeverity(FailureSeverity.HIGH);
        failure.setErrorMessage("Test error message");
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