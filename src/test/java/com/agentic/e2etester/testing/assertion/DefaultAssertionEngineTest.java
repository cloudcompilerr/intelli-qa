package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DefaultAssertionEngineTest {
    
    private DefaultAssertionEngine assertionEngine;
    private TestContext testContext;
    
    @BeforeEach
    void setUp() {
        assertionEngine = new DefaultAssertionEngine();
        testContext = createTestContext();
    }
    
    @Test
    void testEvaluateAssertion_NullRule_ReturnsFailedResult() {
        AssertionResult result = assertionEngine.evaluateAssertion(null, testContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("INVALID_RULE", result.getRuleId());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    void testEvaluateAssertion_DisabledRule_ReturnsSkippedResult() {
        AssertionRule rule = new AssertionRule("test-rule", AssertionType.EQUALS, "Test rule");
        rule.setEnabled(false);
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("test-rule", result.getRuleId());
        assertEquals("Assertion rule is disabled", result.getMessage());
    }
    
    @Test
    void testEvaluateAssertion_EqualsType_Success() {
        AssertionRule rule = new AssertionRule("equals-test", AssertionType.EQUALS, "Test equals assertion");
        rule.setActualValuePath("context.correlationId");
        rule.setExpectedValue("test-correlation-123");
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("equals-test", result.getRuleId());
        assertEquals("test-correlation-123", result.getActualValue());
        assertEquals("test-correlation-123", result.getExpectedValue());
    }
    
    @Test
    void testEvaluateAssertion_EqualsType_Failure() {
        AssertionRule rule = new AssertionRule("equals-test", AssertionType.EQUALS, "Test equals assertion");
        rule.setActualValuePath("context.correlationId");
        rule.setExpectedValue("different-value");
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("equals-test", result.getRuleId());
        assertEquals("test-correlation-123", result.getActualValue());
        assertEquals("different-value", result.getExpectedValue());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    void testEvaluateAssertion_ContainsType_Success() {
        AssertionRule rule = new AssertionRule("contains-test", AssertionType.CONTAINS, "Test contains assertion");
        rule.setActualValuePath("testVariable");
        rule.setExpectedValue("test");
        
        testContext.setVariable("testVariable", "this is a test value");
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("contains-test", result.getRuleId());
    }
    
    @Test
    void testEvaluateAssertion_GreaterThanType_Success() {
        AssertionRule rule = new AssertionRule("gt-test", AssertionType.GREATER_THAN, "Test greater than assertion");
        rule.setActualValuePath("numericValue");
        rule.setExpectedValue(10);
        
        testContext.setVariable("numericValue", 15);
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("gt-test", result.getRuleId());
    }
    
    @Test
    void testEvaluateAssertion_RegexMatchType_Success() {
        AssertionRule rule = new AssertionRule("regex-test", AssertionType.REGEX_MATCH, "Test regex assertion");
        rule.setActualValuePath("emailValue");
        rule.setExpectedValue("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        
        testContext.setVariable("emailValue", "test@example.com");
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("regex-test", result.getRuleId());
    }
    
    @Test
    void testEvaluateAssertion_ResponseTimeType_Success() {
        AssertionRule rule = new AssertionRule("response-time-test", AssertionType.RESPONSE_TIME, "Test response time assertion");
        rule.setActualValuePath("interaction.responseTime");
        rule.setExpectedValue(Duration.ofMillis(2000));
        
        // Add service interaction with response time
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("test-service");
        interaction.setResponseTime(Duration.ofMillis(1500));
        testContext.addInteraction(interaction);
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("response-time-test", result.getRuleId());
    }
    
    @Test
    void testEvaluateAssertion_StatusCodeType_Success() {
        AssertionRule rule = new AssertionRule("status-code-test", AssertionType.STATUS_CODE, "Test status code assertion");
        rule.setActualValuePath("statusCode");
        rule.setExpectedValue(200);
        
        testContext.setVariable("statusCode", 200);
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("status-code-test", result.getRuleId());
    }
    
    @Test
    void testEvaluateAssertions_MultipleRules() {
        List<AssertionRule> rules = Arrays.asList(
            createRule("rule1", AssertionType.EQUALS, "context.correlationId", "test-correlation-123"),
            createRule("rule2", AssertionType.CONTAINS, "testVariable", "test"),
            createRule("rule3", AssertionType.GREATER_THAN, "numericValue", 10)
        );
        
        testContext.setVariable("testVariable", "this is a test value");
        testContext.setVariable("numericValue", 15);
        
        List<AssertionResult> results = assertionEngine.evaluateAssertions(rules, testContext);
        
        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(AssertionResult::getPassed));
    }
    
    @Test
    void testValidateBusinessOutcome_Success() {
        BusinessAssertion assertion = new BusinessAssertion("business-test", "Order completion test", 
                                                           "order_completed", Map.of("orderType", "standard"));
        assertion.setExpectedValue("COMPLETED");
        
        testContext.setVariable("businessOutcome", "COMPLETED");
        
        AssertionResult result = assertionEngine.validateBusinessOutcome(assertion, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("business-test", result.getRuleId());
        assertEquals(AssertionSeverity.CRITICAL, result.getSeverity());
    }
    
    @Test
    void testValidateTechnicalMetrics_ResponseTime_Success() {
        TechnicalAssertion assertion = new TechnicalAssertion("tech-test", "Response time test", "response_time");
        assertion.setServiceId("test-service");
        assertion.setResponseTimeThreshold(Duration.ofMillis(2000));
        
        ServiceInteraction interaction = new ServiceInteraction();
        interaction.setServiceId("test-service");
        interaction.setResponseTime(Duration.ofMillis(1500));
        testContext.addInteraction(interaction);
        
        AssertionResult result = assertionEngine.validateTechnicalMetrics(assertion, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("tech-test", result.getRuleId());
    }
    
    @Test
    void testValidateDataConsistency_RequiredFields_Success() {
        DataAssertion assertion = new DataAssertion("data-test", "Required fields test", "userData");
        assertion.setRequiredFields(Arrays.asList("id", "name", "email"));
        
        Map<String, Object> userData = Map.of(
            "id", "123",
            "name", "John Doe",
            "email", "john@example.com"
        );
        testContext.setVariable("userData", userData);
        
        AssertionResult result = assertionEngine.validateDataConsistency(assertion, testContext);
        
        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("data-test", result.getRuleId());
    }
    
    @Test
    void testValidateDataConsistency_RequiredFields_Failure() {
        DataAssertion assertion = new DataAssertion("data-test", "Required fields test", "userData");
        assertion.setRequiredFields(Arrays.asList("id", "name", "email"));
        
        Map<String, Object> userData = Map.of(
            "id", "123",
            "name", "John Doe"
            // Missing email field
        );
        testContext.setVariable("userData", userData);
        
        AssertionResult result = assertionEngine.validateDataConsistency(assertion, testContext);
        
        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("data-test", result.getRuleId());
    }
    
    @Test
    void testRegisterCustomEvaluator() {
        CustomAssertionEvaluator customEvaluator = new CustomAssertionEvaluator() {
            @Override
            public String getEvaluatorName() {
                return "TestEvaluator";
            }
            
            @Override
            public boolean canEvaluate(AssertionRule rule) {
                return "custom_test".equals(rule.getCondition());
            }
            
            @Override
            public AssertionResult evaluate(AssertionRule rule, TestContext context) {
                return new AssertionResult(rule.getRuleId(), true);
            }
        };
        
        assertionEngine.registerCustomEvaluator(customEvaluator);
        
        AssertionRule rule = new AssertionRule("custom-rule", AssertionType.CUSTOM, "Custom test");
        rule.setCondition("custom_test");
        
        assertTrue(assertionEngine.supportsAssertion(rule));
        
        AssertionResult result = assertionEngine.evaluateAssertion(rule, testContext);
        assertNotNull(result);
        assertTrue(result.getPassed());
    }
    
    @Test
    void testSupportsAssertion_BuiltInTypes() {
        AssertionRule rule = new AssertionRule("test", AssertionType.EQUALS, "Test");
        assertTrue(assertionEngine.supportsAssertion(rule));
        
        rule.setType(null);
        assertFalse(assertionEngine.supportsAssertion(rule));
        
        assertFalse(assertionEngine.supportsAssertion(null));
    }
    
    private TestContext createTestContext() {
        TestContext context = new TestContext("test-correlation-123");
        context.setCurrentStepId("step-1");
        context.setTestExecutionPlanId("plan-123");
        context.setStartTime(Instant.now());
        return context;
    }
    
    private AssertionRule createRule(String ruleId, AssertionType type, String actualValuePath, Object expectedValue) {
        AssertionRule rule = new AssertionRule(ruleId, type, "Test rule");
        rule.setActualValuePath(actualValuePath);
        rule.setExpectedValue(expectedValue);
        return rule;
    }
}