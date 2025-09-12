package com.agentic.e2etester.testing.assertion;

import com.agentic.e2etester.model.AssertionRule;
import com.agentic.e2etester.model.AssertionSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a business logic assertion that validates business outcomes and rules.
 * Used for validating end-to-end business processes and customer-facing functionality.
 */
public class BusinessAssertion extends AssertionRule {
    
    @NotBlank(message = "Business rule cannot be blank")
    private String businessRule;
    
    @NotNull(message = "Business context cannot be null")
    private Map<String, Object> businessContext;
    
    private String customerSegment;
    private String orderType;
    private String fulfillmentPath;
    
    // Default constructor
    public BusinessAssertion() {
        super();
        this.setSeverity(AssertionSeverity.CRITICAL);
    }
    
    // Constructor with required fields
    public BusinessAssertion(String ruleId, String description, String businessRule, Map<String, Object> businessContext) {
        super(ruleId, null, description);
        this.businessRule = businessRule;
        this.businessContext = businessContext;
        this.setSeverity(AssertionSeverity.CRITICAL);
    }
    
    // Getters and setters
    public String getBusinessRule() {
        return businessRule;
    }
    
    public void setBusinessRule(String businessRule) {
        this.businessRule = businessRule;
    }
    
    public Map<String, Object> getBusinessContext() {
        return businessContext;
    }
    
    public void setBusinessContext(Map<String, Object> businessContext) {
        this.businessContext = businessContext;
    }
    
    public String getCustomerSegment() {
        return customerSegment;
    }
    
    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
    }
    
    public String getOrderType() {
        return orderType;
    }
    
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    
    public String getFulfillmentPath() {
        return fulfillmentPath;
    }
    
    public void setFulfillmentPath(String fulfillmentPath) {
        this.fulfillmentPath = fulfillmentPath;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BusinessAssertion that = (BusinessAssertion) o;
        return Objects.equals(businessRule, that.businessRule) &&
               Objects.equals(customerSegment, that.customerSegment) &&
               Objects.equals(orderType, that.orderType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), businessRule, customerSegment, orderType);
    }
    
    @Override
    public String toString() {
        return "BusinessAssertion{" +
               "ruleId='" + getRuleId() + '\'' +
               ", businessRule='" + businessRule + '\'' +
               ", customerSegment='" + customerSegment + '\'' +
               ", orderType='" + orderType + '\'' +
               ", fulfillmentPath='" + fulfillmentPath + '\'' +
               '}';
    }
}