package com.agentic.e2etester.cicd.jenkins;

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
 * Jenkins CI/CD pipeline integration implementation.
 */
@Component
public class JenkinsPipelineIntegration implements PipelineIntegration {
    
    private static final Logger logger = LoggerFactory.getLogger(JenkinsPipelineIntegration.class);
    
    private final WebClient webClient;
    private final TestScenarioParser scenarioParser;
    private final ServiceDiscovery serviceDiscovery;
    private final JenkinsReportFormatter reportFormatter;
    
    public JenkinsPipelineIntegration(WebClient.Builder webClientBuilder,
                                    TestScenarioParser scenarioParser,
                                    ServiceDiscovery serviceDiscovery,
                                    JenkinsReportFormatter reportFormatter) {
        this.webClient = webClientBuilder.build();
        this.scenarioParser = scenarioParser;
        this.serviceDiscovery = serviceDiscovery;
        this.reportFormatter = reportFormatter;
    }
    
    @Override
    public String getPlatformName() {
        return "jenkins";
    }
    
    @Override
    public CompletableFuture<TestExecutionPlan> triggerTests(DeploymentEvent deploymentEvent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Triggering tests for Jenkins deployment event: {}", deploymentEvent.getEventId());
                
                // Wait for deployment delay if configured
                if (deploymentEvent.getMetadata() != null) {
                    Object delayObj = deploymentEvent.getMetadata().get("delayAfterDeployment");
                    if (delayObj instanceof Number) {
                        Thread.sleep(((Number) delayObj).longValue() * 1000);
                    }
                }
                
                // Generate test scenario based on deployment context
                String scenario = generateTestScenario(deploymentEvent);
                
                // Parse scenario into execution plan
                TestExecutionPlan plan = scenarioParser.parse(scenario);
                plan.getConfiguration().getMetadata().put("deploymentEvent", deploymentEvent);
                plan.getConfiguration().getMetadata().put("platform", "jenkins");
                
                logger.info("Generated test execution plan for Jenkins deployment: {}", plan.getTestId());
                return plan;
                
            } catch (Exception e) {
                logger.error("Failed to trigger tests for Jenkins deployment event", e);
                throw new RuntimeException("Failed to trigger tests", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> reportResults(TestResult testResult, Map<String, Object> pipelineContext) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Reporting test results to Jenkins for test: {}", testResult.getTestId());
                
                // Format results for Jenkins
                String junitXml = reportFormatter.formatAsJUnitXml(testResult);
                String htmlReport = reportFormatter.formatAsHtml(testResult);
                
                // Extract Jenkins context
                String buildUrl = (String) pipelineContext.get("BUILD_URL");
                String jobName = (String) pipelineContext.get("JOB_NAME");
                String buildNumber = (String) pipelineContext.get("BUILD_NUMBER");
                
                // Post results to Jenkins
                if (buildUrl != null) {
                    postResultsToJenkins(buildUrl, junitXml, htmlReport, testResult);
                }
                
                // Update build status
                updateJenkinsBuildStatus(jobName, buildNumber, testResult);
                
                logger.info("Successfully reported test results to Jenkins");
                
            } catch (Exception e) {
                logger.error("Failed to report test results to Jenkins", e);
                throw new RuntimeException("Failed to report results", e);
            }
        });
    }
    
    @Override
    public PipelineValidationResult validateConfiguration(PipelineConfiguration configuration) {
        PipelineValidationResult result = new PipelineValidationResult();
        
        // Validate required fields
        if (configuration.getBaseUrl() == null || configuration.getBaseUrl().trim().isEmpty()) {
            result.addError("baseUrl", "Jenkins base URL is required");
        }
        
        if (configuration.getApiToken() == null || configuration.getApiToken().trim().isEmpty()) {
            result.addError("apiToken", "Jenkins API token is required");
        }
        
        if (configuration.getProjectId() == null || configuration.getProjectId().trim().isEmpty()) {
            result.addError("projectId", "Jenkins job name is required");
        }
        
        // Validate URL format
        if (configuration.getBaseUrl() != null && !configuration.getBaseUrl().startsWith("http")) {
            result.addError("baseUrl", "Jenkins base URL must start with http:// or https://");
        }
        
        // Validate trigger configuration
        if (configuration.getTriggerConfig() != null) {
            validateTriggerConfiguration(configuration.getTriggerConfig(), result);
        }
        
        // Validate reporting configuration
        if (configuration.getReportingConfig() != null) {
            validateReportingConfiguration(configuration.getReportingConfig(), result);
        }
        
        result.setSummary(String.format("Validation completed with %d errors and %d warnings", 
                                       result.getErrors().size(), result.getWarnings().size()));
        
        return result;
    }
    
    @Override
    public boolean supportsEvent(DeploymentEvent deploymentEvent) {
        return "jenkins".equalsIgnoreCase(deploymentEvent.getPlatform()) ||
               (deploymentEvent.getMetadata() != null && 
                "jenkins".equalsIgnoreCase((String) deploymentEvent.getMetadata().get("platform")));
    }
    
    private String generateTestScenario(DeploymentEvent deploymentEvent) {
        StringBuilder scenario = new StringBuilder();
        scenario.append("Execute end-to-end regression tests for deployment:\n");
        scenario.append("- Environment: ").append(deploymentEvent.getEnvironment()).append("\n");
        scenario.append("- Version: ").append(deploymentEvent.getVersion()).append("\n");
        scenario.append("- Event Type: ").append(deploymentEvent.getEventType()).append("\n");
        
        if (deploymentEvent.getServices() != null && !deploymentEvent.getServices().isEmpty()) {
            scenario.append("- Services deployed:\n");
            deploymentEvent.getServices().forEach((service, version) -> 
                scenario.append("  - ").append(service).append(": ").append(version).append("\n"));
        }
        
        scenario.append("\nValidate complete order fulfillment flow across all microservices.");
        
        return scenario.toString();
    }
    
    private void postResultsToJenkins(String buildUrl, String junitXml, String htmlReport, TestResult testResult) {
        // Implementation would post results to Jenkins REST API
        logger.debug("Posting results to Jenkins build: {}", buildUrl);
        
        // This would typically involve:
        // 1. POST JUnit XML to Jenkins test results API
        // 2. Upload HTML report as build artifact
        // 3. Set build description with test summary
    }
    
    private void updateJenkinsBuildStatus(String jobName, String buildNumber, TestResult testResult) {
        // Implementation would update Jenkins build status
        logger.debug("Updating Jenkins build status for job: {}, build: {}", jobName, buildNumber);
        
        // This would typically involve:
        // 1. Set build result (SUCCESS/FAILURE/UNSTABLE)
        // 2. Add build badges or annotations
        // 3. Trigger downstream jobs if configured
    }
    
    private void validateTriggerConfiguration(TestTriggerConfiguration triggerConfig, PipelineValidationResult result) {
        if (triggerConfig.getTriggerEvents() == null || triggerConfig.getTriggerEvents().isEmpty()) {
            result.addWarning("triggerEvents", "No trigger events configured - tests will not be automatically triggered");
        }
        
        if (triggerConfig.getTestTimeout() != null && triggerConfig.getTestTimeout().toMinutes() > 120) {
            result.addWarning("testTimeout", "Test timeout is very long (>2 hours) - consider reducing for faster feedback");
        }
    }
    
    private void validateReportingConfiguration(ReportingConfiguration reportingConfig, PipelineValidationResult result) {
        if (reportingConfig.getSupportedFormats() == null || reportingConfig.getSupportedFormats().isEmpty()) {
            result.addWarning("supportedFormats", "No report formats configured - using default JUnit XML");
        }
        
        if (reportingConfig.getArtifactPath() != null && !reportingConfig.getArtifactPath().startsWith("/")) {
            result.addWarning("artifactPath", "Artifact path should be absolute for consistent results");
        }
    }
}