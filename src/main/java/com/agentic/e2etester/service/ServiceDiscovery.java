package com.agentic.e2etester.service;

import com.agentic.e2etester.model.HealthCheckResult;
import com.agentic.e2etester.model.ServiceInfo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for service discovery operations
 */
public interface ServiceDiscovery {
    
    /**
     * Discover services automatically using various discovery mechanisms
     * @return List of discovered services
     */
    CompletableFuture<List<ServiceInfo>> discoverServices();
    
    /**
     * Register a service manually
     * @param serviceInfo Service information to register
     * @return The registered service info
     */
    ServiceInfo registerService(ServiceInfo serviceInfo);
    
    /**
     * Unregister a service
     * @param serviceId Service ID to unregister
     * @return true if service was found and removed
     */
    boolean unregisterService(String serviceId);
    
    /**
     * Get all registered services
     * @return List of all registered services
     */
    List<ServiceInfo> getAllServices();
    
    /**
     * Get service by ID
     * @param serviceId Service ID to find
     * @return Optional containing the service if found
     */
    Optional<ServiceInfo> getService(String serviceId);
    
    /**
     * Get services by name
     * @param serviceName Service name to search for
     * @return List of services with matching name
     */
    List<ServiceInfo> getServicesByName(String serviceName);
    
    /**
     * Perform health check on a specific service
     * @param serviceId Service ID to check
     * @return Health check result
     */
    CompletableFuture<HealthCheckResult> checkServiceHealth(String serviceId);
    
    /**
     * Perform health check on all registered services
     * @return List of health check results
     */
    CompletableFuture<List<HealthCheckResult>> checkAllServicesHealth();
    
    /**
     * Start continuous health monitoring
     */
    void startHealthMonitoring();
    
    /**
     * Stop continuous health monitoring
     */
    void stopHealthMonitoring();
}