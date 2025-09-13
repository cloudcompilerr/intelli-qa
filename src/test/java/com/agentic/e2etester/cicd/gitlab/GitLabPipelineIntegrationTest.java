package com.agentic.e2etester.cicd.gitlab;

import com.agentic.e2etester.cicd.*;
import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.model.TestStatus;
import com.agentic.e2etester.ai.TestScenarioParser;
import com.agentic.e2etester.service.ServiceDiscovery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GitLab pipeline integration.
 */
@ExtendWith(MockitoExtension.class)
class GitLabPipelineIntegrationTest {
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private TestScenarioParser scenarioParser;
    
    @Mock
    private ServiceDiscovery serviceDiscovery;
    
    @Mock
    private GitLabReportFormatter reportFormatter;
    
    private GitLabPipelineIntegration gitlabIntegration;
    
    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        gitlabIntegration = new GitLabPipelineIntegration(
            webClientBuilder, scenarioParser, serviceDiscovery, reportFormatter);
    }
    
    @Test
    void testGetPlatformName() {
        // When
        String platformName = gitlabIntegration.getPlatformName();
        
        // Then
        assertThat(platformName).isEqualTo("gitlab");
    }
    
    @Test
    void testSupportsEvent() {
        // Given
        DeploymentEvent gitlabEvent = createGitLabDeploymentEvent();
        DeploymentEvent pipelineEvent = createGitLabPipelineEvent();
        DeploymentEvent otherEvent = createOtherDeploymentEvent();
        
        // When & Then
        assertThat(gitlabIntegration.supportsEvent(gitlabEvent)).isTrue();
        assertThat(gitlabIntegration.supportsEvent(pipelineEvent)).isTrue();
        assertThat(gitlabIntegration.supportsEvent(otherEvent)).isFalse();
    }
    
    @Test
    void testTriggerTests() throws Exception {
        // Given
        DeploymentEvent deploymentEvent = createGitLabDeploymentEvent();
        TestExecutionPlan mockPlan = createMockTestExecutionPlan();
        
        when(scenarioParser.parse(any(String.class))).thenReturn(mockPlan);
        
        // When
        CompletableFuture<TestExecutionPlan> future = gitlabIntegration.triggerTests(deploymentEvent);
        TestExecutionPlan result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTestId()).isEqualTo("test-plan-123");
        assertThat(result.getConfiguration().getMetadata()).containsKey("deploymentEvent");
        assertThat(result.getConfiguration().getMetadata()).containsEntry("platform", "gitlab");
        assertThat(result.getConfiguration().getMetadata()).containsKey("pipelineId");
    }
    
    @Test
    void testReportResults() throws Exception {
        // Given
        TestResult testResult = createMockTestResult();
        Map<String, Object> pipelineContext = createGitLabPipelineContext();
        
        when(reportFormatter.formatAsJUnitXml(testResult)).thenReturn("<testsuite/>");
        when(reportFormatter.formatAsGitLabReport(testResult)).thenReturn("{}");
        
        // When
        CompletableFuture<Void> future = gitlabIntegration.reportResults(testResult, pipelineContext);
        future.get(); // Wait for completion
        
        // Then - no exception should be thrown
        assertThat(future).isCompleted();
    }
    
    @Test
    void testValidateConfiguration() {
        // Given
        PipelineConfiguration validConfig = createValidGitLabConfiguration();
        PipelineConfiguration invalidConfig = createInvalidGitLabConfiguration();
        
        // When
        PipelineValidationResult validResult = gitlabIntegration.validateConfiguration(validConfig);
        PipelineValidationResult invalidResult = gitlabIntegration.validateConfiguration(invalidConfig);
        
        // Then
        assertThat(validResult.isValid()).isTrue();
        assertThat(validResult.getErrors()).isEmpty();
        
        assertThat(invalidResult.isValid()).isFalse();
        assertThat(invalidResult.getErrors()).isNotEmpty();
    }
    
    @Test
    void testValidateConfigurationWithInvalidProjectId() {
        // Given
        PipelineConfiguration config = createValidGitLabConfiguration();
        config.setProjectId("invalid-project-id-format!");
        
        // When
        PipelineValidationResult result = gitlabIntegration.validateConfiguration(config);
        
        // Then
        assertThat(result.isValid()).isTrue(); // Should still be valid, just with warning
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0).getField()).isEqualTo("projectId");
    }
    
    @Test
    void testValidateConfigurationWithWeakWebhookSecret() {
        // Given
        PipelineConfiguration config = createValidGitLabConfiguration();
        config.setWebhookSecret("weak");
        
        // When
        PipelineValidationResult result = gitlabIntegration.validateConfiguration(config);
        
        // Then
        assertThat(result.isValid()).isTrue(); // Should still be valid, just with warning
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0).getField()).isEqualTo("webhookSecret");
    }
    
    private DeploymentEvent createGitLabDeploymentEvent() {
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId("gitlab-event-123");
        event.setPlatform("gitlab");
        event.setProjectId("123");
        event.setEnvironment("staging");
        event.setVersion("1.0.0");
        event.setBranch("main");
        event.setCommitHash("abc123");
        event.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
        event.setTimestamp(Instant.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("CI_PIPELINE_ID", "456");
        metadata.put("CI_PROJECT_ID", "123");
        metadata.put("CI_MERGE_REQUEST_IID", "789");
        event.setMetadata(metadata);
        
        return event;
    }
    
    private DeploymentEvent createGitLabPipelineEvent() {
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId("pipeline-event-123");
        event.setPlatform("other");
        event.setProjectId("test-project");
        event.setEnvironment("staging");
        event.setVersion("1.0.0");
        event.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
        event.setTimestamp(Instant.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("CI_PIPELINE_ID", "456"); // This makes it a GitLab event
        event.setMetadata(metadata);
        
        return event;
    }
    
    private DeploymentEvent createOtherDeploymentEvent() {
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId("other-event-123");
        event.setPlatform("jenkins");
        event.setProjectId("test-project");
        event.setEnvironment("staging");
        event.setVersion("1.0.0");
        event.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
        event.setTimestamp(Instant.now());
        return event;
    }
    
    private TestExecutionPlan createMockTestExecutionPlan() {
        TestExecutionPlan plan = new TestExecutionPlan();
        plan.setTestId("test-plan-123");
        plan.setScenario("Mock GitLab test scenario");
        return plan;
    }
    
    private TestResult createMockTestResult() {
        TestResult result = new TestResult();
        result.setTestId("test-result-123");
        result.setStatus(TestStatus.PASSED);
        result.setStartTime(Instant.now().minus(Duration.ofMinutes(5)));
        result.setEndTime(Instant.now());
        result.setExecutionTime(Duration.ofMinutes(5));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("environment", "staging");
        metadata.put("version", "1.0.0");
        metadata.put("pipelineId", "456");
        result.setMetadata(metadata);
        
        return result;
    }
    
    private Map<String, Object> createGitLabPipelineContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("CI_PROJECT_ID", "123");
        context.put("CI_PIPELINE_ID", "456");
        context.put("CI_JOB_ID", "789");
        context.put("CI_MERGE_REQUEST_IID", "101");
        return context;
    }
    
    private PipelineConfiguration createValidGitLabConfiguration() {
        PipelineConfiguration config = new PipelineConfiguration();
        config.setPlatform("gitlab");
        config.setBaseUrl("https://gitlab.example.com");
        config.setApiToken("valid-api-token");
        config.setProjectId("123");
        config.setWebhookSecret("very-secure-webhook-secret-key");
        
        TestTriggerConfiguration triggerConfig = new TestTriggerConfiguration();
        triggerConfig.setTriggerEvents(List.of(DeploymentEventType.DEPLOYMENT_COMPLETED));
        config.setTriggerConfig(triggerConfig);
        
        ReportingConfiguration reportingConfig = new ReportingConfiguration();
        reportingConfig.setSupportedFormats(List.of(ReportFormat.JUNIT_XML, ReportFormat.JSON));
        config.setReportingConfig(reportingConfig);
        
        return config;
    }
    
    private PipelineConfiguration createInvalidGitLabConfiguration() {
        PipelineConfiguration config = new PipelineConfiguration();
        config.setPlatform("gitlab");
        // Missing required fields: baseUrl, apiToken, projectId
        return config;
    }
}