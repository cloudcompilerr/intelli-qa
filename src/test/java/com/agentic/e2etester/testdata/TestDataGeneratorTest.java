package com.agentic.e2etester.testdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestDataGeneratorTest {
    
    private TestDataGenerator generator;
    
    @BeforeEach
    void setUp() {
        generator = new TestDataGenerator();
    }
    
    @Test
    void shouldGenerateOrderFulfillmentData() {
        // Given
        TestDataRequest request = new TestDataRequest("order_fulfillment", Map.of());
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKey("order");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) data.get("order");
        assertThat(order).containsKeys("orderId", "customerId", "orderDate", "totalAmount", "items");
        assertThat(order.get("orderId")).asString().startsWith("ORD-");
        assertThat(order.get("customerId")).asString().startsWith("CUST-");
    }
    
    @Test
    void shouldGenerateCustomerData() {
        // Given
        TestDataRequest request = new TestDataRequest();
        request.setRequiredDataTypes(List.of("customer"));
        request.setGenerateRealisticData(true);
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKey("customer");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) data.get("customer");
        assertThat(customer).containsKeys("customerId", "firstName", "lastName", "email", "phone");
        assertThat(customer.get("customerId")).asString().startsWith("CUST-");
        assertThat(customer.get("email")).asString().contains("@test.com");
    }
    
    @Test
    void shouldGeneratePaymentData() {
        // Given
        TestDataRequest request = new TestDataRequest("payment_processing", Map.of());
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKey("payment");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> payment = (Map<String, Object>) data.get("payment");
        assertThat(payment).containsKeys("paymentId", "orderId", "amount", "currency", "method", "status");
        assertThat(payment.get("paymentId")).asString().startsWith("PAY-");
        assertThat(payment.get("currency")).isEqualTo("USD");
        assertThat(payment.get("method")).isEqualTo("CREDIT_CARD");
    }
    
    @Test
    void shouldGenerateProductData() {
        // Given
        TestDataRequest request = new TestDataRequest();
        request.setRequiredDataTypes(List.of("product"));
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKey("product");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) data.get("product");
        assertThat(product).containsKeys("productId", "name", "description", "price", "category", "inStock", "stockQuantity");
        assertThat(product.get("productId")).asString().startsWith("PROD-");
        assertThat(product.get("category")).isEqualTo("Electronics");
    }
    
    @Test
    void shouldApplyParameters() {
        // Given
        Map<String, Object> parameters = Map.of(
            "customerId", "CUST-OVERRIDE",
            "orderStatus", "COMPLETED"
        );
        TestDataRequest request = new TestDataRequest("order_fulfillment", parameters);
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsEntry("customerId", "CUST-OVERRIDE");
        assertThat(data).containsEntry("orderStatus", "COMPLETED");
    }
    
    @Test
    void shouldApplyConstraints() {
        // Given
        TestDataRequest request = new TestDataRequest("order_fulfillment", Map.of());
        request.setConstraints(Map.of("currency", "EUR"));
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKey("order");
        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) data.get("order");
        assertThat(order).containsEntry("currency", "EUR");
    }
    
    @Test
    void shouldGenerateMultipleDataTypes() {
        // Given
        TestDataRequest request = new TestDataRequest();
        request.setRequiredDataTypes(List.of("customer", "product", "address"));
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKeys("customer", "product", "address");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) data.get("address");
        assertThat(address).containsKeys("street", "city", "state", "zipCode", "country");
        assertThat(address.get("country")).isEqualTo("US");
    }
    
    @Test
    void shouldGenerateGenericDataForUnknownScenario() {
        // Given
        TestDataRequest request = new TestDataRequest("unknown_scenario", Map.of());
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKeys("testId", "timestamp", "correlationId", "environment");
        assertThat(data.get("testId")).asString().startsWith("TEST-");
        assertThat(data.get("environment")).isEqualTo("test");
    }
    
    @Test
    void shouldGenerateRealisticDataWhenRequested() {
        // Given
        TestDataRequest request = new TestDataRequest();
        request.setRequiredDataTypes(List.of("string", "number", "boolean", "uuid"));
        request.setGenerateRealisticData(true);
        
        // When
        Map<String, Object> data = generator.generateData(request);
        
        // Then
        assertThat(data).containsKeys("string", "number", "boolean", "uuid");
        assertThat(data.get("string")).isInstanceOf(String.class);
        assertThat(data.get("number")).isInstanceOf(Integer.class);
        assertThat(data.get("boolean")).isInstanceOf(Boolean.class);
        assertThat(data.get("uuid")).asString().contains("-"); // UUID format
    }
}