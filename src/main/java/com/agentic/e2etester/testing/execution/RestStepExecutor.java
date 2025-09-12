package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.integration.rest.RestApiAdapter;
import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Step executor for REST API calls.
 */
@Component
public class RestStepExecutor implements StepExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(RestStepExecutor.class);
    
    private final RestApiAdapter restApiAdapter;
    
    public RestStepExecutor(RestApiAdapter restApiAdapter) {
        this.restApiAdapter = restApiAdapter;
    }
    
    @Override
    public CompletableFuture<StepResult> executeStep(TestStep step, TestContext context) {
        logger.debug("Executing REST step: {}", step.getStepId());
        
        StepResult stepResult = new StepResult(step.getStepId(), TestStatus.RUNNING);
        stepResult.setStartTime(Instant.now());
        
        // Extract HTTP method and endpoint from step data
        String method = (String) step.getInputData().get("method");
        String endpoint = (String) step.getInputData().get("endpoint");
        Object requestBody = step.getInputData().get("body");
        
        CompletableFuture<ServiceInteraction> interactionFuture;
        
        // Execute based on HTTP method
        switch (method.toUpperCase()) {
            case "GET":
                interactionFuture = restApiAdapter.get(step.getTargetService(), endpoint, context.getCorrelationId(), null);
                break;
            case "POST":
                interactionFuture = restApiAdapter.post(step.getTargetService(), endpoint, requestBody, context.getCorrelationId(), null);
                break;
            case "PUT":
                interactionFuture = restApiAdapter.put(step.getTargetService(), endpoint, requestBody, context.getCorrelationId(), null);
                break;
            case "DELETE":
                interactionFuture = restApiAdapter.delete(step.getTargetService(), endpoint, context.getCorrelationId(), null);
                break;
            default:
                stepResult.setStatus(TestStatus.FAILED);
                stepResult.setErrorMessage("Unsupported HTTP method: " + method);
                stepResult.setEndTime(Instant.now());
                return CompletableFuture.completedFuture(stepResult);
        }
        
        return interactionFuture.handle((interaction, throwable) -> {
            stepResult.setEndTime(Instant.now());
            
            if (throwable != null) {
                stepResult.setStatus(TestStatus.FAILED);
                stepResult.setErrorMessage(throwable.getMessage());
            } else {
                context.addInteraction(interaction);
                stepResult.setOutput(interaction.getResponse());
                stepResult.setStatus(interaction.getStatus() == InteractionStatus.SUCCESS ? 
                    TestStatus.PASSED : TestStatus.FAILED);
            }
            
            return stepResult;
        });
    }
    
    @Override
    public boolean canExecute(TestStep step) {
        return step.getType() == StepType.REST_CALL;
    }
}