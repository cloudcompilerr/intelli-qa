package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.model.TestContext;
import com.agentic.e2etester.model.TestStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Execution context that tracks the state of a running test execution.
 * Provides thread-safe access to test state and control mechanisms.
 */
public class TestExecutionContext {
    
    private final String testId;
    private final TestContext testContext;
    private final AtomicReference<TestStatus> status;
    private final AtomicReference<CompletableFuture<Void>> executionFuture;
    private volatile boolean pauseRequested;
    private volatile boolean cancelRequested;
    
    public TestExecutionContext(String testId, TestContext testContext) {
        this.testId = testId;
        this.testContext = testContext;
        this.status = new AtomicReference<>(TestStatus.RUNNING);
        this.executionFuture = new AtomicReference<>();
        this.pauseRequested = false;
        this.cancelRequested = false;
    }
    
    public String getTestId() {
        return testId;
    }
    
    public TestContext getTestContext() {
        return testContext;
    }
    
    public TestStatus getStatus() {
        return status.get();
    }
    
    public void setStatus(TestStatus newStatus) {
        status.set(newStatus);
    }
    
    public boolean compareAndSetStatus(TestStatus expected, TestStatus update) {
        return status.compareAndSet(expected, update);
    }
    
    public CompletableFuture<Void> getExecutionFuture() {
        return executionFuture.get();
    }
    
    public void setExecutionFuture(CompletableFuture<Void> future) {
        executionFuture.set(future);
    }
    
    public boolean isPauseRequested() {
        return pauseRequested;
    }
    
    public void requestPause() {
        this.pauseRequested = true;
    }
    
    public void clearPause() {
        this.pauseRequested = false;
    }
    
    public boolean isCancelRequested() {
        return cancelRequested;
    }
    
    public void requestCancel() {
        this.cancelRequested = true;
    }
    
    public boolean shouldStop() {
        return cancelRequested || status.get() == TestStatus.CANCELLED;
    }
    
    public boolean shouldPause() {
        return pauseRequested && !cancelRequested;
    }
}