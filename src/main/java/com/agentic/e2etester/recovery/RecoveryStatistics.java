package com.agentic.e2etester.recovery;

/**
 * Statistics about the recovery system
 */
public class RecoveryStatistics {
    
    private final int totalCircuitBreakers;
    private final int openCircuitBreakers;
    private final int degradedServices;
    private final int testsWithRollbackActions;
    
    public RecoveryStatistics(int totalCircuitBreakers, int openCircuitBreakers, 
                            int degradedServices, int testsWithRollbackActions) {
        this.totalCircuitBreakers = totalCircuitBreakers;
        this.openCircuitBreakers = openCircuitBreakers;
        this.degradedServices = degradedServices;
        this.testsWithRollbackActions = testsWithRollbackActions;
    }
    
    public int getTotalCircuitBreakers() { return totalCircuitBreakers; }
    public int getOpenCircuitBreakers() { return openCircuitBreakers; }
    public int getDegradedServices() { return degradedServices; }
    public int getTestsWithRollbackActions() { return testsWithRollbackActions; }
    
    public double getCircuitBreakerHealthPercentage() {
        if (totalCircuitBreakers == 0) return 100.0;
        return ((double) (totalCircuitBreakers - openCircuitBreakers) / totalCircuitBreakers) * 100.0;
    }
}