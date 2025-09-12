package com.agentic.e2etester.service;

import com.agentic.e2etester.model.HealthCheckResult;
import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Health checker that uses Spring Boot Actuator health endpoints
 */
@Component
public class ActuatorHealthChecker implements HealthChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(ActuatorHealthChecker.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${agentic.health-check.timeout:5}")
    private int timeoutSeconds;
    
    @Value("${agentic.health-check.actuator-path:/actuator/health}")
    private String actuatorHealthPath;
    
    public ActuatorHealthChecker() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<HealthCheckResult> checkHealth(ServiceInfo serviceInfo) {
        String healthUrl = buildHealthUrl(serviceInfo);
        Instant startTime = Instant.now();
        
        logger.debug("Checking health for service {} at {}", serviceInfo.getServiceId(), healthUrl);
        
        return webClient.get()
            .uri(healthUrl)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .map(responseBody -> {
                Duration responseTime = Duration.between(startTime, Instant.now());
                return parseHealthResponse(serviceInfo.getServiceId(), responseBody, responseTime);
            })
            .onErrorResume(throwable -> {
                Duration responseTime = Duration.between(startTime, Instant.now());
                return reactor.core.publisher.Mono.just(
                    createErrorResult(serviceInfo.getServiceId(), throwable, responseTime)
                );
            })
            .toFuture();
    }
    
    @Override
    public boolean supports(ServiceInfo serviceInfo) {
        // This checker supports any service that has a base URL
        // In a real implementation, you might check for specific metadata
        // or service types that indicate Spring Boot Actuator availability
        return serviceInfo != null && 
               serviceInfo.getBaseUrl() != null && 
               !serviceInfo.getBaseUrl().trim().isEmpty();
    }
    
    private String buildHealthUrl(ServiceInfo serviceInfo) {
        String baseUrl = serviceInfo.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        String healthPath = actuatorHealthPath;
        if (!healthPath.startsWith("/")) {
            healthPath = "/" + healthPath;
        }
        
        return baseUrl + healthPath;
    }
    
    private HealthCheckResult parseHealthResponse(String serviceId, String responseBody, Duration responseTime) {
        try {
            JsonNode healthNode = objectMapper.readTree(responseBody);
            HealthCheckResult result = new HealthCheckResult(serviceId, determineStatus(healthNode));
            result.setResponseTime(responseTime);
            result.setDetails(parseHealthDetails(healthNode));
            
            String status = healthNode.path("status").asText();
            result.setStatusMessage("Actuator health status: " + status);
            
            logger.debug("Health check successful for service {}: {} ({}ms)", 
                serviceId, result.getStatus(), responseTime.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.warn("Failed to parse health response for service {}: {}", serviceId, e.getMessage());
            HealthCheckResult result = HealthCheckResult.unhealthy(serviceId, "Failed to parse health response");
            result.setResponseTime(responseTime);
            result.setError(e);
            return result;
        }
    }
    
    private ServiceStatus determineStatus(JsonNode healthNode) {
        String status = healthNode.path("status").asText().toUpperCase();
        
        switch (status) {
            case "UP":
                return ServiceStatus.HEALTHY;
            case "DOWN":
                return ServiceStatus.UNHEALTHY;
            case "OUT_OF_SERVICE":
                return ServiceStatus.MAINTENANCE;
            case "UNKNOWN":
                return ServiceStatus.UNKNOWN;
            default:
                // For custom statuses or partial health, consider as degraded
                return ServiceStatus.DEGRADED;
        }
    }
    
    private Map<String, Object> parseHealthDetails(JsonNode healthNode) {
        Map<String, Object> details = new HashMap<>();
        
        // Extract basic status information
        details.put("status", healthNode.path("status").asText());
        
        // Extract components if available
        JsonNode components = healthNode.path("components");
        if (!components.isMissingNode()) {
            Map<String, Object> componentDetails = new HashMap<>();
            components.fields().forEachRemaining(entry -> {
                String componentName = entry.getKey();
                JsonNode componentNode = entry.getValue();
                Map<String, Object> componentInfo = new HashMap<>();
                componentInfo.put("status", componentNode.path("status").asText());
                
                JsonNode detailsNode = componentNode.path("details");
                if (!detailsNode.isMissingNode()) {
                    componentInfo.put("details", detailsNode);
                }
                
                componentDetails.put(componentName, componentInfo);
            });
            details.put("components", componentDetails);
        }
        
        // Extract groups if available
        JsonNode groups = healthNode.path("groups");
        if (!groups.isMissingNode()) {
            details.put("groups", groups);
        }
        
        return details;
    }
    
    private HealthCheckResult createErrorResult(String serviceId, Throwable throwable, Duration responseTime) {
        HealthCheckResult result;
        
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException webEx = (WebClientResponseException) throwable;
            String message = String.format("HTTP %d: %s", webEx.getRawStatusCode(), webEx.getStatusText());
            result = HealthCheckResult.unhealthy(serviceId, message);
        } else {
            result = HealthCheckResult.unhealthy(serviceId, throwable);
        }
        
        result.setResponseTime(responseTime);
        
        logger.debug("Health check failed for service {}: {} ({}ms)", 
            serviceId, throwable.getMessage(), responseTime.toMillis());
        
        return result;
    }
}