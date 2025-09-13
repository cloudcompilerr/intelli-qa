package com.agentic.e2etester.cicd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for CI/CD integration.
 */
@Component
@ConfigurationProperties(prefix = "agentic.cicd")
public class CiCdConfiguration {
    
    private boolean autoTriggerEnabled = true;
    private List<DeploymentEventType> triggerEvents;
    private List<String> supportedEnvironments;
    private Duration defaultTestTimeout = Duration.ofMinutes(30);
    private int maxConcurrentTests = 5;
    private Map<String, PipelineConfiguration> pipelines;
    private WebhookConfiguration webhook;
    
    // Getters and setters
    public boolean isAutoTriggerEnabled() { return autoTriggerEnabled; }
    public void setAutoTriggerEnabled(boolean autoTriggerEnabled) { this.autoTriggerEnabled = autoTriggerEnabled; }
    
    public List<DeploymentEventType> getTriggerEvents() { return triggerEvents; }
    public void setTriggerEvents(List<DeploymentEventType> triggerEvents) { this.triggerEvents = triggerEvents; }
    
    public List<String> getSupportedEnvironments() { return supportedEnvironments; }
    public void setSupportedEnvironments(List<String> supportedEnvironments) { 
        this.supportedEnvironments = supportedEnvironments; 
    }
    
    public Duration getDefaultTestTimeout() { return defaultTestTimeout; }
    public void setDefaultTestTimeout(Duration defaultTestTimeout) { this.defaultTestTimeout = defaultTestTimeout; }
    
    public int getMaxConcurrentTests() { return maxConcurrentTests; }
    public void setMaxConcurrentTests(int maxConcurrentTests) { this.maxConcurrentTests = maxConcurrentTests; }
    
    public Map<String, PipelineConfiguration> getPipelines() { return pipelines; }
    public void setPipelines(Map<String, PipelineConfiguration> pipelines) { this.pipelines = pipelines; }
    
    public WebhookConfiguration getWebhook() { return webhook; }
    public void setWebhook(WebhookConfiguration webhook) { this.webhook = webhook; }
    
    public static class WebhookConfiguration {
        private boolean enabled = true;
        private String path = "/webhook/deployment";
        private String secret;
        private boolean validateSignature = true;
        private List<String> allowedIps;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        
        public boolean isValidateSignature() { return validateSignature; }
        public void setValidateSignature(boolean validateSignature) { this.validateSignature = validateSignature; }
        
        public List<String> getAllowedIps() { return allowedIps; }
        public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }
    }
}