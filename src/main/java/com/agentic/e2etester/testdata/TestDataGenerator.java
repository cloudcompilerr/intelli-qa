package com.agentic.e2etester.testdata;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Component for generating dynamic test data
 */
@Component
public class TestDataGenerator {
    
    private final Random random = new Random();
    
    public Map<String, Object> generateData(TestDataRequest request) {
        Map<String, Object> data = new HashMap<>();
        
        // Generate basic test data based on scenario
        if (request.getTestScenario() != null) {
            data.putAll(generateScenarioSpecificData(request.getTestScenario()));
        }
        
        // Generate data for required types
        if (request.getRequiredDataTypes() != null) {
            for (String dataType : request.getRequiredDataTypes()) {
                data.putAll(generateDataByType(dataType, request.isGenerateRealisticData()));
            }
        }
        
        // Apply parameters
        if (request.getParameters() != null) {
            data.putAll(request.getParameters());
        }
        
        // Apply constraints
        if (request.getConstraints() != null) {
            data = applyConstraints(data, request.getConstraints());
        }
        
        return data;
    }
    
    private Map<String, Object> generateScenarioSpecificData(String scenario) {
        Map<String, Object> data = new HashMap<>();
        
        switch (scenario.toLowerCase()) {
            case "order_fulfillment":
                data.putAll(generateOrderData());
                break;
            case "payment_processing":
                data.putAll(generatePaymentData());
                break;
            case "inventory_management":
                data.putAll(generateInventoryData());
                break;
            case "customer_registration":
                data.putAll(generateCustomerData());
                break;
            default:
                data.putAll(generateGenericTestData());
        }
        
        return data;
    }
    
    private Map<String, Object> generateDataByType(String dataType, boolean realistic) {
        Map<String, Object> data = new HashMap<>();
        
        switch (dataType.toLowerCase()) {
            case "customer":
                data.putAll(generateCustomerData());
                break;
            case "order":
                data.putAll(generateOrderData());
                break;
            case "product":
                data.putAll(generateProductData());
                break;
            case "payment":
                data.putAll(generatePaymentData());
                break;
            case "address":
                data.putAll(generateAddressData());
                break;
            default:
                data.put(dataType, generateGenericValue(dataType, realistic));
        }
        
        return data;
    }
    
    private Map<String, Object> generateOrderData() {
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", "ORD-" + generateRandomString(8));
        order.put("customerId", "CUST-" + generateRandomString(6));
        order.put("orderDate", Instant.now().toString());
        order.put("totalAmount", random.nextDouble() * 1000 + 10);
        order.put("currency", "USD");
        order.put("status", "PENDING");
        
        List<Map<String, Object>> items = new ArrayList<>();
        int itemCount = random.nextInt(5) + 1;
        for (int i = 0; i < itemCount; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", "PROD-" + generateRandomString(6));
            item.put("quantity", random.nextInt(10) + 1);
            item.put("price", random.nextDouble() * 100 + 5);
            items.add(item);
        }
        order.put("items", items);
        
        return Map.of("order", order);
    }
    
    private Map<String, Object> generateCustomerData() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("customerId", "CUST-" + generateRandomString(6));
        customer.put("firstName", generateRandomName());
        customer.put("lastName", generateRandomName());
        customer.put("email", generateRandomEmail());
        customer.put("phone", generateRandomPhone());
        customer.put("registrationDate", Instant.now().toString());
        customer.put("status", "ACTIVE");
        
        return Map.of("customer", customer);
    }
    
    private Map<String, Object> generateProductData() {
        Map<String, Object> product = new HashMap<>();
        product.put("productId", "PROD-" + generateRandomString(6));
        product.put("name", "Test Product " + generateRandomString(4));
        product.put("description", "Test product description");
        product.put("price", random.nextDouble() * 500 + 10);
        product.put("category", "Electronics");
        product.put("inStock", random.nextBoolean());
        product.put("stockQuantity", random.nextInt(100));
        
        return Map.of("product", product);
    }
    
    private Map<String, Object> generatePaymentData() {
        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentId", "PAY-" + generateRandomString(8));
        payment.put("orderId", "ORD-" + generateRandomString(8));
        payment.put("amount", random.nextDouble() * 1000 + 10);
        payment.put("currency", "USD");
        payment.put("method", "CREDIT_CARD");
        payment.put("status", "PENDING");
        payment.put("timestamp", Instant.now().toString());
        
        return Map.of("payment", payment);
    }
    
    private Map<String, Object> generateInventoryData() {
        Map<String, Object> inventory = new HashMap<>();
        inventory.put("productId", "PROD-" + generateRandomString(6));
        inventory.put("warehouseId", "WH-" + generateRandomString(4));
        inventory.put("quantity", random.nextInt(1000));
        inventory.put("reservedQuantity", random.nextInt(100));
        inventory.put("lastUpdated", Instant.now().toString());
        
        return Map.of("inventory", inventory);
    }
    
    private Map<String, Object> generateAddressData() {
        Map<String, Object> address = new HashMap<>();
        address.put("street", generateRandomString(10) + " Street");
        address.put("city", "Test City");
        address.put("state", "TS");
        address.put("zipCode", String.format("%05d", random.nextInt(100000)));
        address.put("country", "US");
        
        return Map.of("address", address);
    }
    
    private Map<String, Object> generateGenericTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("testId", "TEST-" + generateRandomString(8));
        data.put("timestamp", Instant.now().toString());
        data.put("correlationId", UUID.randomUUID().toString());
        data.put("environment", "test");
        
        return data;
    }
    
    private Object generateGenericValue(String type, boolean realistic) {
        if (realistic) {
            switch (type.toLowerCase()) {
                case "string":
                    return generateRandomString(10);
                case "number":
                case "integer":
                    return random.nextInt(1000);
                case "decimal":
                case "double":
                    return random.nextDouble() * 1000;
                case "boolean":
                    return random.nextBoolean();
                case "date":
                case "timestamp":
                    return Instant.now().toString();
                case "uuid":
                    return UUID.randomUUID().toString();
                default:
                    return "test_" + generateRandomString(6);
            }
        } else {
            return "test_value_" + generateRandomString(4);
        }
    }
    
    private Map<String, Object> applyConstraints(Map<String, Object> data, Map<String, String> constraints) {
        Map<String, Object> constrainedData = new HashMap<>(data);
        
        for (Map.Entry<String, String> constraint : constraints.entrySet()) {
            String key = constraint.getKey();
            String value = constraint.getValue();
            
            // Apply constraint recursively to nested structures
            applyConstraintRecursively(constrainedData, key, value);
        }
        
        return constrainedData;
    }
    
    @SuppressWarnings("unchecked")
    private void applyConstraintRecursively(Map<String, Object> data, String key, String value) {
        // First check if the key exists at the current level
        if (data.containsKey(key)) {
            if (value.startsWith("min:")) {
                // Handle minimum value constraints
                return;
            } else if (value.startsWith("max:")) {
                // Handle maximum value constraints
                return;
            } else {
                // Direct value override
                data.put(key, value);
                return;
            }
        }
        
        // If not found at current level, search nested maps
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Map) {
                applyConstraintRecursively((Map<String, Object>) entry.getValue(), key, value);
            }
        }
    }
    
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private String generateRandomName() {
        String[] names = {"John", "Jane", "Bob", "Alice", "Charlie", "Diana", "Eve", "Frank"};
        return names[random.nextInt(names.length)];
    }
    
    private String generateRandomEmail() {
        return generateRandomName().toLowerCase() + "@test.com";
    }
    
    private String generateRandomPhone() {
        return String.format("(%03d) %03d-%04d", 
                           random.nextInt(900) + 100,
                           random.nextInt(900) + 100,
                           random.nextInt(9000) + 1000);
    }
}