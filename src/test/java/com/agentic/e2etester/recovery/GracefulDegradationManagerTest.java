package com.agentic.e2etester.recovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class GracefulDegradationManagerTest {
    
    private GracefulDegradationManager degradationManager;
    private TestDegradationStrategy minimalStrategy;
    private TestDegradationStrategy moderateStrategy;
    
    @BeforeEach
    void setUp() {
        degradationManager = new GracefulDegradationManager();
        
        minimalStrategy = new TestDegradationStrategy(
            DegradationLevel.MINIMAL, 
            Set.of("operation1", "operation2"),
            "Minimal degradation strategy"
        );
        
        moderateStrategy = new TestDegradationStrategy(
            DegradationLevel.MODERATE,
            Set.of("operation2", "operation3"),
            "Moderate degradation strategy"
        );
        
        degradationManager.registerStrategy(minimalStrategy);
        degradationManager.registerStrategy(moderateStrategy);
    }
    
    @Test
    void shouldStartWithNoDegradation() {
        assertEquals(DegradationLevel.NONE, degradationManager.getCurrentDegradationLevel("test-service"));
        assertTrue(degradationManager.getDegradedServices().isEmpty());
    }
    
    @Test
    void shouldSetServiceDegradationLevel() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MINIMAL);
        
        assertEquals(DegradationLevel.MINIMAL, degradationManager.getCurrentDegradationLevel("test-service"));
        assertEquals(1, degradationManager.getDegradedServices().size());
        assertEquals(DegradationLevel.MINIMAL, degradationManager.getDegradedServices().get("test-service"));
    }
    
    @Test
    void shouldSetGlobalDegradationLevel() {
        degradationManager.setGlobalDegradationLevel(DegradationLevel.MODERATE);
        
        assertEquals(DegradationLevel.MODERATE, degradationManager.getCurrentDegradationLevel("any-service"));
    }
    
    @Test
    void shouldUseMoreSevereDegradationLevel() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MINIMAL);
        degradationManager.setGlobalDegradationLevel(DegradationLevel.MODERATE);
        
        // Should use the more severe level (MODERATE)
        assertEquals(DegradationLevel.MODERATE, degradationManager.getCurrentDegradationLevel("test-service"));
    }
    
    @Test
    void shouldExecuteWithDegradation() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MINIMAL);
        
        RuntimeException failure = new RuntimeException("Service failure");
        CompletableFuture<String> result = degradationManager.executeWithDegradation(
            "operation1", "test-service", failure, "param1", "param2"
        );
        
        assertEquals("degraded-response", result.join());
        assertTrue(minimalStrategy.wasExecuted());
    }
    
    @Test
    void shouldTryMoreSevereStrategies() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MINIMAL);
        
        // Make minimal strategy unable to handle this failure
        minimalStrategy.setCanHandle(false);
        
        RuntimeException failure = new RuntimeException("Service failure");
        CompletableFuture<String> result = degradationManager.executeWithDegradation(
            "operation2", "test-service", failure, "param1"
        );
        
        assertEquals("degraded-response", result.join());
        assertFalse(minimalStrategy.wasExecuted());
        assertTrue(moderateStrategy.wasExecuted());
    }
    
    @Test
    void shouldFailIfNoSuitableStrategy() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MINIMAL);
        
        RuntimeException failure = new RuntimeException("Service failure");
        CompletableFuture<String> result = degradationManager.executeWithDegradation(
            "unsupported-operation", "test-service", failure
        );
        
        assertTrue(result.isCompletedExceptionally());
    }
    
    @Test
    void shouldCheckIfOperationShouldBeDegrade() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MINIMAL);
        
        assertTrue(degradationManager.shouldDegrade("test-service", "operation1"));
        assertFalse(degradationManager.shouldDegrade("test-service", "unsupported-operation"));
        assertFalse(degradationManager.shouldDegrade("normal-service", "operation1"));
    }
    
    @Test
    void shouldResetServiceDegradation() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MODERATE);
        assertEquals(DegradationLevel.MODERATE, degradationManager.getCurrentDegradationLevel("test-service"));
        
        degradationManager.resetServiceDegradationLevel("test-service");
        assertEquals(DegradationLevel.NONE, degradationManager.getCurrentDegradationLevel("test-service"));
        assertTrue(degradationManager.getDegradedServices().isEmpty());
    }
    
    @Test
    void shouldResetGlobalDegradation() {
        degradationManager.setGlobalDegradationLevel(DegradationLevel.SEVERE);
        assertEquals(DegradationLevel.SEVERE, degradationManager.getCurrentDegradationLevel("any-service"));
        
        degradationManager.resetGlobalDegradationLevel();
        assertEquals(DegradationLevel.NONE, degradationManager.getCurrentDegradationLevel("any-service"));
    }
    
    @Test
    void shouldUpdateDegradationLevelToMoreSevere() {
        degradationManager.setServiceDegradationLevel("test-service", DegradationLevel.MINIMAL);
        
        // Execute degradation that should upgrade to moderate level
        moderateStrategy.setCanHandle(true);
        RuntimeException failure = new RuntimeException("Severe failure");
        
        degradationManager.executeWithDegradation("operation2", "test-service", failure);
        
        // Should have been upgraded to moderate level
        assertEquals(DegradationLevel.MODERATE, degradationManager.getCurrentDegradationLevel("test-service"));
    }
    
    // Test helper class
    private static class TestDegradationStrategy implements DegradationStrategy {
        private final DegradationLevel level;
        private final Set<String> supportedOperations;
        private final String description;
        private boolean canHandle = true;
        private boolean executed = false;
        
        public TestDegradationStrategy(DegradationLevel level, Set<String> supportedOperations, String description) {
            this.level = level;
            this.supportedOperations = supportedOperations;
            this.description = description;
        }
        
        @Override
        public DegradationLevel getDegradationLevel() {
            return level;
        }
        
        @Override
        public boolean canHandle(Throwable failure, String serviceId) {
            return canHandle;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<T> executeDegraded(String operationName, String serviceId, 
                                                       Throwable originalFailure, Object... parameters) {
            executed = true;
            return CompletableFuture.completedFuture((T) "degraded-response");
        }
        
        @Override
        public Set<String> getSupportedOperations() {
            return supportedOperations;
        }
        
        @Override
        public String getDescription() {
            return description;
        }
        
        public void setCanHandle(boolean canHandle) {
            this.canHandle = canHandle;
        }
        
        public boolean wasExecuted() {
            return executed;
        }
    }
}