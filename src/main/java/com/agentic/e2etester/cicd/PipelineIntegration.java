package com.agentic.e2etester.cicd;

import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.model.TestResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for CI/CD pipeline integrations.
 * Provides common functionality for triggering tests and reporting results.
 */
public interface PipelineIntegration {
    
    /**
     * Gets the name of the CI/CD platform this integration supports.
     * 
     * @return the platform name (e.g., "jenkins", "gitlab", "github-actions")
     */
    String getPlatformName();
    
    /**
     * Triggers test execution based on deployment event.
     * 
     * @param deploymentEvent the deployment event containing context
     * @return future containing the test execution plan
     */
    CompletableFuture<TestExecutionPlan> triggerTests(DeploymentEvent deploymentEvent);
    
    /**
     * Reports test results back to the CI/CD platform.
     * 
     * @param testResult the test execution result
     * @param pipelineContext context information for the pipeline
     * @return future indicating success/failure of reporting
     */
    CompletableFuture<Void> reportResults(TestResult testResult, Map<String, Object> pipelineContext);
    
    /**
     * Validates the pipeline configuration.
     * 
     * @param configuration the pipeline configuration to validate
     * @return validation result with any errors or warnings
     */
    PipelineValidationResult validateConfiguration(PipelineConfiguration configuration);
    
    /**
     * Checks if this integration supports the given deployment event.
     * 
     * @param deploymentEvent the deployment event to check
     * @return true if this integration can handle the event
     */
    boolean supportsEvent(DeploymentEvent deploymentEvent);
}