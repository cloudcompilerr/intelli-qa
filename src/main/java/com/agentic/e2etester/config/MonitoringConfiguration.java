package com.agentic.e2etester.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for monitoring and observability components.
 * Sets up Prometheus metrics, custom meter filters, and monitoring integrations.
 */
@Configuration
public class MonitoringConfiguration {
    
    /**
     * Configure Prometheus meter registry for metrics export
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    /**
     * Configure meter registry with common tags and filters
     */
    @Bean
    public MeterRegistry configureMeterRegistry(PrometheusMeterRegistry prometheusMeterRegistry) {
        prometheusMeterRegistry.config()
                .commonTags("application", "agentic-e2e-tester")
                .commonTags("version", "1.0.0")
                .meterFilter(MeterFilter.deny(id -> {
                    String uri = id.getTag("uri");
                    return uri != null && uri.startsWith("/actuator");
                }));
        return prometheusMeterRegistry;
    }
}