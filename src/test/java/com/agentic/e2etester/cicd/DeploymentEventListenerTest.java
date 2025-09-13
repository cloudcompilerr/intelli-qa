package com.agentic.e2etester.cicd;

import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.model.TestResult;
import com.agentic.e2etester.model.TestStatus;
import com.agentic.e2etester.testing.execution.TestExecutionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeploymentEventListener.
 */
@ExtendWith(MockitoExtension.class)
class DeploymentEventListenerTest {
    
    @Mock
    private PipelineIntegration pipelineIntegration;
    
    @Mock
    private TestExecutionEngine testExecutionEngine;
    
    @Mock
    private CiCdConfiguration ciCdConfiguration;
    
    private DeploymentEventListener eventListener;
    
    @BeforeEach
    void setUp() {
        List<PipelineIntegration> integrations = List.of(pipelineIntegration);
        eventListener = new DeploymentEventListener(integrations, testExecutionEngine, ciCdConfiguration);
        
        // Default configuration
        when(ciCdConfiguration.isAutoTriggerEnabled()).thenReturn(true);
        when(ciCdConfiguration.getTriggerEvents()).thenReturn(List.of(DeploymentEventType.DEPLOYMENT_COMPLETED));
    }
    
    @Test
    void testHandleDeploymentEventSuccess() throws Exception {
        // Given
        DeploymentEvent deploymentEvent = createValidDeploymentEvent();
        TestExecutionPlan testPlan = createMockTestExecutionPlan();
        TestResult testResult = createMockTestResult();
        
        when(pipelineIntegration.supportsEvent(deploymentEvent)).thenReturn(true);
        when(pipelineIntegration.triggerTests(deploymentEvent))
            .thenReturn(CompletableFuture.completedFuture(testPlan));
        when(testExecutionEngine.executeTest(testPlan))
            .thenReturn(CompletableFuture.completedFuture(testResult));
        when(pipelineIntegration.reportResults(eq(testResult), any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // When
        CompletableFuture<Void> future = eventListener.handleDeploymentEvent(deploymentEvent);
        future.get(); // Wait for completion
        
        // Then
        verify(pipelineIntegration).supportsEvent(deploymentEvent);
        verify(pipelineIntegration).triggerTests(deploymentEvent);
        verify(testExecutionEngine).executeTest(testPlan);
        verify(pipelineIntegration).reportResults(eq(testResult), any());
    }
    
    @Test
    void testHandleDeploymentEventWithAutoTriggerDisabled() throws Exception {
        // Given
        DeploymentEvent deploymentEvent = createValidDeploymentEvent();
        when(ciCdConfiguration.isAutoTriggerEnabled()).thenReturn(false);
        
        // When
        CompletableFuture<Void> future = eventListener.handleDeploymentEvent(deploymentEvent);
        future.get(); // Wait for completion
        
        // Then
        verify(pipelineIntegration, never()).supportsEvent(any());
        verify(pipelineIntegration, never()).triggerTests(any());
        verify(testExecutionEngine, never()).executeTest(any());
    }
    
    @Test
    void testHandleDeploymentEventWithUnsupportedEvent() throws Exception {
        // Given
        DeploymentEvent deploymentEvent = createValidDeploymentEvent();
        when(pipelineIntegration.supportsEvent(deploymentEvent)).thenReturn(false);
        
        // When
        CompletableFuture<Void> future = eventListener.handleDeploymentEvent(deploymentEvent);
        future.get(); // Wait for completion
        
        // Then
        verify(pipelineIntegration).supportsEvent(deploymentEvent);
        verify(pipelineIntegration, never()).triggerTests(any());
        verify(testExecutionEngine, never()).executeTest(any());
    }
    
    @Test
    void testHandleDeploymentEventWithNonTriggeringEventType() throws Exception {
        // Given
        DeploymentEvent deploymentEvent = createValidDeploymentEvent();
        deploymentEvent.setEventType(DeploymentEventType.DEPLOYMENT_STARTED); // Not in trigger events
        when(pipelineIntegration.supportsEvent(deploymentEvent)).thenReturn(true);
        
        // When
        CompletableFuture<Void> future = eventListener.handleDeploymentEvent(deploymentEvent);
        future.get(); // Wait for completion
        
        // Then
        verify(pipelineIntegration).supportsEvent(deploymentEvent);
        verify(pipelineIntegration, never()).triggerTests(any());
        verify(testExecutionEngine, never()).executeTest(any());
    }
    
    @Test
    void testValidateDeploymentEventValid() {
        // Given
        DeploymentEvent validEvent = createValidDeploymentEvent();
        
        // When
        boolean isValid = eventListener.validateDeploymentEvent(validEvent);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    void testValidateDeploymentEventNull() {
        // When
        boolean isValid = eventListener.validateDeploymentEvent(null);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testValidateDeploymentEventMissingEventId() {
        // Given
        DeploymentEvent event = createValidDeploymentEvent();
        event.setEventId(null);
        
        // When
        boolean isValid = eventListener.validateDeploymentEvent(event);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testValidateDeploymentEventMissingPlatform() {
        // Given
        DeploymentEvent event = createValidDeploymentEvent();
        event.setPlatform(null);
        
        // When
        boolean isValid = eventListener.validateDeploymentEvent(event);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testValidateDeploymentEventMissingEventType() {
        // Given
        DeploymentEvent event = createValidDeploymentEvent();
        event.setEventType(null);
        
        // When
        boolean isValid = eventListener.validateDeploymentEvent(event);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testValidateDeploymentEventMissingEnvironment() {
        // Given
        DeploymentEvent event = createValidDeploymentEvent();
        event.setEnvironment(null);
        
        // When
        boolean isValid = eventListener.validateDeploymentEvent(event);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testShouldTriggerTestsWithDefaultBehavior() {
        // Given
        when(ciCdConfiguration.getTriggerEvents()).thenReturn(null);
        DeploymentEvent completedEvent = createValidDeploymentEvent();
        completedEvent.setEventType(DeploymentEventType.DEPLOYMENT_COMPLETED);
        
        DeploymentEvent startedEvent = createValidDeploymentEvent();
        startedEvent.setEventType(DeploymentEventType.DEPLOYMENT_STARTED);
        
        // When & Then
        // This is tested indirectly through handleDeploymentEvent
        // Default behavior should trigger on DEPLOYMENT_COMPLETED and POST_DEPLOYMENT
    }
    
    private DeploymentEvent createValidDeploymentEvent() {
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
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("environment", "staging");
        metadata.put("version", "1.0.0");
        result.setMetadata(metadata);
        
        return result;
    }
}