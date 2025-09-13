package com.agentic.e2etester.cicd;

import java.util.List;
import java.util.Map;

/**
 * Configuration for test result reporting.
 */
public class ReportingConfiguration {
    
    private List<ReportFormat> supportedFormats;
    private boolean publishTestResults;
    private boolean publishArtifacts;
    private String artifactPath;
    private Map<String, String> customReportFields;
    private boolean notifyOnFailure;
    private boolean notifyOnSuccess;
    private List<String> notificationChannels;
    private boolean blockPipelineOnFailure;
    
    public ReportingConfiguration() {
        this.publishTestResults = true;
        this.publishArtifacts = true;
        this.notifyOnFailure = true;
        this.notifyOnSuccess = false;
        this.blockPipelineOnFailure = true;
    }
    
    // Getters and setters
    public List<ReportFormat> getSupportedFormats() { return supportedFormats; }
    public void setSupportedFormats(List<ReportFormat> supportedFormats) { 
        this.supportedFormats = supportedFormats; 
    }
    
    public boolean isPublishTestResults() { return publishTestResults; }
    public void setPublishTestResults(boolean publishTestResults) { this.publishTestResults = publishTestResults; }
    
    public boolean isPublishArtifacts() { return publishArtifacts; }
    public void setPublishArtifacts(boolean publishArtifacts) { this.publishArtifacts = publishArtifacts; }
    
    public String getArtifactPath() { return artifactPath; }
    public void setArtifactPath(String artifactPath) { this.artifactPath = artifactPath; }
    
    public Map<String, String> getCustomReportFields() { return customReportFields; }
    public void setCustomReportFields(Map<String, String> customReportFields) { 
        this.customReportFields = customReportFields; 
    }
    
    public boolean isNotifyOnFailure() { return notifyOnFailure; }
    public void setNotifyOnFailure(boolean notifyOnFailure) { this.notifyOnFailure = notifyOnFailure; }
    
    public boolean isNotifyOnSuccess() { return notifyOnSuccess; }
    public void setNotifyOnSuccess(boolean notifyOnSuccess) { this.notifyOnSuccess = notifyOnSuccess; }
    
    public List<String> getNotificationChannels() { return notificationChannels; }
    public void setNotificationChannels(List<String> notificationChannels) { 
        this.notificationChannels = notificationChannels; 
    }
    
    public boolean isBlockPipelineOnFailure() { return blockPipelineOnFailure; }
    public void setBlockPipelineOnFailure(boolean blockPipelineOnFailure) { 
        this.blockPipelineOnFailure = blockPipelineOnFailure; 
    }
}