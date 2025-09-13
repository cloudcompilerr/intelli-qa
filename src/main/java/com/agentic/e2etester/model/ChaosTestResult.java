package com.agentic.e2etester.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of chaos testing experiments for resilience validation.
 */
public class ChaosTestResult {
    private Object experiment;
    private Instant startTime;
    private Instant endTime;
    private boolean systemRecovered;
    private boolean dataConsistencyMaintained;
    private Duration recoveryTime;
    private int circuitBreakerActivations;
    private boolean partitionTolerance;
    private boolean eventualConsistencyAchieved;
    private boolean dataLossOccurred;
    private boolean databaseFailoverSuccessful;
    private boolean dataReplicationIntact;
    private boolean readWriteOperationsContinued;
    private boolean messageDeliveryGuaranteed;
    private boolean producerResilienceVerified;
    private boolean consumerResilienceVerified;
    private boolean messageLossOccurred;
    private boolean gracefulDegradationActivated;
    private boolean resourceCleanupSuccessful;
    private boolean systemCrashOccurred;
    private boolean cascadeContained;
    private boolean circuitBreakersActivated;
    private boolean partialSystemFunctionality;
    private boolean timeoutHandlingEffective;
    private boolean retryMechanismsWorking;
    private boolean latencyToleranceVerified;
    private boolean securityMaintained;
    private boolean unauthorizedAccessOccurred;
    private boolean fallbackSecurityActivated;
    private boolean dataValidationEffective;
    private boolean corruptionHandlingSuccessful;
    private boolean dataRecoverySuccessful;
    private double overallSystemResilience;
    private boolean businessContinuityMaintained;
    private boolean recoveryTimeAcceptable;
    private List<Exception> errors = new ArrayList<>();

    // Getters and Setters
    public Object getExperiment() { return experiment; }
    public void setExperiment(Object experiment) { this.experiment = experiment; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public boolean getSystemRecovered() { return systemRecovered; }
    public void setSystemRecovered(boolean systemRecovered) { this.systemRecovered = systemRecovered; }

    public boolean getDataConsistencyMaintained() { return dataConsistencyMaintained; }
    public void setDataConsistencyMaintained(boolean dataConsistencyMaintained) { this.dataConsistencyMaintained = dataConsistencyMaintained; }

    public Duration getRecoveryTime() { return recoveryTime; }
    public void setRecoveryTime(Duration recoveryTime) { this.recoveryTime = recoveryTime; }

    public int getCircuitBreakerActivations() { return circuitBreakerActivations; }
    public void setCircuitBreakerActivations(int circuitBreakerActivations) { this.circuitBreakerActivations = circuitBreakerActivations; }

    public boolean getPartitionTolerance() { return partitionTolerance; }
    public void setPartitionTolerance(boolean partitionTolerance) { this.partitionTolerance = partitionTolerance; }

    public boolean getEventualConsistencyAchieved() { return eventualConsistencyAchieved; }
    public void setEventualConsistencyAchieved(boolean eventualConsistencyAchieved) { this.eventualConsistencyAchieved = eventualConsistencyAchieved; }

    public boolean getDataLossOccurred() { return dataLossOccurred; }
    public void setDataLossOccurred(boolean dataLossOccurred) { this.dataLossOccurred = dataLossOccurred; }

    public boolean getDatabaseFailoverSuccessful() { return databaseFailoverSuccessful; }
    public void setDatabaseFailoverSuccessful(boolean databaseFailoverSuccessful) { this.databaseFailoverSuccessful = databaseFailoverSuccessful; }

    public boolean getDataReplicationIntact() { return dataReplicationIntact; }
    public void setDataReplicationIntact(boolean dataReplicationIntact) { this.dataReplicationIntact = dataReplicationIntact; }

    public boolean getReadWriteOperationsContinued() { return readWriteOperationsContinued; }
    public void setReadWriteOperationsContinued(boolean readWriteOperationsContinued) { this.readWriteOperationsContinued = readWriteOperationsContinued; }

    public boolean getMessageDeliveryGuaranteed() { return messageDeliveryGuaranteed; }
    public void setMessageDeliveryGuaranteed(boolean messageDeliveryGuaranteed) { this.messageDeliveryGuaranteed = messageDeliveryGuaranteed; }

    public boolean getProducerResilienceVerified() { return producerResilienceVerified; }
    public void setProducerResilienceVerified(boolean producerResilienceVerified) { this.producerResilienceVerified = producerResilienceVerified; }

    public boolean getConsumerResilienceVerified() { return consumerResilienceVerified; }
    public void setConsumerResilienceVerified(boolean consumerResilienceVerified) { this.consumerResilienceVerified = consumerResilienceVerified; }

    public boolean getMessageLossOccurred() { return messageLossOccurred; }
    public void setMessageLossOccurred(boolean messageLossOccurred) { this.messageLossOccurred = messageLossOccurred; }

    public boolean getGracefulDegradationActivated() { return gracefulDegradationActivated; }
    public void setGracefulDegradationActivated(boolean gracefulDegradationActivated) { this.gracefulDegradationActivated = gracefulDegradationActivated; }

    public boolean getResourceCleanupSuccessful() { return resourceCleanupSuccessful; }
    public void setResourceCleanupSuccessful(boolean resourceCleanupSuccessful) { this.resourceCleanupSuccessful = resourceCleanupSuccessful; }

    public boolean getSystemCrashOccurred() { return systemCrashOccurred; }
    public void setSystemCrashOccurred(boolean systemCrashOccurred) { this.systemCrashOccurred = systemCrashOccurred; }

    public boolean getCascadeContained() { return cascadeContained; }
    public void setCascadeContained(boolean cascadeContained) { this.cascadeContained = cascadeContained; }

    public boolean getCircuitBreakersActivated() { return circuitBreakersActivated; }
    public void setCircuitBreakersActivated(boolean circuitBreakersActivated) { this.circuitBreakersActivated = circuitBreakersActivated; }

    public boolean getPartialSystemFunctionality() { return partialSystemFunctionality; }
    public void setPartialSystemFunctionality(boolean partialSystemFunctionality) { this.partialSystemFunctionality = partialSystemFunctionality; }

    public boolean getTimeoutHandlingEffective() { return timeoutHandlingEffective; }
    public void setTimeoutHandlingEffective(boolean timeoutHandlingEffective) { this.timeoutHandlingEffective = timeoutHandlingEffective; }

    public boolean getRetryMechanismsWorking() { return retryMechanismsWorking; }
    public void setRetryMechanismsWorking(boolean retryMechanismsWorking) { this.retryMechanismsWorking = retryMechanismsWorking; }

    public boolean getLatencyToleranceVerified() { return latencyToleranceVerified; }
    public void setLatencyToleranceVerified(boolean latencyToleranceVerified) { this.latencyToleranceVerified = latencyToleranceVerified; }

    public boolean getSecurityMaintained() { return securityMaintained; }
    public void setSecurityMaintained(boolean securityMaintained) { this.securityMaintained = securityMaintained; }

    public boolean getUnauthorizedAccessOccurred() { return unauthorizedAccessOccurred; }
    public void setUnauthorizedAccessOccurred(boolean unauthorizedAccessOccurred) { this.unauthorizedAccessOccurred = unauthorizedAccessOccurred; }

    public boolean getFallbackSecurityActivated() { return fallbackSecurityActivated; }
    public void setFallbackSecurityActivated(boolean fallbackSecurityActivated) { this.fallbackSecurityActivated = fallbackSecurityActivated; }

    public boolean getDataValidationEffective() { return dataValidationEffective; }
    public void setDataValidationEffective(boolean dataValidationEffective) { this.dataValidationEffective = dataValidationEffective; }

    public boolean getCorruptionHandlingSuccessful() { return corruptionHandlingSuccessful; }
    public void setCorruptionHandlingSuccessful(boolean corruptionHandlingSuccessful) { this.corruptionHandlingSuccessful = corruptionHandlingSuccessful; }

    public boolean getDataRecoverySuccessful() { return dataRecoverySuccessful; }
    public void setDataRecoverySuccessful(boolean dataRecoverySuccessful) { this.dataRecoverySuccessful = dataRecoverySuccessful; }

    public double getOverallSystemResilience() { return overallSystemResilience; }
    public void setOverallSystemResilience(double overallSystemResilience) { this.overallSystemResilience = overallSystemResilience; }

    public boolean getBusinessContinuityMaintained() { return businessContinuityMaintained; }
    public void setBusinessContinuityMaintained(boolean businessContinuityMaintained) { this.businessContinuityMaintained = businessContinuityMaintained; }

    public boolean getRecoveryTimeAcceptable() { return recoveryTimeAcceptable; }
    public void setRecoveryTimeAcceptable(boolean recoveryTimeAcceptable) { this.recoveryTimeAcceptable = recoveryTimeAcceptable; }

    public List<Exception> getErrors() { return errors; }
    public void setErrors(List<Exception> errors) { this.errors = errors; }
    public void addError(Exception error) { this.errors.add(error); }
}