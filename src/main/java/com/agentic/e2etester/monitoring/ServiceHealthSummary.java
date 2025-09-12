package com.agentic.e2etester.monitoring;

import java.time.Instant;
import java.util.Map;

/**
 * Summary of service health information.
 */
public class ServiceHealthSummary {
    
    private final Map<String, ServiceHealthInfo> serviceHealth;
    
    public ServiceHealthSummary(Map<String, ServiceHealthInfo> serviceHealth) {
        this.serviceHealth = serviceHealth;
    }
    
    public Map<String, ServiceHealthInfo> getServiceHealth() {
        return serviceHealth;
    }
    
    public long getHealthyServices() {
        return serviceHealth.values().stream()
                .mapToLong(health -> health.isHealthy() ? 1 : 0)
                .sum();
    }
    
    /**
     * Individual service health information.
     */
    public static class ServiceHealthInfo {
        private final String serviceId;
        private final long totalInteractions;
        private final long successfulInteractions;
        private final double averageResponseTime;
        private final Instant lastInteractionTime;
        
        public ServiceHealthInfo(String serviceId, long totalInteractions, long successfulInteractions,
                               double averageResponseTime, Instant lastInteractionTime) {
            this.serviceId = serviceId;
            this.totalInteractions = totalInteractions;
            this.successfulInteractions = successfulInteractions;
            this.averageResponseTime = averageResponseTime;
            this.lastInteractionTime = lastInteractionTime;
        }
        
        public boolean isHealthy() {
            if (totalInteractions == 0) return true; // No interactions yet
            double successRate = (double) successfulInteractions / totalInteractions;
            return successRate >= 0.95; // 95% success rate threshold
        }
        
        // Getters
        public String getServiceId() {
            return serviceId;
        }
        
        public long getTotalInteractions() {
            return totalInteractions;
        }
        
        public long getSuccessfulInteractions() {
            return successfulInteractions;
        }
        
        public double getAverageResponseTime() {
            return averageResponseTime;
        }
        
        public Instant getLastInteractionTime() {
            return lastInteractionTime;
        }
    }
}