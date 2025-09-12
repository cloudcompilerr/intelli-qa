package com.agentic.e2etester.integration.rest;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for REST API integration components.
 */
@Configuration
public class RestIntegrationConfiguration {
    
    @Value("${agentic.e2etester.rest.connection-timeout:10000}")
    private int connectionTimeoutMs;
    
    @Value("${agentic.e2etester.rest.read-timeout:30000}")
    private int readTimeoutMs;
    
    @Value("${agentic.e2etester.rest.write-timeout:30000}")
    private int writeTimeoutMs;
    
    @Value("${agentic.e2etester.rest.max-connections:100}")
    private int maxConnections;
    
    @Value("${agentic.e2etester.rest.max-connections-per-route:20}")
    private int maxConnectionsPerRoute;
    
    /**
     * Configure WebClient with optimized settings for microservice communication
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        
        // Configure HTTP client with timeouts and connection pooling
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS))
                );
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // Increase buffer size for large responses
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
                });
    }
}