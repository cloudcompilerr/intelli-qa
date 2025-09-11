package com.agentic.e2etester.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for TestExecutionPlan.
 */
@Documented
@Constraint(validatedBy = TestExecutionPlanValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTestExecutionPlan {
    String message() default "Invalid test execution plan";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}