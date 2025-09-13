package com.agentic.e2etester.recovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RollbackManagerTest {
    
    private RollbackManager rollbackManager;
    
    @BeforeEach
    void setUp() {
        rollbackManager = new RollbackManager();
    }
    
    @Test
    void shouldRegisterRollbackAction() {
        TestRollbackAction action = new TestRollbackAction("action1", "service1", 10);
        rollbackManager.registerRollbackAction("test1", action);
        
        assertTrue(rollbackManager.hasRollbackActions("test1"));
        assertEquals(1, rollbackManager.getRollbackActionCount("test1"));
    }
    
    @Test
    void shouldExecuteRollbackActions() {
        TestRollbackAction action1 = new TestRollbackAction("action1", "service1", 10);
        TestRollbackAction action2 = new TestRollbackAction("action2", "service1", 5);
        
        rollbackManager.registerRollbackAction("test1", action1);
        rollbackManager.registerRollbackAction("test1", action2);
        
        RollbackResult result = rollbackManager.executeRollback("test1").join();
        
        assertTrue(result.isSuccessful());
        assertEquals("test1", result.getTestId());
        assertEquals(2, result.getActionResults().size());
        assertEquals(2, result.getSuccessfulActionCount());
        assertEquals(0, result.getFailedActionCount());
        
        assertTrue(action1.wasExecuted());
        assertTrue(action2.wasExecuted());
    }
    
    @Test
    void shouldExecuteActionsInPriorityOrder() {
        TestRollbackAction lowPriority = new TestRollbackAction("low", "service1", 1);
        TestRollbackAction highPriority = new TestRollbackAction("high", "service1", 10);
        TestRollbackAction mediumPriority = new TestRollbackAction("medium", "service1", 5);
        
        rollbackManager.registerRollbackAction("test1", lowPriority);
        rollbackManager.registerRollbackAction("test1", highPriority);
        rollbackManager.registerRollbackAction("test1", mediumPriority);
        
        RollbackResult result = rollbackManager.executeRollback("test1").join();
        
        assertTrue(result.isSuccessful());
        
        // Verify execution order by checking execution times
        assertTrue(highPriority.getExecutionTime() < mediumPriority.getExecutionTime());
        assertTrue(mediumPriority.getExecutionTime() < lowPriority.getExecutionTime());
    }
    
    @Test
    void shouldHandleFailedRollbackActions() {
        TestRollbackAction successAction = new TestRollbackAction("success", "service1", 10);
        TestRollbackAction failAction = new TestRollbackAction("fail", "service1", 5);
        failAction.setShouldFail(true);
        
        rollbackManager.registerRollbackAction("test1", successAction);
        rollbackManager.registerRollbackAction("test1", failAction);
        
        RollbackResult result = rollbackManager.executeRollback("test1").join();
        
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getSuccessfulActionCount());
        assertEquals(1, result.getFailedActionCount());
        assertEquals(1, result.getFailedActions().size());
        
        assertTrue(successAction.wasExecuted());
        assertTrue(failAction.wasExecuted());
    }
    
    @Test
    void shouldSkipNonExecutableActions() {
        TestRollbackAction executableAction = new TestRollbackAction("executable", "service1", 10);
        TestRollbackAction nonExecutableAction = new TestRollbackAction("non-executable", "service1", 5);
        nonExecutableAction.setCanExecute(false);
        
        rollbackManager.registerRollbackAction("test1", executableAction);
        rollbackManager.registerRollbackAction("test1", nonExecutableAction);
        
        RollbackResult result = rollbackManager.executeRollback("test1").join();
        
        assertFalse(result.isSuccessful()); // One action failed (non-executable)
        assertEquals(1, result.getSuccessfulActionCount());
        assertEquals(1, result.getFailedActionCount());
        
        assertTrue(executableAction.wasExecuted());
        assertFalse(nonExecutableAction.wasExecuted());
    }
    
    @Test
    void shouldExecuteRollbackForSpecificServices() {
        TestRollbackAction service1Action = new TestRollbackAction("action1", "service1", 10);
        TestRollbackAction service2Action = new TestRollbackAction("action2", "service2", 10);
        TestRollbackAction service3Action = new TestRollbackAction("action3", "service3", 10);
        
        rollbackManager.registerRollbackAction("test1", service1Action);
        rollbackManager.registerRollbackAction("test1", service2Action);
        rollbackManager.registerRollbackAction("test1", service3Action);
        
        RollbackResult result = rollbackManager.executeRollbackForServices(
            "test1", Set.of("service1", "service3")).join();
        
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getSuccessfulActionCount());
        
        assertTrue(service1Action.wasExecuted());
        assertFalse(service2Action.wasExecuted());
        assertTrue(service3Action.wasExecuted());
    }
    
    @Test
    void shouldReturnSuccessForNoRollbackActions() {
        RollbackResult result = rollbackManager.executeRollback("empty-test").join();
        
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getActionResults().size());
    }
    
    @Test
    void shouldClearRollbackActions() {
        TestRollbackAction action = new TestRollbackAction("action1", "service1", 10);
        rollbackManager.registerRollbackAction("test1", action);
        
        assertTrue(rollbackManager.hasRollbackActions("test1"));
        
        rollbackManager.clearRollbackActions("test1");
        
        assertFalse(rollbackManager.hasRollbackActions("test1"));
        assertEquals(0, rollbackManager.getRollbackActionCount("test1"));
    }
    
    @Test
    void shouldGetTestsWithRollbackActions() {
        TestRollbackAction action1 = new TestRollbackAction("action1", "service1", 10);
        TestRollbackAction action2 = new TestRollbackAction("action2", "service2", 10);
        
        rollbackManager.registerRollbackAction("test1", action1);
        rollbackManager.registerRollbackAction("test2", action2);
        
        Set<String> testsWithActions = rollbackManager.getTestsWithRollbackActions();
        
        assertEquals(2, testsWithActions.size());
        assertTrue(testsWithActions.contains("test1"));
        assertTrue(testsWithActions.contains("test2"));
    }
    
    @Test
    void shouldNotExecuteAlreadyExecutedActions() {
        TestRollbackAction action = new TestRollbackAction("action1", "service1", 10);
        rollbackManager.registerRollbackAction("test1", action);
        
        // Execute rollback first time
        RollbackResult result1 = rollbackManager.executeRollback("test1").join();
        assertTrue(result1.isSuccessful());
        assertTrue(action.wasExecuted());
        
        // Reset action state and execute again
        action.reset();
        RollbackResult result2 = rollbackManager.executeRollback("test1").join();
        
        assertTrue(result2.isSuccessful());
        assertFalse(action.wasExecuted()); // Should not execute again
    }
    
    // Test helper class
    private static class TestRollbackAction implements RollbackAction {
        private final String id;
        private final String serviceId;
        private final int priority;
        private boolean canExecute = true;
        private boolean shouldFail = false;
        private final AtomicBoolean executed = new AtomicBoolean(false);
        private long executionTime = 0;
        
        public TestRollbackAction(String id, String serviceId, int priority) {
            this.id = id;
            this.serviceId = serviceId;
            this.priority = priority;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public String getDescription() {
            return "Test rollback action: " + id;
        }
        
        @Override
        public CompletableFuture<Void> execute() {
            executionTime = System.nanoTime();
            executed.set(true);
            
            if (shouldFail) {
                return CompletableFuture.failedFuture(new RuntimeException("Test failure"));
            }
            
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        public boolean canExecute() {
            return canExecute;
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        public String getServiceId() {
            return serviceId;
        }
        
        public boolean wasExecuted() {
            return executed.get();
        }
        
        public long getExecutionTime() {
            return executionTime;
        }
        
        public void setCanExecute(boolean canExecute) {
            this.canExecute = canExecute;
        }
        
        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }
        
        public void reset() {
            executed.set(false);
            executionTime = 0;
        }
    }
}