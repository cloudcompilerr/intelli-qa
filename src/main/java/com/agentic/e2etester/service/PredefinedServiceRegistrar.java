package com.agentic.e2etester.service;

import com.agentic.e2etester.config.ServiceDiscoveryConfiguration;
import com.agentic.e2etester.model.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Component that registers predefined services from configuration at application startup
 */
@Component
public class PredefinedServiceRegistrar {
    
    private static final Logger logger = LoggerFactory.getLogger(PredefinedServiceRegistrar.class);
    
    private final ServiceDiscoveryManager serviceDiscoveryManager;
    private final ServiceDiscoveryConfiguration configuration;
    
    @Autowired
    public PredefinedServiceRegistrar(ServiceDiscoveryManager serviceDiscoveryManager,
                                    ServiceDiscoveryConfiguration configuration) {
        this.serviceDiscoveryManager = serviceDiscoveryManager;
        this.configuration = configuration;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void registerPredefinedServices() {
        if (configuration.getPredefinedServices() == null || configuration.getPredefinedServices().isEmpty()) {
            logger.info("No predefined services configured");
            return;
        }
        
        logger.info("Registering {} predefined services", configuration.getPredefinedServices().size());
        
        for (ServiceDiscoveryConfiguration.PredefinedService predefinedService : configuration.getPredefinedServices()) {
            try {
                ServiceInfo serviceInfo = convertToServiceInfo(predefinedService);
                serviceDiscoveryManager.registerService(serviceInfo);
                logger.info("Registered predefined service: {} at {}", 
                    serviceInfo.getServiceName(), serviceInfo.getBaseUrl());
            } catch (Exception e) {
                logger.error("Failed to register predefined service {}: {}", 
                    predefinedService.getServiceId(), e.getMessage(), e);
            }
        }
        
        logger.info("Completed registration of predefined services");
    }
    
    private ServiceInfo convertToServiceInfo(ServiceDiscoveryConfiguration.PredefinedService predefinedService) {
        ServiceInfo serviceInfo = new ServiceInfo(
            predefinedService.getServiceId(),
            predefinedService.getServiceName(),
            predefinedService.getBaseUrl()
        );
        
        serviceInfo.setVersion(predefinedService.getVersion());
        serviceInfo.setMetadata(predefinedService.getMetadata());
        
        // Initialize empty endpoints set - will be populated by discovery
        serviceInfo.setEndpoints(new HashSet<>());
        
        return serviceInfo;
    }
}