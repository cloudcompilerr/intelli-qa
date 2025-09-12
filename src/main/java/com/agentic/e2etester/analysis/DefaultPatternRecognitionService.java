package com.agentic.e2etester.analysis;

import com.agentic.e2etester.model.FailureType;
import com.agentic.e2etester.model.TestFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Default implementation of pattern recognition service
 */
@Service
public class DefaultPatternRecognitionService implements PatternRecognitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultPatternRecognitionService.class);
    
    // In-memory pattern storage - in production, this would be a persistent database
    private final Map<String, Integer> knownPatterns = new HashMap<>();
    private final List<List<FailureType>> knownSequences = new ArrayList<>();
    
    @Override
    public CompletableFuture<Map<String, Integer>> recognizeFailurePatterns(List<TestFailure> failures) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Recognizing patterns from {} failures", failures.size());
                
                Map<String, Integer> patterns = new HashMap<>();
                
                // Analyze failure type patterns
                Map<FailureType, Long> typeFrequency = failures.stream()
                    .collect(Collectors.groupingBy(TestFailure::getFailureType, Collectors.counting()));
                
                typeFrequency.forEach((type, count) -> 
                    patterns.put("FAILURE_TYPE_" + type.name(), count.intValue()));
                
                // Analyze service-specific patterns
                Map<String, Long> serviceFailures = failures.stream()
                    .filter(f -> f.getServiceId() != null)
                    .collect(Collectors.groupingBy(TestFailure::getServiceId, Collectors.counting()));
                
                serviceFailures.forEach((service, count) -> 
                    patterns.put("SERVICE_" + service + "_FAILURES", count.intValue()));
                
                // Analyze temporal patterns
                analyzeTemporalPatterns(failures, patterns);
                
                // Analyze error message patterns
                analyzeErrorMessagePatterns(failures, patterns);
                
                // Update known patterns
                patterns.forEach((pattern, count) -> 
                    knownPatterns.merge(pattern, count, Integer::sum));
                
                return patterns;
                
            } catch (Exception e) {
                logger.error("Error recognizing failure patterns", e);
                return Collections.emptyMap();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<List<FailureType>>> identifyFailureSequences(List<TestFailure> failures) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Identifying failure sequences from {} failures", failures.size());
                
                List<List<FailureType>> sequences = new ArrayList<>();
                
                // Sort failures by timestamp
                List<TestFailure> sortedFailures = failures.stream()
                    .sorted(Comparator.comparing(TestFailure::getTimestamp))
                    .collect(Collectors.toList());
                
                // Group failures by correlation ID to identify sequences
                Map<String, List<TestFailure>> correlatedFailures = sortedFailures.stream()
                    .filter(f -> f.getCorrelationId() != null)
                    .collect(Collectors.groupingBy(TestFailure::getCorrelationId));
                
                for (List<TestFailure> correlatedGroup : correlatedFailures.values()) {
                    if (correlatedGroup.size() > 1) {
                        List<FailureType> sequence = correlatedGroup.stream()
                            .map(TestFailure::getFailureType)
                            .collect(Collectors.toList());
                        sequences.add(sequence);
                    }
                }
                
                // Identify common sequences
                identifyCommonSequences(sequences);
                
                return sequences;
                
            } catch (Exception e) {
                logger.error("Error identifying failure sequences", e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<String, Object>> analyzePreFailureBehavior(
            Map<String, Object> systemMetrics, List<TestFailure> failures) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Analyzing pre-failure behavior patterns");
                
                Map<String, Object> behaviorPatterns = new HashMap<>();
                
                // Analyze system metrics before failures
                for (TestFailure failure : failures) {
                    Instant failureTime = failure.getTimestamp();
                    
                    // Look for patterns in the 5 minutes before failure
                    Instant preFailureStart = failureTime.minus(5, ChronoUnit.MINUTES);
                    
                    // Analyze CPU, memory, network patterns
                    analyzeCpuPatterns(systemMetrics, preFailureStart, failureTime, behaviorPatterns);
                    analyzeMemoryPatterns(systemMetrics, preFailureStart, failureTime, behaviorPatterns);
                    analyzeNetworkPatterns(systemMetrics, preFailureStart, failureTime, behaviorPatterns);
                }
                
                return behaviorPatterns;
                
            } catch (Exception e) {
                logger.error("Error analyzing pre-failure behavior", e);
                return Collections.emptyMap();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> updatePatternDatabase(List<TestFailure> recentFailures) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Updating pattern database with {} recent failures", recentFailures.size());
                
                // Update failure patterns
                recognizeFailurePatterns(recentFailures).join();
                
                // Update failure sequences
                List<List<FailureType>> newSequences = identifyFailureSequences(recentFailures).join();
                knownSequences.addAll(newSequences);
                
                // Prune old patterns if database gets too large
                pruneOldPatterns();
                
                logger.info("Pattern database updated successfully");
                
            } catch (Exception e) {
                logger.error("Error updating pattern database", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Map<FailureType, Double>> predictPotentialFailures(Map<String, Object> currentSystemState) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Predicting potential failures based on current system state");
                
                Map<FailureType, Double> predictions = new HashMap<>();
                
                // Initialize all failure types with low probability
                for (FailureType type : FailureType.values()) {
                    predictions.put(type, 0.1);
                }
                
                // Analyze current system state indicators
                analyzeSystemStateForPrediction(currentSystemState, predictions);
                
                // Apply historical pattern weights
                applyHistoricalPatternWeights(predictions);
                
                // Normalize probabilities
                normalizeProbabilities(predictions);
                
                return predictions;
                
            } catch (Exception e) {
                logger.error("Error predicting potential failures", e);
                return Collections.emptyMap();
            }
        });
    }
    
    private void analyzeTemporalPatterns(List<TestFailure> failures, Map<String, Integer> patterns) {
        // Analyze patterns by hour of day
        Map<Integer, Long> hourlyFailures = failures.stream()
            .collect(Collectors.groupingBy(
                f -> f.getTimestamp().atZone(java.time.ZoneId.systemDefault()).getHour(),
                Collectors.counting()));
        
        hourlyFailures.forEach((hour, count) -> 
            patterns.put("HOURLY_PATTERN_" + hour, count.intValue()));
        
        // Analyze patterns by day of week
        Map<Integer, Long> dailyFailures = failures.stream()
            .collect(Collectors.groupingBy(
                f -> f.getTimestamp().atZone(java.time.ZoneId.systemDefault()).getDayOfWeek().getValue(),
                Collectors.counting()));
        
        dailyFailures.forEach((day, count) -> 
            patterns.put("DAILY_PATTERN_" + day, count.intValue()));
    }
    
    private void analyzeErrorMessagePatterns(List<TestFailure> failures, Map<String, Integer> patterns) {
        // Extract common keywords from error messages
        Map<String, Integer> keywordCounts = new HashMap<>();
        
        for (TestFailure failure : failures) {
            if (failure.getErrorMessage() != null) {
                String[] words = failure.getErrorMessage().toLowerCase()
                    .replaceAll("[^a-zA-Z0-9\\s]", "")
                    .split("\\s+");
                
                for (String word : words) {
                    if (word.length() > 3 && !isCommonWord(word)) {
                        keywordCounts.merge(word, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Add significant keywords as patterns
        keywordCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .forEach(entry -> patterns.put("ERROR_KEYWORD_" + entry.getKey().toUpperCase(), entry.getValue()));
    }
    
    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of("the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "how", "man", "new", "now", "old", "see", "two", "way", "who", "boy", "did", "its", "let", "put", "say", "she", "too", "use");
        return commonWords.contains(word);
    }
    
    private void identifyCommonSequences(List<List<FailureType>> sequences) {
        // Find sequences that appear multiple times
        Map<List<FailureType>, Integer> sequenceFrequency = new HashMap<>();
        
        for (List<FailureType> sequence : sequences) {
            sequenceFrequency.merge(sequence, 1, Integer::sum);
        }
        
        // Store sequences that appear more than once
        sequenceFrequency.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .forEach(knownSequences::add);
    }
    
    private void analyzeCpuPatterns(Map<String, Object> systemMetrics, Instant start, Instant end, 
                                  Map<String, Object> patterns) {
        // Simulate CPU pattern analysis
        Object cpuData = systemMetrics.get("cpu_usage");
        if (cpuData != null) {
            patterns.put("HIGH_CPU_BEFORE_FAILURE", true);
        }
    }
    
    private void analyzeMemoryPatterns(Map<String, Object> systemMetrics, Instant start, Instant end, 
                                     Map<String, Object> patterns) {
        // Simulate memory pattern analysis
        Object memoryData = systemMetrics.get("memory_usage");
        if (memoryData != null) {
            patterns.put("MEMORY_PRESSURE_BEFORE_FAILURE", true);
        }
    }
    
    private void analyzeNetworkPatterns(Map<String, Object> systemMetrics, Instant start, Instant end, 
                                      Map<String, Object> patterns) {
        // Simulate network pattern analysis
        Object networkData = systemMetrics.get("network_latency");
        if (networkData != null) {
            patterns.put("HIGH_LATENCY_BEFORE_FAILURE", true);
        }
    }
    
    private void analyzeSystemStateForPrediction(Map<String, Object> currentState, 
                                               Map<FailureType, Double> predictions) {
        // Analyze current system indicators
        Object cpuUsage = currentState.get("cpu_usage");
        if (cpuUsage instanceof Number && ((Number) cpuUsage).doubleValue() > 80.0) {
            predictions.put(FailureType.INFRASTRUCTURE_FAILURE, 0.7);
            predictions.put(FailureType.TIMEOUT_FAILURE, 0.6);
        }
        
        Object memoryUsage = currentState.get("memory_usage");
        if (memoryUsage instanceof Number && ((Number) memoryUsage).doubleValue() > 90.0) {
            predictions.put(FailureType.INFRASTRUCTURE_FAILURE, 0.8);
            predictions.put(FailureType.SERVICE_FAILURE, 0.5);
        }
        
        Object networkLatency = currentState.get("network_latency");
        if (networkLatency instanceof Number && ((Number) networkLatency).doubleValue() > 1000.0) {
            predictions.put(FailureType.NETWORK_FAILURE, 0.7);
            predictions.put(FailureType.TIMEOUT_FAILURE, 0.8);
        }
    }
    
    private void applyHistoricalPatternWeights(Map<FailureType, Double> predictions) {
        // Apply weights based on historical patterns
        for (FailureType type : predictions.keySet()) {
            String patternKey = "FAILURE_TYPE_" + type.name();
            Integer historicalCount = knownPatterns.get(patternKey);
            
            if (historicalCount != null && historicalCount > 0) {
                // Increase probability based on historical frequency
                double currentProb = predictions.get(type);
                double historicalWeight = Math.min(0.3, historicalCount * 0.01);
                predictions.put(type, Math.min(0.95, currentProb + historicalWeight));
            }
        }
    }
    
    private void normalizeProbabilities(Map<FailureType, Double> predictions) {
        // Ensure all probabilities are between 0.0 and 1.0
        predictions.replaceAll((type, prob) -> Math.max(0.0, Math.min(1.0, prob)));
    }
    
    private void pruneOldPatterns() {
        // Keep only the most significant patterns to prevent memory bloat
        if (knownPatterns.size() > 1000) {
            // Remove patterns with low frequency
            knownPatterns.entrySet().removeIf(entry -> entry.getValue() < 2);
        }
        
        if (knownSequences.size() > 100) {
            // Keep only the most recent sequences
            while (knownSequences.size() > 100) {
                knownSequences.remove(0);
            }
        }
    }
}