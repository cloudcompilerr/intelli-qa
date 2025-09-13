package com.agentic.e2etester.cicd.jenkins;

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
 * Unit tests for Jenkins pipeline integration.
 */
@ExtendWith(MockitoExtension.class)
class JenkinsPipelineIntegrationTest {
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private TestScenarioParser scenarioParser;
    
    @Mock
    private ServiceDiscovery serviceDiscovery;
    
    @Mock
    private JenkinsReportFormatter reportFormatter;
    
    private JenkinsPipelineIntegration jenkinsIntegration;
    
    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        jenkinsIntegration = new JenkinsPipelineIntegration(
            webClientBuilder, scenarioParser, serviceDiscovery, reportFormatter);
    }
    
    @Test
    void testGetPlatformName() {
        // When
        String platformName = jenkinsIntegration.getPlatformName();
        
        // Then
        assertThat(platformName).isEqualTo("jenkins");
    }
    
    @Test
    void testSupportsEvent() {
        // Given
        DeploymentEvent jenkinsEvent = createJenkinsDeploymentEvent();
        DeploymentEvent otherEvent = createOtherDeploymentEvent();
        
        // When & Then
        assertThat(jenkinsIntegration.supportsEvent(jenkinsEvent)).isTrue();
        assertThat(jenkinsIntegration.supportsEvent(otherEvent)).isFalse();
    }
    
    @Test
    void testTriggerTests() throws Exception {
        // Given
        DeploymentEvent deploymentEvent = createJenkinsDeploymentEvent();
        TestExecutionPlan mockPlan = createMockTestExecutionPlan();
        
        when(scenarioParser.parse(any(String.class))).thenReturn(mockPlan);
        
        // When
        CompletableFuture<TestExecutionPlan> future = jenkinsIntegration.triggerTests(deploymentEvent);
        TestExecutionPlan result = future.get();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTestId()).isEqualTo("test-plan-123");
        assertThat(result.getConfiguration().getMetadata()).containsKey("deploymentEvent");
        assertThat(result.getConfiguration().getMetadata()).containsEntry("platform", "jenkins");
    }
    
    @Test
    void testReportResults() throws Exception {
        // Given
        TestResult testResult = createMockTestResult();
        Map<String, Object> pipelineContext = createJenkinsPipelineContext();
        
        when(reportFormatter.formatAsJUnitXml(testResult)).thenReturn("<testsuite/>");
        when(reportFormatter.formatAsHtml(testResult)).thenReturn("<html/>");
        
        // When
        CompletableFuture<Void> future = jenkinsIntegration.reportResults(testResult, pipelineContext);
        future.get(); // Wait for completion
        
        // Then - no exception should be thrown
        assertThat(future).isCompleted();
    }
    
    @Test
    void testValidateConfiguration() {
        // Given
        PipelineConfiguration validConfig = createValidJenkinsConfiguration();
        PipelineConfiguration invalidConfig = createInvalidJenkinsConfiguration();
        
        // When
        PipelineValidationResult validResult = jenkinsIntegration.validateConfiguration(validConfig);
        PipelineValidationResult invalidResult = jenkinsIntegration.validateConfiguration(invalidConfig);
        
        // Then
        assertThat(validResult.isValid()).isTrue();
        assertThat(validResult.getErrors()).isEmpty();
        
        assertThat(invalidResult.isValid()).isFalse();
        assertThat(invalidResult.getErrors()).isNotEmpty();
        assertThat(invalidResult.getErrors().get(0).getField()).isEqualTo("baseUrl");
    }
    
    @Test
    void testValidateConfigurationWithMissingApiToken() {
        // Given
        PipelineConfiguration config = createValidJenkinsConfiguration();
        config.setApiToken(null);
        
        // When
        PipelineValidationResult result = jenkinsIntegration.validateConfiguration(config);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getField()).isEqualTo("apiToken");
    }
    
    @Test
    void testValidateConfigurationWithInvalidUrl() {
        // Given
        PipelineConfiguration config = createValidJenkinsConfiguration();
        config.setBaseUrl("invalid-url");
        
        // When
        PipelineValidationResult result = jenkinsIntegration.validateConfiguration(config);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getField()).isEqualTo("baseUrl");
    }
    
    private DeploymentEvent createJenkinsDeploymentEvent() {
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId("jenkins-event-123");
        event.setPlatform("jenkins");
        event.setProjectId("test-job");
        event.setEnvironment("staging");
        event.setVersion("123");
        event.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
        event.setTimestamp(Instant.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("BUILD_URL", "http://jenkins.example.com/job/test-job/123/");
        metadata.put("JOB_NAME", "test-job");
        metadata.put("BUILD_NUMBER", "123");
        event.setMetadata(metadata);
        
        return event;
    }
    
    private DeploymentEvent createOtherDeploymentEvent() {
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId("other-event-123");
        event.setPlatform("gitlab");
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
        plan.setScenario("Mock Jenkins test scenario");
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
        result.setMetadata(metadata);
        
        return result;
    }
    
    private Map<String, Object> createJenkinsPipelineContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("BUILD_URL", "http://jenkins.example.com/job/test-job/123/");
        context.put("JOB_NAME", "test-job");
        context.put("BUILD_NUMBER", "123");
        return context;
    }
    
    private PipelineConfiguration createValidJenkinsConfiguration() {
        PipelineConfiguration config = new PipelineConfiguration();
        config.setPlatform("jenkins");
        config.setBaseUrl("https://jenkins.example.com");
        config.setApiToken("valid-api-token");
        config.setProjectId("test-job");
        
        TestTriggerConfiguration triggerConfig = new TestTriggerConfiguration();
        triggerConfig.setTriggerEvents(List.of(DeploymentEventType.DEPLOYMENT_COMPLETED));
        config.setTriggerConfig(triggerConfig);
        
        ReportingConfiguration reportingConfig = new ReportingConfiguration();
        reportingConfig.setSupportedFormats(List.of(ReportFormat.JUNIT_XML));
        config.setReportingConfig(reportingConfig);
        
        return config;
    }
    
    private PipelineConfiguration createInvalidJenkinsConfiguration() {
        PipelineConfiguration config = new PipelineConfiguration();
        config.setPlatform("jenkins");
        // Missing required fields: baseUrl, apiToken, projectId
        return config;
    }
}