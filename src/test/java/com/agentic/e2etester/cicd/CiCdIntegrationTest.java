package com.agentic.e2etester.cicd;

import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.model.TestStatus;
import com.agentic.e2etester.ai.TestScenarioParser;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for CI/CD pipeline integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CiCdIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TestScenarioParser scenarioParser;
    
    @MockBean
    private TestExecutionEngine testExecutionEngine;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/cicd";
        
        // Mock test scenario parser
        TestExecutionPlan mockPlan = createMockTestExecutionPlan();
        when(scenarioParser.parse(any(String.class))).thenReturn(mockPlan);
        
        // Mock test execution engine
        TestResult mockResult = createMockTestResult();
        when(testExecutionEngine.executeTest(any(TestExecutionPlan.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResult));
    }
    
    @Test
    void testWebhookHealthEndpoint() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/webhook/health", Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("healthy");
    }
    
    @Test
    void testGenericDeploymentWebhook() throws Exception {
        // Given
        DeploymentEvent deploymentEvent = createDeploymentEvent();
        String payload = objectMapper.writeValueAsString(deploymentEvent);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/webhook/deployment", request, Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("accepted");
    }
    
    @Test
    void testJenkinsWebhook() throws Exception {
        // Given
        Map<String, Object> jenkinsPayload = createJenkinsPayload();
        String payload = objectMapper.writeValueAsString(jenkinsPayload);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-jenkins-event", "build-completed");
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/webhook/jenkins", request, Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
    }
    
    @Test
    void testGitLabWebhook() throws Exception {
        // Given
        Map<String, Object> gitlabPayload = createGitLabPayload();
        String payload = objectMapper.writeValueAsString(gitlabPayload);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-gitlab-event", "Pipeline Hook");
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/webhook/gitlab", request, Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
    }
    
    @Test
    void testWebhookWithInvalidPayload() {
        // Given
        String invalidPayload = "invalid json";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(invalidPayload, headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/webhook/deployment", request, Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsKey("error");
    }
    
    private DeploymentEvent createDeploymentEvent() {
        DeploymentEvent event = new DeploymentEvent();
        event.setEventId("test-event-123");
        event.setPlatform("test");
        event.setProjectId("test-project");
        event.setEnvironment("staging");
        event.setVersion("1.0.0");
        event.setBranch("main");
        event.setCommitHash("abc123");
        event.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
        event.setTimestamp(Instant.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test", "value");
        event.setMetadata(metadata);
        
        return event;
    }
    
    private Map<String, Object> createJenkinsPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("job_name", "test-job");
        payload.put("build_number", "123");
        payload.put("build_status", "SUCCESS");
        payload.put("build_url", "http://jenkins.example.com/job/test-job/123/");
        payload.put("environment", "staging");
        payload.put("branch", "main");
        return payload;
    }
    
    private Map<String, Object> createGitLabPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("project_id", 123);
        payload.put("pipeline_id", 456);
        payload.put("environment", "staging");
        payload.put("ref", "main");
        
        Map<String, Object> commit = new HashMap<>();
        commit.put("id", "abc123");
        payload.put("commit", commit);
        
        Map<String, Object> objectAttributes = new HashMap<>();
        objectAttributes.put("status", "success");
        payload.put("object_attributes", objectAttributes);
        
        return payload;
    }
    
    private TestExecutionPlan createMockTestExecutionPlan() {
        TestExecutionPlan plan = new TestExecutionPlan();
        plan.setTestId("test-plan-123");
        plan.setScenario("Mock test scenario");
        return plan;
    }
    
    private TestResult createMockTestResult() {
        TestResult result = new TestResult();
        result.setTestId("test-result-123");
        result.setStatus(TestStatus.PASSED);
        result.setStartTime(Instant.now().minus(Duration.ofMinutes(5)));
        result.setEndTime(Instant.now());
        result.setExecutionTime(Duration.ofMinutes(5));
        return result;
    }
}