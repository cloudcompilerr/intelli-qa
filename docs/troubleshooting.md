# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with the Agentic E2E Tester system.

## Table of Contents

1. [Application Startup Issues](#application-startup-issues)
2. [AI Model Problems](#ai-model-problems)
3. [Database Connection Issues](#database-connection-issues)
4. [Kafka Integration Problems](#kafka-integration-problems)
5. [Test Execution Issues](#test-execution-issues)
6. [Performance Problems](#performance-problems)
7. [Security and Authentication Issues](#security-and-authentication-issues)
8. [Monitoring and Logging Issues](#monitoring-and-logging-issues)

## Application Startup Issues

### Symptom: Application fails to start

**Common Causes:**
- Missing dependencies
- Configuration errors
- Port conflicts
- Insufficient resources

**Diagnostic Steps:**

1. **Check application logs:**
   ```bash
   # Docker Compose
   docker-compose logs agentic-tester
   
   # Kubernetes
   kubectl logs deployment/agentic-tester -n agentic-e2e-tester
   ```

2. **Verify configuration:**
   ```bash
   # Check environment variables
   docker exec agentic-tester env | grep -E "(SPRING|AGENTIC)"
   
   # Validate configuration file
   kubectl get configmap agentic-config -o yaml
   ```

3. **Check resource availability:**
   ```bash
   # Docker
   docker stats
   
   # Kubernetes
   kubectl top pods -n agentic-e2e-tester
   kubectl describe pod <pod-name> -n agentic-e2e-tester
   ```

**Solutions:**

1. **Port conflicts:**
   ```yaml
   # Change ports in docker-compose.yml
   ports:
     - "8081:8080"  # Use different host port
   ```

2. **Memory issues:**
   ```yaml
   # Increase memory limits
   environment:
     - JAVA_OPTS=-Xmx4g -Xms2g
   ```

3. **Configuration validation:**
   ```bash
   # Test configuration syntax
   java -jar app.jar --spring.config.location=file:./config/ --spring.profiles.active=test
   ```

### Symptom: Slow startup times

**Diagnostic Steps:**

1. **Check startup logs for bottlenecks:**
   ```bash
   grep -E "(Started|Completed|Initialized)" application.log
   ```

2. **Monitor resource usage during startup:**
   ```bash
   # Monitor CPU and memory
   top -p $(pgrep java)
   ```

**Solutions:**

1. **Optimize JVM settings:**
   ```bash
   export JAVA_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
   ```

2. **Reduce connection pool sizes during startup:**
   ```yaml
   spring:
     datasource:
       hikari:
         minimum-idle: 1
         maximum-pool-size: 5
   ```

## AI Model Problems

### Symptom: AI model not responding

**Diagnostic Steps:**

1. **Check Ollama service status:**
   ```bash
   # Docker
   docker exec ollama ollama list
   curl http://localhost:11434/api/tags
   
   # Kubernetes
   kubectl exec deployment/ollama -n agentic-e2e-tester -- ollama list
   ```

2. **Verify model availability:**
   ```bash
   curl -X POST http://localhost:11434/api/generate \
     -H "Content-Type: application/json" \
     -d '{"model": "codellama:7b", "prompt": "Hello", "stream": false}'
   ```

3. **Check resource usage:**
   ```bash
   # Monitor GPU/CPU usage
   nvidia-smi  # If using GPU
   htop
   ```

**Solutions:**

1. **Download missing models:**
   ```bash
   docker exec ollama ollama pull codellama:7b
   docker exec ollama ollama pull mistral:7b
   ```

2. **Increase timeout values:**
   ```yaml
   agentic:
     ai:
       model-timeout: PT60S
       max-retries: 5
   ```

3. **Switch to smaller model:**
   ```yaml
   spring:
     ai:
       ollama:
         chat:
           model: codellama:7b  # Instead of 13b or 34b
   ```

### Symptom: Poor AI response quality

**Diagnostic Steps:**

1. **Check model parameters:**
   ```yaml
   spring:
     ai:
       ollama:
         chat:
           options:
             temperature: 0.1  # Lower for more consistent responses
             top-p: 0.9
   ```

2. **Review prompt templates:**
   ```bash
   grep -r "prompt-templates" config/
   ```

**Solutions:**

1. **Optimize prompt engineering:**
   ```yaml
   agentic:
     ai:
       prompt-templates:
         test-parsing: |
           You are an expert test engineer. Parse this scenario into structured test steps:
           
           Scenario: {scenario}
           
           Requirements:
           - Be specific and actionable
           - Include validation steps
           - Consider error cases
   ```

2. **Use appropriate model for task:**
   ```yaml
   agentic:
     ai:
       models:
         primary: codellama:13b      # Better for code understanding
         analysis: mistral:7b        # Good for text analysis
   ```

## Database Connection Issues

### Symptom: Database connection failures

**Diagnostic Steps:**

1. **Test database connectivity:**
   ```bash
   # Direct connection test
   psql -h postgres -U agentic_user -d vectordb -c "SELECT 1;"
   
   # From application container
   kubectl exec deployment/agentic-tester -n agentic-e2e-tester -- \
     pg_isready -h postgres-service -p 5432
   ```

2. **Check connection pool status:**
   ```bash
   curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
   ```

3. **Review database logs:**
   ```bash
   kubectl logs deployment/postgres -n agentic-e2e-tester
   ```

**Solutions:**

1. **Fix connection string:**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://postgres-service:5432/vectordb
       username: ${POSTGRES_USERNAME}
       password: ${POSTGRES_PASSWORD}
   ```

2. **Adjust connection pool:**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 10
         connection-timeout: 20000
         idle-timeout: 300000
   ```

3. **Check network policies:**
   ```bash
   kubectl get networkpolicy -n agentic-e2e-tester
   ```

### Symptom: Slow database queries

**Diagnostic Steps:**

1. **Enable query logging:**
   ```yaml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```

2. **Check database performance:**
   ```sql
   -- Check slow queries
   SELECT query, mean_time, calls 
   FROM pg_stat_statements 
   ORDER BY mean_time DESC 
   LIMIT 10;
   
   -- Check index usage
   SELECT schemaname, tablename, attname, n_distinct, correlation 
   FROM pg_stats 
   WHERE tablename = 'test_patterns';
   ```

**Solutions:**

1. **Add database indexes:**
   ```sql
   CREATE INDEX idx_test_patterns_embedding ON test_patterns USING ivfflat (embedding vector_cosine_ops);
   CREATE INDEX idx_test_patterns_created_at ON test_patterns (created_at);
   ```

2. **Optimize queries:**
   ```java
   @Query("SELECT tp FROM TestPattern tp WHERE tp.createdAt > :since ORDER BY tp.createdAt DESC")
   List<TestPattern> findRecentPatterns(@Param("since") Instant since, Pageable pageable);
   ```

## Kafka Integration Problems

### Symptom: Kafka connection failures

**Diagnostic Steps:**

1. **Test Kafka connectivity:**
   ```bash
   # List topics
   kubectl exec deployment/kafka -n agentic-e2e-tester -- \
     kafka-topics.sh --list --bootstrap-server localhost:9092
   
   # Test producer
   kubectl exec deployment/kafka -n agentic-e2e-tester -- \
     kafka-console-producer.sh --topic test-topic --bootstrap-server localhost:9092
   ```

2. **Check consumer group status:**
   ```bash
   kubectl exec deployment/kafka -n agentic-e2e-tester -- \
     kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
     --describe --group agentic-e2e-tester
   ```

**Solutions:**

1. **Fix bootstrap servers configuration:**
   ```yaml
   spring:
     kafka:
       bootstrap-servers: kafka-service:9092
   ```

2. **Create missing topics:**
   ```bash
   kubectl exec deployment/kafka -n agentic-e2e-tester -- \
     kafka-topics.sh --create --topic agentic.test.events \
     --bootstrap-server localhost:9092 --partitions 6 --replication-factor 1
   ```

### Symptom: High consumer lag

**Diagnostic Steps:**

1. **Monitor consumer lag:**
   ```bash
   kubectl exec deployment/kafka -n agentic-e2e-tester -- \
     kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
     --describe --group agentic-e2e-tester
   ```

2. **Check consumer performance:**
   ```bash
   curl http://localhost:8081/actuator/metrics/kafka.consumer.fetch.rate
   ```

**Solutions:**

1. **Increase consumer instances:**
   ```yaml
   spring:
     kafka:
       consumer:
         max-poll-records: 100
         fetch-max-wait: 500
   ```

2. **Optimize message processing:**
   ```java
   @KafkaListener(topics = "agentic.test.events", concurrency = "3")
   public void handleTestEvent(TestEvent event) {
       // Process asynchronously
       CompletableFuture.runAsync(() -> processEvent(event));
   }
   ```

## Test Execution Issues

### Symptom: Tests failing unexpectedly

**Diagnostic Steps:**

1. **Check test execution logs:**
   ```bash
   curl http://localhost:8080/api/v1/tests/{testId}/logs
   ```

2. **Review correlation traces:**
   ```bash
   curl http://localhost:8080/api/v1/tests/{testId}/trace
   ```

3. **Verify service health:**
   ```bash
   curl http://localhost:8080/api/v1/services
   ```

**Solutions:**

1. **Increase test timeouts:**
   ```yaml
   agentic:
     testing:
       execution:
         default-timeout: PT15M
         step-timeout: PT5M
   ```

2. **Improve error handling:**
   ```java
   @Retryable(value = {ServiceUnavailableException.class}, maxAttempts = 3)
   public TestResult executeTestStep(TestStep step) {
       // Implementation with proper error handling
   }
   ```

### Symptom: Tests hanging or timing out

**Diagnostic Steps:**

1. **Check active test count:**
   ```bash
   curl http://localhost:8081/actuator/metrics/agentic.tests.active
   ```

2. **Monitor thread usage:**
   ```bash
   curl http://localhost:8081/actuator/metrics/jvm.threads.live
   ```

**Solutions:**

1. **Adjust thread pool configuration:**
   ```yaml
   agentic:
     testing:
       execution:
         thread-pool-size: 20
         queue-capacity: 100
   ```

2. **Implement proper timeout handling:**
   ```java
   @Async
   @Timeout(value = 300, unit = TimeUnit.SECONDS)
   public CompletableFuture<TestResult> executeTest(TestExecutionPlan plan) {
       // Implementation
   }
   ```

## Performance Problems

### Symptom: High memory usage

**Diagnostic Steps:**

1. **Monitor JVM memory:**
   ```bash
   curl http://localhost:8081/actuator/metrics/jvm.memory.used
   curl http://localhost:8081/actuator/metrics/jvm.memory.max
   ```

2. **Generate heap dump:**
   ```bash
   kubectl exec deployment/agentic-tester -n agentic-e2e-tester -- \
     jcmd 1 GC.run_finalization
   ```

**Solutions:**

1. **Optimize JVM settings:**
   ```bash
   export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
   ```

2. **Implement caching strategies:**
   ```java
   @Cacheable(value = "testPatterns", key = "#scenario")
   public List<TestPattern> findSimilarPatterns(String scenario) {
       // Implementation
   }
   ```

### Symptom: High CPU usage

**Diagnostic Steps:**

1. **Profile CPU usage:**
   ```bash
   kubectl exec deployment/agentic-tester -n agentic-e2e-tester -- \
     jcmd 1 JFR.start duration=60s filename=profile.jfr
   ```

2. **Check thread contention:**
   ```bash
   curl http://localhost:8081/actuator/metrics/jvm.threads.blocked
   ```

**Solutions:**

1. **Optimize concurrent processing:**
   ```java
   @Async("testExecutionTaskExecutor")
   public CompletableFuture<Void> processTestBatch(List<TestStep> steps) {
       // Parallel processing
   }
   ```

2. **Implement rate limiting:**
   ```java
   @RateLimiter(name = "testExecution", fallbackMethod = "fallbackExecution")
   public TestResult executeTest(TestExecutionPlan plan) {
       // Implementation
   }
   ```

## Security and Authentication Issues

### Symptom: Authentication failures

**Diagnostic Steps:**

1. **Check authentication configuration:**
   ```yaml
   agentic:
     security:
       authentication:
         type: jwt
         jwt:
           secret: ${JWT_SECRET}
   ```

2. **Verify token validity:**
   ```bash
   curl -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/v1/auth/validate
   ```

**Solutions:**

1. **Fix JWT configuration:**
   ```yaml
   agentic:
     security:
       authentication:
         jwt:
           expiration: PT24H
           issuer: agentic-e2e-tester
   ```

2. **Update security filters:**
   ```java
   @Override
   protected void configure(HttpSecurity http) throws Exception {
       http.oauth2ResourceServer()
           .jwt()
           .jwtDecoder(jwtDecoder());
   }
   ```

## Monitoring and Logging Issues

### Symptom: Missing metrics

**Diagnostic Steps:**

1. **Check metrics endpoint:**
   ```bash
   curl http://localhost:8081/actuator/prometheus
   ```

2. **Verify Prometheus configuration:**
   ```yaml
   # prometheus.yml
   scrape_configs:
     - job_name: 'agentic-e2e-tester'
       static_configs:
         - targets: ['agentic-tester:8081']
   ```

**Solutions:**

1. **Enable missing metrics:**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus
   ```

2. **Add custom metrics:**
   ```java
   @Component
   public class CustomMetrics {
       private final Counter testCounter;
       
       public CustomMetrics(MeterRegistry meterRegistry) {
           this.testCounter = Counter.builder("agentic.tests.total")
               .register(meterRegistry);
       }
   }
   ```

### Symptom: Log aggregation issues

**Diagnostic Steps:**

1. **Check log format:**
   ```yaml
   logging:
     pattern:
       console: '{"timestamp":"%d","level":"%level","message":"%msg"}%n'
   ```

2. **Verify log shipping:**
   ```bash
   kubectl logs deployment/filebeat -n logging
   ```

**Solutions:**

1. **Configure structured logging:**
   ```yaml
   logging:
     config: classpath:logback-spring.xml
   ```

2. **Set up log rotation:**
   ```yaml
   logging:
     file:
       name: /app/logs/application.log
       max-size: 100MB
       max-history: 30
   ```

## Emergency Procedures

### System Recovery

1. **Graceful shutdown:**
   ```bash
   kubectl scale deployment agentic-tester --replicas=0 -n agentic-e2e-tester
   ```

2. **Database backup:**
   ```bash
   kubectl exec deployment/postgres -n agentic-e2e-tester -- \
     pg_dump -U agentic_user vectordb > backup.sql
   ```

3. **Service restart:**
   ```bash
   kubectl rollout restart deployment/agentic-tester -n agentic-e2e-tester
   ```

### Data Recovery

1. **Restore from backup:**
   ```bash
   kubectl exec -i deployment/postgres -n agentic-e2e-tester -- \
     psql -U agentic_user vectordb < backup.sql
   ```

2. **Clear corrupted cache:**
   ```bash
   kubectl exec deployment/redis -n agentic-e2e-tester -- redis-cli FLUSHALL
   ```

## Getting Help

### Log Collection

```bash
# Collect all relevant logs
kubectl logs deployment/agentic-tester -n agentic-e2e-tester --previous > app.log
kubectl logs deployment/postgres -n agentic-e2e-tester > postgres.log
kubectl logs deployment/kafka -n agentic-e2e-tester > kafka.log
kubectl describe pod -l app=agentic-tester -n agentic-e2e-tester > pod-describe.log
```

### System Information

```bash
# Gather system information
kubectl get all -n agentic-e2e-tester > resources.yaml
kubectl top pods -n agentic-e2e-tester > resource-usage.txt
curl http://localhost:8081/actuator/health > health-check.json
```

### Support Channels

- **Documentation**: Check the [User Guide](./user-guide.md) and [API Documentation](./api/README.md)
- **Configuration**: Review [Configuration Reference](./configuration.md)
- **Deployment**: See [Deployment Guide](./deployment/README.md)
- **Community**: Join our community forums or Slack channel
- **Enterprise Support**: Contact your support representative