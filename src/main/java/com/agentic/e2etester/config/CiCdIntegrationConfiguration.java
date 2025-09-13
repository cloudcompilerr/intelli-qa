package com.agentic.e2etester.config;

import com.agentic.e2etester.cicd.CiCdConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for CI/CD integration components.
 */
@Configuration
@EnableConfigurationProperties(CiCdConfiguration.class)
public class CiCdIntegrationConfiguration {
    
    // Configuration is handled through @ConfigurationProperties
    // Additional beans can be defined here if needed
}