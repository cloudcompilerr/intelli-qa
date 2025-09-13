# API Documentation

The Agentic E2E Tester provides a comprehensive REST API for test execution, monitoring, and management.

## Base URL

```
http://localhost:8080/api/v1
```

## Authentication

The API supports multiple authentication methods:

- **API Key**: Include `X-API-Key` header
- **JWT Token**: Include `Authorization: Bearer <token>` header
- **Basic Auth**: For development environments

## API Endpoints

### Test Management

#### Parse Test Scenario
Convert natural language test scenarios into executable test plans.

```http
POST /tests/parse
Content-Type: application/json

{
  "scenario": "Test order fulfillment from cart to delivery",
  "context": {
    "environment": "staging",
    "services": ["order-service", "payment-service", "inventory-service"]
  }
}
```

**Response:**
```json
{
  "testId": "test-12345",
  "executionPlan": {
    "steps": [
      {
        "stepId": "step-1",
        "type": "REST_CALL",
        "targetService": "order-service",
        "action": "POST /orders",
        "expectedOutcomes": ["ORDER_CREATED"]
      }
    ]
  },
  "estimatedDuration": "PT5M"
}
```

#### Execute Test
Execute a test plan or natural language scenario.

```http
POST /tests/execute
Content-Type: application/json

{
  "scenario": "Create order and validate payment processing",
  "testData": {
    "productId": "12345",
    "customerId": "customer-001",
    "paymentMethod": "credit-card"
  },
  "configuration": {
    "timeout": "PT10M",
    "retryPolicy": {
      "maxAttempts": 3,
      "backoffMultiplier": 2
    }
  }
}
```

**Response:**
```json
{
  "testId": "test-67890",
  "status": "RUNNING",
  "startTime": "2025-01-15T10:30:00Z",
  "estimatedCompletion": "2025-01-15T10:35:00Z",
  "progressUrl": "/tests/test-67890/progress"
}
```

#### Get Test Status
Retrieve current status and progress of a running test.

```http
GET /tests/{testId}/status
```

**Response:**
```json
{
  "testId": "test-67890",
  "status": "RUNNING",
  "progress": {
    "completedSteps": 3,
    "totalSteps": 8,
    "currentStep": {
      "stepId": "step-4",
      "description": "Validating payment processing",
      "status": "IN_PROGRESS"
    }
  },
  "metrics": {
    "duration": "PT2M30S",
    "successfulAssertions": 12,
    "failedAssertions": 0
  }
}
```

#### Get Test Results
Retrieve detailed test execution results.

```http
GET /tests/{testId}/results
```

**Response:**
```json
{
  "testId": "test-67890",
  "status": "COMPLETED",
  "result": "PASSED",
  "duration": "PT4M15S",
  "summary": {
    "totalSteps": 8,
    "passedSteps": 8,
    "failedSteps": 0,
    "skippedSteps": 0
  },
  "steps": [
    {
      "stepId": "step-1",
      "description": "Create order",
      "status": "PASSED",
      "duration": "PT0.5S",
      "assertions": [
        {
          "type": "HTTP_STATUS",
          "expected": 201,
          "actual": 201,
          "result": "PASSED"
        }
      ]
    }
  ],
  "correlationTrace": {
    "traceId": "trace-abc123",
    "spans": [...]
  }
}
```

### Service Discovery

#### List Discovered Services
Get all discovered microservices and their health status.

```http
GET /services
```

**Response:**
```json
{
  "services": [
    {
      "serviceId": "order-service",
      "name": "Order Management Service",
      "baseUrl": "http://order-service:8080",
      "health": "UP",
      "version": "1.2.3",
      "endpoints": [
        {
          "path": "/orders",
          "methods": ["GET", "POST"],
          "description": "Order management operations"
        }
      ]
    }
  ]
}
```

#### Register Service
Manually register a service for testing.

```http
POST /services/register
Content-Type: application/json

{
  "serviceId": "custom-service",
  "name": "Custom Service",
  "baseUrl": "http://custom-service:8080",
  "healthEndpoint": "/actuator/health",
  "apiDocumentation": "http://custom-service:8080/v3/api-docs"
}
```

### Monitoring and Metrics

#### Get Test Metrics
Retrieve aggregated test execution metrics.

```http
GET /metrics/tests
```

**Query Parameters:**
- `from`: Start date (ISO 8601)
- `to`: End date (ISO 8601)
- `service`: Filter by service name
- `status`: Filter by test status

**Response:**
```json
{
  "period": {
    "from": "2025-01-15T00:00:00Z",
    "to": "2025-01-15T23:59:59Z"
  },
  "summary": {
    "totalTests": 45,
    "passedTests": 42,
    "failedTests": 3,
    "averageDuration": "PT3M45S",
    "successRate": 93.3
  },
  "byService": [
    {
      "serviceId": "order-service",
      "testCount": 15,
      "successRate": 100.0,
      "averageResponseTime": "PT0.2S"
    }
  ]
}
```

#### Get System Health
Check overall system health and component status.

```http
GET /health/system
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "llm": {
      "status": "UP",
      "details": {
        "model": "codellama:7b",
        "responseTime": "PT0.5S"
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "brokers": 3,
        "topics": 12
      }
    },
    "couchbase": {
      "status": "UP",
      "details": {
        "clusters": 1,
        "buckets": 3
      }
    }
  }
}
```

### AI and Analysis

#### Analyze Test Failure
Get AI-powered analysis of test failures.

```http
POST /analysis/failure
Content-Type: application/json

{
  "testId": "test-failed-123",
  "includeRemediation": true,
  "includeSimilarFailures": true
}
```

**Response:**
```json
{
  "analysisId": "analysis-456",
  "rootCause": {
    "category": "SERVICE_UNAVAILABLE",
    "service": "payment-service",
    "description": "Payment service returned 503 Service Unavailable",
    "confidence": 0.95
  },
  "remediation": [
    {
      "type": "RETRY_WITH_BACKOFF",
      "description": "Retry the payment request with exponential backoff",
      "priority": "HIGH"
    },
    {
      "type": "CHECK_SERVICE_HEALTH",
      "description": "Verify payment service health and dependencies",
      "priority": "MEDIUM"
    }
  ],
  "similarFailures": [
    {
      "testId": "test-789",
      "similarity": 0.87,
      "resolution": "Service was restarted and test passed on retry"
    }
  ]
}
```

### Configuration Management

#### Get Configuration
Retrieve current system configuration.

```http
GET /config
```

#### Update Configuration
Update system configuration (requires admin privileges).

```http
PUT /config
Content-Type: application/json

{
  "testing": {
    "defaultTimeout": "PT5M",
    "maxConcurrentTests": 10
  },
  "ai": {
    "model": "codellama:13b",
    "temperature": 0.1
  }
}
```

## Error Handling

All API endpoints return consistent error responses:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid test scenario format",
    "details": {
      "field": "scenario",
      "reason": "Scenario cannot be empty"
    },
    "timestamp": "2025-01-15T10:30:00Z",
    "requestId": "req-12345"
  }
}
```

### Common Error Codes

- `VALIDATION_ERROR` (400): Invalid request data
- `UNAUTHORIZED` (401): Authentication required
- `FORBIDDEN` (403): Insufficient permissions
- `NOT_FOUND` (404): Resource not found
- `CONFLICT` (409): Resource conflict
- `RATE_LIMITED` (429): Too many requests
- `INTERNAL_ERROR` (500): Server error
- `SERVICE_UNAVAILABLE` (503): Service temporarily unavailable

## Rate Limiting

API endpoints are rate limited:

- **Test Execution**: 10 requests per minute per API key
- **Status Queries**: 100 requests per minute per API key
- **Configuration**: 5 requests per minute per API key

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1642248600
```

## Webhooks

Configure webhooks to receive test completion notifications:

```http
POST /webhooks/register
Content-Type: application/json

{
  "url": "https://your-system.com/webhook",
  "events": ["test.completed", "test.failed"],
  "secret": "your-webhook-secret"
}
```

Webhook payload example:
```json
{
  "event": "test.completed",
  "testId": "test-12345",
  "status": "PASSED",
  "timestamp": "2025-01-15T10:35:00Z",
  "results": {
    "duration": "PT4M15S",
    "successRate": 100.0
  }
}
```

## SDK and Client Libraries

Official client libraries are available for:

- **Java**: `agentic-e2e-client-java`
- **Python**: `agentic-e2e-client-python`
- **Node.js**: `agentic-e2e-client-node`
- **Go**: `agentic-e2e-client-go`

Example usage (Java):
```java
AgenticTestClient client = AgenticTestClient.builder()
    .baseUrl("http://localhost:8080")
    .apiKey("your-api-key")
    .build();

TestResult result = client.executeTest(
    "Test order fulfillment flow",
    Map.of("productId", "12345")
);
```