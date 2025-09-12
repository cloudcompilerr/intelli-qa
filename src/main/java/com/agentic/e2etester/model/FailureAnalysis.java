package com.agentic.e2etester.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of AI-powered failure analysis
 */
public class FailureAnalysis {
    private String analysisId;
    private String failureId;
    private String rootCause;
    private String rootCauseCategory;
    private double confidenceScore;
    private List<String> contributingFactors;
    private List<RemediationSuggestion> remediationSuggestions;
    private List<String> relatedFailures;
    private Map<String, Object> analysisMetadata;
    private Instant analysisTimestamp;
    private String analysisModel;

    public FailureAnalysis() {
        this.analysisTimestamp = Instant.now();
    }

    public FailureAnalysis(String analysisId, String failureId, String rootCause, double confidenceScore) {
        this.analysisId = analysisId;
        this.failureId = failureId;
        this.rootCause = rootCause;
        this.confidenceScore = confidenceScore;
        this.analysisTimestamp = Instant.now();
    }

    // Getters and setters
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }

    public String getFailureId() { return failureId; }
    public void setFailureId(String failureId) { this.failureId = failureId; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getRootCauseCategory() { return rootCauseCategory; }
    public void setRootCauseCategory(String rootCauseCategory) { this.rootCauseCategory = rootCauseCategory; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public List<String> getContributingFactors() { return contributingFactors; }
    public void setContributingFactors(List<String> contributingFactors) { this.contributingFactors = contributingFactors; }

    public List<RemediationSuggestion> getRemediationSuggestions() { return remediationSuggestions; }
    public void setRemediationSuggestions(List<RemediationSuggestion> remediationSuggestions) { this.remediationSuggestions = remediationSuggestions; }

    public List<String> getRelatedFailures() { return relatedFailures; }
    public void setRelatedFailures(List<String> relatedFailures) { this.relatedFailures = relatedFailures; }

    public Map<String, Object> getAnalysisMetadata() { return analysisMetadata; }
    public void setAnalysisMetadata(Map<String, Object> analysisMetadata) { this.analysisMetadata = analysisMetadata; }

    public Instant getAnalysisTimestamp() { return analysisTimestamp; }
    public void setAnalysisTimestamp(Instant analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }

    public String getAnalysisModel() { return analysisModel; }
    public void setAnalysisModel(String analysisModel) { this.analysisModel = analysisModel; }
}