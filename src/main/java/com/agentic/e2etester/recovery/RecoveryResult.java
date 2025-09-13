package com.agentic.e2etester.recovery;

import com.agentic.e2etester.model.TestFailure;

import java.time.Instant;

/**
 * Result of a recovery operation
 */
public class RecoveryResult {
    
    private final String testId;
    private final TestFailure originalFailure;
    private final boolean recoveryAttempted;
    private final boolean recoverySuccessful;
    private final RollbackResult rollbackResult;
    private final boolean degradationApplied;
    private final Instant timestamp;
    private final String recoveryMessage;
    
    private RecoveryResult(Builder builder) {
        this.testId = builder.testId;
        this.originalFailure = builder.originalFailure;
        this.recoveryAttempted = builder.recoveryAttempted;
        this.recoverySuccessful = builder.recoverySuccessful;
        this.rollbackResult = builder.rollbackResult;
        this.degradationApplied = builder.degradationApplied;
        this.timestamp = Instant.now();
        this.recoveryMessage = builder.recoveryMessage;
    }
    
    // Getters
    public String getTestId() { return testId; }
    public TestFailure getOriginalFailure() { return originalFailure; }
    public boolean isRecoveryAttempted() { return recoveryAttempted; }
    public boolean isRecoverySuccessful() { return recoverySuccessful; }
    public RollbackResult getRollbackResult() { return rollbackResult; }
    public boolean isDegradationApplied() { return degradationApplied; }
    public Instant getTimestamp() { return timestamp; }
    public String getRecoveryMessage() { return recoveryMessage; }
    
    public boolean hasRollback() {
        return rollbackResult != null;
    }
    
    public boolean isRollbackSuccessful() {
        return rollbackResult != null && rollbackResult.isSuccessful();
    }
    
    public static class Builder {
        private final String testId;
        private final TestFailure originalFailure;
        private boolean recoveryAttempted = false;
        private boolean recoverySuccessful = false;
        private RollbackResult rollbackResult;
        private boolean degradationApplied = false;
        private String recoveryMessage;
        
        public Builder(String testId, TestFailure originalFailure) {
            this.testId = testId;
            this.originalFailure = originalFailure;
        }
        
        public Builder withRecoveryAttempted(boolean attempted) {
            this.recoveryAttempted = attempted;
            return this;
        }
        
        public Builder withRecoverySuccessful(boolean successful) {
            this.recoverySuccessful = successful;
            return this;
        }
        
        public Builder withRollbackResult(RollbackResult rollbackResult) {
            this.rollbackResult = rollbackResult;
            return this;
        }
        
        public Builder withDegradationApplied(boolean applied) {
            this.degradationApplied = applied;
            return this;
        }
        
        public Builder withRecoveryMessage(String message) {
            this.recoveryMessage = message;
            return this;
        }
        
        public RecoveryResult build() {
            return new RecoveryResult(this);
        }
    }
}