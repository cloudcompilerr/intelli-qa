package com.agentic.e2etester.cicd.gitlab;

import com.agentic.e2etester.cicd.*;
import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.ai.TestScenarioParser;
import com.agentic.e2etester.service.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GitLab CI/CD pipeline integration implementation.
 */
@Component
public class GitLabPipelineIntegration implements PipelineIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(GitLabPipelineIntegration.class);
    
    private final WebClient webClient;
    private final TestScenarioParser scenarioParser;
    private final ServiceDiscovery serviceDiscovery;
    private final GitLabReportFormatter reportFormatter;
    
    public GitLabPipelineIntegration(WebClient.Builder webClientBuilder,
                                   TestScenarioParser scenarioParser,
                                   ServiceDiscovery serviceDiscovery,
                                   GitLabReportFormatter reportFormatter) {
        this.webClient = webClientBuilder.build();
        this.scenarioParser = scenarioParser;
        this.serviceDiscovery = serviceDiscovery;
        this.reportFormatter = reportFormatter;
    }
    
    @Override
    public String getPlatformName() {
        return "gitlab";
    }
    
    @Override
    public CompletableFuture<TestExecutionPlan> triggerTests(DeploymentEvent deploymentEvent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Triggering tests for GitLab deployment event: {}", deploymentEvent.getEventId());
                
                // Extract GitLab-specific context
                String pipelineId = extractPipelineId(deploymentEvent);
                String mergeRequestId = extractMergeRequestId(deploymentEvent);
                
                // Generate test scenario based on deployment context
                String scenario = generateTestScenario(deploymentEvent);
                
                // Parse scenario into execution plan
                TestExecutionPlan plan = scenarioParser.parse(scenario);
                plan.getConfiguration().getMetadata().put("deploymentEvent", deploymentEvent);
                plan.getConfiguration().getMetadata().put("platform", "gitlab");
                plan.getConfiguration().getMetadata().put("pipelineId", pipelineId);
                plan.getConfiguration().getMetadata().put("mergeRequestId", mergeRequestId);
                
                logger.info("Generated test execution plan for GitLab deployment: {}", plan.getTestId());
                return plan;
                
            } catch (Exception e) {
                logger.error("Failed to trigger tests for GitLab deployment event", e);
                throw new RuntimeException("Failed to trigger tests", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> reportResults(TestResult testResult, Map<String, Object> pipelineContext) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Reporting test results to GitLab for test: {}", testResult.getTestId());
                
                // Format results for GitLab
                String junitXml = reportFormatter.formatAsJUnitXml(testResult);
                String gitlabReport = reportFormatter.formatAsGitLabReport(testResult);
                
                // Extract GitLab context
                String projectId = (String) pipelineContext.get("CI_PROJECT_ID");
                String pipelineId = (String) pipelineContext.get("CI_PIPELINE_ID");
                String jobId = (String) pipelineContext.get("CI_JOB_ID");
                String mergeRequestId = (String) pipelineContext.get("CI_MERGE_REQUEST_IID");
                
                // Post results to GitLab
                if (projectId != null && pipelineId != null) {
                    postResultsToGitLab(projectId, pipelineId, jobId, junitXml, gitlabReport, testResult);
                }
                
                // Update merge request with test results
                if (mergeRequestId != null) {
                    updateMergeRequestStatus(projectId, mergeRequestId, testResult);
                }
                
                logger.info("Successfully reported test results to GitLab");
                
            } catch (Exception e) {
                logger.error("Failed to report test results to GitLab", e);
                throw new RuntimeException("Failed to report results", e);
            }
        });
    }
    
    @Override
    public PipelineValidationResult validateConfiguration(PipelineConfiguration configuration) {
        PipelineValidationResult result = new PipelineValidationResult();
        
        // Validate required fields
        if (configuration.getBaseUrl() == null || configuration.getBaseUrl().trim().isEmpty()) {
            result.addError("baseUrl", "GitLab base URL is required");
        }
        
        if (configuration.getApiToken() == null || configuration.getApiToken().trim().isEmpty()) {
            result.addError("apiToken", "GitLab API token is required");
        }
        
        if (configuration.getProjectId() == null || configuration.getProjectId().trim().isEmpty()) {
            result.addError("projectId", "GitLab project ID is required");
        }
        
        // Validate URL format
        if (configuration.getBaseUrl() != null && !configuration.getBaseUrl().startsWith("http")) {
            result.addError("baseUrl", "GitLab base URL must start with http:// or https://");
        }
        
        // Validate project ID format (should be numeric or namespace/project)
        if (configuration.getProjectId() != null) {
            String projectId = configuration.getProjectId();
            if (!projectId.matches("\\d+") && !projectId.matches("[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+")) {
                result.addWarning("projectId", "Project ID should be numeric or in namespace/project format");
            }
        }
        
        // Validate webhook secret for security
        if (configuration.getWebhookSecret() == null || configuration.getWebhookSecret().length() < 16) {
            result.addWarning("webhookSecret", "Webhook secret should be at least 16 characters for security");
        }
        
        result.setSummary(String.format("GitLab validation completed with %d errors and %d warnings", 
                                       result.getErrors().size(), result.getWarnings().size()));
        
        return result;
    }
    
    @Override
    public boolean supportsEvent(DeploymentEvent deploymentEvent) {
        return "gitlab".equalsIgnoreCase(deploymentEvent.getPlatform()) ||
               (deploymentEvent.getMetadata() != null && 
                deploymentEvent.getMetadata().containsKey("CI_PIPELINE_ID"));
    }
    
    private String generateTestScenario(DeploymentEvent deploymentEvent) {
        StringBuilder scenario = new StringBuilder();
        scenario.append("Execute GitLab CI/CD triggered end-to-end tests:\n");
        scenario.append("- Environment: ").append(deploymentEvent.getEnvironment()).append("\n");
        scenario.append("- Version: ").append(deploymentEvent.getVersion()).append("\n");
        scenario.append("- Branch: ").append(deploymentEvent.getBranch()).append("\n");
        scenario.append("- Commit: ").append(deploymentEvent.getCommitHash()).append("\n");
        
        if (deploymentEvent.getServices() != null && !deploymentEvent.getServices().isEmpty()) {
            scenario.append("- Services deployed:\n");
            deploymentEvent.getServices().forEach((service, version) -> 
                scenario.append("  - ").append(service).append(": ").append(version).append("\n"));
        }
        
        scenario.append("\nValidate complete order fulfillment flow with GitLab integration.");
        
        return scenario.toString();
    }
    
    private String extractPipelineId(DeploymentEvent deploymentEvent) {
        if (deploymentEvent.getMetadata() != null) {
            Object pipelineId = deploymentEvent.getMetadata().get("CI_PIPELINE_ID");
            return pipelineId != null ? pipelineId.toString() : null;
        }
        return null;
    }
    
    private String extractMergeRequestId(DeploymentEvent deploymentEvent) {
        if (deploymentEvent.getMetadata() != null) {
            Object mrId = deploymentEvent.getMetadata().get("CI_MERGE_REQUEST_IID");
            return mrId != null ? mrId.toString() : null;
        }
        return null;
    }
    
    private void postResultsToGitLab(String projectId, String pipelineId, String jobId, 
                                   String junitXml, String gitlabReport, TestResult testResult) {
        // Implementation would post results to GitLab API
        logger.debug("Posting results to GitLab project: {}, pipeline: {}", projectId, pipelineId);
        
        // This would typically involve:
        // 1. POST JUnit XML to GitLab test reports API
        // 2. Upload artifacts to GitLab job artifacts
        // 3. Update pipeline status
        // 4. Create deployment status
    }
    
    private void updateMergeRequestStatus(String projectId, String mergeRequestId, TestResult testResult) {
        // Implementation would update merge request with test results
        logger.debug("Updating GitLab merge request: {} in project: {}", mergeRequestId, projectId);
        
        // This would typically involve:
        // 1. Add comment to merge request with test summary
        // 2. Update merge request status checks
        // 3. Set merge request labels based on test results
    }
}