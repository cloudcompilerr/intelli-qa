package com.agentic.e2etester.config;

import com.agentic.e2etester.testdata.DefaultTestDataManager;
import com.agentic.e2etester.testdata.TestDataGenerator;
import com.agentic.e2etester.testdata.TestDataManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for test data management
 */
@Configuration
@ConfigurationProperties(prefix = "agentic.test-data")
public class TestDataConfiguration {
    
    private boolean enabled = true;
    private boolean autoCleanup = true;
    private Duration cleanupDelay = Duration.ofMinutes(5);
    private int maxDataSizePerTest = 1000;
    private boolean enableSnapshots = true;
    private int maxSnapshots = 10;
    private boolean enableValidation = true;
    private Duration validationTimeout = Duration.ofSeconds(30);
    
    @Bean
    public TestDataManager testDataManager(TestEnvironmentConfiguration environmentConfig,
                                         TestDataGenerator dataGenerator) {
        return new DefaultTestDataManager(dataGenerator, environmentConfig);
    }
    
    @Bean
    public TestDataGenerator testDataGenerator() {
        return new TestDataGenerator();
    }
    
    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isAutoCleanup() {
        return autoCleanup;
    }
    
    public void setAutoCleanup(boolean autoCleanup) {
        this.autoCleanup = autoCleanup;
    }
    
    public Duration getCleanupDelay() {
        return cleanupDelay;
    }
    
    public void setCleanupDelay(Duration cleanupDelay) {
        this.cleanupDelay = cleanupDelay;
    }
    
    public int getMaxDataSizePerTest() {
        return maxDataSizePerTest;
    }
    
    public void setMaxDataSizePerTest(int maxDataSizePerTest) {
        this.maxDataSizePerTest = maxDataSizePerTest;
    }
    
    public boolean isEnableSnapshots() {
        return enableSnapshots;
    }
    
    public void setEnableSnapshots(boolean enableSnapshots) {
        this.enableSnapshots = enableSnapshots;
    }
    
    public int getMaxSnapshots() {
        return maxSnapshots;
    }
    
    public void setMaxSnapshots(int maxSnapshots) {
        this.maxSnapshots = maxSnapshots;
    }
    
    public boolean isEnableValidation() {
        return enableValidation;
    }
    
    public void setEnableValidation(boolean enableValidation) {
        this.enableValidation = enableValidation;
    }
    
    public Duration getValidationTimeout() {
        return validationTimeout;
    }
    
    public void setValidationTimeout(Duration validationTimeout) {
        this.validationTimeout = validationTimeout;
    }
}