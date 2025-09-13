package com.agentic.e2etester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "agentic.test-environment")
public class TestEnvironmentConfiguration {
    
    private String activeEnvironment = "local";
    private Map<String, EnvironmentConfig> environments = new HashMap<>();
    
    public TestEnvironmentConfiguration() {
        initializeDefaultEnvironments();
    }
    
    private void initializeDefaultEnvironments() {
        EnvironmentConfig local = new EnvironmentConfig();
        local.setName("local");
        local.setDescription("Local development environment");
        local.setDataIsolationEnabled(true);
        local.setCleanupAfterTest(true);
        local.setMaxConcurrentTests(5);
        local.setTestTimeout(Duration.ofMinutes(10));
        environments.put("local", local);
        
        EnvironmentConfig dev = new EnvironmentConfig();
        dev.setName("dev");
        dev.setDescription("Development environment");
        dev.setDataIsolationEnabled(true);
        dev.setCleanupAfterTest(true);
        dev.setMaxConcurrentTests(10);
        dev.setTestTimeout(Duration.ofMinutes(15));
        environments.put("dev", dev);
        
        EnvironmentConfig staging = new EnvironmentConfig();
        staging.setName("staging");
        staging.setDescription("Staging environment");
        staging.setDataIsolationEnabled(true);
        staging.setCleanupAfterTest(false);
        staging.setMaxConcurrentTests(20);
        staging.setTestTimeout(Duration.ofMinutes(30));
        environments.put("staging", staging);
        
        EnvironmentConfig prod = new EnvironmentConfig();
        prod.setName("production");
        prod.setDescription("Production environment");
        prod.setDataIsolationEnabled(true);
        prod.setCleanupAfterTest(false);
        prod.setMaxConcurrentTests(50);
        prod.setTestTimeout(Duration.ofHours(1));
        environments.put("production", prod);
    }
    
    public String getActiveEnvironment() {
        return activeEnvironment;
    }
    
    public void setActiveEnvironment(String activeEnvironment) {
        this.activeEnvironment = activeEnvironment;
    }
    
    public Map<String, EnvironmentConfig> getEnvironments() {
        return environments;
    }
    
    public void setEnvironments(Map<String, EnvironmentConfig> environments) {
        this.environments = environments;
    }
    
    public EnvironmentConfig getCurrentEnvironment() {
        return environments.get(activeEnvironment);
    }
    
    public EnvironmentConfig getEnvironment(String name) {
        return environments.get(name);
    }
    
    public boolean isEnvironmentAvailable(String name) {
        return environments.containsKey(name);
    }
    
    public static class EnvironmentConfig {
        private String name;
        private String description;
        private boolean dataIsolationEnabled;
        private boolean cleanupAfterTest;
        private int maxConcurrentTests;
        private Duration testTimeout;
        private Map<String, String> properties = new HashMap<>();
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public boolean isDataIsolationEnabled() {
            return dataIsolationEnabled;
        }
        
        public void setDataIsolationEnabled(boolean dataIsolationEnabled) {
            this.dataIsolationEnabled = dataIsolationEnabled;
        }
        
        public boolean isCleanupAfterTest() {
            return cleanupAfterTest;
        }
        
        public void setCleanupAfterTest(boolean cleanupAfterTest) {
            this.cleanupAfterTest = cleanupAfterTest;
        }
        
        public int getMaxConcurrentTests() {
            return maxConcurrentTests;
        }
        
        public void setMaxConcurrentTests(int maxConcurrentTests) {
            this.maxConcurrentTests = maxConcurrentTests;
        }
        
        public Duration getTestTimeout() {
            return testTimeout;
        }
        
        public void setTestTimeout(Duration testTimeout) {
            this.testTimeout = testTimeout;
        }
        
        public Map<String, String> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
        
        public String getProperty(String key) {
            return properties.get(key);
        }
        
        public String getProperty(String key, String defaultValue) {
            return properties.getOrDefault(key, defaultValue);
        }
    }
}