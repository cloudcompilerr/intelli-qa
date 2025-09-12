package com.agentic.e2etester.integration.rest;

import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.service.ServiceDiscovery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for dynamic endpoint discovery and OpenAPI documentation parsing.
 */
@Service
public class EndpointDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(EndpointDiscoveryService.class);
    
    private final ServiceDiscovery serviceDiscovery;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ServiceInfo> endpointCache = new ConcurrentHashMap<>();
    
    public EndpointDiscoveryService(ServiceDiscovery serviceDiscovery, 
                                  WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper) {
        this.serviceDiscovery = serviceDiscovery;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * Resolve a full URL for a service endpoint
     */
    public CompletableFuture<String> resolveEndpoint(String serviceId, String endpoint) {
        return CompletableFuture.supplyAsync(() -> serviceDiscovery.getService(serviceId).orElse(null))
                .thenApply(serviceInfo -> {
                    if (serviceInfo == null) {
                        throw new IllegalArgumentException("Service not found: " + serviceId);
                    }
                    
                    String baseUrl = serviceInfo.getBaseUrl();
                    if (baseUrl == null) {
                        throw new IllegalArgumentException("Base URL not available for service: " + serviceId);
                    }
                    
                    // Ensure proper URL formatting
                    if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
                        return baseUrl + "/" + endpoint;
                    } else if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
                        return baseUrl + endpoint.substring(1);
                    } else {
                        return baseUrl + endpoint;
                    }
                });
    }
    
    /**
     * Discover available endpoints for a service using OpenAPI documentation
     */
    public CompletableFuture<ServiceInfo> discoverServiceEndpoints(String serviceId) {
        // Check cache first
        ServiceInfo cachedInfo = endpointCache.get(serviceId);
        if (cachedInfo != null) {
            return CompletableFuture.completedFuture(cachedInfo);
        }
        
        return CompletableFuture.supplyAsync(() -> serviceDiscovery.getService(serviceId).orElse(null))
                .thenCompose(serviceInfo -> {
                    if (serviceInfo == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Try to discover endpoints from OpenAPI documentation
                    return discoverFromOpenAPI(serviceInfo)
                            .thenCompose(openApiEndpoints -> {
                                if (!openApiEndpoints.isEmpty()) {
                                    serviceInfo.setEndpoints(openApiEndpoints);
                                    endpointCache.put(serviceId, serviceInfo);
                                    return CompletableFuture.completedFuture(serviceInfo);
                                }
                                
                                // Fallback to actuator endpoints
                                return discoverFromActuator(serviceInfo)
                                        .thenApply(actuatorEndpoints -> {
                                            serviceInfo.setEndpoints(actuatorEndpoints);
                                            endpointCache.put(serviceId, serviceInfo);
                                            return serviceInfo;
                                        });
                            });
                })
                .exceptionally(throwable -> {
                    logger.warn("Failed to discover endpoints for service {}: {}", serviceId, throwable.getMessage());
                    return null;
                });
    }
    
    /**
     * Discover endpoints from OpenAPI/Swagger documentation
     */
    private CompletableFuture<Set<String>> discoverFromOpenAPI(ServiceInfo serviceInfo) {
        String baseUrl = serviceInfo.getBaseUrl();
        Set<String> endpoints = new HashSet<>();
        
        // Try common OpenAPI documentation paths
        String[] openApiPaths = {
            "/v3/api-docs",
            "/v2/api-docs", 
            "/swagger/v1/swagger.json",
            "/api-docs",
            "/swagger.json"
        };
        
        return tryOpenApiPaths(baseUrl, openApiPaths, 0, endpoints);
    }
    
    private CompletableFuture<Set<String>> tryOpenApiPaths(String baseUrl, String[] paths, 
                                                         int index, Set<String> endpoints) {
        if (index >= paths.length) {
            return CompletableFuture.completedFuture(endpoints);
        }
        
        String openApiUrl = baseUrl + paths[index];
        
        return webClient
                .get()
                .uri(openApiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        JsonNode apiDoc = objectMapper.readTree(response);
                        Set<String> discoveredEndpoints = parseOpenApiEndpoints(apiDoc);
                        endpoints.addAll(discoveredEndpoints);
                        logger.debug("Discovered {} endpoints from OpenAPI at {}", discoveredEndpoints.size(), openApiUrl);
                        return Mono.just(endpoints);
                    } catch (Exception e) {
                        logger.debug("Failed to parse OpenAPI documentation from {}: {}", openApiUrl, e.getMessage());
                        return Mono.just(endpoints);
                    }
                })
                .onErrorResume(error -> {
                    logger.debug("Failed to fetch OpenAPI documentation from {}: {}", openApiUrl, error.getMessage());
                    return Mono.just(endpoints);
                })
                .toFuture()
                .thenCompose(result -> tryOpenApiPaths(baseUrl, paths, index + 1, result));
    }
    
    /**
     * Parse endpoints from OpenAPI JSON documentation
     */
    private Set<String> parseOpenApiEndpoints(JsonNode apiDoc) {
        Set<String> endpoints = new HashSet<>();
        
        // Parse OpenAPI 3.x format
        JsonNode paths = apiDoc.get("paths");
        if (paths != null && paths.isObject()) {
            paths.fieldNames().forEachRemaining(path -> {
                endpoints.add(path);
                
                // Also add HTTP methods for each path
                JsonNode pathNode = paths.get(path);
                if (pathNode.isObject()) {
                    pathNode.fieldNames().forEachRemaining(method -> {
                        if (isHttpMethod(method)) {
                            endpoints.add(method.toUpperCase() + " " + path);
                        }
                    });
                }
            });
        }
        
        return endpoints;
    }
    
    /**
     * Discover endpoints from Spring Boot Actuator
     */
    private CompletableFuture<Set<String>> discoverFromActuator(ServiceInfo serviceInfo) {
        String actuatorUrl = serviceInfo.getBaseUrl() + "/actuator";
        
        return webClient
                .get()
                .uri(actuatorUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode actuatorInfo = objectMapper.readTree(response);
                        Set<String> endpoints = new HashSet<>();
                        
                        // Parse actuator endpoints
                        JsonNode links = actuatorInfo.get("_links");
                        if (links != null && links.isObject()) {
                            links.fieldNames().forEachRemaining(endpoint -> {
                                endpoints.add("/actuator/" + endpoint);
                            });
                        }
                        
                        logger.debug("Discovered {} actuator endpoints for service {}", 
                                   endpoints.size(), serviceInfo.getServiceId());
                        return endpoints;
                    } catch (Exception e) {
                        logger.debug("Failed to parse actuator response: {}", e.getMessage());
                        return new HashSet<String>();
                    }
                })
                .onErrorReturn(new HashSet<>())
                .toFuture();
    }
    
    /**
     * Check if a string represents an HTTP method
     */
    private boolean isHttpMethod(String method) {
        return method != null && (
            method.equalsIgnoreCase("get") ||
            method.equalsIgnoreCase("post") ||
            method.equalsIgnoreCase("put") ||
            method.equalsIgnoreCase("delete") ||
            method.equalsIgnoreCase("patch") ||
            method.equalsIgnoreCase("head") ||
            method.equalsIgnoreCase("options")
        );
    }
    
    /**
     * Clear the endpoint cache for a specific service
     */
    public void clearCache(String serviceId) {
        endpointCache.remove(serviceId);
        logger.debug("Cleared endpoint cache for service: {}", serviceId);
    }
    
    /**
     * Clear all endpoint caches
     */
    public void clearAllCaches() {
        endpointCache.clear();
        logger.debug("Cleared all endpoint caches");
    }
}