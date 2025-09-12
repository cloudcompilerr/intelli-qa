package com.agentic.e2etester.analysis;

import com.agentic.e2etester.ai.LLMService;
import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * LLM-powered implementation of failure analysis system
 */
@Service
public class LLMFailureAnalyzer implements FailureAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMFailureAnalyzer.class);
    
    private final LLMService llmService;
    private final LogCorrelationService logCorrelationService;
    private final PatternRecognitionService patternRecognitionService;
    
    @Autowired
    public LLMFailureAnalyzer(LLMService llmService, 
                             LogCorrelationService logCorrelationService,
                             PatternRecognitionService patternRecognitionService) {
        this.llmService = llmService;
        this.logCorrelationService = logCorrelationService;
        this.patternRecognitionService = patternRecognitionService;
    }
    
    @Override
    public CompletableFuture<FailureAnalysis> analyzeFailure(TestFailure failure, TestContext context) {
        logger.info("Starting AI-powered failure analysis for failure: {}", failure.getFailureId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Gather additional context
                List<String> correlatedLogs = logCorrelationService
                    .extractRelevantLogs(failure, context)
                    .join();
                
                // Step 2: Find similar historical failures
                List<TestFailure> similarFailures = findSimilarFailures(failure).join();
                
                // Step 3: Build analysis prompt
                String analysisPrompt = buildFailureAnalysisPrompt(failure, context, correlatedLogs, similarFailures);
                
                // Step 4: Get LLM analysis
                String llmResponse = llmService.sendPrompt(analysisPrompt).getContent();
                
                // Step 5: Parse LLM response into structured analysis
                FailureAnalysis analysis = parseAnalysisResponse(llmResponse, failure.getFailureId());
                
                // Step 6: Enhance with pattern recognition insights
                enhanceWithPatternInsights(analysis, failure, context);
                
                logger.info("Completed failure analysis for failure: {} with confidence: {}", 
                    failure.getFailureId(), analysis.getConfidenceScore());
                
                return analysis;
                
            } catch (Exception e) {
                logger.error("Error during failure analysis for failure: {}", failure.getFailureId(), e);
                return createFallbackAnalysis(failure);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<FailureAnalysis>> correlateFailures(List<TestFailure> failures) {
        logger.info("Correlating {} failures for pattern analysis", failures.size());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Group failures by correlation ID and time proximity
                Map<String, List<TestFailure>> correlatedGroups = groupFailuresByCorrelation(failures);
                
                List<FailureAnalysis> analyses = new ArrayList<>();
                
                for (Map.Entry<String, List<TestFailure>> group : correlatedGroups.entrySet()) {
                    String correlationPrompt = buildCorrelationAnalysisPrompt(group.getValue());
                    String llmResponse = llmService.sendPrompt(correlationPrompt).getContent();
                    
                    FailureAnalysis correlatedAnalysis = parseCorrelationResponse(llmResponse, group.getValue());
                    analyses.add(correlatedAnalysis);
                }
                
                return analyses;
                
            } catch (Exception e) {
                logger.error("Error during failure correlation analysis", e);
                return failures.stream()
                    .map(this::createFallbackAnalysis)
                    .collect(Collectors.toList());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> learnFromPatterns(List<TestFailure> historicalFailures) {
        logger.info("Learning from {} historical failures", historicalFailures.size());
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Update pattern recognition database
                patternRecognitionService.updatePatternDatabase(historicalFailures).join();
                
                // Extract learning insights for LLM context
                String learningPrompt = buildPatternLearningPrompt(historicalFailures);
                String insights = llmService.sendPrompt(learningPrompt).getContent();
                
                // Store insights for future analysis (implementation would depend on storage mechanism)
                storeAnalysisInsights(insights);
                
                logger.info("Successfully learned from {} historical failures", historicalFailures.size());
                
            } catch (Exception e) {
                logger.error("Error during pattern learning", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<TestFailure>> findSimilarFailures(TestFailure failure) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // This would typically query a vector database or similarity search system
                // For now, implementing basic similarity based on failure type and error message
                return findSimilarFailuresBasic(failure);
                
            } catch (Exception e) {
                logger.error("Error finding similar failures", e);
                return Collections.emptyList();
            }
        });
    }
    
    private String buildFailureAnalysisPrompt(TestFailure failure, TestContext context, 
                                            List<String> logs, List<TestFailure> similarFailures) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following test failure and provide root cause analysis:\n\n");
        
        prompt.append("FAILURE DETAILS:\n");
        prompt.append("- Failure Type: ").append(failure.getFailureType()).append("\n");
        prompt.append("- Error Message: ").append(failure.getErrorMessage()).append("\n");
        prompt.append("- Service: ").append(failure.getServiceId()).append("\n");
        prompt.append("- Timestamp: ").append(failure.getTimestamp()).append("\n");
        
        if (failure.getStackTrace() != null) {
            prompt.append("- Stack Trace: ").append(failure.getStackTrace()).append("\n");
        }
        
        prompt.append("\nTEST CONTEXT:\n");
        prompt.append("- Test Execution Plan ID: ").append(context.getTestExecutionPlanId()).append("\n");
        prompt.append("- Correlation ID: ").append(context.getCorrelationId()).append("\n");
        
        if (!logs.isEmpty()) {
            prompt.append("\nRELEVANT LOGS:\n");
            logs.stream().limit(10).forEach(log -> prompt.append("- ").append(log).append("\n"));
        }
        
        if (!similarFailures.isEmpty()) {
            prompt.append("\nSIMILAR HISTORICAL FAILURES:\n");
            similarFailures.stream().limit(3).forEach(sf -> 
                prompt.append("- ").append(sf.getErrorMessage()).append(" (").append(sf.getFailureType()).append(")\n"));
        }
        
        prompt.append("\nPlease provide:\n");
        prompt.append("1. Root cause analysis\n");
        prompt.append("2. Contributing factors\n");
        prompt.append("3. Confidence score (0.0-1.0)\n");
        prompt.append("4. Remediation suggestions with priority\n");
        prompt.append("5. Root cause category\n");
        
        return prompt.toString();
    }
    
    private FailureAnalysis parseAnalysisResponse(String llmResponse, String failureId) {
        FailureAnalysis analysis = new FailureAnalysis();
        analysis.setAnalysisId(UUID.randomUUID().toString());
        analysis.setFailureId(failureId);
        analysis.setAnalysisModel("LLM-Analysis");
        
        // Parse LLM response - this is a simplified implementation
        // In practice, you'd use more sophisticated parsing or structured output
        String[] lines = llmResponse.split("\n");
        
        for (String line : lines) {
            if (line.toLowerCase().contains("root cause:")) {
                analysis.setRootCause(extractValue(line));
            } else if (line.toLowerCase().contains("confidence:")) {
                try {
                    double confidence = Double.parseDouble(extractValue(line).replaceAll("[^0-9.]", ""));
                    analysis.setConfidenceScore(Math.min(1.0, Math.max(0.0, confidence)));
                } catch (NumberFormatException e) {
                    analysis.setConfidenceScore(0.5); // Default confidence
                }
            } else if (line.toLowerCase().contains("category:")) {
                analysis.setRootCauseCategory(extractValue(line));
            }
        }
        
        // Extract remediation suggestions
        analysis.setRemediationSuggestions(parseRemediationSuggestions(llmResponse));
        
        // Set default values if not parsed
        if (analysis.getRootCause() == null) {
            analysis.setRootCause("Unable to determine root cause from available information");
        }
        if (analysis.getConfidenceScore() == 0.0) {
            analysis.setConfidenceScore(0.3);
        }
        
        return analysis;
    }
    
    private List<RemediationSuggestion> parseRemediationSuggestions(String llmResponse) {
        List<RemediationSuggestion> suggestions = new ArrayList<>();
        
        // Simple parsing - in practice, you'd use more sophisticated NLP
        String[] lines = llmResponse.split("\n");
        boolean inSuggestions = false;
        
        for (String line : lines) {
            if (line.toLowerCase().contains("remediation") || line.toLowerCase().contains("suggestion")) {
                inSuggestions = true;
                continue;
            }
            
            if (inSuggestions && line.trim().startsWith("-") || line.trim().matches("\\d+\\..*")) {
                String suggestionText = line.replaceAll("^[-\\d.\\s]+", "").trim();
                if (!suggestionText.isEmpty()) {
                    RemediationSuggestion suggestion = new RemediationSuggestion();
                    suggestion.setSuggestionId(UUID.randomUUID().toString());
                    suggestion.setTitle("Remediation Action");
                    suggestion.setDescription(suggestionText);
                    suggestion.setType(determineRemediationType(suggestionText));
                    suggestion.setPriority(suggestions.size() + 1);
                    suggestion.setConfidenceScore(0.7);
                    suggestions.add(suggestion);
                }
            }
        }
        
        return suggestions;
    }
    
    private RemediationType determineRemediationType(String suggestionText) {
        String lower = suggestionText.toLowerCase();
        
        if (lower.contains("restart") || lower.contains("reboot")) {
            return RemediationType.SERVICE_RESTART;
        } else if (lower.contains("config") || lower.contains("setting")) {
            return RemediationType.CONFIGURATION_CHANGE;
        } else if (lower.contains("network") || lower.contains("connectivity")) {
            return RemediationType.NETWORK_FIX;
        } else if (lower.contains("data") || lower.contains("database")) {
            return RemediationType.DATA_REPAIR;
        } else if (lower.contains("code") || lower.contains("bug")) {
            return RemediationType.CODE_FIX;
        } else if (lower.contains("scale") || lower.contains("resource")) {
            return RemediationType.INFRASTRUCTURE_SCALING;
        } else {
            return RemediationType.MANUAL_INTERVENTION;
        }
    }
    
    private String extractValue(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex != -1 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        return "";
    }
    
    private void enhanceWithPatternInsights(FailureAnalysis analysis, TestFailure failure, TestContext context) {
        // Add pattern recognition insights to the analysis
        try {
            Map<String, Object> currentState = new HashMap<>();
            currentState.put("failureType", failure.getFailureType());
            currentState.put("serviceId", failure.getServiceId());
            currentState.put("timestamp", failure.getTimestamp());
            
            Map<FailureType, Double> predictions = patternRecognitionService
                .predictPotentialFailures(currentState).join();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("predictedFailures", predictions);
            metadata.put("patternAnalysisTimestamp", System.currentTimeMillis());
            
            analysis.setAnalysisMetadata(metadata);
            
        } catch (Exception e) {
            logger.warn("Could not enhance analysis with pattern insights", e);
        }
    }
    
    private FailureAnalysis createFallbackAnalysis(TestFailure failure) {
        FailureAnalysis fallback = new FailureAnalysis();
        fallback.setAnalysisId(UUID.randomUUID().toString());
        fallback.setFailureId(failure.getFailureId());
        fallback.setRootCause("Analysis failed - " + failure.getErrorMessage());
        fallback.setRootCauseCategory("UNKNOWN");
        fallback.setConfidenceScore(0.1);
        fallback.setAnalysisModel("Fallback-Analysis");
        
        // Basic remediation suggestion
        RemediationSuggestion basicSuggestion = new RemediationSuggestion();
        basicSuggestion.setSuggestionId(UUID.randomUUID().toString());
        basicSuggestion.setTitle("Manual Investigation Required");
        basicSuggestion.setDescription("Automated analysis failed. Manual investigation required.");
        basicSuggestion.setType(RemediationType.MANUAL_INTERVENTION);
        basicSuggestion.setPriority(1);
        basicSuggestion.setConfidenceScore(0.9);
        
        fallback.setRemediationSuggestions(Arrays.asList(basicSuggestion));
        
        return fallback;
    }
    
    private Map<String, List<TestFailure>> groupFailuresByCorrelation(List<TestFailure> failures) {
        return failures.stream()
            .collect(Collectors.groupingBy(f -> 
                f.getCorrelationId() != null ? f.getCorrelationId() : "unknown"));
    }
    
    private String buildCorrelationAnalysisPrompt(List<TestFailure> correlatedFailures) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following correlated failures to identify common root causes:\n\n");
        
        for (int i = 0; i < correlatedFailures.size(); i++) {
            TestFailure failure = correlatedFailures.get(i);
            prompt.append("FAILURE ").append(i + 1).append(":\n");
            prompt.append("- Type: ").append(failure.getFailureType()).append("\n");
            prompt.append("- Service: ").append(failure.getServiceId()).append("\n");
            prompt.append("- Error: ").append(failure.getErrorMessage()).append("\n");
            prompt.append("- Time: ").append(failure.getTimestamp()).append("\n\n");
        }
        
        prompt.append("Please identify:\n");
        prompt.append("1. Common root cause across all failures\n");
        prompt.append("2. Failure cascade pattern\n");
        prompt.append("3. Primary failure point\n");
        prompt.append("4. Recommended remediation approach\n");
        
        return prompt.toString();
    }
    
    private FailureAnalysis parseCorrelationResponse(String llmResponse, List<TestFailure> failures) {
        FailureAnalysis analysis = new FailureAnalysis();
        analysis.setAnalysisId(UUID.randomUUID().toString());
        analysis.setFailureId("CORRELATED-" + UUID.randomUUID().toString());
        analysis.setAnalysisModel("LLM-Correlation-Analysis");
        
        // Parse correlation response similar to single failure analysis
        analysis.setRootCause(extractRootCauseFromResponse(llmResponse));
        analysis.setConfidenceScore(0.6); // Default for correlation analysis
        analysis.setRootCauseCategory("CORRELATED_FAILURE");
        
        // Set related failures
        analysis.setRelatedFailures(failures.stream()
            .map(TestFailure::getFailureId)
            .collect(Collectors.toList()));
        
        return analysis;
    }
    
    private String extractRootCauseFromResponse(String response) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("common root cause") || 
                line.toLowerCase().contains("root cause")) {
                return extractValue(line);
            }
        }
        return "Unable to determine common root cause";
    }
    
    private String buildPatternLearningPrompt(List<TestFailure> historicalFailures) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following historical failures to extract learning patterns:\n\n");
        
        Map<FailureType, Long> failureTypeCounts = historicalFailures.stream()
            .collect(Collectors.groupingBy(TestFailure::getFailureType, Collectors.counting()));
        
        prompt.append("FAILURE TYPE DISTRIBUTION:\n");
        failureTypeCounts.forEach((type, count) -> 
            prompt.append("- ").append(type).append(": ").append(count).append(" occurrences\n"));
        
        prompt.append("\nPlease identify:\n");
        prompt.append("1. Most common failure patterns\n");
        prompt.append("2. Seasonal or temporal patterns\n");
        prompt.append("3. Service-specific failure tendencies\n");
        prompt.append("4. Preventive measures recommendations\n");
        
        return prompt.toString();
    }
    
    private void storeAnalysisInsights(String insights) {
        // In a real implementation, this would store insights in a database or vector store
        logger.info("Storing analysis insights: {}", insights.substring(0, Math.min(100, insights.length())));
    }
    
    private List<TestFailure> findSimilarFailuresBasic(TestFailure failure) {
        // Basic similarity matching - in practice, this would use vector similarity search
        // For now, returning empty list as this would require a failure database
        return Collections.emptyList();
    }
}