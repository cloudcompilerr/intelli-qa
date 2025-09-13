package com.agentic.e2etester.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TestEnvironmentConfigurationTest {
    
    private TestEnvironmentConfiguration configuration;
    
    @BeforeEach
    void setUp() {
        configuration = new TestEnvironmentConfiguration();
    }
    
    @Test
    void shouldHaveDefaultEnvironments() {
        assertThat(configuration.getEnvironments()).hasSize(4);
        assertThat(configuration.getEnvironments()).containsKeys("local", "dev", "staging", "production");
    }
    
    @Test
    void shouldHaveDefaultActiveEnvironment() {
        assertThat(configuration.getActiveEnvironment()).isEqualTo("local");
    }
    
    @Test
    void shouldReturnCurrentEnvironment() {
        TestEnvironmentConfiguration.EnvironmentConfig current = configuration.getCurrentEnvironment();
        
        assertThat(current).isNotNull();
        assertThat(current.getName()).isEqualTo("local");
        assertThat(current.isDataIsolationEnabled()).isTrue();
        assertThat(current.isCleanupAfterTest()).isTrue();
    }
    
    @Test
    void shouldReturnSpecificEnvironment() {
        TestEnvironmentConfiguration.EnvironmentConfig dev = configuration.getEnvironment("dev");
        
        assertThat(dev).isNotNull();
        assertThat(dev.getName()).isEqualTo("dev");
        assertThat(dev.getMaxConcurrentTests()).isEqualTo(10);
        assertThat(dev.getTestTimeout()).isEqualTo(Duration.ofMinutes(15));
    }
    
    @Test
    void shouldCheckEnvironmentAvailability() {
        assertThat(configuration.isEnvironmentAvailable("local")).isTrue();
        assertThat(configuration.isEnvironmentAvailable("nonexistent")).isFalse();
    }
    
    @Test
    void shouldSetActiveEnvironment() {
        configuration.setActiveEnvironment("staging");
        
        assertThat(configuration.getActiveEnvironment()).isEqualTo("staging");
        
        TestEnvironmentConfiguration.EnvironmentConfig current = configuration.getCurrentEnvironment();
        assertThat(current.getName()).isEqualTo("staging");
        assertThat(current.isCleanupAfterTest()).isFalse();
    }
    
    @Test
    void shouldConfigureEnvironmentProperties() {
        TestEnvironmentConfiguration.EnvironmentConfig env = configuration.getEnvironment("local");
        
        env.getProperties().put("custom.property", "test-value");
        
        assertThat(env.getProperty("custom.property")).isEqualTo("test-value");
        assertThat(env.getProperty("nonexistent", "default")).isEqualTo("default");
    }
    
    @Test
    void shouldValidateProductionEnvironmentSettings() {
        TestEnvironmentConfiguration.EnvironmentConfig prod = configuration.getEnvironment("production");
        
        assertThat(prod.getName()).isEqualTo("production");
        assertThat(prod.isDataIsolationEnabled()).isTrue();
        assertThat(prod.isCleanupAfterTest()).isFalse(); // Don't cleanup in production
        assertThat(prod.getMaxConcurrentTests()).isEqualTo(50);
        assertThat(prod.getTestTimeout()).isEqualTo(Duration.ofHours(1));
    }
}