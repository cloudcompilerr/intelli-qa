# Examples

This directory contains practical examples of using the Agentic E2E Tester for various testing scenarios.

## Table of Contents

1. [Basic Test Scenarios](#basic-test-scenarios)
2. [E-commerce Order Flow](#e-commerce-order-flow)
3. [Payment Processing](#payment-processing)
4. [User Management](#user-management)
5. [API Integration Testing](#api-integration-testing)
6. [Error Handling Scenarios](#error-handling-scenarios)
7. [Performance Testing](#performance-testing)

## Basic Test Scenarios

### Simple Health Check Test

```json
{
  "scenario": "Verify all microservices are healthy and responding",
  "testData": {},
  "configuration": {
    "timeout": "PT2M",
    "environment": "staging"
  }
}
```

**Expected Behavior:**
- Discovers all registered services
- Performs health checks on each service
- Validates response times are within acceptable limits
- Reports overall system health status

### Service Discovery Test

```json
{
  "scenario": "Test automatic service discovery and endpoint validation",
  "testData": {
    "expectedServices": ["order-service", "payment-service", "inventory-service", "notification-service"]
  },
  "configuration": {
    "timeout": "PT5M",
    "validateEndpoints": true
  }
}
```

## E-commerce Order Flow

### Complete Order Fulfillment

```json
{
  "scenario": "Test complete order fulfillment from cart creation to delivery confirmation",
  "testData": {
    "customer": {
      "id": "customer-12345",
      "email": "test@example.com",
      "membershipLevel": "premium"
    },
    "products": [
      {
        "id": "product-67890",
        "name": "Wireless Headphones",
        "price": 99.99,
        "quantity": 2
      }
    ],
    "paymentMethod": {
      "type": "credit_card",
      "last4": "4567"
    },
    "shippingAddress": {
      "street": "123 Test Street",
      "city": "Test City",
      "zipCode": "12345",
      "country": "US"
    }
  },
  "configuration": {
    "timeout": "PT10M",
    "validateInventory": true,
    "validatePayment": true,
    "validateShipping": true
  }
}
```

**Test Steps Generated:**
1. Create shopping cart
2. Add products to cart
3. Apply membership discounts
4. Validate inventory availability
5. Process payment
6. Update inventory
7. Generate shipping label
8. Send confirmation email
9. Update order status
10. Validate end-to-end data consistency

### Order Cancellation Flow

```json
{
  "scenario": "Test order cancellation and refund process",
  "testData": {
    "orderId": "order-98765",
    "cancellationReason": "customer_request",
    "refundMethod": "original_payment_method"
  },
  "configuration": {
    "timeout": "PT5M",
    "validateRefund": true,
    "validateInventoryRestore": true
  }
}
```

## Payment Processing

### Credit Card Payment

```json
{
  "scenario": "Process credit card payment with fraud detection and authorization",
  "testData": {
    "payment": {
      "amount": 149.99,
      "currency": "USD",
      "cardNumber": "4111111111111111",
      "expiryMonth": "12",
      "expiryYear": "2025",
      "cvv": "123"
    },
    "billingAddress": {
      "street": "456 Payment Ave",
      "city": "Finance City",
      "zipCode": "54321",
      "country": "US"
    },
    "merchantId": "merchant-123"
  },
  "configuration": {
    "timeout": "PT3M",
    "validateFraudCheck": true,
    "validateAuthorization": true,
    "validateSettlement": false
  }
}
```

### Payment Failure Handling

```json
{
  "scenario": "Test payment failure scenarios and retry mechanisms",
  "testData": {
    "payment": {
      "amount": 99.99,
      "currency": "USD",
      "cardNumber": "4000000000000002",  // Declined card
      "expiryMonth": "12",
      "expiryYear": "2025",
      "cvv": "123"
    },
    "retryPolicy": {
      "maxAttempts": 3,
      "backoffMultiplier": 2
    }
  },
  "expectedOutcomes": [
    {
      "type": "PAYMENT_DECLINED",
      "action": "RETRY_WITH_DIFFERENT_CARD"
    }
  ]
}
```

## User Management

### User Registration Flow

```json
{
  "scenario": "Test new user registration with email verification",
  "testData": {
    "user": {
      "email": "newuser@example.com",
      "password": "SecurePassword123!",
      "firstName": "John",
      "lastName": "Doe",
      "dateOfBirth": "1990-01-01"
    },
    "preferences": {
      "newsletter": true,
      "notifications": true
    }
  },
  "configuration": {
    "timeout": "PT5M",
    "validateEmailSent": true,
    "validatePasswordStrength": true
  }
}
```

### User Authentication Flow

```json
{
  "scenario": "Test user login with multi-factor authentication",
  "testData": {
    "credentials": {
      "email": "existing@example.com",
      "password": "UserPassword123!"
    },
    "mfaEnabled": true,
    "deviceTrusted": false
  },
  "configuration": {
    "timeout": "PT3M",
    "validateMFA": true,
    "validateSessionCreation": true
  }
}
```

## API Integration Testing

### REST API Chain Testing

```json
{
  "scenario": "Test REST API chain across multiple microservices",
  "testData": {
    "apiChain": [
      {
        "service": "user-service",
        "endpoint": "/api/v1/users",
        "method": "POST",
        "payload": {
          "name": "Test User",
          "email": "test@example.com"
        }
      },
      {
        "service": "profile-service",
        "endpoint": "/api/v1/profiles",
        "method": "POST",
        "payload": {
          "userId": "${previous.response.id}",
          "preferences": {"theme": "dark"}
        }
      },
      {
        "service": "notification-service",
        "endpoint": "/api/v1/notifications/welcome",
        "method": "POST",
        "payload": {
          "userId": "${step1.response.id}",
          "email": "${step1.response.email}"
        }
      }
    ]
  },
  "configuration": {
    "timeout": "PT5M",
    "validateResponseChain": true
  }
}
```

### GraphQL API Testing

```json
{
  "scenario": "Test GraphQL API with complex queries and mutations",
  "testData": {
    "queries": [
      {
        "name": "getUserWithOrders",
        "query": "query GetUser($id: ID!) { user(id: $id) { id name email orders { id total status } } }",
        "variables": {"id": "user-123"}
      },
      {
        "name": "createOrder",
        "query": "mutation CreateOrder($input: OrderInput!) { createOrder(input: $input) { id total status } }",
        "variables": {
          "input": {
            "userId": "user-123",
            "items": [{"productId": "prod-456", "quantity": 2}]
          }
        }
      }
    ]
  },
  "configuration": {
    "timeout": "PT3M",
    "validateSchema": true
  }
}
```

## Error Handling Scenarios

### Service Unavailable Handling

```json
{
  "scenario": "Test system behavior when critical services are unavailable",
  "testData": {
    "unavailableServices": ["payment-service"],
    "expectedBehavior": "graceful_degradation",
    "fallbackMechanisms": ["cached_responses", "retry_with_backoff"]
  },
  "configuration": {
    "timeout": "PT5M",
    "simulateFailures": true,
    "validateFallbacks": true
  }
}
```

### Network Timeout Handling

```json
{
  "scenario": "Test network timeout handling and retry mechanisms",
  "testData": {
    "timeoutScenarios": [
      {
        "service": "external-api",
        "timeoutDuration": "PT30S",
        "expectedRetries": 3
      }
    ]
  },
  "configuration": {
    "timeout": "PT10M",
    "validateRetryPolicy": true,
    "validateCircuitBreaker": true
  }
}
```

### Data Consistency Validation

```json
{
  "scenario": "Test data consistency across multiple databases after failures",
  "testData": {
    "transactionScenario": {
      "operations": [
        {"database": "orders", "operation": "insert", "table": "orders"},
        {"database": "inventory", "operation": "update", "table": "products"},
        {"database": "payments", "operation": "insert", "table": "transactions"}
      ],
      "failurePoint": "after_inventory_update"
    }
  },
  "configuration": {
    "timeout": "PT5M",
    "validateRollback": true,
    "validateConsistency": true
  }
}
```

## Performance Testing

### Load Testing Scenario

```json
{
  "scenario": "Test system performance under high load",
  "testData": {
    "loadProfile": {
      "virtualUsers": 100,
      "duration": "PT5M",
      "rampUpTime": "PT1M"
    },
    "testScenarios": [
      {
        "name": "user_registration",
        "weight": 30,
        "data": {"userCount": 1000}
      },
      {
        "name": "order_creation",
        "weight": 50,
        "data": {"orderCount": 500}
      },
      {
        "name": "payment_processing",
        "weight": 20,
        "data": {"paymentCount": 200}
      }
    ]
  },
  "configuration": {
    "timeout": "PT10M",
    "validatePerformanceMetrics": true,
    "performanceThresholds": {
      "averageResponseTime": "PT2S",
      "errorRate": 0.01,
      "throughput": 100
    }
  }
}
```

### Stress Testing Scenario

```json
{
  "scenario": "Test system breaking point and recovery",
  "testData": {
    "stressProfile": {
      "initialLoad": 50,
      "maxLoad": 1000,
      "incrementStep": 50,
      "incrementInterval": "PT30S"
    },
    "monitoringMetrics": [
      "cpu_usage",
      "memory_usage",
      "response_time",
      "error_rate",
      "database_connections"
    ]
  },
  "configuration": {
    "timeout": "PT30M",
    "stopOnFailure": false,
    "validateRecovery": true
  }
}
```

## Advanced Scenarios

### Chaos Engineering Test

```json
{
  "scenario": "Test system resilience with chaos engineering",
  "testData": {
    "chaosExperiments": [
      {
        "type": "pod_failure",
        "target": "payment-service",
        "duration": "PT2M"
      },
      {
        "type": "network_latency",
        "target": "database",
        "latency": "PT5S",
        "duration": "PT3M"
      },
      {
        "type": "cpu_stress",
        "target": "order-service",
        "cpuLoad": 90,
        "duration": "PT2M"
      }
    ]
  },
  "configuration": {
    "timeout": "PT15M",
    "validateRecovery": true,
    "monitorSystemHealth": true
  }
}
```

### Multi-Environment Testing

```json
{
  "scenario": "Test deployment across multiple environments",
  "testData": {
    "environments": ["staging", "pre-prod", "prod"],
    "testSuite": "smoke_tests",
    "deploymentValidation": {
      "validateVersion": true,
      "validateConfiguration": true,
      "validateConnectivity": true
    }
  },
  "configuration": {
    "timeout": "PT20M",
    "parallelExecution": false,
    "stopOnFirstFailure": true
  }
}
```

## Running Examples

### Using REST API

```bash
# Execute a basic test
curl -X POST http://localhost:8080/api/v1/tests/execute \
  -H "Content-Type: application/json" \
  -d @examples/basic-health-check.json

# Execute with custom configuration
curl -X POST http://localhost:8080/api/v1/tests/execute \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d @examples/order-fulfillment.json
```

### Using CLI (if available)

```bash
# Execute test from file
agentic-tester execute --file examples/payment-processing.json

# Execute with environment override
agentic-tester execute --file examples/user-registration.json --env production
```

### Batch Execution

```bash
# Execute multiple tests
for test in examples/*.json; do
  echo "Executing $test"
  curl -X POST http://localhost:8080/api/v1/tests/execute \
    -H "Content-Type: application/json" \
    -d @"$test"
  sleep 10
done
```

## Custom Test Development

### Creating Custom Scenarios

1. **Define the business flow:**
   ```json
   {
     "scenario": "Your custom business flow description",
     "testData": {
       // Your specific test data
     }
   }
   ```

2. **Add validation rules:**
   ```json
   {
     "expectedOutcomes": [
       {
         "type": "HTTP_STATUS",
         "service": "your-service",
         "expected": 200
       },
       {
         "type": "DATABASE_STATE",
         "table": "your_table",
         "condition": "count > 0"
       }
     ]
   }
   ```

3. **Configure environment-specific settings:**
   ```json
   {
     "configuration": {
       "environment": "staging",
       "timeout": "PT5M",
       "retryPolicy": {
         "maxAttempts": 3,
         "backoffMultiplier": 2
       }
     }
   }
   ```

### Best Practices

1. **Use realistic test data** that mirrors production scenarios
2. **Include edge cases** and error conditions
3. **Validate both positive and negative outcomes**
4. **Set appropriate timeouts** based on expected execution time
5. **Use correlation IDs** for tracing across services
6. **Document expected behavior** for complex scenarios
7. **Version your test scenarios** for reproducibility

For more examples and detailed explanations, see the [User Guide](../user-guide.md).