package com.agentic.e2etester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for service discovery
 */
@Configuration
@ConfigurationProperties(prefix = "agentic.service-discovery")
public class ServiceDiscoveryConfiguration {
    
    /**
     * Whether auto-discovery is enabled
     */
    private boolean autoDiscoveryEnabled = true;
    
    /**
     * Interval in seconds for health checks
     */
    private int healthCheckInterval = 30;
    
    /**
     * Interval in seconds for service discovery
     */
    private int discoveryInterval = 60;
    
    /**
     * Timeout in seconds for health check requests
     */
    private int healthCheckTimeout = 5;
    
    /**
     * Path to actuator health endpoint
     */
    private String actuatorHealthPath = "/actuator/health";
    
    /**
     * Predefined services to register at startup
     */
    private List<PredefinedService> predefinedServices = new ArrayList<>();
    
    /**
     * Discovery mechanisms to enable
     */
    private List<String> discoveryMechanisms = new ArrayList<>();
    
    // Getters and setters
    public boolean isAutoDiscoveryEnabled() {
        return autoDiscoveryEnabled;
    }
    
    public void setAutoDiscoveryEnabled(boolean autoDiscoveryEnabled) {
        this.autoDiscoveryEnabled = autoDiscoveryEnabled;
    }
    
    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }
    
    public void setHealthCheckInterval(int healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }
    
    public int getDiscoveryInterval() {
        return discoveryInterval;
    }
    
    public void setDiscoveryInterval(int discoveryInterval) {
        this.discoveryInterval = discoveryInterval;
    }
    
    public int getHealthCheckTimeout() {
        return healthCheckTimeout;
    }
    
    public void setHealthCheckTimeout(int healthCheckTimeout) {
        this.healthCheckTimeout = healthCheckTimeout;
    }
    
    public String getActuatorHealthPath() {
        return actuatorHealthPath;
    }
    
    public void setActuatorHealthPath(String actuatorHealthPath) {
        this.actuatorHealthPath = actuatorHealthPath;
    }
    
    public List<PredefinedService> getPredefinedServices() {
        return predefinedServices;
    }
    
    public void setPredefinedServices(List<PredefinedService> predefinedServices) {
        this.predefinedServices = predefinedServices;
    }
    
    public List<String> getDiscoveryMechanisms() {
        return discoveryMechanisms;
    }
    
    public void setDiscoveryMechanisms(List<String> discoveryMechanisms) {
        this.discoveryMechanisms = discoveryMechanisms;
    }
    
    /**
     * Configuration for predefined services
     */
    public static class PredefinedService {
        private String serviceId;
        private String serviceName;
        private String baseUrl;
        private String version;
        private Map<String, String> metadata;
        
        // Getters and setters
        public String getServiceId() {
            return serviceId;
        }
        
        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }
        
        public String getServiceName() {
            return serviceName;
        }
        
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public Map<String, String> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }
}