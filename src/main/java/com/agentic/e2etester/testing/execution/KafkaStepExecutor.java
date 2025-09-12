package com.agentic.e2etester.testing.execution;

import com.agentic.e2etester.integration.kafka.KafkaTestProducer;
import com.agentic.e2etester.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Step executor for Kafka event publishing.
 */
@Component
public class KafkaStepExecutor implements StepExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaStepExecutor.class);
    
    private final KafkaTestProducer kafkaTestProducer;
    
    public KafkaStepExecutor(KafkaTestProducer kafkaTestProducer) {
        this.kafkaTestProducer = kafkaTestProducer;
    }
    
    @Override
    public CompletableFuture<StepResult> executeStep(TestStep step, TestContext context) {
        logger.debug("Executing Kafka step: {}", step.getStepId());
        
        StepResult stepResult = new StepResult(step.getStepId(), TestStatus.RUNNING);
        stepResult.setStartTime(Instant.now());
        
        // Extract topic and event data from step
        String topic = (String) step.getInputData().get("topic");
        Object eventData = step.getInputData().get("eventData");
        
        if (topic == null) {
            stepResult.setStatus(TestStatus.FAILED);
            stepResult.setErrorMessage("Topic is required for Kafka step");
            stepResult.setEndTime(Instant.now());
            return CompletableFuture.completedFuture(stepResult);
        }
        
        return kafkaTestProducer.sendTestEvent(topic, eventData, context)
            .handle((sendResult, throwable) -> {
                stepResult.setEndTime(Instant.now());
                
                if (throwable != null) {
                    stepResult.setStatus(TestStatus.FAILED);
                    stepResult.setErrorMessage("Failed to send Kafka event: " + throwable.getMessage());
                } else {
                    stepResult.setStatus(TestStatus.PASSED);
                    stepResult.setOutput(sendResult);
                }
                
                return stepResult;
            });
    }
    
    @Override
    public boolean canExecute(TestStep step) {
        return step.getType() == StepType.KAFKA_EVENT;
    }
}