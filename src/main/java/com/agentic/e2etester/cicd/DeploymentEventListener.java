package com.agentic.e2etester.cicd;

import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Listens for deployment events and triggers appropriate test executions.
 */
@Component
public class DeploymentEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(DeploymentEventListener.class);
    
    private final List<PipelineIntegration> pipelineIntegrations;
    private final TestExecutionEngine testExecutionEngine;
    private final CiCdConfiguration ciCdConfiguration;
    
    public DeploymentEventListener(List<PipelineIntegration> pipelineIntegrations,
                                 TestExecutionEngine testExecutionEngine,
                                 CiCdConfiguration ciCdConfiguration) {
        this.pipelineIntegrations = pipelineIntegrations;
        this.testExecutionEngine = testExecutionEngine;
        this.ciCdConfiguration = ciCdConfiguration;
    }
    
    /**
     * Processes a deployment event and triggers appropriate tests.
     */
    public CompletableFuture<Void> handleDeploymentEvent(DeploymentEvent deploymentEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Processing deployment event: {} from platform: {}", 
                           deploymentEvent.getEventId(), deploymentEvent.getPlatform());
                
                // Check if auto-triggering is enabled
                if (!ciCdConfiguration.isAutoTriggerEnabled()) {
                    logger.info("Auto-triggering is disabled, skipping event: {}", deploymentEvent.getEventId());
                    return;
                }
                
                // Find appropriate pipeline integration
                PipelineIntegration integration = findIntegration(deploymentEvent);
                if (integration == null) {
                    logger.warn("No suitable pipeline integration found for event: {}", deploymentEvent.getEventId());
                    return;
                }
                
                // Check if this event type should trigger tests
                if (!shouldTriggerTests(deploymentEvent)) {
                    logger.info("Event type {} does not trigger tests, skipping", deploymentEvent.getEventType());
                    return;
                }
                
                // Trigger test execution
                integration.triggerTests(deploymentEvent)
                    .thenCompose(testExecutionEngine::executeTest)
                    .thenAccept(testResult -> {
                        logger.info("Test execution completed for deployment event: {}", deploymentEvent.getEventId());
                        
                        // Report results back to the CI/CD platform
                        integration.reportResults(testResult, deploymentEvent.getMetadata())
                            .exceptionally(throwable -> {
                                logger.error("Failed to report test results for event: {}", 
                                           deploymentEvent.getEventId(), throwable);
                                return null;
                            });
                    })
                    .exceptionally(throwable -> {
                        logger.error("Test execution failed for deployment event: {}", 
                                   deploymentEvent.getEventId(), throwable);
                        return null;
                    });
                
            } catch (Exception e) {
                logger.error("Failed to handle deployment event: {}", deploymentEvent.getEventId(), e);
            }
        });
    }
    
    /**
     * Validates a deployment event before processing.
     */
    public boolean validateDeploymentEvent(DeploymentEvent deploymentEvent) {
        if (deploymentEvent == null) {
            logger.warn("Deployment event is null");
            return false;
        }
        
        if (deploymentEvent.getEventId() == null || deploymentEvent.getEventId().trim().isEmpty()) {
            logger.warn("Deployment event missing event ID");
            return false;
        }
        
        if (deploymentEvent.getPlatform() == null || deploymentEvent.getPlatform().trim().isEmpty()) {
            logger.warn("Deployment event missing platform information");
            return false;
        }
        
        if (deploymentEvent.getEventType() == null) {
            logger.warn("Deployment event missing event type");
            return false;
        }
        
        if (deploymentEvent.getEnvironment() == null || deploymentEvent.getEnvironment().trim().isEmpty()) {
            logger.warn("Deployment event missing environment information");
            return false;
        }
        
        return true;
    }
    
    private PipelineIntegration findIntegration(DeploymentEvent deploymentEvent) {
        return pipelineIntegrations.stream()
                .filter(integration -> integration.supportsEvent(deploymentEvent))
                .findFirst()
                .orElse(null);
    }
    
    private boolean shouldTriggerTests(DeploymentEvent deploymentEvent) {
        // Check if the event type is configured to trigger tests
        List<DeploymentEventType> triggerEvents = ciCdConfiguration.getTriggerEvents();
        if (triggerEvents == null || triggerEvents.isEmpty()) {
            // Default behavior: trigger on deployment completion and post-deployment
            return deploymentEvent.getEventType() == DeploymentEventType.DEPLOYMENT_COMPLETED ||
                   deploymentEvent.getEventType() == DeploymentEventType.POST_DEPLOYMENT;
        }
        
        return triggerEvents.contains(deploymentEvent.getEventType());
    }
}