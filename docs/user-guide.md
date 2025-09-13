# User Guide

This guide covers how to use the Agentic E2E Tester for comprehensive end-to-end testing of microservices architectures.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Writing Test Scenarios](#writing-test-scenarios)
3. [Test Execution](#test-execution)
4. [Monitoring and Analysis](#monitoring-and-analysis)
5. [Configuration](#configuration)
6. [Best Practices](#best-practices)

## Getting Started

### Prerequisites

Before using the Agentic E2E Tester, ensure you have:

1. **System Access**: API credentials or web interface access
2. **Service Discovery**: Your microservices are discoverable or manually registered
3. **Test Environment**: A dedicated testing environment with realistic data
4. **Basic Understanding**: Familiarity with your system's business flows

### First Steps

1. **Verify System Health**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Check Service Discovery**
   ```bash
   curl http://localhost:8080/api/v1/services
   ```

3. **Test AI Integration**
   ```bash
   curl -X POST http://localhost:8080/api/v1/tests/parse \
     -H "Content-Type: application/json" \
     -d '{"scenario": "Simple health check test"}'
   ```

## Writing Test Scenarios

### Natural Language Format

The system accepts test scenarios written in plain English. Here are guidelines for effective scenario writing:

#### Basic Structure

```
Test [business flow] from [starting point] to [end point]
```

**Example:**
```
Test order fulfillment from cart creation to delivery confirmation
```

#### Detailed Scenarios

Include specific conditions, data, and expected outcomes:

```
Create order with product ID 12345 and quantity 2
Validate inventory is reduced by 2 units
Process payment of $49.98 using credit card
Confirm shipping notification is sent to customer
Verify order status changes to 'shipped'
```

#### Advanced Scenarios with Conditions

```
Test order fulfillment with the following conditions:
- Customer: premium member with ID customer-001
- Product: high-demand item with ID product-12345
- Payment: credit card ending in 4567
- Shipping: express delivery to zip code 12345

Expected outcomes:
- Order processed within 30 seconds
- Inventory updated in real-time
- Payment authorized successfully
- Express shipping label generated
- Customer notification sent via email and SMS
```

### Scenario Templates

#### E-commerce Order Flow
```
Test complete order processing:
1. Add product [PRODUCT_ID] to cart
2. Apply discount code [DISCOUNT_CODE]
3. Proceed to checkout with customer [CUSTOMER_ID]
4. Process payment using [PAYMENT_METHOD]
5. Validate inventory deduction
6. Generate shipping label
7. Send confirmation email
8. Update order status to 'confirmed'
```

#### User Registration Flow
```
Test new user registration:
1. Submit registration form with email [EMAIL] and password
2. Validate email format and password strength
3. Send verification email
4. Confirm email verification
5. Create user profile in database
6. Send welcome email
7. Log user registration event
```

#### Payment Processing Flow
```
Test payment processing with error handling:
1. Process payment of $[AMOUNT] for order [ORDER_ID]
2. Handle payment gateway timeout (retry 3 times)
3. If payment fails, send failure notification
4. If payment succeeds, update order status
5. Generate receipt and send to customer
6. Log payment transaction
```

### Data Placeholders

Use placeholders for dynamic test data:

- `[PRODUCT_ID]` - Will be replaced with actual product ID
- `[CUSTOMER_ID]` - Customer identifier
- `[ORDER_ID]` - Order reference
- `[AMOUNT]` - Monetary value
- `[EMAIL]` - Email address
- `[TIMESTAMP]` - Current timestamp

## Test Execution

### Execution Methods

#### 1. REST API

Execute tests programmatically using the REST API:

```bash
curl -X POST http://localhost:8080/api/v1/tests/execute \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": "Test order fulfillment from cart to delivery",
    "testData": {
      "productId": "12345",
      "customerId": "customer-001",
      "quantity": 2
    },
    "configuration": {
      "timeout": "PT10M",
      "environment": "staging"
    }
  }'
```

#### 2. Web Interface

1. Navigate to `http://localhost:8080/dashboard`
2. Click "New Test"
3. Enter your scenario in the text area
4. Configure test parameters
5. Click "Execute Test"

#### 3. CI/CD Integration

Integrate with your CI/CD pipeline:

```yaml
# GitLab CI example
test_e2e:
  stage: test
  script:
    - |
      curl -X POST $AGENTIC_TESTER_URL/api/v1/tests/execute \
        -H "Authorization: Bearer $API_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
          "scenario": "Test deployment verification",
          "configuration": {
            "environment": "staging",
            "timeout": "PT15M"
          }
        }' | jq -r '.testId' > test_id.txt
    - |
      TEST_ID=$(cat test_id.txt)
      while true; do
        STATUS=$(curl -s $AGENTIC_TESTER_URL/api/v1/tests/$TEST_ID/status | jq -r '.status')
        if [ "$STATUS" = "COMPLETED" ]; then
          RESULT=$(curl -s $AGENTIC_TESTER_URL/api/v1/tests/$TEST_ID/results | jq -r '.result')
          if [ "$RESULT" = "PASSED" ]; then
            echo "Tests passed!"
            exit 0
          else
            echo "Tests failed!"
            exit 1
          fi
        fi
        sleep 10
      done
```

### Test Configuration

#### Timeout Settings

```json
{
  "configuration": {
    "timeout": "PT10M",           // Overall test timeout
    "stepTimeout": "PT2M",        // Individual step timeout
    "assertionTimeout": "PT30S"   // Assertion timeout
  }
}
```

#### Retry Policies

```json
{
  "configuration": {
    "retryPolicy": {
      "maxAttempts": 3,
      "backoffMultiplier": 2,
      "initialDelay": "PT1S",
      "maxDelay": "PT30S"
    }
  }
}
```

#### Environment Configuration

```json
{
  "configuration": {
    "environment": "staging",
    "services": {
      "order-service": "http://staging-order-service:8080",
      "payment-service": "http://staging-payment-service:8080"
    },
    "testData": {
      "useRealData": false,
      "dataSet": "staging-dataset-v1"
    }
  }
}
```

## Monitoring and Analysis

### Real-time Monitoring

#### Test Progress Dashboard

Access the dashboard at `http://localhost:8080/dashboard` to view:

- **Active Tests**: Currently running tests with progress bars
- **Service Health**: Real-time health status of all services
- **System Metrics**: Performance metrics and resource usage
- **Recent Results**: Latest test results and trends

#### Live Test Execution

When a test is running, you can monitor:

1. **Step-by-step Progress**: See which step is currently executing
2. **Service Interactions**: View API calls and responses in real-time
3. **Correlation Tracking**: Follow requests across services
4. **Performance Metrics**: Response times and throughput

### Test Results Analysis

#### Success Analysis

For passed tests, review:

- **Execution Timeline**: How long each step took
- **Service Performance**: Response times and resource usage
- **Data Validation**: All assertions that passed
- **Coverage Report**: Which services and endpoints were tested

#### Failure Analysis

The AI-powered failure analysis provides:

1. **Root Cause Identification**
   - Primary failure point
   - Contributing factors
   - Confidence score

2. **Impact Assessment**
   - Affected services
   - Business impact
   - Severity classification

3. **Remediation Suggestions**
   - Immediate fixes
   - Long-term improvements
   - Prevention strategies

4. **Similar Failures**
   - Historical patterns
   - Previous resolutions
   - Learning insights

### Metrics and Reporting

#### Key Metrics

- **Success Rate**: Percentage of tests passing
- **Average Duration**: Mean test execution time
- **Service Reliability**: Individual service success rates
- **Performance Trends**: Response time trends over time

#### Custom Reports

Generate reports for:

- **Daily Test Summary**: All tests executed in a day
- **Service Health Report**: Health trends for each service
- **Performance Report**: Performance metrics and bottlenecks
- **Failure Analysis Report**: Detailed failure patterns and resolutions

## Configuration

### System Configuration

#### AI Model Settings

```yaml
spring:
  ai:
    ollama:
      chat:
        model: codellama:13b  # Use larger model for better accuracy
        options:
          temperature: 0.1    # Low temperature for consistent results
          top-p: 0.9
          max-tokens: 2048
```

#### Test Execution Settings

```yaml
agentic:
  testing:
    execution:
      max-concurrent-tests: 5
      default-timeout: PT5M
      step-timeout: PT2M
    correlation:
      timeout: PT30S
      max-trace-depth: 10
```

#### Service Discovery Settings

```yaml
agentic:
  discovery:
    auto-discovery: true
    health-check-interval: PT30S
    service-timeout: PT10S
    endpoints:
      - pattern: "http://*-service:8080"
        health-path: "/actuator/health"
```

### Environment-Specific Configuration

#### Development Environment

```yaml
spring:
  profiles: dev
agentic:
  testing:
    use-mock-services: true
    log-level: DEBUG
    save-test-data: true
```

#### Staging Environment

```yaml
spring:
  profiles: staging
agentic:
  testing:
    use-real-services: true
    cleanup-test-data: true
    alert-on-failure: true
```

#### Production Environment

```yaml
spring:
  profiles: prod
agentic:
  testing:
    read-only-mode: true
    monitoring-only: true
    alert-threshold: 95
```

## Best Practices

### Writing Effective Test Scenarios

1. **Be Specific**: Include exact data values and expected outcomes
2. **Use Business Language**: Write scenarios that business stakeholders can understand
3. **Cover Edge Cases**: Include error conditions and boundary cases
4. **Keep It Focused**: One scenario should test one business flow
5. **Include Context**: Provide relevant background information

### Test Data Management

1. **Use Realistic Data**: Test with data that resembles production
2. **Isolate Test Data**: Ensure tests don't interfere with each other
3. **Clean Up**: Remove test data after execution
4. **Version Control**: Track test data changes
5. **Secure Sensitive Data**: Mask or encrypt sensitive information

### Performance Optimization

1. **Parallel Execution**: Run independent tests in parallel
2. **Smart Retries**: Use exponential backoff for transient failures
3. **Resource Management**: Monitor and limit resource usage
4. **Caching**: Cache service discovery and configuration data
5. **Monitoring**: Track performance metrics and optimize bottlenecks

### Maintenance and Updates

1. **Regular Health Checks**: Monitor system health continuously
2. **Update Test Scenarios**: Keep scenarios current with business changes
3. **Review Failures**: Analyze and learn from test failures
4. **Update Dependencies**: Keep AI models and libraries updated
5. **Documentation**: Maintain up-to-date documentation

### Integration with Development Workflow

1. **Pre-commit Testing**: Run quick smoke tests before code commits
2. **Pull Request Validation**: Execute relevant tests for code changes
3. **Deployment Verification**: Validate deployments with comprehensive tests
4. **Regression Testing**: Run full test suites on schedule
5. **Performance Monitoring**: Continuous performance validation

### Troubleshooting Common Issues

#### Test Timeouts
- Increase timeout values for slow services
- Check service health and dependencies
- Review network connectivity

#### AI Model Issues
- Verify model is loaded and responding
- Check resource allocation (CPU/memory)
- Review model configuration

#### Service Discovery Problems
- Verify service endpoints are accessible
- Check authentication and authorization
- Review network policies and firewalls

#### Data Validation Failures
- Verify test data format and values
- Check database connectivity
- Review data transformation logic