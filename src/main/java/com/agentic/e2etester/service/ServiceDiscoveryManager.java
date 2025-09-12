package com.agentic.e2etester.service;

import com.agentic.e2etester.model.HealthCheckResult;
import com.agentic.e2etester.model.ServiceInfo;
import com.agentic.e2etester.model.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Main service discovery manager that coordinates service discovery and health monitoring
 */
@Service
public class ServiceDiscoveryManager implements ServiceDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryManager.class);
    
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final List<HealthChecker> healthCheckers;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @Value("${agentic.service-discovery.health-check-interval:30}")
    private int healthCheckIntervalSeconds;
    
    @Value("${agentic.service-discovery.discovery-interval:60}")
    private int discoveryIntervalSeconds;
    
    @Value("${agentic.service-discovery.auto-discovery-enabled:true}")
    private boolean autoDiscoveryEnabled;
    
    private ScheduledFuture<?> healthMonitoringTask;
    private ScheduledFuture<?> discoveryTask;
    
    @Autowired
    public ServiceDiscoveryManager(List<HealthChecker> healthCheckers) {
        this.healthCheckers = healthCheckers != null ? healthCheckers : new ArrayList<>();
    }
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing ServiceDiscoveryManager with {} health checkers", healthCheckers.size());
        if (autoDiscoveryEnabled) {
            startAutoDiscovery();
        }
        startHealthMonitoring();
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ServiceDiscoveryManager");
        stopHealthMonitoring();
        stopAutoDiscovery();
        scheduler.shutdown();
        executor.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            executor.shutdownNow();
        }
    }
    
    @Override
    public CompletableFuture<List<ServiceInfo>> discoverServices() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting service discovery");
            List<ServiceInfo> discoveredServices = new ArrayList<>();
            
            // TODO: Implement actual discovery mechanisms
            // For now, this is a placeholder that would be extended with:
            // - Spring Boot Actuator endpoint scanning
            // - Service registry integration (Eureka, Consul, etc.)
            // - Network scanning for known ports
            // - Configuration-based service definitions
            
            logger.info("Discovered {} services", discoveredServices.size());
            return discoveredServices;
        }, executor);
    }
    
    @Override
    public ServiceInfo registerService(ServiceInfo serviceInfo) {
        if (serviceInfo == null || serviceInfo.getServiceId() == null) {
            throw new IllegalArgumentException("Service info and service ID cannot be null");
        }
        
        logger.info("Registering service: {}", serviceInfo);
        services.put(serviceInfo.getServiceId(), serviceInfo);
        
        // Perform initial health check
        checkServiceHealth(serviceInfo.getServiceId())
            .thenAccept(result -> {
                ServiceInfo service = services.get(serviceInfo.getServiceId());
                if (service != null) {
                    service.setStatus(result.getStatus());
                    service.setLastHealthCheck(result.getTimestamp());
                    service.setHealthDetails(result.getDetails());
                }
            })
            .exceptionally(throwable -> {
                logger.warn("Initial health check failed for service {}: {}", 
                    serviceInfo.getServiceId(), throwable.getMessage());
                return null;
            });
        
        return serviceInfo;
    }
    
    @Override
    public boolean unregisterService(String serviceId) {
        if (serviceId == null) {
            return false;
        }
        
        ServiceInfo removed = services.remove(serviceId);
        if (removed != null) {
            logger.info("Unregistered service: {}", serviceId);
            return true;
        }
        return false;
    }
    
    @Override
    public List<ServiceInfo> getAllServices() {
        return new ArrayList<>(services.values());
    }
    
    @Override
    public Optional<ServiceInfo> getService(String serviceId) {
        return Optional.ofNullable(services.get(serviceId));
    }
    
    @Override
    public List<ServiceInfo> getServicesByName(String serviceName) {
        if (serviceName == null) {
            return new ArrayList<>();
        }
        
        return services.values().stream()
            .filter(service -> serviceName.equals(service.getServiceName()))
            .collect(Collectors.toList());
    }
    
    @Override
    public CompletableFuture<HealthCheckResult> checkServiceHealth(String serviceId) {
        ServiceInfo service = services.get(serviceId);
        if (service == null) {
            return CompletableFuture.completedFuture(
                HealthCheckResult.unhealthy(serviceId, "Service not found in registry")
            );
        }
        
        return checkServiceHealth(service);
    }
    
    private CompletableFuture<HealthCheckResult> checkServiceHealth(ServiceInfo service) {
        // Find appropriate health checker
        HealthChecker checker = healthCheckers.stream()
            .filter(hc -> hc.supports(service))
            .findFirst()
            .orElse(null);
        
        if (checker == null) {
            logger.warn("No health checker found for service: {}", service.getServiceId());
            return CompletableFuture.completedFuture(
                HealthCheckResult.unhealthy(service.getServiceId(), "No health checker available")
            );
        }
        
        return checker.checkHealth(service)
            .thenApply(result -> {
                // Update service status
                service.setStatus(result.getStatus());
                service.setLastHealthCheck(result.getTimestamp());
                service.setHealthDetails(result.getDetails());
                return result;
            })
            .exceptionally(throwable -> {
                logger.error("Health check failed for service {}: {}", 
                    service.getServiceId(), throwable.getMessage());
                service.setStatus(ServiceStatus.UNHEALTHY);
                service.setLastHealthCheck(Instant.now());
                return HealthCheckResult.unhealthy(service.getServiceId(), throwable);
            });
    }
    
    @Override
    public CompletableFuture<List<HealthCheckResult>> checkAllServicesHealth() {
        List<CompletableFuture<HealthCheckResult>> futures = services.values().stream()
            .map(this::checkServiceHealth)
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    @Override
    public void startHealthMonitoring() {
        if (healthMonitoringTask != null && !healthMonitoringTask.isDone()) {
            logger.info("Health monitoring is already running");
            return;
        }
        
        logger.info("Starting health monitoring with interval: {} seconds", healthCheckIntervalSeconds);
        healthMonitoringTask = scheduler.scheduleAtFixedRate(
            this::performHealthChecks,
            healthCheckIntervalSeconds,
            healthCheckIntervalSeconds,
            TimeUnit.SECONDS
        );
    }
    
    @Override
    public void stopHealthMonitoring() {
        if (healthMonitoringTask != null) {
            logger.info("Stopping health monitoring");
            healthMonitoringTask.cancel(false);
            healthMonitoringTask = null;
        }
    }
    
    private void startAutoDiscovery() {
        if (discoveryTask != null && !discoveryTask.isDone()) {
            logger.info("Auto discovery is already running");
            return;
        }
        
        logger.info("Starting auto discovery with interval: {} seconds", discoveryIntervalSeconds);
        discoveryTask = scheduler.scheduleAtFixedRate(
            this::performAutoDiscovery,
            0, // Start immediately
            discoveryIntervalSeconds,
            TimeUnit.SECONDS
        );
    }
    
    private void stopAutoDiscovery() {
        if (discoveryTask != null) {
            logger.info("Stopping auto discovery");
            discoveryTask.cancel(false);
            discoveryTask = null;
        }
    }
    
    private void performHealthChecks() {
        try {
            logger.debug("Performing scheduled health checks for {} services", services.size());
            checkAllServicesHealth()
                .thenAccept(results -> {
                    long healthyCount = results.stream()
                        .mapToLong(result -> result.isHealthy() ? 1 : 0)
                        .sum();
                    logger.debug("Health check completed: {}/{} services healthy", 
                        healthyCount, results.size());
                })
                .exceptionally(throwable -> {
                    logger.error("Error during scheduled health checks", throwable);
                    return null;
                });
        } catch (Exception e) {
            logger.error("Unexpected error during health checks", e);
        }
    }
    
    private void performAutoDiscovery() {
        try {
            logger.debug("Performing auto discovery");
            discoverServices()
                .thenAccept(discoveredServices -> {
                    for (ServiceInfo service : discoveredServices) {
                        if (!services.containsKey(service.getServiceId())) {
                            registerService(service);
                        }
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Error during auto discovery", throwable);
                    return null;
                });
        } catch (Exception e) {
            logger.error("Unexpected error during auto discovery", e);
        }
    }
    
    /**
     * Get service registry statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalServices", services.size());
        
        Map<ServiceStatus, Long> statusCounts = services.values().stream()
            .collect(Collectors.groupingBy(
                ServiceInfo::getStatus,
                Collectors.counting()
            ));
        
        stats.put("servicesByStatus", statusCounts);
        stats.put("healthCheckersCount", healthCheckers.size());
        stats.put("autoDiscoveryEnabled", autoDiscoveryEnabled);
        stats.put("healthMonitoringActive", healthMonitoringTask != null && !healthMonitoringTask.isDone());
        
        return stats;
    }
}