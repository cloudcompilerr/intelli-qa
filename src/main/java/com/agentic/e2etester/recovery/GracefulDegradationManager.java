package com.agentic.e2etester.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manager for graceful degradation strategies
 */
@Component
public class GracefulDegradationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(GracefulDegradationManager.class);
    
    private final Map<DegradationLevel, List<DegradationStrategy>> strategies;
    private final Map<String, AtomicReference<DegradationLevel>> serviceDegradationLevels;
    private final AtomicReference<DegradationLevel> globalDegradationLevel;
    
    public GracefulDegradationManager() {
        this.strategies = new EnumMap<>(DegradationLevel.class);
        this.serviceDegradationLevels = new ConcurrentHashMap<>();
        this.globalDegradationLevel = new AtomicReference<>(DegradationLevel.NONE);
        
        // Initialize strategy lists
        for (DegradationLevel level : DegradationLevel.values()) {
            strategies.put(level, new ArrayList<>());
        }
    }
    
    /**
     * Register a degradation strategy
     */
    public void registerStrategy(DegradationStrategy strategy) {
        DegradationLevel level = strategy.getDegradationLevel();
        strategies.get(level).add(strategy);
        logger.info("Registered degradation strategy for level {}: {}", level, strategy.getDescription());
    }
    
    /**
     * Attempt to execute an operation with graceful degradation
     */
    public <T> CompletableFuture<T> executeWithDegradation(
            String operationName,
            String serviceId,
            Throwable failure,
            Object... parameters) {
        
        DegradationLevel currentLevel = getCurrentDegradationLevel(serviceId);
        
        // Try strategies from current level to most severe
        for (DegradationLevel level : getDegradationLevelsFrom(currentLevel)) {
            List<DegradationStrategy> levelStrategies = strategies.get(level);
            
            for (DegradationStrategy strategy : levelStrategies) {
                if (strategy.canHandle(failure, serviceId) && 
                    strategy.getSupportedOperations().contains(operationName)) {
                    
                    logger.info("Applying degradation strategy {} for operation {} on service {}", 
                        strategy.getDescription(), operationName, serviceId);
                    
                    // Update degradation level if more severe
                    updateDegradationLevel(serviceId, level);
                    
                    return strategy.executeDegraded(operationName, serviceId, failure, parameters);
                }
            }
        }
        
        // No suitable degradation strategy found
        logger.warn("No degradation strategy found for operation {} on service {} with failure: {}", 
            operationName, serviceId, failure.getMessage());
        
        return CompletableFuture.failedFuture(failure);
    }
    
    /**
     * Check if an operation should be degraded
     */
    public boolean shouldDegrade(String serviceId, String operationName) {
        DegradationLevel currentLevel = getCurrentDegradationLevel(serviceId);
        return currentLevel != DegradationLevel.NONE && hasStrategyFor(currentLevel, operationName);
    }
    
    /**
     * Get current degradation level for a service
     */
    public DegradationLevel getCurrentDegradationLevel(String serviceId) {
        DegradationLevel serviceLevel = serviceDegradationLevels
            .computeIfAbsent(serviceId, k -> new AtomicReference<>(DegradationLevel.NONE))
            .get();
        
        DegradationLevel globalLevel = globalDegradationLevel.get();
        
        // Return the more severe level
        return serviceLevel.isMoreSevereThan(globalLevel) ? serviceLevel : globalLevel;
    }
    
    /**
     * Set degradation level for a specific service
     */
    public void setServiceDegradationLevel(String serviceId, DegradationLevel level) {
        serviceDegradationLevels
            .computeIfAbsent(serviceId, k -> new AtomicReference<>(DegradationLevel.NONE))
            .set(level);
        
        logger.info("Set degradation level for service {} to {}", serviceId, level);
    }
    
    /**
     * Set global degradation level
     */
    public void setGlobalDegradationLevel(DegradationLevel level) {
        globalDegradationLevel.set(level);
        logger.info("Set global degradation level to {}", level);
    }
    
    /**
     * Reset degradation level for a service
     */
    public void resetServiceDegradationLevel(String serviceId) {
        serviceDegradationLevels.remove(serviceId);
        logger.info("Reset degradation level for service {}", serviceId);
    }
    
    /**
     * Reset global degradation level
     */
    public void resetGlobalDegradationLevel() {
        globalDegradationLevel.set(DegradationLevel.NONE);
        logger.info("Reset global degradation level");
    }
    
    /**
     * Get all services currently under degradation
     */
    public Map<String, DegradationLevel> getDegradedServices() {
        Map<String, DegradationLevel> result = new HashMap<>();
        serviceDegradationLevels.forEach((serviceId, levelRef) -> {
            DegradationLevel level = levelRef.get();
            if (level != DegradationLevel.NONE) {
                result.put(serviceId, level);
            }
        });
        return result;
    }
    
    private void updateDegradationLevel(String serviceId, DegradationLevel newLevel) {
        AtomicReference<DegradationLevel> currentLevelRef = serviceDegradationLevels
            .computeIfAbsent(serviceId, k -> new AtomicReference<>(DegradationLevel.NONE));
        
        currentLevelRef.updateAndGet(currentLevel -> 
            newLevel.isMoreSevereThan(currentLevel) ? newLevel : currentLevel);
    }
    
    private List<DegradationLevel> getDegradationLevelsFrom(DegradationLevel startLevel) {
        List<DegradationLevel> levels = new ArrayList<>();
        DegradationLevel[] allLevels = DegradationLevel.values();
        
        for (int i = startLevel.ordinal(); i < allLevels.length; i++) {
            levels.add(allLevels[i]);
        }
        
        return levels;
    }
    
    private boolean hasStrategyFor(DegradationLevel level, String operationName) {
        return strategies.get(level).stream()
            .anyMatch(strategy -> strategy.getSupportedOperations().contains(operationName));
    }
}