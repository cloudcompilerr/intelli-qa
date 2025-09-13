# Quick Start Guide

Get the Agentic E2E Tester up and running in minutes.

## Prerequisites

- **Docker** 20.10+ and Docker Compose
- **Java** 17 or higher
- **Maven** 3.8+
- **Git** for cloning the repository

## 1. Clone and Setup

```bash
git clone <your-repository-url>
cd agentic-e2e-tester
```

## 2. Start Dependencies with Docker Compose

The system requires several services to run. Start them all with:

```bash
docker-compose up -d
```

This starts:
- **Ollama LLM** (localhost:11434) - Local AI model
- **Kafka** (localhost:9092) - Message broker
- **Couchbase** (localhost:8091) - Database
- **Prometheus** (localhost:9090) - Metrics collection

## 3. Initialize the Vector Database

```bash
./scripts/init-vector-db.sql
```

## 4. Setup Local LLM

```bash
./scripts/setup-ollama.sh
```

This script will:
- Pull the CodeLlama model for code understanding
- Configure Ollama for optimal performance
- Test the connection

## 5. Build and Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 6. Verify Installation

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### AI Service Check
```bash
curl http://localhost:8080/api/v1/ai/health
```

### Test the API
```bash
curl -X POST http://localhost:8080/api/v1/tests/parse \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": "Test order fulfillment flow from cart to delivery confirmation"
  }'
```

## 7. Run Your First Test

### Using the REST API

```bash
curl -X POST http://localhost:8080/api/v1/tests/execute \
  -H "Content-Type: application/json" \
  -d '{
    "scenario": "Create order with product ID 12345, validate inventory deduction, confirm payment processing, and verify shipping notification",
    "testData": {
      "productId": "12345",
      "customerId": "customer-001",
      "quantity": 2
    }
  }'
```

### Using the Web Interface

1. Open `http://localhost:8080` in your browser
2. Navigate to the Test Dashboard
3. Click "Create New Test"
4. Enter your test scenario in plain English
5. Click "Execute Test"

## 8. Monitor Test Execution

### Real-time Dashboard
- Open `http://localhost:8080/dashboard`
- View live test execution progress
- Monitor service health and interactions

### Prometheus Metrics
- Open `http://localhost:9090`
- Query test execution metrics
- Set up alerts for failures

## Configuration

### Environment Variables

```bash
# LLM Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=codellama:7b

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Couchbase Configuration
COUCHBASE_CONNECTION_STRING=couchbase://localhost
COUCHBASE_USERNAME=Administrator
COUCHBASE_PASSWORD=password

# Application Configuration
SPRING_PROFILES_ACTIVE=local
LOG_LEVEL=INFO
```

### Application Properties

Key configuration in `application.yml`:

```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        model: ${OLLAMA_MODEL:codellama:7b}
        options:
          temperature: 0.1
          top-p: 0.9

agentic:
  testing:
    correlation:
      timeout: 30s
    execution:
      max-concurrent-tests: 5
      default-timeout: 300s
```

## Next Steps

1. **Read the User Guide**: [User Guide](./user-guide.md)
2. **Explore Examples**: [Examples](./examples/README.md)
3. **Configure for Your Environment**: [Configuration Reference](./configuration.md)
4. **Set up CI/CD Integration**: [Deployment Guide](./deployment/README.md)

## Troubleshooting

If you encounter issues:

1. **Check service health**: `docker-compose ps`
2. **View logs**: `docker-compose logs -f`
3. **Verify connectivity**: Test each service endpoint
4. **Review configuration**: Check environment variables
5. **Consult troubleshooting guide**: [Troubleshooting](./troubleshooting.md)

## Common Issues

### Ollama Model Not Loading
```bash
# Check if model is downloaded
docker exec ollama ollama list

# Pull model manually if needed
docker exec ollama ollama pull codellama:7b
```

### Kafka Connection Issues
```bash
# Check Kafka is running
docker-compose ps kafka

# Test Kafka connectivity
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Couchbase Authentication
```bash
# Reset Couchbase admin password
docker exec couchbase couchbase-cli user-manage --cluster localhost \
  --username Administrator --password password \
  --set --rbac-username testuser --rbac-password testpass \
  --roles admin --auth-domain local
```