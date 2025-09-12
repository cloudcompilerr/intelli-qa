package com.agentic.e2etester.service;

import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of TestMemoryService using Spring AI VectorStore for pattern storage
 * and in-memory storage for correlation traces and execution history.
 */
@Service
public class DefaultTestMemoryService implements TestMemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultTestMemoryService.class);
    
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    
    // In-memory storage for correlation traces and execution history
    private final Map<String, TestPattern> patterns = new ConcurrentHashMap<>();
    private final Map<String, CorrelationTrace> correlationTraces = new ConcurrentHashMap<>();
    private final List<TestExecutionHistory> executionHistory = new ArrayList<>();
    
    public DefaultTestMemoryService(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public TestPattern storeTestPattern(TestPattern pattern) {
        logger.debug("Storing test pattern: {}", pattern.getPatternId());
        
        try {
            // Store in local cache
            patterns.put(pattern.getPatternId(), pattern);
            
            // Create document for vector store
            String content = createPatternContent(pattern);
            Map<String, Object> metadata = createPatternMetadata(pattern);
            
            Document document = new Document(pattern.getPatternId(), content, metadata);
            vectorStore.add(List.of(document));
            
            logger.info("Successfully stored test pattern: {}", pattern.getPatternId());
            return pattern;
            
        } catch (Exception e) {
            logger.error("Failed to store test pattern: {}", pattern.getPatternId(), e);
            throw new RuntimeException("Failed to store test pattern", e);
        }
    }
    
    @Override
    public List<TestPattern> findSimilarPatterns(TestContext context, int limit) {
        logger.debug("Finding similar patterns for context: {}", context.getCorrelationId());
        
        try {
            String query = createContextQuery(context);
            SearchRequest searchRequest = SearchRequest.query(query).withTopK(limit);
            
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            return documents.stream()
                    .map(doc -> patterns.get(doc.getId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Failed to find similar patterns for context: {}", context.getCorrelationId(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<TestPattern> findPatternsByType(PatternType type, int limit) {
        logger.debug("Finding patterns by type: {}", type);
        
        return patterns.values().stream()
                .filter(pattern -> pattern.getType() == type)
                .sorted((p1, p2) -> Double.compare(p2.getSuccessRate(), p1.getSuccessRate()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<TestPattern> updatePatternUsage(String patternId, boolean success, long executionTimeMs) {
        logger.debug("Updating pattern usage: {} - success: {}", patternId, success);
        
        TestPattern pattern = patterns.get(patternId);
        if (pattern != null) {
            pattern.incrementUsage();
            pattern.updateSuccessRate(success);
            
            // Update average execution time
            long currentAvg = pattern.getAverageExecutionTimeMs();
            int usageCount = pattern.getUsageCount();
            long newAvg = ((currentAvg * (usageCount - 1)) + executionTimeMs) / usageCount;
            pattern.setAverageExecutionTimeMs(newAvg);
            
            // Re-store the updated pattern
            storeTestPattern(pattern);
            
            return Optional.of(pattern);
        }
        
        return Optional.empty();
    }
    
    @Override
    public TestExecutionHistory storeExecutionHistory(TestExecutionHistory history) {
        logger.debug("Storing execution history: {}", history.getExecutionId());
        
        synchronized (executionHistory) {
            executionHistory.add(history);
            
            // Keep only recent history (last 1000 records)
            if (executionHistory.size() > 1000) {
                executionHistory.subList(0, executionHistory.size() - 1000).clear();
            }
        }
        
        logger.info("Successfully stored execution history: {}", history.getExecutionId());
        return history;
    }
    
    @Override
    public List<TestExecutionHistory> findExecutionHistoryByCorrelationId(String correlationId) {
        logger.debug("Finding execution history by correlation ID: {}", correlationId);
        
        return executionHistory.stream()
                .filter(history -> correlationId.equals(history.getCorrelationId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TestExecutionHistory> findRecentExecutionHistory(int limit) {
        logger.debug("Finding recent execution history, limit: {}", limit);
        
        return executionHistory.stream()
                .sorted((h1, h2) -> h2.getStartTime().compareTo(h1.getStartTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TestPattern> analyzeAndExtractPatterns(int limit) {
        logger.debug("Analyzing and extracting patterns from recent executions, limit: {}", limit);
        
        List<TestPattern> newPatterns = new ArrayList<>();
        
        try {
            List<TestExecutionHistory> recentHistory = findRecentExecutionHistory(limit);
            
            // Group by service flow patterns
            Map<List<String>, List<TestExecutionHistory>> flowGroups = recentHistory.stream()
                    .filter(history -> history.getServicesInvolved() != null)
                    .collect(Collectors.groupingBy(TestExecutionHistory::getServicesInvolved));
            
            // Extract patterns from groups with multiple executions
            for (Map.Entry<List<String>, List<TestExecutionHistory>> entry : flowGroups.entrySet()) {
                List<TestExecutionHistory> histories = entry.getValue();
                if (histories.size() >= 3) { // Minimum occurrences to consider as pattern
                    TestPattern pattern = extractPatternFromHistories(histories);
                    if (pattern != null) {
                        newPatterns.add(pattern);
                        storeTestPattern(pattern);
                    }
                }
            }
            
            logger.info("Extracted {} new patterns from recent executions", newPatterns.size());
            
        } catch (Exception e) {
            logger.error("Failed to analyze and extract patterns", e);
        }
        
        return newPatterns;
    }
    
    @Override
    public CorrelationTrace storeCorrelationTrace(CorrelationTrace trace) {
        logger.debug("Storing correlation trace: {}", trace.getCorrelationId());
        
        correlationTraces.put(trace.getCorrelationId(), trace);
        
        logger.info("Successfully stored correlation trace: {}", trace.getCorrelationId());
        return trace;
    }
    
    @Override
    public Optional<CorrelationTrace> findCorrelationTrace(String correlationId) {
        logger.debug("Finding correlation trace: {}", correlationId);
        
        return Optional.ofNullable(correlationTraces.get(correlationId));
    }
    
    @Override
    public Optional<CorrelationTrace> addSpanToTrace(String correlationId, TraceSpan span) {
        logger.debug("Adding span to trace: {} - span: {}", correlationId, span.getSpanId());
        
        CorrelationTrace trace = correlationTraces.get(correlationId);
        if (trace != null) {
            if (trace.getTraceSpans() == null) {
                trace.setTraceSpans(new ArrayList<>());
            }
            trace.addSpan(span);
            return Optional.of(trace);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<CorrelationTrace> completeTrace(String correlationId) {
        logger.debug("Completing trace: {}", correlationId);
        
        CorrelationTrace trace = correlationTraces.get(correlationId);
        if (trace != null) {
            trace.completeTrace();
            return Optional.of(trace);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<CorrelationTrace> failTrace(String correlationId, String errorDetails) {
        logger.debug("Failing trace: {} - error: {}", correlationId, errorDetails);
        
        CorrelationTrace trace = correlationTraces.get(correlationId);
        if (trace != null) {
            trace.failTrace(errorDetails);
            return Optional.of(trace);
        }
        
        return Optional.empty();
    }
    
    @Override
    public int cleanupOldData(int retentionDays) {
        logger.debug("Cleaning up old data, retention days: {}", retentionDays);
        
        Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int cleanedCount = 0;
        
        // Clean up old execution history
        synchronized (executionHistory) {
            int originalSize = executionHistory.size();
            executionHistory.removeIf(history -> history.getStartTime().isBefore(cutoffTime));
            cleanedCount += originalSize - executionHistory.size();
        }
        
        // Clean up old correlation traces
        int originalTraceSize = correlationTraces.size();
        correlationTraces.entrySet().removeIf(entry -> 
                entry.getValue().getStartTime().isBefore(cutoffTime));
        cleanedCount += originalTraceSize - correlationTraces.size();
        
        logger.info("Cleaned up {} old data records", cleanedCount);
        return cleanedCount;
    }
    
    // Helper methods
    
    private String createPatternContent(TestPattern pattern) {
        StringBuilder content = new StringBuilder();
        content.append("Pattern: ").append(pattern.getName()).append("\n");
        content.append("Type: ").append(pattern.getType()).append("\n");
        content.append("Description: ").append(pattern.getDescription()).append("\n");
        
        if (pattern.getServiceFlow() != null) {
            content.append("Services: ").append(String.join(" -> ", pattern.getServiceFlow())).append("\n");
        }
        
        if (pattern.getTags() != null) {
            content.append("Tags: ").append(String.join(", ", pattern.getTags())).append("\n");
        }
        
        return content.toString();
    }
    
    private Map<String, Object> createPatternMetadata(TestPattern pattern) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("patternId", pattern.getPatternId());
        metadata.put("type", pattern.getType().toString());
        metadata.put("usageCount", pattern.getUsageCount());
        metadata.put("successRate", pattern.getSuccessRate());
        metadata.put("createdAt", pattern.getCreatedAt().toString());
        
        if (pattern.getTags() != null) {
            metadata.put("tags", String.join(",", pattern.getTags()));
        }
        
        return metadata;
    }
    
    private String createContextQuery(TestContext context) {
        StringBuilder query = new StringBuilder();
        
        if (context.getInteractions() != null && !context.getInteractions().isEmpty()) {
            List<String> services = context.getInteractions().stream()
                    .map(ServiceInteraction::getServiceId)
                    .distinct()
                    .collect(Collectors.toList());
            query.append("Services: ").append(String.join(" ", services)).append(" ");
        }
        
        if (context.getEvents() != null && !context.getEvents().isEmpty()) {
            List<String> eventTypes = context.getEvents().stream()
                    .map(event -> event.getType().toString())
                    .distinct()
                    .collect(Collectors.toList());
            query.append("Events: ").append(String.join(" ", eventTypes)).append(" ");
        }
        
        return query.toString().trim();
    }
    
    private TestPattern extractPatternFromHistories(List<TestExecutionHistory> histories) {
        if (histories.isEmpty()) {
            return null;
        }
        
        TestExecutionHistory first = histories.get(0);
        String patternId = "pattern-" + UUID.randomUUID().toString();
        String name = "Auto-extracted pattern for " + String.join("->", first.getServicesInvolved());
        
        TestPattern pattern = new TestPattern(patternId, name, PatternType.SUCCESS_FLOW);
        pattern.setServiceFlow(first.getServicesInvolved());
        pattern.setDescription("Automatically extracted pattern from " + histories.size() + " similar executions");
        
        // Calculate success rate
        long successCount = histories.stream()
                .mapToLong(h -> h.isSuccessful() ? 1 : 0)
                .sum();
        pattern.setSuccessRate((double) successCount / histories.size());
        
        // Calculate average execution time
        long avgTime = (long) histories.stream()
                .mapToLong(TestExecutionHistory::getExecutionTimeMs)
                .average()
                .orElse(0.0);
        pattern.setAverageExecutionTimeMs(avgTime);
        
        pattern.setUsageCount(histories.size());
        
        return pattern;
    }
}