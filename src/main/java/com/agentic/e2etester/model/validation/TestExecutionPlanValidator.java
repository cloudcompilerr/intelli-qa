package com.agentic.e2etester.model.validation;

import com.agentic.e2etester.model.TestExecutionPlan;
import com.agentic.e2etester.model.TestStep;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Custom validator for TestExecutionPlan that validates business rules.
 */
public class TestExecutionPlanValidator implements ConstraintValidator<ValidTestExecutionPlan, TestExecutionPlan> {
    
    @Override
    public void initialize(ValidTestExecutionPlan constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(TestExecutionPlan plan, ConstraintValidatorContext context) {
        if (plan == null) {
            return true; // Let @NotNull handle null validation
        }
        
        boolean isValid = true;
        context.disableDefaultConstraintViolation();
        
        // Validate step IDs are unique
        if (plan.getSteps() != null && !plan.getSteps().isEmpty()) {
            Set<String> stepIds = new HashSet<>();
            for (TestStep step : plan.getSteps()) {
                if (step.getStepId() != null) {
                    if (!stepIds.add(step.getStepId())) {
                        context.buildConstraintViolationWithTemplate(
                            "Duplicate step ID found: " + step.getStepId())
                            .addPropertyNode("steps")
                            .addConstraintViolation();
                        isValid = false;
                    }
                }
            }
            
            // Validate step dependencies exist
            for (TestStep step : plan.getSteps()) {
                if (step.getDependsOn() != null) {
                    for (String dependency : step.getDependsOn()) {
                        if (!stepIds.contains(dependency)) {
                            context.buildConstraintViolationWithTemplate(
                                "Step dependency not found: " + dependency + " for step: " + step.getStepId())
                                .addPropertyNode("steps")
                                .addConstraintViolation();
                            isValid = false;
                        }
                    }
                }
            }
        }
        
        // Validate assertion rule IDs are unique
        if (plan.getAssertions() != null && !plan.getAssertions().isEmpty()) {
            Set<String> assertionIds = new HashSet<>();
            for (var assertion : plan.getAssertions()) {
                if (assertion.getRuleId() != null) {
                    if (!assertionIds.add(assertion.getRuleId())) {
                        context.buildConstraintViolationWithTemplate(
                            "Duplicate assertion rule ID found: " + assertion.getRuleId())
                            .addPropertyNode("assertions")
                            .addConstraintViolation();
                        isValid = false;
                    }
                }
            }
        }
        
        return isValid;
    }
}