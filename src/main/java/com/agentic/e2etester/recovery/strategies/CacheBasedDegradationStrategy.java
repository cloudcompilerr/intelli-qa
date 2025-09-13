package com.agentic.e2etester.recovery.strategies;

import com.agentic.e2etester.recovery.DegradationLevel;
import com.agentic.e2etester.recovery.DegradationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Degradation strategy that uses cached responses when services are unavailable
 */
@Component
public class CacheBasedDegradationStrategy implements DegradationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheBasedDegradationStrategy.class);
    
    private final Map<String, Object> responseCache = new ConcurrentHashMap<>();
    private final Set<String> supportedOperations = Set.of(
        "getServiceHealth", "getServiceInfo", "validateData", "queryDatabase"
    );
    
    @Override
    public DegradationLevel getDegradationLevel() {
        return DegradationLevel.MINIMAL;
    }
    
    @Override
    public boolean canHandle(Throwable failure, String serviceId) {
        // Can handle network and service failures
        return failure instanceof java.net.ConnectException ||
               failure instanceof java.net.SocketTimeoutException ||
               failure.getMessage().contains("Connection refused") ||
               failure.getMessage().contains("Service unavailable");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> executeDegraded(
            String operationName, 
            String serviceId, 
            Throwable originalFailure, 
            Object... parameters) {
        
        String cacheKey = serviceId + ":" + operationName;
        Object cachedResponse = responseCache.get(cacheKey);
        
        if (cachedResponse != null) {
            logger.info("Returning cached response for {} on service {} due to: {}", 
                operationName, serviceId, originalFailure.getMessage());
            return CompletableFuture.completedFuture((T) cachedResponse);
        }
        
        // Return default response based on operation type
        T defaultResponse = getDefaultResponse(operationName, serviceId);
        if (defaultResponse != null) {
            logger.info("Returning default response for {} on service {} due to: {}", 
                operationName, serviceId, originalFailure.getMessage());
            return CompletableFuture.completedFuture(defaultResponse);
        }
        
        // Cannot provide degraded response
        return CompletableFuture.failedFuture(originalFailure);
    }
    
    @Override
    public Set<String> getSupportedOperations() {
        return supportedOperations;
    }
    
    @Override
    public String getDescription() {
        return "Cache-based degradation - returns cached or default responses when services are unavailable";
    }
    
    /**
     * Cache a response for future degraded operations
     */
    public void cacheResponse(String serviceId, String operationName, Object response) {
        String cacheKey = serviceId + ":" + operationName;
        responseCache.put(cacheKey, response);
        logger.debug("Cached response for {} on service {}", operationName, serviceId);
    }
    
    /**
     * Clear cache for a service
     */
    public void clearCache(String serviceId) {
        responseCache.entrySet().removeIf(entry -> entry.getKey().startsWith(serviceId + ":"));
        logger.debug("Cleared cache for service {}", serviceId);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getDefaultResponse(String operationName, String serviceId) {
        return switch (operationName) {
            case "getServiceHealth" -> (T) createDefaultHealthResponse(serviceId);
            case "getServiceInfo" -> (T) createDefaultServiceInfo(serviceId);
            case "validateData" -> (T) Boolean.FALSE; // Conservative default
            case "queryDatabase" -> (T) java.util.Collections.emptyList();
            default -> null;
        };
    }
    
    private Object createDefaultHealthResponse(String serviceId) {
        return Map.of(
            "status", "DEGRADED",
            "serviceId", serviceId,
            "message", "Service health check failed, using degraded response"
        );
    }
    
    private Object createDefaultServiceInfo(String serviceId) {
        return Map.of(
            "serviceId", serviceId,
            "status", "UNKNOWN",
            "version", "unknown",
            "degraded", true
        );
    }
}