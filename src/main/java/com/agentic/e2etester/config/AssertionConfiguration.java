package com.agentic.e2etester.config;

import com.agentic.e2etester.testing.assertion.AssertionEngine;
import com.agentic.e2etester.testing.assertion.CustomAssertionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Configuration class for the assertion framework.
 * Automatically registers all custom assertion evaluators with the assertion engine.
 */
@Configuration
public class AssertionConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AssertionConfiguration.class);
    
    private final AssertionEngine assertionEngine;
    private final List<CustomAssertionEvaluator> customEvaluators;
    
    @Autowired
    public AssertionConfiguration(AssertionEngine assertionEngine, 
                                List<CustomAssertionEvaluator> customEvaluators) {
        this.assertionEngine = assertionEngine;
        this.customEvaluators = customEvaluators;
    }
    
    @PostConstruct
    public void registerCustomEvaluators() {
        if (customEvaluators != null && !customEvaluators.isEmpty()) {
            logger.info("Registering {} custom assertion evaluators", customEvaluators.size());
            
            for (CustomAssertionEvaluator evaluator : customEvaluators) {
                try {
                    assertionEngine.registerCustomEvaluator(evaluator);
                    logger.debug("Registered custom assertion evaluator: {}", evaluator.getEvaluatorName());
                } catch (Exception e) {
                    logger.error("Failed to register custom assertion evaluator {}: {}", 
                               evaluator.getEvaluatorName(), e.getMessage(), e);
                }
            }
            
            logger.info("Successfully registered custom assertion evaluators");
        } else {
            logger.info("No custom assertion evaluators found to register");
        }
    }
}