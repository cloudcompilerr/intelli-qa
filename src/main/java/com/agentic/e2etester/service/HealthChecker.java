package com.agentic.e2etester.service;

import com.agentic.e2etester.model.HealthCheckResult;
import com.agentic.e2etester.model.ServiceInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for performing health checks on services
 */
public interface HealthChecker {
    
    /**
     * Perform a health check on the given service
     * @param serviceInfo Service to check
     * @return Health check result
     */
    CompletableFuture<HealthCheckResult> checkHealth(ServiceInfo serviceInfo);
    
    /**
     * Check if the health checker supports the given service
     * @param serviceInfo Service to check
     * @return true if this checker can handle the service
     */
    boolean supports(ServiceInfo serviceInfo);
}