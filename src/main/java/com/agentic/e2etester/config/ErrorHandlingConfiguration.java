package com.agentic.e2etester.config;

import com.agentic.e2etester.recovery.GracefulDegradationManager;
import com.agentic.e2etester.recovery.strategies.CacheBasedDegradationStrategy;
import com.agentic.e2etester.recovery.strategies.SkipNonCriticalDegradationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration for error handling and recovery system
 */
@Configuration
public class ErrorHandlingConfiguration {
    
    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(10, r -> {
            Thread thread = new Thread(r, "error-handling-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Bean
    public GracefulDegradationManager gracefulDegradationManager(
            CacheBasedDegradationStrategy cacheStrategy,
            SkipNonCriticalDegradationStrategy skipStrategy) {
        
        GracefulDegradationManager manager = new GracefulDegradationManager();
        
        // Register degradation strategies
        manager.registerStrategy(cacheStrategy);
        manager.registerStrategy(skipStrategy);
        
        return manager;
    }
}