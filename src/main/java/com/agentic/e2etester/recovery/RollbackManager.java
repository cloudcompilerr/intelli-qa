package com.agentic.e2etester.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manager for rollback operations during test failures
 */
@Component
public class RollbackManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RollbackManager.class);
    
    private final Map<String, Queue<RollbackAction>> testRollbackActions;
    private final Map<String, Set<String>> executedActions;
    
    public RollbackManager() {
        this.testRollbackActions = new ConcurrentHashMap<>();
        this.executedActions = new ConcurrentHashMap<>();
    }
    
    /**
     * Register a rollback action for a test
     */
    public void registerRollbackAction(String testId, RollbackAction action) {
        testRollbackActions.computeIfAbsent(testId, k -> new ConcurrentLinkedQueue<>()).offer(action);
        logger.debug("Registered rollback action {} for test {}: {}", 
            action.getId(), testId, action.getDescription());
    }
    
    /**
     * Execute all rollback actions for a test
     */
    public CompletableFuture<RollbackResult> executeRollback(String testId) {
        Queue<RollbackAction> actions = testRollbackActions.get(testId);
        if (actions == null || actions.isEmpty()) {
            logger.info("No rollback actions to execute for test {}", testId);
            return CompletableFuture.completedFuture(new RollbackResult(testId, true, Collections.emptyList()));
        }
        
        logger.info("Starting rollback for test {} with {} actions", testId, actions.size());
        
        // Sort actions by priority (highest first)
        List<RollbackAction> sortedActions = new ArrayList<>(actions);
        sortedActions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        List<RollbackActionResult> results = new ArrayList<>();
        Set<String> executed = executedActions.computeIfAbsent(testId, k -> ConcurrentHashMap.newKeySet());
        
        return executeRollbackActions(sortedActions, executed, results)
            .thenApply(success -> {
                boolean allSuccessful = results.stream().allMatch(RollbackActionResult::isSuccessful);
                logger.info("Rollback for test {} completed. Success: {}, Actions: {}/{}", 
                    testId, allSuccessful, 
                    results.stream().mapToInt(r -> r.isSuccessful() ? 1 : 0).sum(),
                    results.size());
                
                return new RollbackResult(testId, allSuccessful, results);
            });
    }
    
    /**
     * Execute rollback actions for specific services
     */
    public CompletableFuture<RollbackResult> executeRollbackForServices(String testId, Set<String> serviceIds) {
        Queue<RollbackAction> actions = testRollbackActions.get(testId);
        if (actions == null || actions.isEmpty()) {
            return CompletableFuture.completedFuture(new RollbackResult(testId, true, Collections.emptyList()));
        }
        
        // Filter actions for specified services
        List<RollbackAction> filteredActions = actions.stream()
            .filter(action -> serviceIds.contains(action.getServiceId()))
            .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
            .toList();
        
        if (filteredActions.isEmpty()) {
            logger.info("No rollback actions found for services {} in test {}", serviceIds, testId);
            return CompletableFuture.completedFuture(new RollbackResult(testId, true, Collections.emptyList()));
        }
        
        logger.info("Starting partial rollback for test {} with {} actions for services {}", 
            testId, filteredActions.size(), serviceIds);
        
        List<RollbackActionResult> results = new ArrayList<>();
        Set<String> executed = executedActions.computeIfAbsent(testId, k -> ConcurrentHashMap.newKeySet());
        
        return executeRollbackActions(filteredActions, executed, results)
            .thenApply(success -> {
                boolean allSuccessful = results.stream().allMatch(RollbackActionResult::isSuccessful);
                return new RollbackResult(testId, allSuccessful, results);
            });
    }
    
    /**
     * Check if rollback is needed for a test
     */
    public boolean hasRollbackActions(String testId) {
        Queue<RollbackAction> actions = testRollbackActions.get(testId);
        return actions != null && !actions.isEmpty();
    }
    
    /**
     * Get the number of rollback actions for a test
     */
    public int getRollbackActionCount(String testId) {
        Queue<RollbackAction> actions = testRollbackActions.get(testId);
        return actions != null ? actions.size() : 0;
    }
    
    /**
     * Clear rollback actions for a test
     */
    public void clearRollbackActions(String testId) {
        testRollbackActions.remove(testId);
        executedActions.remove(testId);
        logger.debug("Cleared rollback actions for test {}", testId);
    }
    
    /**
     * Get all tests with pending rollback actions
     */
    public Set<String> getTestsWithRollbackActions() {
        return new HashSet<>(testRollbackActions.keySet());
    }
    
    private CompletableFuture<Boolean> executeRollbackActions(
            List<RollbackAction> actions, 
            Set<String> executed, 
            List<RollbackActionResult> results) {
        
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        
        for (RollbackAction action : actions) {
            future = future.thenCompose(previousSuccess -> {
                // Skip if already executed
                if (executed.contains(action.getId())) {
                    logger.debug("Skipping already executed rollback action {}", action.getId());
                    return CompletableFuture.completedFuture(previousSuccess);
                }
                
                // Check if action can be executed
                if (!action.canExecute()) {
                    logger.warn("Rollback action {} cannot be executed, skipping", action.getId());
                    results.add(new RollbackActionResult(action.getId(), action.getDescription(), 
                        false, "Action cannot be executed"));
                    return CompletableFuture.completedFuture(false);
                }
                
                logger.info("Executing rollback action {}: {}", action.getId(), action.getDescription());
                
                return action.execute()
                    .handle((result, throwable) -> {
                        executed.add(action.getId());
                        
                        if (throwable != null) {
                            logger.error("Rollback action {} failed", action.getId(), throwable);
                            results.add(new RollbackActionResult(action.getId(), action.getDescription(), 
                                false, throwable.getMessage()));
                            return false;
                        } else {
                            logger.info("Rollback action {} completed successfully", action.getId());
                            results.add(new RollbackActionResult(action.getId(), action.getDescription(), 
                                true, null));
                            return previousSuccess;
                        }
                    });
            });
        }
        
        return future;
    }
}