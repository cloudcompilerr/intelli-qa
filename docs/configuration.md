# Configuration Reference

This document provides comprehensive configuration options for the Agentic E2E Tester system.

## Table of Contents

1. [Application Configuration](#application-configuration)
2. [AI Model Configuration](#ai-model-configuration)
3. [Database Configuration](#database-configuration)
4. [Kafka Configuration](#kafka-configuration)
5. [Security Configuration](#security-configuration)
6. [Monitoring Configuration](#monitoring-configuration)
7. [Environment-Specific Settings](#environment-specific-settings)

## Application Configuration

### Core Settings

```yaml
agentic:
  testing:
    execution:
      max-concurrent-tests: 10          # Maximum number of concurrent test executions
      default-timeout: PT10M            # Default test timeout (ISO 8601 duration)
      step-timeout: PT2M                # Individual step timeout
      retry-attempts: 3                 # Default retry attempts for failed steps
      retry-delay: PT5S                 # Delay between retry attempts
    
    correlation:
      timeout: PT30S                    # Correlation tracking timeout
      max-trace-depth: 20               # Maximum depth for distributed tracing
      cleanup-interval: PT1H            # Interval for cleaning up old traces
    
    memory:
      pattern-cache-size: 1000          # Number of test patterns to cache
      history-retention-days: 30        # Days to retain test execution history
      vector-dimensions: 1536           # Vector dimensions for embeddings
    
    validation:
      strict-mode: true                 # Enable strict validation mode
      schema-validation: true           # Enable JSON schema validation
      type-checking: true               # Enable type checking for test data
```

### Service Discovery

```yaml
agentic:
  discovery:
    auto-discovery: true                # Enable automatic service discovery
    health-check-interval: PT30S        # Health check interval
    service-timeout: PT10S              # Service response timeout
    max-retries: 3                      # Maximum retries for service calls
    
    endpoints:
      - pattern: "http://*-service:8080"
        health-path: "/actuator/health"
        metrics-path: "/actuator/prometheus"
      - pattern: "https://*.yourdomain.com"
        health-path: "/health"
        auth-required: true
    
    excluded-services:
      - "internal-service"
      - "debug-service"
```

## AI Model Configuration

### Ollama Configuration

```yaml
spring:
  ai:
    ollama:
      base-url: http://ollama:11434       # Ollama service URL
      chat:
        model: codellama:7b               # Default model for chat
        options:
          temperature: 0.1                # Creativity level (0.0-1.0)
          top-p: 0.9                     # Nucleus sampling parameter
          top-k: 40                      # Top-k sampling parameter
          max-tokens: 2048               # Maximum tokens in response
          repeat-penalty: 1.1            # Repetition penalty
          seed: -1                       # Random seed (-1 for random)
      
      embedding:
        model: nomic-embed-text          # Model for embeddings
        options:
          dimensions: 1536               # Embedding dimensions

agentic:
  ai:
    model-timeout: PT30S                 # Timeout for AI model requests
    max-retries: 3                       # Maximum retries for failed requests
    retry-delay: PT2S                    # Delay between retries
    
    prompt-templates:
      test-parsing: |
        Parse the following test scenario into structured steps:
        Scenario: {scenario}
        
        Return a JSON structure with test steps, expected outcomes, and validation rules.
      
      failure-analysis: |
        Analyze the following test failure:
        Error: {error}
        Context: {context}
        Logs: {logs}
        
        Provide root cause analysis and remediation suggestions.
    
    models:
      primary: codellama:7b              # Primary model for test generation
      fallback: mistral:7b               # Fallback model if primary fails
      analysis: codellama:13b            # Model for failure analysis
```

### Model Management

```yaml
agentic:
  ai:
    model-management:
      auto-download: true                # Automatically download missing models
      update-check-interval: PT24H       # Check for model updates
      cleanup-unused: true               # Remove unused models
      max-models-cached: 3               # Maximum models to keep in memory
      
    performance:
      batch-size: 10                     # Batch size for bulk operations
      parallel-requests: 3               # Parallel AI requests
      cache-responses: true              # Cache AI responses
      cache-ttl: PT1H                    # Cache time-to-live
```

## Database Configuration

### PostgreSQL Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/vectordb
    username: ${POSTGRES_USERNAME}
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver
    
    hikari:
      maximum-pool-size: 20              # Maximum connection pool size
      minimum-idle: 5                    # Minimum idle connections
      connection-timeout: 30000          # Connection timeout (ms)
      idle-timeout: 600000               # Idle timeout (ms)
      max-lifetime: 1800000              # Maximum connection lifetime (ms)
      leak-detection-threshold: 60000    # Connection leak detection (ms)
      
  jpa:
    hibernate:
      ddl-auto: validate                 # Schema validation mode
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20                 # Batch size for inserts/updates
        order_inserts: true              # Order inserts for better performance
        order_updates: true              # Order updates for better performance
        jdbc.batch_versioned_data: true  # Enable batch versioned data
```

### Vector Database Configuration

```yaml
agentic:
  vector-store:
    provider: pgvector                   # Vector store provider
    dimensions: 1536                     # Vector dimensions
    similarity-threshold: 0.8            # Similarity threshold for matches
    max-results: 10                      # Maximum results to return
    
    indexing:
      algorithm: ivfflat                 # Indexing algorithm
      lists: 100                         # Number of lists for IVF
      probes: 10                         # Number of probes for search
```

### Couchbase Configuration

```yaml
agentic:
  couchbase:
    connection-string: couchbase://couchbase:11210
    username: ${COUCHBASE_USERNAME}
    password: ${COUCHBASE_PASSWORD}
    bucket: agentic-tests                # Default bucket name
    
    timeout:
      connect: PT10S                     # Connection timeout
      query: PT30S                       # Query timeout
      kv: PT5S                          # Key-value operation timeout
      
    connection-pool:
      max-connections: 50                # Maximum connections
      max-idle-time: PT5M               # Maximum idle time
      
    retry:
      max-attempts: 3                    # Maximum retry attempts
      backoff-multiplier: 2              # Backoff multiplier
      initial-delay: PT1S                # Initial retry delay
```

## Kafka Configuration

### Producer Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                          # Acknowledgment mode
      retries: 3                         # Number of retries
      batch-size: 16384                  # Batch size in bytes
      linger-ms: 5                       # Linger time in milliseconds
      buffer-memory: 33554432            # Buffer memory in bytes
      compression-type: snappy           # Compression type
      
      properties:
        enable.idempotence: true         # Enable idempotent producer
        max.in.flight.requests.per.connection: 5
```

### Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      group-id: agentic-e2e-tester       # Consumer group ID
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest        # Offset reset strategy
      enable-auto-commit: false          # Disable auto-commit
      fetch-min-size: 1                  # Minimum fetch size
      fetch-max-wait: 500                # Maximum fetch wait time
      max-poll-records: 500              # Maximum records per poll
      
      properties:
        spring.json.trusted.packages: "com.agentic.e2etester.model"
        isolation.level: read_committed   # Transaction isolation level
```

### Topic Configuration

```yaml
agentic:
  kafka:
    topics:
      test-events:
        name: agentic.test.events
        partitions: 6                    # Number of partitions
        replication-factor: 3            # Replication factor
        retention-ms: 604800000          # Retention time (7 days)
        
      test-results:
        name: agentic.test.results
        partitions: 3
        replication-factor: 3
        retention-ms: 2592000000         # Retention time (30 days)
        
      service-events:
        name: agentic.service.events
        partitions: 12
        replication-factor: 3
        retention-ms: 86400000           # Retention time (1 day)
```

## Security Configuration

### Authentication

```yaml
agentic:
  security:
    enabled: true                        # Enable security features
    
    authentication:
      type: jwt                          # Authentication type (jwt, basic, oauth2)
      jwt:
        secret: ${JWT_SECRET}            # JWT signing secret
        expiration: PT24H                # Token expiration time
        issuer: agentic-e2e-tester       # Token issuer
        
      basic:
        enabled: false                   # Enable basic authentication
        
      oauth2:
        enabled: false                   # Enable OAuth2
        provider: keycloak               # OAuth2 provider
        client-id: ${OAUTH2_CLIENT_ID}
        client-secret: ${OAUTH2_CLIENT_SECRET}
        authorization-uri: ${OAUTH2_AUTH_URI}
        token-uri: ${OAUTH2_TOKEN_URI}
```

### Authorization

```yaml
agentic:
  security:
    authorization:
      enabled: true                      # Enable authorization
      default-role: USER                 # Default role for authenticated users
      
      roles:
        ADMIN:
          permissions:
            - "test:execute"
            - "test:delete"
            - "config:update"
            - "system:admin"
        USER:
          permissions:
            - "test:execute"
            - "test:view"
        VIEWER:
          permissions:
            - "test:view"
      
      endpoints:
        "/api/v1/admin/**": ["ADMIN"]
        "/api/v1/tests/execute": ["ADMIN", "USER"]
        "/api/v1/tests/view": ["ADMIN", "USER", "VIEWER"]
```

### Audit Configuration

```yaml
agentic:
  security:
    audit:
      enabled: true                      # Enable audit logging
      log-successful-auth: true          # Log successful authentications
      log-failed-auth: true              # Log failed authentications
      log-admin-actions: true            # Log administrative actions
      
      retention:
        days: 90                         # Audit log retention period
        max-size: 1GB                    # Maximum audit log size
        
      events:
        - TEST_EXECUTED
        - TEST_DELETED
        - CONFIG_CHANGED
        - USER_LOGIN
        - USER_LOGOUT
        - ADMIN_ACTION
```

## Monitoring Configuration

### Metrics Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
      
  endpoint:
    health:
      show-details: when-authorized      # Health detail visibility
      show-components: always            # Show component health
      
    metrics:
      enabled: true                      # Enable metrics endpoint
      
    prometheus:
      enabled: true                      # Enable Prometheus metrics
      
  metrics:
    export:
      prometheus:
        enabled: true                    # Enable Prometheus export
        step: PT15S                      # Metrics collection interval
        
    tags:
      application: agentic-e2e-tester    # Application tag
      environment: ${ENVIRONMENT:dev}    # Environment tag
      
    distribution:
      percentiles-histogram:
        http.server.requests: true       # Enable request histograms
        agentic.test.duration: true      # Enable test duration histograms
```

### Custom Metrics

```yaml
agentic:
  monitoring:
    custom-metrics:
      enabled: true                      # Enable custom metrics
      
      counters:
        - name: agentic_tests_total
          description: "Total number of tests executed"
          tags: ["environment", "service"]
          
        - name: agentic_tests_failed_total
          description: "Total number of failed tests"
          tags: ["environment", "service", "failure_type"]
          
      gauges:
        - name: agentic_tests_active
          description: "Number of currently active tests"
          
        - name: agentic_service_health_status
          description: "Service health status (1=healthy, 0=unhealthy)"
          tags: ["service_name"]
          
      timers:
        - name: agentic_test_duration
          description: "Test execution duration"
          tags: ["test_type", "environment"]
          
        - name: agentic_ai_response_duration
          description: "AI model response time"
          tags: ["model", "operation"]
```

## Environment-Specific Settings

### Development Environment

```yaml
spring:
  profiles: dev
  
agentic:
  testing:
    execution:
      max-concurrent-tests: 3            # Lower concurrency for dev
    
  ai:
    model-timeout: PT60S                 # Longer timeout for debugging
    
  security:
    enabled: false                       # Disable security for dev
    
  monitoring:
    debug-mode: true                     # Enable debug monitoring
    
logging:
  level:
    com.agentic.e2etester: DEBUG         # Debug logging
    org.springframework.ai: DEBUG        # AI framework debugging
```

### Staging Environment

```yaml
spring:
  profiles: staging
  
agentic:
  testing:
    execution:
      max-concurrent-tests: 5            # Moderate concurrency
    
  security:
    enabled: true                        # Enable security
    authentication:
      type: basic                        # Simple auth for staging
      
  monitoring:
    alert-on-failure: true               # Enable failure alerts
    
logging:
  level:
    root: INFO                           # Info level logging
    com.agentic.e2etester: DEBUG         # Debug app logging
```

### Production Environment

```yaml
spring:
  profiles: prod
  
agentic:
  testing:
    execution:
      max-concurrent-tests: 20           # High concurrency for prod
      
  security:
    enabled: true                        # Full security enabled
    authentication:
      type: jwt                          # JWT authentication
    audit:
      enabled: true                      # Full audit logging
      
  monitoring:
    alert-on-failure: true               # Enable all alerts
    performance-monitoring: true         # Enable performance monitoring
    
logging:
  level:
    root: WARN                           # Warn level for root
    com.agentic.e2etester: INFO          # Info level for app
    
  file:
    name: /app/logs/agentic-e2e-tester.log
    max-size: 100MB                      # Log file rotation
    max-history: 30                      # Keep 30 days of logs
```

## Configuration Validation

### Schema Validation

```yaml
agentic:
  config:
    validation:
      enabled: true                      # Enable config validation
      strict-mode: true                  # Strict validation mode
      fail-on-unknown-properties: true   # Fail on unknown properties
      
    schema:
      test-execution-plan: classpath:schemas/test-execution-plan.json
      test-result: classpath:schemas/test-result.json
      service-config: classpath:schemas/service-config.json
```

### Environment Variable Overrides

```bash
# Core settings
AGENTIC_TESTING_EXECUTION_MAX_CONCURRENT_TESTS=10
AGENTIC_TESTING_EXECUTION_DEFAULT_TIMEOUT=PT10M

# AI configuration
SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434
SPRING_AI_OLLAMA_CHAT_MODEL=codellama:7b

# Database configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/vectordb
POSTGRES_USERNAME=agentic_user
POSTGRES_PASSWORD=secure_password

# Security configuration
AGENTIC_SECURITY_ENABLED=true
JWT_SECRET=your-jwt-secret-key

# Monitoring configuration
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

## Configuration Best Practices

1. **Use Environment Variables**: For sensitive data and environment-specific settings
2. **Profile-Specific Configs**: Separate configurations for different environments
3. **Validation**: Always validate configuration on startup
4. **Documentation**: Document all configuration options
5. **Defaults**: Provide sensible defaults for all settings
6. **Security**: Never commit secrets to version control
7. **Monitoring**: Monitor configuration changes and their impact