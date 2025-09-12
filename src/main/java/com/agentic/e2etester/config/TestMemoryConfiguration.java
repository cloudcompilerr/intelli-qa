package com.agentic.e2etester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for test memory and context management components.
 */
@Configuration
public class TestMemoryConfiguration {
    
    /**
     * Provides ObjectMapper bean for JSON serialization/deserialization.
     * 
     * @return configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Register JavaTime module and others
        return mapper;
    }
}