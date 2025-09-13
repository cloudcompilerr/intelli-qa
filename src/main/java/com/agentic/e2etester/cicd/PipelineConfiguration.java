package com.agentic.e2etester.cicd;

import java.util.List;
import java.util.Map;

/**
 * Configuration for CI/CD pipeline integration.
 */
public class PipelineConfiguration {
    
    private String platform;
    private String baseUrl;
    private String projectId;
    private String apiToken;
    private String webhookSecret;
    private List<String> supportedEnvironments;
    private Map<String, String> customHeaders;
    private Map<String, Object> platformSpecificConfig;
    private TestTriggerConfiguration triggerConfig;
    private ReportingConfiguration reportingConfig;
    
    public PipelineConfiguration() {}
    
    public PipelineConfiguration(String platform, String baseUrl, String projectId) {
        this.platform = platform;
        this.baseUrl = baseUrl;
        this.projectId = projectId;
    }
    
    // Getters and setters
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    
    public List<String> getSupportedEnvironments() { return supportedEnvironments; }
    public void setSupportedEnvironments(List<String> supportedEnvironments) { 
        this.supportedEnvironments = supportedEnvironments; 
    }
    
    public Map<String, String> getCustomHeaders() { return customHeaders; }
    public void setCustomHeaders(Map<String, String> customHeaders) { this.customHeaders = customHeaders; }
    
    public Map<String, Object> getPlatformSpecificConfig() { return platformSpecificConfig; }
    public void setPlatformSpecificConfig(Map<String, Object> platformSpecificConfig) { 
        this.platformSpecificConfig = platformSpecificConfig; 
    }
    
    public TestTriggerConfiguration getTriggerConfig() { return triggerConfig; }
    public void setTriggerConfig(TestTriggerConfiguration triggerConfig) { this.triggerConfig = triggerConfig; }
    
    public ReportingConfiguration getReportingConfig() { return reportingConfig; }
    public void setReportingConfig(ReportingConfiguration reportingConfig) { this.reportingConfig = reportingConfig; }
}